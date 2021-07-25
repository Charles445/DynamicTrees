package com.ferreusveritas.dynamictrees.util.json;

import com.ferreusveritas.dynamictrees.util.CommonVoxelShapes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;

/**
 * @author Harley O'Connor
 */
public final class VoxelShapeGetter implements JsonGetter<VoxelShape> {

    @Override
    public FetchResult<VoxelShape> get(JsonElement jsonElement) {
        final FetchResult<VoxelShape> voxelShape = new FetchResult<>();

        JsonHelper.JsonElementReader.of(jsonElement).ifOfType(String.class, string ->
                voxelShape.setValue(CommonVoxelShapes.SHAPES.getOrDefault(string.toLowerCase(), VoxelShapes.block())))
                .elseIfOfType(AxisAlignedBB.class, axisAlignedBB -> voxelShape.setValue(VoxelShapes.create(axisAlignedBB)))
                .elseIfOfType(JsonArray.class, jsonArray -> {
                    voxelShape.setValue(VoxelShapes.empty());
                    for (JsonElement element : jsonArray) {
                        JsonHelper.JsonElementReader.of(element).ifOfType(AxisAlignedBB.class, axisAlignedBB ->
                                VoxelShapes.or(voxelShape.getValue(), VoxelShapes.create(axisAlignedBB)))
                                .elseUnsupportedError(voxelShape::addWarning).ifFailed(voxelShape::addWarning);
                    }
                }).elseUnsupportedError(voxelShape::setErrorMessage).ifFailed(voxelShape::setErrorMessage);

        return voxelShape;
    }

}
