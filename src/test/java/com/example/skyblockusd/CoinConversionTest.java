package com.example.skyblockusd;

import org.junit.jupiter.api.Test;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

class CoinConversionTest {
    @Test void usesElevenThousandGemBundleWithoutIntermediateRounding() {
        var value = CoinConversion.of(10_000_000, 10_000_000);
        assertEquals(1d, value.cookies());
        assertEquals(325d * 100d / 11000d, value.usd(), 1e-12);
        assertEquals("1.000 cookies | $2.95", value.display());
    }
    @Test void aWholeBundleIsExactlyOneHundredDollars() {
        assertEquals(100d, CoinConversion.of(11_000d / 325d * 10_000_000d, 10_000_000d).usd(), 1e-10);
    }
    @Test void fractionsZeroAndLargeAmountsAreSupported() {
        assertEquals(.5, CoinConversion.of(5, 10).cookies());
        assertEquals(0, CoinConversion.of(0, 10).usd());
        assertEquals(100_000, CoinConversion.of(1e12, 1e7).cookies());
    }
    @Test void neverInventsAnExchangeRate() {
        for (double invalid : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) assertThrows(IllegalArgumentException.class, () -> CoinConversion.of(1, invalid));
        assertThrows(IllegalArgumentException.class, () -> CoinConversion.of(Double.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> CoinConversion.of(Double.MAX_VALUE, Double.MIN_VALUE));
    }
    @Test void tinyValuesAreNotMisrepresentedAsFree() { assertTrue(CoinConversion.of(1, 10_000_000).display().contains("<$0.01")); }
    @Test void formattingDoesNotDependOnThePlayersLocale() {
        Locale previous = Locale.getDefault();
        try { Locale.setDefault(Locale.GERMANY); assertEquals("1.000 cookies | $2.95", CoinConversion.of(10, 10).display()); }
        finally { Locale.setDefault(previous); }
    }
}
