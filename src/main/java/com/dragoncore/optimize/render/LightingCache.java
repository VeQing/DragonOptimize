package com.dragoncore.optimize.render;

import com.dragoncore.optimize.config.DragonOptConfig;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 光照缓存合并 (Lighting Cache Merge) —— 仿 Phosphor 思路。
 *
 * <p>原版 1.12.2 的光照计算会在相邻区块重复触发多次 "propagateSkylightOcclusion"，
 * 这个缓存以 {@code (chunkX,chunkZ,height)} 为键合并重复请求，减少不必要的区块读写。</p>
 *
 * <p>缓存只在 {@link DragonOptConfig#lightCacheMerge} 为 true 时生效，并且每 tick 自动清理，
 * 避免占用过多内存。</p>
 */
public class LightingCache {

    private static final LightingCache INSTANCE = new LightingCache();

    private final ConcurrentHashMap<Long, Boolean> completed = new ConcurrentHashMap<>();
    private volatile long currentTickKey;

    private LightingCache() {
    }

    public static LightingCache getInstance() {
        return INSTANCE;
    }

    /**
     * 检查某个 (chunkX, chunkZ, height) 是否在此 tick 内已经处理。
     * 若已处理则返回 {@code true}，调用方可以跳过；否则标记为已处理并返回 {@code false}。
     */
    public boolean tryMarkProcessed(int chunkX, int chunkZ, int height) {
        if (!DragonOptConfig.lightCacheMerge) {
            return false;
        }
        long key = pack(chunkX, chunkZ, height) ^ currentTickKey;
        Boolean prev = completed.putIfAbsent(key, Boolean.TRUE);
        return prev != null;
    }

    /** 每 tick 重置缓存，保持瞬时性。 */
    public void onTickStart() {
        if (!DragonOptConfig.lightCacheMerge) {
            if (!completed.isEmpty()) {
                completed.clear();
            }
            return;
        }
        currentTickKey = System.nanoTime();
        // 适度清理
        if (completed.size() > 8192) {
            completed.clear();
        }
    }

    private static long pack(int cx, int cz, int height) {
        return ((long) cx & 0xFFFFFFL)
                | (((long) cz & 0xFFFFFFL) << 24)
                | (((long) height & 0xFFL) << 48);
    }
}
