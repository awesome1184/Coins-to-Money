package com.example.skyblockusd;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.PlayerScoreEntry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SidebarOrdinaryTailTest {
    @BeforeAll static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }
    @Test void completesAnIncompleteGroupUsingOnlyTheVisibleOrdinaryScore() {
        ModConfig.INSTANCE = new ModConfig();
        long now = System.currentTimeMillis();
        var entry = new PlayerScoreEntry("row", 56, Component.literal("Purse: 7,014,5"), null);
        var state = new CookiePriceFetcher.State(new BazaarQuote(10_000_000, now), null);
        var result = ScoreboardCoinHelper.replace(entry.ownerName(), entry.formatValue(StyledFormat.SIDEBAR_DEFAULT), entry, StyledFormat.SIDEBAR_DEFAULT, state, now);
        assertEquals("Purse: $2.07", result.name().getString());
        assertEquals("", result.score().getString());
        assertEquals(56, entry.value());
    }
}
