package com.austin.civicnations.event;

import com.austin.civicnations.CivicNations;
import com.austin.civicnations.data.NationRecord;
import com.austin.civicnations.integration.FTBIntegration;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = CivicNations.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TerritoryNotifier {
    private static final Map<UUID, TerritoryState> LAST_STATE = new HashMap<>();

    private TerritoryNotifier() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 10 != 0) {
            return;
        }

        ChunkDimPos currentChunk = new ChunkDimPos(player);
        TerritoryState oldState = LAST_STATE.get(player.getUUID());
        if (oldState != null && oldState.chunk().equals(currentChunk)) {
            return;
        }

        Optional<NationRecord> owner = FTBIntegration.getNationAt(
                player.serverLevel(),
                player.blockPosition()
        );
        UUID nationId = owner.map(NationRecord::nationId).orElse(null);
        LAST_STATE.put(player.getUUID(), new TerritoryState(currentChunk, nationId));

        UUID previousNationId = oldState == null ? null : oldState.nationId();
        if (nationId != null && !nationId.equals(previousNationId)) {
            NationRecord nation = owner.orElseThrow();
            player.displayClientMessage(Component.literal(
                    "Entering " + nation.name() + " Territory [" + nation.abbreviation() + "]"
            ).withStyle(ChatFormatting.GOLD), true);
        } else if (nationId == null && previousNationId != null) {
            player.displayClientMessage(Component.literal("Entering Wilderness")
                    .withStyle(ChatFormatting.GRAY), true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_STATE.remove(event.getEntity().getUUID());
    }

    private record TerritoryState(ChunkDimPos chunk, UUID nationId) {}
}
