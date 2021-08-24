package com.ferreusveritas.dynamictrees.blocks.branches;

import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.trees.Family;
import com.ferreusveritas.dynamictrees.util.CoordUtils;
import com.ferreusveritas.dynamictrees.util.RootConnections;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings("deprecation")
public class SurfaceRootBlock extends Block implements IWaterLoggable {

    public static final int MAX_RADIUS = 8;

    protected static final IntegerProperty RADIUS = IntegerProperty.create("radius", 1, MAX_RADIUS);
    public static final BooleanProperty GROUNDED = BooleanProperty.create("grounded");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final Family family;

    public SurfaceRootBlock(Family family) {
        this(Material.WOOD, family);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    public SurfaceRootBlock(Material material, Family family) {
        super(Block.Properties.of(material)
                .harvestTool(ToolType.AXE)
                .harvestLevel(0)
                .strength(2.5f, 1.0F)
                .sound(SoundType.WOOD));

        this.family = family;
    }

    public Family getFamily() {
        return family;
    }

    public static class RootConnection {
        public RootConnections.ConnectionLevel level;
        public int radius;

        public RootConnection(RootConnections.ConnectionLevel level, int radius) {
            this.level = level;
            this.radius = radius;
        }

        @Override
        public String toString() {
            return super.toString() + " Level: " + this.level.toString() + " Radius: " + this.radius;
        }
    }

    @Override
    public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, PlayerEntity player) {
        return new ItemStack(this.family.getBranchItem());
    }

