package com.ferreusveritas.dynamictrees.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.TranslationTextComponent;

import java.awt.*;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public final class HexColorArgument implements ArgumentType<Integer> {

    public static final DynamicCommandExceptionType COLOR_INVALID = new DynamicCommandExceptionType(colourString -> new TranslationTextComponent("argument.color.invalid", colourString));

    public static HexColorArgument hex() {
        return new HexColorArgument();
    }

    public static int getHexCode(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Integer.class);
    }

    @Override
    public Integer parse(StringReader reader) throws CommandSyntaxException {
        final String in = reader.readString();
        final int colour;

        try {
            colour = Color.decode((in.startsWith("#") ? "" : "#") + in).getRGB();
        } catch (NumberFormatException e) {
            throw COLOR_INVALID.create(in);
        }

        return colour;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return ISuggestionProvider.suggest(Collections.singletonList("00FFFF"), builder);
    }

}
