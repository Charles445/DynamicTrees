package com.ferreusveritas.dynamictrees.api.resource.loading.preparation;

import com.ferreusveritas.dynamictrees.api.resource.ResourceCollector;
import com.ferreusveritas.dynamictrees.api.resource.Resource;
import com.ferreusveritas.dynamictrees.deserialisation.JsonHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.resources.IResource;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * @author Harley O'Connor
 */
public final class JsonResourcePreparer extends AbstractResourcePreparer<JsonElement> {

    private static final String JSON_EXTENSION = ".json";

    public JsonResourcePreparer(String folderName) {
        this(folderName, ResourceCollector.ordered());
    }

    public JsonResourcePreparer(String folderName, ResourceCollector<JsonElement> resourceCollector) {
        super(folderName, JSON_EXTENSION, resourceCollector);
    }

    @Override
    protected void readAndPutResource(IResource resource, ResourceLocation resourceName) throws PreparationException {
        final JsonElement jsonElement = readResource(resource);
        this.resourceCollector.put(new Resource<>(resourceName, jsonElement));
    }

    @Nonnull
    static JsonElement readResource(IResource resource) throws PreparationException {
        final Reader reader = getReader(resource);
        final JsonElement json = tryParseJson(reader);

        if (json == null) {
            throw new PreparationException("Couldn't load file as it's null or empty");
        }
        return json;
    }

    private static BufferedReader getReader(IResource resource) {
        return new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
    }

    @Nullable
    private static JsonElement tryParseJson(Reader reader) throws PreparationException {
        try {
            return JSONUtils.fromJson(JsonHelper.getGson(), reader, JsonElement.class);
        } catch (JsonParseException e) {
            throw new PreparationException(e);
        }
    }

}
