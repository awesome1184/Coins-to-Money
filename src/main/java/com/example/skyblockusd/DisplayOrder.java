package com.example.skyblockusd;

import java.util.List;

/** Hidden values are skipped, not replaced with empty separators. */
public enum DisplayOrder {
    COINS_MONEY_COOKIES("Coins > Money > Cookies", Kind.COINS, Kind.MONEY, Kind.COOKIES),
    MONEY_COINS_COOKIES("Money > Coins > Cookies", Kind.MONEY, Kind.COINS, Kind.COOKIES),
    MONEY_COOKIES_COINS("Money > Cookies > Coins", Kind.MONEY, Kind.COOKIES, Kind.COINS),
    COINS_COOKIES_MONEY("Coins > Cookies > Money", Kind.COINS, Kind.COOKIES, Kind.MONEY),
    COOKIES_COINS_MONEY("Cookies > Coins > Money", Kind.COOKIES, Kind.COINS, Kind.MONEY),
    COOKIES_MONEY_COINS("Cookies > Money > Coins", Kind.COOKIES, Kind.MONEY, Kind.COINS);

    public enum Kind { COINS, MONEY, COOKIES }
    public final String label;
    public final List<Kind> parts;
    DisplayOrder(String label, Kind... parts) { this.label = label; this.parts = List.of(parts); }
    public DisplayOrder next() { return values()[(ordinal() + 1) % values().length]; }
}
