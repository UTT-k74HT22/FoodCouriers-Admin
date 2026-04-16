package com.utt.foodcouriers_admin.data.repository;

import android.text.TextUtils;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.data.model.OrderStatus;
import com.utt.foodcouriers_admin.data.model.Shipper;
import com.utt.foodcouriers_admin.data.remote.BaseSupabaseClient;
import com.utt.foodcouriers_admin.data.remote.OrderClient;

import java.util.List;

public class OrderRepository {

    private static OrderRepository instance;
    private final OrderClient orderClient;

    private OrderRepository() {
        this.orderClient = OrderClient.getInstance();
    }

    public static synchronized OrderRepository getInstance() {
        if (instance == null) {
            instance = new OrderRepository();
        }
        return instance;
    }

    /**
     * Lấy danh sách đơn hàng thực tế từ Supabase
     */
    public void getOrders(OrderStatus status, String query, String shipperId, RepositoryCallback<List<Order>> callback) {
        // Cấu trúc select để lấy thông tin join
        String selectClause = "*,user:users!user_id(*),restaurant:restaurants!restaurant_id(id,name),shipper:users!shipper_id(*),items:order_items(*)";
        
        // Tạo filter
        StringBuilder filterBuilder = new StringBuilder();
        if (status != null) {
            filterBuilder.append("status=eq.").append(status.getValue());
        }
        
        if (shipperId != null && !shipperId.isEmpty()) {
            if (filterBuilder.length() > 0) filterBuilder.append("&");
            filterBuilder.append("shipper_id=eq.").append(shipperId);
        }
        // Sắp xếp đơn mới nhất lên đầu
        if (filterBuilder.length() > 0) filterBuilder.append("&");
        filterBuilder.append("order=created_at.desc");

        orderClient.getOrders(selectClause, filterBuilder.toString(), new BaseSupabaseClient.ApiCallback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> result) {
                callback.onComplete(BaseResponse.success(result));
            }

            @Override
            public void onError(String error) {
                callback.onComplete(BaseResponse.error("FETCH_ERROR", error));
            }
        });
    }

    public void searchOrders(String orderCode, RepositoryCallback<List<Order>> callback) {
        String selectClause ="*,user:users!user_id(*),restaurant:restaurants!restaurant_id(id,name)";

        String filter = "order_code=eq." + orderCode + "&order=created_at.desc";

        orderClient.getOrders(selectClause, filter, new BaseSupabaseClient.ApiCallback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> result) {
                callback.onComplete(BaseResponse.success(result));
            }

            @Override
            public void onError(String error) {
                callback.onComplete(BaseResponse.error("SEARCH_ERROR", error));
            }
        });
    }

    /**
     * Lấy chi tiết một đơn hàng theo ID
     */
    public void getOrderById(String orderId, RepositoryCallback<Order> callback) {
        String selectClause = "*,user:users!user_id(*),restaurant:restaurants!restaurant_id(id,name),shipper:users!shipper_id(*),items:order_items(*)";
        String filter = "id=eq." + orderId;

        orderClient.getOrders(selectClause, filter, new BaseSupabaseClient.ApiCallback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> result) {
                if (result != null && !result.isEmpty()) {
                    callback.onComplete(BaseResponse.success(result.get(0)));
                } else {
                    callback.onComplete(BaseResponse.error("NOT_FOUND", "Order not found"));
                }
            }

            @Override
            public void onError(String error) {
                callback.onComplete(BaseResponse.error("FETCH_ERROR", error));
            }
        });
    }

    /**
     * Cập nhật trạng thái đơn hàng (PATCH đơn giản)
     */
    public void updateStatus(String orderId, OrderStatus status, RepositoryCallback<Void> callback) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", status.getValue());
        
        // Nếu chuyển sang confirmed, tự động chuyển delivery_status sang searching theo BA
        if (status == OrderStatus.CONFIRMED) {
            updates.put("delivery_status", "searching");
        }

        orderClient.updateOrder(orderId, updates, new BaseSupabaseClient.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                callback.onComplete(BaseResponse.success(null));
            }

            @Override
            public void onError(String error) {
                callback.onComplete(BaseResponse.error("UPDATE_ERROR", error));
            }
        });
    }

    /**
     * Gán Shipper (Admin thực hiện gán thủ công)
     */
    public void assignShipper(String orderId, Shipper shipper, RepositoryCallback<Void> callback) {
        if (shipper == null || shipper.getUserId() == null) {
            callback.onComplete(BaseResponse.error("INVALID_SHIPPER", "Shipper information is missing"));
            return;
        }
        
        // Sử dụng hàm acceptOrder nhưng gọi từ phía Admin để gán
        acceptOrder(orderId, shipper.getUserId(), callback);
    }

    /**
     * Lấy danh sách Shipper có sẵn để gán đơn (Dữ liệu thật từ Supabase)
     * @param restaurantId - ID nhà hàng để lọc shipper (null = lấy tất cả)
     */
    public void getAssignableShippers(String restaurantId, RepositoryCallback<List<Shipper>> callback) {
        if (restaurantId == null || restaurantId.isEmpty()) {
            orderClient.getShippers(new BaseSupabaseClient.ApiCallback<List<Shipper>>() {
                @Override
                public void onSuccess(List<Shipper> result) {
                    callback.onComplete(BaseResponse.success(result));
                }

                @Override
                public void onError(String error) {
                    callback.onComplete(BaseResponse.error("FETCH_ERROR", error));
                }
            });
        } else {
            orderClient.getShippersByRestaurant(restaurantId, new BaseSupabaseClient.ApiCallback<List<Shipper>>() {
                @Override
                public void onSuccess(List<Shipper> result) {
                    callback.onComplete(BaseResponse.success(result));
                }

                @Override
                public void onError(String error) {
                    callback.onComplete(BaseResponse.error("FETCH_ERROR", error));
                }
            });
        }
    }

    /**
     * Chấp nhận đơn hàng qua RPC accept_order_v2
     */
    public void acceptOrder(String orderId, String shipperUserId, RepositoryCallback<Void> callback) {
        orderClient.acceptOrder(orderId, shipperUserId, new BaseSupabaseClient.ApiCallback<OrderClient.RpcResponse>() {
            @Override
            public void onSuccess(OrderClient.RpcResponse result) {
                if (result.isSuccess()) {
                    callback.onComplete(BaseResponse.success(null));
                } else {
                    callback.onComplete(BaseResponse.error("RPC_ERROR", result.getMessage()));
                }
            }

            @Override
            public void onError(String error) {
                callback.onComplete(BaseResponse.error("NETWORK_ERROR", error));
            }
        });
    }

    /**
     * Xác nhận lấy hàng qua RPC pickup_order
     */
    public void pickupOrder(String orderId, String shipperUserId, RepositoryCallback<Void> callback) {
        orderClient.pickupOrder(orderId, shipperUserId, new BaseSupabaseClient.ApiCallback<OrderClient.RpcResponse>() {
            @Override
            public void onSuccess(OrderClient.RpcResponse result) {
                if (result.isSuccess()) {
                    callback.onComplete(BaseResponse.success(null));
                } else {
                    callback.onComplete(BaseResponse.error("RPC_ERROR", result.getMessage()));
                }
            }

            @Override
            public void onError(String error) {
                callback.onComplete(BaseResponse.error("NETWORK_ERROR", error));
            }
        });
    }

    /**
     * Hoàn tất đơn hàng qua RPC complete_order
     */
    public void completeOrder(String orderId, String shipperUserId, RepositoryCallback<Void> callback) {
        orderClient.completeOrder(orderId, shipperUserId, new BaseSupabaseClient.ApiCallback<OrderClient.RpcResponse>() {
            @Override
            public void onSuccess(OrderClient.RpcResponse result) {
                if (result.isSuccess()) {
                    callback.onComplete(BaseResponse.success(null));
                } else {
                    callback.onComplete(BaseResponse.error("RPC_ERROR", result.getMessage()));
                }
            }

            @Override
            public void onError(String error) {
                callback.onComplete(BaseResponse.error("NETWORK_ERROR", error));
            }
        });
    }
}
