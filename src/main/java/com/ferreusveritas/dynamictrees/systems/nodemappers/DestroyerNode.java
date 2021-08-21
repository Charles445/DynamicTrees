package com.ferreusveritas.dynamictrees.systems.nodemappers;

import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.api.network.NodeInspector;
import com.ferreusveritas.dynamictrees.blocks.branches.BranchBlock;
import com.ferreusveritas.dynamictrees.systems.BranchConnectables;
import com.ferreusveritas.dynamictrees.trees.Species;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Destroys all branches on a tree and the surrounding leaves.
 *
 * @author ferreusveritas
 */
public class DestroyerNode implements NodeInspector {

    Species species;//Destroy any node that's made of the same kind of wood
    private final List<BlockPos> endPoints;//We always need to track endpoints during destruction
    private PlayerEntity player = null;

    public DestroyerNode(Species species) {
        this.endPoints = new ArrayList<>(32);
        this.species = species;
    }

    public DestroyerNode setPlayer(PlayerEntity player) {
        this.player = player;
        return this;
    }

    public List<BlockPos> getEnds() {
        return endPoints;
    }

    @Override
    public boolean run(BlockState blockState, IWorld world, BlockPos pos, @Nullable Direction fromDir) {
        if (BranchConnectables.getConnectionRadiusForBlock(blockState, world, pos, fromDir == null ? null : fromDir.getOpposite()) > 0) {
            if (player != null && world instanceof World) {
                TileEntity te = world.getBlockEntity(pos);
                blockState.getBlock().removedByPlayer(blockState, (World) world, pos, player, true, world.getFluidState(pos));
                blockState.getBlock().playerDestroy((World) world, player, pos, blockState, te, player.getMainHandItem());
            } else {
                world.removeBlock(pos, false);
            }
            return true;
        }

        BranchBlock branch = TreeHelper.getBranch(blockState);

        if (branch != null && species.getFamily() == branch.getFamily()) {
            boolean waterlogged = blockState.hasProperty(BlockStateProperties.WATERLOGGED) && blockState.getValue(BlockStateProperties.WATERLOGGED);
            if (branch.getRadius(blockState) == species.getFamily().getPrimaryThickness()) {
                endPoints.add(pos);
            }
            world.setBlock(pos, waterlogged ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);//Destroy the branch and notify the client
        }

        return true;
    }

    @Override
    public boolean returnRun(BlockState blockState, IWorld world, BlockPos pos, Direction fromDir) {
        return false;
    }
}
