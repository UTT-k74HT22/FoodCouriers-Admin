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
     * Lấy danh sách đơn hàng đang chờ shipper (Searching hoặc Ready for Pickup)
     */
    public void getAvailableOrders(RepositoryCallback<List<Order>> callback) {
        String selectClause = "*,user:users!user_id(*),restaurant:restaurants!restaurant_id(id,name)";
        // Lọc đơn đang searching hoặc đã ready_for_pickup nhưng chưa có shipper
        String filter = "delivery_status=eq.searching&shipper_id=is.null";
        
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
        String selectClause = "*,user:users!user_id(*),restaurant:restaurants!restaurant_id(id,name)";
        // Lọc đơn đã gán cho shipper này nhưng chưa hoàn thành
        String filter = "shipper_id=eq." + shipperUserId + "&delivery_status=in.(assigned,arriving_pickup,waiting_pickup,picked_up)";
        
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
        String selectClause = "*,user:users!user_id(*),restaurant:restaurants!restaurant_id(id,name)";
        String filter = "shipper_id=eq." + shipperUserId + "&delivery_status=eq.completed&order=created_at.desc";
        
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
}
