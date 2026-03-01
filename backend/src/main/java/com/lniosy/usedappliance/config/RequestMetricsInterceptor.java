package com.lniosy.usedappliance.config;

import com.lniosy.usedappliance.service.RequestMetricsService;
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
        requestMetricsService.record(request.getMethod(), path, cost);
    }
}
