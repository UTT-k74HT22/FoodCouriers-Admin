package com.utt.foodcouriers_admin.ui.user.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.User;

public class UserAdapter extends ListAdapter<User, UserAdapter.UserViewHolder> {

    public interface UserActionListener {
        void onUserClick(User user);
        void onStatusChange(User user, boolean isActive);
    }

    private UserActionListener listener;

    public UserAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setListener(UserActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    private static final DiffUtil.ItemCallback<User> DIFF_CALLBACK = new DiffUtil.ItemCallback<User>() {
        @Override
        public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return !TextUtils.isEmpty(oldItem.getId())
                    && !TextUtils.isEmpty(newItem.getId())
                    && TextUtils.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return TextUtils.equals(oldItem.getFullName(), newItem.getFullName())
                    && TextUtils.equals(oldItem.getEmail(), newItem.getEmail())
                    && TextUtils.equals(oldItem.getPhone(), newItem.getPhone())
                    && TextUtils.equals(oldItem.getAvatarUrl(), newItem.getAvatarUrl())
                    && TextUtils.equals(oldItem.getRole(), newItem.getRole())
                    && oldItem.isActive() == newItem.isActive();
        }
    };

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final android.widget.ImageView ivAvatar;
        private final TextView tvName;
        private final TextView tvEmail;
        private final TextView tvPhone;
        private final TextView tvStatus;
        private final MaterialSwitch switchActive;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_user_avatar);
            tvName = itemView.findViewById(R.id.tv_user_name);
            tvEmail = itemView.findViewById(R.id.tv_user_email);
            tvPhone = itemView.findViewById(R.id.tv_user_phone);
            tvStatus = itemView.findViewById(R.id.tv_user_status);
            switchActive = itemView.findViewById(R.id.sw_is_active);
        }

        void bind(User user) {
            tvName.setText(TextUtils.isEmpty(user.getFullName()) ? "--" : user.getFullName());
            tvEmail.setText(TextUtils.isEmpty(user.getEmail()) ? "--" : user.getEmail());
            tvPhone.setText(TextUtils.isEmpty(user.getPhone()) ? "--" : user.getPhone());
            tvStatus.setText(user.isActive() ? itemView.getContext().getString(R.string.user_status_active) : itemView.getContext().getString(R.string.user_status_inactive));
            tvStatus.setBackgroundResource(user.isActive() ? R.drawable.admin_badge_success : R.drawable.admin_badge_pending);

            switchActive.setOnCheckedChangeListener(null);
            switchActive.setChecked(user.isActive());
            switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onStatusChange(user, isChecked);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });

            if (TextUtils.isEmpty(user.getAvatarUrl())) {
                ivAvatar.setImageResource(R.drawable.ic_users);
            } else {
                Glide.with(ivAvatar.getContext())
                        .load(user.getAvatarUrl())
                        .placeholder(R.drawable.ic_users)
                        .error(R.drawable.ic_users)
                        .centerCrop()
                        .into(ivAvatar);
            }
        }
    }
}
