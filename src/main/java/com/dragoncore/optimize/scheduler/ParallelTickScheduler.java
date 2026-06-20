package com.dragoncore.optimize.scheduler;

import com.dragoncore.optimize.config.DragonOptConfig;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并行 tick 调度器 —— 解决 1.12.2 单核处理的核心痛点。
 *
 * <p>设计：</p>
 * <ul>
 * <li>在一个 tick 开始时调用 {@link #beginTick()} 重置计数器。</li>
 * <li>调用 {@link #runBatched(List, ThrowingConsumer)} 将列表分片并行执行，每完成一个子任务对计数器 -1。</li>
 * <li>调用 {@link #awaitTick()} 等待计数器归零。</li>
 * <li>每分块内部保持串行执行，保证 Minecraft 行为顺序一致；块之间并行。</li>
 * </ul>
 */
public class ParallelTickScheduler {

    private final ExecutorService executor;
    private final int workers;
    private final AtomicInteger pending = new AtomicInteger(0);
    private volatile boolean running = true;
    private volatile Object currentTickToken;

    public ParallelTickScheduler(int threads) {
        this.workers = Math.max(1, threads);
        this.executor = Executors.newFixedThreadPool(workers, new DragonOptThreadFactory());
    }

    public void beginTick() {
        if (!running) {
            return;
        }
        pending.set(0);
        currentTickToken = new Object();
    }

    /**
     * 将 items 分块提交给线程池并行处理，调用线程不阻塞（直到 awaitTick 才等待）。
     */
    public <T> void runBatched(List<T> items, ThrowingConsumer<T> consumer) {
        if (items == null || items.isEmpty()) {
            return;
        }
        if (!running) {
            runSerial(items, consumer);
            return;
        }
        int batchSize = Math.max(8, DragonOptConfig.tickBatchSize);
        int total = items.size();
        int batches = (total + batchSize - 1) / batchSize;
        final Object token = currentTickToken;
        pending.addAndGet(batches);
        for (int i = 0; i < batches; i++) {
            final int start = i * batchSize;
            final int end = Math.min(total, start + batchSize);
            executor.execute(() -> {
                try {
                    for (int j = start; j < end; j++) {
                        if (currentTickToken != token) {
                            // 新 tick 已开始，放弃旧 tick 任务，避免跨 tick 竞态
                            return;
                        }
                        T item = items.get(j);
                        try {
                            consumer.accept(item);
                        } catch (Throwable t) {
                            DragonOptExceptionHandler.onTickException(item, t);
                        }
                    }
                } catch (ConcurrentModificationException ignored) {
                    // 忽略，避免被 Mod 修改集合导致的崩溃
                } finally {
                    pending.decrementAndGet();
                }
            });
        }
    }

    public void awaitTick() {
        awaitTick(50, TimeUnit.MILLISECONDS);
    }

    public void awaitTick(long timeout, TimeUnit unit) {
        if (!running) {
            return;
        }
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (pending.get() > 0) {
            if (System.nanoTime() > deadline) {
                // 超时：放弃等待，下一个 tick 会自动重置。
                return;
            }
            // 小 yield 以减少忙等 CPU 占用
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void resume() {
        running = true;
    }

    public void shutdown() {
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                List<Runnable> pendingList = executor.shutdownNow();
                if (pendingList != null) {
                    pendingList.clear();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int workerCount() {
        return workers;
    }

    public int pendingCount() {
        return pending.get();
    }

    public boolean isRunning() {
        return running;
    }

    // ===== 内部 =====

    private <T> void runSerial(List<T> items, ThrowingConsumer<T> consumer) {
        for (T item : items) {
            try {
                consumer.accept(item);
            } catch (Throwable t) {
                DragonOptExceptionHandler.onTickException(item, t);
            }
        }
    }

    public interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    private static final class DragonOptThreadFactory implements ThreadFactory {
        private static final AtomicInteger COUNTER = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "DragonOpt-Worker-" + COUNTER.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        }
    }

    // ===== 全局实例 =====
    private static volatile ParallelTickScheduler GLOBAL;

    public static void installGlobal(ParallelTickScheduler s) {
        GLOBAL = s;
    }

    public static ParallelTickScheduler global() {
        return GLOBAL;
    }

    /** 便于 ASM 直接调用的便捷方法。 */
    public static <T> void runBatchedGlobal(List<T> items, ThrowingConsumer<T> consumer) {
        ParallelTickScheduler s = GLOBAL;
        if (s != null) {
            s.runBatched(items, consumer);
        } else {
            for (T item : new ArrayList<>(items)) {
                try {
                    consumer.accept(item);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public static void beginTickGlobal() {
        ParallelTickScheduler s = GLOBAL;
        if (s != null) {
            s.beginTick();
        }
    }

    public static void awaitTickGlobal() {
        ParallelTickScheduler s = GLOBAL;
        if (s != null) {
            s.awaitTick(50, TimeUnit.MILLISECONDS);
        }
    }
}
