package com.austin.civicnations.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class NationRecord {
    public static final UUID SERVER_OWNER_UUID = new UUID(0L, 0L);
    private static final int MAX_HISTORY_ENTRIES = 200;

    private final UUID nationId;
    private final String name;
    private final String abbreviation;
    private final UUID founderUuid;
    private UUID leaderUuid;
    private final UUID ftbTeamId;
    private final ItemStack banner;
    private final long createdAt;
    private final int claimLimit;
    private final boolean serverOwned;
    private final Map<UUID, NationMember> members;
    private final List<NationHistoryEntry> history;
    private NationCurrency currency;

    public NationRecord(UUID nationId, String name, String abbreviation, UUID founderUuid,
                        UUID leaderUuid, UUID ftbTeamId, ItemStack banner, long createdAt,
                        int claimLimit, boolean serverOwned, Map<UUID, NationMember> members,
                        List<NationHistoryEntry> history, NationCurrency currency) {
        this.nationId = nationId;
        this.name = name;
        this.abbreviation = abbreviation;
        this.founderUuid = founderUuid;
        this.leaderUuid = leaderUuid;
        this.ftbTeamId = ftbTeamId;
        this.banner = banner.copy();
        this.createdAt = createdAt;
        this.claimLimit = claimLimit;
        this.serverOwned = serverOwned;
        this.members = new LinkedHashMap<>(members);
        this.history = new ArrayList<>(history);
        this.currency = currency;

        if (!serverOwned) {
            this.members.putIfAbsent(leaderUuid,
                    new NationMember(leaderUuid, NationRole.LEADER, createdAt));
            normalizeLeaderRole();
        } else {
            this.leaderUuid = SERVER_OWNER_UUID;
            this.members.remove(SERVER_OWNER_UUID);
            this.members.replaceAll((id, member) -> member.role() == NationRole.LEADER
                    ? new NationMember(id, NationRole.OFFICIAL, member.joinedAt())
                    : member);
        }
        trimHistory();
    }

    public static NationRecord create(UUID nationId, String name, String abbreviation,
                                      UUID founderUuid, UUID ftbTeamId, ItemStack banner,
                                      long createdAt, int claimLimit) {
        Map<UUID, NationMember> members = new LinkedHashMap<>();
        members.put(founderUuid, new NationMember(founderUuid, NationRole.LEADER, createdAt));
        return new NationRecord(
                nationId,
                name,
                abbreviation,
                founderUuid,
                founderUuid,
                ftbTeamId,
                banner,
                createdAt,
                claimLimit,
                false,
                members,
                List.of(),
                null
        );
    }

    public static NationRecord createServerOwned(UUID nationId, String name, String abbreviation,
                                                 UUID technicalFounderUuid, UUID ftbTeamId,
                                                 ItemStack banner, long createdAt, int claimLimit) {
        Map<UUID, NationMember> members = new LinkedHashMap<>();
        members.put(technicalFounderUuid,
                new NationMember(technicalFounderUuid, NationRole.CITIZEN, createdAt));
        return new NationRecord(
                nationId,
                name,
                abbreviation,
                technicalFounderUuid,
                SERVER_OWNER_UUID,
                ftbTeamId,
                banner,
                createdAt,
                claimLimit,
                true,
                members,
                List.of(),
                null
        );
    }

    public UUID nationId() { return nationId; }
    public String name() { return name; }
    public String abbreviation() { return abbreviation; }
    public UUID founderUuid() { return founderUuid; }
    public UUID leaderUuid() { return leaderUuid; }
    public UUID ftbTeamId() { return ftbTeamId; }
    public ItemStack banner() { return banner.copy(); }
    public long createdAt() { return createdAt; }
    public int claimLimit() { return claimLimit; }
    public boolean serverOwned() { return serverOwned; }

    public Optional<NationCurrency> currency() {
        return Optional.ofNullable(currency);
    }

    public boolean createCurrency(NationCurrency newCurrency) {
        if (currency != null || newCurrency == null) {
            return false;
        }
        currency = newCurrency;
        return true;
    }

    public Collection<NationMember> members() {
        return Collections.unmodifiableCollection(members.values());
    }

    public List<NationHistoryEntry> history() {
        return Collections.unmodifiableList(history);
    }

    public int memberCount() {
        return members.size();
    }

    public boolean isMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    public Optional<NationMember> member(UUID playerId) {
        return Optional.ofNullable(members.get(playerId));
    }

    public NationRole roleOf(UUID playerId) {
        NationMember member = members.get(playerId);
        return member == null ? NationRole.CITIZEN : member.role();
    }

    public boolean hasPermission(UUID playerId, NationPermission permission) {
        NationMember member = members.get(playerId);
        return member != null && member.role().has(permission);
    }

    public boolean addMember(UUID playerId, NationRole role, long joinedAt) {
        if (members.containsKey(playerId) || playerId.equals(SERVER_OWNER_UUID)) {
            return false;
        }
        NationRole safeRole = role == NationRole.LEADER ? NationRole.OFFICIAL : role;
        members.put(playerId, new NationMember(playerId, safeRole, joinedAt));
        return true;
    }

    public boolean removeMember(UUID playerId) {
        if (!serverOwned && leaderUuid.equals(playerId)) {
            return false;
        }
        return members.remove(playerId) != null;
    }

    public boolean setRole(UUID playerId, NationRole role) {
        NationMember current = members.get(playerId);
        if (current == null || (!serverOwned && leaderUuid.equals(playerId)) || role == NationRole.LEADER) {
            return false;
        }
        members.put(playerId, new NationMember(playerId, role, current.joinedAt()));
        return true;
    }

    public boolean transferLeadership(UUID newLeaderId) {
        if (serverOwned) {
            return false;
        }
        NationMember newLeader = members.get(newLeaderId);
        NationMember oldLeader = members.get(leaderUuid);
        if (newLeader == null || oldLeader == null || leaderUuid.equals(newLeaderId)) {
            return false;
        }

        UUID oldLeaderId = leaderUuid;
        members.put(oldLeaderId, new NationMember(oldLeaderId, NationRole.OFFICIAL, oldLeader.joinedAt()));
        members.put(newLeaderId, new NationMember(newLeaderId, NationRole.LEADER, newLeader.joinedAt()));
        leaderUuid = newLeaderId;
        return true;
    }

    public void addHistory(long timestamp, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        history.add(new NationHistoryEntry(timestamp, message.trim()));
        trimHistory();
    }

    private void trimHistory() {
        while (history.size() > MAX_HISTORY_ENTRIES) {
            history.remove(0);
        }
    }

    private void normalizeLeaderRole() {
        NationMember leader = members.get(leaderUuid);
        members.put(leaderUuid, new NationMember(leaderUuid, NationRole.LEADER, leader.joinedAt()));
        members.replaceAll((id, member) -> {
            if (!id.equals(leaderUuid) && member.role() == NationRole.LEADER) {
                return new NationMember(id, NationRole.OFFICIAL, member.joinedAt());
            }
            return member;
        });
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("NationId", nationId);
        tag.putString("Name", name);
        tag.putString("Abbreviation", abbreviation);
        tag.putUUID("Founder", founderUuid);
        tag.putUUID("Leader", leaderUuid);
        tag.putUUID("FtbTeam", ftbTeamId);
        tag.put("Banner", banner.save(new CompoundTag()));
        tag.putLong("CreatedAt", createdAt);
        tag.putInt("ClaimLimit", claimLimit);
        tag.putBoolean("ServerOwned", serverOwned);

        ListTag memberList = new ListTag();
        members.values().forEach(member -> memberList.add(member.save()));
        tag.put("Members", memberList);

        ListTag historyList = new ListTag();
        history.forEach(entry -> historyList.add(entry.save()));
        tag.put("History", historyList);
        if (currency != null) {
            tag.put("Currency", currency.save());
        }
        return tag;
    }

    public static NationRecord load(CompoundTag tag) {
        boolean serverOwned = tag.getBoolean("ServerOwned");
        UUID leader = serverOwned
                ? SERVER_OWNER_UUID
                : tag.getUUID("Leader");
        UUID founder = tag.hasUUID("Founder") ? tag.getUUID("Founder") : leader;
        long createdAt = tag.getLong("CreatedAt");

        Map<UUID, NationMember> members = new LinkedHashMap<>();
        if (tag.contains("Members", Tag.TAG_LIST)) {
            ListTag memberList = tag.getList("Members", Tag.TAG_COMPOUND);
            for (Tag raw : memberList) {
                NationMember member = NationMember.load((CompoundTag) raw);
                members.put(member.playerId(), member);
            }
        }

        if (members.isEmpty() && !serverOwned) {
            members.put(leader, new NationMember(leader, NationRole.LEADER, createdAt));
        }

        List<NationHistoryEntry> history = new ArrayList<>();
        if (tag.contains("History", Tag.TAG_LIST)) {
            ListTag historyList = tag.getList("History", Tag.TAG_COMPOUND);
            for (Tag raw : historyList) {
                history.add(NationHistoryEntry.load((CompoundTag) raw));
            }
        }

        NationCurrency currency = tag.contains("Currency", Tag.TAG_COMPOUND)
                ? NationCurrency.load(tag.getCompound("Currency"))
                : null;

        return new NationRecord(
                tag.getUUID("NationId"),
                tag.getString("Name"),
                tag.getString("Abbreviation"),
                founder,
                leader,
                tag.getUUID("FtbTeam"),
                ItemStack.of(tag.getCompound("Banner")),
                createdAt,
                tag.contains("ClaimLimit") ? tag.getInt("ClaimLimit") : 25,
                serverOwned,
                members,
                history,
                currency
        );
    }
}
