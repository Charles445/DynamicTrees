package com.ferreusveritas.dynamictrees.systems.genfeatures;

import com.ferreusveritas.dynamictrees.data.DTBlockTags;
import com.ferreusveritas.dynamictrees.systems.genfeatures.context.PostGenerationContext;
import com.ferreusveritas.dynamictrees.trees.Species;
import com.ferreusveritas.dynamictrees.util.CoordUtils;
import com.ferreusveritas.dynamictrees.util.SafeChunkBounds;
import com.ferreusveritas.dynamictrees.util.SimpleVoxmap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;

public class UndergrowthGenFeature extends GenFeature {

    public UndergrowthGenFeature(ResourceLocation registryName) {
        super(registryName);
    }

    @Override
    protected void registerProperties() {
    }

    @Override
    protected boolean postGenerate(GenFeatureConfiguration configuration, PostGenerationContext context) {
        final boolean worldGen = context.isWorldGen();
        final int radius = context.radius();

        if (!worldGen || radius <= 2) {
            return false;
        }

        final IWorld world = context.world();
        final BlockPos rootPos = context.pos();
        final SafeChunkBounds bounds = context.bounds();
        final Species species = context.species();

        final Vector3d vTree = new Vector3d(rootPos.getX(), rootPos.getY(), rootPos.getZ()).add(0.5, 0.5, 0.5);

        for (int i = 0; i < 2; i++) {

            int rad = MathHelper.clamp(world.getRandom().nextInt(radius - 2) + 2, 2, radius - 1);
            Vector3d v = vTree.add(new Vector3d(1, 0, 0).scale(rad).yRot((float) (world.getRandom().nextFloat() * Math.PI * 2)));
            BlockPos vPos = new BlockPos(v);

            if (!bounds.inBounds(vPos, true)) {
                continue;
            }

            final BlockPos groundPos = CoordUtils.findWorldSurface(world, vPos, true);
            final BlockState soilBlockState = world.getBlockState(groundPos);

            BlockPos pos = groundPos.above();
            if (species.isAcceptableSoil(world, groundPos, soilBlockState)) {
                final int type = world.getRandom().nextInt(2);
                world.setBlock(pos, (type == 0 ? Blocks.OAK_LOG : Blocks.JUNGLE_LOG).defaultBlockState(), 2);
                pos = pos.above(world.getRandom().nextInt(3));

                final BlockState leavesState = (type == 0 ? Blocks.OAK_LEAVES : Blocks.JUNGLE_LEAVES).defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);

                final SimpleVoxmap leafMap = species.getLeavesProperties().getCellKit().getLeafCluster();
                final BlockPos.Mutable leafPos = new BlockPos.Mutable();
                for (BlockPos.Mutable dPos : leafMap.getAllNonZero()) {
                    leafPos.set(pos.getX() + dPos.getX(), pos.getY() + dPos.getY(), pos.getZ() + dPos.getZ());

                    if (bounds.inBounds(leafPos, true) && (CoordUtils.coordHashCode(leafPos, 0) % 5) != 0 &&
                            (world.getBlockState(leafPos).canBeReplacedByLeaves(world, leafPos) ||
                                    world.getBlockState(leafPos).is(DTBlockTags.FOLIAGE))) {
                        world.setBlock(leafPos, leavesState, 2);
                    }
                }
            }
        }

        return true;
    }

}
