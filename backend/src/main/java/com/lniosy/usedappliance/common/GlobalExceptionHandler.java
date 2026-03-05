package com.lniosy.usedappliance.common;

import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import com.lniosy.usedappliance.service.ErrorLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ErrorLogService errorLogService;

    public GlobalExceptionHandler(ErrorLogService errorLogService) {
        this.errorLogService = errorLogService;
    }

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBiz(BizException ex, HttpServletRequest request) {
        if (ex.getCode() >= 500) {
            errorLogService.record(request, ex.getCode(), ex.getCode(), ex.getMessage(), ex);
            log.error("业务异常: code={}, message={}", ex.getCode(), ex.getMessage(), ex);
        }
        return ApiResponse.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class,
            ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ApiResponse<Void> handleValidation(Exception ex, HttpServletRequest request) {
        log.warn("请求参数不合法: path={}, message={}", request == null ? "-" : request.getRequestURI(), ex.getMessage());
        return ApiResponse.fail(400, "请求参数不合法");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnknown(Exception ex, HttpServletRequest request) {
        errorLogService.record(request, 500, 500, ex.getMessage(), ex);
        log.error("系统内部错误", ex);
        return ApiResponse.fail(500, "系统内部错误");
    }
}
