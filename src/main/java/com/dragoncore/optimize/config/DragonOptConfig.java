package com.dragoncore.optimize.config;

import com.dragoncore.optimize.DragonOptimize;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

public final class DragonOptConfig {

    public enum Preset {
        PERFORMANCE("性能优先"),
        BALANCED("平衡"),
        QUALITY("品质优先"),
        CUSTOM("自定义");

        private final String display;

        Preset(String display) {
            this.display = display;
        }

        public String display() {
            return display;
        }
    }

    private static Configuration cfg;
    private static File cfgFile;

    public static volatile Preset activePreset = Preset.BALANCED;
    public static volatile int workerThreads = 0;
    public static volatile boolean parallelEntityTick = true;
    public static volatile boolean parallelTileTick = true;
    public static volatile boolean parallelChunkTick = true;
    public static volatile boolean asyncEntityAI = true;
    public static volatile int tickBatchSize = 64;
    public static volatile boolean dynamicRenderDistance = false;
    public static volatile int particleLimit = 4000;
    public static volatile boolean disableEntityShadows = false;
    public static volatile boolean lightCacheMerge = true;
    public static volatile boolean lazyChunkLoad = true;
    public static volatile boolean dragonCoreCompat = true;
    public static volatile int dragonCoreHeartbeatSec = 15;
    public static volatile String guiHotkey = "key.keyboard.o";

    private DragonOptConfig() {
    }

    public static void init(File suggestedConfigFile) {
        cfgFile = new File(suggestedConfigFile.getParentFile(), "dragonoptimize.cfg");
        cfg = new Configuration(cfgFile);
        load();
    }

    public static Preset currentPreset() {
        return activePreset;
    }

    public static void applyPreset(Preset preset) {
        switch (preset) {
            case PERFORMANCE:
                workerThreads = Math.max(2, Runtime.getRuntime().availableProcessors() - 2);
                parallelEntityTick = true;
                parallelTileTick = true;
                parallelChunkTick = true;
                asyncEntityAI = true;
                tickBatchSize = 128;
                dynamicRenderDistance = true;
                particleLimit = 800;
                disableEntityShadows = true;
                lightCacheMerge = true;
                lazyChunkLoad = true;
                break;
            case BALANCED:
                workerThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
                parallelEntityTick = true;
                parallelTileTick = true;
                parallelChunkTick = true;
                asyncEntityAI = true;
                tickBatchSize = 64;
                dynamicRenderDistance = false;
                particleLimit = 4000;
                disableEntityShadows = false;
                lightCacheMerge = true;
                lazyChunkLoad = true;
                break;
            case QUALITY:
                workerThreads = Math.max(2, Runtime.getRuntime().availableProcessors());
                parallelEntityTick = false;
                parallelTileTick = false;
                parallelChunkTick = false;
                asyncEntityAI = false;
                tickBatchSize = 32;
                dynamicRenderDistance = false;
                particleLimit = 16000;
                disableEntityShadows = false;
                lightCacheMerge = false;
                lazyChunkLoad = false;
                break;
        }
        activePreset = preset;
    }

    public static void load() {
        if (cfg == null) return;
        cfg.load();

        String presetName = cfg.getString("activePreset", Configuration.CATEGORY_GENERAL,
                Preset.BALANCED.name(), "");
        try {
            activePreset = Preset.valueOf(presetName.toUpperCase());
        } catch (IllegalArgumentException e) {
            activePreset = Preset.BALANCED;
        }

        workerThreads = cfg.getInt("workerThreads", "scheduler", 0, 0, 32, "");
        parallelEntityTick = cfg.getBoolean("parallelEntityTick", "scheduler", true, "");
        parallelTileTick = cfg.getBoolean("parallelTileTick", "scheduler", true, "");
        parallelChunkTick = cfg.getBoolean("parallelChunkTick", "scheduler", true, "");
        asyncEntityAI = cfg.getBoolean("asyncEntityAI", "scheduler", true, "");
        tickBatchSize = cfg.getInt("tickBatchSize", "scheduler", 64, 8, 1024, "");

        dynamicRenderDistance = cfg.getBoolean("dynamicRenderDistance", "render", false, "");
        particleLimit = cfg.getInt("particleLimit", "render", 4000, 0, 65535, "");
        disableEntityShadows = cfg.getBoolean("disableEntityShadows", "render", false, "");
        lightCacheMerge = cfg.getBoolean("lightCacheMerge", "render", true, "");
        lazyChunkLoad = cfg.getBoolean("lazyChunkLoad", "render", true, "");

        dragonCoreCompat = cfg.getBoolean("dragonCoreCompat", "dragoncore", true, "");
        dragonCoreHeartbeatSec = cfg.getInt("dragonCoreHeartbeatSec", "dragoncore", 15, 1, 120, "");

        guiHotkey = cfg.getString("guiHotkey", "client", "key.keyboard.o", "");

        if (activePreset != Preset.CUSTOM) {
            applyPreset(activePreset);
        }

        if (cfg.hasChanged()) {
            cfg.save();
        }
    }

    public static void save() {
        if (cfg == null) return;
        setProp(Configuration.CATEGORY_GENERAL, "activePreset", activePreset.name());

        setInt("scheduler", "workerThreads", workerThreads);
        setBool("scheduler", "parallelEntityTick", parallelEntityTick);
        setBool("scheduler", "parallelTileTick", parallelTileTick);
        setBool("scheduler", "parallelChunkTick", parallelChunkTick);
        setBool("scheduler", "asyncEntityAI", asyncEntityAI);
        setInt("scheduler", "tickBatchSize", tickBatchSize);

        setBool("render", "dynamicRenderDistance", dynamicRenderDistance);
        setInt("render", "particleLimit", particleLimit);
        setBool("render", "disableEntityShadows", disableEntityShadows);
        setBool("render", "lightCacheMerge", lightCacheMerge);
        setBool("render", "lazyChunkLoad", lazyChunkLoad);

        setBool("dragoncore", "dragonCoreCompat", dragonCoreCompat);
        setInt("dragoncore", "dragonCoreHeartbeatSec", dragonCoreHeartbeatSec);

        setProp("client", "guiHotkey", guiHotkey);

        cfg.save();
        try {
            File jsonFile = new File(cfgFile.getParentFile(), "dragonoptimize.json");
            FileUtils.writeStringToFile(jsonFile, JsonConfigExporter.toJson(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private static void setBool(String cat, String key, boolean value) {
        Property p = cfg.get(cat, key, value);
        p.set(value);
    }

    private static void setInt(String cat, String key, int value) {
        Property p = cfg.get(cat, key, value);
        p.set(value);
    }

    private static void setProp(String cat, String key, String value) {
        Property p = cfg.get(cat, key, value);
        p.set(value);
    }

    public static Configuration configuration() {
        return cfg;
    }
}
