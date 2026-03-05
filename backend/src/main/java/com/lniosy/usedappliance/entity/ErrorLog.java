package com.lniosy.usedappliance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("error_log")
public class ErrorLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String path;
    private String method;
    private Integer httpStatus;
    private Integer errorCode;
    private String errorMessage;
    private String exceptionClass;
    private String stackTrace;
    private String clientIp;
    private String userAgent;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public Integer getErrorCode() { return errorCode; }
    public void setErrorCode(Integer errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getExceptionClass() { return exceptionClass; }
    public void setExceptionClass(String exceptionClass) { this.exceptionClass = exceptionClass; }
    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
