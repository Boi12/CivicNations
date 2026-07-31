package com.austin.civicnations.integration;

import com.austin.civicnations.CivicNations;
import com.austin.civicnations.data.NationMember;
import com.austin.civicnations.data.NationPermission;
import com.austin.civicnations.data.NationRecord;
import com.austin.civicnations.data.NationRole;
import com.austin.civicnations.data.NationSavedData;
import com.austin.civicnations.service.NationService;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.event.CompoundEventResult;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent;
import dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.api.property.PrivacyMode;
import dev.ftb.mods.ftbteams.api.event.PlayerJoinedPartyTeamEvent;
import dev.ftb.mods.ftbteams.api.event.PlayerLeftPartyTeamEvent;
import dev.ftb.mods.ftbteams.api.event.PlayerLoggedInAfterTeamEvent;
import dev.ftb.mods.ftbteams.api.event.PlayerTransferredTeamOwnershipEvent;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * All direct interaction with FTB Teams and FTB Chunks is isolated here.
 * Civic Nations data is authoritative; FTB data is synchronized to it.
 */
public final class FTBIntegration {
    private static int internalSyncDepth;
    private static boolean registered;

    private FTBIntegration() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        ClaimedChunkEvent.BEFORE_CLAIM.register(FTBIntegration::beforeClaim);
        ClaimedChunkEvent.BEFORE_UNCLAIM.register(FTBIntegration::beforeUnclaim);
        ClaimedChunkEvent.BEFORE_LOAD.register(FTBIntegration::beforeForceLoad);

