package com.utt.foodcouriers_admin.data.repository;

import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.data.model.OrderStatus;
import com.utt.foodcouriers_admin.data.repository.base.BaseSupabaseRepository;
import com.utt.foodcouriers_admin.ui.order.OrderMockDataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeliveryRepository extends BaseSupabaseRepository {

    private static DeliveryRepository instance;

    public static synchronized DeliveryRepository getInstance() {
        if (instance == null) {
            instance = new DeliveryRepository();
        }
        return instance;
    }

    private DeliveryRepository() {}

    /**
     * Lấy danh sách đơn hàng đang chờ shipper (ready_for_pickup)
     */
    public void getAvailableOrders(RepositoryCallback<List<Order>> callback) {
        // Trong bản test, lọc từ MockDataSource
        List<Order> available = new ArrayList<>();
        for (Order order : OrderMockDataSource.getOrders()) {
            if (order.getOrderStatus() == OrderStatus.READY_FOR_PICKUP && order.getShipperId() == null) {
                available.add(order);
            }
        }
        postResponse(callback, BaseResponse.success(available));
        
        // Sau này chuyển sang Supabase:
        // fetchList("orders", "?status=eq.ready_for_pickup&shipper_id=is.null", Order[].class, callback);
    }

    /**
     * Lấy danh sách đơn hàng shipper đang giao
     */
    public void getActiveDeliveries(String shipperUserId, RepositoryCallback<List<Order>> callback) {
        List<Order> active = new ArrayList<>();
        for (Order order : OrderMockDataSource.getOrders()) {
            if (order.getShipperId() != null && order.getShipperId().equals(shipperUserId) 
                && order.getOrderStatus() == OrderStatus.DELIVERING) {
                active.add(order);
            }
        }
        postResponse(callback, BaseResponse.success(active));
        
        // fetchList("orders", "?shipper_id=eq." + shipperUserId + "&status=eq.delivering", Order[].class, callback);
    }

    /**
     * Lấy lịch sử giao hàng
     */
    public void getDeliveryHistory(String shipperUserId, RepositoryCallback<List<Order>> callback) {
        List<Order> history = new ArrayList<>();
        for (Order order : OrderMockDataSource.getOrders()) {
            if (order.getShipperId() != null && order.getShipperId().equals(shipperUserId) 
                && order.getOrderStatus() == OrderStatus.DELIVERED) {
                history.add(order);
            }
        }
        postResponse(callback, BaseResponse.success(history));
    }

    /**
     * Nhận đơn hàng (Gọi qua RPC để tránh tranh chấp)
     */
    public void acceptOrder(String orderId, String shipperUserId, RepositoryCallback<Void> callback) {
        // Giả lập logic trong MockDataSource
        Order order = OrderMockDataSource.getOrderById(orderId);
        if (order != null && order.getOrderStatus() == OrderStatus.READY_FOR_PICKUP && order.getShipperId() == null) {
            order.setShipperId(shipperUserId);
            order.setStatus(OrderStatus.DELIVERING.getValue());
            postResponse(callback, BaseResponse.success(null));
        } else {
            postResponse(callback, BaseResponse.error("ALREADY_TAKEN", "Đơn hàng đã được nhận bởi người khác."));
        }

        /*
        // Thực tế sẽ gọi RPC trên Supabase
        Map<String, Object> params = new HashMap<>();
        params.put("p_order_id", orderId);
        params.put("p_shipper_user_id", shipperUserId);
        
        executeRpc("accept_order", params, Void.class, callback);
        */
    }

    /**
     * Cập nhật trạng thái đơn hàng (Đã lấy hàng, Đã giao)
     */
    public void updateStatus(String orderId, OrderStatus status, RepositoryCallback<Order> callback) {
        Order order = OrderMockDataSource.updateStatus(orderId, status);
        if (order != null) {
            postResponse(callback, BaseResponse.success(order));
        } else {
            postResponse(callback, BaseResponse.error("UPDATE_FAILED", "Không thể cập nhật trạng thái."));
        }
    }
}
