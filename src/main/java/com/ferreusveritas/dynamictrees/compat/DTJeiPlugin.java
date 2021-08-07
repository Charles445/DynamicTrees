package com.ferreusveritas.dynamictrees.compat;

import com.ferreusveritas.dynamictrees.DynamicTrees;
import com.ferreusveritas.dynamictrees.init.DTRegistries;
import com.ferreusveritas.dynamictrees.items.DendroPotion;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Harley O'Connor
 */
@JeiPlugin
public final class DTJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return DynamicTrees.resLoc(DynamicTrees.MOD_ID);
    }

    @Override
    public void registerItemSubtypes(final ISubtypeRegistration registration) {
        registration.useNbtForSubtypes(DTRegistries.DENDRO_POTION);
    }

    @Override
    public void registerRecipes(final IRecipeRegistration registration) {
        final IVanillaRecipeFactory factory = registration.getVanillaRecipeFactory();
        final List<IJeiBrewingRecipe> brewingRecipes = new ArrayList<>();

        DendroPotion.brewingRecipes.forEach(recipe ->
                brewingRecipes.add(makeJeiBrewingRecipe(factory, recipe.getInput(), recipe.getIngredient(), recipe.getOutput())));

        registration.addRecipes(brewingRecipes, VanillaRecipeCategoryUid.BREWING);
    }

    private static IJeiBrewingRecipe makeJeiBrewingRecipe(IVanillaRecipeFactory factory, final ItemStack inputStack, final ItemStack ingredientStack, ItemStack output) {
        return factory.createBrewingRecipe(Collections.singletonList(ingredientStack), inputStack, output);
    }

}
