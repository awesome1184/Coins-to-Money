Have you ever felt like you overspend in SkyBlock? No more! This mod is here to guilt-trip you into reconsidering your every purchase by equating each coin loss and gain to a real-world money gain/loss.

Features -

    Translates your purchases from SkyBlock coins into the USD equivalent
    It also shows how many cookies an item is worth
    Support for any currency
    Customizable precision

Press K to open the settings and 0 to disable the mod (Mod Menu isnt a dependency due to this) (these keybinds can be edited)

110 gems = 1 USD 1 cookie = about 3 dollars


<img width="434" height="300" alt="image" src="https://github.com/user-attachments/assets/17286502-5895-48fc-8d5f-53a84484e6c7" />
















CLANKER'S DOCUMENTATION BELOW, DON'T READ IF YOU'RE ALLERGIC TO AI SLOP
# Coins to Money

Coins to Money is a client-side Fabric mod for Hypixel SkyBlock. It puts coin prices into perspective by showing their value in real money or Booster Cookies. It changes what you see, not your balance or the prices you pay in-game.

## Installation

Download the JAR from [GitHub Releases](https://github.com/awesome1184/Coins-to-Money/releases) and put it in your Minecraft instance's `mods` folder alongside [Fabric API](https://modrinth.com/mod/fabric-api). Remove the previous Coins to Money JAR when updating; your saved settings will carry over.

The current release is for **Minecraft 26.2**, with **Java 25**, **Fabric Loader 0.19.5 or newer**, and **Fabric API 0.160.0+26.2 or newer for 26.2**. Use an older release for Minecraft 26.1.2. [Mod Menu](https://modrinth.com/mod/modmenu) is optional.

## Using the mod

Press **K** to open settings or **O** to turn conversion on and off. Both keys can be changed in Minecraft's controls. Hover over a setting for an explanation.

Money is shown by default. You can also show cookies, keep the original coin amount, or combine all three. Choose which comes first and whether the values use brackets, parentheses, bars, or equals signs. Money and cookies appear in green; original coins keep their existing colour. Decimal precision is adjustable for money and cookies separately.

Conversion works in the purse sidebar, Bazaar and Auction House tooltips, NPC prices, and server messages containing coin amounts. Sidebar, tooltip, chat, and SkyHanni conversion can each be switched off separately. Mana, Soulflow, health, item counts, and other non-coin values are left alone.

## How the values are calculated

The mod checks the Bazaar's current Booster Cookie instant-buy price once a minute while it is enabled in SkyBlock. It divides the coin amount by that price to get the cookie equivalent, then uses **325 gems per cookie** and **11,000 gems for $100** to calculate dollars. On that comparison basis, one cookie is about **$2.95**.

This is a reference cost, not a cash-out value or a claim about today's cheapest gem bundle. If a cookie price is unavailable, the original text stays visible. An older quote is marked stale until it can be refreshed or expires.

## Other currencies

Open **Currency** in the settings, enter a code such as `EUR`, `GBP`, `JPY`, or `RSD`, and set how many units of that currency equal one US dollar. Press **Apply** to save. For example, entering `EUR` and a hypothetical rate of `0.90` would turn a $10 equivalent into €9.00.

Currency rates are entered manually and saved locally; they do not update automatically. The Bazaar cookie price still updates on its own. No API key is needed.

## CustomScoreboard and SkyHanni

[CustomScoreboard](https://github.com/meowdding/CustomScoreboard) integration supports Purse and Piggy displays, compact and localized numbers, different label placements, chunked purse values, and Hypixel Lines. It keeps CustomScoreboard's layout and uses the underlying purse balance for the conversion rather than estimating from an abbreviated number.

[SkyHanni](https://github.com/hannibal002/SkyHanni) integration covers supported chest-profit breakdowns, tracker totals, profit-per-hour displays, and crop-money values. It changes the displayed amounts without changing SkyHanni's calculations. Some cached overlays may need reopening after you change display settings.

Both integrations activate automatically when the corresponding mod is installed. Use the **26.2 versions** of those mods and install their usual dependencies; neither mod is bundled with Coins to Money. The supported builds for this release are **CustomScoreboard 1.12.14** and **SkyHanni 7.57.0**.

If a purse display remains unchanged, open settings and use **Copy purse diagnostics** when reporting the issue. This copies the displayed balances and relevant mod details to your clipboard; it does not upload anything.

## Credits

**awesome1184** — Original creator.

**veney** — Main "developer" (we just used his chatgpt pro subscription to get it done lol)
