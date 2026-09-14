package com.example.skyblockusd;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class NonCoinCostTest {
    private static final long NOW = 1_800_000_000_000L;
    private final CookiePriceFetcher.State state = new CookiePriceFetcher.State(new BazaarQuote(12_295_597.2, NOW), null);

    @ParameterizedTest
    @ValueSource(strings = {"Mana Cost: 25", "Mana Cost: 25✎", "§7Mana Cost: §b25✎", "Soulflow Cost: 1", "Health Cost: 50", "Energy Cost: 100", "Ability Cost: 50", "Stamina Cost: 20", "Rage Cost: 5", "Experience Cost: 30", "Item Cost: 64", "Cost: 50", "Mana Price: 50", "Mana Balance: 50", "Price: 50 mana", "Price: 50✎", "Price: 50%", "Price: 2 blocks", "Cost: 50 bits", "Cost: 50 copper", "Cost: 50 essence", "Cost: 50 motes"})
    void nonCoinValuesStayUntouched(String text) {
        Component original = Component.literal(text);
        ModConfig config = new ModConfig(); config.showCookies = true;
        assertTrue(CoinParser.find(text, true, true).isEmpty(), text);
        assertSame(original, CoinText.convert(original, true, true, state, NOW, config), text);
    }

    @Test void explicitCoinCostsStillConvertInTheSameTooltip() {
        ModConfig config = new ModConfig();
        assertEquals("Mana Cost: 25✎\nSoulflow Cost: 1\nCost: $2.40",
                CoinText.convert(Component.literal("Mana Cost: 25✎\nSoulflow Cost: 1\nCost: 10,000,000 coins"), true, true, state, NOW, config).getString());
        assertEquals("Ability Cost: $2.40", CoinText.convert(Component.literal("Ability Cost: 10,000,000 coins"), true, true, state, NOW, config).getString());
    }
}
