# Changelog

## 1.2.7

- Ported the sidebar, settings screens and team colours to Minecraft 26.2.
- Updated Fabric API, Mod Menu and optional mod integrations for 26.2.
- This build requires Minecraft 26.2; older game versions should use an earlier release.
- Simplified the README while preserving the creator's opening section.
- Added awesome1184 as original creator and veney as main developer in the README and mod metadata.

## 1.2.3

- CustomScoreboard money and cookie values now use its read-only, unrounded purse API instead
  of parsing its localized/compact display. Original coin text, gains and layout are retained.
- Support both Component and legacy-string number arrangements, chunked purse and Hypixel Lines.
- Keep conversion/quote refresh active using the optional mods' native SkyBlock context as well
  as the vanilla sidebar, including when a replacement sidebar is in use.
- Add optional SkyHanni coin formatter, dungeon/Kuudra chest profit, shared tracker totals and
  money/hour, crop-money cells, and labelled profit renderable integration. Never hook generic numbers.
- Measure and render the same projected text, preserve source text and signed calculations,
  and request the native self-clearing tracker update when settings or quotes change.
- Add a SkyHanni profits toggle, preserve green equivalents and non-coin resource safeguards.
- Expand diagnostics with installed versions and adapter call/change counters.
- Test the published CustomScoreboard 1.12.14-2 and SkyHanni 7.56.0 builds separately and together,
  plus a client without either optional mod. No optional dependency or fixture is bundled.

## 1.2.2

- Stops treating Mana Cost, Soulflow Cost and other compound resource labels as coin prices.
- Bare price fields reject resource units, stat glyphs, percentages and other ambiguous suffixes.
- Explicit coin prices remain supported, including mixed coins/resource purchases.
- Adds optional meowdding CustomScoreboard purse/piggy, chunked purse and Hypixel Lines integration before layout; no data mutation or global text hook.
- Keeps money/cookies green, gain text intact, existing display options and no standalone HUD.
- Adds price-context regressions and real-client tests with and without published CustomScoreboard 1.12.11.

## 1.2.1

- Fixes the exact live purse row containing the unsupported `§p` marker inside `7,416,701`.
- Uses Minecraft's own formatted-text decoder for both matching and styled replacements,
  including unknown codes and component boundaries; no scoreboard arithmetic workaround.
- Preserves the original gold coins, green equivalents, yellow gain suffix and blank score.
- Diagnostics now report the installed version and the decoded visible text/parsed purse.
- Adds exact-diagnostic unit regressions and real-client BlankFormat/team-owner rendering checks.

## 1.2.0

- Recover a split purse with an incomplete comma group even when its visible tail uses
  StyledFormat, not FixedFormat. Complete amounts still never absorb numeric ordering scores.
- Match vanilla's team-colour sidebar selection as well as the default SIDEBAR slot.
- Add clipboard-only purse diagnostics: actual displayed columns, format types, Unicode
  codepoints, conversion output, settings and whether the render hook ran. No upload/telemetry.
- Explain every setting and input through hover tooltips.
- Add all six coin/money/cookie orders and four layouts: brackets, parentheses, bars, equals.
- Restore green money and cookie text while retained coin amounts keep their own styles.
- Add manual real-world currency codes and a validated target-units-per-USD multiplier.
  Currency edits apply atomically; cancel discards changes. No FX API or invented rates.
- Extend unit regressions and real-client testing for StyledFormat, coloured sidebars,
  all order/layout/visibility combinations, green output, tooltips and currency editing.

## 1.1.1

- USD-only replacement is now the default; removes the original coin unit as well as its number.
- Settings control USD, cookies, keeping coins, display precision, and individual surfaces.
- Removes the standalone cookie-rate HUD, including old saved `showGui` behavior.
- Converts the complete vanilla sidebar row, including `FixedFormat` value text and split digits;
  no longer hooks only the team/name portion or guesses digits from the ordering integer.
- Fixes settings-screen double blur by leaving background extraction to vanilla's screen wrapper.
- Adds screenshot-derived text regressions and real-client sidebar/settings rendering tests.

## 1.1.0

- Added live Booster Cookie instant-buy quotes and cookie/USD equivalents.
- Used the requested 325 gems per cookie and 11,000 gems / $100 comparison basis.
- Added tooltip and server-message integration, configurable surfaces and a rate HUD.
- Added rate validation, asynchronous refreshes, stale/expired quote handling and regression tests.
- Replaced the old partial-number arithmetic with team/name conversion. This still missed values
  in the separate formatted score column; corrected in 1.1.1.
