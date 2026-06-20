package com.dragoncore.optimize.render;

import com.dragoncore.optimize.config.DragonOptConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LazyChunkLoader {

    private final ConcurrentHashMap<Long, AtomicInteger> pending = new ConcurrentHashMap<>();
    private int tickCounter;

    public boolean shouldLoad(int cx, int cz, int dimension) {
        if (!DragonOptConfig.lazyChunkLoad) return true;
        long key = pack(cx, cz, dimension);
        AtomicInteger count = pending.computeIfAbsent(key, k -> new AtomicInteger(0));
        return count.incrementAndGet() == 1;
    }

    public void tick() {
        tickCounter++;
        if (tickCounter % 5 == 0) pending.clear();
    }

    private static long pack(int cx, int cz, int dimension) {
        return ((long) (dimension & 0xFFFF))
                | (((long) (cx & 0xFFFFFF)) << 16)
                | (((long) (cz & 0xFFFFFF)) << 40);
    }
}
