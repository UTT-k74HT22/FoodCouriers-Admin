package com.utt.foodcouriers_admin.data.remote;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.utt.foodcouriers_admin.data.model.User;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Client for handling Supabase authentication operations.
 * Extends BaseSupabaseClient to leverage common functionalities.
 */
public class AuthClient extends BaseSupabaseClient {
    
    private static AuthClient instance;
    
    // Private constructor to enforce Singleton pattern
    private AuthClient() {
        super(); // Calls BaseSupabaseClient constructor
    }
    
    /**
     * Returns the singleton instance of AuthClient.
     * @return The singleton instance.
     */
    public static synchronized AuthClient getInstance() {
        if (instance == null) {
            instance = new AuthClient();
        }
        return instance;
    }
    
    // --- Authentication Methods ---
    
    /**
     * Signs in a user with email and password.
     * @param email The user's email.
     * @param password The user's password.
     * @param callback The callback to handle the result.
     */
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
                okhttp3.MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)
        );
        
        Request request = new Request.Builder()
                .url(SupabaseConfig.AUTH_URL + "/token?grant_type=password")
                .post(requestBody)
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON)
                .build();

        Log.d(TAG, "signIn request: " + request.url() + " email=" + email);
        
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "signIn network failure", e);
                postError(callback, "Network error: " + e.getMessage());
            }
            
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
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
                        String errorMessage = parseAuthError(json); // Use base class error parsing
                        postError(callback, errorMessage);
                    }
                }
            }
        });
    }
    
    /**
     * Signs up a new user.
     * @param email The new user's email.
     * @param password The new user's password.
     * @param name The new user's full name.
     * @param phone The new user's phone number.
     * @param callback The callback to handle the result.
     */
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
                okhttp3.MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)
        );
        
        Request request = new Request.Builder()
                .url(SupabaseConfig.AUTH_URL + "/signup")
                .post(requestBody)
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON)
                .build();

        Log.d(TAG, "signUp request: " + request.url() + " email=" + email);
        
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "signUp network failure", e);
                postError(callback, "Network error: " + e.getMessage());
            }
            
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
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
                            // Create user profile after successful auth signup
                            createUserProfile(authResponse.getUser().getId(), name, phone, email, callback);
                        } else {
                            Log.e(TAG, "signUp parse failure: access token or user is null");
                            postError(callback, "Invalid response from server");
                        }
                    } else {
                        String errorMessage = parseAuthError(json); // Use base class error parsing
                        postError(callback, errorMessage);
                    }
                }
            }
        });
    }
    
    /**
     * Signs out the current user. Clears session tokens.
     * @param callback The callback to handle the result.
     */
    public void signOut(ApiCallback<Void> callback) {
        // Supabase auth/v1/logout requires POST with Authorization header
        Request request = new Request.Builder()
                .url(SupabaseConfig.AUTH_URL + "/logout")
                .post(RequestBody.create("", okhttp3.MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)))
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken) // Use Bearer token for logout
                .build();
        
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                // Even if network fails, clear session locally
                clearSession();
                postSuccess(callback, null);
            }
            
            @Override
            public void onResponse(okhttp3.Call call, Response response) {
                clearSession(); // Clear session locally regardless of server response success
                postSuccess(callback, null);
            }
        });
    }
    
    /**
     * Gets the currently authenticated user's details from the Auth API.
     * @param callback The callback to handle the result.
     */
    public void getCurrentUser(ApiCallback<User> callback) {
        if (!isAuthenticated()) {
            postError(callback, "Not authenticated");
            return;
        }
        
        // Fetch user details from Supabase Auth endpoint
        Request request = new Request.Builder()
                .url(SupabaseConfig.AUTH_URL + "/user")
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
                    if (response.isSuccessful()) {
                        String json = responseBody.string();
                        // AuthUser is a simple model for the user object returned by /user endpoint
                        AuthUser authUser = gson.fromJson(json, AuthUser.class);
                        if (authUser != null && authUser.getId() != null) {
                            // Fetch full user profile from the 'users' table
                            fetchUserProfile(authUser.getId(), callback);
                        } else {
                            postError(callback, "Failed to parse user details");
                        }
                    } else {
                        postError(callback, "Failed to get user: " + response.code());
                    }
                }
            }
        });
    }
    
    // --- User Profile Fetching ---
    
    /**
     * Fetches the full user profile from the 'users' table using the Supabase auth_id.
     * This is used after authentication to get the user's role and other profile data.
     * @param authId The auth_id obtained from Supabase Auth.
     * @param callback The callback to handle the result.
     */
    protected void fetchUserProfile(String authId, ApiCallback<User> callback) {
        // Construct URL to query the 'users' table by auth_id
        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/users?auth_id=eq." + authId)
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .build();

        Log.d(TAG, "fetchUserProfile request: " + request.url() + " authId=" + authId);
        
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "fetchUserProfile network failure", e);
                postError(callback, "Network error: " + e.getMessage());
            }
            
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    Log.d(TAG, "fetchUserProfile response code=" + response.code() + " body=" + json);
                    if (response.isSuccessful()) {
                        // Supabase REST API returns an array, even for a single match
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
    
    /**
     * Creates a new user profile entry in the 'users' table.
     * This is called after a successful signup to create the associated profile.
     * @param authId The Supabase Auth ID.
     * @param name User's full name.
     * @param phone User's phone number.
     * @param email User's email.
     * @param callback Callback to handle the result.
     */
    protected void createUserProfile(String authId, String name, String phone, String email, ApiCallback<User> callback) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("auth_id", authId);
        profile.put("full_name", name);
        profile.put("phone", phone);
        profile.put("email", email);
        profile.put("role", "admin"); // Default role for new signups, can be adjusted
        profile.put("is_active", true);
        
        RequestBody requestBody = RequestBody.create(
                gson.toJson(profile),
                okhttp3.MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)
        );
        
        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/users")
                .post(requestBody)
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .addHeader(SupabaseConfig.HEADER_PREFER, SupabaseConfig.PREF_RETURN_REPRESENTATION) // To get the created user object back
                .build();

        Log.d(TAG, "createUserProfile request: " + request.url() + " email=" + email);
        
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "createUserProfile network failure", e);
                postError(callback, "Network error: " + e.getMessage());
            }
            
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
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
    
    // --- Inner Classes for Auth Responses (kept within AuthClient as they are auth-specific) ---
    
    /**
     * Represents the response structure from Supabase Auth token endpoint.
     */
    protected static class AuthResponse {
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
    
    /**
     * Represents the user object returned by Supabase Auth API (e.g., /user endpoint).
     */
    protected static class AuthUser {
        private String id;
        private String email;
        
        public String getId() { return id; }
        public String getEmail() { return email; }
    }
    
    // AuthError class is also part of BaseSupabaseClient as it can be used for parsing various errors.
    // It's defined in BaseSupabaseClient.java for now.
}
