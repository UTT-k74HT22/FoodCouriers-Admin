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
    public void fetchOrders(OrderStatus status, String query) {
        _isLoading.setValue(true);
        orderRepository.getOrders(status, query, new RepositoryCallback<List<Order>>() {
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

    /** Cập nhật trạng thái đơn hàng (ví dụ: Chờ -> Xác nhận) */
    public void updateStatus(String orderId, OrderStatus status) {
        _isLoading.setValue(true);
        orderRepository.updateStatus(orderId, status, new RepositoryCallback<Void>() {
            @Override
            public void onComplete(BaseResponse<Void> response) {
                _isLoading.setValue(false);
                if (response.isSuccess()) {
                    // Cập nhật thành công, gửi tín hiệu null hoặc một chuỗi đặc biệt
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
}
