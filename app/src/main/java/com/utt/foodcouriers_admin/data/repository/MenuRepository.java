package com.utt.foodcouriers_admin.data.repository;

import com.utt.foodcouriers_admin.data.model.Category;
import com.utt.foodcouriers_admin.data.model.MenuItem;
import com.utt.foodcouriers_admin.data.model.Restaurant;
import com.utt.foodcouriers_admin.data.remote.BaseSupabaseClient;
import com.utt.foodcouriers_admin.data.remote.MenuClient;
import com.utt.foodcouriers_admin.data.remote.RestaurantClient;

public class MenuRepository {

    private static MenuRepository instance;
    private final MenuClient menuClient; // Use MenuClient
    private final RestaurantClient restaurantClient; // Use RestaurantClient

    private MenuRepository() {
        menuClient = MenuClient.getInstance(); // Get instance of MenuClient
        restaurantClient = RestaurantClient.getInstance(); // Get instance of RestaurantClient
    }

    public static synchronized MenuRepository getInstance() {
        if (instance == null) {
            instance = new MenuRepository();
        }
        return instance;
    }

    // --- Menu Item Operations ---

    public void getMenuItems(String restaurantId, String categoryId, BaseSupabaseClient.ApiCallback<MenuItem[]> callback) {
        // Delegate call to MenuClient
        menuClient.getMenuItems(restaurantId, categoryId, callback);
    }

    public void createMenuItem(MenuItem item, BaseSupabaseClient.ApiCallback<MenuItem> callback) {
        // Delegate call to MenuClient
        menuClient.createMenuItem(item, callback);
    }

    public void updateMenuItem(MenuItem item, BaseSupabaseClient.ApiCallback<MenuItem> callback) {
        // Delegate call to MenuClient
        menuClient.updateMenuItem(item, callback);
    }

    public void deleteMenuItem(String id, BaseSupabaseClient.ApiCallback<Void> callback) {
        // Delegate call to MenuClient
        menuClient.deleteMenuItem(id, callback);
    }

    // --- Category Operations ---

    public void getCategories(BaseSupabaseClient.ApiCallback<Category[]> callback) {
        // Delegate call to MenuClient
        menuClient.getCategories(callback);
    }

    // --- Restaurant Operations ---

    public void getRestaurants(BaseSupabaseClient.ApiCallback<Restaurant[]> callback) {
        // Delegate call to RestaurantClient
        restaurantClient.getRestaurants(callback);
    }

    public void getRestaurantsForStaff(String userId, BaseSupabaseClient.ApiCallback<Restaurant[]> callback) {
        // Delegate call to RestaurantClient
        restaurantClient.getRestaurantsForStaff(userId, callback);
    }
}
