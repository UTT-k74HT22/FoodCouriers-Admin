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
                 // Lấy thực thể ReportClient đã viết ở bước trước
                 reportClient = ReportClient.getInstance();
             }

                public static synchronized ReportRepository getInstance() {
                 if (instance == null) {
                         instance = new ReportRepository();
                     }
                 return instance;
             }

                /**
          * Lấy dữ liệu báo cáo từ Client và trả về kết quả cho ViewModel qua Callback
          * @param fromDate Ngày bắt đầu
          * @param toDate Ngày kết thúc
          * @param callback Hàm xử lý kết quả (Thành công/Thất bại)
          */
                public void getReportData(String fromDate, String toDate, ReportCallback callback) {
                 reportClient.getDailyStats(fromDate, toDate, new BaseSupabaseClient.ApiCallback<List<DailyStat>>() {
                @Override
                public void onSuccess(List<DailyStat> result) {
                                 // Nếu lấy dữ liệu thành công, trả danh sách DailyStat về cho UI
                                 if (result != null && !result.isEmpty()) {
                                         callback.onSuccess(result);
                                     } else {
                                         callback.onError("Không có dữ liệu trong khoảng thời gian này.");
                                     }
                             }

                        @Override
                public void onError(String error) {
                                 // Nếu có lỗi (mất mạng, lỗi server...), trả thông báo lỗi về cho UI
                                 callback.onError(error);
                             }
            });
             }

                /**
          * Interface để truyền dữ liệu ngược lại cho Fragment/ViewModel
          */
                public interface ReportCallback {
            void onSuccess(List<DailyStat> stats);
            void onError(String error);
        }
    }