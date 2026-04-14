package com.utt.foodcouriers_admin.data.repository;

import com.utt.foodcouriers_admin.data.model.DailyStat;
import com.utt.foodcouriers_admin.data.remote.BaseSupabaseClient;
import com.utt.foodcouriers_admin.data.remote.ReportClient;
import java.util.List;

/**
 * Repository quản lý logic dữ liệu báo cáo cho UI
 */
public class ReportRepository {
    private static ReportRepository instance;
    private final ReportClient reportClient;

    private ReportRepository() {
        reportClient = ReportClient.getInstance();
    }

    public static synchronized ReportRepository getInstance() {
        if (instance == null) {
            instance = new ReportRepository();
        }
        return instance;
    }

    public void getReportData(String fromDate, String toDate, ReportCallback callback) {
        reportClient.getDailyStats(fromDate, toDate, new BaseSupabaseClient.ApiCallback<List<DailyStat>>() {
            @Override
            public void onSuccess(List<DailyStat> result) {
                if (result != null && !result.isEmpty()) {
                    callback.onSuccess(result);
                } else {
                    callback.onError("Không có dữ liệu trong khoảng thời gian này.");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public interface ReportCallback {
        void onSuccess(List<DailyStat> stats);
        void onError(String error);
    }
}
