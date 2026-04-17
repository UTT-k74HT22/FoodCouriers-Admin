package com.utt.foodcouriers_admin.data.repository;

import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.data.model.OrderStatus;
import com.utt.foodcouriers_admin.data.remote.BaseSupabaseClient;
import com.utt.foodcouriers_admin.data.remote.OrderClient;
import com.utt.foodcouriers_admin.data.repository.base.BaseSupabaseRepository;

import java.util.List;

public class DeliveryRepository extends BaseSupabaseRepository {

    private static DeliveryRepository instance;
    private final OrderClient orderClient;

    public static synchronized DeliveryRepository getInstance() {
        if (instance == null) {
            instance = new DeliveryRepository();
        }
        return instance;
    }

    private DeliveryRepository() {
        this.orderClient = OrderClient.getInstance();
    }

    /**
     * Lấy danh sách đơn hàng chưa có shipper
     * Bao gồm: unassigned (chưa gán) và searching (đang tìm tài xế)
     */
    public void getAvailableOrders(RepositoryCallback<List<Order>> callback) {
        String selectClause = "*,user:users!user_id(*),restaurant:restaurants!restaurant_id(id,name),items:order_items(*)";
        // Lọc đơn chưa có shipper: unassigned hoặc searching
        String filter = "or(delivery_status.eq.unassigned,delivery_status.eq.searching)"
                + "&shipper_id=is.null"
                + "&status=not.in.(cancelled,delivered)"
                + "&order=created_at.desc";
        
        orderClient.getOrders(selectClause, filter, new BaseSupabaseClient.ApiCallback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> result) {
                postResponse(callback, BaseResponse.success(result));
            }

            @Override
            public void onError(String error) {
                postResponse(callback, BaseResponse.error("FETCH_ERROR", error));
            }
        });
    }

    /**
     * Lấy danh sách đơn hàng shipper đang giao
     */
    public void getActiveDeliveries(String shipperUserId, RepositoryCallback<List<Order>> callback) {
        String selectClause = "*,user:users!user_id(*),restaurant:restaurants!restaurant_id(id,name),items:order_items(*)";
        String filter = "shipper_id=eq." + shipperUserId
                + "&status=not.in.(cancelled,delivered)"
                + "&order=created_at.desc";
        
        orderClient.getOrders(selectClause, filter, new BaseSupabaseClient.ApiCallback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> result) {
                postResponse(callback, BaseResponse.success(result));
            }

            @Override
            public void onError(String error) {
                postResponse(callback, BaseResponse.error("FETCH_ERROR", error));
            }
        });
    }

    /**
     * Lấy lịch sử giao hàng
     */
    public void getDeliveryHistory(String shipperUserId, RepositoryCallback<List<Order>> callback) {
        String selectClause = "*,user:users!user_id(*),restaurant:restaurants!restaurant_id(id,name),items:order_items(*)";
        String filter = "shipper_id=eq." + shipperUserId + "&status=eq.delivered&order=created_at.desc";
        
        orderClient.getOrders(selectClause, filter, new BaseSupabaseClient.ApiCallback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> result) {
                postResponse(callback, BaseResponse.success(result));
            }

            @Override
            public void onError(String error) {
                postResponse(callback, BaseResponse.error("FETCH_ERROR", error));
            }
        });
    }

    /**
     * Nhận đơn hàng (Sử dụng RPC accept_order_v2 để đồng bộ với logic mới)
     */
    public void acceptOrder(String orderId, String shipperUserId, RepositoryCallback<Void> callback) {
        orderClient.acceptOrder(orderId, shipperUserId, new BaseSupabaseClient.ApiCallback<OrderClient.RpcResponse>() {
            @Override
            public void onSuccess(OrderClient.RpcResponse result) {
                if (result.isSuccess()) {
                    postResponse(callback, BaseResponse.success(null));
                } else {
                    postResponse(callback, BaseResponse.error("RPC_ERROR", result.getMessage()));
                }
            }

            @Override
            public void onError(String error) {
                postResponse(callback, BaseResponse.error("NETWORK_ERROR", error));
            }
        });
    }

    /**
     * Cập nhật trạng thái đơn hàng (Đã lấy hàng, Đã giao)
     * Lưu ý: ViewModel có thể gọi trực tiếp các hàm pickupOrder/completeOrder của Repository này
     */
    public void pickupOrder(String orderId, String shipperUserId, RepositoryCallback<Void> callback) {
        orderClient.pickupOrder(orderId, shipperUserId, new BaseSupabaseClient.ApiCallback<OrderClient.RpcResponse>() {
            @Override
            public void onSuccess(OrderClient.RpcResponse result) {
                if (result.isSuccess()) {
                    postResponse(callback, BaseResponse.success(null));
                } else {
                    postResponse(callback, BaseResponse.error("RPC_ERROR", result.getMessage()));
                }
            }

            @Override
            public void onError(String error) {
                postResponse(callback, BaseResponse.error("NETWORK_ERROR", error));
            }
        });
    }

    public void completeOrder(String orderId, String shipperUserId, RepositoryCallback<Void> callback) {
        orderClient.completeOrder(orderId, shipperUserId, new BaseSupabaseClient.ApiCallback<OrderClient.RpcResponse>() {
            @Override
            public void onSuccess(OrderClient.RpcResponse result) {
                if (result.isSuccess()) {
                    postResponse(callback, BaseResponse.success(null));
                } else {
                    postResponse(callback, BaseResponse.error("RPC_ERROR", result.getMessage()));
                }
            }

            @Override
            public void onError(String error) {
                postResponse(callback, BaseResponse.error("NETWORK_ERROR", error));
            }
        });
    }

    public void getOrderById(String orderId, RepositoryCallback<Order> callback) {
        orderClient.getOrders("*,restaurant:restaurants!restaurant_id(id,name),items:order_items(*)", "id=eq." + orderId, new BaseSupabaseClient.ApiCallback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> result) {
                if (result != null && !result.isEmpty()) {
                    Order order = result.get(0);
                    if (order.getUserId() != null) {
                        fetchUserDetails(order.getUserId(), order, callback);
                    } else {
                        postResponse(callback, BaseResponse.success(order));
                    }
                } else {
                    postResponse(callback, BaseResponse.error("NOT_FOUND", "Order not found"));
                }
            }

            @Override
            public void onError(String error) {
                postResponse(callback, BaseResponse.error("FETCH_ERROR", error));
            }
        });
    }

    private void fetchUserDetails(String userId, Order order, RepositoryCallback<Order> callback) {
        orderClient.getUsers("id=eq." + userId, new BaseSupabaseClient.ApiCallback<List<Order.OrderUser>>() {
            @Override
            public void onSuccess(List<Order.OrderUser> users) {
                android.util.Log.d("DeliveryRepo", "Fetched users count: " + (users != null ? users.size() : 0));
                if (users != null && !users.isEmpty()) {
                    order.setUser(users.get(0));
                    android.util.Log.d("DeliveryRepo", "User set: " + users.get(0).getFullName());
                } else {
                    android.util.Log.d("DeliveryRepo", "User not found or empty");
                }
                postResponse(callback, BaseResponse.success(order));
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("DeliveryRepo", "Error fetching user: " + error);
                postResponse(callback, BaseResponse.success(order));
            }
        });
    }
}
