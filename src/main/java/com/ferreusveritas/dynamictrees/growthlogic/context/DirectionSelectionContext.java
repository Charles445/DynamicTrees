package com.ferreusveritas.dynamictrees.growthlogic.context;

import com.ferreusveritas.dynamictrees.blocks.branches.BranchBlock;
import com.ferreusveritas.dynamictrees.systems.GrowSignal;
import com.ferreusveritas.dynamictrees.trees.Species;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * @author Harley O'Connor
 */
public class DirectionSelectionContext extends PositionalSpeciesContext {

    private final BranchBlock branch;
    private final GrowSignal signal;

    public DirectionSelectionContext(World world, BlockPos pos, Species species, BranchBlock branch, GrowSignal signal) {
        super(world, pos, species);
        this.branch = branch;
        this.signal = signal;
    }

    public BranchBlock branch() {
        return branch;
    }

    public GrowSignal signal() {
        return signal;
    }

}
