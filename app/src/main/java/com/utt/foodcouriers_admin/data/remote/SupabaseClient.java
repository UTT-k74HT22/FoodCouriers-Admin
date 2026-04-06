package com.utt.foodcouriers_admin.data.remote;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.utt.foodcouriers_admin.data.model.Category;
import com.utt.foodcouriers_admin.data.model.MenuItem;
import com.utt.foodcouriers_admin.data.model.User;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SupabaseClient {
    
    private static final String TAG = "SupabaseClient";
    private static SupabaseClient instance;
    
    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;
    
    private String accessToken;
    private String refreshToken;
    
    private SupabaseClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        gson = new GsonBuilder().setLenient().create();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static synchronized SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }
    
    public void setSession(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
    
    public void clearSession() {
        this.accessToken = null;
        this.refreshToken = null;
    }
    
    public boolean isAuthenticated() {
        return accessToken != null && !accessToken.isEmpty();
    }
    
    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
    
    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
    
    // ==================== AUTH ====================
    
    public void signIn(String email, String password, ApiCallback<User> callback) {
        if (!SupabaseConfig.isConfigured()) {
            postError(callback, "Supabase is not configured");
            return;
        }

        Log.d(TAG, "signIn config: " + SupabaseConfig.debugSummary());

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        
        RequestBody requestBody = RequestBody.create(
                gson.toJson(body), 
                MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)
        );
        
        Request request = new Request.Builder()
                .url(SupabaseConfig.AUTH_URL + "/token?grant_type=password")
                .post(requestBody)
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON)
                .build();

        Log.d(TAG, "signIn request: " + request.url() + " email=" + email);
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "signIn network failure", e);
                postError(callback, "Network error: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    Log.d(TAG, "signIn response code=" + response.code() + " body=" + json);
                    if (response.isSuccessful()) {
                        AuthResponse authResponse = gson.fromJson(json, AuthResponse.class);
                        
                        if (authResponse != null
                                && authResponse.getAccessToken() != null
                                && authResponse.getUser() != null
                                && authResponse.getUser().getId() != null) {
                            setSession(authResponse.getAccessToken(), authResponse.getRefreshToken());
                            Log.d(TAG, "signIn success: authUserId=" + authResponse.getUser().getId());
                            fetchUserProfile(authResponse.getUser().getId(), callback);
                        } else {
                            Log.e(TAG, "signIn parse failure: access token or user is null");
                            postError(callback, "Invalid response from server");
                        }
                    } else {
                        String errorMessage = parseAuthError(json);
                        postError(callback, errorMessage);
                    }
                }
            }
        });
    }
    
    public void signUp(String email, String password, String name, String phone, ApiCallback<User> callback) {
        if (!SupabaseConfig.isConfigured()) {
            postError(callback, "Supabase is not configured");
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("data", Map.of("full_name", name, "phone", phone != null ? phone : ""));
        
        RequestBody requestBody = RequestBody.create(
                gson.toJson(body), 
                MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)
        );
        
        Request request = new Request.Builder()
                .url(SupabaseConfig.AUTH_URL + "/signup")
                .post(requestBody)
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON)
                .build();

        Log.d(TAG, "signUp request: " + request.url() + " email=" + email);
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "signUp network failure", e);
                postError(callback, "Network error: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    Log.d(TAG, "signUp response code=" + response.code() + " body=" + json);
                    if (response.isSuccessful()) {
                        AuthResponse authResponse = gson.fromJson(json, AuthResponse.class);
                        
                        if (authResponse != null
                                && authResponse.getAccessToken() != null
                                && authResponse.getUser() != null
                                && authResponse.getUser().getId() != null) {
                            setSession(authResponse.getAccessToken(), authResponse.getRefreshToken());
                            createUserProfile(authResponse.getUser().getId(), name, phone, email, callback);
                        } else {
                            Log.e(TAG, "signUp parse failure: access token or user is null");
                            postError(callback, "Invalid response from server");
                        }
                    } else {
                        String errorMessage = parseAuthError(json);
                        postError(callback, errorMessage);
                    }
                }
            }
        });
    }
    
    public void signOut(ApiCallback<Void> callback) {
        Request request = new Request.Builder()
                .url(SupabaseConfig.AUTH_URL + "/logout")
                .post(RequestBody.create("", MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)))
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                clearSession();
                postSuccess(callback, null);
            }
            
            @Override
            public void onResponse(Call call, Response response) {
                clearSession();
                postSuccess(callback, null);
            }
        });
    }
    
    public void getCurrentUser(ApiCallback<User> callback) {
        if (!isAuthenticated()) {
            postError(callback, "Not authenticated");
            return;
        }
        
        Request request = new Request.Builder()
                .url(SupabaseConfig.AUTH_URL + "/user")
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, "Network error: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        String json = responseBody.string();
                        AuthUser authUser = gson.fromJson(json, AuthUser.class);
                        fetchUserProfile(authUser.getId(), callback);
                    } else {
                        postError(callback, "Failed to get user");
                    }
                }
            }
        });
    }
    
    // ==================== USERS ====================
    
    private void fetchUserProfile(String authId, ApiCallback<User> callback) {
        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/users?auth_id=eq." + authId)
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .build();

        Log.d(TAG, "fetchUserProfile request: " + request.url() + " authId=" + authId);
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "fetchUserProfile network failure", e);
                postError(callback, "Network error: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    Log.d(TAG, "fetchUserProfile response code=" + response.code() + " body=" + json);
                    if (response.isSuccessful()) {
                        User[] users = gson.fromJson(json, User[].class);
                        if (users != null && users.length > 0) {
                            Log.d(TAG, "fetchUserProfile success: profileId=" + users[0].getId() + " role=" + users[0].getRole());
                            postSuccess(callback, users[0]);
                        } else {
                            postError(callback, "User profile not found");
                        }
                    } else {
                        postError(callback, parseRestError("Failed to fetch profile", response.code(), json));
                    }
                }
            }
        });
    }
    
    private void createUserProfile(String authId, String name, String phone, String email, ApiCallback<User> callback) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("auth_id", authId);
        profile.put("full_name", name);
        profile.put("phone", phone);
        profile.put("email", email);
        profile.put("role", "admin");
        profile.put("is_active", true);
        
        RequestBody requestBody = RequestBody.create(
                gson.toJson(profile),
                MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)
        );
        
        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/users")
                .post(requestBody)
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .addHeader(SupabaseConfig.HEADER_PREFER, SupabaseConfig.PREF_RETURN_REPRESENTATION)
                .build();

        Log.d(TAG, "createUserProfile request: " + request.url() + " email=" + email);
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "createUserProfile network failure", e);
                postError(callback, "Network error: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    Log.d(TAG, "createUserProfile response code=" + response.code() + " body=" + json);
                    if (response.isSuccessful()) {
                        User[] users = gson.fromJson(json, User[].class);
                        if (users != null && users.length > 0) {
                            postSuccess(callback, users[0]);
                        } else {
                            postError(callback, "Failed to create profile");
                        }
                    } else {
                        postError(callback, parseRestError("Failed to create profile", response.code(), json));
                    }
                }
            }
        });
    }
    
    // ==================== CATEGORIES ====================

    public void getCategories(ApiCallback<Category[]> callback) {
        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/categories?select=*&order=sort_order.asc")
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, "Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
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

    // ==================== MENU ITEMS ====================

    public void getMenuItems(String categoryId, ApiCallback<MenuItem[]> callback) {
        String url = SupabaseConfig.REST_URL + "/menu_items?select=*&order=sort_order.asc";
        if (categoryId != null) {
            url += "&category_id=eq." + categoryId;
        }

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, "Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
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

    // ==================== HELPERS ====================
    
    private String parseAuthError(String json) {
        try {
            AuthError error = gson.fromJson(json, AuthError.class);
            if (error != null) {
                if (error.getMsg() != null && !error.getMsg().isBlank()) {
                    return error.getMsg();
                }
                if (error.getErrorDescription() != null && !error.getErrorDescription().isBlank()) {
                    return error.getErrorDescription();
                }
                if (error.getMessage() != null && !error.getMessage().isBlank()) {
                    return error.getMessage();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse error", e);
        }
        Log.e(TAG, "Authentication failed with raw body: " + json);
        return "Authentication failed";
    }

    private String parseRestError(String fallbackMessage, int statusCode, String json) {
        try {
            AuthError error = gson.fromJson(json, AuthError.class);
            if (error != null) {
                if (error.getMessage() != null && !error.getMessage().isBlank()) {
                    return error.getMessage();
                }
                if (error.getMsg() != null && !error.getMsg().isBlank()) {
                    return error.getMsg();
                }
                if (error.getErrorDescription() != null && !error.getErrorDescription().isBlank()) {
                    return error.getErrorDescription();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse REST error", e);
        }

        Log.e(TAG, "REST request failed with status=" + statusCode + " body=" + json);
        return fallbackMessage + " (" + statusCode + ")";
    }
    
    private <T> void postSuccess(ApiCallback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }
    
    private <T> void postError(ApiCallback<T> callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }
    
    // ==================== INNER CLASSES ====================
    
    private static class AuthResponse {
        @SerializedName("access_token")
        private String accessToken;
        @SerializedName("token_type")
        private String tokenType;
        @SerializedName("expires_in")
        private Long expiresIn;
        @SerializedName("refresh_token")
        private String refreshToken;
        private AuthUser user;
        
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public AuthUser getUser() { return user; }
    }
    
    private static class AuthUser {
        private String id;
        private String email;
        
        public String getId() { return id; }
        public String getEmail() { return email; }
    }
    
    private static class AuthError {
        private String error;
        @SerializedName("error_code")
        private String errorCode;
        @SerializedName("error_description")
        private String errorDescription;
        private String message;
        private String msg;
        
        public String getErrorDescription() { return errorDescription; }
        public String getMessage() { return message; }
        public String getMsg() { return msg; }
    }
}
