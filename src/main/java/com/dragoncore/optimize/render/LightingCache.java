package com.dragoncore.optimize.render;

import com.dragoncore.optimize.config.DragonOptConfig;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LightingCache {

    private final ConcurrentHashMap<Long, AtomicInteger> requestCount = new ConcurrentHashMap<>();
    private int tickCounter;

    public boolean request(int cx, int cz, int height) {
        if (!DragonOptConfig.lightCacheMerge) return true;
        long key = pack(cx, cz, height);
        AtomicInteger counter = requestCount.computeIfAbsent(key, k -> new AtomicInteger(0));
        return counter.incrementAndGet() == 1;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        tickCounter++;
        if (tickCounter % 10 == 0) requestCount.clear();
    }

    public int size() {
        return requestCount.size();
    }

    private static long pack(int cx, int cz, int height) {
        return ((long) (cx & 0xFFFFF))
                | (((long) (cz & 0xFFFFF)) << 20)
                | (((long) (height & 0xFF)) << 40);
    }
}
