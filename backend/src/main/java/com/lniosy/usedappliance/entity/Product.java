package com.lniosy.usedappliance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;

@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sellerId;
    private Long categoryId;
    private String title;
    private String brand;
    private String model;
    private LocalDate purchaseDate;
    private String conditionLevel;
    private String functionStatus;
    private String repairHistory;
    private String description;
    private String videoUrl;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String region;
    private String tradeMethods;
    private String status;
    private String rejectReason;
    private Boolean sold;
    private Integer salesCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public String getConditionLevel() { return conditionLevel; }
    public void setConditionLevel(String conditionLevel) { this.conditionLevel = conditionLevel; }
    public String getFunctionStatus() { return functionStatus; }
    public void setFunctionStatus(String functionStatus) { this.functionStatus = functionStatus; }
    public String getRepairHistory() { return repairHistory; }
    public void setRepairHistory(String repairHistory) { this.repairHistory = repairHistory; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getTradeMethods() { return tradeMethods; }
    public void setTradeMethods(String tradeMethods) { this.tradeMethods = tradeMethods; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public Boolean getSold() { return sold; }
    public void setSold(Boolean sold) { this.sold = sold; }
    public Integer getSalesCount() { return salesCount; }
    public void setSalesCount(Integer salesCount) { this.salesCount = salesCount; }
}
