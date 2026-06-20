package com.dragoncore.optimize.scheduler;

import com.dragoncore.optimize.DragonOptimize;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class DragonOptExceptionHandler {

    private static final ConcurrentHashMap<String, AtomicInteger> COUNTERS = new ConcurrentHashMap<>();
    private static final int LOG_EVERY = 500;

    private DragonOptExceptionHandler() {
    }

    static void onTickException(Object context, Throwable t) {
        if (t == null) return;
        String key = t.getClass().getName();
        if (t.getStackTrace().length > 0) {
            key += "@" + t.getStackTrace()[0].getClassName() + "#" + t.getStackTrace()[0].getMethodName();
        }
        AtomicInteger counter = COUNTERS.computeIfAbsent(key, k -> new AtomicInteger(0));
        int n = counter.incrementAndGet();
        if (n % LOG_EVERY == 1) {
            DragonOptimize.LOGGER.warn("[DragonOptimize] tick 异常 #{} (ctx={})：{}",
                    n, context == null ? "-" : context.getClass().getSimpleName(), t.toString());
        }
    }
}
