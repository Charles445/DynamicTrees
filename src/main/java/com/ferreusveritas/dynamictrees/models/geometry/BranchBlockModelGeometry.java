package com.ferreusveritas.dynamictrees.models.geometry;

import com.ferreusveritas.dynamictrees.client.thickrings.ThickRingTextureManager;
import com.ferreusveritas.dynamictrees.models.bakedmodels.BasicBranchBlockBakedModel;
import com.ferreusveritas.dynamictrees.models.bakedmodels.ThickBranchBlockBakedModel;
import com.ferreusveritas.dynamictrees.models.loaders.BranchBlockModelLoader;
import com.ferreusveritas.dynamictrees.trees.Family;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.geometry.IModelGeometry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Bakes {@link BasicBranchBlockBakedModel} from bark and rings texture locations given by {@link
 * BranchBlockModelLoader}.
 *
 * <p>Can also be used by sub-classes to bake other models, like for roots in
 * {@link RootBlockModelGeometry}.</p>
 *
 * @author Harley O'Connor
 */
@OnlyIn(Dist.CLIENT)
public class BranchBlockModelGeometry implements IModelGeometry<BranchBlockModelGeometry> {

    protected final Set<ResourceLocation> textures = new HashSet<>();
    protected final ResourceLocation barkResLoc;
    protected final ResourceLocation ringsResLoc;
    protected final boolean forceThickness;

    protected ResourceLocation familyResLoc;
    protected Family family;

    protected ResourceLocation thickRingsResLoc;

    public BranchBlockModelGeometry(@Nullable final ResourceLocation barkResLoc, @Nullable final ResourceLocation ringsResLoc, @Nullable final ResourceLocation familyResLoc, final boolean forceThickness) {
        this.barkResLoc = barkResLoc;
        this.ringsResLoc = ringsResLoc;
        this.familyResLoc = familyResLoc;
        this.forceThickness = forceThickness;

        this.addTextures(barkResLoc, ringsResLoc);
    }

    /**
     * Adds the given texture {@link ResourceLocation} objects to the list. Checks they're not null before adding them
     * so {@link Nullable} objects can be fed safely.
     *
     * @param textureResourceLocations Texture {@link ResourceLocation} objects.
     */
    protected void addTextures(final ResourceLocation... textureResourceLocations) {
        for (ResourceLocation resourceLocation : textureResourceLocations) {
            if (resourceLocation != null) {
                this.textures.add(resourceLocation);
            }
        }
    }

    @Override
    public IBakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<RenderMaterial, TextureAtlasSprite> spriteGetter, IModelTransform modelTransform, ItemOverrideList overrides, ResourceLocation modelLocation) {
        if (!this.useThickModel(this.setFamily(modelLocation))) {
            return new BasicBranchBlockBakedModel(modelLocation, this.barkResLoc, this.ringsResLoc);
        } else {
            return new ThickBranchBlockBakedModel(modelLocation, this.barkResLoc, this.ringsResLoc, this.thickRingsResLoc);
        }
    }

    private ResourceLocation setFamilyResLoc(final ResourceLocation modelResLoc) {
        if (this.familyResLoc == null) {
            this.familyResLoc = new ResourceLocation(modelResLoc.getNamespace(), modelResLoc.getPath().replace("block/", "").replace("_branch", "").replace("stripped_", ""));
        }
        return this.familyResLoc;
    }

    private Family setFamily(final ResourceLocation modelResLoc) {
        if (this.family == null) {
            this.family = Family.REGISTRY.get(this.setFamilyResLoc(modelResLoc));
        }
        return this.family;
    }

    private boolean useThickModel(final Family family) {
        return this.forceThickness || family.isThick();
    }

    @SuppressWarnings("deprecation")
    @Override
    public Collection<RenderMaterial> getTextures(IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
        if (this.thickRingsResLoc == null && this.useThickModel(this.setFamily(new ResourceLocation(owner.getModelName())))) {
            this.thickRingsResLoc = ThickRingTextureManager.addRingTextureLocation(this.ringsResLoc);
            this.addTextures(this.thickRingsResLoc);
        }

        return this.textures.stream()
                .map(resourceLocation -> new RenderMaterial(AtlasTexture.LOCATION_BLOCKS, resourceLocation))
                .collect(Collectors.toList());
    }

}
