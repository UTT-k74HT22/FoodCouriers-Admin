package com.utt.foodcouriers_admin.data.request;

import com.google.gson.annotations.SerializedName;

public class RestaurantUpsertRequest {

    private String name;
    private String description;
    private String address;
    private String phone;
    @SerializedName("image_url")
    private String imageUrl;
    @SerializedName("is_active")
    private Boolean isActive;
    @SerializedName("is_open")
    private Boolean isOpen;
    @SerializedName("open_time")
    private String openTime;
    @SerializedName("close_time")
    private String closeTime;
    @SerializedName("delivery_fee")
    private Integer deliveryFee;
    @SerializedName("min_order")
    private Integer minOrder;
    private Double latitude;
    private Double longitude;

    public RestaurantUpsertRequest() {
    }

    public RestaurantUpsertRequest(
            String name,
            String description,
            String address,
            String phone,
            String imageUrl,
            Boolean isActive,
            Boolean isOpen,
            String openTime,
            String closeTime,
            Integer deliveryFee,
            Integer minOrder,
            Double latitude,
            Double longitude
    ) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.phone = phone;
        this.imageUrl = imageUrl;
        this.isActive = isActive;
        this.isOpen = isOpen;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.deliveryFee = deliveryFee;
        this.minOrder = minOrder;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Boolean getIsOpen() {
        return isOpen;
    }

    public void setIsOpen(Boolean open) {
        isOpen = open;
    }

    public String getOpenTime() {
        return openTime;
    }

    public void setOpenTime(String openTime) {
        this.openTime = openTime;
    }

    public String getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(String closeTime) {
        this.closeTime = closeTime;
    }

    public Integer getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(Integer deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public Integer getMinOrder() {
        return minOrder;
    }

    public void setMinOrder(Integer minOrder) {
        this.minOrder = minOrder;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
