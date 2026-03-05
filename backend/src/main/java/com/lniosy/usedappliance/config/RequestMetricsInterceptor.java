package com.lniosy.usedappliance.config;

import com.lniosy.usedappliance.service.RequestMetricsService;
import com.lniosy.usedappliance.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestMetricsInterceptor implements HandlerInterceptor {
    private static final String START_AT = "request_metrics_start_at";
    private final RequestMetricsService requestMetricsService;

    public RequestMetricsInterceptor(RequestMetricsService requestMetricsService) {
        this.requestMetricsService = requestMetricsService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_AT, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object startObj = request.getAttribute(START_AT);
        if (!(startObj instanceof Long startAt)) {
            return;
        }
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api/")) {
            return;
        }
        long cost = Math.max(0, System.currentTimeMillis() - startAt);
        long requestBytes = Math.max(0L, request.getContentLengthLong());
        long responseBytes = parseResponseBytes(response);
        requestMetricsService.record(request.getMethod(), path, cost, requestBytes, responseBytes);

        try {
            requestMetricsService.recordUserActivity(SecurityUtils.currentUserId());
        } catch (Exception ignored) {
            // Anonymous request
        }
    }

    private long parseResponseBytes(HttpServletResponse response) {
        String value = response.getHeader("Content-Length");
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(value.trim()));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}
