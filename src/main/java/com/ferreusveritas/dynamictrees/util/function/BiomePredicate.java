package com.ferreusveritas.dynamictrees.util.function;

import com.ferreusveritas.dynamictrees.api.configurations.ConfigurationProperty;
import net.minecraft.world.biome.Biome;

import java.util.function.Predicate;

/**
 * A {@link Predicate} that tests if something should happen in a {@link Biome}. Mainly used as a {@link
 * ConfigurationProperty}.
 *
 * @author Harley O'Connor
 */
@FunctionalInterface
public interface BiomePredicate extends Predicate<Biome> {
}
