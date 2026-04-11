package com.utt.foodcouriers_admin.data.model;

public enum OrderStatus {
    PENDING("pending", "Chờ xác nhận"),
    CONFIRMED("confirmed", "Đã xác nhận"),
    PREPARING("preparing", "Đang chuẩn bị"),
    READY_FOR_PICKUP("ready_for_pickup", "Chờ shipper"),
    DELIVERING("delivering", "Đang giao"),
    DELIVERED("delivered", "Hoàn thành"),
    CANCELLED("cancelled", "Đã hủy");

    private final String value;
    private final String label;

    OrderStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public boolean isLocked() {
        return this == DELIVERED || this == CANCELLED;
    }

    public OrderStatus next() {
        switch (this) {
            case PENDING:
                return CONFIRMED;
            case CONFIRMED:
                return PREPARING;
            case PREPARING:
                return READY_FOR_PICKUP;
            case READY_FOR_PICKUP:
                return DELIVERING;
            case DELIVERING:
                return DELIVERED;
            default:
                return this;
        }
    }

    public static OrderStatus fromValue(String value) {
        if (value == null) {
            return PENDING;
        }
        for (OrderStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return PENDING;
    }
}
