package com.utt.foodcouriers_admin.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Promotion implements Serializable {
    private String id;
    private String code;
    private String name;
    private String description;

    @SerializedName("discount_type")
    private String discountType;

    @SerializedName("discount_value")
    private int discountValue;

    @SerializedName("min_order")
    private int minOrder;

    @SerializedName("max_discount")
    private Integer maxDiscount;

    @SerializedName("start_date")
    private String startDate;

    @SerializedName("end_date")
    private String endDate;

    @SerializedName("usage_limit")
    private Integer usageLimit;

    @SerializedName("usage_count")
    private int usageCount;

    @SerializedName("is_active")
    private boolean isActive;

    @SerializedName("created_at")
    private String createdAt;

    public Promotion() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public int getDiscountValue() { return discountValue; }
    public void setDiscountValue(int discountValue) { this.discountValue = discountValue; }

    public int getMinOrder() { return minOrder; }
    public void setMinOrder(int minOrder) { this.minOrder = minOrder; }

    public Integer getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(Integer maxDiscount) { this.maxDiscount = maxDiscount; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }

    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isPercent() {
        return "percent".equals(discountType);
    }

    public String getDiscountDisplay() {
        if (isPercent()) {
            return discountValue + "%";
        } else {
            return discountValue + "đ";
        }
    }
}