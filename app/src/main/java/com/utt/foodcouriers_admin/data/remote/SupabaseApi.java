package com.utt.foodcouriers_admin.data.remote;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.utt.foodcouriers_admin.BuildConfig;
import com.utt.foodcouriers_admin.data.model.AdminProfile;
import com.utt.foodcouriers_admin.data.model.AuthTokenResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseApi {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String PROFILE_COLUMNS =
            "account_id,auth_user_id,email,username,role,status,last_login_at,"
                    + "user_id,full_name,avatar_url,phone,job_title,department,note";

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();

    public boolean hasValidConfiguration() {
        return BuildConfig.SUPABASE_URL != null && !BuildConfig.SUPABASE_URL.isBlank()
                && BuildConfig.SUPABASE_PUBLISHABLE_KEY != null
                && !BuildConfig.SUPABASE_PUBLISHABLE_KEY.isBlank();
    }

    public AuthTokenResponse signIn(String email, String password) throws IOException, ApiException {
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("email", email);
        requestJson.addProperty("password", password);

        Request request = new Request.Builder()
                .url(BuildConfig.SUPABASE_URL + "/auth/v1/token?grant_type=password")
                .addHeader("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(gson.toJson(requestJson), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new ApiException(extractErrorMessage(body, "Đăng nhập thất bại."));
            }
            AuthTokenResponse authTokenResponse = gson.fromJson(body, AuthTokenResponse.class);
            if (authTokenResponse == null
                    || authTokenResponse.getAccessToken() == null
                    || authTokenResponse.getUser() == null) {
                throw new ApiException("Không đọc được phiên đăng nhập từ Supabase.");
            }
            return authTokenResponse;
        }
    }

    public AdminProfile fetchAdminProfile(String authUserId, String accessToken) throws IOException, ApiException {
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/admin_account_profiles?select="
                + PROFILE_COLUMNS
                + "&auth_user_id=eq." + authUserId;

        Request request = authorizedRequestBuilder(url, accessToken)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new ApiException(extractErrorMessage(body, "Không lấy được hồ sơ admin."));
            }
            Type type = new TypeToken<List<AdminProfile>>() {
            }.getType();
            List<AdminProfile> profiles = gson.fromJson(body, type);
            if (profiles == null || profiles.isEmpty()) {
                throw new ApiException("Tài khoản đã xác thực nhưng chưa có quyền admin.");
            }
            return profiles.get(0);
        }
    }

    public void updateLastLogin(String authUserId, String accessToken) throws IOException, ApiException {
        JsonObject payload = new JsonObject();
        payload.addProperty("last_login_at", Instant.now().toString());

        Request request = authorizedRequestBuilder(
                BuildConfig.SUPABASE_URL + "/rest/v1/account?auth_user_id=eq." + authUserId,
                accessToken
        )
                .addHeader("Prefer", "return=minimal")
                .method("PATCH", RequestBody.create(gson.toJson(payload), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new ApiException(extractErrorMessage(body, "Không cập nhật được lần đăng nhập gần nhất."));
            }
        }
    }

    private Request.Builder authorizedRequestBuilder(String url, String accessToken) {
        return new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json");
    }

    private String extractErrorMessage(String body, String fallbackMessage) {
        if (body == null || body.isBlank()) {
            return fallbackMessage;
        }
        try {
            JsonObject object = gson.fromJson(body, JsonObject.class);
            if (object == null) {
                return fallbackMessage;
            }
            if (object.has("msg")) {
                return object.get("msg").getAsString();
            }
            if (object.has("error_description")) {
                return object.get("error_description").getAsString();
            }
            if (object.has("message")) {
                return object.get("message").getAsString();
            }
        } catch (Exception ignored) {
            return fallbackMessage;
        }
        return fallbackMessage;
    }

    public static class ApiException extends Exception {
        public ApiException(String message) {
            super(message);
        }
    }
}
