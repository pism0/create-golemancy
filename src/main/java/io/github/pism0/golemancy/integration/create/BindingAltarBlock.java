package io.github.pism0.golemancy.integration.create;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import io.github.pism0.golemancy.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Binding Altar (SDD 8.2): a Create kinetic block powered by a shaft from below. It is the one
 * point where frame, core and inscription meet and the golem's spec is built.
 *
 * <p>Like other Create machines it has no container screen: items are placed and taken by hand,
 * funnel, chute, deployer or mechanical arm, and its state is read from the hover/goggle tooltip.
 */
public class BindingAltarBlock extends KineticBlock implements IBE<BindingAltarBlockEntity> {
    /** True while a binding is in progress: switches to the lit model and emits light. */
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 10, 16),   // casing
            Block.box(1, 10, 1, 15, 13, 15),  // plinth
            Block.box(0, 13, 0, 16, 16, 16)); // rune dish

    public BindingAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WORKING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(WORKING));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        return onBlockEntityUseItemOn(level, pos, altar -> {
            ItemStack remainder = altar.insert(stack, false);
            if (remainder.getCount() == stack.getCount()) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, remainder);
            }
            return ItemInteractionResult.SUCCESS;
        });
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        return onBlockEntityUse(level, pos, altar -> {
            // Empty hand: take the finished golem; if there is none, take the inputs back.
            for (ItemStack taken : altar.takeForPlayer()) {
                player.getInventory().placeItemBackInInventory(taken);
            }
            return InteractionResult.SUCCESS;
        });
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(WORKING) ? 10 : 0;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        // 15 when a finished golem is waiting, otherwise progress 0..14: lets a line throttle itself.
        BindingAltarBlockEntity altar = getBlockEntity(level, pos);
        return altar == null ? 0 : altar.comparatorSignal();
    }

    @Override
    public Class<BindingAltarBlockEntity> getBlockEntityClass() {
        return BindingAltarBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BindingAltarBlockEntity> getBlockEntityType() {
        return ModBlockEntities.BINDING_ALTAR.get();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
