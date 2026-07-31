package com.austin.civicnations.item;

import com.austin.civicnations.data.NationRecord;
import com.austin.civicnations.data.NationSavedData;
import com.austin.civicnations.data.NationRole;
import com.austin.civicnations.integration.FTBIntegration;
import com.austin.civicnations.menu.NationCreationMenu;
import com.austin.civicnations.menu.NationOverviewMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.Optional;

public final class NationLedgerItem extends Item {
    public NationLedgerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(held);
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        NationSavedData data = NationSavedData.get(serverPlayer.server);
        Optional<NationRecord> nation = data.getNationForPlayer(serverPlayer.getUUID());

        if (nation.isEmpty()) {
            NetworkHooks.openScreen(
                    serverPlayer,
                    new SimpleMenuProvider(
                            (containerId, inventory, ignored) -> new NationCreationMenu(containerId, inventory),
                            Component.translatable("menu.civicnations.create_nation")
                    )
            );
        } else {
            NationRecord record = nation.get();
            int claimedChunks = FTBIntegration.getClaimedChunkCount(record);
            NationRole playerRole = record.roleOf(serverPlayer.getUUID());
            NetworkHooks.openScreen(
                    serverPlayer,
                    new SimpleMenuProvider(
                            (containerId, inventory, ignored) ->
                                    new NationOverviewMenu(containerId, inventory, record, playerRole, claimedChunks, serverPlayer),
                            Component.translatable("menu.civicnations.nation_overview")
                    ),
                    buffer -> NationOverviewMenu.writeOpenData(buffer, record, playerRole, claimedChunks, serverPlayer)
            );
        }

        return InteractionResultHolder.consume(held);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.civicnations.nation_ledger.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }
}
