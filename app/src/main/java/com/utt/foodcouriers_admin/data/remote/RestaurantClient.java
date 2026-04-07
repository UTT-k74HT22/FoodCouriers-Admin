package com.utt.foodcouriers_admin.data.remote;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.utt.foodcouriers_admin.data.model.Restaurant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Client xử lý các operations liên quan đến nhà hàng trong Supabase.
 * Extends BaseSupabaseClient để tái sử dụng các chức năng chung.
 */
public class RestaurantClient extends BaseSupabaseClient {

    private static RestaurantClient instance;

    private RestaurantClient() {
        super();
    }

    public static synchronized RestaurantClient getInstance() {
        if (instance == null) {
            instance = new RestaurantClient();
        }
        return instance;
    }

    // --- Restaurants ---

    /**
     * Lấy tất cả nhà hàng, sắp xếp theo tên.
     * @param callback Callback xử lý kết quả.
     */
    public void getRestaurants(ApiCallback<Restaurant[]> callback) {
        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/restaurants?select=*&order=name.asc")
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                postError(callback, "Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "[]";
                    if (response.isSuccessful()) {
                        Restaurant[] restaurants = gson.fromJson(json, Restaurant[].class);
                        postSuccess(callback, restaurants);
                    } else {
                        postError(callback, parseRestError("Failed to fetch restaurants", response.code(), json));
                    }
                }
            }
        });
    }

    /**
     * Lấy danh sách nhà hàng được phân công cho nhân viên.
     * Truy vấn bảng 'restaurant_staff' và join với bảng 'restaurants'.
     * @param userId ID của user (nhân viên).
     * @param callback Callback xử lý kết quả.
     */
    public void getRestaurantsForStaff(String userId, ApiCallback<Restaurant[]> callback) {
        String url = SupabaseConfig.REST_URL + "/restaurant_staff?user_id=eq." + userId + "&select=restaurant_id,restaurant:restaurants(*)";
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                postError(callback, "Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "[]";
                    if (response.isSuccessful()) {
                        StaffRestaurantJoin[] joins = gson.fromJson(json, StaffRestaurantJoin[].class);
                        Restaurant[] restaurants = new Restaurant[joins.length];
                        for (int i = 0; i < joins.length; i++) {
                            restaurants[i] = joins[i].restaurant;
                        }
                        postSuccess(callback, restaurants);
                    } else {
                        postError(callback, parseRestError("Failed to fetch assigned restaurants", response.code(), json));
                    }
                }
            }
        });
    }
    
    // --- Inner Class cho Restaurant Staff Join ---
    /**
     * Đại diện cấu trúc trả về khi join restaurants với restaurant_staff.
     * Sử dụng cụ thể cho method getRestaurantsForStaff.
     */
    protected static class StaffRestaurantJoin {
        @SerializedName("restaurant_id")
        String restaurantId;
        Restaurant restaurant;
    }
}
