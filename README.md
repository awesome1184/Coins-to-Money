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

Use **Copy purse diagnostics** while in SkyBlock to inspect a purse that remains unchanged.
It copies the currency rows, their format types/codepoints, hook activity, installed optional
mod versions and conversion settings. It includes displayed balances, but not credentials,
chat, or player names from other scoreboard rows. It stays on the clipboard until shared.

Other mods that replace vanilla's sidebar or draw their own text may need integration.
Server-rounded numbers (for example 1.2m) cannot recover their original precision.

## Optional mod integrations (1.2.3)

Mana Cost, Soulflow Cost and other compound resource labels are not treated as coin
price fields. Unknown units, stat glyphs and percentages in bare price fields fail closed.
Explicit coin amounts still convert, including the coin portion of mixed-resource purchases.

**CustomScoreboard:** supports the published **1.12.14-2 for 26.1.x** build. Purse/Piggy,
all four label/number placements, long/compact and localized numbers, chunked purse and
Hypixel Lines are supported. Money and cookies use its unrounded, read-only purse API;
retained coins keep CustomScoreboard's chosen representation. Its own builder measures
and arranges the converted components. The Sidebar toggle controls this integration.

**SkyHanni:** supports the published **7.56.0 for 26.1.x** build. Coin-specific formatting
covers chest profit and tracker item-value breakdowns; additional display adapters cover
shared tracker totals/profit per hour, labelled mining money/hour, and crop-money cells.
Counts, XP, mana, Soulflow, percentages, source prices and profit arithmetic are not modified.
Use **K -> SkyHanni profits** to control this integration independently of the sidebar.
Existing money/cookie/coin order, layout, currency and precision settings apply to both mods.

SkyHanni's original renderable text is retained where the label adapter is used, and the
same projection is measured and drawn. Shared trackers receive their native one-frame
refresh request on display/quote changes. Other cached overlays rebuild on their usual
native refresh; reopen a chest overlay after changing display settings if it is still cached.
No conversion is fabricated while the cookie quote is unavailable.

Native SkyBlock context is used as well as the vanilla sidebar. CustomScoreboard, SkyHanni,
Kotlin and their dependencies remain optional and are not bundled. Install each mod's own
normal dependencies. Older/newer mod versions may change signatures; the clipboard diagnostics
include installed versions and adapter counters. These integrations do not imply support for
arbitrary mod-drawn numbers or every future SkyHanni display.

See [integration details and reproducible tests](docs/1.2.3-INTEGRATIONS.md).

## Build and test

Use JDK 25 and Gradle 9.5.1:

```sh
gradle clean build --no-daemon
```

Install `build/libs/coins-to-money-1.2.3.jar` with Fabric Loader 0.19.5+ and Fabric API
0.155.3+26.1.2, replacing the previous JAR. Do not install sources or the smoke-test mod.

CI runs unit regressions, verifies the distribution, and boots four actual Fabric clients:
without optional mods, CustomScoreboard alone, SkyHanni alone, and both together. The mod
binaries are pinned with SHA-512 in `ci/integration-lock.json` and are not substituted with mocks.
Each run exercises 120 settings/currency rendering frames, with actual native widgets where
installed. Tests cover complete CustomScoreboard builds under three locales and both numeric
formats, chest-profit construction, shared tracker totals, crop formatting, green values,
widths, gates, settings changes and unchanged underlying balances/calculations.

Test fixtures are excluded from the distributed JAR. Existing section-p purse, non-coin cost,
and all 168 display order/layout/visibility regressions remain. These are synthetic inputs in
real mods, **not a logged-in Hypixel playtest**. Reports, four client logs, source snapshot and
SHA-256 are attached to the successful build. The GitHub release publishes that exact tested
JAR without rebuilding it.
