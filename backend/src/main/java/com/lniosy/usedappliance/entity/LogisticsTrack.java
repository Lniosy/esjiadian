package com.lniosy.usedappliance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("logistics_track")
public class LogisticsTrack {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long logisticsId;
    private LocalDateTime trackTime;
    private String content;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLogisticsId() { return logisticsId; }
    public void setLogisticsId(Long logisticsId) { this.logisticsId = logisticsId; }
    public LocalDateTime getTrackTime() { return trackTime; }
    public void setTrackTime(LocalDateTime trackTime) { this.trackTime = trackTime; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
