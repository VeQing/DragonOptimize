package com.dragoncore.optimize.core;

import com.dragoncore.optimize.config.DragonOptConfig;
import com.dragoncore.optimize.scheduler.ParallelTickScheduler;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 将 Forge tick 事件桥接到 DragonOptimize 并行调度器。
 *
 * <p>注意：真正的并行化需要配合 {@code DragonOptimizeLoadingPlugin} 中的 ASM 改造。
 * 这里只负责：</p>
 * <ul>
 * <li>在世界加载时注入全局调度器；</li>
 * <li>在每个 tick 结束后调用 {@link ParallelTickScheduler#awaitTick()}，避免跨 tick 竞态；</li>
 * <li>当预设切换时保存配置。</li>
 * </ul>
 */
public class CoreTickListener {

    private final ParallelTickScheduler scheduler;

    public CoreTickListener(ParallelTickScheduler scheduler) {
        this.scheduler = scheduler;
        ParallelTickScheduler.installGlobal(scheduler);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld() == null || event.getWorld().isRemote) {
            return;
        }
        scheduler.resume();
    }

    @SubscribeEvent
    public void onWorldTickEnd(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        World world = event.world;
        if (world == null || world.isRemote) {
            return;
        }
        if (!DragonOptConfig.parallelEntityTick && !DragonOptConfig.parallelTileTick && !DragonOptConfig.parallelChunkTick) {
            return;
        }
        // 等待当前 tick 的并行任务完成，再让主线程继续下一 tick
        ParallelTickScheduler.awaitTickGlobal();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // 预留：客户端视距 / 粒子动态调整
    }
}
