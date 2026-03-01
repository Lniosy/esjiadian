package com.lniosy.usedappliance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("shop")
public class Shop {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String logoUrl;
    private String intro;
    private String categories;
    private String region;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getIntro() { return intro; }
    public void setIntro(String intro) { this.intro = intro; }
    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
}
