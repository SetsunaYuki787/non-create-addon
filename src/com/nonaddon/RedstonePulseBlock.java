package com.nonaddon;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A block that does nothing but emit full redstone power in every direction and then removes
 * itself after {@link NoNAddon#PULSE_TICKS} ticks. It is invisible, has no collision and drops
 * nothing, so it can be placed temporarily anywhere and is never obtainable in survival.
 */
public class RedstonePulseBlock extends Block {
    public RedstonePulseBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return 15;
    }

    @Override
    protected int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return 15;
    }

    @Override
    protected boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.scheduleBlockTick(pos, this, NoNAddon.PULSE_TICKS);
        }
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Only remove ourselves: if something replaced the pulse in the meantime (a player placing
        // a block into the replaceable air slot) that block must survive.
        if (state.isOf(this)) {
            world.removeBlock(pos, false);
        }
    }
}
