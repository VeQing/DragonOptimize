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
        if (!running) return;
        pending.set(0);
        currentTickToken = new Object();
    }

    public <T> void runBatched(List<T> items, ThrowingConsumer<T> consumer) {
        if (items == null || items.isEmpty()) return;
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
                        if (currentTickToken != token) return;
                        T item = items.get(j);
                        try {
                            consumer.accept(item);
                        } catch (Throwable t) {
                            DragonOptExceptionHandler.onTickException(item, t);
                        }
                    }
                } catch (ConcurrentModificationException ignored) {
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
        if (!running) return;
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (pending.get() > 0) {
            if (System.nanoTime() > deadline) return;
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

    public void pause() {
        running = false;
    }

    public void shutdown() {
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                List<Runnable> pendingList = executor.shutdownNow();
                if (pendingList != null) pendingList.clear();
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

    private static volatile ParallelTickScheduler GLOBAL;

    public static void installGlobal(ParallelTickScheduler s) {
        GLOBAL = s;
    }

    public static ParallelTickScheduler global() {
        return GLOBAL;
    }

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
        if (s != null) s.beginTick();
    }

    public static void awaitTickGlobal() {
        ParallelTickScheduler s = GLOBAL;
        if (s != null) s.awaitTick(50, TimeUnit.MILLISECONDS);
    }
}
