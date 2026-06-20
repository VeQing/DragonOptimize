package com.dragoncore.optimize.config;

import com.dragoncore.optimize.DragonOptimize;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * DragonOptimize 的全局配置中心。
 *
 * <p>提供四类预设（PERFORMANCE / BALANCED / QUALITY / CUSTOM），用户可在 GUI 中自由切换。</p>
 */
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

    // ==== 通用 ====
    public static volatile Preset activePreset = Preset.BALANCED;

    // ==== 并行调度 ====
    /** 并行调度器的工作线程数；0 表示根据 CPU 自动选择。 */
    public static volatile int workerThreads = 0;
    /** 是否启用并行实体 tick。 */
    public static volatile boolean parallelEntityTick = true;
    /** 是否启用并行 TileEntity tick。 */
    public static volatile boolean parallelTileTick = true;
    /** 是否启用并行区块（chunk）tick。 */
    public static volatile boolean parallelChunkTick = true;
    /** AI 任务异步化开关。 */
    public static volatile boolean asyncEntityAI = true;
    /** 每个 tick 批次大小（控制每批提交给线程池的实体数量）。 */
    public static volatile int tickBatchSize = 64;

    // ==== 渲染 / 品质 ====
    /** 启用视距动态调整：低 FPS 时自动降低视距。 */
    public static volatile boolean dynamicRenderDistance = false;
    /** 限制粒子数量，性能档下会显著降低。 */
    public static volatile int particleLimit = 4000;
    /** 是否关闭实体阴影（性能）。 */
    public static volatile boolean disableEntityShadows = false;
    /** 是否启用光照缓存合并（仿 Phosphor 思路）。 */
    public static volatile boolean lightCacheMerge = true;
    /** 是否启用区块懒加载优化。 */
    public static volatile boolean lazyChunkLoad = true;

    // ==== 龙核兼容 ====
    /** 启用龙核协议检测。 */
    public static volatile boolean dragonCoreCompat = true;
    /** 与龙核服务器通信时的心跳间隔（秒）。 */
    public static volatile int dragonCoreHeartbeatSec = 15;

    // ==== GUI 快捷键 ====
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
            case CUSTOM:
            default:
                // 保持用户设置，不覆盖
                break;
        }
        activePreset = preset;
    }

    public static void load() {
        if (cfg == null) {
            return;
        }
        cfg.load();

        String presetName = cfg.getString("activePreset", Configuration.CATEGORY_GENERAL,
                Preset.BALANCED.name(), "当前启用的预设：PERFORMANCE / BALANCED / QUALITY / CUSTOM");
        try {
            activePreset = Preset.valueOf(presetName.toUpperCase());
        } catch (IllegalArgumentException e) {
            activePreset = Preset.BALANCED;
        }

        workerThreads = cfg.getInt("workerThreads", "scheduler", 0, 0, 32,
                "并行调度线程数；0 表示根据 CPU 自动选择。");
        parallelEntityTick = cfg.getBoolean("parallelEntityTick", "scheduler",
                true, "启用并行实体 tick。");
        parallelTileTick = cfg.getBoolean("parallelTileTick", "scheduler",
                true, "启用并行 TileEntity tick。");
        parallelChunkTick = cfg.getBoolean("parallelChunkTick", "scheduler",
                true, "启用并行区块 tick。");
        asyncEntityAI = cfg.getBoolean("asyncEntityAI", "scheduler",
                true, "启用 AI 任务异步化。");
        tickBatchSize = cfg.getInt("tickBatchSize", "scheduler",
                64, 8, 1024, "每 tick 提交给线程池的批次大小。");

        dynamicRenderDistance = cfg.getBoolean("dynamicRenderDistance", "render",
                false, "启用动态视距（FPS 低时降低）。");
        particleLimit = cfg.getInt("particleLimit", "render",
                4000, 0, 65535, "粒子数量上限。");
        disableEntityShadows = cfg.getBoolean("disableEntityShadows", "render",
                false, "关闭实体阴影以换取性能。");
        lightCacheMerge = cfg.getBoolean("lightCacheMerge", "render",
                true, "启用光照缓存合并。");
        lazyChunkLoad = cfg.getBoolean("lazyChunkLoad", "render",
                true, "启用区块懒加载优化。");

        dragonCoreCompat = cfg.getBoolean("dragonCoreCompat", "dragoncore",
                true, "启用龙核服务器兼容层。");
        dragonCoreHeartbeatSec = cfg.getInt("dragonCoreHeartbeatSec", "dragoncore",
                15, 1, 120, "与龙核服务器通信时的心跳间隔（秒）。");

        guiHotkey = cfg.getString("guiHotkey", "client", "key.keyboard.o",
                "打开 DragonOptimize 面板的快捷键（参照 Minecraft GLFW key 名称）。");

        // 若预设为非 CUSTOM，则根据预设值同步参数
        if (activePreset != Preset.CUSTOM) {
            applyPreset(activePreset);
        }

        if (cfg.hasChanged()) {
            cfg.save();
        }
    }

    public static void save() {
        if (cfg == null) {
            return;
        }
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
            // 同步写一份 JSON 供 Web UI / 外部工具使用
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
