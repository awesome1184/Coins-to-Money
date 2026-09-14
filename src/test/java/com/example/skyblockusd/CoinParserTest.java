package com.example.skyblockusd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CoinParserTest {
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "7,014,556|7014556", "0|0", "999|999", "1,234.56|1234.56", "1.5k|1500", "2M|2000000",
        "3.25b|3250000000", "1t|1000000000000", "-1,234|-1234", "+2.5K|2500", "1000000000000|1000000000000", "1q|1000000000000000"
    })
    void parsesWholeNumbers(String text, double expected) { assertEquals(expected, CoinParser.parseNumber(text)); }

    @ParameterizedTest
    @ValueSource(strings = {"7,014,5", "1,00", "1,,000", "12.3.4", "NaN", "Infinity", "1e9", "1kk", "", "-", "1,234,", "123abc"})
    void rejectsMalformedAmounts(String text) {
        assertTrue(Double.isNaN(CoinParser.parseNumber(text)));
        assertTrue(CoinParser.find("Purse: " + text, true, true).isEmpty());
        assertTrue(CoinParser.find(text + " coins", true, true).isEmpty());
    }
    @Test void joinsTheReportedSuffixBeforeParsing() {
        String prefix = "§6Purse: ", middle = "§67,014,5", suffix = "§656";
        assertTrue(CoinParser.find(prefix + middle, true, false).isEmpty());
        assertEquals(7_014_556d, CoinParser.purse(prefix + middle + suffix).orElseThrow());
    }
    @Test void supportsFormattingBetweenAnyDigits() {
        assertEquals(7_014_556d, CoinParser.purse("Purse: 7,§a014,\u200b556").orElseThrow());
    }
    @Test void matchesAllPricesOnceInOrder() {
        var matches = CoinParser.find("Buy price: 1,000 coins / Sell price: 950 Coins", true, true);
        assertEquals(2, matches.size()); assertEquals(1000, matches.get(0).coins()); assertEquals(950, matches.get(1).coins());
    }
    @Test void doesNotConvertStatsDatesOrItemCounts() {
        assertTrue(CoinParser.find("Health: 1,200 | 12/09/26 | 64x Stone | 325 Gems | 2 cookies", true, true).isEmpty());
        assertTrue(CoinParser.find("Price: 325 Gems | Cost: 1,000 Bits", true, true).isEmpty());
    }
    @Test void balancesAndPriceOnlyLabelsAreOptional() {
        assertTrue(CoinParser.find("Purse: 500 Price: 600", false, false).isEmpty());
        assertEquals(2, CoinParser.find("Purse: 500 Price: 600", true, true).size());
    }
    @Test void bankDoesNotOverwritePurseAndPiggyBankWorks() {
        assertTrue(CoinParser.purse("Bank: 999,999").isEmpty());
        assertEquals(123, CoinParser.purse("Piggy Bank: 123").orElseThrow());
    }
    @Test void isIdempotentAndLeavesPurseGainSeparate() {
        assertTrue(CoinParser.find("1,000 coins [0.001 cookies | $<0.01 USD]", true, true).isEmpty());
        assertEquals(7_014_556d, CoinParser.purse("Purse: 7,014,556 (+56)").orElseThrow());
    }
}
