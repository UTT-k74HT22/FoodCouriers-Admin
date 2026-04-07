package com.utt.foodcouriers_admin.data.repository;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;

import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.remote.AuthClient;
import com.utt.foodcouriers_admin.data.remote.SupabaseConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class StorageRepository {

    private static final String TAG = "StorageRepository";
    private static final MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_MIME_TYPES = {"image/jpeg", "image/png", "image/webp", "image/gif"};

    private static StorageRepository instance;
    private final AuthClient sessionClient = AuthClient.getInstance();

    public static synchronized StorageRepository getInstance() {
        if (instance == null) {
            instance = new StorageRepository();
        }
        return instance;
    }

    public void uploadImage(Context context, Uri imageUri, String folder, RepositoryCallback<String> callback) {
        if (imageUri == null) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Image URI is required"));
            return;
        }

        String mimeType = getMimeType(context, imageUri);
        boolean isAllowed = false;
        for (String allowed : ALLOWED_MIME_TYPES) {
            if (allowed.equals(mimeType)) {
                isAllowed = true;
                break;
            }
        }
        if (!isAllowed) {
            postResponse(callback, BaseResponse.error("INVALID_TYPE", "Only JPEG, PNG, WebP, GIF are allowed"));
            return;
        }

        if (TextUtils.isEmpty(folder)) {
            folder = "uploads";
        }

        String fileName = generateFileName(imageUri, context);
        String path = folder + "/" + fileName;
        String uploadUrl = SupabaseConfig.STORAGE_URL + "/object/" + SupabaseConfig.SUPABASE_STORAGE_BUCKET + "/" + path;

        byte[] imageData = readBytesFromUri(context, imageUri);
        if (imageData == null) {
            postResponse(callback, BaseResponse.error("READ_ERROR", "Failed to read image file"));
            return;
        }

        if (imageData.length > MAX_FILE_SIZE) {
            postResponse(callback, BaseResponse.error("SIZE_EXCEEDED", "File size exceeds 5MB limit"));
            return;
        }

        Request.Builder requestBuilder = new Request.Builder()
                .url(uploadUrl)
                .put(RequestBody.create(imageData, OCTET_STREAM))
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", getMimeType(context, imageUri));

        String accessToken = sessionClient.getAccessToken();
        if (!TextUtils.isEmpty(accessToken)) {
            requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
        }

        Request request = requestBuilder.build();

        HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postResponse(callback, BaseResponse.error("NETWORK_ERROR", "Upload failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    if (response.isSuccessful()) {
                        String publicUrl = SupabaseConfig.buildPublicImageUrl(path);
                        postResponse(callback, BaseResponse.success(publicUrl, "Upload successful"));
                    } else {
                        String errorMsg = parseError(json, response.code());
                        postResponse(callback, BaseResponse.error("UPLOAD_FAILED", errorMsg));
                    }
                }
            }
        });
    }

    public void deleteImage(String imagePath, RepositoryCallback<Void> callback) {
        if (TextUtils.isEmpty(imagePath)) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Image path is required"));
            return;
        }

        String deleteUrl = SupabaseConfig.STORAGE_URL + "/object/" + SupabaseConfig.SUPABASE_STORAGE_BUCKET + "/" + imagePath;

        Request.Builder requestBuilder = new Request.Builder()
                .url(deleteUrl)
                .delete()
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY);

        String accessToken = sessionClient.getAccessToken();
        if (!TextUtils.isEmpty(accessToken)) {
            requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
        }

        Request request = requestBuilder.build();

        HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postResponse(callback, BaseResponse.error("NETWORK_ERROR", "Delete failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        postResponse(callback, BaseResponse.success(null, "Delete successful"));
                    } else {
                        String json = responseBody != null ? responseBody.string() : "";
                        String errorMsg = parseError(json, response.code());
                        postResponse(callback, BaseResponse.error("DELETE_FAILED", errorMsg));
                    }
                }
            }
        });
    }

    private String generateFileName(Uri uri, Context context) {
        String originalName = getFileName(uri, context);
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    private String getFileName(Uri uri, Context context) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }

    private String getMimeType(Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        return mimeType != null ? mimeType : "image/jpeg";
    }

    private byte[] readBytesFromUri(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(data)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            inputStream.close();
            return buffer.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read bytes from URI", e);
            return null;
        }
    }

    private String parseError(String json, int statusCode) {
        try {
            if (!TextUtils.isEmpty(json)) {
                StorageError error = new com.google.gson.Gson().fromJson(json, StorageError.class);
                if (error != null && error.getMessage() != null) {
                    return error.getMessage();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse storage error", e);
        }
        return "Upload failed (Status: " + statusCode + ")";
    }

    private <T> void postResponse(RepositoryCallback<T> callback, BaseResponse<T> response) {
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        mainHandler.post(() -> callback.onComplete(response));
    }

    private static class StorageError {
        String message;
        String error;
        String statusCode;

        public String getMessage() { return message; }
        public String getError() { return error; }
        public String getStatusCode() { return statusCode; }
    }
}
