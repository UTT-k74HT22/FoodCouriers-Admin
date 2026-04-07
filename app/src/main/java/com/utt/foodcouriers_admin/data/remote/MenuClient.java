package com.utt.foodcouriers_admin.data.remote;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.utt.foodcouriers_admin.data.model.Category;
import com.utt.foodcouriers_admin.data.model.MenuItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Client for handling Supabase menu and category operations.
 * Extends BaseSupabaseClient to leverage common functionalities.
 */
public class MenuClient extends BaseSupabaseClient {

    private static MenuClient instance;

    private MenuClient() {
        super();
    }

    public static synchronized MenuClient getInstance() {
        if (instance == null) {
            instance = new MenuClient();
        }
        return instance;
    }

    // --- Categories ---

    /**
     * Fetches all categories, ordered by sort_order.
     * @param callback Callback to handle the result.
     */
    public void getCategories(ApiCallback<Category[]> callback) {
        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/categories?select=*&order=sort_order.asc")
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
                        Category[] categories = gson.fromJson(json, Category[].class);
                        postSuccess(callback, categories);
                    } else {
                        postError(callback, parseRestError("Failed to fetch categories", response.code(), json));
                    }
                }
            }
        });
    }

    // --- Menu Items ---

    /**
     * Fetches menu items, optionally filtering by category and/or restaurant.
     * Use null for parameters to ignore filtering.
     * @param restaurantId Optional filter by restaurant ID.
     * @param categoryId Optional filter by category ID.
     * @param callback Callback to handle the result.
     */
    public void getMenuItems(String restaurantId, String categoryId, ApiCallback<MenuItem[]> callback) {
        // Start with base URL and select all fields, ordered by sort_order
        StringBuilder urlBuilder = new StringBuilder(SupabaseConfig.REST_URL + "/menu_items?select=*&order=sort_order.asc");

        // Append filters if provided
        if (restaurantId != null) {
            urlBuilder.append("&restaurant_id=eq.").append(restaurantId);
        }
        if (categoryId != null) {
            urlBuilder.append("&category_id=eq.").append(categoryId);
        }

        Request.Builder requestBuilder = new Request.Builder()
                .url(urlBuilder.toString())
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY);

        if (accessToken != null && !accessToken.isEmpty()) {
            requestBuilder.addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken);
        }

        Request request = requestBuilder.build();

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
                        MenuItem[] items = gson.fromJson(json, MenuItem[].class);
                        postSuccess(callback, items);
                    } else {
                        postError(callback, parseRestError("Failed to fetch menu items", response.code(), json));
                    }
                }
            }
        });
    }
    
    /**
     * Overloaded method to fetch menu items filtered by category only.
     * @param categoryId Category ID to filter by.
     * @param callback Callback to handle the result.
     */
    public void getMenuItems(String categoryId, ApiCallback<MenuItem[]> callback) {
        getMenuItems(null, categoryId, callback);
    }

    /**
     * Creates a new menu item.
     * @param item The MenuItem object to create.
     * @param callback Callback to handle the result.
     */
    public void createMenuItem(MenuItem item, ApiCallback<MenuItem> callback) {
        RequestBody body = RequestBody.create(
                gson.toJson(item),
                okhttp3.MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)
        );

        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/menu_items")
                .post(body)
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .addHeader(SupabaseConfig.HEADER_PREFER, SupabaseConfig.PREF_RETURN_REPRESENTATION) // To get the created object back
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                postError(callback, "Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    if (response.isSuccessful()) {
                        MenuItem[] items = gson.fromJson(json, MenuItem[].class);
                        if (items != null && items.length > 0) {
                            postSuccess(callback, items[0]);
                        } else {
                            postError(callback, "Failed to create menu item");
                        }
                    } else {
                        postError(callback, parseRestError("Failed to create menu item", response.code(), json));
                    }
                }
            }
        });
    }

    /**
     * Updates an existing menu item.
     * @param item The MenuItem object with updated data and ID.
     * @param callback Callback to handle the result.
     */
    public void updateMenuItem(MenuItem item, ApiCallback<MenuItem> callback) {
        RequestBody body = RequestBody.create(
                gson.toJson(item),
                okhttp3.MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)
        );

        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/menu_items?id=eq." + item.getId()) // Filter by item ID
                .patch(body) // Use PATCH for partial updates
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .addHeader(SupabaseConfig.HEADER_PREFER, SupabaseConfig.PREF_RETURN_REPRESENTATION) // To get the updated object back
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                postError(callback, "Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    if (response.isSuccessful()) {
                        MenuItem[] items = gson.fromJson(json, MenuItem[].class);
                        if (items != null && items.length > 0) {
                            postSuccess(callback, items[0]);
                        } else {
                            postError(callback, "Failed to update menu item");
                        }
                    } else {
                        postError(callback, parseRestError("Failed to update menu item", response.code(), json));
                    }
                }
            }
        });
    }

    /**
     * Deletes a menu item by its ID.
     * @param id The ID of the menu item to delete.
     * @param callback Callback to handle the result.
     */
    public void deleteMenuItem(String id, ApiCallback<Void> callback) {
        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/menu_items?id=eq." + id)
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
                if (response.isSuccessful()) {
                    postSuccess(callback, null);
                } else {
                    try (ResponseBody responseBody = response.body()) {
                        String json = responseBody != null ? responseBody.string() : "";
                        postError(callback, parseRestError("Failed to delete menu item", response.code(), json));
                    }
                }
            }
        });
    }
}
