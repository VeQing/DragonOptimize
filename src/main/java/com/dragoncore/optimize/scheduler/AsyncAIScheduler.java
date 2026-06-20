package com.dragoncore.optimize.scheduler;

import com.dragoncore.optimize.config.DragonOptConfig;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步 AI 任务调度器。
 *
 * <p>将 {@code EntityAIBase#shouldExecute()} / {@code startExecuting()} 等较重的重计算操作
 * 异步化到低优先级线程池执行，避免阻塞主线程。实际改变实体状态的动作仍然在主线程 tick
 * 回写，确保行为一致性。</p>
 */
public class AsyncAIScheduler {

    private static final AsyncAIScheduler INSTANCE = new AsyncAIScheduler();

    private final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();
    private final ExecutorService executor;

    private AsyncAIScheduler() {
        int threads = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
        this.executor = Executors.newFixedThreadPool(threads, new ThreadFactory() {
            private final AtomicInteger c = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "DragonOpt-AI-" + c.incrementAndGet());
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            }
        });
    }

    public static AsyncAIScheduler getInstance() {
        return INSTANCE;
    }

    /** 提交一个 AI 任务；结果由具体实现存储在实体的 volatile 字段中。 */
    public void submit(Runnable task) {
        if (!DragonOptConfig.asyncEntityAI) {
            task.run();
            return;
        }
        pending.offer(task);
        executor.execute(() -> {
            Runnable r;
            while ((r = pending.poll()) != null) {
                try {
                    r.run();
                } catch (Throwable ignored) {
                }
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
