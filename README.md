# Coins to Money

Client-side Fabric mod for **Minecraft 26.1.2 / Java 25**, originally by awesome1184.
Replaces SkyBlock coin amounts with USD by default. Cookie equivalents and original coin
amounts are optional settings; there is no standalone HUD.

## Conversion

```
cookies = coins / current cookie instant-buy price
USD = cookies * 325 * (100 / 11000)
```

The requested basis is **11,000 gems for $100 USD** and **325 gems per Booster Cookie**:
approximately **$2.954545 per cookie**. Fractions are retained until display rounding. This is
a replacement-cost comparison, not a cash-out value. Taxes, promotions and large-order
slippage are not included; cookie counts are fractional equivalents, not whole-item quotes.

The mod queries Hypixel's public Bazaar endpoint once per minute while enabled in SkyBlock.
It selects the cheapest available `BOOSTER_COOKIE.buy_summary` offer with at least one cookie.
In Hypixel's API naming this is the instant-buy side. `quick_status.buyPrice` is a weighted
average, so it is deliberately **not** used. No API key, profile API or telemetry is needed.

API reference: https://api.hypixel.net/#tag/SkyBlock/paths/~1v2~1skyblock~1bazaar/get

## Display and settings

Default examples, using a **sample** 12,295,597.2-coin cookie price (not a fixed exchange rate):

```
Purse: 7,416,611                  -> Purse: $1.78
Worth 13.6M coins                  -> Worth $3.27
Price per unit: 57,716.6 coins     -> Price per unit: $0.01
```

**K** opens settings; **O** toggles the mod. Both are rebindable. Mod Menu is optional.
Settings include independent sidebar, tooltip and server-message switches; **Show dollars**
(default on), **Show cookies** (default off), **Keep coin amounts** (default off), USD precision
(2–8 decimals), and cookie precision (1–6 decimals). At least one display value remains on.
Legacy `showGui` settings are ignored: the old floating rate HUD has been removed completely.
The price/status and conversion basis are visible inside settings instead.

Prices are replaced in-place, including the word `coins`, without altering surrounding text
or colors. Multiple prices on one line are converted individually. Keeping coins enabled
instead produces an adjacent bracketed equivalent. Player chat is not rewritten; server
messages and the action bar use the same conversion routine.

The sidebar hook acts on the complete vanilla `Gui$1DisplayEntry` construction, AFTER team
formatting AND `PlayerScoreEntry.formatValue()`, but BEFORE layout. This covers coin amounts
in the name, the separate value column, and digits split across both. Server `FixedFormat`
text is read as text; raw sorting scores are never multiplied by 100 or appended as digits.
No global team/name mutation is installed, and original scoreboard data stays unchanged.

The mod activates for a `SKYBLOCK` sidebar on `hypixel.net` or its subdomains. It does not
rewrite unrelated stats, quantities, gems or Bits. Third-party mods replacing vanilla's
sidebar/rendering path need separate integration. Precision already rounded by the server
(e.g. `1.2m`) cannot be recovered.

## Failure behaviour

No fabricated fallback price is used. Before the first valid response, original coin
text stays untouched and settings report an unavailable price. Last-good quotes are marked stale
after three minutes or immediately after an API error; they expire after 30 minutes.
Requests have timeouts and response-size limits, use one daemon worker, and never block
rendering. Legacy saved exchange-rate fields are not treated as current prices.

## Build and test

Install JDK 25 and Gradle 9.5.1, then run:

```sh
gradle clean build --no-daemon
```

Install `build/libs/coins-to-money-1.1.1.jar` with Fabric Loader 0.19.5+ and Fabric API
0.155.3+26.1.2. Do not install the sources JAR. CI publishes the mod JAR and test reports as
workflow artifacts.

CI runs unit tests plus a test-only Fabric client under Xvfb. The client invokes the actual
mixin-transformed vanilla sidebar row factory with full, partial and fixed-format amounts,
checks both displayed columns and measured widths, clicks display settings and reloads the
saved config, then renders 60 frames of the settings screen and synthetic sidebar. A real
`GuiRenderState` blur is requested each frame to catch the reported double-blur crash. The
smoke-test mod is NOT included in the installable JAR.

Run the client regressions on Linux with Xvfb available:

```sh
gradle smokeClasses --no-daemon
LIBGL_ALWAYS_SOFTWARE=1 python3 ci/smoke_client.py
```

These are synthetic rendering tests, not a logged-in Hypixel playtest. Verify the changing
purse and current server-side Bazaar/AH formats in-game after installation.
