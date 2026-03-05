package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lniosy.usedappliance.entity.ErrorLog;
import com.lniosy.usedappliance.mapper.ErrorLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ErrorLogService {
    private static final Logger log = LoggerFactory.getLogger(ErrorLogService.class);
    private final ErrorLogMapper errorLogMapper;

    public ErrorLogService(ErrorLogMapper errorLogMapper) {
        this.errorLogMapper = errorLogMapper;
    }

    public void record(HttpServletRequest request, Integer httpStatus, Integer errorCode, String message, Throwable throwable) {
        try {
            ErrorLog errorLog = new ErrorLog();
            errorLog.setUserId(safeCurrentUserId());
            errorLog.setPath(request == null ? "-" : trimToLen(request.getRequestURI(), 255));
            errorLog.setMethod(request == null ? "-" : trimToLen(request.getMethod(), 16));
            errorLog.setHttpStatus(httpStatus == null ? 500 : httpStatus);
            errorLog.setErrorCode(errorCode);
            errorLog.setErrorMessage(trimToLen(message == null ? "未知错误" : message, 500));
            errorLog.setExceptionClass(throwable == null ? null : throwable.getClass().getName());
            errorLog.setStackTrace(throwable == null ? null : trimToLen(stackTraceText(throwable), 8000));
            errorLog.setClientIp(request == null ? "-" : trimToLen(resolveIp(request), 64));
            errorLog.setUserAgent(request == null ? null : trimToLen(request.getHeader("User-Agent"), 500));
            errorLogMapper.insert(errorLog);
        } catch (Exception ex) {
            log.warn("记录错误日志失败: {}", ex.getMessage());
        }
    }

    public List<Map<String, Object>> latest(Integer limit, Integer httpStatus, String keyword) {
        try {
            int size = safeLimit(limit);
            LambdaQueryWrapper<ErrorLog> qw = new LambdaQueryWrapper<ErrorLog>()
                    .orderByDesc(ErrorLog::getId)
                    .last("limit " + size);
            if (httpStatus != null) {
                if (httpStatus == 4) {
                    qw.ge(ErrorLog::getHttpStatus, 400).lt(ErrorLog::getHttpStatus, 500);
                } else if (httpStatus == 5) {
                    qw.ge(ErrorLog::getHttpStatus, 500).lt(ErrorLog::getHttpStatus, 600);
                } else {
                    qw.eq(ErrorLog::getHttpStatus, httpStatus);
                }
            }
            if (keyword != null && !keyword.isBlank()) {
                String key = keyword.trim();
                qw.and(w -> w.like(ErrorLog::getPath, key)
                        .or().like(ErrorLog::getErrorMessage, key)
                        .or().like(ErrorLog::getExceptionClass, key));
            }
            return errorLogMapper.selectList(qw)
                    .stream()
                    .map(logEntry -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", logEntry.getId());
                        row.put("userId", logEntry.getUserId() == null ? 0L : logEntry.getUserId());
                        row.put("path", safeText(logEntry.getPath()));
                        row.put("method", safeText(logEntry.getMethod()));
                        row.put("httpStatus", logEntry.getHttpStatus() == null ? 500 : logEntry.getHttpStatus());
                        row.put("errorCode", logEntry.getErrorCode() == null ? 0 : logEntry.getErrorCode());
                        row.put("errorMessage", safeText(logEntry.getErrorMessage()));
                        row.put("exceptionClass", safeText(logEntry.getExceptionClass()));
                        row.put("stackTrace", safeText(logEntry.getStackTrace()));
                        row.put("clientIp", safeText(logEntry.getClientIp()));
                        row.put("userAgent", safeText(logEntry.getUserAgent()));
                        row.put("createdAt", logEntry.getCreatedAt() == null ? 0L :
                                logEntry.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                        return row;
                    })
                    .toList();
        } catch (Exception ex) {
            log.warn("查询错误日志失败: {}", ex.getMessage());
            return List.of();
        }
    }

    private Long safeCurrentUserId() {
        try {
            return com.lniosy.usedappliance.util.SecurityUtils.currentUserId();
        } catch (Exception ignored) {
            return null;
        }
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

    private String stackTraceText(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private int safeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 100;
        }
        return Math.min(limit, 500);
    }

    private String trimToLen(String text, int maxLen) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }
}
