package com.marcosfgdev.mtickets.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {

    private final Map<String, Long> lastRequest;
    private final Map<String, Integer> requestCount;
    private final int maxRequests;
    private final long windowMs;

    public RateLimiter(int maxRequests, long windowMs) {
        this.lastRequest = new ConcurrentHashMap<>();
        this.requestCount = new ConcurrentHashMap<>();
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    public boolean isAllowed(String identifier) {
        long now = System.currentTimeMillis();
        Long lastTime = lastRequest.get(identifier);

        if (lastTime == null || (now - lastTime) > windowMs) {
            lastRequest.put(identifier, now);
            requestCount.put(identifier, 1);
            return true;
        }

        int count = requestCount.getOrDefault(identifier, 0);
        if (count >= maxRequests) {
            return false;
        }

        requestCount.put(identifier, count + 1);
        return true;
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        lastRequest.entrySet().removeIf(entry -> (now - entry.getValue()) > windowMs);
        requestCount.keySet().removeIf(key -> !lastRequest.containsKey(key));
    }
}
