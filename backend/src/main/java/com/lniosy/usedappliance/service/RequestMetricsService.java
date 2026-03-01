package com.lniosy.usedappliance.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RequestMetricsService {
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong totalResponseMs = new AtomicLong(0);
    private final AtomicLong slowRequestCount = new AtomicLong(0);
    private final ConcurrentHashMap<String, EndpointStat> endpointStats = new ConcurrentHashMap<>();
    private final long slowRequestThresholdMs;

    public RequestMetricsService(@Value("${app.monitor.slow-request-threshold-ms:800}") long slowRequestThresholdMs) {
        this.slowRequestThresholdMs = slowRequestThresholdMs;
    }

    public void record(String method, String path, long responseMs) {
        requestCount.incrementAndGet();
        totalResponseMs.addAndGet(Math.max(0, responseMs));
        if (responseMs >= slowRequestThresholdMs) {
            slowRequestCount.incrementAndGet();
        }
        String key = method + " " + path;
        EndpointStat stat = endpointStats.computeIfAbsent(key, k -> new EndpointStat());
        stat.count.incrementAndGet();
        stat.totalMs.addAndGet(Math.max(0, responseMs));
        if (responseMs >= slowRequestThresholdMs) {
            stat.slowCount.incrementAndGet();
        }
    }

    public Map<String, Object> snapshot() {
        long count = requestCount.get();
        double avg = count == 0 ? 0 : (double) totalResponseMs.get() / count;
        var topEndpoints = endpointStats.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<String, EndpointStat> e) -> e.getValue().count.get()).reversed())
                .limit(10)
                .map(e -> Map.<String, Object>of(
                        "endpoint", e.getKey(),
                        "count", e.getValue().count.get(),
                        "avgResponseMs", e.getValue().avgMs(),
                        "slowCount", e.getValue().slowCount.get()
                ))
                .toList();
        return Map.of(
                "requestCount", count,
                "avgResponseMs", avg,
                "slowRequestCount", slowRequestCount.get(),
                "slowRequestThresholdMs", slowRequestThresholdMs,
                "topEndpoints", topEndpoints
        );
    }

    private static class EndpointStat {
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong totalMs = new AtomicLong(0);
        private final AtomicLong slowCount = new AtomicLong(0);

        private double avgMs() {
            long c = count.get();
            return c == 0 ? 0 : (double) totalMs.get() / c;
        }
    }
}
