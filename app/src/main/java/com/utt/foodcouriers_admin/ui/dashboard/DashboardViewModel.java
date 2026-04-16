package com.utt.foodcouriers_admin.ui.dashboard;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.DashboardStats;
import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.data.model.OrderItem;
import com.utt.foodcouriers_admin.data.repository.DashboardRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * DashboardViewModel - Cập nhật logic xử lý biểu đồ 7 ngày.
 */
public class DashboardViewModel extends ViewModel {

    private final DashboardRepository repository;

    private final MutableLiveData<DashboardStats> stats = new MutableLiveData<>();
    private final MutableLiveData<List<Order>> recentOrders = new MutableLiveData<>();
    private final MutableLiveData<List<OrderItem>> topItems = new MutableLiveData<>();
    private final MutableLiveData<double[]> weeklyRevenue = new MutableLiveData<>(); // Doanh thu 7 ngày
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private static final int REFRESH_INTERVAL = 15000;
    private boolean isRefreshing = false;

    public DashboardViewModel() {
        this.repository = DashboardRepository.getInstance();
    }

    public LiveData<DashboardStats> getStats() { return stats; }
    public LiveData<List<Order>> getRecentOrders() { return recentOrders; }
    public LiveData<List<OrderItem>> getTopItems() { return topItems; }
    public LiveData<double[]> getWeeklyRevenue() { return weeklyRevenue; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void startAutoRefresh() { if (!isRefreshing) { isRefreshing = true; refreshHandler.post(refreshRunnable); } }
    public void stopAutoRefresh() { isRefreshing = false; refreshHandler.removeCallbacks(refreshRunnable); }

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() { if (isRefreshing) { loadDashboardData(false); refreshHandler.postDelayed(this, REFRESH_INTERVAL); } }
    };

    public void loadDashboardData() { loadDashboardData(true); }

    public void loadDashboardData(boolean showLoading) {
        if (showLoading) isLoading.setValue(true);
        
        // 1. Lấy dữ liệu 7 ngày qua để làm Stats + Biểu đồ
        repository.getRecentDaysOrders(new RepositoryCallback<List<Order>>() {
            @Override
            public void onComplete(BaseResponse<List<Order>> response) {
                if (response.isSuccess() && response.getData() != null) {
                    processWeeklyStatsAndChart(response.getData());
                }
                checkAllLoaded();
            }
        });

        // 2. Load Recent Orders
        repository.getRecentOrders(new RepositoryCallback<List<Order>>() {
            @Override
            public void onComplete(BaseResponse<List<Order>> response) {
                if (response.isSuccess()) { recentOrders.postValue(response.getData()); }
                checkAllLoaded();
            }
        });

        // 3. Load Top Items
        repository.getTopItems(new RepositoryCallback<List<OrderItem>>() {
            @Override
            public void onComplete(BaseResponse<List<OrderItem>> response) {
                if (response.isSuccess() && response.getData() != null) { processTopItems(response.getData()); }
            }
        });
    }

    private void processWeeklyStatsAndChart(List<Order> allOrders) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        parser.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Tạo mảng 7 ngày gần nhất (Thứ tự: Hôm kia -> ... -> Hôm qua -> Hôm nay)
        double[] revenueData = new double[7];
        String[] dates = new String[7];
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            dates[i] = sdf.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        int tOrders = 0; double tRevenue = 0; int tPending = 0; int tCompleted = 0;
        int yOrders = 0; double yRevenue = 0;

        for (Order o : allOrders) {
            if (o.getCreatedAt() == null) continue;
            try {
                Date orderDate = parser.parse(o.getCreatedAt());
                String dayStr = sdf.format(orderDate);

                // Tính toán cho Stats (Hôm nay & Hôm qua)
                if (dates[6].equals(dayStr)) {
                    tOrders++;
                    if ("delivered".equals(o.getStatus())) { tCompleted++; tRevenue += o.getTotal(); }
                    else if (!"cancelled".equals(o.getStatus())) { tPending++; }
                } else if (dates[5].equals(dayStr)) {
                    yOrders++;
                    if ("delivered".equals(o.getStatus())) { yRevenue += o.getTotal(); }
                }

                // Tính toán cho Biểu đồ (7 ngày)
                if ("delivered".equals(o.getStatus())) {
                    for (int i = 0; i < 7; i++) {
                        if (dates[i].equals(dayStr)) {
                            revenueData[i] += o.getTotal();
                            break;
                        }
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        // Đổ dữ liệu vào Stats
        DashboardStats newStats = new DashboardStats();
        newStats.setTotalOrders(tOrders); newStats.setTotalRevenue(tRevenue);
        newStats.setPendingOrders(tPending); newStats.setCompletedOrders(tCompleted);
        newStats.setOrderTrend(calculateTrend(tOrders, yOrders));
        newStats.setRevenueTrend(calculateTrend(tRevenue, yRevenue));
        stats.postValue(newStats);

        // Đổ dữ liệu vào Biểu đồ
        weeklyRevenue.postValue(revenueData);
    }

    private void processTopItems(List<OrderItem> rawItems) {
        Map<String, OrderItem> groupedMap = new HashMap<>();
        for (OrderItem item : rawItems) {
            String name = item.getMenuItemName();
            if (name == null) continue;
            if (groupedMap.containsKey(name)) {
                OrderItem ex = groupedMap.get(name);
                ex.setQuantity(ex.getQuantity() + item.getQuantity());
            } else {
                OrderItem clone = new OrderItem();
                clone.setMenuItemName(name);
                clone.setQuantity(item.getQuantity());
                clone.setMenuItemPrice(item.getMenuItemPrice());
                groupedMap.put(name, clone);
            }
        }
        List<OrderItem> sortedList = new ArrayList<>(groupedMap.values());
        Collections.sort(sortedList, (a, b) -> Integer.compare(b.getQuantity(), a.getQuantity()));
        topItems.postValue(sortedList.size() > 5 ? sortedList.subList(0, 5) : sortedList);
    }

    private double calculateTrend(double current, double previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return ((current - previous) / previous) * 100.0;
    }

    private void checkAllLoaded() { isLoading.postValue(false); }

    @Override protected void onCleared() { super.onCleared(); stopAutoRefresh(); }
}
