package com.example.skyblockusd;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class NonCoinCostTest {
    @ParameterizedTest @ValueSource(strings = {
        "Mana Cost: 50", "Mana Cost: 50✎", "Soulflow Cost: 1", "Health Cost: 100",
        "Intelligence Cost: 200", "Energy Cost: 50", "Stamina Cost: 20", "Ability Cost: 10",
        "Rift Time Cost: 10", "Item Cost: 64", "Arrow Cost: 1", "Experience Cost: 30",
        "Reduced Mana Cost: 50", "Future Resource Cost: 42", "Mana Price: 25",
        "Mana Cost: §350✎", "§7Soulflow §pCost: §31", "Mana Cost: 25%",
        "Cost: 50 Mana", "Cost: 50 Soulflow", "Cost: 100 Health", "Cost: 64 Items",
        "Cost: 50✎", "Cost: 100❤", "Cost: 20%", "Cost: 20 seconds", "Cost: 20/100",
        "Price: 325 Gems", "Cost: 1,000 Bits", "Cost: 20 Copper", "Cost: 100 Motes",
        "Cost: 20 Essence", "Cost: 5 Tokens", "Cost: 2 Cookies", "Cost: 1 NewResource",
        "Damage: +100", "Strength: +125 (+25)", "Speed: +50", "Cooldown: 3s"
    })
    void resourceCostsAndStatsKeepTheExactOriginalComponent(String text) {
        var original = Component.literal(text).withStyle(ChatFormatting.AQUA);
        assertTrue(CoinParser.find(text, true, true).isEmpty(), text);
        assertSame(original, convert(original));
    }
    @ParameterizedTest @ValueSource(strings = {
        "Cost: 1,000", "Price: 1,000", "Buy price: 1,000", "Sell price: 1,000",
        "Starting bid: 1,000", "Top bid: 1,000", "Your bid: 1,000", "BIN price: 1,000",
        "Buy it now: 1,000", "Price per unit: 1,000", "  Cost: 1,000", "• Cost: 1,000",
        "Price: 1,000 each", "Price: 1,000 per unit", "Price: 1,000/unit"
    })
    void completeCoinPriceFieldsStillWork(String text) {
        var matches = CoinParser.find(text, true, true);
        assertEquals(1, matches.size(), text); assertEquals(1000d, matches.getFirst().coins());
    }
    @Test void mixedResourcesOnlyConvertTheExplicitCoinAmount() {
        var original = Component.literal("Mana Cost: 50 | Soulflow Cost: 1 | Cost: 12,295,597.2 coins + 20 Essence");
        assertEquals("Mana Cost: 50 | Soulflow Cost: 1 | Cost: $2.95 + 20 Essence", convert(original).getString());
    }
    @Test void multiplePriceFieldsAndNonCoinFieldsCanShareALine() {
        assertEquals(2, CoinParser.find("Buy price: 100 | Sell price: 200 | Mana Cost: 50", true, true).size());
        assertEquals(2, CoinParser.find("Buy price: 100 / Sell price: 200", true, true).size());
    }
    @Test void screenshotTooltipKeepsAbilityAndResourceText() {
        String tooltip = "Heroic Aspect of the End\nDamage: +100\nStrength: +125 (+25)\nUltimate Wise V\n"
                + "Ability: Instant Transmission RIGHT CLICK\nTeleport 12 blocks ahead of you and\n"
                + "gain +50 Speed for 3 seconds.\nMana Cost: 50✎\n"
                + "Ability: Ether Transmission SNEAK RIGHT CLICK\nTeleport to your targeted block up to 61 blocks away.\n"
                + "Soulflow Cost: 1\nMana Cost: 50✎";
        var original = Component.literal(tooltip); assertSame(original, convert(original));
    }
    @Test void formattingSplitsDoNotMakeResourceCostsIntoCoinLabels() {
        var original = Component.literal("Mana ").append(Component.literal("Cost: "))
                .append(Component.literal("50").withStyle(ChatFormatting.AQUA)).append("✎");
        assertSame(original, convert(original));
    }
    private static Component convert(Component c) {
        return CoinText.convert(c, true, true, CoinTextTest.LIVE, CoinTextTest.NOW, new ModConfig());
    }
}
