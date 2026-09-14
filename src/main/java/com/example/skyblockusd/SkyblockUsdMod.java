package com.example.skyblockusd;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SkyblockUsdMod implements ModInitializer {
    public static final String MOD_ID = "coins-to-money";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    @Override public void onInitialize() {
        ModConfig.load();
        LOGGER.info("Coins to Money: 11,000 gems / $100 USD, 325 gems per cookie");
    }
}
