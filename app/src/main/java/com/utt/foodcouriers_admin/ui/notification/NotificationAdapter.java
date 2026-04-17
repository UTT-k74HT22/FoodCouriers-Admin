package com.utt.foodcouriers_admin.ui.notification;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.AppNotification;
import com.utt.foodcouriers_admin.ui.order.OrderUiFormatter;

public class NotificationAdapter extends ListAdapter<AppNotification, NotificationAdapter.NotificationViewHolder> {

    public interface NotificationActionListener {
        void onMarkRead(AppNotification notification);
    }

    private NotificationActionListener listener;

    public NotificationAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setListener(NotificationActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    private static final DiffUtil.ItemCallback<AppNotification> DIFF_CALLBACK = new DiffUtil.ItemCallback<AppNotification>() {
        @Override
        public boolean areItemsTheSame(@NonNull AppNotification oldItem, @NonNull AppNotification newItem) {
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull AppNotification oldItem, @NonNull AppNotification newItem) {
            return oldItem.isRead() == newItem.isRead()
                    && equals(oldItem.getTitle(), newItem.getTitle())
                    && equals(oldItem.getBody(), newItem.getBody())
                    && equals(oldItem.getCreatedAt(), newItem.getCreatedAt());
        }

        private boolean equals(String first, String second) {
            return first == null ? second == null : first.equals(second);
        }
    };

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvBody;
        private final TextView tvTime;
        private final View unreadIndicator;
        private final MaterialButton btnMarkRead;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_notification_title);
            tvBody = itemView.findViewById(R.id.tv_notification_body);
            tvTime = itemView.findViewById(R.id.tv_notification_time);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
            btnMarkRead = itemView.findViewById(R.id.btn_mark_read);
        }

        void bind(AppNotification notification) {
            tvTitle.setText(notification.getTitle());
            tvBody.setText(notification.getBody());
            tvTime.setText(OrderUiFormatter.formatTime(notification.getCreatedAt()));
            unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
            btnMarkRead.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
            btnMarkRead.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMarkRead(notification);
                }
            });
        }
    }
}
