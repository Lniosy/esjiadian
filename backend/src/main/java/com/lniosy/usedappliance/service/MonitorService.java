package com.lniosy.usedappliance.service;

import com.lniosy.usedappliance.dto.monitor.MonitorAlertDto;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;

@Service
public class MonitorService {
    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;
    private final NotificationService notificationService;
    private final MonitorAlertEventService monitorAlertEventService;
    private final RequestMetricsService requestMetricsService;
    private final double cpuThreshold;
    private final double processCpuThreshold;
    private final double jvmMemoryUsageThreshold;
    private final double diskUsageThreshold;
    private final double avgResponseThresholdMs;
    private final double redisHitRateThreshold;
    private final long alertCooldownSeconds;
    private final ConcurrentHashMap<String, Long> lastAlertEpoch = new ConcurrentHashMap<>();

    public MonitorService(JdbcTemplate jdbcTemplate,
                          RedisConnectionFactory redisConnectionFactory,
                          NotificationService notificationService,
                          MonitorAlertEventService monitorAlertEventService,
                          RequestMetricsService requestMetricsService,
                          @Value("${app.monitor.alert.cpu-threshold:0.85}") double cpuThreshold,
                          @Value("${app.monitor.alert.process-cpu-threshold:0.85}") double processCpuThreshold,
                          @Value("${app.monitor.alert.jvm-memory-usage-threshold:0.90}") double jvmMemoryUsageThreshold,
                          @Value("${app.monitor.alert.disk-usage-threshold:0.90}") double diskUsageThreshold,
                          @Value("${app.monitor.alert.avg-response-ms-threshold:500}") double avgResponseThresholdMs,
                          @Value("${app.monitor.alert.redis-hit-rate-threshold:0.70}") double redisHitRateThreshold,
                          @Value("${app.monitor.alert.cooldown-seconds:600}") long alertCooldownSeconds) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
        this.notificationService = notificationService;
        this.monitorAlertEventService = monitorAlertEventService;
        this.requestMetricsService = requestMetricsService;
        this.cpuThreshold = cpuThreshold;
        this.processCpuThreshold = processCpuThreshold;
        this.jvmMemoryUsageThreshold = jvmMemoryUsageThreshold;
        this.diskUsageThreshold = diskUsageThreshold;
        this.avgResponseThresholdMs = avgResponseThresholdMs;
        this.redisHitRateThreshold = redisHitRateThreshold;
        this.alertCooldownSeconds = alertCooldownSeconds;
    }

    public Map<String, Object> snapshot() {
        com.sun.management.OperatingSystemMXBean osBean =
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        Runtime rt = Runtime.getRuntime();
        long totalMem = rt.totalMemory();
        long freeMem = rt.freeMemory();
        long usedMem = totalMem - freeMem;
        double jvmMemoryUsage = totalMem == 0 ? 0 : (double) usedMem / totalMem;

        boolean dbUp = true;
        boolean redisUp = true;
        long mysqlSlowQueries = -1L;
        long mysqlThreadsConnected = -1L;
        long redisHits = -1L;
        long redisMisses = -1L;
        double redisHitRate = -1D;
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            mysqlSlowQueries = mysqlStatusValue("Slow_queries");
            mysqlThreadsConnected = mysqlStatusValue("Threads_connected");
        } catch (DataAccessException ex) {
            dbUp = false;
        }

        try (RedisConnection conn = redisConnectionFactory.getConnection()) {
            if (conn != null) {
                conn.ping();
                Properties stats = conn.serverCommands().info("stats");
                redisHits = parseLong(stats == null ? null : stats.getProperty("keyspace_hits"));
                redisMisses = parseLong(stats == null ? null : stats.getProperty("keyspace_misses"));
                long total = Math.max(0, redisHits) + Math.max(0, redisMisses);
                if (total > 0 && redisHits >= 0 && redisMisses >= 0) {
                    redisHitRate = (double) redisHits / total;
                }
            }
        } catch (Exception ex) {
            redisUp = false;
        }

        Map<String, Object> requestMetrics = requestMetricsService.snapshot();
        double avgResponseMs = ((Number) requestMetrics.getOrDefault("avgResponseMs", 0D)).doubleValue();
        double inboundBps = ((Number) requestMetrics.getOrDefault("inboundBytesPerSecond", 0D)).doubleValue();
        double outboundBps = ((Number) requestMetrics.getOrDefault("outboundBytesPerSecond", 0D)).doubleValue();

        // 磁盘指标
        File root = new File("/");
        long diskTotal = root.getTotalSpace();
        long diskFree = root.getFreeSpace();
        double diskUsage = diskTotal == 0 ? 0 : (double) (diskTotal - diskFree) / diskTotal;

        List<MonitorAlertDto> alerts = evaluateAlerts(
                osBean.getSystemCpuLoad(),
                osBean.getProcessCpuLoad(),
                jvmMemoryUsage,
                diskUsage,
                avgResponseMs,
                redisHitRate,
                dbUp,
                redisUp
        );

        Map<String, Object> serverMetrics = new LinkedHashMap<>();
        serverMetrics.put("cpuLoad", osBean.getSystemCpuLoad());
        serverMetrics.put("processCpuLoad", osBean.getProcessCpuLoad());
        serverMetrics.put("availableProcessors", osBean.getAvailableProcessors());
        serverMetrics.put("systemLoadAverage", osBean.getSystemLoadAverage());
        serverMetrics.put("jvmUsedMemory", usedMem);
        serverMetrics.put("jvmTotalMemory", totalMem);
        serverMetrics.put("jvmMemoryUsage", jvmMemoryUsage);
        serverMetrics.put("diskTotal", diskTotal);
        serverMetrics.put("diskFree", diskFree);
        serverMetrics.put("diskUsage", diskUsage);
        serverMetrics.put("networkInboundBps", inboundBps);
        serverMetrics.put("networkOutboundBps", outboundBps);
        serverMetrics.put("networkTotalBps", inboundBps + outboundBps);

        return Map.of(
                "server", serverMetrics,
                "application", Map.of(
                        "uptimeMillis", ManagementFactory.getRuntimeMXBean().getUptime(),
                        "threadCount", ManagementFactory.getThreadMXBean().getThreadCount(),
                        "onlineUsers", requestMetrics.getOrDefault("onlineUserCount", 0L),
                        "requestMetrics", requestMetrics
                ),
                "database", Map.of("mysqlUp", dbUp),
                "databaseMetrics", Map.of(
                        "slowQueries", mysqlSlowQueries,
                        "threadsConnected", mysqlThreadsConnected
                ),
                "cache", Map.of("redisUp", redisUp),
                "cacheMetrics", Map.of(
                        "keyspaceHits", redisHits,
                        "keyspaceMisses", redisMisses,
                        "hitRate", redisHitRate
                ),
                "thresholds", Map.of(
                        "cpuThreshold", cpuThreshold,
                        "processCpuThreshold", processCpuThreshold,
                        "jvmMemoryUsageThreshold", jvmMemoryUsageThreshold,
                        "diskUsageThreshold", diskUsageThreshold,
                        "avgResponseThresholdMs", avgResponseThresholdMs,
                        "redisHitRateThreshold", redisHitRateThreshold
                ),
                "alerts", alerts
        );
    }

    public int checkAndNotifyAlerts() {
        Map<String, Object> snapshot = snapshot();
        @SuppressWarnings("unchecked")
        List<MonitorAlertDto> alerts = (List<MonitorAlertDto>) snapshot.get("alerts");
        int notified = 0;
        long now = Instant.now().getEpochSecond();
        monitorAlertEventService.syncCurrentAlerts(alerts);
        for (MonitorAlertDto alert : alerts) {
            long last = lastAlertEpoch.getOrDefault(alert.key(), 0L);
            if (now - last < alertCooldownSeconds) {
                continue;
            }
            notifyByLevel(alert);
            lastAlertEpoch.put(alert.key(), now);
            notified++;
        }
        return notified;
    }

    private void notifyByLevel(MonitorAlertDto alert) {
        String level = alert.level() == null ? "WARN" : alert.level().toUpperCase();
        if ("ERROR".equals(level)) {
            notificationService.createForAdmins(
                    "MONITOR_ALERT_ERROR",
                    "[ERROR] " + alert.title(),
                    alert.message()
            );
            return;
        }
        notificationService.createForAdmins(
                "MONITOR_ALERT",
                "[WARN] " + alert.title(),
                alert.message()
        );
    }

    public Map<String, Object> mysqlSlowQueryDetails(Integer limit) {
        int size = safeLimit(limit);
        String sql = """
                SELECT DIGEST_TEXT, COUNT_STAR,
                       ROUND(SUM_TIMER_WAIT / 1000000000000, 2) AS total_ms,
                       ROUND(AVG_TIMER_WAIT / 1000000000000, 2) AS avg_ms,
                       SUM_ROWS_EXAMINED AS rows_examined
                FROM performance_schema.events_statements_summary_by_digest
                WHERE SCHEMA_NAME = DATABASE()
                  AND DIGEST_TEXT IS NOT NULL
                ORDER BY SUM_TIMER_WAIT DESC
                LIMIT ?
                """;
        try {
            List<Map<String, Object>> list = jdbcTemplate.query(sql, ps -> ps.setInt(1, size), (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("digestText", trimSql(rs.getString("DIGEST_TEXT")));
                row.put("count", rs.getLong("COUNT_STAR"));
                row.put("totalMs", rs.getDouble("total_ms"));
                row.put("avgMs", rs.getDouble("avg_ms"));
                row.put("rowsExamined", rs.getLong("rows_examined"));
                return row;
            });
            return Map.of("available", true, "list", list);
        } catch (Exception ex) {
            return Map.of("available", false, "message", "performance_schema 不可用或无权限", "list", List.of());
        }
    }

    private List<MonitorAlertDto> evaluateAlerts(double cpuLoad,
                                                 double processCpuLoad,
                                                 double jvmMemoryUsage,
                                                 double diskUsage,
                                                 double avgResponseMs,
                                                 double redisHitRate,
                                                 boolean dbUp,
                                                 boolean redisUp) {
        List<MonitorAlertDto> alerts = new ArrayList<>();
        long now = Instant.now().toEpochMilli();
        long expireAt = now + (alertCooldownSeconds * 1000);
        if (cpuLoad >= cpuThreshold) {
            String level = metricLevel(cpuLoad, cpuThreshold, 1.15);
            alerts.add(newAlert(
                    "cpu_high", level, "CPU使用率过高",
                    "系统CPU使用率达到 " + round4(cpuLoad) + "，阈值 " + round4(cpuThreshold),
                    cpuLoad, cpuThreshold, now, expireAt
            ));
        }
        if (processCpuLoad >= processCpuThreshold) {
            String level = metricLevel(processCpuLoad, processCpuThreshold, 1.15);
            alerts.add(newAlert(
                    "process_cpu_high", level, "进程CPU使用率过高",
                    "应用进程CPU使用率达到 " + round4(processCpuLoad) + "，阈值 " + round4(processCpuThreshold),
                    processCpuLoad, processCpuThreshold, now, expireAt
            ));
        }
        if (jvmMemoryUsage >= jvmMemoryUsageThreshold) {
            String level = metricLevel(jvmMemoryUsage, jvmMemoryUsageThreshold, 1.10);
            alerts.add(newAlert(
                    "jvm_memory_high", level, "JVM内存占用过高",
                    "JVM内存使用率达到 " + round4(jvmMemoryUsage) + "，阈值 " + round4(jvmMemoryUsageThreshold),
                    jvmMemoryUsage, jvmMemoryUsageThreshold, now, expireAt
            ));
        }
        if (diskUsage >= diskUsageThreshold) {
            String level = metricLevel(diskUsage, diskUsageThreshold, 1.05);
            alerts.add(newAlert(
                    "disk_usage_high", level, "磁盘使用率过高",
                    "磁盘使用率达到 " + round4(diskUsage) + "，阈值 " + round4(diskUsageThreshold),
                    diskUsage, diskUsageThreshold, now, expireAt
            ));
        }
        if (avgResponseMs >= avgResponseThresholdMs) {
            String level = metricLevel(avgResponseMs, avgResponseThresholdMs, 2.0);
            alerts.add(newAlert(
                    "avg_response_high", level, "接口平均响应时间偏高",
                    "当前平均响应时间 " + round4(avgResponseMs) + "ms，阈值 " + round4(avgResponseThresholdMs) + "ms",
                    avgResponseMs, avgResponseThresholdMs, now, expireAt
            ));
        }
        if (redisHitRate >= 0 && redisHitRate < redisHitRateThreshold) {
            String level = redisHitRate < redisHitRateThreshold * 0.6 ? "ERROR" : "WARN";
            alerts.add(newAlert(
                    "redis_hit_rate_low", level, "Redis命中率偏低",
                    "当前命中率 " + round4(redisHitRate) + "，阈值 " + round4(redisHitRateThreshold),
                    redisHitRate, redisHitRateThreshold, now, expireAt
            ));
        }
        if (!dbUp) {
            alerts.add(newAlert(
                    "mysql_down", "ERROR", "MySQL连接异常",
                    "MySQL健康检查失败，请检查数据库连接状态",
                    0, 0, now, expireAt
            ));
        }
        if (!redisUp) {
            alerts.add(newAlert(
                    "redis_down", "WARN", "Redis连接异常",
                    "Redis连接失败，系统已自动切换至本地内存保底模式，核心功能（验证码等）暂不受影响",
                    0, 0, now, expireAt
            ));
        }
        return alerts;
    }

    private String metricLevel(double current, double threshold, double errorMultiplier) {
        if (threshold <= 0) {
            return "WARN";
        }
        return current >= threshold * errorMultiplier ? "ERROR" : "WARN";
    }

    private MonitorAlertDto newAlert(String key, String level, String title, String message,
                                     double currentValue, double threshold,
                                     long detectedAt, long expireAt) {
        return new MonitorAlertDto(
                key,
                level,
                title,
                message,
                currentValue,
                threshold,
                detectedAt,
                expireAt
        );
    }

    private String round4(double n) {
        return String.format("%.4f", n);
    }

    private long mysqlStatusValue(String name) {
        return jdbcTemplate.query("SHOW GLOBAL STATUS LIKE ?", ps -> ps.setString(1, name), rs -> {
            if (rs.next()) {
                return parseLong(rs.getString("Value"));
            }
            return -1L;
        });
    }

    private long parseLong(String text) {
        if (text == null || text.isBlank()) {
            return -1L;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }

    private int safeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 20;
        }
        return Math.min(limit, 100);
    }

    private String trimSql(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 220) {
            return normalized;
        }
        return normalized.substring(0, 220) + "...";
    }
}
