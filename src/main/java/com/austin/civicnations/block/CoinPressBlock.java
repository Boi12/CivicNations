package com.austin.civicnations.block;

import com.austin.civicnations.block.entity.CoinPressBlockEntity;
import com.austin.civicnations.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/** A two-block-tall mechanical press used to manufacture blank coins. */
public final class CoinPressBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF =
            EnumProperty.create("half", DoubleBlockHalf.class);

    private static final VoxelShape LOWER_SHAPE = Shapes.or(
            box(0, 0, 0, 16, 5, 16),
            box(1, 5, 2, 4, 16, 14),
            box(12, 5, 2, 15, 16, 14),
            box(4, 5, 3, 12, 8, 13),
            box(5, 8, 4, 11, 10, 12)
    );
    private static final VoxelShape UPPER_SHAPE = Shapes.or(
            box(1, 0, 2, 4, 13, 14),
            box(12, 0, 2, 15, 13, 14),
            box(1, 13, 2, 15, 16, 14),
            box(5, 8, 4, 11, 13, 12),
            box(6, 0, 5, 10, 8, 11)
    );

    public CoinPressBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        if (pos.getY() >= context.getLevel().getMaxBuildHeight() - 1
                || !context.getLevel().getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
            level.blockUpdated(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, 3);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? new CoinPressBlockEntity(pos, state)
                : null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.COIN_PRESS.get(),
                CoinPressBlockEntity::serverTick);
    }

    private static boolean isMatchingPartner(BlockState state, BlockState neighborState) {
        return neighborState.getBlock() == state.getBlock()
                && neighborState.getValue(FACING) == state.getValue(FACING)
                && neighborState.getValue(HALF) != state.getValue(HALF);
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

        BlockPos lowerPos = lowerPos(state, pos);
        if (!isComplete(level, lowerPos)) {
            return InteractionResult.CONSUME;
        }
        BlockEntity blockEntity = level.getBlockEntity(lowerPos);
        if (!(blockEntity instanceof CoinPressBlockEntity coinPress)) {
            return InteractionResult.PASS;
        }
        NetworkHooks.openScreen(serverPlayer, coinPress,
                buffer -> buffer.writeBlockPos(lowerPos));
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean moving) {
        if (state.getBlock() != newState.getBlock()) {
            if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof CoinPressBlockEntity coinPress) {
                    Containers.dropContents(level, pos, coinPress);
                    level.updateNeighbourForOutputSignal(pos, this);
                }
            }

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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_SHAPE : UPPER_SHAPE;
    }

    public static BlockPos lowerPos(BlockState state, BlockPos pos) {
        if (!(state.getBlock() instanceof CoinPressBlock) || !state.hasProperty(HALF)) {
            return pos;
        }
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }

    public static BlockPos otherPos(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
    }

    public static boolean isComplete(Level level, BlockPos lowerPos) {
        BlockState lower = level.getBlockState(lowerPos);
        if (!(lower.getBlock() instanceof CoinPressBlock)
                || lower.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return false;
        }
        BlockState upper = level.getBlockState(lowerPos.above());
        return isMatchingPartner(lower, upper)
                && upper.getValue(HALF) == DoubleBlockHalf.UPPER;
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
        builder.add(FACING, HALF);
    }
}
