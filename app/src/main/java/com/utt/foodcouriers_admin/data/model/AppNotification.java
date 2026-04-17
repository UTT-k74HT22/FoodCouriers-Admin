package com.utt.foodcouriers_admin.data.model;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class AppNotification {

    private String id;

    @SerializedName("user_id")
    private String userId;

    private String title;
    private String body;
    private String type;
    private JsonObject data;

    @SerializedName("is_read")
    private boolean read;

    @SerializedName("created_at")
    private String createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public JsonObject getData() { return data; }
    public void setData(JsonObject data) { this.data = data; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
