package com.dragoncore.optimize.core;

import com.dragoncore.optimize.config.DragonOptConfig;
import com.dragoncore.optimize.render.LightingCache;
import com.dragoncore.optimize.scheduler.ParallelTickScheduler;
import net.minecraftforge.common.MinecraftForge;

public class DragonOptimizeCore {

    private ParallelTickScheduler tickScheduler;

    public void bootstrap() {
        int threads = DragonOptConfig.workerThreads;
        if (threads <= 0) {
            threads = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        }
        tickScheduler = new ParallelTickScheduler(threads);
        ParallelTickScheduler.installGlobal(tickScheduler);
        MinecraftForge.EVENT_BUS.register(new CoreTickListener(tickScheduler));
        MinecraftForge.EVENT_BUS.register(new LightingCache());
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
