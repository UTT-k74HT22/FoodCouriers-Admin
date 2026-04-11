package com.utt.foodcouriers_admin.data.repository;

import android.text.TextUtils;

import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.data.model.OrderStatus;
import com.utt.foodcouriers_admin.data.model.Shipper;
import com.utt.foodcouriers_admin.ui.order.OrderMockDataSource;

import java.util.ArrayList;
import java.util.List;

public class OrderRepository {

    private static OrderRepository instance;

    public static synchronized OrderRepository getInstance() {
        if (instance == null) {
            instance = new OrderRepository();
        }
        return instance;
    }

    public void getOrders(OrderStatus status, String query, RepositoryCallback<List<Order>> callback) {
        List<Order> filtered = new ArrayList<>();
        for (Order order : OrderMockDataSource.getOrders()) {
            if (status != null && order.getOrderStatus() != status) {
                continue;
            }
            if (!matchesQuery(order, query)) {
                continue;
            }
            filtered.add(order);
        }
        callback.onComplete(BaseResponse.success(filtered));
    }

    public void getOrderById(String orderId, RepositoryCallback<Order> callback) {
        Order order = OrderMockDataSource.getOrderById(orderId);
        if (order == null) {
            callback.onComplete(BaseResponse.error("NOT_FOUND", "Order not found"));
            return;
        }
        callback.onComplete(BaseResponse.success(order));
    }

    public void updateStatus(String orderId, OrderStatus status, RepositoryCallback<Order> callback) {
        Order order = OrderMockDataSource.updateStatus(orderId, status);
        if (order == null) {
            callback.onComplete(BaseResponse.error("UPDATE_FAILED", "Unable to update order"));
            return;
        }
        callback.onComplete(BaseResponse.success(order));
    }

    public void assignShipper(String orderId, Shipper shipper, RepositoryCallback<Order> callback) {
        Order order = OrderMockDataSource.assignShipper(orderId, shipper);
        if (order == null) {
            callback.onComplete(BaseResponse.error("UPDATE_FAILED", "Unable to assign shipper"));
            return;
        }
        callback.onComplete(BaseResponse.success(order));
    }

    public void getAssignableShippers(RepositoryCallback<List<Shipper>> callback) {
        callback.onComplete(BaseResponse.success(OrderMockDataSource.getShippers()));
    }

    private boolean matchesQuery(Order order, String query) {
        if (TextUtils.isEmpty(query)) {
            return true;
        }
        String normalized = query.trim().toLowerCase();
        boolean matchCode = order.getOrderCode() != null && order.getOrderCode().toLowerCase().contains(normalized);
        boolean matchCustomer = order.getUser() != null && order.getUser().getFullName() != null
                && order.getUser().getFullName().toLowerCase().contains(normalized);
        boolean matchPhone = order.getUser() != null && order.getUser().getPhone() != null
                && order.getUser().getPhone().contains(normalized);
        boolean matchRestaurant = order.getRestaurant() != null && order.getRestaurant().getName() != null
                && order.getRestaurant().getName().toLowerCase().contains(normalized);
        return matchCode || matchCustomer || matchPhone || matchRestaurant;
    }
}
