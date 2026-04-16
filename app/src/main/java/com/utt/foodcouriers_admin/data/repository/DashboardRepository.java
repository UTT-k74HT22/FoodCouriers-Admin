package com.utt.foodcouriers_admin.data.repository;

import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.DashboardStats;
import com.utt.foodcouriers_admin.data.repository.base.BaseSupabaseRepository;

/**
 * DashboardRepository - "Người phục vụ" đi lấy dữ liệu cho Dashboard.
 * Kế thừa BaseSupabaseRepository để dùng các hàm fetchSingle/fetchList.
 */
public class DashboardRepository extends BaseSupabaseRepository {
    
    // Tên View (Virtual Table) trong Supabase để tính toán thống kê hàng ngày.
    private static final String VIEW_STATS = "v_daily_stats";
    
    private static DashboardRepository instance;

    // Singleton pattern - Chỉ có duy nhất 1 "người phục vụ" này trong toàn app.
    public static synchronized DashboardRepository getInstance() {
        if (instance == null) {
            instance = new DashboardRepository();
        }
        return instance;
    }

    /**
     * Lấy tất cả đơn hàng của ngày hôm nay để tự tính toán thống kê (Bypass View lỗi)
     */
    public void getTodayOrders(RepositoryCallback<java.util.List<com.utt.foodcouriers_admin.data.model.Order>> callback) {
        // Lấy 100 đơn hàng mới nhất. ViewModel sẽ lo việc lọc đúng ngày hôm nay.
        // Cách này bypass được hoàn toàn các lỗi lệch múi giờ giữa App và Database.
        fetchList("orders", "?order=created_at.desc&limit=100",
                com.utt.foodcouriers_admin.data.model.Order[].class, callback);
    }

    /**
     * Lấy danh sách món ăn bán chạy (Top Items)
     * Chỉ lấy các món hiện đang còn tồn tại trong danh mục món ăn (menu_items)
     */
    public void getTopItems(RepositoryCallback<java.util.List<com.utt.foodcouriers_admin.data.model.OrderItem>> callback) {
        // !inner join với bảng menu_items: Chỉ lấy những món vẫn còn tồn tại trong hệ thống
        // !inner join với bảng orders: Chỉ lấy những món từ đơn hàng đã giao thành công
        fetchList("order_items", "?select=*,menu_item:menu_items!inner(id,name),order:orders!inner(status)&order.status=eq.delivered&order=created_at.desc&limit=200",
                com.utt.foodcouriers_admin.data.model.OrderItem[].class, callback);
    }
    /**
     * Lấy danh sách 10 đơn hàng mới nhất cần xử lý (status='pending')
     */
    public void getRecentOrders(RepositoryCallback<java.util.List<com.utt.foodcouriers_admin.data.model.Order>> callback) {
        // Query: status=eq.pending (đang chờ), order by created_at desc (mới nhất lên đầu), limit 10
        // Cần join thêm thông tin user và restaurant nếu view/table hỗ trợ
        fetchList("orders", "?status=eq.pending&order=created_at.desc&limit=10", 
                com.utt.foodcouriers_admin.data.model.Order[].class, callback);
    }
}
