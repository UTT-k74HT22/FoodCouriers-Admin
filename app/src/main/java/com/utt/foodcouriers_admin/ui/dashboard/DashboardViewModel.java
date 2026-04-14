package com.utt.foodcouriers_admin.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.DashboardStats;
import com.utt.foodcouriers_admin.data.repository.DashboardRepository;

/**
 * DashboardViewModel - Đã sửa lỗi để khớp với RepositoryCallback của dự án.
 */
public class DashboardViewModel extends ViewModel {

    private final DashboardRepository repository;

    private final MutableLiveData<DashboardStats> stats = new MutableLiveData<>();
    private final MutableLiveData<java.util.List<com.utt.foodcouriers_admin.data.model.Order>> recentOrders = new MutableLiveData<>();
    private final MutableLiveData<java.util.List<com.utt.foodcouriers_admin.data.model.OrderItem>> topItems = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public DashboardViewModel() {
        this.repository = DashboardRepository.getInstance();
    }

    public LiveData<DashboardStats> getStats() { return stats; }
    public LiveData<java.util.List<com.utt.foodcouriers_admin.data.model.Order>> getRecentOrders() { return recentOrders; }
    public LiveData<java.util.List<com.utt.foodcouriers_admin.data.model.OrderItem>> getTopItems() { return topItems; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadDashboardData() {
        isLoading.setValue(true);
        
        // 1. Tự tính toán thống kê từ danh sách đơn hàng thực tế của hôm nay (Không dùng View DB lỗi)
        repository.getTodayOrders(new RepositoryCallback<java.util.List<com.utt.foodcouriers_admin.data.model.Order>>() {
            @Override
            public void onComplete(BaseResponse<java.util.List<com.utt.foodcouriers_admin.data.model.Order>> response) {
                if (response.isSuccess() && response.getData() != null) {
                    java.util.List<com.utt.foodcouriers_admin.data.model.Order> todayOrders = response.getData();
                    
                    DashboardStats newStats = new DashboardStats();
                    int total = todayOrders.size();
                    double revenue = 0;
                    int pending = 0;
                    int completed = 0;
                    int cancelled = 0;

                    for (com.utt.foodcouriers_admin.data.model.Order o : todayOrders) {
                        String status = o.getStatus();
                        if ("delivered".equals(status)) {
                            completed++;
                            revenue += o.getTotal();
                        } else if ("cancelled".equals(status)) {
                            cancelled++;
                        } else if ("pending".equals(status) || "confirmed".equals(status) || 
                                   "preparing".equals(status) || "ready_for_pickup".equals(status) || 
                                   "delivering".equals(status)) {
                            pending++;
                        }
                    }

                    newStats.setTotalOrders(total);
                    newStats.setTotalRevenue(revenue);
                    newStats.setPendingOrders(pending);
                    newStats.setCompletedOrders(completed);
                    newStats.setCancelledOrders(cancelled);
                    
                    stats.postValue(newStats);
                }
                checkAllLoaded();
            }
        });

        // 2. Load Recent Orders
        repository.getRecentOrders(new RepositoryCallback<java.util.List<com.utt.foodcouriers_admin.data.model.Order>>() {
            @Override
            public void onComplete(BaseResponse<java.util.List<com.utt.foodcouriers_admin.data.model.Order>> response) {
                if (response.isSuccess()) {
                    recentOrders.postValue(response.getData());
                }
                checkAllLoaded();
            }
        });

        // 3. Load Top Items (Tạm thời lấy dữ liệu đơn giản)
        repository.getTopItems(new RepositoryCallback<java.util.List<com.utt.foodcouriers_admin.data.model.OrderItem>>() {
            @Override
            public void onComplete(BaseResponse<java.util.List<com.utt.foodcouriers_admin.data.model.OrderItem>> response) {
                if (response.isSuccess()) {
                    topItems.postValue(response.getData());
                }
            }
        });
    }

    private void checkAllLoaded() {
        // Simple logic to stop loading spinner
        isLoading.postValue(false);
    }
}
