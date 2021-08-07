package com.ferreusveritas.dynamictrees.blocks;

import com.ferreusveritas.dynamictrees.compat.seasons.SeasonHelper;
import com.ferreusveritas.dynamictrees.trees.Species;
import com.ferreusveritas.dynamictrees.util.BlockStates;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

import static com.ferreusveritas.dynamictrees.util.ShapeUtils.createFruitShape;

@SuppressWarnings({"deprecation", "unused"})
public class FruitBlock extends Block implements IGrowable {

    enum MatureFruitAction {
        NOTHING,
        DROP,
        ROT,
        CUSTOM
    }

    /**
     * Default shapes for the apple fruit, each element is the shape for each growth stage.
     */
    protected AxisAlignedBB[] FRUIT_AABB = new AxisAlignedBB[]{
            createFruitShape(1, 1, 0, 16),
            createFruitShape(1, 2, 0, 16),
            createFruitShape(2.5f, 5, 0),
            createFruitShape(2.5f, 5, 1.25f)
    };

    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    private static final Map<Species, Set<FruitBlock>> SPECIES_FRUIT_MAP = new HashMap<>();

    @Nonnull
    public static Set<FruitBlock> getFruitBlocksForSpecies(Species species) {
        return SPECIES_FRUIT_MAP.getOrDefault(species, new HashSet<>());
    }

    protected ItemStack droppedFruit = ItemStack.EMPTY;
    protected Supplier<Boolean> canBoneMeal = () -> false; // Q: Does dusting an apple with bone dust make it grow faster? A: Not by default.
    protected Vector3d itemSpawnOffset = new Vector3d(0.5, 0.6, 0.5);
    private Species species;

    public FruitBlock() {
        super(Properties.of(Material.PLANT)
                .randomTicks()
                .strength(0.3f));
    }

    public FruitBlock setCanBoneMeal(boolean canBoneMeal) {
        return this.setCanBoneMeal(() -> canBoneMeal);
    }

    public FruitBlock setCanBoneMeal(Supplier<Boolean> canBoneMeal) {
        this.canBoneMeal = canBoneMeal;
        return this;
    }

    public void setItemSpawnOffset(float x, float y, float z) {
        this.itemSpawnOffset = new Vector3d(Math.min(Math.max(x, 0), 1), Math.min(Math.max(y, 0), 1), Math.min(Math.max(z, 0), 1));
    }

    public void setSpecies(Species species) {
        if (SPECIES_FRUIT_MAP.containsKey(species)) {
            SPECIES_FRUIT_MAP.get(species).add(this);
        } else {
            Set<FruitBlock> set = new HashSet<>();
            set.add(this);
            SPECIES_FRUIT_MAP.put(species, set);
        }
        this.species = species;
    }

    public Species getSpecies() {
        return this.species == null ? Species.NULL_SPECIES : this.species;
    }

    public float getMinimumSeasonalValue() {
        return 0.3f;
    }

    @Override
    public void tick(BlockState state, ServerWorld world, BlockPos pos, Random rand) {
        this.doTick(state, world, pos, rand);
    }

    public void doTick(BlockState state, World world, BlockPos pos, Random rand) {
        if (this.shouldBlockDrop(world, pos, state)) {
            this.dropBlock(world, state, pos);
            return;
        }

        final int age = state.getValue(AGE);
        final Float season = SeasonHelper.getSeasonValue(world, pos);
        final Species species = this.getSpecies();

        if (season != null && species.isValid()) { // Non-Null means we are season capable.
            if (species.seasonalFruitProductionFactor(world, pos) < getMinimumSeasonalValue()) {
                this.outOfSeasonAction(world, pos); // Destroy the block or similar action.
                return;
            }
            if (age == 0 && species.testFlowerSeasonHold(season)) {
                return; // Keep fruit at the flower stage.
            }
        }

        if (age < 3) {
            final boolean doGrow = rand.nextFloat() < this.getGrowthChance(world, pos);
            final boolean eventGrow = ForgeHooks.onCropsGrowPre(world, pos, state, doGrow);
            if (season != null ? doGrow || eventGrow : eventGrow) { // Prevent a seasons mod from canceling the growth, we handle that ourselves.
                world.setBlock(pos, state.setValue(AGE, age + 1), 2);
                ForgeHooks.onCropsGrowPost(world, pos, state);
            }
        } else {
            if (age == 3) {
                switch (this.matureAction(world, pos, state, rand)) {
                    case NOTHING:
                    case CUSTOM:
                        break;
                    case DROP:
                        this.dropBlock(world, state, pos);
                        break;
                    case ROT:
                        world.setBlockAndUpdate(pos, BlockStates.AIR);
                        break;
                }
            }
        }
    }

    protected float getGrowthChance(World world, BlockPos blockPos) {
        return 0.2f;
    }

