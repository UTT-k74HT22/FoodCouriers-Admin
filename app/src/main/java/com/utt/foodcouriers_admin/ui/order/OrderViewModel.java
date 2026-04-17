package com.utt.foodcouriers_admin.ui.order;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.gson.JsonObject;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.data.model.OrderStatus;
import com.utt.foodcouriers_admin.data.model.Shipper;
import com.utt.foodcouriers_admin.data.repository.OrderRepository;

import java.util.List;

import com.utt.foodcouriers_admin.data.realtime.OrderRealtimeManager;

/** 
 * Logic chính của order - MVVM ViewModel
 */
public class OrderViewModel extends ViewModel {
    private final OrderRepository orderRepository;
    
    // Danh sách đơn hàng
    private final MutableLiveData<List<Order>> _orders = new MutableLiveData<>();
    public final LiveData<List<Order>> orders = _orders;
    
    // Trạng thái tải dữ liệu
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    // Thông báo lỗi
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    // Realtime events
    private final MutableLiveData<JsonObject> _newOrderEvent = new MutableLiveData<>();
    public final LiveData<JsonObject> newOrderEvent = _newOrderEvent;

    private final MutableLiveData<JsonObject> _orderUpdatedEvent = new MutableLiveData<>();
    public final LiveData<JsonObject> orderUpdatedEvent = _orderUpdatedEvent;

    private final MutableLiveData<JsonObject> _orderDeletedEvent = new MutableLiveData<>();
    public final LiveData<JsonObject> orderDeletedEvent = _orderDeletedEvent;

    public OrderViewModel() {
        orderRepository = OrderRepository.getInstance();
    }

    // Lấy danh sách đơn hàng
    public void fetchOrders(OrderStatus status, String query) {
        fetchOrders(status, query, null);
    }

    public void fetchOrders(OrderStatus status, String query, String shipperId) {
        _isLoading.setValue(true);
        orderRepository.getOrders(status, query, shipperId, new RepositoryCallback<List<Order>>() {
            @Override
            public void onComplete(BaseResponse<List<Order>> response) {
                _isLoading.setValue(false);
                if (response.isSuccess()) {
                    _orders.setValue(response.getData());
                } else {
                    _errorMessage.setValue(response.getMessage());
                }
            }
        });
    }

    /** Lấy danh sách đơn hàng đang chờ Shipper nhận (Dành cho Shipper) */
    public void fetchAvailableOrders() {
        _isLoading.setValue(true);
        com.utt.foodcouriers_admin.data.repository.DeliveryRepository.getInstance()
                .getAvailableOrders(new RepositoryCallback<List<Order>>() {
            @Override
            public void onComplete(BaseResponse<List<Order>> response) {
                _isLoading.setValue(false);
                if (response.isSuccess()) {
                    _orders.setValue(response.getData());
                } else {
                    _errorMessage.setValue(response.getMessage());
                }
            }
        });
    }

    /** Shipper tự nhận đơn hàng */
    public void acceptOrder(String orderId, String shipperUserId) {
        _isLoading.setValue(true);
        com.utt.foodcouriers_admin.data.repository.DeliveryRepository.getInstance()
                .acceptOrder(orderId, shipperUserId, new RepositoryCallback<Void>() {
            @Override
            public void onComplete(BaseResponse<Void> response) {
                _isLoading.setValue(false);
                if (response.isSuccess()) {
                    _errorMessage.setValue(null);
                } else {
                    _errorMessage.setValue(response.getMessage());
                }
            }
        });
    }

    /** Cập nhật trạng thái đơn hàng (ví dụ: Chờ -> Xác nhận) */
    public void updateStatus(String orderId, OrderStatus status) {
        _isLoading.setValue(true);
        orderRepository.updateStatus(orderId, status, new RepositoryCallback<Void>() {
            @Override
            public void onComplete(BaseResponse<Void> response) {
                _isLoading.setValue(false);
                if (response.isSuccess()) {
                    _errorMessage.setValue(null); 
                } else {
                    _errorMessage.setValue(response.getMessage());
                }
            }
        });
    }

