package com.lniosy.usedappliance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("monitor_alert_event")
public class MonitorAlertEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String alertKey;
    private String level;
    private String title;
    private String message;
    private Double currentValue;
    private Double thresholdValue;
    private String status;
    private Long ackBy;
    private LocalDateTime ackAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAlertKey() { return alertKey; }
    public void setAlertKey(String alertKey) { this.alertKey = alertKey; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Double getCurrentValue() { return currentValue; }
    public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }
    public Double getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(Double thresholdValue) { this.thresholdValue = thresholdValue; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getAckBy() { return ackBy; }
    public void setAckBy(Long ackBy) { this.ackBy = ackBy; }
    public LocalDateTime getAckAt() { return ackAt; }
    public void setAckAt(LocalDateTime ackAt) { this.ackAt = ackAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
