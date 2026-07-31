package com.austin.civicnations.block;

import com.austin.civicnations.data.NationCurrency;
import com.austin.civicnations.data.NationPermission;
import com.austin.civicnations.data.NationRecord;
import com.austin.civicnations.data.NationSavedData;
import com.austin.civicnations.integration.FTBIntegration;
import com.austin.civicnations.menu.CoinMintMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * A two-block-wide national Coin Mint. The placed block is the left/master half;
 * the right half is created automatically relative to the machine's facing.
 */
public final class CoinMintBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<MintPart> PART = EnumProperty.create("part", MintPart.class);

    public CoinMintBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, MintPart.LEFT));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos rightPos = context.getClickedPos().relative(facing.getClockWise());
        if (!context.getLevel().getWorldBorder().isWithinBounds(rightPos)
                || !context.getLevel().getBlockState(rightPos).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, facing).setValue(PART, MintPart.LEFT);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockPos rightPos = pos.relative(state.getValue(FACING).getClockWise());
            level.setBlock(rightPos, state.setValue(PART, MintPart.RIGHT), 3);
            level.blockUpdated(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, 3);
        }
    }

    private static boolean isMatchingPartner(BlockState state, BlockState neighborState) {
        return neighborState.getBlock() == state.getBlock()
                && neighborState.getValue(FACING) == state.getValue(FACING)
                && neighborState.getValue(PART) != state.getValue(PART);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean moving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockPos otherPos = otherPos(state, pos);
            BlockState otherState = level.getBlockState(otherPos);
            if (isMatchingPartner(state, otherState)) {
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(2001, otherPos, Block.getId(otherState));
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        BlockPos masterPos = masterPos(state, pos);
        BlockState masterState = level.getBlockState(masterPos);
        // Alpha 3 used a one-block Coin Mint. When an old placed mint is first
        // opened, repair it into the new two-wide structure if the right space is clear.
        if (!isComplete(level, masterPos)
                && masterState.getBlock() instanceof CoinMintBlock
                && masterState.getValue(PART) == MintPart.LEFT) {
            BlockPos legacyRightPos = masterPos.relative(
                    masterState.getValue(FACING).getClockWise());
            if (level.getBlockState(legacyRightPos).isAir()) {
                level.setBlock(legacyRightPos,
                        masterState.setValue(PART, MintPart.RIGHT), 3);
            }
        }
        if (!isComplete(level, masterPos)) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "The Coin Mint is incomplete. Clear the space beside it or replace the machine.")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.CONSUME;
        }
        masterState = level.getBlockState(masterPos);

        NationSavedData data = NationSavedData.get(serverPlayer.server);
        Optional<NationRecord> nationOptional = data.getNationForPlayer(serverPlayer.getUUID());
        if (nationOptional.isEmpty()) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "You must belong to a nation to use a Coin Mint.").withStyle(ChatFormatting.RED));
            return InteractionResult.CONSUME;
        }

        NationRecord nation = nationOptional.get();
        NationCurrency currency = nation.currency().orElse(null);
        if (currency == null) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "Your nation must establish a currency before using the Coin Mint.")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.CONSUME;
        }

        boolean serverAdmin = nation.serverOwned() && serverPlayer.hasPermissions(2);
        if (!serverAdmin && !nation.hasPermission(serverPlayer.getUUID(), NationPermission.MINT_CURRENCY)) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "Your role cannot operate the Coin Mint.").withStyle(ChatFormatting.RED));
            return InteractionResult.CONSUME;
        }

        BlockPos rightPos = otherPos(masterState, masterPos);
        if (!isClaimedByNation(serverPlayer, masterPos, nation)
                || !isClaimedByNation(serverPlayer, rightPos, nation)) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "Both halves of a Coin Mint must be inside territory claimed by its nation.")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.CONSUME;
        }

        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, ignored) -> CoinMintMenu.serverMenu(
                        containerId, inventory, masterPos, currency),
                Component.translatable("menu.civicnations.coin_mint")
        );
        NetworkHooks.openScreen(serverPlayer, provider,
                buffer -> CoinMintMenu.writeOpenData(buffer, masterPos, currency));
        return InteractionResult.CONSUME;
    }

    private static boolean isClaimedByNation(ServerPlayer player, BlockPos pos,
                                             NationRecord nation) {
        Optional<NationRecord> owner = FTBIntegration.getNationAt(player.serverLevel(), pos);
        return owner.isPresent() && owner.get().nationId().equals(nation.nationId());
    }

    public static BlockPos masterPos(BlockState state, BlockPos pos) {
        if (!(state.getBlock() instanceof CoinMintBlock) || !state.hasProperty(PART)) {
            return pos;
        }
        return state.getValue(PART) == MintPart.LEFT
                ? pos
                : pos.relative(state.getValue(FACING).getCounterClockWise());
    }

    public static BlockPos otherPos(BlockState state, BlockPos pos) {
        return state.getValue(PART) == MintPart.LEFT
                ? pos.relative(state.getValue(FACING).getClockWise())
                : pos.relative(state.getValue(FACING).getCounterClockWise());
    }

    public static boolean isComplete(Level level, BlockPos masterPos) {
        BlockState left = level.getBlockState(masterPos);
        if (!(left.getBlock() instanceof CoinMintBlock)
                || left.getValue(PART) != MintPart.LEFT) {
            return false;
        }
        BlockState right = level.getBlockState(
                masterPos.relative(left.getValue(FACING).getClockWise()));
        return isMatchingPartner(left, right) && right.getValue(PART) == MintPart.RIGHT;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    public enum MintPart implements StringRepresentable {
        LEFT("left"),
        RIGHT("right");

        private final String serializedName;

        MintPart(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
