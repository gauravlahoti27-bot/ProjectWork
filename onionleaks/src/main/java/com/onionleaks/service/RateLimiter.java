package com.onionleaks.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal in-memory token bucket. Behind Tor every request arrives from 127.0.0.1, so we key
 * on the anonymous session id rather than the IP, plus a global bucket to blunt floods.
 * Nothing is persisted and nothing identifying is recorded.
 */
public class RateLimiter {

    private static final class Bucket {
        double tokens;
        Instant last;
        Bucket(double t) { tokens = t; last = Instant.now(); }
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Bucket global = new Bucket(200);
    private final double capacity;
    private final double refillPerSecond;

    public RateLimiter() { this(10, 10.0 / 60.0); }            // 10 actions, refill 10/min
    public RateLimiter(double capacity, double refillPerSecond) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
    }

    public boolean tryAcquire(String key) {
        if (!take(global, 200, 200.0 / 60.0)) return false;
        Bucket b = buckets.computeIfAbsent(key, k -> new Bucket(capacity));
        boolean ok = take(b, capacity, refillPerSecond);
        if (buckets.size() > 50_000) buckets.clear();            // crude memory guard
        return ok;
    }

    private boolean take(Bucket b, double cap, double rate) {
        synchronized (b) {
            Instant now = Instant.now();
            double elapsed = Duration.between(b.last, now).toMillis() / 1000.0;
            b.tokens = Math.min(cap, b.tokens + elapsed * rate);
            b.last = now;
            if (b.tokens < 1) return false;
            b.tokens -= 1;
            return true;
        }
    }
}
