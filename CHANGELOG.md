# Changelog

## 1.2.2

- Stops matching the suffix of Mana Cost, Soulflow Cost and other non-coin stat labels.
- Generic costs now require an explicit coins unit; implicit price labels must be complete fields
  and unknown units/icons/percentages are not assumed to be coins.
- Adds optional CustomScoreboard purse/Piggy integration for its four number formats,
  chunked purse and vanilla-lines mode before widget width/layout calculation.
- Uses CustomScoreboard's raw purse source for conversion, retaining its original localized or
  compact coin display, labels and gain suffix. No server/API balances are changed.
- Keeps green equivalents, all user-selected layouts and the existing vanilla purse fix.
- CI tests with and without the published CustomScoreboard 1.12.14-2 JAR and its dependencies.

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
