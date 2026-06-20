package com.dragoncore.optimize.scheduler;

import com.dragoncore.optimize.config.DragonOptConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncAIScheduler {

    private static final AsyncAIScheduler INSTANCE = new AsyncAIScheduler();

    private final ExecutorService executor;
    private final AtomicInteger submittedTasks = new AtomicInteger(0);

    private AsyncAIScheduler() {
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
        this.executor = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "DragonOpt-AI-" + new AtomicInteger(0).incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    }

    public static AsyncAIScheduler getInstance() {
        return INSTANCE;
    }

    public static void onAIMethodEntered() {
        INSTANCE.tick();
    }

    public void tick() {
        if (!DragonOptConfig.asyncEntityAI) return;
        int pending = submittedTasks.incrementAndGet();
        if (pending > 1024) {
            submittedTasks.decrementAndGet();
            return;
        }
        executor.submit(() -> {
            try {
            } finally {
                submittedTasks.decrementAndGet();
            }
        });
    }

    public void submit(Runnable r) {
        if (!DragonOptConfig.asyncEntityAI) {
            r.run();
            return;
        }
        executor.submit(r);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
