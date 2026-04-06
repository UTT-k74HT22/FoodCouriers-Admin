package com.utt.foodcouriers_admin.data.repository.base;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.remote.SupabaseClient;
import com.utt.foodcouriers_admin.data.remote.SupabaseConfig;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public abstract class BaseSupabaseRepository {

    private static final String TAG = "BaseSupabaseRepo";
    private static final MediaType JSON = MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON);
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    protected final Gson gson = new GsonBuilder().setLenient().create();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SupabaseClient sessionClient = SupabaseClient.getInstance();

    protected <T> void fetchList(String table, String query, Class<T[]> clazz, RepositoryCallback<List<T>> callback) {
        executeArrayRequest(buildGetRequest(table, query), clazz, callback, "Fetch " + table + " failed");
    }

    protected <T> void fetchSingle(String table, String filterQuery, Class<T[]> clazz, RepositoryCallback<T> callback) {
        executeArrayRequest(buildGetRequest(table, filterQuery), clazz, new RepositoryCallback<List<T>>() {
            @Override
            public void onComplete(BaseResponse<List<T>> response) {
                if (!response.isSuccess()) {
                    postResponse(callback, BaseResponse.error(getErrorCode(response), response.getMessage()));
                    return;
                }

                List<T> items = response.getData();
                if (items == null || items.isEmpty()) {
                    postResponse(callback, BaseResponse.error("NOT_FOUND", "Item not found"));
                    return;
                }

                postResponse(callback, BaseResponse.success(items.get(0), "Success"));
            }
        }, "Fetch " + table + " failed");
    }

    protected <T> void createItem(String table, Object payload, Class<T[]> clazz, RepositoryCallback<T> callback) {
        Request request = withDefaultHeaders(new Request.Builder())
                .url(buildTableUrl(table))
                .post(RequestBody.create(gson.toJson(payload), JSON))
                .addHeader(SupabaseConfig.HEADER_PREFER, SupabaseConfig.PREF_RETURN_REPRESENTATION)
                .build();
        executeMutation(request, clazz, callback, "Create " + table + " failed");
    }

    protected <T> void updateItem(String table, String filterQuery, Object payload, Class<T[]> clazz, RepositoryCallback<T> callback) {
        Request request = withDefaultHeaders(new Request.Builder())
                .url(buildTableUrl(table) + filterQuery)
                .patch(RequestBody.create(gson.toJson(payload), JSON))
                .addHeader(SupabaseConfig.HEADER_PREFER, SupabaseConfig.PREF_RETURN_REPRESENTATION)
                .build();
        executeMutation(request, clazz, callback, "Update " + table + " failed");
    }

    protected void deleteItem(String table, String filterQuery, RepositoryCallback<Void> callback) {
        Request request = withDefaultHeaders(new Request.Builder())
                .url(buildTableUrl(table) + filterQuery)
                .delete()
                .build();

        HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postResponse(callback, BaseResponse.error("NETWORK_ERROR", e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    if (response.isSuccessful()) {
                        postResponse(callback, BaseResponse.success(null, "Deleted successfully"));
                    } else {
                        postResponse(callback, BaseResponse.error("DELETE_FAILED", parseRestError("Delete failed", response.code(), json)));
                    }
                }
            }
        });
    }

    protected BaseResponse<Void> validateRequired(String value, String fieldName) {
        if (TextUtils.isEmpty(value) || TextUtils.isEmpty(value.trim())) {
            return BaseResponse.error("VALIDATION_ERROR", fieldName + " is required");
        }
        return null;
    }

    protected BaseResponse<Void> validateNonNegative(Integer value, String fieldName) {
        if (value != null && value < 0) {
            return BaseResponse.error("VALIDATION_ERROR", fieldName + " must be greater than or equal to 0");
        }
        return null;
    }

    protected String eqIdFilter(String id) {
        return "?id=eq." + id;
    }

    protected String buildTableUrl(String table) {
        return SupabaseConfig.REST_URL + "/" + table;
    }

    private Request buildGetRequest(String table, String query) {
        return withDefaultHeaders(new Request.Builder())
                .url(buildTableUrl(table) + query)
                .get()
                .build();
    }

    private Request.Builder withDefaultHeaders(Request.Builder builder) {
        builder.addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON);

        String accessToken = sessionClient.getAccessToken();
        if (!TextUtils.isEmpty(accessToken)) {
            builder.addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken);
        }
        return builder;
    }

    private <T> void executeArrayRequest(
            Request request,
            Class<T[]> clazz,
            RepositoryCallback<List<T>> callback,
            String fallbackMessage
    ) {
        HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postResponse(callback, BaseResponse.error("NETWORK_ERROR", e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "[]";
                    if (response.isSuccessful()) {
                        T[] items = gson.fromJson(json, clazz);
                        postResponse(callback, BaseResponse.success(Arrays.asList(items), "Success"));
                    } else {
                        postResponse(callback, BaseResponse.error("FETCH_FAILED", parseRestError(fallbackMessage, response.code(), json)));
                    }
                }
            }
        });
    }

    private <T> void executeMutation(
            Request request,
            Class<T[]> clazz,
            RepositoryCallback<T> callback,
            String fallbackMessage
    ) {
        HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postResponse(callback, BaseResponse.error("NETWORK_ERROR", e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "[]";
                    if (response.isSuccessful()) {
                        T[] items = gson.fromJson(json, clazz);
                        if (items != null && items.length > 0) {
                            postResponse(callback, BaseResponse.success(items[0], "Success"));
                        } else {
                            postResponse(callback, BaseResponse.error("EMPTY_RESPONSE", "Server returned empty data"));
                        }
                    } else {
                        postResponse(callback, BaseResponse.error("MUTATION_FAILED", parseRestError(fallbackMessage, response.code(), json)));
                    }
                }
            }
        });
    }

    private String parseRestError(String fallbackMessage, int statusCode, String json) {
        try {
            RestError error = gson.fromJson(json, RestError.class);
            if (error != null) {
                if (!TextUtils.isEmpty(error.message)) {
                    return error.message;
                }
                if (!TextUtils.isEmpty(error.msg)) {
                    return error.msg;
                }
                if (!TextUtils.isEmpty(error.errorDescription)) {
                    return error.errorDescription;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse rest error", e);
        }
        return fallbackMessage + " (" + statusCode + ")";
    }

    private String getErrorCode(BaseResponse<?> response) {
        if (response.getError() == null || TextUtils.isEmpty(response.getError().getCode())) {
            return "UNKNOWN_ERROR";
        }
        return response.getError().getCode();
    }

    protected <T> void postResponse(RepositoryCallback<T> callback, BaseResponse<T> response) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onComplete(response);
            }
        });
    }

    private static class RestError {
        String message;
        String msg;
        @SerializedName("error_description")
        String errorDescription;
    }
}
