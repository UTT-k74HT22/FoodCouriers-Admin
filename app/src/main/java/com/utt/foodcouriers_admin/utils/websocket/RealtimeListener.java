package com.utt.foodcouriers_admin.utils.websocket;

import com.google.gson.JsonObject;

/**
 * Sử dụng để lắng nghe sự kiện của một channel.
 */
public interface RealtimeListener {
    /**
     * Hàm được gọi khi có sự kiện mới.
     * @param record Dữ liệu của sự kiện.
     */
    void onInsert(JsonObject record);

    /**
     * Hàm được gọi khi có sự kiện cập nhật.
     * @param record Dữ liệu mới.
     * @param oldRecord Dữ liệu cũ.
     */
    void onUpdate(JsonObject record, JsonObject oldRecord);

    /**
     * Hàm được gọi khi có sự kiện xóa.
     * @param oldRecord Dữ liệu cũ.
     */
    void onDelete(JsonObject oldRecord);

    /**
     * Hàm được gọi khi kết nối thành công.
     */
    default void onConnected() {}

    /**
     * Hàm được gọi khi kết nối bị mất.
     */
    default void onDisconnected() {}

    /**
     * Hàm được gọi khi có lỗi.
     * @param error Thông báo lỗi.
     */
    default void onError(String error) {}
}