package com.austin.civicnations.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class NationSavedData extends SavedData {
    private static final String DATA_NAME = "civicnations_nations";

    private final Map<UUID, NationRecord> nations = new LinkedHashMap<>();
    private final Map<UUID, UUID> nationByPlayer = new HashMap<>();
    private final Map<UUID, UUID> nationByFtbTeam = new HashMap<>();
    private final Map<UUID, Map<UUID, NationInvitation>> invitationsByPlayer = new HashMap<>();
    private final Map<UUID, Long> joinCooldownUntil = new HashMap<>();

    public static NationSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                NationSavedData::load,
                NationSavedData::new,
                DATA_NAME
        );
    }

    public Collection<NationRecord> getNations() {
        return Collections.unmodifiableCollection(nations.values());
    }

    public Optional<NationRecord> getNation(UUID nationId) {
        return Optional.ofNullable(nations.get(nationId));
    }

    public Optional<NationRecord> findNationByName(String name) {
        String normalized = normalize(name);
        return nations.values().stream()
                .filter(nation -> normalize(nation.name()).equals(normalized))
                .findFirst();
    }

    public Optional<NationRecord> getNationForPlayer(UUID playerId) {
        UUID nationId = nationByPlayer.get(playerId);
        return nationId == null ? Optional.empty() : getNation(nationId);
    }

    public Optional<NationRecord> getNationForFtbTeam(UUID teamId) {
        UUID nationId = nationByFtbTeam.get(teamId);
        return nationId == null ? Optional.empty() : getNation(nationId);
    }

    public boolean nameTaken(String name) {
        return findNationByName(name).isPresent();
    }

    public boolean abbreviationTaken(String abbreviation) {
        String normalized = normalize(abbreviation);
        return nations.values().stream().anyMatch(n -> normalize(n.abbreviation()).equals(normalized));
    }

    public void addNation(NationRecord nation) {
        nations.put(nation.nationId(), nation);
        nationByFtbTeam.put(nation.ftbTeamId(), nation.nationId());
        nation.members().forEach(member -> nationByPlayer.put(member.playerId(), nation.nationId()));
        setDirty();
    }

    public Optional<NationRecord> removeNation(UUID nationId) {
        NationRecord removed = nations.remove(nationId);
        if (removed == null) {
            return Optional.empty();
        }

        removed.members().forEach(member -> nationByPlayer.remove(member.playerId(), nationId));
        nationByFtbTeam.remove(removed.ftbTeamId(), nationId);
        invitationsByPlayer.values().forEach(invites -> invites.remove(nationId));
        invitationsByPlayer.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        setDirty();
        return Optional.of(removed);
    }

    public boolean addMember(UUID nationId, UUID playerId, NationRole role, long joinedAt) {
        if (nationByPlayer.containsKey(playerId)) {
            return false;
        }
        NationRecord nation = nations.get(nationId);
        if (nation == null || !nation.addMember(playerId, role, joinedAt)) {
            return false;
        }
        nationByPlayer.put(playerId, nationId);
        clearInvitations(playerId);
        setDirty();
        return true;
    }

    public boolean removeMember(UUID nationId, UUID playerId) {
        NationRecord nation = nations.get(nationId);
        if (nation == null || !nation.removeMember(playerId)) {
            return false;
        }
        nationByPlayer.remove(playerId, nationId);
        setDirty();
        return true;
    }

    public boolean setRole(UUID nationId, UUID playerId, NationRole role) {
        NationRecord nation = nations.get(nationId);
        if (nation == null || !nation.setRole(playerId, role)) {
            return false;
        }
        setDirty();
        return true;
    }

    public boolean transferLeadership(UUID nationId, UUID newLeaderId) {
        NationRecord nation = nations.get(nationId);
        if (nation == null || !nation.transferLeadership(newLeaderId)) {
            return false;
        }
        setDirty();
        return true;
    }

    public void addInvitation(NationInvitation invitation) {
        invitationsByPlayer
                .computeIfAbsent(invitation.invitedPlayerId(), ignored -> new LinkedHashMap<>())
                .put(invitation.nationId(), invitation);
        setDirty();
    }

    public Optional<NationInvitation> getInvitation(UUID invitedPlayerId, UUID nationId, long now) {
        pruneExpiredInvitations(now);
        Map<UUID, NationInvitation> invitations = invitationsByPlayer.get(invitedPlayerId);
        return invitations == null ? Optional.empty() : Optional.ofNullable(invitations.get(nationId));
    }

    public List<NationInvitation> getInvitations(UUID invitedPlayerId, long now) {
        pruneExpiredInvitations(now);
        Map<UUID, NationInvitation> invitations = invitationsByPlayer.get(invitedPlayerId);
        return invitations == null ? List.of() : List.copyOf(invitations.values());
    }

    public boolean removeInvitation(UUID invitedPlayerId, UUID nationId) {
        Map<UUID, NationInvitation> invitations = invitationsByPlayer.get(invitedPlayerId);
        if (invitations == null || invitations.remove(nationId) == null) {
            return false;
        }
        if (invitations.isEmpty()) {
            invitationsByPlayer.remove(invitedPlayerId);
        }
        setDirty();
        return true;
    }

    public void clearInvitations(UUID invitedPlayerId) {
        if (invitationsByPlayer.remove(invitedPlayerId) != null) {
            setDirty();
        }
    }

    public void setJoinCooldown(UUID playerId, long until) {
        if (until <= 0L) {
            joinCooldownUntil.remove(playerId);
        } else {
            joinCooldownUntil.put(playerId, until);
        }
        setDirty();
    }

    public long getJoinCooldownUntil(UUID playerId) {
        return joinCooldownUntil.getOrDefault(playerId, 0L);
    }

    public long getJoinCooldownRemaining(UUID playerId, long now) {
        long until = getJoinCooldownUntil(playerId);
        if (until <= now) {
            if (until > 0L) {
                joinCooldownUntil.remove(playerId);
                setDirty();
            }
            return 0L;
        }
        return until - now;
    }

    public void addHistory(UUID nationId, String message) {
        NationRecord nation = nations.get(nationId);
        if (nation == null) {
            return;
        }
        nation.addHistory(System.currentTimeMillis(), message);
        setDirty();
    }

    public void markChanged() {
        setDirty();
    }

    private void pruneExpiredInvitations(long now) {
        boolean changed = false;
        for (Map<UUID, NationInvitation> invitations : invitationsByPlayer.values()) {
            changed |= invitations.values().removeIf(invitation -> invitation.isExpired(now));
        }
        changed |= invitationsByPlayer.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        if (changed) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag nationList = new ListTag();
        nations.values().forEach(nation -> nationList.add(nation.save()));
        root.put("Nations", nationList);

        ListTag invitationList = new ListTag();
        invitationsByPlayer.values().forEach(map -> map.values().forEach(invite -> invitationList.add(invite.save())));
        root.put("Invitations", invitationList);

        ListTag cooldownList = new ListTag();
        joinCooldownUntil.forEach((player, until) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", player);
            entry.putLong("Until", until);
            cooldownList.add(entry);
        });
        root.put("JoinCooldowns", cooldownList);
        return root;
    }

    public static NationSavedData load(CompoundTag root) {
        NationSavedData data = new NationSavedData();

        ListTag nationList = root.getList("Nations", Tag.TAG_COMPOUND);
        for (Tag raw : nationList) {
            NationRecord nation = NationRecord.load((CompoundTag) raw);
            data.nations.put(nation.nationId(), nation);
            data.nationByFtbTeam.put(nation.ftbTeamId(), nation.nationId());
            nation.members().forEach(member -> data.nationByPlayer.put(member.playerId(), nation.nationId()));
        }

        // Migration from the original proof-of-concept, which stored memberships separately.
        if (root.contains("Memberships", Tag.TAG_LIST)) {
            ListTag oldMemberships = root.getList("Memberships", Tag.TAG_COMPOUND);
            for (Tag raw : oldMemberships) {
                CompoundTag entry = (CompoundTag) raw;
                UUID player = entry.getUUID("Player");
                UUID nationId = entry.getUUID("Nation");
                NationRecord nation = data.nations.get(nationId);
                if (nation != null && !nation.isMember(player)) {
                    nation.addMember(player, NationRole.CITIZEN, nation.createdAt());
                    data.nationByPlayer.put(player, nationId);
                }
            }
        }

        ListTag invitationList = root.getList("Invitations", Tag.TAG_COMPOUND);
        for (Tag raw : invitationList) {
            NationInvitation invite = NationInvitation.load((CompoundTag) raw);
            data.invitationsByPlayer
                    .computeIfAbsent(invite.invitedPlayerId(), ignored -> new LinkedHashMap<>())
                    .put(invite.nationId(), invite);
        }

        ListTag cooldownList = root.getList("JoinCooldowns", Tag.TAG_COMPOUND);
        for (Tag raw : cooldownList) {
            CompoundTag entry = (CompoundTag) raw;
            data.joinCooldownUntil.put(entry.getUUID("Player"), entry.getLong("Until"));
        }

        return data;
    }

    private static String normalize(String input) {
        return input.trim().toLowerCase(Locale.ROOT);
    }
}
