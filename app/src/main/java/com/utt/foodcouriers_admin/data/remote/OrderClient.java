package com.utt.foodcouriers_admin.data.remote;

import com.google.gson.reflect.TypeToken;
import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.data.model.Shipper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OrderClient extends BaseSupabaseClient {

    private static final String RPC_URL = SupabaseConfig.REST_URL + "/rpc/";
    private static final String ORDERS_URL = SupabaseConfig.REST_URL + "/orders";
    private static final String SHIPPERS_URL = SupabaseConfig.REST_URL + "/shippers";
    
    private static OrderClient instance;

    public static synchronized OrderClient getInstance() {
        if (instance == null) {
            instance = new OrderClient();
        }
        return instance;
    }

    private OrderClient() {
        super();
    }

    /**
     * Lấy danh sách đơn hàng với filter và join thông tin user, restaurant, shipper
     */
    public void getOrders(String selectClause, String filterParams, ApiCallback<List<Order>> callback) {
        new Thread(() -> {
            try {
                String url = ORDERS_URL + "?select=" + selectClause;
                if (filterParams != null && !filterParams.isEmpty()) {
                    url += "&" + filterParams;
                }

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                        .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String json = response.body() != null ? response.body().string() : "[]";
                    if (response.isSuccessful()) {
                        List<Order> orders = gson.fromJson(json, new TypeToken<List<Order>>() {}.getType());
                        postSuccess(callback, orders);
                    } else {
                        postError(callback, parseRestError("Failed to fetch orders", response.code(), json));
                    }
                }
            } catch (IOException e) {
                postError(callback, e.getMessage());
            }
        }).start();
    }

    /**
     * Cập nhật thông tin đơn hàng (PATCH)
     */
    public void updateOrder(String orderId, Map<String, Object> updates, ApiCallback<Void> callback) {
        new Thread(() -> {
            try {
                String jsonBody = gson.toJson(updates);
                RequestBody body = RequestBody.create(jsonBody, MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON));

                Request request = new Request.Builder()
                        .url(ORDERS_URL + "?id=eq." + orderId)
                        .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                        .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                        .patch(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        postSuccess(callback, null);
                    } else {
                        String json = response.body() != null ? response.body().string() : "";
                        postError(callback, parseRestError("Update failed", response.code(), json));
                    }
                }
            } catch (IOException e) {
                postError(callback, e.getMessage());
            }
        }).start();
    }

    /**
     * Lấy danh sách Shipper có sẵn để gán đơn
     */
    public void getShippers(ApiCallback<List<Shipper>> callback) {
        new Thread(() -> {
            try {
                // Lấy shipper đang active, available và join thông tin user
                String url = SHIPPERS_URL + "?select=*,user:users!user_id(*)&is_active=eq.true&is_available=eq.true";

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                        .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String json = response.body() != null ? response.body().string() : "[]";
                    if (response.isSuccessful()) {
                        List<Shipper> shippers = gson.fromJson(json, new TypeToken<List<Shipper>>() {}.getType());
                        postSuccess(callback, shippers);
                    } else {
                        postError(callback, parseRestError("Failed to fetch shippers", response.code(), json));
                    }
                }
            } catch (IOException e) {
                postError(callback, e.getMessage());
            }
        }).start();
    }

    /**
     * Gọi RPC accept_order_v2
     */
    public void acceptOrder(String orderId, String shipperUserId, ApiCallback<RpcResponse> callback) {
        callRpc("accept_order_v2", Map.of("p_order_id", orderId, "p_shipper_user_id", shipperUserId), callback);
    }

    /**
     * Gọi RPC pickup_order
     */
    public void pickupOrder(String orderId, String shipperUserId, ApiCallback<RpcResponse> callback) {
        callRpc("pickup_order", Map.of("p_order_id", orderId, "p_shipper_user_id", shipperUserId), callback);
    }

    /**
     * Gọi RPC complete_order
     */
    public void completeOrder(String orderId, String shipperUserId, ApiCallback<RpcResponse> callback) {
        callRpc("complete_order", Map.of("p_order_id", orderId, "p_shipper_user_id", shipperUserId), callback);
    }

    private void callRpc(String functionName, Map<String, Object> params, ApiCallback<RpcResponse> callback) {
        new Thread(() -> {
            try {
                String jsonBody = gson.toJson(params);
                RequestBody body = RequestBody.create(jsonBody, MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON));

                Request request = new Request.Builder()
                        .url(RPC_URL + functionName)
                        .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                        .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String json = response.body() != null ? response.body().string() : "{}";
                    if (response.isSuccessful()) {
                        RpcResponse rpcRes = gson.fromJson(json, RpcResponse.class);
                        postSuccess(callback, rpcRes);
                    } else {
                        postError(callback, parseRestError("RPC failed", response.code(), json));
                    }
                }
            } catch (IOException e) {
                postError(callback, e.getMessage());
            }
        }).start();
    }

    public static class RpcResponse {
        private boolean success;
        private String message;

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
