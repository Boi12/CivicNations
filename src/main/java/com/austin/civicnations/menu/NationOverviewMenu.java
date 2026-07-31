package com.austin.civicnations.menu;

import com.austin.civicnations.data.CurrencyTransaction;
import com.austin.civicnations.data.NationCurrency;
import com.austin.civicnations.data.NationHistoryEntry;
import com.austin.civicnations.data.NationMember;
import com.austin.civicnations.data.NationPermission;
import com.austin.civicnations.data.NationRecord;
import com.austin.civicnations.data.NationRole;
import com.austin.civicnations.registry.ModMenus;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class NationOverviewMenu extends AbstractContainerMenu {
    private final UUID nationId;
    private final String nationName;
    private final String abbreviation;
    private final UUID founderId;
    private final UUID leaderId;
    private final ItemStack banner;
    private final int memberCount;
    private final NationRole playerRole;
    private final int claimedChunks;
    private final int claimLimit;
    private final boolean serverOwned;
    private final boolean canAssignRoles;
    private final boolean canRemoveMembers;
    private final boolean canTransferLeadership;
    private final boolean currencyConfigured;
    private final String currencyName;
    private final String currencyCode;
    private final String currencySymbol;
    private final List<Integer> currencyDenominations;
    private final long treasuryBalance;
    private final long totalIssued;
    private final long viewerBalance;
    private final List<CurrencyView> currencyTransactions;
    private final List<MemberView> members;
    private final List<HistoryView> history;

    public NationOverviewMenu(int containerId, Inventory inventory, NationRecord nation,
                              NationRole playerRole, int claimedChunks, ServerPlayer viewer) {
        super(ModMenus.NATION_OVERVIEW.get(), containerId);
        this.nationId = nation.nationId();
        this.nationName = nation.name();
        this.abbreviation = nation.abbreviation();
        this.founderId = nation.founderUuid();
        this.leaderId = nation.leaderUuid();
        this.banner = nation.banner();
        this.memberCount = nation.memberCount();
        this.playerRole = playerRole;
        this.claimedChunks = claimedChunks;
        this.claimLimit = nation.claimLimit();
        this.serverOwned = nation.serverOwned();
        boolean serverAdmin = nation.serverOwned() && viewer.hasPermissions(2);
        this.canAssignRoles = serverAdmin || nation.hasPermission(viewer.getUUID(), NationPermission.ASSIGN_ROLES);
        this.canRemoveMembers = serverAdmin || nation.hasPermission(viewer.getUUID(), NationPermission.REMOVE_MEMBERS);
        this.canTransferLeadership = !nation.serverOwned()
                && nation.hasPermission(viewer.getUUID(), NationPermission.TRANSFER_LEADERSHIP);
        NationCurrency currency = nation.currency().orElse(null);
        this.currencyConfigured = currency != null;
        this.currencyName = currency == null ? "" : currency.name();
        this.currencyCode = currency == null ? "" : currency.code();
        this.currencySymbol = currency == null ? "" : currency.symbol();
        this.currencyDenominations = currency == null ? List.of() : List.copyOf(currency.denominations());
        this.treasuryBalance = currency == null ? 0L : currency.treasuryBalance();
        this.totalIssued = currency == null ? 0L : currency.totalIssued();
        this.viewerBalance = currency == null ? 0L : currency.accountBalance(viewer.getUUID());
        this.currencyTransactions = currency == null ? List.of() : currency.transactions().stream()
                .sorted(Comparator.comparingLong(CurrencyTransaction::timestamp).reversed())
                .limit(50)
                .map(entry -> new CurrencyView(entry.timestamp(), entry.type(), entry.actor(),
                        entry.amount(), entry.note()))
                .toList();
        this.members = buildMembers(nation, viewer);
        this.history = nation.history().stream()
                .sorted(Comparator.comparingLong(NationHistoryEntry::timestamp).reversed())
                .limit(100)
                .map(entry -> new HistoryView(entry.timestamp(), entry.message()))
                .toList();
    }

    private NationOverviewMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.NATION_OVERVIEW.get(), containerId);
        this.nationId = buffer.readUUID();
        this.nationName = buffer.readUtf(24);
        this.abbreviation = buffer.readUtf(5);
        this.founderId = buffer.readUUID();
        this.leaderId = buffer.readUUID();
        this.banner = buffer.readItem();
        this.memberCount = buffer.readVarInt();
        this.playerRole = NationRole.parse(buffer.readUtf(16)).orElse(NationRole.CITIZEN);
        this.claimedChunks = buffer.readVarInt();
        this.claimLimit = buffer.readVarInt();
        this.serverOwned = buffer.readBoolean();
        this.canAssignRoles = buffer.readBoolean();
        this.canRemoveMembers = buffer.readBoolean();
        this.canTransferLeadership = buffer.readBoolean();
        this.currencyConfigured = buffer.readBoolean();
        this.currencyName = buffer.readUtf(24);
        this.currencyCode = buffer.readUtf(5);
        this.currencySymbol = buffer.readUtf(3);
        int denominationSize = Math.min(buffer.readVarInt(), 16);
        List<Integer> denominationViews = new ArrayList<>();
        for (int index = 0; index < denominationSize; index++) {
            denominationViews.add(buffer.readVarInt());
        }
        this.currencyDenominations = List.copyOf(denominationViews);
        this.treasuryBalance = buffer.readLong();
        this.totalIssued = buffer.readLong();
        this.viewerBalance = buffer.readLong();
        int transactionSize = Math.min(buffer.readVarInt(), 50);
        List<CurrencyView> transactionViews = new ArrayList<>();
        for (int index = 0; index < transactionSize; index++) {
            transactionViews.add(new CurrencyView(
                    buffer.readLong(),
                    buffer.readUtf(24),
                    buffer.readUtf(40),
                    buffer.readLong(),
                    buffer.readUtf(100)
            ));
        }
        this.currencyTransactions = List.copyOf(transactionViews);

        int memberSize = Math.min(buffer.readVarInt(), 100);
        List<MemberView> memberViews = new ArrayList<>();
        for (int index = 0; index < memberSize; index++) {
            memberViews.add(new MemberView(
                    buffer.readUUID(),
                    buffer.readUtf(40),
                    NationRole.parse(buffer.readUtf(16)).orElse(NationRole.CITIZEN)
            ));
        }
        this.members = List.copyOf(memberViews);

        int historySize = Math.min(buffer.readVarInt(), 100);
        List<HistoryView> historyViews = new ArrayList<>();
        for (int index = 0; index < historySize; index++) {
            historyViews.add(new HistoryView(buffer.readLong(), buffer.readUtf(180)));
        }
        this.history = List.copyOf(historyViews);
    }

    public static NationOverviewMenu fromNetwork(int containerId, Inventory inventory,
                                                  FriendlyByteBuf buffer) {
        return new NationOverviewMenu(containerId, inventory, buffer);
    }

    public static void writeOpenData(FriendlyByteBuf buffer, NationRecord nation,
                                     NationRole playerRole, int claimedChunks,
                                     ServerPlayer viewer) {
        buffer.writeUUID(nation.nationId());
        buffer.writeUtf(nation.name(), 24);
        buffer.writeUtf(nation.abbreviation(), 5);
        buffer.writeUUID(nation.founderUuid());
        buffer.writeUUID(nation.leaderUuid());
        buffer.writeItem(nation.banner());
        buffer.writeVarInt(nation.memberCount());
        buffer.writeUtf(playerRole.serializedName(), 16);
        buffer.writeVarInt(claimedChunks);
        buffer.writeVarInt(nation.claimLimit());
        buffer.writeBoolean(nation.serverOwned());
        boolean serverAdmin = nation.serverOwned() && viewer.hasPermissions(2);
        buffer.writeBoolean(serverAdmin || nation.hasPermission(viewer.getUUID(), NationPermission.ASSIGN_ROLES));
        buffer.writeBoolean(serverAdmin || nation.hasPermission(viewer.getUUID(), NationPermission.REMOVE_MEMBERS));
        buffer.writeBoolean(!nation.serverOwned()
                && nation.hasPermission(viewer.getUUID(), NationPermission.TRANSFER_LEADERSHIP));

        NationCurrency currency = nation.currency().orElse(null);
        buffer.writeBoolean(currency != null);
        buffer.writeUtf(currency == null ? "" : currency.name(), 24);
        buffer.writeUtf(currency == null ? "" : currency.code(), 5);
        buffer.writeUtf(currency == null ? "" : currency.symbol(), 3);
        List<Integer> denominations = currency == null ? List.of() : currency.denominations();
        buffer.writeVarInt(denominations.size());
        denominations.forEach(buffer::writeVarInt);
        buffer.writeLong(currency == null ? 0L : currency.treasuryBalance());
        buffer.writeLong(currency == null ? 0L : currency.totalIssued());
        buffer.writeLong(currency == null ? 0L : currency.accountBalance(viewer.getUUID()));
        List<CurrencyTransaction> transactions = currency == null ? List.of()
                : currency.transactions().stream()
                        .sorted(Comparator.comparingLong(CurrencyTransaction::timestamp).reversed())
                        .limit(50)
                        .toList();
        buffer.writeVarInt(transactions.size());
        for (CurrencyTransaction entry : transactions) {
            buffer.writeLong(entry.timestamp());
            buffer.writeUtf(entry.type(), 24);
            buffer.writeUtf(entry.actor(), 40);
            buffer.writeLong(entry.amount());
            buffer.writeUtf(entry.note(), 100);
        }

        List<MemberView> members = buildMembers(nation, viewer);
        buffer.writeVarInt(members.size());
        for (MemberView member : members) {
            buffer.writeUUID(member.playerId());
            buffer.writeUtf(member.name(), 40);
            buffer.writeUtf(member.role().serializedName(), 16);
        }

        List<NationHistoryEntry> history = nation.history().stream()
                .sorted(Comparator.comparingLong(NationHistoryEntry::timestamp).reversed())
                .limit(100)
                .toList();
        buffer.writeVarInt(history.size());
        for (NationHistoryEntry entry : history) {
            buffer.writeLong(entry.timestamp());
            buffer.writeUtf(entry.message(), 180);
        }
    }

    private static List<MemberView> buildMembers(NationRecord nation, ServerPlayer viewer) {
        return nation.members().stream()
                .sorted(Comparator.comparingInt((NationMember member) -> member.role().authorityLevel())
                        .reversed()
                        .thenComparing(member -> playerName(viewer, member.playerId()),
                                String.CASE_INSENSITIVE_ORDER))
                .map(member -> new MemberView(
                        member.playerId(),
                        playerName(viewer, member.playerId()),
                        member.role()
                ))
                .toList();
    }

    private static String playerName(ServerPlayer viewer, UUID playerId) {
        ServerPlayer online = viewer.server.getPlayerList().getPlayer(playerId);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        return viewer.server.getProfileCache()
                .get(playerId)
                .map(GameProfile::getName)
                .orElse(playerId.toString());
    }

    public UUID nationId() { return nationId; }
    public String nationName() { return nationName; }
    public String abbreviation() { return abbreviation; }
    public UUID founderId() { return founderId; }
    public UUID leaderId() { return leaderId; }
    public ItemStack banner() { return banner.copy(); }
    public int memberCount() { return memberCount; }
    public NationRole playerRole() { return playerRole; }
    public int claimedChunks() { return claimedChunks; }
    public int claimLimit() { return claimLimit; }
    public boolean serverOwned() { return serverOwned; }
    public boolean canAssignRoles() { return canAssignRoles; }
    public boolean canRemoveMembers() { return canRemoveMembers; }
    public boolean canTransferLeadership() { return canTransferLeadership; }
    public boolean currencyConfigured() { return currencyConfigured; }
    public String currencyName() { return currencyName; }
    public String currencyCode() { return currencyCode; }
    public String currencySymbol() { return currencySymbol; }
    public List<Integer> currencyDenominations() { return currencyDenominations; }
    public long treasuryBalance() { return treasuryBalance; }
    public long totalIssued() { return totalIssued; }
    public long viewerBalance() { return viewerBalance; }
    public List<CurrencyView> currencyTransactions() { return currencyTransactions; }
    public List<MemberView> members() { return members; }
    public List<HistoryView> history() { return history; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public record MemberView(UUID playerId, String name, NationRole role) {
    }

    public record HistoryView(long timestamp, String message) {
    }

    public record CurrencyView(long timestamp, String type, String actor, long amount, String note) {
    }
}
