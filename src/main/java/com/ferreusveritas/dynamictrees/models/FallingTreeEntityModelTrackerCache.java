package com.ferreusveritas.dynamictrees.models;

import com.ferreusveritas.dynamictrees.entities.FallingTreeEntity;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class FallingTreeEntityModelTrackerCache {

    public static Map<Integer, FallingTreeEntityModel> modelMap = new ConcurrentHashMap<>();

    public static FallingTreeEntityModel getModel(FallingTreeEntity entity) {
        return modelMap.computeIfAbsent(entity.getId(), e -> new FallingTreeEntityModel(entity));
    }

    public static void cleanupModels(World world, FallingTreeEntity entity) {
        modelMap.remove(entity.getId());
        cleanupModels(world);
    }

    public static void cleanupModels(World world) {
        modelMap = modelMap.entrySet().stream()
                .filter(map -> world.getEntity(map.getKey()) != null)
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }
}
