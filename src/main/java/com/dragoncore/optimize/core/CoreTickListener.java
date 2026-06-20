package com.dragoncore.optimize.core;

import com.dragoncore.optimize.config.DragonOptConfig;
import com.dragoncore.optimize.render.LazyChunkLoader;
import com.dragoncore.optimize.scheduler.ParallelTickScheduler;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

public class CoreTickListener {

    private final ParallelTickScheduler scheduler;
    private final LazyChunkLoader chunkLoader = new LazyChunkLoader();
    private int tickCounter;

    public CoreTickListener(ParallelTickScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld() != null && !event.getWorld().isRemote) {
            scheduler.resume();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        scheduler.pause();
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.world == null) return;
        if (event.phase == TickEvent.Phase.START) {
            scheduler.beginTick();
            chunkLoader.tick();
        } else if (event.phase == TickEvent.Phase.END) {
            scheduleParallelHelpers(event.world);
            scheduler.awaitTick(50, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        tickCounter++;
        if (tickCounter % 20 == 0) {
        }
    }

    private void scheduleParallelHelpers(World world) {
        if (!DragonOptConfig.parallelEntityTick && !DragonOptConfig.parallelTileTick) return;
        if (DragonOptConfig.parallelEntityTick) {
            List<Entity> entities = world.loadedEntityList;
            if (entities != null && !entities.isEmpty()) {
                scheduler.runBatched(entities, e -> {
                    if (e == null) return;
                    e.isEntityAlive();
                });
            }
        }
        if (DragonOptConfig.parallelTileTick) {
            List<TileEntity> tes = world.loadedTileEntityList;
            if (tes != null && !tes.isEmpty()) {
                scheduler.runBatched(tes, te -> {
                    if (te == null) return;
                });
            }
        }
    }
}
