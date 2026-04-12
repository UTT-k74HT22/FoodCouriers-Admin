package com.utt.foodcouriers_admin.ui.order;

import android.text.TextUtils;

import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.OrderStatus;

import java.text.NumberFormat;
import java.util.Locale;

public final class OrderUiFormatter {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private OrderUiFormatter() {
    }

    public static String formatCurrency(int amount) {
        return CURRENCY_FORMAT.format(amount) + "đ";
    }

    public static String formatTime(String rawValue) {
        if (TextUtils.isEmpty(rawValue)) {
            return "--";
        }
        String normalized = rawValue.replace('T', ' ');
        if (normalized.length() >= 16) {
            return normalized.substring(11, 16) + " - " + normalized.substring(0, 10);
        }
        return normalized;
    }

    public static int badgeBackgroundRes(OrderStatus status) {
        switch (status) {
            case CONFIRMED:
            case PREPARING:
            case DELIVERING:
                return R.drawable.admin_badge_info;
            case DELIVERED:
                return R.drawable.admin_badge_success;
            case CANCELLED:
                return R.drawable.admin_badge_cancel;
            case PENDING:
            default:
                return R.drawable.admin_badge_pending;
        }
    }

    public static int badgeTextColorRes(OrderStatus status) {
        switch (status) {
            case CONFIRMED:
                return R.color.black;
            case PREPARING:
                return R.color.black;
            case DELIVERING:
                return R.color.black;
            case DELIVERED:
                return R.color.white;
            case CANCELLED:
                return R.color.white;
            case PENDING:
                return R.color.black;
            case READY_FOR_PICKUP:
                return R.color.black;
                default:
                return R.color.white;
        }
    }
}
