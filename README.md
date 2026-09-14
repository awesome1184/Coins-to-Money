# Coins to Money

Client-side Fabric mod for **Minecraft 26.1.2 / Java 25**, originally by awesome1184.
Replaces SkyBlock coin amounts with their USD equivalents. Cookie counts are optional; there is no HUD overlay.

## Conversion

```
cookies = coins / current cookie instant-buy price
USD = cookies * 325 * (100 / 11000)
```

The requested basis is **11,000 gems for $100 USD** and **325 gems per Booster Cookie**, approximately $2.954545 per cookie. Fractions are retained until display rounding. This is a replacement-cost comparison, not a cash-out value. The fixed gem-bundle basis is explained in [docs/CONVERSION.md](docs/CONVERSION.md).

The public Bazaar endpoint is queried once per minute while enabled in SkyBlock. The mod selects the cheapest available `BOOSTER_COOKIE.buy_summary` offer with at least one cookie, not the weighted `quick_status.buyPrice` average. No API key or profile API is required. Bulk-order slippage and taxes are not included.

## Display and controls

- Purse, Piggy Bank and bank/balance sidebar rows, including the separate formatted score column.
- Bazaar, Auction House, NPC and other recognized coin prices in item tooltips. Amounts and the `coins` unit are replaced in place, e.g. `Worth $3.27`.
- Server chat and action-bar coin amounts, retaining surrounding style and click/hover metadata. Signed player-chat events are not modified.

**K** opens settings; **O** toggles the mod. Both keys are rebindable. Mod Menu is optional.
Cookie counts are OFF by default and can be enabled in settings; USD precision is configurable from 2 to 8 decimals. The top-left HUD has been removed completely. Saved `showGui`, `guiX` and `guiY` settings are ignored, so upgrading does not require deleting the config.

The mod requires a `SKYBLOCK` sidebar on `hypixel.net` or a subdomain, including team-specific sidebar slots. Other currencies, item counts, stats and malformed currency amounts are left alone. Custom renderers supplied by other mods are not guaranteed to be covered. Server-rounded amounts such as `1.2m` cannot be made more precise.

## Failure behaviour

No fabricated fallback price is used. Until a valid rate is available, original text stays unchanged. Quotes are marked `(stale)` after three minutes or immediately after an API error, and expire after 30 minutes. Requests run off the rendering thread with timeouts and response-size limits. Legacy saved exchange rates are not treated as current prices.

## Build and tests

Install JDK 25 and Gradle 9.5.1:

```sh
gradle clean build --no-daemon
```

Install `build/libs/coins-to-money-1.1.1.jar` with Fabric Loader 0.19.5+ and Fabric API 0.155.3+26.1.2. Do not install the sources JAR or an older copy alongside it.

For rendering tests, install Xvfb, Mesa and ImageMagick, then run:

```sh
xvfb-run -a python3 ci/smoke_client.py
```

JUnit covers parsing, USD arithmetic, rate validation and expiry, styled text replacement, complete and split formatted-score values, objective-level number formats, leading zeros, large balances, raw ordering-score preservation, repeat conversion and non-coin values.

A separate test mod exercises the actual enabled sidebar and tooltip hooks, checks measured score widths, renders a synthetic sidebar/tooltip and the real settings screen with blur enabled, then verifies returning to its parent. CI publishes the JAR, test reports, logs and screenshots. Test code is excluded from the release JAR.

The settings screen relies on Minecraft's existing background extraction instead of requesting blur twice. The sidebar hook runs at `Gui.lambda$displayScoreboardSidebar$1` after both visible components are assembled, before layout; no global team-name hook or `partial * 100 + score` guess remains.

These are synthetic rendering tests, **not a logged-in Hypixel playtest**. Live-server verification still means comparing a changing purse, Bazaar/AH tooltips and toggles against the unmodified game.
