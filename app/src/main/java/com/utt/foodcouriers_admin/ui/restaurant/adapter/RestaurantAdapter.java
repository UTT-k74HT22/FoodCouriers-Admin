package com.utt.foodcouriers_admin.ui.restaurant.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.Restaurant;

public class RestaurantAdapter extends ListAdapter<Restaurant, RestaurantAdapter.RestaurantViewHolder> {

    public interface RestaurantActionListener {
        void onEdit(Restaurant restaurant);
        void onStatusChange(Restaurant restaurant, boolean isActive);
        void onDelete(Restaurant restaurant);
        void onViewMenu(Restaurant restaurant);
    }

    private RestaurantActionListener listener;

    public RestaurantAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setListener(RestaurantActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RestaurantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_restaurant, parent, false);
        return new RestaurantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RestaurantViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    private static final DiffUtil.ItemCallback<Restaurant> DIFF_CALLBACK = new DiffUtil.ItemCallback<Restaurant>() {
        @Override
        public boolean areItemsTheSame(@NonNull Restaurant oldItem, @NonNull Restaurant newItem) {
            if (oldItem.getId() == null || newItem.getId() == null) {
                return false;
            }
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Restaurant oldItem, @NonNull Restaurant newItem) {
            return TextUtils.equals(oldItem.getName(), newItem.getName())
                    && TextUtils.equals(oldItem.getDescription(), newItem.getDescription())
                    && TextUtils.equals(oldItem.getAddress(), newItem.getAddress())
                    && TextUtils.equals(oldItem.getPhone(), newItem.getPhone())
                    && TextUtils.equals(oldItem.getImageUrl(), newItem.getImageUrl())
                    && oldItem.getRating() == newItem.getRating()
                    && oldItem.getReviewCount() == newItem.getReviewCount()
                    && oldItem.isActive() == newItem.isActive()
                    && oldItem.isOpen() == newItem.isOpen()
                    && TextUtils.equals(oldItem.getOpenTime(), newItem.getOpenTime())
                    && TextUtils.equals(oldItem.getCloseTime(), newItem.getCloseTime())
                    && oldItem.getDeliveryFee() == newItem.getDeliveryFee()
                    && oldItem.getMinOrder() == newItem.getMinOrder()
                    && TextUtils.equals(oldItem.getUpdatedAt(), newItem.getUpdatedAt());
        }
    };

    class RestaurantViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivImage;
        private final TextView tvName;
        private final TextView tvAddress;
        private final TextView tvRating;
        private final TextView tvStatus;
        private final MaterialSwitch swIsActive;
        private final ImageButton btnMore;

        RestaurantViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_restaurant_image);
            tvName = itemView.findViewById(R.id.tv_name);
            tvAddress = itemView.findViewById(R.id.tv_address);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvStatus = itemView.findViewById(R.id.tv_status);
            swIsActive = itemView.findViewById(R.id.sw_is_active);
            btnMore = itemView.findViewById(R.id.btn_more);
        }

        void bind(Restaurant restaurant) {
            tvName.setText(restaurant.getName());

            String address = restaurant.getAddress();
            if (TextUtils.isEmpty(address)) {
                tvAddress.setText("Chưa có địa chỉ");
                tvAddress.setAlpha(0.7f);
            } else {
                tvAddress.setText(address);
                tvAddress.setAlpha(1f);
            }

            tvRating.setText(String.format("★ %.1f (%d)", restaurant.getRating(), restaurant.getReviewCount()));
            if (restaurant.getRating() == 0 && restaurant.getReviewCount() == 0) {
                tvRating.setText("Chưa có đánh giá");
                tvRating.setAlpha(0.6f);
            } else {
                tvRating.setAlpha(1f);
            }

            if (restaurant.isOpen()) {
                tvStatus.setText("Mở cửa");
                tvStatus.setBackgroundResource(R.drawable.admin_badge_success);
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.white));
            } else {
                tvStatus.setText("Đóng cửa");
                tvStatus.setBackgroundResource(R.drawable.admin_badge_pending);
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.white));
            }

swIsActive.setOnCheckedChangeListener(null);
            swIsActive.setChecked(restaurant.isActive());
            final var thisListener = listener;
            swIsActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String title = isChecked ? "Kích hoạt nhà hàng" : "Vô hiệu nhà hàng";
                String message = isChecked ? "Bạn có muốn kích hoạt nhà hàng này?"
                        : "Nhà hàng sẽ bị ẩn khỏi ứng dụng. Bạn có chắc chắn?";
                new MaterialAlertDialogBuilder(itemView.getContext())
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("Đồng ý", (dialog, which) -> {
                            if (thisListener != null) {
                                thisListener.onStatusChange(restaurant, isChecked);
                            }
                        })
                        .setNegativeButton("Hủy", (dialog, which) -> {
                            swIsActive.setOnCheckedChangeListener(null);
                            swIsActive.setChecked(!isChecked);
                        })
                        .setOnCancelListener(dialog -> {
                            swIsActive.setOnCheckedChangeListener(null);
                            swIsActive.setChecked(!isChecked);
                        })
                        .show();
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(restaurant);
                }
            });

            loadImage(restaurant.getImageUrl());

            btnMore.setOnClickListener(v -> showPopupMenu(v, restaurant));

            itemView.setContentDescription(restaurant.getName() + ", " + (restaurant.isActive() ? "Active" : "Hidden"));
        }

        private void showPopupMenu(View anchor, Restaurant restaurant) {
            PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
            popup.getMenu().add(0, 1, 0, "Xem menu");
            popup.getMenu().add(0, 2, 1, "Xóa");
            popup.setOnMenuItemClickListener(item -> {
                if (listener == null) return false;
                switch (item.getItemId()) {
                    case 1:
                        listener.onViewMenu(restaurant);
                        return true;
                    case 2:
                        listener.onDelete(restaurant);
                        return true;
                    default:
                        return false;
                }
            });
            popup.show();
        }

        private void loadImage(String url) {
            if (TextUtils.isEmpty(url)) {
                ivImage.setImageResource(R.drawable.ic_restaurant);
                return;
            }
            Glide.with(ivImage.getContext())
                    .load(url)
                    .placeholder(R.drawable.ic_restaurant)
                    .error(R.drawable.ic_restaurant)
                    .centerCrop()
                    .into(ivImage);
        }
    }
}
