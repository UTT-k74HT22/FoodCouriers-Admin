package com.utt.foodcouriers_admin.ui.notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.AppNotification;
import com.utt.foodcouriers_admin.data.repository.NotificationRepository;
import com.utt.foodcouriers_admin.utils.SessionManager;
import com.utt.foodcouriers_admin.utils.ToastBanner;

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment implements NotificationAdapter.NotificationActionListener {

    private static final int FILTER_ALL = 0;
    private static final int FILTER_UNREAD = 1;
    private static final int FILTER_READ = 2;

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private View emptyState;
    private View progressBar;
    private MaterialButton btnMarkAllRead;
    private MaterialButton btnDeleteAll;
    private TextView tvEmptyTitle;
    private TextView tvEmptyMessage;

    private NotificationAdapter adapter;
    private NotificationRepository repository;
    private SessionManager sessionManager;
    private final List<AppNotification> notifications = new ArrayList<>();
    private int currentFilter = FILTER_ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = NotificationRepository.getInstance();
        sessionManager = SessionManager.getInstance(requireContext());

        tabLayout = view.findViewById(R.id.tab_notification_type);
        recyclerView = view.findViewById(R.id.rv_notifications);
        emptyState = view.findViewById(R.id.empty_state);
        progressBar = view.findViewById(R.id.progress_bar);
        btnMarkAllRead = view.findViewById(R.id.btn_mark_all_read);
        btnDeleteAll = view.findViewById(R.id.btn_delete_all);
        tvEmptyTitle = emptyState.findViewById(R.id.tvEmptyTitle);
        tvEmptyMessage = emptyState.findViewById(R.id.tvEmptyMessage);

        setupTabs();
        setupRecycler();
        setupActions();
        loadNotifications();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (repository != null) {
            loadNotifications();
        }
    }

    private void setupTabs() {
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Tất cả").setTag(FILTER_ALL));
        tabLayout.addTab(tabLayout.newTab().setText(" Chưa đọc").setTag(FILTER_UNREAD));
        tabLayout.addTab(tabLayout.newTab().setText(" Đã đọc").setTag(FILTER_READ));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Object tag = tab.getTag();
                currentFilter = tag instanceof Integer ? (Integer) tag : FILTER_ALL;
                renderNotifications();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) { loadNotifications(); }
        });
    }

    private void setupRecycler() {
        adapter = new NotificationAdapter();
        adapter.setListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupActions() {
        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
        btnDeleteAll.setOnClickListener(v -> confirmDeleteAll());
    }

    private void loadNotifications() {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            renderError("Khong tim thay thong tin nguoi dung");
            return;
        }

        setLoading(true);
        repository.getNotifications(userId, new RepositoryCallback<List<AppNotification>>() {
            @Override
            public void onComplete(BaseResponse<List<AppNotification>> response) {
                if (!isAdded()) return;
                setLoading(false);
                if (response.isSuccess() && response.getData() != null) {
                    notifications.clear();
                    notifications.addAll(response.getData());
                    renderNotifications();
                } else {
                    renderError(response.getMessage());
                }
            }
        });
    }

    private void renderNotifications() {
        List<AppNotification> filtered = new ArrayList<>();
        for (AppNotification notification : notifications) {
            if (currentFilter == FILTER_UNREAD && notification.isRead()) {
                continue;
            }
            if (currentFilter == FILTER_READ && !notification.isRead()) {
                continue;
            }
            filtered.add(notification);
        }

        adapter.submitList(filtered);
        boolean isEmpty = filtered.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvEmptyTitle.setText("Không có thông báo ");
        tvEmptyMessage.setText(" Thông báo mới sẽ xuất hiện ở đâu ");

        btnDeleteAll.setEnabled(!notifications.isEmpty());
        btnMarkAllRead.setEnabled(hasUnreadNotifications());
    }

    private void renderError(String message) {
        notifications.clear();
        adapter.submitList(new ArrayList<>());
        emptyState.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmptyTitle.setText("Không tải được thông báo ");
        tvEmptyMessage.setText(message != null ? message : "Vui lòng thử lại");
        btnDeleteAll.setEnabled(false);
        btnMarkAllRead.setEnabled(false);
    }

    private boolean hasUnreadNotifications() {
        for (AppNotification notification : notifications) {
            if (!notification.isRead()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onMarkRead(AppNotification notification) {
        if (notification == null || notification.getId() == null) {
            return;
        }

        repository.markAsRead(notification.getId(), new RepositoryCallback<AppNotification>() {
            @Override
            public void onComplete(BaseResponse<AppNotification> response) {
                if (!isAdded()) return;
                if (response.isSuccess()) {
                    ToastBanner.showSuccess("Đánh dấu đã đọc");
                    loadNotifications();
                } else {
                    ToastBanner.showError(response.getMessage());
                }
            }
        });
    }

    private void markAllAsRead() {
        if (!hasUnreadNotifications()) {
            ToastBanner.showInfo("Không có thông báo chưa đọc nào để đánh dấu");
            return;
        }

        repository.markAllAsRead(sessionManager.getUserId(), new RepositoryCallback<AppNotification>() {
            @Override
            public void onComplete(BaseResponse<AppNotification> response) {
                if (!isAdded()) return;
                if (response.isSuccess()) {
                    ToastBanner.showSuccess("Đã đánh dấu tất cả đã đọc");
                    loadNotifications();
                } else {
                    ToastBanner.showError(response.getMessage());
                }
            }
        });
    }

    private void confirmDeleteAll() {
        if (notifications.isEmpty()) {
            ToastBanner.showInfo("Không có thông báo nào để xóa");
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("xóa tất cả thông báo")
                .setMessage("Tất cả thông báo sẽ bị xóa khỏi hệ thống. Bạn có muốn xóa không?.")
                .setNegativeButton("Hủy ", null)
                .setPositiveButton("Xóa ", (dialog, which) -> deleteAll())
                .show();
    }

    private void deleteAll() {
        repository.deleteAll(sessionManager.getUserId(), new RepositoryCallback<Void>() {
            @Override
            public void onComplete(BaseResponse<Void> response) {
                if (!isAdded()) return;
                if (response.isSuccess()) {
                    ToastBanner.showSuccess("Đã xóa tất cả thông báo");
                    loadNotifications();
                } else {
                    ToastBanner.showError(response.getMessage());
                }
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnMarkAllRead.setEnabled(!loading && hasUnreadNotifications());
        btnDeleteAll.setEnabled(!loading && !notifications.isEmpty());
    }
}
