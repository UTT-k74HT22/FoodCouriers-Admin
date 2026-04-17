package com.utt.foodcouriers_admin.data.realtime;

import android.util.Log;

import com.google.gson.JsonObject;
import com.utt.foodcouriers_admin.data.remote.BaseSupabaseClient;
import com.utt.foodcouriers_admin.data.remote.SupabaseClientManager;
import com.utt.foodcouriers_admin.data.remote.SupabaseRealtimeClient;
import com.utt.foodcouriers_admin.utils.websocket.RealtimeChannel;
import com.utt.foodcouriers_admin.utils.websocket.RealtimeListener;

import java.util.HashMap;
import java.util.Map;

/**
 * OrderRealtimeManager - Quản lý realtime subscriptions cho orders trong Admin app.
 * 
 * Sử dụng:
 * <pre>
 * OrderRealtimeManager.getInstance().subscribe(new OrderRealtimeCallback() {
 *     {@literal @}Override
 *     public void onNewOrder(JsonObject order) { ... }
 *     {@literal @}Override
 *     public void onOrderUpdated(JsonObject newOrder, JsonObject oldOrder) { ... }
 * });
 * </pre>
 */
public class OrderRealtimeManager {

    private static final String TAG = "OrderRealtimeMgr";

    private static OrderRealtimeManager instance;

    private RealtimeChannel orderChannel;
    private boolean isSubscribed = false;
    private final Map<OrderRealtimeCallback, RealtimeListener> listeners = new HashMap<>();

    private OrderRealtimeManager() {}

    public static synchronized OrderRealtimeManager getInstance() {
        if (instance == null) {
            instance = new OrderRealtimeManager();
        }
        return instance;
    }

    /**
     * Subscribe lắng nghe tất cả orders thay đổi.
     * Nên gọi sau khi đăng nhập thành công.
     * 
     * @param callback Callback để nhận events
     */
    public void subscribe(OrderRealtimeCallback callback) {
        if (callback == null) {
            return;
        }
        
        if (listeners.containsKey(callback)) {
            Log.d(TAG, "Already subscribed to orders for this callback");
            return;
        }

        SupabaseRealtimeClient client = SupabaseRealtimeClient.getInstance();

        RealtimeListener listener = createListener(callback);

        // Subscribe to all orders (no filter). SupabaseRealtimeClient will reuse
        // the existing channel and add this listener when already subscribed.
        orderChannel = client.subscribe("public:orders", null, listener);
        if (orderChannel != null) {
            listeners.put(callback, listener);
        }
        isSubscribed = orderChannel != null && !listeners.isEmpty();
        Log.d(TAG, "Subscribed to orders: " + (isSubscribed ? "success" : "failed")
                + ", listeners=" + listeners.size());
    }

    /**
     * Subscribe với filter cụ thể (ví dụ: theo restaurant_id).
     * 
     * @param filter Filter query (vd: "restaurant_id=eq.123")
     * @param callback Callback để nhận events
     */
    public void subscribeWithFilter(String filter, OrderRealtimeCallback callback) {
        if (callback == null) {
            return;
        }

        unsubscribe(callback);

        SupabaseRealtimeClient client = SupabaseRealtimeClient.getInstance();
        RealtimeListener listener = createListener(callback);

        orderChannel = client.subscribe("public:orders", filter, listener);
        if (orderChannel != null) {
            listeners.put(callback, listener);
        }
        isSubscribed = orderChannel != null && !listeners.isEmpty();
        Log.d(TAG, "Subscribed to orders with filter: " + filter + " -> "
                + (isSubscribed ? "success" : "failed") + ", listeners=" + listeners.size());
    }

    private RealtimeListener createListener(OrderRealtimeCallback callback) {
        return new RealtimeListener() {
            @Override
            public void onInsert(JsonObject record) {
                Log.d(TAG, "New order inserted: " + getOrderCode(record));
                callback.onNewOrder(record);
            }

            @Override
            public void onUpdate(JsonObject record, JsonObject oldRecord) {
                Log.d(TAG, "Order updated: " + getOrderCode(record));
                callback.onOrderUpdated(record, oldRecord);
            }

            @Override
            public void onDelete(JsonObject oldRecord) {
                Log.d(TAG, "Order deleted: " + getOrderCode(oldRecord));
                callback.onOrderDeleted(oldRecord);
            }

            @Override
            public void onConnected() {
                Log.d(TAG, "Connected to order realtime");
                callback.onRealtimeConnected();
            }

            @Override
            public void onDisconnected() {
                Log.w(TAG, "Disconnected from order realtime");
                callback.onRealtimeDisconnected();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Realtime error: " + error);
                handleRealtimeError(error);
                callback.onRealtimeError(error);
            }
        };
    }

    /**
     * Unsubscribe khỏi order realtime.
     * Nên gọi khi logout hoặc không cần theo dõi nữa.
     */
    public void unsubscribe() {
        if (orderChannel != null) {
            SupabaseRealtimeClient.getInstance().unsubscribe(orderChannel);
            orderChannel = null;
        }
        listeners.clear();
        isSubscribed = false;
        Log.d(TAG, "Unsubscribed from orders");
    }

    public void unsubscribe(OrderRealtimeCallback callback) {
        if (callback == null) {
            return;
        }

        RealtimeListener listener = listeners.remove(callback);
        if (listener != null && orderChannel != null) {
            SupabaseRealtimeClient.getInstance().unsubscribe(orderChannel, listener);
        }

        isSubscribed = orderChannel != null && !listeners.isEmpty();
        if (!isSubscribed) {
            orderChannel = null;
        }
        Log.d(TAG, "Unsubscribed one orders listener, listeners=" + listeners.size());
    }

    /**
     * Kiểm tra đã subscribe chưa.
     */
    public boolean isSubscribed() {
        return isSubscribed;
    }

    private String getOrderCode(JsonObject record) {
        if (record == null) return "unknown";
        if (record.has("order_code") && !record.get("order_code").isJsonNull()) {
            return record.get("order_code").getAsString();
        }
        if (record.has("id") && !record.get("id").isJsonNull()) {
            return record.get("id").getAsString();
        }
        return "unknown";
    }

    private void handleRealtimeError(String error) {
        if (error == null || !error.contains("InvalidJWTToken")) {
            return;
        }

        SupabaseClientManager.refreshTokenNow(new BaseSupabaseClient.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (Boolean.TRUE.equals(result) && !SupabaseRealtimeClient.getInstance().isConnected()) {
                    SupabaseRealtimeClient.getInstance().connect();
                }
            }

            @Override
            public void onError(String refreshError) {
                Log.e(TAG, "Failed to refresh token for realtime: " + refreshError);
            }
        });
    }

    /**
     * Callback interface cho order realtime events.
     */
    public interface OrderRealtimeCallback {
        void onNewOrder(JsonObject order);
        void onOrderUpdated(JsonObject newOrder, JsonObject oldOrder);
        void onOrderDeleted(JsonObject oldOrder);
        default void onRealtimeConnected() {}
        default void onRealtimeDisconnected() {}
        default void onRealtimeError(String error) {}
    }
}
