package com.utt.foodcouriers_admin.data.remote;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.utt.foodcouriers_admin.data.model.Restaurant;
import com.utt.foodcouriers_admin.data.request.RestaurantUpsertRequest;

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
                .url(SupabaseConfig.REST_URL + "/restaurants?select=*&order=created_at.desc")
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

    public void getRestaurants(String searchQuery, Boolean isActive, int limit, int offset, ApiCallback<Restaurant[]> callback) {
        StringBuilder urlBuilder = new StringBuilder(SupabaseConfig.REST_URL + "/restaurants?select=*&order=created_at.desc");

        if (searchQuery != null && !searchQuery.isEmpty()) {
            urlBuilder.append("&name=ilike.*").append(searchQuery).append("*");
        }
        if (isActive != null) {
            urlBuilder.append("&is_active=eq.").append(isActive);
        }
        if (limit > 0) {
            urlBuilder.append("&limit=").append(limit);
        }
        if (offset >= 0) {
            urlBuilder.append("&offset=").append(offset);
        }

        Request request = new Request.Builder()
                .url(urlBuilder.toString())
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

    public void createRestaurant(RestaurantUpsertRequest request, ApiCallback<Restaurant[]> callback) {
        String jsonBody = gson.toJson(request);
        okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json"));

        Request httpRequest = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/restaurants?select=*")
                .post(body)
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, "application/json")
                .build();

        client.newCall(httpRequest).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                postError(callback, "Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "[]";
                    if (response.isSuccessful()) {
                        Restaurant[] result = gson.fromJson(json, Restaurant[].class);
                        postSuccess(callback, result);
                    } else {
                        postError(callback, parseRestError("Failed to create restaurant", response.code(), json));
                    }
                }
            }
        });
    }

    public void updateRestaurant(String id, RestaurantUpsertRequest request, ApiCallback<Restaurant[]> callback) {
        String jsonBody = gson.toJson(request);
        okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json"));

        Request httpRequest = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/restaurants?id=eq." + id + "&select=*")
                .patch(body)
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, "application/json")
                .build();

        client.newCall(httpRequest).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                postError(callback, "Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "[]";
                    if (response.isSuccessful()) {
                        Restaurant[] result = gson.fromJson(json, Restaurant[].class);
                        postSuccess(callback, result);
                    } else {
                        postError(callback, parseRestError("Failed to update restaurant", response.code(), json));
                    }
                }
            }
        });
    }

    public void deleteRestaurant(String id, ApiCallback<Void> callback) {
        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/restaurants?id=eq." + id)
                .delete()
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
                    if (response.isSuccessful()) {
                        postSuccess(callback, null);
                    } else {
                        String json = responseBody != null ? responseBody.string() : "";
                        postError(callback, parseRestError("Failed to delete restaurant", response.code(), json));
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
