package com.lniosy.usedappliance.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lniosy.usedappliance.common.ApiResponse;
import com.lniosy.usedappliance.service.ErrorLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final String TOO_MANY_REQUESTS_MSG = "请求过于频繁，请稍后再试";
    private static final String DEFAULT_GROUP = "DEFAULT";
    private final ObjectMapper objectMapper;
    private final ErrorLogService errorLogService;
    private final boolean enabled;
    private final long windowMs;
    private final int defaultAnonymousLimit;
    private final int defaultAuthenticatedLimit;
    private final List<LimitGroup> groups;
    private final ConcurrentHashMap<String, Counter> counterMap = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupAt = new AtomicLong(0);

    public RateLimitFilter(ObjectMapper objectMapper,
                           ErrorLogService errorLogService,
                           RateLimitProperties properties) {
        this.objectMapper = objectMapper;
        this.errorLogService = errorLogService;
        this.enabled = properties.isEnabled();
        this.windowMs = Math.max(1, properties.getWindowSeconds()) * 1000L;
        this.defaultAnonymousLimit = Math.max(1, properties.getMaxAnonymous());
        this.defaultAuthenticatedLimit = Math.max(1, properties.getMaxAuthenticated());
        this.groups = initGroups(properties.getGroups());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api/")) {
            return true;
        }
        return path.startsWith("/api/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long now = System.currentTimeMillis();
        LimitKey limitKey = resolveKey(request, request.getRequestURI());
        Counter counter = counterMap.computeIfAbsent(limitKey.key(), k -> new Counter(now));
        if (!counter.tryAcquire(now, windowMs, limitKey.limit())) {
            errorLogService.record(request, 429, 429, TOO_MANY_REQUESTS_MSG, null);
            write429(response);
            return;
        }
        maybeCleanup(now);
        filterChain.doFilter(request, response);
    }

    private LimitKey resolveKey(HttpServletRequest request, String path) {
        LimitGroup group = resolveGroup(path);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth == null ? null : auth.getPrincipal();
        if (principal instanceof AuthUser authUser) {
            int limit = group.authenticatedLimit();
            return new LimitKey("g:" + group.name() + "|user:" + authUser.userId(), limit);
        }
        int limit = group.anonymousLimit();
        return new LimitKey("g:" + group.name() + "|ip:" + resolveIp(request), limit);
    }

    private LimitGroup resolveGroup(String path) {
        if (path == null) {
            return LimitGroup.defaultGroup(defaultAnonymousLimit, defaultAuthenticatedLimit);
        }
        for (LimitGroup group : groups) {
            if (group.matches(path)) {
                return group;
            }
        }
        return LimitGroup.defaultGroup(defaultAnonymousLimit, defaultAuthenticatedLimit);
    }

    private List<LimitGroup> initGroups(List<RateLimitProperties.Group> rawGroups) {
        if (rawGroups == null || rawGroups.isEmpty()) {
            return List.of();
        }
        List<LimitGroup> list = new ArrayList<>();
        for (RateLimitProperties.Group group : rawGroups) {
            if (group == null) {
                continue;
            }
            String name = safeName(group.getName());
            List<String> prefixes = sanitizePrefixes(group.getPathPrefixes());
            if (prefixes.isEmpty()) {
                continue;
            }
            int anonymousLimit = normalizeLimit(group.getMaxAnonymous(), defaultAnonymousLimit);
            int authenticatedLimit = normalizeLimit(group.getMaxAuthenticated(), defaultAuthenticatedLimit);
            list.add(new LimitGroup(name, prefixes, anonymousLimit, authenticatedLimit));
        }
        return list;
    }

    private String safeName(String name) {
        if (name == null || name.isBlank()) {
            return DEFAULT_GROUP;
        }
        return name.trim().toUpperCase();
    }

    private int normalizeLimit(Integer raw, int fallback) {
        if (raw == null || raw <= 0) {
            return fallback;
        }
        return raw;
    }

    private List<String> sanitizePrefixes(List<String> rawPrefixes) {
        if (rawPrefixes == null || rawPrefixes.isEmpty()) {
            return List.of();
        }
        List<String> prefixes = new ArrayList<>();
        for (String prefix : rawPrefixes) {
            if (prefix == null || prefix.isBlank()) {
                continue;
            }
            prefixes.add(prefix.trim());
        }
        return prefixes;
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isBlank()) {
                return first;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void write429(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> body = ApiResponse.fail(429, TOO_MANY_REQUESTS_MSG);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private void maybeCleanup(long now) {
        long last = lastCleanupAt.get();
        if (now - last < 60_000L) {
            return;
        }
        if (!lastCleanupAt.compareAndSet(last, now)) {
            return;
        }
        long staleBefore = now - (windowMs * 2);
        for (Map.Entry<String, Counter> entry : counterMap.entrySet()) {
            if (entry.getValue().lastSeenMs < staleBefore) {
                counterMap.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private record LimitKey(String key, int limit) {
    }

    private record LimitGroup(String name, List<String> pathPrefixes, int anonymousLimit, int authenticatedLimit) {
        boolean matches(String path) {
            for (String prefix : pathPrefixes) {
                if (path.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }

        static LimitGroup defaultGroup(int anonymousLimit, int authenticatedLimit) {
            return new LimitGroup(DEFAULT_GROUP, List.of(), anonymousLimit, authenticatedLimit);
        }
    }

    private static class Counter {
        private long windowStartMs;
        private int count;
        private long lastSeenMs;

        private Counter(long now) {
            this.windowStartMs = now;
            this.lastSeenMs = now;
            this.count = 0;
        }

        private synchronized boolean tryAcquire(long now, long windowMs, int limit) {
            if (now - windowStartMs >= windowMs) {
                windowStartMs = now;
                count = 0;
            }
            lastSeenMs = now;
            if (count >= limit) {
                return false;
            }
            count++;
            return true;
        }
    }
}
