package com.utt.foodcouriers_admin.data.repository;

import android.text.TextUtils;
import android.util.Log;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Category;
import com.utt.foodcouriers_admin.data.model.MenuItem;
import com.utt.foodcouriers_admin.data.model.Restaurant;
import com.utt.foodcouriers_admin.data.remote.BaseSupabaseClient;
import com.utt.foodcouriers_admin.data.remote.SupabaseConfig;
import com.utt.foodcouriers_admin.data.repository.base.BaseSupabaseRepository;
import com.utt.foodcouriers_admin.data.repository.base.CrudRepository;
import com.utt.foodcouriers_admin.data.request.MenuUpsertRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MenuRepository extends BaseSupabaseRepository implements CrudRepository<MenuItem, MenuUpsertRequest> {

    private static final String TAG = "MenuRepository";
    private static final String TABLE_MENU = "menu_items";
    private static final String TABLE_CATEGORY = "categories";
    private static final String TABLE_RESTAURANT = "restaurants";
    private static MenuRepository instance;

    private final BaseSupabaseClient apiClient = new BaseSupabaseClient() {}.getClass().getEnclosingClass().getSimpleName() != null 
            ? new com.utt.foodcouriers_admin.data.remote.BaseSupabaseClient() {}
            : null;
    
    private static final okhttp3.OkHttpClient HTTP_CLIENT = new okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    public static synchronized MenuRepository getInstance() {
        if (instance == null) {
            instance = new MenuRepository();
        }
        return instance;
    }

    private MenuRepository() {
    }

    // --- Menu Item CRUD Operations ---

    @Override
    public void getAll(RepositoryCallback<List<MenuItem>> callback) {
        getMenuItems(null, null, callback);
    }

    public void getMenuItems(String restaurantId, String categoryId, RepositoryCallback<List<MenuItem>> callback) {
        StringBuilder query = new StringBuilder("?select=*&order=sort_order.asc");
        if (!TextUtils.isEmpty(restaurantId)) {
            query.append("&restaurant_id=eq.").append(restaurantId);
        }
        if (!TextUtils.isEmpty(categoryId)) {
            query.append("&category_id=eq.").append(categoryId);
        }
        fetchList(TABLE_MENU, query.toString(), MenuItem[].class, callback);
    }

    @Override
    public void getById(String id, RepositoryCallback<MenuItem> callback) {
        if (TextUtils.isEmpty(id)) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Menu item id is required"));
            return;
        }
        fetchSingle(TABLE_MENU, eqIdFilter(id.trim()), MenuItem[].class, callback);
    }

    @Override
    public void create(MenuUpsertRequest request, RepositoryCallback<MenuItem> callback) {
        BaseResponse<Void> validation = validate(request, true);
        if (validation != null) {
            postResponse(callback, BaseResponse.error(validation.getError().getCode(), validation.getMessage()));
            return;
        }
        createItem(TABLE_MENU, request, MenuItem[].class, callback);
    }

    @Override
    public void update(String id, MenuUpsertRequest request, RepositoryCallback<MenuItem> callback) {
        if (TextUtils.isEmpty(id)) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Menu item id is required"));
            return;
        }

        BaseResponse<Void> validation = validate(request, false);
        if (validation != null) {
            postResponse(callback, BaseResponse.error(validation.getError().getCode(), validation.getMessage()));
            return;
        }

        updateItem(TABLE_MENU, eqIdFilter(id.trim()), request, MenuItem[].class, callback);
    }

    @Override
    public void delete(String id, RepositoryCallback<Void> callback) {
        if (TextUtils.isEmpty(id)) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Menu item id is required"));
            return;
        }
        deleteItem(TABLE_MENU, eqIdFilter(id.trim()), callback);
    }

    public void updateAvailability(String id, boolean isAvailable, RepositoryCallback<MenuItem> callback) {
        if (TextUtils.isEmpty(id)) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Menu item id is required"));
            return;
        }
        
        MenuUpsertRequest request = new MenuUpsertRequest();
        request.setIsAvailable(isAvailable);
        updateItem(TABLE_MENU, eqIdFilter(id.trim()), request, MenuItem[].class, callback);
    }

    private BaseResponse<Void> validate(MenuUpsertRequest request, boolean requireAll) {
        if (request == null) {
            return BaseResponse.error("VALIDATION_ERROR", "Menu item payload is required");
        }

        if (requireAll || request.getName() != null) {
            BaseResponse<Void> nameValidation = validateRequired(request.getName(), "Name");
            if (nameValidation != null) return nameValidation;
        }

        if (requireAll || request.getRestaurantId() != null) {
            BaseResponse<Void> restaurantValidation = validateRequired(request.getRestaurantId(), "Restaurant");
            if (restaurantValidation != null) return restaurantValidation;
        }

        if (requireAll || request.getCategoryId() != null) {
            BaseResponse<Void> categoryValidation = validateRequired(request.getCategoryId(), "Category");
            if (categoryValidation != null) return categoryValidation;
        }

        if (requireAll || request.getPrice() != null) {
            BaseResponse<Void> priceValidation = validateNonNegative(request.getPrice(), "Price");
            if (priceValidation != null) return priceValidation;
        }

        return null;
    }

    // --- Category Operations ---

    public void getCategories(RepositoryCallback<List<Category>> callback) {
        fetchList(TABLE_CATEGORY, "?select=*&order=sort_order.asc", Category[].class, callback);
    }

    public void getCategoryById(String id, RepositoryCallback<Category> callback) {
        fetchSingle(TABLE_CATEGORY, eqIdFilter(id), Category[].class, callback);
    }

    // --- Restaurant Operations (Giữ nguyên pattern cũ để tương thích) ---

    public void getRestaurants(BaseSupabaseClient.ApiCallback<Restaurant[]> callback) {
        String url = SupabaseConfig.REST_URL + "/restaurants?select=*&order=name.asc";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .build();

        HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, "Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "[]";
                    if (response.isSuccessful()) {
                        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setLenient().create();
                        Restaurant[] restaurants = gson.fromJson(json, Restaurant[].class);
                        postSuccess(callback, restaurants);
                    } else {
                        postError(callback, "Failed to fetch restaurants: " + response.code());
                    }
                }
            }
        });
    }

    public void getRestaurantsForStaff(String userId, BaseSupabaseClient.ApiCallback<Restaurant[]> callback) {
        String url = SupabaseConfig.REST_URL + "/restaurant_staff?user_id=eq." + userId + "&select=restaurant_id,restaurant:restaurants(*)";
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .build();

        HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, "Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "[]";
                    if (response.isSuccessful()) {
                        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setLenient().create();
                        StaffRestaurantJoin[] joins = gson.fromJson(json, StaffRestaurantJoin[].class);
                        Restaurant[] restaurants = new Restaurant[joins.length];
                        for (int i = 0; i < joins.length; i++) {
                            restaurants[i] = joins[i].restaurant;
                        }
                        postSuccess(callback, restaurants);
                    } else {
                        postError(callback, "Failed to fetch assigned restaurants: " + response.code());
                    }
                }
            }
        });
    }

    private <T> void postError(BaseSupabaseClient.ApiCallback<T> callback, String error) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onError(error));
    }

    private <T> void postSuccess(BaseSupabaseClient.ApiCallback<T> callback, T result) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onSuccess(result));
    }

    private static class StaffRestaurantJoin {
        String restaurantId;
        Restaurant restaurant;
    }
}
