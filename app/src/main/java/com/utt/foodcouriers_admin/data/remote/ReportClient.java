package com.utt.foodcouriers_admin.data.remote;

    import android.util.Log;
     import com.utt.foodcouriers_admin.data.model.DailyStat;
     import java.io.IOException;
     import java.util.Arrays;
     import java.util.List;
     import okhttp3.Call;
     import okhttp3.Callback;
    import okhttp3.Request;
    import okhttp3.Response;
    import okhttp3.ResponseBody;

            public class ReportClient extends BaseSupabaseClient {
        private static final String TAG = "ReportClient";
        private static ReportClient instance;

                private ReportClient() {
                 super();
             }

                public static synchronized ReportClient getInstance() {
                 if (instance == null) instance = new ReportClient();
                 return instance;
             }

                public void getDailyStats(String fromDate, String toDate, ApiCallback<List<DailyStat>> callback) {
                 // Tạo URL sử dụng REST_URL từ SupabaseConfig của bạn
                 String url = SupabaseConfig.REST_URL + "/v_daily_stats" +
                                 "?date=gte." + fromDate +
                                 "&date=lte." + toDate +
                                 "&order=date.asc";

                 Log.d(TAG, "Calling: " + url);

                 Request request = new Request.Builder()
                         .url(url)
                         .get()
                         // Sử dụng đúng các hằng số trong SupabaseConfig.java của bạn
                         .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                         .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + AuthClient.getInstance().getAccessToken())
                         .build();

                 client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                                 postError(callback, "Lỗi kết nối: " + e.getMessage());
                             }

                        @Override
                public void onResponse(Call call, Response response) throws IOException {
                                 try (ResponseBody responseBody = response.body()) {
                                         String json = responseBody != null ? responseBody.string() : "";

                                         if (response.isSuccessful()) {
                                                 // Chuyển JSON thành mảng DailyStat
                                                 DailyStat[] statsArray = gson.fromJson(json, DailyStat[].class);
                                                 if (statsArray != null) {
                                                         // Trả kết quả về cho UI qua Main Thread
                                                         postSuccess(callback, Arrays.asList(statsArray));
                                                     } else {
                                                         postError(callback, "Không có dữ liệu");
                                                     }
                                             } else {
                                                 // Sử dụng hàm parseRestError có sẵn trong BaseSupabaseClient của bạn
                                                 String errorMessage = parseRestError("Lỗi lấy báo cáo", response.code(), json);
                                                 postError(callback, errorMessage);
                                             }
                                     } catch (Exception e) {
                                         postError(callback, "Lỗi xử lý dữ liệu: " + e.getMessage());
                                     }
                             }
            });
             }
    }