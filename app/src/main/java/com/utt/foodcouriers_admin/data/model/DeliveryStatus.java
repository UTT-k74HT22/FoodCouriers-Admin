package com.utt.foodcouriers_admin.data.model;

public enum DeliveryStatus {
    UNASSIGNED("unassigned", "Chưa gán"),
    SEARCHING("searching", "Đang tìm tài xế"),
    ASSIGNED("assigned", "Đã gán tài xế"),
    ARRIVING_PICKUP("arriving_pickup", "Đang đến quán"),
    WAITING_PICKUP("waiting_pickup", "Đang chờ món"),
    PICKED_UP("picked_up", "Đã lấy hàng"),
    COMPLETED("completed", "Giao xong"),
    FAILED("failed", "Thất bại");

    private final String value;
    private final String label;

    DeliveryStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static DeliveryStatus fromValue(String value) {
        if (value == null) return UNASSIGNED;
        for (DeliveryStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return UNASSIGNED;
    }
}
