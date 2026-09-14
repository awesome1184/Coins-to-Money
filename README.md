# Coins to Money

Client-side Fabric mod for **Minecraft 26.1.2 / Java 25**, originally by awesome1184.
Replaces recognized SkyBlock coin prices with money. USD-only is the default; cookies and
original coins are optional. There is no standalone HUD.

## Settings

**K** opens settings; **O** toggles the mod. Both keys are rebindable. Mod Menu is optional.
Every control and currency input has a hover explanation. Settings save automatically,
except currency edits which require **Apply**; **Cancel** discards those edits.

Enable money, cookies and/or original coins, then choose any of their **six display orders**.
Hidden values are skipped. Layouts are **brackets**, **parentheses**, **inline bars** or
**equals signs**. Examples (illustrative values, not live exchange quotes):

```text
Price: 1,000 coins [$1.00]
Price: $1.00 (1,000 coins)
Price: $1.00 | 0.338 cookies | 1,000 coins
Price: 1,000 coins = $1.00
```

Money and cookies are green; retained coin amounts keep their original colour and style.
Money precision is 2-8 decimals and cookie precision is 1-6. Tiny non-zero values show a
less-than indicator rather than being misrepresented as zero. At least one value stays on.
The settings preview uses an illustrative quote and is not a live price display.

## Conversion and other currencies

```text
cookies = coins / current cookie instant-buy price
USD = cookies * 325 * (100 / 11000)
selected currency = USD * currencyPerUsd
```

The requested comparison basis remains **11,000 gems for $100** and **325 gems per cookie**,
approximately $2.954545 per cookie, not a claim about the cheapest current shop bundle.
Fractions are retained until display rounding. This is a replacement-cost comparison,
not cash-out value; taxes, promotions and order slippage are not included.

Open **Currency** to enter a supported three-letter real-world code, such as USD, EUR,
GBP, JPY, CAD, CHF or RSD. Enter **target-currency units per ONE USD**. For example only,
EUR with `0.92` means $10 displays as EUR 9.20. That example is not a current FX quote.
Supply your chosen rate; it is saved locally and **does not auto-refresh**. USD uses 1.
Bad codes, non-positive/non-finite rates and overflow fail closed. A bad saved currency/rate
pair falls back together to USD/1, not a made-up rate under a foreign currency symbol.

Hypixel's public Bazaar endpoint supplies the cheapest available one-cookie instant-buy
`BOOSTER_COOKIE.buy_summary` offer. `quick_status.buyPrice` is a weighted average and is not
used. The quote refreshes once a minute while enabled in SkyBlock. No API key, profile API,
FX provider or telemetry is used. Reference: https://api.hypixel.net/

No fabricated fallback quote is used. Until the first valid quote, original text stays.
Last-good quotes are marked stale after three minutes or on an API error and expire after
30 minutes. Requests use a daemon worker, timeouts and response limits, never the render thread.

## Coverage and sidebar troubleshooting

Supports vanilla sidebar currency rows, Bazaar/AH/NPC item tooltips, server game messages
and the action bar. Item stats, quantities and non-coin currencies are not rewritten.
Existing chat messages do not reformat retroactively when settings change.

SkyBlock detection selects the same objective as vanilla, including team-colour slots,
on hypixel.net or a subdomain. Sidebar conversion handles the assembled team/name text
and the actual formatted value column before vanilla calculates width. If a non-fixed
numeric column completes an otherwise incomplete comma group, the displayed pieces are
joined; a complete left-hand amount never absorbs an ordinary numeric ordering score.
No multiplication-by-100 guess or server scoreboard mutation is used.

Since 1.2.1, matching and replacement share vanilla's formatted-text decoder. Unsupported
section-sign pairs such as `§p` are consumed just as Minecraft consumes them; digits on
either side remain part of the same number. This fixes the diagnostic row
`Purse: §67,416,7§p§601 §e(+5)` with an empty `BlankFormat` value. Its purse is **7,416,701**;
`(+5)` is a separate gain suffix, and the raw score of 5 is not part of the balance.

If a live purse still remains unchanged, use **Copy purse diagnostics** while in SkyBlock.
It copies the currency rows, their format types/codepoints, hook activity and conversion
settings. It includes displayed balances, but not credentials, chat, or player names from
other scoreboard rows. It stays on the clipboard until you choose to share it. This allows
an exact live layout to be examined instead of guessing from a screenshot.

Other mods that replace vanilla's sidebar or draw their own text may need integration.
Server-rounded numbers (for example 1.2m) cannot recover their original precision.

## Build and test

Use JDK 25 and Gradle 9.5.1:

```sh
gradle clean build --no-daemon
```

Install `build/libs/coins-to-money-1.2.1.jar` with Fabric Loader 0.19.5+ and Fabric API
0.155.3+26.1.2, replacing the previous JAR. Do not install sources or the smoke-test mod.

CI runs unit regressions, verifies the distribution, and boots an actual Fabric client
under Xvfb to test transformed sidebar rows, widths, hover tooltips, settings persistence,
manual-rate validation and 120 settings/currency render frames. Test-only fixtures are not
included in the shipped JAR. Unit coverage includes all 168 order/layout/visibility combinations.
The client tests use synthetic data, **not a logged-in Hypixel playtest**. Reports, logs,
source snapshot and SHA-256 are attached to the successful workflow run.
