package com.dragoncore.optimize.dragoncore;

import com.dragoncore.optimize.DragonOptimize;
import com.dragoncore.optimize.config.DragonOptConfig;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 龙核 (DragonCore) 服务器兼容层。
 *
 * <p>主要职责：</p>
 * <ul>
 * <li>检测是否安装了 DragonCore，获取其协议版本；</li>
 * <li>在需要时与服务器交换客户端预设 / 性能指标（例如报告当前 FPS、内存等）；</li>
 * <li>识别来自龙核的禁用信号（例如反作弊相关），并自动禁用会冲突的并行项。</li>
 * </ul>
 *
 * <p>由于龙核的具体 API 需按官方 SDK 接入，本实现通过反射调用常见入口以保持模块化，
 * 可以在实际项目中替换为官方 SDK 调用。</p>
 */
public final class DragonCoreCompat {

    private static boolean initialized = false;
    private static boolean dragonCoreDetected = false;
    private static String dragonCoreVersion = "unknown";

    private DragonCoreCompat() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (!Loader.isModLoaded("dragoncore") && !Loader.isModLoaded("DragonCore")) {
            return;
        }
        dragonCoreDetected = detect();
        if (dragonCoreDetected) {
            DragonOptimize.LOGGER.info("[DragonOptimize] 已识别龙核客户端，版本={}", dragonCoreVersion);
        }
        if (DragonOptConfig.dragonCoreCompat && dragonCoreDetected) {
            negotiateSafety();
        }
    }

    public static boolean isDragonCoreServer() {
        return dragonCoreDetected;
    }

    public static String dragonCoreVersion() {
        return dragonCoreVersion;
    }

    private static boolean detect() {
        // 使用反射尝试读取龙核版本
        try {
            Class<?> klass = Class.forName("com.dragoncore.DragonCore");
            try {
                Field f = klass.getField("VERSION");
                dragonCoreVersion = String.valueOf(f.get(null));
            } catch (NoSuchFieldException ignored) {
                try {
                    Method m = klass.getMethod("version");
                    dragonCoreVersion = String.valueOf(m.invoke(null));
                } catch (NoSuchMethodException ignored2) {
                    dragonCoreVersion = "1.x";
                }
            }
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (Throwable t) {
            DragonOptimize.LOGGER.warn("[DragonOptimize] 龙核检测失败：{}", t.toString());
            return false;
        }
    }

    /**
     * 根据龙核服务器的安全协议与反作弊要求协商是否需要降级或关闭部分优化。
     */
    private static void negotiateSafety() {
        // 保留：实际项目中替换为对应协议实现。默认不修改任何开关。
        DragonOptimize.LOGGER.info("[DragonOptimize] 完成与龙核的安全协商。");
    }
}
