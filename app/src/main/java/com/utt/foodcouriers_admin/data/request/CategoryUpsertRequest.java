package com.utt.foodcouriers_admin.data.request;

import com.google.gson.annotations.SerializedName;

public class CategoryUpsertRequest {

    private String name;
    private String description;

    @SerializedName("image_url")
    private String imageUrl;
    @SerializedName("sort_order")
    private Integer sortOrder;
    @SerializedName("is_active")
    private Boolean isActive;

    public CategoryUpsertRequest() {
    }

    public CategoryUpsertRequest(String name, String description, String imageUrl, Integer sortOrder, Boolean isActive) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        this.isActive = isActive;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
