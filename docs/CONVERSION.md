# Conversion basis

This implementation intentionally uses the requested **$100 USD / 11,000 gems** comparison
basis. It is a custom fixed rate, not a live Hypixel Store price or a claim that this is the
cheapest currently available gem bundle.

Hypixel announced different gem bundles in April 2026, including 17,000 gems for $120 USD;
the announcement confirms that Booster Cookies still cost 325 gems. The requested 11,000-gem
basis is retained rather than silently changed. Only the **Bazaar coin price** updates live.

Official announcement:
https://hypixel.net/threads/skyblock-gem-price-increase-april-2026.6084009/

The code uses `325 * 100 / 11000` USD per cookie, approximately $2.954545, without intermediate
rounding. It divides any recognized coin amount by the lowest available instant-buy offer
for one cookie. It does not calculate bulk order-book slippage, integer gem-bundle purchases,
leftover gems, promotions or taxes. The displayed count is a fractional cookie equivalent.
