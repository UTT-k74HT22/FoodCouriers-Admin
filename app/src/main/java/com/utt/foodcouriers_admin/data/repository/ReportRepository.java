package com.utt.foodcouriers_admin.data.repository;

import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.data.repository.base.BaseSupabaseRepository;
import java.util.List;

/**
 * Repository quản lý logic dữ liệu báo cáo cho UI
 */
public class ReportRepository extends BaseSupabaseRepository {
    private static ReportRepository instance;

    private ReportRepository() {
        super();
    }

    public static synchronized ReportRepository getInstance() {
        if (instance == null) {
            instance = new ReportRepository();
        }
        return instance;
    }

    /**
     * Lấy danh sách đơn hàng trong khoảng thời gian để tự tính toán báo cáo (Bypass View lỗi)
     */
    public void getReportData(String fromDate, String toDate, com.utt.foodcouriers_admin.data.common.RepositoryCallback<List<Order>> callback) {
        // Query: Lấy tất cả cột của orders, kèm theo thông tin restaurant và order_items
        String select = "select=*,restaurant:restaurants(id,name),items:order_items(*)";
        String filter = "&created_at=gte." + fromDate + "T00:00:00Z&created_at=lte." + toDate + "T23:59:59Z&order=created_at.desc";
        String query = "?" + select + filter;
        
        fetchList("orders", query, Order[].class, callback);
    }
}
