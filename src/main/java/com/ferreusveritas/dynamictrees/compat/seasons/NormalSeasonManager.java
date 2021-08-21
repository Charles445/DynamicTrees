package com.ferreusveritas.dynamictrees.compat.seasons;

import com.ferreusveritas.dynamictrees.api.seasons.ClimateZoneType;
import com.ferreusveritas.dynamictrees.api.seasons.SeasonGrowthCalculator;
import com.ferreusveritas.dynamictrees.api.seasons.SeasonManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

public class NormalSeasonManager implements SeasonManager {

	public static final Supplier<NormalSeasonManager> NULL = NormalSeasonManager::new;

    private final Map<ResourceLocation, SeasonContext> seasonContextMap = new HashMap<>();
    private Function<World, Tuple<SeasonProvider, SeasonGrowthCalculator>> seasonMapper = w -> new Tuple<>(new NullSeasonProvider(), new NullSeasonGrowthCalculator());

    public NormalSeasonManager() {
    }

    public NormalSeasonManager(Function<World, Tuple<SeasonProvider, SeasonGrowthCalculator>> seasonMapper) {
        this.seasonMapper = seasonMapper;
    }

    private Tuple<SeasonProvider, SeasonGrowthCalculator> createProvider(World world) {
        return seasonMapper.apply(world);
    }

    private SeasonContext getContext(World world) {
        return seasonContextMap.computeIfAbsent(world.dimension().location(), d -> {
            Tuple<SeasonProvider, SeasonGrowthCalculator> tuple = createProvider(world);
            return new SeasonContext(tuple.getA(), tuple.getB());
        });
    }

    public void flushMappings() {
        seasonContextMap.clear();
    }


    ////////////////////////////////////////////////////////////////
    // Tropical Predicate
    ////////////////////////////////////////////////////////////////

    static private final float TROPICAL_THRESHHOLD = 0.8f; //Same threshold used by Serene Seasons.  Seems smart enough

    private BiPredicate<World, BlockPos> isTropical = (world, rootPos) -> world.getUncachedNoiseBiome(rootPos.getX() >> 2, rootPos.getY() >> 2, rootPos.getZ() >> 2).getBaseTemperature() > TROPICAL_THRESHHOLD;

    /**
     * Set the global predicate that determines if a world location is tropical. Predicate should return true if
     * tropical, false if temperate.
     */
    public void setTropicalPredicate(BiPredicate<World, BlockPos> predicate) {
        isTropical = predicate;
    }

    public boolean isTropical(World world, BlockPos rootPos) {
        return isTropical.test(world, rootPos);
    }


    ////////////////////////////////////////////////////////////////
    // ISeasonManager Interface
    ////////////////////////////////////////////////////////////////

    public void updateTick(World world, long worldTicks) {
        getContext(world).updateTick(world, worldTicks);
    }

    public float getGrowthFactor(World world, BlockPos rootPos, float offset) {
        SeasonContext context = getContext(world);
        return isTropical(world, rootPos) ? context.getTropicalGrowthFactor(offset) : context.getTemperateGrowthFactor(offset);
    }

    public float getSeedDropFactor(World world, BlockPos rootPos, float offset) {
        SeasonContext context = getContext(world);
        return isTropical(world, rootPos) ? context.getTropicalSeedDropFactor(offset) : context.getTemperateSeedDropFactor(offset);
    }

    @Override
    public float getFruitProductionFactor(World world, BlockPos rootPos, float offset, boolean getAsScan) {
        if (getAsScan) {
            return getFruitProductionFactorAsScan(world.dimension().location(), rootPos, offset);
        }

        SeasonContext context = getContext(world);
        return isTropical(world, rootPos) ? context.getTropicalFruitProductionFactor(offset) : context.getTemperateFruitProductionFactor(offset);
    }

    public Float getSeasonValue(World world, BlockPos pos) {
        return getContext(world).getSeasonProvider().getSeasonValue(world, pos);
    }

    @Override
    public boolean shouldSnowMelt(World world, BlockPos pos) {
        return getContext(world).getSeasonProvider().shouldSnowMelt(world, pos);
    }

    public float getFruitProductionFactorAsScan(ResourceLocation dimLoc, BlockPos rootPos, float offset) {
        if (seasonContextMap.size() > 0) {
            float seasonValue = rootPos.getY() / 64.0f;
            boolean tropical = rootPos.getZ() >= 1.0f;
            if (seasonContextMap.containsKey(dimLoc)) {
                SeasonContext context = seasonContextMap.get(dimLoc);
                SeasonGrowthCalculator calculator = context.getCalculator();
                return calculator.calcFruitProduction(seasonValue + offset, tropical ? ClimateZoneType.TROPICAL : ClimateZoneType.TEMPERATE);
            }
        }
        return 0.0f;
    }

}