    ///////////////////////////////////////////
    // BLOCK STATES
    ///////////////////////////////////////////

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(RADIUS, GROUNDED, WATERLOGGED);
    }

    public int getRadius(BlockState blockState) {
        return blockState.getBlock() == this ? blockState.getValue(RADIUS) : 0;
    }

    public int setRadius(IWorld world, BlockPos pos, int radius, int flags) {
        boolean replacingWater = world.getBlockState(pos).getFluidState() == Fluids.WATER.getSource(false);
        world.setBlock(pos, this.getStateForRadius(radius).setValue(WATERLOGGED, replacingWater), flags);
        return radius;
    }

    public BlockState getStateForRadius(int radius) {
        return this.defaultBlockState().setValue(RADIUS, MathHelper.clamp(radius, 0, getMaxRadius()));
    }

    public int getMaxRadius() {
        return MAX_RADIUS;
    }

    public int getRadialHeight(int radius) {
        return radius * 2;
    }

    ///////////////////////////////////////////
    // WATER LOGGING
    ///////////////////////////////////////////

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (stateIn.getValue(WATERLOGGED)) {
            worldIn.getLiquidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(worldIn));
        }
        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    ///////////////////////////////////////////
    // RENDERING
    ///////////////////////////////////////////

    public RootConnections getConnectionData(final IBlockDisplayReader world, final BlockPos pos) {
        final RootConnections connections = new RootConnections();

        for (Direction dir : CoordUtils.HORIZONTALS) {
            final RootConnection connection = this.getSideConnectionRadius(world, pos, dir);

            if (connection == null) {
                continue;
            }

            connections.setRadius(dir, connection.radius);
            connections.setConnectionLevel(dir, connection.level);
        }

        return connections;
    }


    ///////////////////////////////////////////
    // PHYSICAL BOUNDS
    ///////////////////////////////////////////

    @Nonnull
    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        boolean connectionMade = false;
        final int thisRadius = getRadius(state);

        VoxelShape shape = VoxelShapes.empty();

        for (Direction dir : CoordUtils.HORIZONTALS) {
            final RootConnection conn = this.getSideConnectionRadius(world, pos, dir);

            if (conn == null) {
                continue;
            }

            connectionMade = true;
            final int r = MathHelper.clamp(conn.radius, 1, thisRadius);
            final double radius = r / 16.0;
            final double radialHeight = getRadialHeight(r) / 16.0;
            final double gap = 0.5 - radius;

            AxisAlignedBB aabb = new AxisAlignedBB(-radius, 0, -radius, radius, radialHeight, radius);
            aabb = aabb.expandTowards(dir.getStepX() * gap, 0, dir.getStepZ() * gap).move(0.5, 0.0, 0.5);
            shape = VoxelShapes.joinUnoptimized(shape, VoxelShapes.create(aabb), IBooleanFunction.OR);
        }

        if (!connectionMade) {
            double radius = thisRadius / 16.0;
            double radialHeight = getRadialHeight(thisRadius) / 16.0;
            AxisAlignedBB aabb = new AxisAlignedBB(0.5 - radius, 0, 0.5 - radius, 0.5 + radius, radialHeight, 0.5 + radius);
            shape = VoxelShapes.joinUnoptimized(shape, VoxelShapes.create(aabb), IBooleanFunction.OR);
        }

        return shape;
    }

    @Nullable
    protected RootConnection getSideConnectionRadius(IBlockReader blockReader, BlockPos pos, Direction side) {
        if (!side.getAxis().isHorizontal()) {
            return null;
        }

        BlockPos dPos = pos.relative(side);
        BlockState state = CoordUtils.getStateSafe(blockReader, dPos);
        final BlockState upState = CoordUtils.getStateSafe(blockReader, pos.above());

        final RootConnections.ConnectionLevel level = (upState != null && upState.getBlock() == Blocks.AIR && state != null && state.isRedstoneConductor(blockReader, dPos)) ?
                RootConnections.ConnectionLevel.HIGH : (state != null && state.getBlock() == Blocks.AIR ? RootConnections.ConnectionLevel.LOW : RootConnections.ConnectionLevel.MID);

        if (level != RootConnections.ConnectionLevel.MID) {
            dPos = dPos.above(level.getYOffset());
            state = CoordUtils.getStateSafe(blockReader, dPos);
        }

        if (state != null && state.getBlock() instanceof SurfaceRootBlock) {
            return new RootConnection(level, ((SurfaceRootBlock) state.getBlock()).getRadius(state));
        } else if (level == RootConnections.ConnectionLevel.MID && TreeHelper.isBranch(state) && TreeHelper.getTreePart(state).getRadius(state) >= 8) {
            return new RootConnection(RootConnections.ConnectionLevel.MID, 8);
        }

        return null;
    }

    @Override
    public boolean removedByPlayer(BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, FluidState fluid) {
        final BlockState upstate = world.getBlockState(pos.above());

        if (upstate.getBlock() instanceof TrunkShellBlock) {
            world.setBlockAndUpdate(pos, upstate);
        }

        for (Direction dir : CoordUtils.HORIZONTALS) {
            final BlockPos dPos = pos.relative(dir).below();
            world.getBlockState(dPos).neighborChanged(world, dPos, this, pos, false);
        }

        return super.removedByPlayer(state, world, pos, player, willHarvest, fluid);
    }

    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        if (!canBlockStay(world, pos, state)) {
            world.removeBlock(pos, false);
        }
    }

    protected boolean canBlockStay(World world, BlockPos pos, BlockState state) {
        final BlockPos below = pos.below();
        final BlockState belowState = world.getBlockState(below);

        final int radius = getRadius(state);

        if (belowState.isRedstoneConductor(world, below)) { // If a root is sitting on a solid block.
            for (Direction dir : CoordUtils.HORIZONTALS) {
                final RootConnection conn = this.getSideConnectionRadius(world, pos, dir);

                if (conn != null && conn.radius > radius) {
                    return true;
                }
            }
        } else { // If the root has no solid block under it.
            boolean connections = false;

            for (Direction dir : CoordUtils.HORIZONTALS) {
                final RootConnection conn = this.getSideConnectionRadius(world, pos, dir);

                if (conn == null) {
                    continue;
                }

                if (conn.level == RootConnections.ConnectionLevel.MID) {
                    return false;
                }

                if (conn.radius > radius) {
                    connections = true;
                }
            }

            return connections;
        }

        return false;
    }

}
