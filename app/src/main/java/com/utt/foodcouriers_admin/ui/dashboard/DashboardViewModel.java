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
                    java.util.List<com.utt.foodcouriers_admin.data.model.Order> allOrders = response.getData();

                    // Lấy ngày hiện tại (Local Time) theo định dạng yyyy-MM-dd
                    java.text.SimpleDateFormat localFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                    String todayStr = localFormat.format(new java.util.Date());
                    
                    // Format để parse chuỗi UTC từ Supabase (ISO 8601)
                    java.text.SimpleDateFormat parser = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
                    parser.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

                    int total = 0;
                    double revenue = 0;
                    int pending = 0;
                    int completed = 0;
                    int cancelled = 0;

                    for (com.utt.foodcouriers_admin.data.model.Order o : allOrders) {
                        String createdAt = o.getCreatedAt();
                        if (createdAt != null) {
                            try {
                                // 1. Parse chuỗi UTC thành Date object
                                java.util.Date orderDate = parser.parse(createdAt);

                                // 2. Chuyển sang chuỗi yyyy-MM-dd (theo Local Time của máy)
                                String orderDayStr = localFormat.format(orderDate);

                                // 3. Chỉ tính nếu đúng là ngày hôm nay
                                if (todayStr.equals(orderDayStr)) {
                                    String status = o.getStatus();

                                    // Đếm tất cả đơn hàng trong ngày không phân biệt trạng thái
                                    total++;

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
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    DashboardStats newStats = new DashboardStats();
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

        // 3. Load Top Items và thực hiện gom nhóm, tính toán
        repository.getTopItems(new RepositoryCallback<java.util.List<com.utt.foodcouriers_admin.data.model.OrderItem>>() {
            @Override
            public void onComplete(BaseResponse<java.util.List<com.utt.foodcouriers_admin.data.model.OrderItem>> response) {
                if (response.isSuccess() && response.getData() != null) {
                    java.util.List<com.utt.foodcouriers_admin.data.model.OrderItem> rawItems = response.getData();

                    // Sử dụng Map để gom nhóm theo Tên món ăn
                    java.util.Map<String, com.utt.foodcouriers_admin.data.model.OrderItem> groupedMap = new java.util.HashMap<>();

                    for (com.utt.foodcouriers_admin.data.model.OrderItem item : rawItems) {
                        String name = item.getMenuItemName();
                        if (name == null || name.isEmpty()) continue;

                        if (groupedMap.containsKey(name)) {
                            com.utt.foodcouriers_admin.data.model.OrderItem existing = groupedMap.get(name);
                            existing.setQuantity(existing.getQuantity() + item.getQuantity());
                        } else {
                            // Tạo bản sao để tránh làm thay đổi dữ liệu gốc
                            com.utt.foodcouriers_admin.data.model.OrderItem clone = new com.utt.foodcouriers_admin.data.model.OrderItem();
                            clone.setMenuItemName(name);
                            clone.setQuantity(item.getQuantity());
                            clone.setMenuItemPrice(item.getMenuItemPrice());
                            groupedMap.put(name, clone);
                        }
                    }

                    // Chuyển sang List để sắp xếp
                    java.util.List<com.utt.foodcouriers_admin.data.model.OrderItem> sortedList = new java.util.ArrayList<>(groupedMap.values());

                    // Sắp xếp giảm dần theo số lượng (Quantity)
                    java.util.Collections.sort(sortedList, (a, b) -> Integer.compare(b.getQuantity(), a.getQuantity()));

                    // Lấy Top 5 món bán chạy nhất
                    if (sortedList.size() > 5) {
                        topItems.postValue(sortedList.subList(0, 5));
                    } else {
                        topItems.postValue(sortedList);
                    }
                }
            }
        });
    }

    private void checkAllLoaded() {
        // Simple logic to stop loading spinner
        isLoading.postValue(false);
    }
}
