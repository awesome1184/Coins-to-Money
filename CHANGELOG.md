# Changelog

## 1.1.1

- Replace coin prices with dollars in place; remove the `coins` unit. Cookie count is optional and OFF by default.
- Remove the top-left HUD and its settings, even with a saved `showGui: true` configuration.
- Fix the missing purse path by handling BOTH visible sidebar columns, including `FixedFormat` values and split digits, at the vanilla display-entry constructor. Recalculate the score width; never mutate ordering scores or team data.
- Remove the old global team-name hook.
- Fix the settings-screen double-background-blur crash.
- Add regressions for formatted score columns, real enabled mixins, tooltip replacement, settings rendering and returning to the parent screen. Render fixtures are excluded from the production JAR.

## 1.1.0

- Parse the complete team-formatted sidebar row before width calculation. Remove partial
  rendering conversions, numeric stripping, and the `partial * 100 + score` heuristic.
  Raw scoreboard ordering scores are neither incorporated into balances nor suppressed.
- Add fractional Booster Cookie counts and USD equivalents based on $100 / 11,000 gems.
- Use the actual lowest instant-buy cookie offer instead of the weighted quick-status rate.
- Add item/Bazaar/AH/NPC tooltip and server chat/action-bar annotations, plus a cookie/USD HUD.
- Reject malformed/truncated coin amounts; preserve original coin text and components.
- Fetch asynchronously with bounded IO, quote validation, visible stale status and expiry.
- Add automated regression tests and a real Fabric client startup smoke test.

Still targets Minecraft 26.1.2 / Java 25. Version 1.1.0 introduced non-destructive annotations;
1.1.1 restores the requested dollar replacement. See README for coverage and test limitations.
