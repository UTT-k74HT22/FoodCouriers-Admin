package com.utt.foodcouriers_admin.ui.order;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.data.model.OrderStatus;
import com.utt.foodcouriers_admin.data.model.Shipper;
import com.utt.foodcouriers_admin.data.repository.OrderRepository;

import java.util.List;

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

    public OrderViewModel() {
        orderRepository = OrderRepository.getInstance();
    }

    // Lấy danh sách đơn hàng
    public void fetchOrders(OrderStatus status, String query, String shipperId) {
        _isLoading.setValue(true);

        RepositoryCallback<List<Order>> callback = response -> {
            _isLoading.setValue(false);
            if (response.isSuccess()) {
                _orders.setValue(response.getData());
            } else {
                _errorMessage.setValue(response.getMessage());
            }
        };
        if (query != null && !query.trim().isEmpty()) {
            orderRepository.searchOrders(query.trim(), callback);
        }
        else {
            orderRepository.getOrders(status, "", shipperId, callback);
        }
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
}
