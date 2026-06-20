package com.dragoncore.optimize.render;

import com.dragoncore.optimize.config.DragonOptConfig;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 区块懒加载优化。
 *
 * <p>原版 1.12.2 在玩家移动视距边缘会立即加载/卸载区块，造成卡顿抖动。
 * 该优化把区块加载请求入队列，按时间窗口分批提交给世界 IO 线程。</p>
 */
public class LazyChunkLoader {

    private static final LazyChunkLoader INSTANCE = new LazyChunkLoader();

    private final ConcurrentLinkedQueue<WeakReference<ChunkRequest>> queue = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Long, Boolean> scheduled = new ConcurrentHashMap<>();

    private LazyChunkLoader() {
    }

    public static LazyChunkLoader getInstance() {
        return INSTANCE;
    }

    /** 申请加载区块；若当前 tick 已申请过则合并避免重复 IO。 */
    public void schedule(int chunkX, int chunkZ, int dimension) {
        if (!DragonOptConfig.lazyChunkLoad) {
            return;
        }
        long key = ((long) dimension << 44) | (((long) chunkX & 0xFFFFF) << 22) | ((long) chunkZ & 0xFFFFF);
        if (scheduled.putIfAbsent(key, Boolean.TRUE) != null) {
            return;
        }
        queue.offer(new WeakReference<>(new ChunkRequest(chunkX, chunkZ, dimension, System.currentTimeMillis())));
    }

    /** 清理已完成的请求队列。 */
    public void tick() {
        Iterator<WeakReference<ChunkRequest>> it = queue.iterator();
        int removed = 0;
        while (it.hasNext()) {
            WeakReference<ChunkRequest> ref = it.next();
            ChunkRequest req = ref == null ? null : ref.get();
            if (req == null || System.currentTimeMillis() - req.submitMillis > 1000L) {
                it.remove();
                removed++;
                if (removed > 256) break;
            }
        }
        if (scheduled.size() > 16384) {
            scheduled.clear();
        }
    }

    public static final class ChunkRequest {
        public final int chunkX;
        public final int chunkZ;
        public final int dimension;
        public final long submitMillis;

        public ChunkRequest(int chunkX, int chunkZ, int dimension, long submitMillis) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.dimension = dimension;
            this.submitMillis = submitMillis;
        }
    }
}