    /** Shipper xác nhận lấy hàng */
    public void pickupOrder(String orderId, String shipperUserId) {
        _isLoading.setValue(true);
        orderRepository.pickupOrder(orderId, shipperUserId, new RepositoryCallback<Void>() {
            @Override
            public void onComplete(BaseResponse<Void> response) {
                _isLoading.setValue(false);
                if (response.isSuccess()) {
                    _errorMessage.setValue(null);
                } else {
                    _errorMessage.setValue(response.getMessage());
                }
            }
        });
    }

    /** Shipper xác nhận giao hàng thành công */
    public void completeOrder(String orderId, String shipperUserId) {
        _isLoading.setValue(true);
        orderRepository.completeOrder(orderId, shipperUserId, new RepositoryCallback<Void>() {
            @Override
            public void onComplete(BaseResponse<Void> response) {
                _isLoading.setValue(false);
                if (response.isSuccess()) {
                    _errorMessage.setValue(null);
                } else {
                    _errorMessage.setValue(response.getMessage());
                }
            }
        });
    }

    /** Phân giao hàng cho Shipper */
    public void assignShipper(String orderId, Shipper shipper) {
        _isLoading.setValue(true);
        orderRepository.assignShipper(orderId, shipper, new RepositoryCallback<Void>() {
            @Override
            public void onComplete(BaseResponse<Void> response) {
                _isLoading.setValue(false);
                if (response.isSuccess()) {
                    _errorMessage.setValue(null);
                } else {
                    _errorMessage.setValue(response.getMessage());
                }
            }
        });
    }

    /** Lấy danh sách đơn hàng đang giao của shipper */
    public void fetchActiveDeliveries(String shipperUserId) {
        _isLoading.setValue(true);
        com.utt.foodcouriers_admin.data.repository.DeliveryRepository.getInstance()
                .getActiveDeliveries(shipperUserId, new RepositoryCallback<List<Order>>() {
            @Override
            public void onComplete(BaseResponse<List<Order>> response) {
                _isLoading.setValue(false);
                if (response.isSuccess()) {
                    _orders.setValue(response.getData());
                } else {
                    _errorMessage.setValue(response.getMessage());
                }
            }
        });
    }

    /** Lấy lịch sử giao hàng của shipper */
    public void fetchDeliveryHistory(String shipperUserId) {
        _isLoading.setValue(true);
        com.utt.foodcouriers_admin.data.repository.DeliveryRepository.getInstance()
                .getDeliveryHistory(shipperUserId, new RepositoryCallback<List<Order>>() {
            @Override
            public void onComplete(BaseResponse<List<Order>> response) {
                _isLoading.setValue(false);
                if (response.isSuccess()) {
                    _orders.setValue(response.getData());
                } else {
                    _errorMessage.setValue(response.getMessage());
                }
            }
        });
    }

    // ========== Realtime Event Handlers ==========

    /** Gọi khi có đơn hàng mới */
    public void onNewOrder(JsonObject order) {
        _newOrderEvent.postValue(order);
        reloadOrders();
    }

    /** Gọi khi đơn hàng được cập nhật */
    public void onOrderUpdated(JsonObject newOrder, JsonObject oldOrder) {
        _orderUpdatedEvent.postValue(newOrder);
        reloadOrders();
    }

    /** Gọi khi đơn hàng bị xóa */
    public void onOrderDeleted(JsonObject oldOrder) {
        _orderDeletedEvent.postValue(oldOrder);
        reloadOrders();
    }

    private void reloadOrders() {
        if (_orders.getValue() != null) {
            if (isShipperMode) {
                fetchAvailableOrders();
            } else if (currentStatus != null) {
                fetchOrders(currentStatus, currentQuery, currentShipperId);
            }
        }
    }

    private OrderStatus currentStatus;
    private String currentQuery;
    private String currentShipperId;
    private boolean isShipperMode = false;

    public void setShipperMode(boolean isShipper) {
        this.isShipperMode = isShipper;
    }

    public void setCurrentFilter(OrderStatus status, String query, String shipperId) {
        this.currentStatus = status;
        this.currentQuery = query;
        this.currentShipperId = shipperId;
    }
}
