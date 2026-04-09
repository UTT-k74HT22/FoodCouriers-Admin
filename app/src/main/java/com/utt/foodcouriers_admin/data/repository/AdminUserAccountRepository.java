package com.utt.foodcouriers_admin.data.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.User;
import com.utt.foodcouriers_admin.data.remote.AuthClient;
import com.utt.foodcouriers_admin.data.remote.BaseSupabaseClient;
import com.utt.foodcouriers_admin.data.remote.SupabaseConfig;
import com.utt.foodcouriers_admin.data.request.AdminCreateUserAccountRequest;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AdminUserAccountRepository extends BaseSupabaseClient {

    private static final String TAG = "AdminUserAccountRepo";

    private static final MediaType JSON = MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON);
    private static final String CREATE_ACCOUNT_PATH = "/admin-user-accounts";

    private static AdminUserAccountRepository instance;

    public static synchronized AdminUserAccountRepository getInstance() {
        if (instance == null) {
            instance = new AdminUserAccountRepository();
        }
        return instance;
    }

    public void createAccount(AdminCreateUserAccountRequest request, RepositoryCallback<User> callback) {
        if (!SupabaseConfig.isAdminApiConfigured()) {
            Log.d(TAG, "Admin API is not configured: " + SupabaseConfig.adminDebugSummary());
            postRepositoryResponse(callback, BaseResponse.error("CONFIG_ERROR", "Admin API is not configured"));
            return;
        }
        if (request == null) {
            Log.d(TAG, "Create account request is null");
            postRepositoryResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Create account payload is required"));
            return;
        }

        String accessToken = AuthClient.getInstance().getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            Log.d(TAG, "Admin session is not available");
            postRepositoryResponse(callback, BaseResponse.error("AUTH_ERROR", "Admin session is required"));
            return;
        }

        RequestBody requestBody = RequestBody.create(gson.toJson(request), JSON);
        Request httpRequest = new Request.Builder()
                .url(resolveUrl(CREATE_ACCOUNT_PATH))
                .post(requestBody)
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON)
                .build();

        Log.d(TAG, "Create account request: " + httpRequest.toString());
        Log.d(TAG, "Create account request target: " + httpRequest.url());
        Log.d(TAG, "Create account request headers: " + maskHeaders(httpRequest.headers()));

        client.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "Create account request failed: " + e.getMessage());
                postRepositoryResponse(callback, BaseResponse.error("NETWORK_ERROR", e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {

                    Log.d(TAG, "Create account response code: " + response.code());

                    String json = responseBody != null ? responseBody.string() : "";
                    if (!response.isSuccessful()) {
                        Log.d(TAG, "Create account response failed: " + json);
                        postRepositoryResponse(
                                callback,
                                BaseResponse.error("CREATE_ACCOUNT_FAILED", parseCreateAccountError(response.code(), json, httpRequest.url().toString()))
                        );
                        return;
                    }

                    User user = parseUser(json);
                    if (user == null) {
                        Log.d(TAG, "Failed to parse create account response: " + json);
                        postRepositoryResponse(callback, BaseResponse.error("PARSE_ERROR", "Create account response is invalid"));
                        return;
                    }
                    Log.d(TAG, "Create account succeeded: " + user.toString());
                    postRepositoryResponse(callback, BaseResponse.success(user, "User account created successfully"));
                }
            }
        });
    }

    private User parseUser(String json) {
        try {
            CreateUserResponse envelope = gson.fromJson(json, CreateUserResponse.class);
            if (envelope != null && envelope.data != null) {
                Log.d(TAG, "Parsed as CreateUserResponse envelope");
                return envelope.data;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse CreateUserResponse", e);
        }

        try {
            User user = gson.fromJson(json, User.class);
            if (user != null) {
                Log.d(TAG, "Parsed as direct User object");
            }
            return user;
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse User", e);
            return null;
        }
    }

    private String resolveUrl(String path) {
        if (SupabaseConfig.ADMIN_API_BASE_URL.endsWith("/")) {
            return SupabaseConfig.ADMIN_API_BASE_URL.substring(0, SupabaseConfig.ADMIN_API_BASE_URL.length() - 1) + path;
        }
        return SupabaseConfig.ADMIN_API_BASE_URL + path;
    }

    private String parseCreateAccountError(int statusCode, String json, String requestUrl) {
        String parsedMessage = parseRestError("Create user account failed", statusCode, json);
        if (statusCode == 404 && parsedMessage != null && parsedMessage.toLowerCase().contains("function")) {
            return "Admin API endpoint was not found. Expected deployed endpoint at "
                    + requestUrl
                    + ". Check ADMIN_API_BASE_URL and deploy the admin-user-accounts function.";
        }
        return parsedMessage;
    }

    private String maskHeaders(Headers headers) {
        StringBuilder builder = new StringBuilder();
        for (String name : headers.names()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(name).append(": ");
            if (SupabaseConfig.HEADER_AUTH.equalsIgnoreCase(name)
                    || SupabaseConfig.HEADER_AUTHORIZATION.equalsIgnoreCase(name)) {
                builder.append("██");
            } else {
                builder.append(headers.get(name));
            }
        }
        return builder.toString();
    }

    private <T> void postRepositoryResponse(RepositoryCallback<T> callback, BaseResponse<T> response) {
        mainHandler.post(() -> callback.onComplete(response));
    }

    private static class CreateUserResponse {
        User data;
    }

    public void updateAvatar(Context context, String userId, Uri avatarUri, RepositoryCallback<User> callback) {
        if (context == null) {
            postRepositoryResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Context is required"));
            return;
        }
        if (userId == null || userId.isBlank()) {
            postRepositoryResponse(callback, BaseResponse.error("VALIDATION_ERROR", "User ID is required"));
            return;
        }
        if (avatarUri == null) {
            postRepositoryResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Avatar URI is required"));
            return;
        }

        StorageRepository.getInstance().uploadImage(context, avatarUri, "avatars", new RepositoryCallback<String>() {
            @Override
            public void onComplete(BaseResponse<String> response) {
                if (!response.isSuccess()) {
                    String errorCode = response.getError() != null ? response.getError().getCode() : "UPLOAD_ERROR";
                postRepositoryResponse(callback, BaseResponse.error(errorCode, response.getMessage()));
                    return;
                }

                String avatarUrl = response.getData();
                updateUserAvatar(userId, avatarUrl, callback);
            }
        });
    }

    private void updateUserAvatar(String userId, String avatarUrl, RepositoryCallback<User> callback) {
        String accessToken = AuthClient.getInstance().getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            postRepositoryResponse(callback, BaseResponse.error("AUTH_ERROR", "Admin session is required"));
            return;
        }

        String jsonBody = "{\"avatar_url\":\"" + avatarUrl + "\"}";
        RequestBody requestBody = RequestBody.create(jsonBody, JSON);

        String patchUrl = SupabaseConfig.REST_URL + "/user_accounts?id=eq." + userId;

        Request httpRequest = new Request.Builder()
                .url(patchUrl)
                .patch(requestBody)
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON)
                .addHeader("Prefer", "return=representation")
                .build();

        Log.d(TAG, "Update avatar request: " + httpRequest.url());

        client.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "Update avatar request failed: " + e.getMessage());
                postRepositoryResponse(callback, BaseResponse.error("NETWORK_ERROR", e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    Log.d(TAG, "Update avatar response code: " + response.code());

                    String json = responseBody != null ? responseBody.string() : "";
                    if (!response.isSuccessful()) {
                        Log.d(TAG, "Update avatar response failed: " + json);
                        postRepositoryResponse(callback, BaseResponse.error("UPDATE_AVATAR_FAILED", "Failed to update avatar"));
                        return;
                    }

                    User user = parseUser(json);
                    if (user == null) {
                        user = new User();
                        user.setId(userId);
                        user.setAvatarUrl(avatarUrl);
                    }
                    postRepositoryResponse(callback, BaseResponse.success(user, "Avatar updated successfully"));
                }
            }
        });
    }
}
