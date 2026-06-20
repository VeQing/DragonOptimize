package com.dragoncore.optimize.dragoncore;

import com.dragoncore.optimize.DragonOptimize;
import net.minecraftforge.fml.common.Loader;

public final class DragonCoreCompat {

    private static boolean initialized = false;
    private static boolean dragonCoreLoaded = false;
    private static String detectedName = "";

    private DragonCoreCompat() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        try {
            if (Loader.isModLoaded("dragoncore") || Loader.isModLoaded("DragonCore")) {
                dragonCoreLoaded = true;
                detectedName = "dragoncore";
                DragonOptimize.LOGGER.info("[DragonOptimize] DragonCore detected, compat mode enabled.");
            } else {
                DragonOptimize.LOGGER.info("[DragonOptimize] DragonCore not detected.");
            }
        } catch (Throwable t) {
            DragonOptimize.LOGGER.warn("[DragonOptimize] DragonCore detection error (ignored): {}", t.toString());
        }
    }

    public static boolean isDragonCoreServer() {
        return dragonCoreLoaded;
    }

    public static String getDetectedName() {
        return detectedName;
    }
}
