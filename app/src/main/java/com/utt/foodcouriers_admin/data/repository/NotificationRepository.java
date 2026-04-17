package com.utt.foodcouriers_admin.data.repository;

import com.google.gson.annotations.SerializedName;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.AppNotification;
import com.utt.foodcouriers_admin.data.repository.base.BaseSupabaseRepository;

import java.util.List;

public class NotificationRepository extends BaseSupabaseRepository {

    private static NotificationRepository instance;

    public static synchronized NotificationRepository getInstance() {
        if (instance == null) {
            instance = new NotificationRepository();
        }
        return instance;
    }

    private NotificationRepository() {}

    public void getNotifications(String userId, RepositoryCallback<List<AppNotification>> callback) {
        String query = "?select=*&user_id=eq." + userId + "&order=created_at.desc";
        fetchList("notifications", query, AppNotification[].class, callback);
    }

    public void markAsRead(String id, RepositoryCallback<AppNotification> callback) {
        updateItem(
                "notifications",
                "?id=eq." + id,
                new ReadUpdate(true),
                AppNotification[].class,
                callback
        );
    }

    public void markAllAsRead(String userId, RepositoryCallback<AppNotification> callback) {
        updateItem(
                "notifications",
                "?user_id=eq." + userId + "&is_read=eq.false",
                new ReadUpdate(true),
                AppNotification[].class,
                callback
        );
    }

    public void deleteAll(String userId, RepositoryCallback<Void> callback) {
        deleteItem("notifications", "?user_id=eq." + userId, callback);
    }

    private static class ReadUpdate {
        @SerializedName("is_read")
        final boolean read;

        ReadUpdate(boolean read) {
            this.read = read;
        }
    }
}