    /**
     * Override this to make the fruit do something once it's mature.
     *
     * @param world The world
     * @param pos   The position of the fruit block
     * @param state The current blockstate of the fruit
     * @param rand  A random number generator
     * @return MatureFruitAction action to take
     */
    protected MatureFruitAction matureAction(World world, BlockPos pos, BlockState state, Random rand) {
        return MatureFruitAction.NOTHING;
    }

    protected void outOfSeasonAction(World world, BlockPos pos) {
        world.setBlockAndUpdate(pos, BlockStates.AIR);
    }

    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos neighbor, boolean isMoving) {
        this.onNeighborChange(state, world, pos, neighbor);
    }

    @Override
    public void onNeighborChange(BlockState state, IWorldReader world, BlockPos pos, BlockPos neighbor) {
        if (this.shouldBlockDrop(world, pos, state)) {
            this.dropBlock((World) world, state, pos);
        }
    }

    @Override
    public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (state.getValue(AGE) >= 3) {
            this.dropBlock(worldIn, state, pos);
            return ActionResultType.SUCCESS;
        }

        return ActionResultType.FAIL;
    }

    protected void dropBlock(World worldIn, BlockState state, BlockPos pos) {
        worldIn.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        if (state.getValue(AGE) >= 3) {
            worldIn.addFreshEntity(new ItemEntity(worldIn, pos.getX() + itemSpawnOffset.x, pos.getY() + itemSpawnOffset.y, pos.getZ() + itemSpawnOffset.z, this.getFruitDrop(fruitDropCount(state, worldIn, pos))));
        }
    }

    @Override
    public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, PlayerEntity player) {
        return this.getFruitDrop(1);
    }

    /**
     * Checks if Leaves of any kind are above this block. Not picky.
     *
     * @param world
     * @param pos
     * @param state
     * @return True if it should drop (leaves are not above).
     */
    public boolean shouldBlockDrop(IBlockReader world, BlockPos pos, BlockState state) {
        return !(world.getBlockState(pos.above()).getBlock() instanceof LeavesBlock);
    }


    ///////////////////////////////////////////
    //BONEMEAL
    ///////////////////////////////////////////

    @Override
    public boolean isValidBonemealTarget(IBlockReader worldIn, BlockPos pos, BlockState state, boolean isClient) {
        return state.getValue(AGE) < 3;
    }

    @Override
    public boolean isBonemealSuccess(World world, Random rand, BlockPos pos, BlockState state) {
        return this.canBoneMeal.get();
    }

    @Override
    public void performBonemeal(ServerWorld world, Random rand, BlockPos pos, BlockState state) {
        final int age = state.getValue(AGE);
        final int newAge = MathHelper.clamp(age + 1, 0, 3);
        if (newAge != age) {
            world.setBlock(pos, state.setValue(AGE, newAge), 2);
        }
    }


    ///////////////////////////////////////////
    //DROPS
    ///////////////////////////////////////////


    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        // If a loot table has been added load those drops instead (until drop creators).
        if (builder.getLevel().getServer().getLootTables().getIds().contains(this.getLootTable())) {
            return super.getDrops(state, builder);
        }

        final List<ItemStack> drops = new ArrayList<>();

        if (state.getValue(AGE) >= 3) {
            final ItemStack toDrop = this.getFruitDrop(fruitDropCount(state, builder.getLevel(), BlockPos.ZERO));
            if (!toDrop.isEmpty()) {
                drops.add(toDrop);
            }
        }

        return drops;
    }

    public FruitBlock setDroppedItem(ItemStack stack) {
        this.droppedFruit = stack;
        return this;
    }

    //Override this for a custom item drop
    public ItemStack getFruitDrop(int count) {
        ItemStack stack = droppedFruit.copy();
        stack.setCount(count);
        return stack;
    }

    //pos could be BlockPos.ZERO
    protected int fruitDropCount(BlockState state, World world, BlockPos pos) {
        return 1;
    }

    ///////////////////////////////////////////
    // BOUNDARIES
    ///////////////////////////////////////////

    public FruitBlock setShape(int stage, AxisAlignedBB boundingBox) {
        FRUIT_AABB[stage] = boundingBox;
        return this;
    }

    public FruitBlock setShape(AxisAlignedBB[] boundingBox) {
        FRUIT_AABB = boundingBox;
        return this;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return VoxelShapes.create(FRUIT_AABB[state.getValue(AGE)]);
    }

    ///////////////////////////////////////////
    // BLOCKSTATE
    ///////////////////////////////////////////

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    public BlockState getStateForAge(int age) {
        return this.defaultBlockState().setValue(AGE, age);
    }

    public int getAgeForSeasonalWorldGen(IWorld world, BlockPos pos, @Nullable Float seasonValue) {
        if (seasonValue == null) {
            return 3;
        }

        if (this.getSpecies().testFlowerSeasonHold(seasonValue)) {
            return 0; // Fruit is as the flower stage.
        }

        return Math.min(world.getRandom().nextInt(6), 3); // Half the time the fruit is fully mature.
    }

}
