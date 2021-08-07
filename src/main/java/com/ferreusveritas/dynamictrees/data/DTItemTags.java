package com.ferreusveritas.dynamictrees.data;

import com.ferreusveritas.dynamictrees.DynamicTrees;
import net.minecraft.item.Item;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;

/**
 * @author Harley O'Connor
 */
public final class DTItemTags {

    public static final ITag.INamedTag<Item> BRANCHES = bind("branches");
    public static final ITag.INamedTag<Item> BRANCHES_THAT_BURN = bind("branches_that_burn");
    public static final ITag.INamedTag<Item> FUNGUS_BRANCHES = bind("fungus_branches");

    public static final ITag.INamedTag<Item> SEEDS = bind("seeds");
    public static final ITag.INamedTag<Item> FUNGUS_CAPS = bind("fungus_caps");

    private static ITag.INamedTag<Item> bind(String identifier) {
        return ItemTags.bind(DynamicTrees.MOD_ID + ":" + identifier);
    }

}
