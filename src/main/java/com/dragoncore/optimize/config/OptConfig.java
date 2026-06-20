package com.dragoncore.optimize.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class OptConfig {

    public static final String CFG_FILENAME = "dragonoptimize.properties";

    // 城市渲染优化
    public static volatile boolean cityRenderOptimizer = true;
    public static volatile int cityMaxRenderedPlayers = 30;
    public static volatile int cityEmergencyRenderedPlayers = 12;
    public static volatile int cityPlayerNearDistance = 12;
    public static volatile int cityPlayerFarDistance = 28;
    public static volatile int cityLowFpsThreshold = 30;
    public static volatile int cityEmergencyRenderDistance = 6;

    // 其他优化开关（预留，默认关闭，避免依赖未实现的类）
    public static volatile boolean particleLimiter = true;
    public static volatile int particleCap = 4000;

    // 配置文件位置：由 main 模块在加载时注入
    private static File configDir;

    public static void setConfigDir(File dir) {
        configDir = dir;
    }

    public static void load() {
        File cfg = getConfigFile();
        if (cfg == null) return;
        if (!cfg.exists()) {
            save();
            return;
        }
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(cfg)) {
            p.load(fis);
            cityRenderOptimizer = bool(p.getProperty("cityRenderOptimizer"), true);
            cityMaxRenderedPlayers = num(p.getProperty("cityMaxRenderedPlayers"), 30);
            cityEmergencyRenderedPlayers = num(p.getProperty("cityEmergencyRenderedPlayers"), 12);
            cityPlayerNearDistance = num(p.getProperty("cityPlayerNearDistance"), 12);
            cityPlayerFarDistance = num(p.getProperty("cityPlayerFarDistance"), 28);
            cityLowFpsThreshold = num(p.getProperty("cityLowFpsThreshold"), 30);
            cityEmergencyRenderDistance = num(p.getProperty("cityEmergencyRenderDistance"), 6);
            particleLimiter = bool(p.getProperty("particleLimiter"), true);
            particleCap = num(p.getProperty("particleCap"), 4000);
        } catch (IOException ignored) {
            // 配置读取失败则使用默认值，不影响游戏
        }
    }

    public static void save() {
        File cfg = getConfigFile();
        if (cfg == null) return;
        try {
            if (cfg.getParentFile() != null && !cfg.getParentFile().exists()) {
                cfg.getParentFile().mkdirs();
            }
            Properties p = new Properties();
            p.setProperty("cityRenderOptimizer", String.valueOf(cityRenderOptimizer));
            p.setProperty("cityMaxRenderedPlayers", String.valueOf(cityMaxRenderedPlayers));
            p.setProperty("cityEmergencyRenderedPlayers", String.valueOf(cityEmergencyRenderedPlayers));
            p.setProperty("cityPlayerNearDistance", String.valueOf(cityPlayerNearDistance));
            p.setProperty("cityPlayerFarDistance", String.valueOf(cityPlayerFarDistance));
            p.setProperty("cityLowFpsThreshold", String.valueOf(cityLowFpsThreshold));
            p.setProperty("cityEmergencyRenderDistance", String.valueOf(cityEmergencyRenderDistance));
            p.setProperty("particleLimiter", String.valueOf(particleLimiter));
            p.setProperty("particleCap", String.valueOf(particleCap));
            try (FileOutputStream fos = new FileOutputStream(cfg)) {
                p.store(fos, "DragonOptimize config");
            }
        } catch (IOException ignored) {
            // 写配置失败也不影响游戏运行
        }
    }

    private static File getConfigFile() {
        if (configDir != null) return new File(configDir, CFG_FILENAME);
        // 回退：在当前工作目录下的 config 子目录
        return new File("config", CFG_FILENAME);
    }

    private static boolean bool(String v, boolean def) {
        if (v == null) return def;
        return Boolean.parseBoolean(v.trim());
    }

    private static int num(String v, int def) {
        if (v == null) return def;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
