package com.lniosy.usedappliance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("refund_request")
public class RefundRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long applicantId;
    private String reason;
    private String evidenceImages;
    private String status;
    private String rejectReason;
    private String returnCompanyCode;
    private String returnTrackingNo;
    private String returnShipNote;
    private LocalDateTime returnShippedAt;
    private LocalDateTime returnReceivedAt;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getEvidenceImages() { return evidenceImages; }
    public void setEvidenceImages(String evidenceImages) { this.evidenceImages = evidenceImages; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public String getReturnCompanyCode() { return returnCompanyCode; }
    public void setReturnCompanyCode(String returnCompanyCode) { this.returnCompanyCode = returnCompanyCode; }
    public String getReturnTrackingNo() { return returnTrackingNo; }
    public void setReturnTrackingNo(String returnTrackingNo) { this.returnTrackingNo = returnTrackingNo; }
    public String getReturnShipNote() { return returnShipNote; }
    public void setReturnShipNote(String returnShipNote) { this.returnShipNote = returnShipNote; }
    public LocalDateTime getReturnShippedAt() { return returnShippedAt; }
    public void setReturnShippedAt(LocalDateTime returnShippedAt) { this.returnShippedAt = returnShippedAt; }
    public LocalDateTime getReturnReceivedAt() { return returnReceivedAt; }
    public void setReturnReceivedAt(LocalDateTime returnReceivedAt) { this.returnReceivedAt = returnReceivedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
