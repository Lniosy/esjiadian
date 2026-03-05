package com.lniosy.usedappliance.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RequestMetricsService {
    private final long startedAtMs = System.currentTimeMillis();
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong totalResponseMs = new AtomicLong(0);
    private final AtomicLong totalRequestBytes = new AtomicLong(0);
    private final AtomicLong totalResponseBytes = new AtomicLong(0);
    private final AtomicLong slowRequestCount = new AtomicLong(0);
    private final ConcurrentHashMap<String, EndpointStat> endpointStats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> onlineUsers = new ConcurrentHashMap<>();
    private final long slowRequestThresholdMs;

    public RequestMetricsService(@Value("${app.monitor.slow-request-threshold-ms:800}") long slowRequestThresholdMs) {
        this.slowRequestThresholdMs = slowRequestThresholdMs;
    }

    public void record(String method, String path, long responseMs) {
        record(method, path, responseMs, 0L, 0L);
    }

    public void record(String method, String path, long responseMs, long requestBytes, long responseBytes) {
        requestCount.incrementAndGet();
        totalResponseMs.addAndGet(Math.max(0, responseMs));
        totalRequestBytes.addAndGet(Math.max(0, requestBytes));
        totalResponseBytes.addAndGet(Math.max(0, responseBytes));
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

    public void recordUserActivity(Long userId) {
        if (userId != null) {
            onlineUsers.put(userId, System.currentTimeMillis());
        }
    }

    public long getOnlineUserCount() {
        long now = System.currentTimeMillis();
        // 15分钟内有活动即为在线
        long deadline = now - 15 * 60 * 1000;
        onlineUsers.entrySet().removeIf(entry -> entry.getValue() < deadline);
        return onlineUsers.size();
    }

    public Map<String, Object> snapshot() {
        long count = requestCount.get();
        long inBytes = totalRequestBytes.get();
        long outBytes = totalResponseBytes.get();
        double avg = count == 0 ? 0 : (double) totalResponseMs.get() / count;
        double avgInBytes = count == 0 ? 0 : (double) inBytes / count;
        double avgOutBytes = count == 0 ? 0 : (double) outBytes / count;
        long elapsedMs = Math.max(1, System.currentTimeMillis() - startedAtMs);
        double inBps = inBytes * 1000D / elapsedMs;
        double outBps = outBytes * 1000D / elapsedMs;
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
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("requestCount", count);
        row.put("avgResponseMs", avg);
        row.put("totalRequestBytes", inBytes);
        row.put("totalResponseBytes", outBytes);
        row.put("avgRequestBytes", avgInBytes);
        row.put("avgResponseBytes", avgOutBytes);
        row.put("inboundBytesPerSecond", inBps);
        row.put("outboundBytesPerSecond", outBps);
        row.put("slowRequestCount", slowRequestCount.get());
        row.put("onlineUserCount", getOnlineUserCount());
        row.put("slowRequestThresholdMs", slowRequestThresholdMs);
        row.put("topEndpoints", topEndpoints);
        return row;
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
