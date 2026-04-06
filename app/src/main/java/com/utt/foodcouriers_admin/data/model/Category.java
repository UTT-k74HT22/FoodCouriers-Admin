package com.utt.foodcouriers_admin.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Category implements Serializable {
    private String id;
    private String name;
    @SerializedName("image_url")
    private String imageUrl;
    @SerializedName("sort_order")
    private int sortOrder;
    @SerializedName("is_active")
    private boolean isActive;

    public Category() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
