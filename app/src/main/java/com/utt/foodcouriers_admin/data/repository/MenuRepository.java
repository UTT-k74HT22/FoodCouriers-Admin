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
import com.utt.foodcouriers_admin.utils.SessionManager;
import com.utt.foodcouriers_admin.data.repository.base.BaseSupabaseRepository;
import com.utt.foodcouriers_admin.data.repository.base.CrudRepository;
import com.utt.foodcouriers_admin.data.request.MenuUpsertRequest;
import java.util.ArrayList;
import java.util.List;

public class MenuRepository extends BaseSupabaseRepository implements CrudRepository<MenuItem, MenuUpsertRequest> {

    private static final String TAG = "MenuRepository";
    private static final String TABLE_MENU = "menu_items";
    private static final String TABLE_CATEGORY = "categories";
    private static final String TABLE_RESTAURANT = "restaurants";
    private static MenuRepository instance;

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

    // --- Restaurant Operations (Fixed 401 Error) ---

    /**
     * Lấy danh sách tất cả nhà hàng (Dành cho Admin)
     */
    public void getRestaurants(BaseSupabaseClient.ApiCallback<Restaurant[]> callback) {
        fetchList(TABLE_RESTAURANT, "?select=*&order=name.asc", Restaurant[].class, new RepositoryCallback<List<Restaurant>>() {
            @Override
            public void onComplete(BaseResponse<List<Restaurant>> response) {
                if (response.isSuccess() && response.getData() != null) {
                    callback.onSuccess(response.getData().toArray(new Restaurant[0]));
                } else {
                    callback.onError(response.getMessage());
                }
            }
        });
    }

    /**
     * Lấy danh sách nhà hàng được gán cho Staff
     */
    public void getRestaurantsForStaff(String userId, BaseSupabaseClient.ApiCallback<Restaurant[]> callback) {
        // Query through the join table restaurant_staff
        String query = "?user_id=eq." + userId + "&select=restaurant:restaurants(*)";
        
        fetchList("restaurant_staff", query, StaffRestaurantJoin[].class, new RepositoryCallback<List<StaffRestaurantJoin>>() {
            @Override
            public void onComplete(BaseResponse<List<StaffRestaurantJoin>> response) {
                if (response.isSuccess() && response.getData() != null) {
                    List<StaffRestaurantJoin> joins = response.getData();
                    List<Restaurant> restaurants = new ArrayList<>();
                    for (StaffRestaurantJoin join : joins) {
                        if (join.restaurant != null) {
                            restaurants.add(join.restaurant);
                        }
                    }
                    callback.onSuccess(restaurants.toArray(new Restaurant[0]));
                } else {
                    callback.onError(response.getMessage());
                }
            }
        });
    }

    /**
     * Helper class for parsing the nested restaurant object in the join table
     */
    private static class StaffRestaurantJoin {
        Restaurant restaurant;
    }
}
