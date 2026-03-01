package com.lniosy.usedappliance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("logistics_record")
public class LogisticsRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String companyCode;
    private String trackingNo;
    private String status;
    private String shipNote;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String companyCode) { this.companyCode = companyCode; }
    public String getTrackingNo() { return trackingNo; }
    public void setTrackingNo(String trackingNo) { this.trackingNo = trackingNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getShipNote() { return shipNote; }
    public void setShipNote(String shipNote) { this.shipNote = shipNote; }
}
