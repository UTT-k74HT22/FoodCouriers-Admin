package com.utt.foodcouriers_admin.data.repository;

import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.data.repository.base.BaseSupabaseRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * DashboardRepository - Cập nhật để lấy dữ liệu biểu đồ 7 ngày.
 */
public class DashboardRepository extends BaseSupabaseRepository {
    
    private static DashboardRepository instance;

    public static synchronized DashboardRepository getInstance() {
        if (instance == null) {
            instance = new DashboardRepository();
        }
        return instance;
    }

    /**
     * Lấy đơn hàng trong 7 ngày qua để làm biểu đồ và so sánh
     */
    public void getRecentDaysOrders(RepositoryCallback<List<Order>> callback) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        
        // Lấy ngày của 7 ngày trước
        cal.add(Calendar.DAY_OF_YEAR, -7);
        String startDate = sdf.format(cal.getTime());
        
        // Query: created_at >= startDate, sắp xếp mới nhất
        String query = "?created_at=gte." + startDate + "&order=created_at.desc";
        fetchList("orders", query, Order[].class, callback);
    }

    public void getTodayOrders(RepositoryCallback<List<Order>> callback) {
        fetchList("orders", "?order=created_at.desc&limit=200", Order[].class, callback);
    }

    public void getTopItems(RepositoryCallback<List<com.utt.foodcouriers_admin.data.model.OrderItem>> callback) {
        fetchList("order_items", "?select=*,menu_item:menu_items!inner(id,name),order:orders!inner(status)&order.status=eq.delivered&limit=200",
                com.utt.foodcouriers_admin.data.model.OrderItem[].class, callback);
    }

    public void getRecentOrders(RepositoryCallback<List<Order>> callback) {
        fetchList("orders", "?status=eq.pending&order=created_at.desc&limit=10", Order[].class, callback);
    }
}
