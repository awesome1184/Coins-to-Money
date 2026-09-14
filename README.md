# Coins to Money

Client-side Fabric mod for **Minecraft 26.1.2 / Java 25**, originally by awesome1184.
Keeps SkyBlock's coin amounts visible and adds their Booster Cookie and USD equivalents.

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

## Where it appears

- Full purse / Piggy Bank / bank sidebar rows, after the team prefix, name and suffix are joined.
- Item tooltips, including Bazaar, Auction House, NPC shops, bids and costs expressed as coins
  or recognized price labels. Multiple prices receive separate, labelled equivalents.
- Server chat and action-bar coin amounts, retaining the original message components.
- A HUD with the purse's cookie/USD equivalent, coin price per cookie and USD basis.

The mod activates for a `SKYBLOCK` sidebar on `hypixel.net` or its subdomains. It does not
rewrite unrelated item stats, quantities, gems, or player chat. Numbers drawn directly by
other mods without vanilla components/tooltips are not guaranteed to be covered. It cannot
recover precision already rounded by the server (e.g. a displayed `1.2m`).

**K** opens settings; **O** toggles the mod. Both are rebindable. Mod Menu is optional.
HUD coordinates are `guiX` and `guiY` in `config/coins-to-money.json`.

## Failure behaviour

No fabricated fallback price is used. Before the first valid response, the HUD says loading
or unavailable and original coin text stays untouched. Last-good quotes are marked stale
after three minutes or immediately after an API error; they expire after 30 minutes.
Requests have timeouts and response-size limits, use one daemon worker, and never block
rendering. The previous world's purse is cleared on leaving SkyBlock or losing the purse row.
Legacy saved exchange-rate fields are not treated as current prices.

## Build and test

Install JDK 25 and Gradle 9.5.1, then run:

```sh
gradle clean build --no-daemon
```

Install `build/libs/coins-to-money-1.1.0.jar` with Fabric Loader 0.19.5+ and Fabric API
0.155.3+26.1.2. Do not install the sources JAR. CI publishes the mod JAR and test reports as
workflow artifacts, and boots the real client under Xvfb to check entrypoints and mixins.

Regression tests cover the reported `7,014,5` + `56` truncation, real team assembly, raw score
preservation, formatting between digits, comma/decimal/suffix parsing, malformed numbers,
duplicate annotations, multiple prices, non-coin currencies, price-side selection, USD
arithmetic, stale/missing API responses, component preservation, and locale independence.

The startup smoke test is not a live-server test. The remaining manual check is to join
Hypixel, compare a changing purse to the full vanilla amount, hover Bazaar/AH/NPC prices,
check chat, toggle each setting, and compare the cookie quote with a one-cookie instant buy.
Sidebar-replacement mods may require their own integration.
