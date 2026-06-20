package com.dragoncore.optimize.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

final class JsonConfigExporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonConfigExporter() {
    }

    static String toJson() {
        JsonRoot root = new JsonRoot();
        root.activePreset = DragonOptConfig.activePreset.name();
        root.scheduler = new JsonScheduler();
        root.scheduler.workerThreads = DragonOptConfig.workerThreads;
        root.scheduler.parallelEntityTick = DragonOptConfig.parallelEntityTick;
        root.scheduler.parallelTileTick = DragonOptConfig.parallelTileTick;
        root.scheduler.parallelChunkTick = DragonOptConfig.parallelChunkTick;
        root.scheduler.asyncEntityAI = DragonOptConfig.asyncEntityAI;
        root.scheduler.tickBatchSize = DragonOptConfig.tickBatchSize;
        root.render = new JsonRender();
        root.render.dynamicRenderDistance = DragonOptConfig.dynamicRenderDistance;
        root.render.particleLimit = DragonOptConfig.particleLimit;
        root.render.disableEntityShadows = DragonOptConfig.disableEntityShadows;
        root.render.lightCacheMerge = DragonOptConfig.lightCacheMerge;
        root.render.lazyChunkLoad = DragonOptConfig.lazyChunkLoad;
        root.dragoncore = new JsonDragonCore();
        root.dragoncore.dragonCoreCompat = DragonOptConfig.dragonCoreCompat;
        root.dragoncore.dragonCoreHeartbeatSec = DragonOptConfig.dragonCoreHeartbeatSec;
        root.client = new JsonClient();
        root.client.guiHotkey = DragonOptConfig.guiHotkey;
        return GSON.toJson(root);
    }

    static class JsonRoot {
        String activePreset;
        JsonScheduler scheduler;
        JsonRender render;
        JsonDragonCore dragoncore;
        JsonClient client;
    }

    static class JsonScheduler {
        int workerThreads;
        boolean parallelEntityTick;
        boolean parallelTileTick;
        boolean parallelChunkTick;
        boolean asyncEntityAI;
        int tickBatchSize;
    }

    static class JsonRender {
        boolean dynamicRenderDistance;
        int particleLimit;
        boolean disableEntityShadows;
        boolean lightCacheMerge;
        boolean lazyChunkLoad;
    }

    static class JsonDragonCore {
        boolean dragonCoreCompat;
        int dragonCoreHeartbeatSec;
    }

    static class JsonClient {
        String guiHotkey;
    }
}
