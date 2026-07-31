package com.austin.civicnations.service;

import com.austin.civicnations.CivicNations;
import com.austin.civicnations.data.NationInvitation;
import com.austin.civicnations.data.NationMember;
import com.austin.civicnations.data.NationPermission;
import com.austin.civicnations.data.NationRecord;
import com.austin.civicnations.data.NationRole;
import com.austin.civicnations.data.NationSavedData;
import com.austin.civicnations.integration.FTBIntegration;
import com.austin.civicnations.util.BannerDesign;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class NationService {
    public static final int STARTING_CLAIMS = 25;
    public static final long JOIN_COOLDOWN_MILLIS = Duration.ofHours(72).toMillis();
    public static final long INVITATION_DURATION_MILLIS = Duration.ofDays(7).toMillis();

    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9 '\\-]{1,22}[A-Za-z0-9]");
    private static final Pattern ABBREVIATION_PATTERN = Pattern.compile("[A-Za-z0-9]{2,5}");
    private static final Set<String> RESERVED_NAMES = Set.of("admin", "server", "wilderness", "spawn");

    private NationService() {}

    public static NationActionResult createNation(ServerPlayer player, String rawName,
                                                  String rawAbbreviation, ItemStack bannerStack) {
        String name = rawName == null ? "" : rawName.trim();
        String abbreviation = rawAbbreviation == null
                ? ""
                : rawAbbreviation.trim().toUpperCase(Locale.ROOT);

        NationSavedData data = NationSavedData.get(player.server);
        NationActionResult validation = validateCreation(player, data, name, abbreviation, bannerStack);
        if (!validation.success()) {
            sendResult(player, validation);
            return validation;
        }

        Team createdTeam = null;
        UUID nationId = UUID.randomUUID();
        long now = System.currentTimeMillis();

        try {
            createdTeam = FTBIntegration.createNationTeam(player, name, abbreviation, bannerStack);
            NationRecord nation = NationRecord.create(
                    nationId,
                    name,
                    abbreviation,
                    player.getUUID(),
                    createdTeam.getId(),
                    copySingle(bannerStack),
                    now,
                    STARTING_CLAIMS
            );

            nation.addHistory(now, player.getGameProfile().getName() + " founded the nation.");
            data.addNation(nation);
            FTBIntegration.initializeNationTeam(createdTeam);

            NationActionResult result = NationActionResult.success(
                    Component.literal("Nation " + name + " created successfully.")
            );
            sendResult(player, result);
            player.closeContainer();
            return result;
        } catch (Exception exception) {
            CivicNations.LOGGER.error("Failed to create nation '{}' for {}",
                    name, player.getGameProfile().getName(), exception);
            data.removeNation(nationId);

            if (createdTeam instanceof PartyTeam partyTeam) {
                try {
                    partyTeam.forceDisband(player.createCommandSourceStack());
                } catch (CommandSyntaxException rollbackError) {
                    CivicNations.LOGGER.error("Failed to roll back FTB Team after nation creation failure",
                            rollbackError);
                }
            }

            NationActionResult result = NationActionResult.failure(
                    Component.literal("Nation creation failed. Check the server log.")
            );
            sendResult(player, result);
            return result;
        }
    }

    public static NationActionResult createServerNation(ServerPlayer operator, String rawName,
                                                        String rawAbbreviation) {
        String name = rawName == null ? "" : rawName.trim();
        String abbreviation = rawAbbreviation == null
                ? ""
                : rawAbbreviation.trim().toUpperCase(Locale.ROOT);
        NationSavedData data = NationSavedData.get(operator.server);

        if (data.getNationForPlayer(operator.getUUID()).isPresent()) {
            return send(operator, failure("Leave or disband your current nation before creating a server-owned test nation."));
        }
        if (FTBTeamsAPI.api().getManager().getTeamForPlayerID(operator.getUUID())
                .filter(Team::isPartyTeam).isPresent()) {
            return send(operator, failure("Leave your current FTB party before creating a server-owned test nation."));
        }
        NationActionResult identityValidation = validateIdentity(data, name, abbreviation);
        if (!identityValidation.success()) {
            return send(operator, identityValidation);
        }

        ItemStack banner = BannerDesign.create(DyeColor.WHITE, List.of());
        Team createdTeam = null;
        UUID nationId = UUID.randomUUID();
        long now = System.currentTimeMillis();
        try {
            createdTeam = FTBIntegration.createNationTeam(operator, name, abbreviation, banner);
            NationRecord nation = NationRecord.createServerOwned(
                    nationId, name, abbreviation, operator.getUUID(), createdTeam.getId(),
                    banner, now, STARTING_CLAIMS
            );
            nation.addHistory(now, "Server created the nation for role testing.");
            nation.addHistory(now, operator.getGameProfile().getName() + " joined as Citizen.");
            data.addNation(nation);
            FTBIntegration.initializeNationTeam(createdTeam);
            return send(operator, success("Server-owned nation " + name + " created. You are a Citizen for testing."));
        } catch (Exception exception) {
            CivicNations.LOGGER.error("Failed to create server-owned nation '{}'", name, exception);
            data.removeNation(nationId);
            if (createdTeam instanceof PartyTeam partyTeam) {
                try {
                    partyTeam.forceDisband(operator.createCommandSourceStack());
                } catch (CommandSyntaxException rollbackError) {
                    CivicNations.LOGGER.error("Failed to roll back server-owned test team", rollbackError);
                }
            }
            return send(operator, failure("Server-owned nation creation failed. Check the server log."));
        }
    }

    public static NationActionResult adminAddMember(ServerPlayer operator, NationRecord nation,
                                                    ServerPlayer target, NationRole role) {
        NationSavedData data = NationSavedData.get(operator.server);
        if (!nation.serverOwned()) {
            return send(operator, failure("That nation is not server-owned."));
        }
        if (data.getNationForPlayer(target.getUUID()).isPresent()) {
            return send(operator, failure(target.getGameProfile().getName() + " already belongs to a nation."));
        }
        long cooldown = data.getJoinCooldownRemaining(target.getUUID(), System.currentTimeMillis());
        if (cooldown > 0L) {
            return send(operator, failure(target.getGameProfile().getName() + " is on cooldown for "
                    + formatDuration(cooldown) + ". Clear it first if this is a test."));
        }
        NationRole safeRole = role == NationRole.LEADER ? NationRole.OFFICIAL : role;
        try {
            FTBIntegration.joinMember(nation, target);
        } catch (Exception exception) {
            CivicNations.LOGGER.error("Failed to add test member to linked FTB Team", exception);
            return send(operator, failure("The linked FTB Team could not add that player."));
        }
        if (!data.addMember(nation.nationId(), target.getUUID(), safeRole, System.currentTimeMillis())) {
            return send(operator, failure("The test member could not be saved."));
        }
        try {
            FTBIntegration.syncRole(nation, operator, target, safeRole);
        } catch (Exception exception) {
            CivicNations.LOGGER.warn("Civic test role saved, but FTB rank sync failed for {}",
                    target.getGameProfile().getName(), exception);
        }
        data.addHistory(nation.nationId(), target.getGameProfile().getName() + " joined as "
                + safeRole.displayName() + ".");
        return send(operator, success(target.getGameProfile().getName() + " added as "
                + safeRole.displayName() + "."));
    }

    public static NationActionResult adminSetRole(ServerPlayer operator, ServerPlayer target,
                                                  NationRole role) {
        NationSavedData data = NationSavedData.get(operator.server);
        Optional<NationRecord> nationOptional = data.getNationForPlayer(target.getUUID());
        if (nationOptional.isEmpty() || !nationOptional.get().serverOwned()) {
            return send(operator, failure("That player is not in a server-owned nation."));
        }
        if (role == NationRole.LEADER) {
            return send(operator, failure("Server-owned nations have no player leader. Use Citizen, Official, or Treasurer."));
        }
        NationRecord nation = nationOptional.get();
        if (!data.setRole(nation.nationId(), target.getUUID(), role)) {
            return send(operator, failure("The role could not be saved."));
        }
        try {
            FTBIntegration.syncRole(nation, operator, target, role);
        } catch (Exception exception) {
            CivicNations.LOGGER.warn("Civic test role saved, but FTB rank sync failed for {}",
                    target.getGameProfile().getName(), exception);
        }
        data.addHistory(nation.nationId(), target.getGameProfile().getName() + " was assigned "
                + role.displayName() + " by the server.");
        target.sendSystemMessage(Component.literal("Your test role in " + nation.name() + " is now "
                + role.displayName() + ".").withStyle(ChatFormatting.AQUA));
        return send(operator, success(target.getGameProfile().getName() + " is now "
                + role.displayName() + "."));
    }

    public static NationActionResult adminRemoveMember(ServerPlayer operator, ServerPlayer target) {
        NationSavedData data = NationSavedData.get(operator.server);
        Optional<NationRecord> nationOptional = data.getNationForPlayer(target.getUUID());
        if (nationOptional.isEmpty() || !nationOptional.get().serverOwned()) {
            return send(operator, failure("That player is not in a server-owned nation."));
        }
        NationRecord nation = nationOptional.get();
        try {
            FTBIntegration.leaveMember(nation, target.getUUID());
        } catch (Exception exception) {
            CivicNations.LOGGER.error("Failed to remove test member from linked FTB Team", exception);
            return send(operator, failure("The linked FTB Team could not remove that player."));
        }
        if (!data.removeMember(nation.nationId(), target.getUUID())) {
            return send(operator, failure("The test member could not be removed."));
        }
        data.setJoinCooldown(target.getUUID(), System.currentTimeMillis() + JOIN_COOLDOWN_MILLIS);
        data.addHistory(nation.nationId(), target.getGameProfile().getName() + " was removed by the server.");
        return send(operator, success(target.getGameProfile().getName() + " removed from the test nation."));
    }

    public static NationActionResult adminDisbandServerNation(ServerPlayer operator, NationRecord nation) {
        if (!nation.serverOwned()) {
            return send(operator, failure("That nation is not server-owned."));
        }
        int claimedChunks = FTBIntegration.getClaimedChunkCount(nation);
        if (claimedChunks > 0) {
            return send(operator, failure("Unclaim all test territory first. Claims remaining: "
                    + claimedChunks + "."));
        }
        try {
            FTBIntegration.disbandTeam(nation, operator.createCommandSourceStack());
        } catch (Exception exception) {
            CivicNations.LOGGER.error("Failed to disband server-owned nation {}", nation.name(), exception);
            return send(operator, failure("The linked FTB Team could not be disbanded."));
        }
        NationSavedData data = NationSavedData.get(operator.server);
        List<UUID> formerMembers = nation.members().stream().map(NationMember::playerId).toList();
        data.removeNation(nation.nationId());
        long until = System.currentTimeMillis() + JOIN_COOLDOWN_MILLIS;
        formerMembers.forEach(member -> data.setJoinCooldown(member, until));
        return send(operator, success("Server-owned nation " + nation.name() + " disbanded."));
    }

    public static NationActionResult disbandNation(ServerPlayer actor) {
        NationSavedData data = NationSavedData.get(actor.server);
        Optional<NationRecord> nationOptional = data.getNationForPlayer(actor.getUUID());
        if (nationOptional.isEmpty()) {
            return send(actor, failure("You do not belong to a nation."));
        }

        NationRecord nation = nationOptional.get();
        if (nation.serverOwned()) {
            return send(actor, failure("Server-owned nations can only be disbanded with the operator admin command."));
        }
        if (!nation.hasPermission(actor.getUUID(), NationPermission.DISBAND_NATION)) {
            return send(actor, failure("Only the nation leader can disband the nation."));
        }

        int claimedChunks = FTBIntegration.getClaimedChunkCount(nation);
        if (claimedChunks > 0) {
            return send(actor, failure("Unclaim all nation territory before disbanding. Claims remaining: "
                    + claimedChunks + "."));
        }
        if (nation.currency().filter(currency -> currency.treasuryBalance() > 0L
                || currency.accounts().values().stream().anyMatch(balance -> balance > 0L)).isPresent()) {
            return send(actor, failure("Empty the nation treasury and all digital currency accounts before disbanding."));
        }

        try {
            FTBIntegration.disbandTeam(nation, actor.createCommandSourceStack());
        } catch (Exception exception) {
            CivicNations.LOGGER.error("Failed to disband linked FTB Team for {}", nation.name(), exception);
            return send(actor, failure("The linked FTB Team could not be disbanded."));
        }

        long until = System.currentTimeMillis() + JOIN_COOLDOWN_MILLIS;
        List<UUID> formerMembers = nation.members().stream().map(NationMember::playerId).toList();
        data.removeNation(nation.nationId());
        formerMembers.forEach(member -> data.setJoinCooldown(member, until));

        for (UUID memberId : formerMembers) {
            ServerPlayer online = actor.server.getPlayerList().getPlayer(memberId);
            if (online != null) {
                online.sendSystemMessage(Component.literal(
                        nation.name() + " has been disbanded. Nation joining is on cooldown for 72 hours."
                ).withStyle(ChatFormatting.GOLD));
            }
        }

        return NationActionResult.success(Component.literal("Nation disbanded."));
    }

    public static NationActionResult invitePlayer(ServerPlayer actor, ServerPlayer target) {
        NationSavedData data = NationSavedData.get(actor.server);
        Optional<NationRecord> nationOptional = data.getNationForPlayer(actor.getUUID());
        if (nationOptional.isEmpty()) {
            return send(actor, failure("You do not belong to a nation."));
        }

        NationRecord nation = nationOptional.get();
        if (!nation.hasPermission(actor.getUUID(), NationPermission.INVITE_MEMBERS)) {
            return send(actor, failure("Your nation role cannot invite citizens."));
        }
        if (actor.getUUID().equals(target.getUUID())) {
            return send(actor, failure("You cannot invite yourself."));
        }
        if (data.getNationForPlayer(target.getUUID()).isPresent()) {
            return send(actor, failure(target.getGameProfile().getName() + " already belongs to a nation."));
        }

        long now = System.currentTimeMillis();
        long cooldown = data.getJoinCooldownRemaining(target.getUUID(), now);
        if (cooldown > 0L) {
            return send(actor, failure(target.getGameProfile().getName()
                    + " cannot join a nation for another " + formatDuration(cooldown) + "."));
        }

        NationInvitation invitation = new NationInvitation(
                nation.nationId(),
                target.getUUID(),
                actor.getUUID(),
                now,
                now + INVITATION_DURATION_MILLIS
        );
        data.addInvitation(invitation);
        data.addHistory(nation.nationId(), actor.getGameProfile().getName() + " invited "
                + target.getGameProfile().getName() + ".");

        target.sendSystemMessage(Component.literal(
                actor.getGameProfile().getName() + " invited you to join " + nation.name()
                        + ". Use /nation accept " + nation.name() + " or /nation reject " + nation.name() + "."
        ).withStyle(ChatFormatting.AQUA));

        return send(actor, success("Invitation sent to " + target.getGameProfile().getName() + "."));
    }

    public static NationActionResult acceptInvitation(ServerPlayer player, String nationName) {
        NationSavedData data = NationSavedData.get(player.server);
        if (data.getNationForPlayer(player.getUUID()).isPresent()) {
            return send(player, failure("You already belong to a nation."));
        }

        long now = System.currentTimeMillis();
        long cooldown = data.getJoinCooldownRemaining(player.getUUID(), now);
        if (cooldown > 0L) {
            return send(player, failure("You cannot join another nation for "
                    + formatDuration(cooldown) + "."));
        }

        Optional<NationRecord> nationOptional = data.findNationByName(nationName);
        if (nationOptional.isEmpty()) {
            return send(player, failure("No nation named '" + nationName.trim() + "' exists."));
        }
        NationRecord nation = nationOptional.get();

        if (data.getInvitation(player.getUUID(), nation.nationId(), now).isEmpty()) {
            return send(player, failure("You do not have an active invitation from " + nation.name() + "."));
        }

        Optional<Team> effectiveTeam = FTBTeamsAPI.api().getManager().getTeamForPlayer(player);
        if (effectiveTeam.filter(Team::isPartyTeam).isPresent()
                && !effectiveTeam.get().getId().equals(nation.ftbTeamId())) {
            return send(player, failure("Leave your current FTB party before joining a nation."));
        }

        boolean alreadyInLinkedParty = effectiveTeam
                .map(team -> team.getId().equals(nation.ftbTeamId()))
                .orElse(false);

        try {
            if (!alreadyInLinkedParty) {
                FTBIntegration.joinMember(nation, player);
            }
            if (!data.addMember(nation.nationId(), player.getUUID(), NationRole.CITIZEN, now)) {
                if (!alreadyInLinkedParty) {
                    FTBIntegration.leaveMember(nation, player.getUUID());
                }
                return send(player, failure("Nation membership could not be saved."));
            }
        } catch (Exception exception) {
            CivicNations.LOGGER.error("Failed to add {} to nation {}",
                    player.getGameProfile().getName(), nation.name(), exception);
            return send(player, failure("The linked FTB Team could not add you."));
        }

        data.addHistory(nation.nationId(), player.getGameProfile().getName() + " joined the nation.");
        broadcast(nation, player.server, Component.literal(
                player.getGameProfile().getName() + " joined " + nation.name() + "."
        ).withStyle(ChatFormatting.GREEN));
        return NationActionResult.success(Component.literal("You joined " + nation.name() + "."));
    }

    public static NationActionResult rejectInvitation(ServerPlayer player, String nationName) {
        NationSavedData data = NationSavedData.get(player.server);
        Optional<NationRecord> nation = data.findNationByName(nationName);
        if (nation.isEmpty()) {
            return send(player, failure("No nation named '" + nationName.trim() + "' exists."));
        }

        if (!data.removeInvitation(player.getUUID(), nation.get().nationId())) {
            return send(player, failure("You do not have an invitation from " + nation.get().name() + "."));
        }
        return send(player, success("Invitation from " + nation.get().name() + " rejected."));
    }

    public static NationActionResult leaveNation(ServerPlayer player) {
        NationSavedData data = NationSavedData.get(player.server);
        Optional<NationRecord> nationOptional = data.getNationForPlayer(player.getUUID());
        if (nationOptional.isEmpty()) {
            return send(player, failure("You do not belong to a nation."));
        }

        NationRecord nation = nationOptional.get();
        if (nation.leaderUuid().equals(player.getUUID())) {
            int claimedChunks = FTBIntegration.getClaimedChunkCount(nation);
            if (claimedChunks == 0) {
                return send(player, failure("You are the nation leader. Use /nation disband confirm to "
                        + "dissolve this unclaimed nation; no leadership transfer is required."));
            }
            return send(player, failure("Unclaim all " + claimedChunks + " claimed chunk(s), then use "
                    + "/nation disband confirm, or transfer leadership before leaving."));
        }

        try {
            FTBIntegration.leaveMember(nation, player.getUUID());
        } catch (Exception exception) {
            CivicNations.LOGGER.error("Failed to remove {} from linked FTB Team",
                    player.getGameProfile().getName(), exception);
            return send(player, failure("The linked FTB Team could not remove you."));
        }

        data.removeMember(nation.nationId(), player.getUUID());
        data.setJoinCooldown(player.getUUID(), System.currentTimeMillis() + JOIN_COOLDOWN_MILLIS);
        data.addHistory(nation.nationId(), player.getGameProfile().getName() + " left the nation.");
        broadcast(nation, player.server, Component.literal(
                player.getGameProfile().getName() + " left the nation."
        ).withStyle(ChatFormatting.YELLOW));
        return send(player, success("You left " + nation.name() + ". A 72-hour joining cooldown has started."));
    }

    public static NationActionResult removeMember(ServerPlayer actor, ServerPlayer target) {
        NationSavedData data = NationSavedData.get(actor.server);
        Optional<NationRecord> actorNation = data.getNationForPlayer(actor.getUUID());
        Optional<NationRecord> targetNation = data.getNationForPlayer(target.getUUID());
        if (actorNation.isEmpty() || targetNation.isEmpty()
                || !actorNation.get().nationId().equals(targetNation.get().nationId())) {
            return send(actor, failure("That player is not a citizen of your nation."));
        }

        NationRecord nation = actorNation.get();
        if (!nation.hasPermission(actor.getUUID(), NationPermission.REMOVE_MEMBERS)) {
            return send(actor, failure("Your nation role cannot remove citizens."));
        }
        if (target.getUUID().equals(nation.leaderUuid())) {
            return send(actor, failure("The nation leader cannot be removed."));
        }

        NationRole actorRole = nation.roleOf(actor.getUUID());
        NationRole targetRole = nation.roleOf(target.getUUID());
        if (actorRole.authorityLevel() <= targetRole.authorityLevel()) {
            return send(actor, failure("You cannot remove a citizen with an equal or higher role."));
        }

        try {
            FTBIntegration.kickMember(nation, actor, target.getGameProfile());
        } catch (Exception exception) {
            CivicNations.LOGGER.error("Failed to kick {} from linked FTB Team",
                    target.getGameProfile().getName(), exception);
            return send(actor, failure("The linked FTB Team could not remove that player."));
        }

        data.removeMember(nation.nationId(), target.getUUID());
        data.setJoinCooldown(target.getUUID(), System.currentTimeMillis() + JOIN_COOLDOWN_MILLIS);
        data.addHistory(nation.nationId(), target.getGameProfile().getName() + " was removed by "
                + actor.getGameProfile().getName() + ".");
        target.sendSystemMessage(Component.literal(
                "You were removed from " + nation.name() + ". A 72-hour joining cooldown has started."
        ).withStyle(ChatFormatting.RED));
        return send(actor, success(target.getGameProfile().getName() + " was removed from the nation."));
    }

    public static NationActionResult transferLeadership(ServerPlayer actor, ServerPlayer target) {
        NationSavedData data = NationSavedData.get(actor.server);
        Optional<NationRecord> actorNation = data.getNationForPlayer(actor.getUUID());
        Optional<NationRecord> targetNation = data.getNationForPlayer(target.getUUID());
        if (actorNation.isEmpty() || targetNation.isEmpty()
                || !actorNation.get().nationId().equals(targetNation.get().nationId())) {
            return send(actor, failure("That player is not a citizen of your nation."));
        }

        NationRecord nation = actorNation.get();
        if (nation.serverOwned()) {
            return send(actor, failure("Server-owned nations do not transfer leadership."));
        }
        if (!nation.hasPermission(actor.getUUID(), NationPermission.TRANSFER_LEADERSHIP)) {
            return send(actor, failure("Only the nation leader can transfer leadership."));
        }
        if (actor.getUUID().equals(target.getUUID())) {
            return send(actor, failure("You already lead this nation."));
        }

        try {
            FTBIntegration.transferOwnership(nation, actor, target.getGameProfile());
        } catch (Exception exception) {
            CivicNations.LOGGER.error("Failed to transfer linked FTB Team ownership", exception);
            return send(actor, failure("The linked FTB Team could not transfer ownership."));
        }

        if (!data.transferLeadership(nation.nationId(), target.getUUID())) {
            return send(actor, failure("Leadership could not be saved."));
        }

        data.addHistory(nation.nationId(), actor.getGameProfile().getName() + " transferred leadership to "
                + target.getGameProfile().getName() + ".");
        broadcast(nation, actor.server, Component.literal(
                target.getGameProfile().getName() + " is now the leader of " + nation.name() + "."
        ).withStyle(ChatFormatting.GOLD));
        return NationActionResult.success(Component.literal("Leadership transferred."));
    }

    public static NationActionResult setRole(ServerPlayer actor, ServerPlayer target, NationRole newRole) {
        NationSavedData data = NationSavedData.get(actor.server);
        Optional<NationRecord> actorNation = data.getNationForPlayer(actor.getUUID());
        Optional<NationRecord> targetNation = data.getNationForPlayer(target.getUUID());
        if (actorNation.isEmpty() || targetNation.isEmpty()
                || !actorNation.get().nationId().equals(targetNation.get().nationId())) {
            return send(actor, failure("That player is not a citizen of your nation."));
        }

        NationRecord nation = actorNation.get();
        if (!nation.hasPermission(actor.getUUID(), NationPermission.ASSIGN_ROLES)) {
            return send(actor, failure("Only the nation leader can assign roles."));
        }
        if (!newRole.canBeAssignedDirectly()) {
            return send(actor, failure("Use /nation transfer to appoint a new leader."));
        }
        if (nation.leaderUuid().equals(target.getUUID())) {
            return send(actor, failure("Transfer leadership before changing the leader's role."));
        }

        try {
            FTBIntegration.syncRole(nation, actor, target, newRole);
        } catch (Exception exception) {
            CivicNations.LOGGER.error("Failed to synchronize FTB rank for {}",
                    target.getGameProfile().getName(), exception);
            return send(actor, failure("The linked FTB rank could not be updated."));
        }

        if (!data.setRole(nation.nationId(), target.getUUID(), newRole)) {
            return send(actor, failure("The role could not be saved."));
        }

        data.addHistory(nation.nationId(), actor.getGameProfile().getName() + " assigned "
                + target.getGameProfile().getName() + " the role " + newRole.displayName() + ".");
        target.sendSystemMessage(Component.literal(
                "Your role in " + nation.name() + " is now " + newRole.displayName() + "."
        ).withStyle(ChatFormatting.AQUA));
        return send(actor, success(target.getGameProfile().getName() + " is now "
                + newRole.displayName() + "."));
    }

    public static List<NationInvitation> getInvitations(ServerPlayer player) {
        return NationSavedData.get(player.server)
                .getInvitations(player.getUUID(), System.currentTimeMillis());
    }

    private static NationActionResult validateIdentity(NationSavedData data, String name,
                                                       String abbreviation) {
        if (name.length() < 3 || name.length() > 24 || !NAME_PATTERN.matcher(name).matches()) {
            return failure("Nation names must be 3-24 characters and use letters, numbers, spaces, apostrophes, or hyphens.");
        }
        if (RESERVED_NAMES.contains(name.toLowerCase(Locale.ROOT))) {
            return failure("That nation name is reserved.");
        }
        if (data.nameTaken(name)) {
            return failure("That nation name is already taken.");
        }
        if (!ABBREVIATION_PATTERN.matcher(abbreviation).matches()) {
            return failure("Abbreviations must be 2-5 letters or numbers.");
        }
        if (data.abbreviationTaken(abbreviation)) {
            return failure("That abbreviation is already taken.");
        }
        return success("Valid identity.");
    }

    public static NationActionResult validateCreation(ServerPlayer player, NationSavedData data,
                                                       String name, String abbreviation,
                                                       ItemStack bannerStack) {
        if (data.getNationForPlayer(player.getUUID()).isPresent()) {
            return failure("You already belong to a nation.");
        }

        long cooldown = data.getJoinCooldownRemaining(player.getUUID(), System.currentTimeMillis());
        if (cooldown > 0L) {
            return failure("You cannot create or join a nation for another "
                    + formatDuration(cooldown) + ".");
        }

        if (FTBTeamsAPI.api().getManager().getTeamForPlayerID(player.getUUID())
                .filter(Team::isPartyTeam)
                .isPresent()) {
            return failure("You already belong to an FTB party. Leave it before creating a nation.");
        }

        NationActionResult identity = validateIdentity(data, name, abbreviation);
        if (!identity.success()) {
            return identity;
        }
        if (bannerStack.isEmpty() || !(bannerStack.getItem() instanceof BannerItem)) {
            return failure("Choose a banner design for the nation's emblem.");
        }
        return success("Valid nation.");
    }

    public static void sendResult(ServerPlayer player, NationActionResult result) {
        Component styled = result.message().copy().withStyle(
                result.success() ? ChatFormatting.GREEN : ChatFormatting.RED
        );
        player.sendSystemMessage(styled);
        if (!result.success()) {
            player.displayClientMessage(styled, true);
        }
    }

    private static NationActionResult send(ServerPlayer player, NationActionResult result) {
        sendResult(player, result);
        return result;
    }

    private static NationActionResult success(String message) {
        return NationActionResult.success(Component.literal(message));
    }

    private static NationActionResult failure(String message) {
        return NationActionResult.failure(Component.literal(message));
    }

    private static ItemStack copySingle(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private static void broadcast(NationRecord nation, MinecraftServer server, Component message) {
        nation.members().forEach(member -> {
            ServerPlayer online = server.getPlayerList().getPlayer(member.playerId());
            if (online != null) {
                online.sendSystemMessage(message);
            }
        });
    }

    public static String formatDuration(long millis) {
        long totalMinutes = Math.max(1L, Duration.ofMillis(millis).toMinutes());
        long days = totalMinutes / (24L * 60L);
        long hours = (totalMinutes % (24L * 60L)) / 60L;
        long minutes = totalMinutes % 60L;

        List<String> parts = new ArrayList<>();
        if (days > 0L) parts.add(days + (days == 1L ? " day" : " days"));
        if (hours > 0L) parts.add(hours + (hours == 1L ? " hour" : " hours"));
        if (days == 0L && minutes > 0L) parts.add(minutes + (minutes == 1L ? " minute" : " minutes"));
        return String.join(" ", parts);
    }
}
