package com.dragoncore.optimize.core;

import com.dragoncore.optimize.DragonOptimize;
import com.dragoncore.optimize.config.DragonOptConfig;
import com.dragoncore.optimize.scheduler.ParallelTickScheduler;
import net.minecraftforge.common.MinecraftForge;

/**
 * DragonOptimize 核心模块，负责初始化、启动与销毁。
 */
public class DragonOptimizeCore {

    private ParallelTickScheduler tickScheduler;

    public void bootstrap() {
        int threads = DragonOptConfig.workerThreads;
        if (threads <= 0) {
            threads = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        }
        tickScheduler = new ParallelTickScheduler(threads);
        MinecraftForge.EVENT_BUS.register(new CoreTickListener(tickScheduler));
        DragonOptimize.LOGGER.info("[DragonOptimize] 并行调度器启动，工作线程数={}", threads);
    }

    public void onServerStarting() {
        if (tickScheduler != null) {
            tickScheduler.resume();
        }
    }

    public void onServerStopping() {
        if (tickScheduler != null) {
            tickScheduler.shutdown();
        }
    }

    public ParallelTickScheduler scheduler() {
        return tickScheduler;
    }
}
