package com.utt.foodcouriers_admin.data.request;

import com.google.gson.annotations.SerializedName;

public class PromotionUpsertRequest {

    private String code;
    private String name;
    private String description;

    @SerializedName("discount_type")
    private String discountType;

    @SerializedName("discount_value")
    private Integer discountValue;

    @SerializedName("min_order")
    private Integer minOrder;

    @SerializedName("max_discount")
    private Integer maxDiscount;

    @SerializedName("start_date")
    private String startDate;

    @SerializedName("end_date")
    private String endDate;

    @SerializedName("usage_limit")
    private Integer usageLimit;

    @SerializedName("is_active")
    private Boolean isActive;

    public PromotionUpsertRequest() {
    }

    public PromotionUpsertRequest(String code, String name, String description,
                                   String discountType, Integer discountValue,
                                   Integer minOrder, Integer maxDiscount,
                                   String startDate, String endDate,
                                   Integer usageLimit, Boolean isActive) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrder = minOrder;
        this.maxDiscount = maxDiscount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.usageLimit = usageLimit;
        this.isActive = isActive;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public Integer getDiscountValue() { return discountValue; }
    public void setDiscountValue(Integer discountValue) { this.discountValue = discountValue; }

    public Integer getMinOrder() { return minOrder; }
    public void setMinOrder(Integer minOrder) { this.minOrder = minOrder; }

    public Integer getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(Integer maxDiscount) { this.maxDiscount = maxDiscount; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
}