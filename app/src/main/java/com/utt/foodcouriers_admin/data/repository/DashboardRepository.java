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
     * Lấy các đơn hàng gần đây để tính toán thống kê (Đảm bảo Dashboard luôn có dữ liệu)
     */
    public void getRecentOrdersForStats(RepositoryCallback<java.util.List<com.utt.foodcouriers_admin.data.model.Order>> callback) {
        // Lấy 100 đơn hàng gần nhất để tính toán các chỉ số
        fetchList("orders", "?order=created_at.desc&limit=100", 
                com.utt.foodcouriers_admin.data.model.Order[].class, callback);
    }

    /**
     * Lấy danh sách món ăn bán chạy (Top Items)
     */
    public void getTopItems(RepositoryCallback<java.util.List<com.utt.foodcouriers_admin.data.model.OrderItem>> callback) {
        // Vì không sửa DB, ta lấy danh sách order_items gần đây và sẽ tự gom nhóm trong ViewModel
        fetchList("order_items", "?select=*,menu_item:menu_items(name)&limit=50", 
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
