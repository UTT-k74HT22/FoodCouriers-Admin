package com.utt.foodcouriers_admin.data.request;

import com.google.gson.annotations.SerializedName;

public class MenuUpsertRequest {

    @SerializedName("restaurant_id")
    private String restaurantId;

    @SerializedName("category_id")
    private String categoryId;

    private String name;
    private String description;
    private Integer price;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("is_available")
    private Boolean isAvailable;

    @SerializedName("is_featured")
    private Boolean isFeatured;

    @SerializedName("sort_order")
    private Integer sortOrder;

    public MenuUpsertRequest() {
    }

    public MenuUpsertRequest(String restaurantId, String categoryId, String name, String description,
                             Integer price, String imageUrl, Boolean isAvailable, Boolean isFeatured, Integer sortOrder) {
        this.restaurantId = restaurantId;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.isAvailable = isAvailable;
        this.isFeatured = isFeatured;
        this.sortOrder = sortOrder;
    }

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public Boolean getIsFeatured() { return isFeatured; }
    public void setIsFeatured(Boolean isFeatured) { this.isFeatured = isFeatured; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
