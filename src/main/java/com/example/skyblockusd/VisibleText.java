package com.example.skyblockusd;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;

/** Shared vanilla text decoding for currency matching and style-preserving replacement. */
final class VisibleText {
    private VisibleText() { }

    static String plain(FormattedText text) {
        StringBuilder result = new StringBuilder();
        visit(text, (index, style, codepoint) -> {
            result.appendCodePoint(codepoint);
            return true;
        });
        return result.toString();
    }

    static void visit(FormattedText text, FormattedCharSink sink) {
        // Vanilla consumes section-sign pairs even for unknown codes (e.g. Hypixel's
        // section-p scoreboard owner). Decode each component run separately, as vanilla
        // does, so a dangling section sign cannot swallow a digit in the next sibling.
        // Retain the mod's existing exclusion of invisible Unicode format controls.
        StringDecomposer.iterateFormatted(text, Style.EMPTY, (index, style, codepoint) ->
                Character.getType(codepoint) == Character.FORMAT || sink.accept(index, style, codepoint));
    }
}
