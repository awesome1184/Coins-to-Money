# Changelog

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

Still targets Minecraft 26.1.2 / Java 25. The new default is non-destructive annotations rather
than replacing coins with dollars. See README for exact coverage and live-server limitations.
