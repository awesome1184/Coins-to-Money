# Changelog

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
