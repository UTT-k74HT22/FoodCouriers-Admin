package com.utt.foodcouriers_admin.ui.report;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.data.model.OrderItem;
import com.utt.foodcouriers_admin.data.repository.ReportRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportViewModel extends ViewModel {

    private final ReportRepository repository;
    private final MutableLiveData<List<Order>> orders = new MutableLiveData<>();
    private final MutableLiveData<List<TopRestaurantAdapter.RestaurantStats>> topRestaurants = new MutableLiveData<>();
    private final MutableLiveData<List<OrderItem>> topItems = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ReportViewModel() {
        this.repository = ReportRepository.getInstance();
    }

    public LiveData<List<Order>> getOrders() { return orders; }
    public LiveData<List<TopRestaurantAdapter.RestaurantStats>> getTopRestaurants() { return topRestaurants; }
    public LiveData<List<OrderItem>> getTopItems() { return topItems; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadReport(String fromDate, String toDate) {
        isLoading.setValue(true);
        
        repository.getReportData(fromDate, toDate, new RepositoryCallback<List<Order>>() {
            @Override
            public void onComplete(BaseResponse<List<Order>> response) {
                isLoading.postValue(false);
                if (response.isSuccess() && response.getData() != null) {
                    List<Order> orderList = response.getData();
                    orders.postValue(orderList);
                    calculateTopRestaurants(orderList);
                    // Giả lập top món ăn từ các đơn hàng này
                    calculateTopItems(orderList);
                } else {
                    errorMessage.postValue(response.getMessage());
                }
            }
        });
    }

    private void calculateTopRestaurants(List<Order> orderList) {
        Map<String, TopRestaurantAdapter.RestaurantStats> map = new HashMap<>();
        for (Order o : orderList) {
            if (!"delivered".equals(o.getStatus())) continue;
            
            String name = o.getRestaurant() != null ? o.getRestaurant().getName() : "Nhà hàng ẩn";
            TopRestaurantAdapter.RestaurantStats stats = map.getOrDefault(name, new TopRestaurantAdapter.RestaurantStats(name, 0, 0));
            stats.orderCount++;
            stats.revenue += o.getTotal();
            map.put(name, stats);
        }
        List<TopRestaurantAdapter.RestaurantStats> sortedList = new ArrayList<>(map.values());
        Collections.sort(sortedList, (a, b) -> Double.compare(b.revenue, a.revenue));
        topRestaurants.postValue(sortedList.size() > 5 ? sortedList.subList(0, 5) : sortedList);
    }

    private void calculateTopItems(List<Order> orderList) {
        Map<String, OrderItem> map = new HashMap<>();
        for (Order o : orderList) {
            if (!"delivered".equals(o.getStatus()) || o.getItems() == null) continue;
            
            String restaurantName = o.getRestaurant() != null ? o.getRestaurant().getName() : "Unknown";
            
            for (OrderItem item : o.getItems()) {
                String itemName = item.getMenuItemName();
                if (itemName == null) continue;
                
                // Khóa gộp món: Tên món + Tên nhà hàng (để phân biệt cơm tấm quán A và quán B)
                String key = itemName + " (" + restaurantName + ")";
                
                OrderItem existing = map.get(key);
                if (existing != null) {
                    existing.setQuantity(existing.getQuantity() + item.getQuantity());
                    existing.setSubtotal(existing.getSubtotal() + item.getSubtotal());
                } else {
                    OrderItem copy = new OrderItem();
                    copy.setMenuItemName(key); // Hiển thị kèm tên nhà hàng cho rõ ràng
                    copy.setQuantity(item.getQuantity());
                    copy.setSubtotal(item.getSubtotal());
                    map.put(key, copy);
                }
            }
        }
        List<OrderItem> sortedList = new ArrayList<>(map.values());
        // Sắp xếp theo số lượng bán giảm dần
        Collections.sort(sortedList, (a, b) -> b.getQuantity() - a.getQuantity());
        topItems.postValue(sortedList.size() > 10 ? sortedList.subList(0, 10) : sortedList);
    }
}