        TeamEvent.PLAYER_JOINED_PARTY.register(FTBIntegration::onPlayerJoinedParty);
        TeamEvent.PLAYER_LEFT_PARTY.register(FTBIntegration::onPlayerLeftParty);
        TeamEvent.OWNERSHIP_TRANSFERRED.register(FTBIntegration::onOwnershipTransferred);
        TeamEvent.PLAYER_LOGGED_IN.register(FTBIntegration::onPlayerLoggedIn);
        TeamEvent.DELETED.register(FTBIntegration::onTeamDeleted);
    }

    public static void applyServerRules(MinecraftServer server) {
        // Civic Nations owns party creation. Nation creation creates the linked FTB party.
        FTBTeamsAPI.api().setPartyCreationFromAPIOnly(true);

        FTBChunksWorldConfig.MAX_CLAIMED_CHUNKS.set(NationService.STARTING_CLAIMS);
        FTBChunksWorldConfig.HARD_TEAM_CLAIM_LIMIT.set(NationService.STARTING_CLAIMS);
        FTBChunksWorldConfig.MAX_FORCE_LOADED_CHUNKS.set(0);
        FTBChunksWorldConfig.HARD_TEAM_FORCE_LIMIT.set(0);

        if (FTBChunksAPI.api().isManagerLoaded()) {
            int unloaded = 0;
            for (ClaimedChunk claimedChunk : FTBChunksAPI.api().getManager().getAllClaimedChunks()) {
                if (claimedChunk.isForceLoaded()) {
                    claimedChunk.unload(server.createCommandSourceStack());
                    unloaded++;
                }
            }
            if (unloaded > 0) {
                CivicNations.LOGGER.warn("Removed {} existing force-loaded chunk(s)", unloaded);
            }
        }

        NationSavedData nationData = NationSavedData.get(server);
        nationData.getNations().forEach(nation -> getTeam(nation).ifPresent(FTBIntegration::initializeNationTeam));

        CivicNations.LOGGER.info("Applied Civic Nations FTB rules: {} claims, force-loading disabled",
                NationService.STARTING_CLAIMS);
    }

    public static Team createNationTeam(ServerPlayer founder, String name, String abbreviation,
                                        ItemStack bannerStack) throws CommandSyntaxException {
        BannerItem bannerItem = (BannerItem) bannerStack.getItem();
        Color4I teamColor = Color4I.rgb(bannerItem.getColor().getTextColor());
        return runInternal(() -> FTBTeamsAPI.api().getManager().createPartyTeam(
                founder,
                name,
                abbreviation,
                teamColor
        ));
    }

    public static void initializeNationTeam(Team team) {
        ChunkTeamData data = FTBChunksAPI.api().getManager().getOrCreateData(team);
        data.setExtraClaimChunks(0);
        data.setExtraForceLoadChunks(0);

        // FTB Chunks remains the block-protection layer for national territory.
        team.setProperty(FTBChunksProperties.BLOCK_EDIT_MODE, PrivacyMode.PRIVATE);
        team.setProperty(FTBChunksProperties.BLOCK_INTERACT_MODE, PrivacyMode.PRIVATE);
        team.setProperty(FTBChunksProperties.BLOCK_EDIT_AND_INTERACT_MODE, PrivacyMode.PRIVATE);
        team.setProperty(FTBChunksProperties.ENTITY_INTERACT_MODE, PrivacyMode.PRIVATE);
        team.setProperty(FTBChunksProperties.NONLIVING_ENTITY_ATTACK_MODE, PrivacyMode.PRIVATE);
        team.setProperty(FTBChunksProperties.CLAIM_VISIBILITY, PrivacyMode.PUBLIC);
        team.setProperty(FTBChunksProperties.ALLOW_EXPLOSIONS, false);
        team.setProperty(FTBChunksProperties.ALLOW_MOB_GRIEFING, false);

        if (data instanceof ChunkTeamDataImpl implementation) {
            implementation.updateLimits();
        }
    }

    public static Optional<Team> getTeam(NationRecord nation) {
        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            return Optional.empty();
        }
        return FTBTeamsAPI.api().getManager().getTeamByID(nation.ftbTeamId());
    }

    public static Optional<PartyTeam> getPartyTeam(NationRecord nation) {
        return getTeam(nation).filter(PartyTeam.class::isInstance).map(PartyTeam.class::cast);
    }

    public static void joinMember(NationRecord nation, ServerPlayer player) throws CommandSyntaxException {
        PartyTeam team = getPartyTeam(nation)
                .orElseThrow(() -> new IllegalStateException("Linked FTB party is missing"));
        runInternal(() -> {
            team.join(player);
            return null;
        });
    }

    public static void leaveMember(NationRecord nation, UUID playerId) throws CommandSyntaxException {
        PartyTeam team = getPartyTeam(nation)
                .orElseThrow(() -> new IllegalStateException("Linked FTB party is missing"));
        runInternal(() -> {
            team.leave(playerId);
            return null;
        });
    }

    public static void kickMember(NationRecord nation, ServerPlayer actor, GameProfile target)
            throws CommandSyntaxException {
        PartyTeam team = getPartyTeam(nation)
                .orElseThrow(() -> new IllegalStateException("Linked FTB party is missing"));
        runInternal(() -> {
            team.kick(actor.createCommandSourceStack(), List.of(target));
            return null;
        });
    }

    public static void transferOwnership(NationRecord nation, ServerPlayer actor, GameProfile target)
            throws CommandSyntaxException {
        PartyTeam team = getPartyTeam(nation)
                .orElseThrow(() -> new IllegalStateException("Linked FTB party is missing"));
        runInternal(() -> {
            team.transferOwnership(actor.createCommandSourceStack(), target);
            return null;
        });
    }

    public static void disbandTeam(NationRecord nation, CommandSourceStack source)
            throws CommandSyntaxException {
        Optional<PartyTeam> team = getPartyTeam(nation);
        if (team.isEmpty()) {
            return;
        }
        runInternal(() -> {
            team.get().forceDisband(source);
            return null;
        });
    }

    public static void syncRole(NationRecord nation, ServerPlayer actor, ServerPlayer target,
                                NationRole desiredRole) throws CommandSyntaxException {
        PartyTeam team = getPartyTeam(nation)
                .orElseThrow(() -> new IllegalStateException("Linked FTB party is missing"));

        TeamRank current = team.getRankForPlayer(target.getUUID());
        TeamRank desired = switch (desiredRole) {
            case LEADER -> TeamRank.OWNER;
            case TREASURER, OFFICIAL -> TeamRank.OFFICER;
            case CITIZEN -> TeamRank.MEMBER;
        };

        if (current == desired) {
            return;
        }

        runInternal(() -> {
            if (current == TeamRank.MEMBER && desired == TeamRank.OFFICER) {
                team.promote(actor, List.of(target.getGameProfile()));
            } else if (current == TeamRank.OFFICER && desired == TeamRank.MEMBER) {
                team.demote(actor, List.of(target.getGameProfile()));
            }
            return null;
        });
    }

    public static int getClaimedChunkCount(NationRecord nation) {
        if (!FTBTeamsAPI.api().isManagerLoaded() || !FTBChunksAPI.api().isManagerLoaded()) {
            return 0;
        }

        return FTBTeamsAPI.api().getManager().getTeamByID(nation.ftbTeamId())
                .map(team -> FTBChunksAPI.api().getManager().getOrCreateData(team).getClaimedChunks().size())
                .orElse(0);
    }

    public static Optional<NationRecord> getNationAt(ServerLevel level, BlockPos position) {
        if (!FTBChunksAPI.api().isManagerLoaded()) {
            return Optional.empty();
        }

        ClaimedChunk claimedChunk = FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(level, position));
        if (claimedChunk == null) {
            return Optional.empty();
        }

        return NationSavedData.get(level.getServer())
                .getNationForFtbTeam(claimedChunk.getTeamData().getTeam().getId());
    }

    private static CompoundEventResult<ClaimResult> beforeClaim(CommandSourceStack source, ClaimedChunk chunk) {
        ServerPlayer player = source.getEntity() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        if (player == null || source.hasPermission(4)) {
            return CompoundEventResult.pass();
        }

        NationSavedData data = NationSavedData.get(source.getServer());
        Optional<NationRecord> claimedForNation = data.getNationForFtbTeam(chunk.getTeamData().getTeam().getId());
        Optional<NationRecord> playerNation = data.getNationForPlayer(player.getUUID());

        if (claimedForNation.isEmpty() || playerNation.isEmpty()
                || !claimedForNation.get().nationId().equals(playerNation.get().nationId())) {
            return deny("Create or join a nation before claiming chunks.");
        }

        NationRecord nation = playerNation.get();
        if (!nation.hasPermission(player.getUUID(), NationPermission.CLAIM_LAND)) {
            return deny("Your nation role cannot claim land.");
        }

        if (chunk.getTeamData().getClaimedChunks().size() >= nation.claimLimit()) {
            return deny("This nation has reached its " + nation.claimLimit() + "-chunk claim limit.");
        }

        return CompoundEventResult.pass();
    }

    private static CompoundEventResult<ClaimResult> beforeUnclaim(CommandSourceStack source, ClaimedChunk chunk) {
        ServerPlayer player = source.getEntity() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        if (player == null || source.hasPermission(4)) {
            return CompoundEventResult.pass();
        }

        NationSavedData data = NationSavedData.get(source.getServer());
        Optional<NationRecord> nation = data.getNationForFtbTeam(chunk.getTeamData().getTeam().getId());
        if (nation.isEmpty() || !nation.get().isMember(player.getUUID())) {
            return deny("You cannot unclaim another nation's territory.");
        }

        if (!nation.get().hasPermission(player.getUUID(), NationPermission.UNCLAIM_LAND)) {
            return deny("Your nation role cannot unclaim land.");
        }

        return CompoundEventResult.pass();
    }

    private static CompoundEventResult<ClaimResult> beforeForceLoad(CommandSourceStack source, ClaimedChunk chunk) {
        return deny("Force-loaded chunks are disabled on this server.");
    }

    private static CompoundEventResult<ClaimResult> deny(String message) {
        return CompoundEventResult.interruptDefault(ClaimResult.customProblem(message));
    }

    private static void onPlayerJoinedParty(PlayerJoinedPartyTeamEvent event) {
        if (isInternalSync()) {
            return;
        }

        ServerPlayer player = event.getPlayer();
        NationSavedData data = NationSavedData.get(player.server);
        Optional<NationRecord> linkedNation = data.getNationForFtbTeam(event.getTeam().getId());

        if (linkedNation.isEmpty()) {
            // A Civic Nations citizen cannot leave their linked team by joining an unrelated party.
            if (data.getNationForPlayer(player.getUUID()).isPresent()) {
                rejectExternalTeamChange(player, event.getTeam());
            }
            return;
        }

        NationRecord nation = linkedNation.get();
        Optional<NationRecord> currentNation = data.getNationForPlayer(player.getUUID());
        if (currentNation.isPresent() && currentNation.get().nationId().equals(nation.nationId())) {
            return;
        }

        // Accepting a normal FTB invite is not a Civic Nations acceptance and cannot bypass cooldowns.
        rejectExternalTeamChange(player, event.getTeam());
        player.sendSystemMessage(Component.literal(
                "Use /nation accept " + nation.name() + " to join this nation."
        ).withStyle(ChatFormatting.RED));
    }

    private static void rejectExternalTeamChange(ServerPlayer player, Team joinedTeam) {
        if (joinedTeam instanceof PartyTeam joinedParty) {
            try {
                runInternal(() -> {
                    joinedParty.leave(player.getUUID());
                    return null;
                });
            } catch (Exception exception) {
                CivicNations.LOGGER.error("Failed to reject unauthorized FTB party join for {}",
                        player.getGameProfile().getName(), exception);
            }
        }
        repairPlayerTeam(player);
    }

    private static void onPlayerLeftParty(PlayerLeftPartyTeamEvent event) {
        if (isInternalSync()) {
            return;
        }

        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            return;
        }
        MinecraftServer server = FTBTeamsAPI.api().getManager().getServer();

        NationSavedData data = NationSavedData.get(server);
        Optional<NationRecord> linkedNation = data.getNationForFtbTeam(event.getTeam().getId());
        if (linkedNation.isEmpty()) {
            return;
        }

        NationRecord nation = linkedNation.get();
        long cooldownUntil = System.currentTimeMillis() + NationService.JOIN_COOLDOWN_MILLIS;

        if (event.getTeamDeleted()) {
            data.removeNation(nation.nationId()).ifPresent(removed ->
                    removed.members().forEach(member -> data.setJoinCooldown(member.playerId(), cooldownUntil))
            );
        } else if (nation.isMember(event.getPlayerId()) && !nation.leaderUuid().equals(event.getPlayerId())) {
            data.removeMember(nation.nationId(), event.getPlayerId());
            data.setJoinCooldown(event.getPlayerId(), cooldownUntil);
        }
    }

    private static void onOwnershipTransferred(PlayerTransferredTeamOwnershipEvent event) {
        if (isInternalSync()) {
            return;
        }

        if (!FTBTeamsAPI.api().isManagerLoaded() || event.getToProfile() == null) {
            return;
        }
        MinecraftServer server = FTBTeamsAPI.api().getManager().getServer();

        NationSavedData data = NationSavedData.get(server);
        data.getNationForFtbTeam(event.getTeam().getId()).ifPresent(nation -> {
            if (!nation.serverOwned() && nation.isMember(event.getToProfile().getId())) {
                data.transferLeadership(nation.nationId(), event.getToProfile().getId());
            }
        });
    }

    private static void onPlayerLoggedIn(PlayerLoggedInAfterTeamEvent event) {
        repairPlayerTeam(event.getPlayer());
    }

    private static void onTeamDeleted(TeamEvent event) {
        if (isInternalSync()) {
            return;
        }

        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            return;
        }
        MinecraftServer server = FTBTeamsAPI.api().getManager().getServer();

        NationSavedData data = NationSavedData.get(server);
        data.getNationForFtbTeam(event.getTeam().getId()).ifPresent(nation -> {
            long until = System.currentTimeMillis() + NationService.JOIN_COOLDOWN_MILLIS;
            data.removeNation(nation.nationId()).ifPresent(removed ->
                    removed.members().forEach(member -> data.setJoinCooldown(member.playerId(), until))
            );
        });
    }

    public static void repairPlayerTeam(ServerPlayer player) {
        if (!FTBTeamsAPI.api().isManagerLoaded() || isInternalSync()) {
            return;
        }

        NationSavedData data = NationSavedData.get(player.server);
        Optional<NationRecord> civicNation = data.getNationForPlayer(player.getUUID());
        Optional<Team> effectiveTeam = FTBTeamsAPI.api().getManager().getTeamForPlayer(player);

        try {
            if (civicNation.isEmpty()) {
                if (effectiveTeam.isPresent()
                        && effectiveTeam.get() instanceof PartyTeam party
                        && data.getNationForFtbTeam(party.getId()).isPresent()) {
                    runInternal(() -> {
                        party.leave(player.getUUID());
                        return null;
                    });
                }
                return;
            }

            NationRecord nation = civicNation.get();
            Optional<PartyTeam> expectedParty = getPartyTeam(nation);
            if (expectedParty.isEmpty()) {
                player.sendSystemMessage(Component.literal(
                        "Your nation's linked FTB Team is missing. Ask an administrator to repair it."
                ).withStyle(ChatFormatting.RED));
                return;
            }

            if (effectiveTeam.isPresent() && effectiveTeam.get().getId().equals(nation.ftbTeamId())) {
                return;
            }

            if (effectiveTeam.isPresent() && effectiveTeam.get() instanceof PartyTeam wrongParty) {
                runInternal(() -> {
                    wrongParty.leave(player.getUUID());
                    return null;
                });
            }

            runInternal(() -> {
                expectedParty.get().join(player);
                return null;
            });

            NationMember member = nation.member(player.getUUID()).orElse(null);
            if (!nation.serverOwned()) {
                ServerPlayer leader = player.server.getPlayerList().getPlayer(nation.leaderUuid());
                if (member != null && leader != null && member.role() != NationRole.LEADER) {
                    syncRole(nation, leader, player, member.role());
                }
            }
        } catch (Exception exception) {
            CivicNations.LOGGER.error("Failed to repair FTB team membership for {}",
                    player.getGameProfile().getName(), exception);
        }
    }

    private static boolean isInternalSync() {
        return internalSyncDepth > 0;
    }

    private static <T> T runInternal(CheckedSupplier<T> supplier) throws CommandSyntaxException {
        internalSyncDepth++;
        try {
            return supplier.get();
        } finally {
            internalSyncDepth--;
        }
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws CommandSyntaxException;
    }
}
