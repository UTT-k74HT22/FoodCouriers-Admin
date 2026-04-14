package com.utt.foodcouriers_admin.ui.shipper.adapter;

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
import com.google.android.material.materialswitch.MaterialSwitch;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.ShipperProfile;
import com.utt.foodcouriers_admin.ui.common.dialog.ImageZoomDialogFragment;

public class ShipperAdapter extends ListAdapter<ShipperProfile, ShipperAdapter.ShipperViewHolder> {

    public interface ShipperActionListener {
        void onEdit(ShipperProfile shipper);
        void onStatusChange(ShipperProfile shipper, boolean isActive);
        void onDelete(ShipperProfile shipper);
    }

    private ShipperActionListener listener;

    public ShipperAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setListener(ShipperActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ShipperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shipper, parent, false);
        return new ShipperViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShipperViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    private static final DiffUtil.ItemCallback<ShipperProfile> DIFF_CALLBACK = new DiffUtil.ItemCallback<ShipperProfile>() {
        @Override
        public boolean areItemsTheSame(@NonNull ShipperProfile oldItem, @NonNull ShipperProfile newItem) {
            if (oldItem.getId() == null || newItem.getId() == null) {
                return false;
            }
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ShipperProfile oldItem, @NonNull ShipperProfile newItem) {
            return TextUtils.equals(oldItem.getFullName(), newItem.getFullName())
                    && TextUtils.equals(oldItem.getPhone(), newItem.getPhone())
                    && TextUtils.equals(oldItem.getAvatarUrl(), newItem.getAvatarUrl())
                    && TextUtils.equals(oldItem.getRestaurantName(), newItem.getRestaurantName())
                    && oldItem.isActive() == newItem.isActive();
        }
    };

    class ShipperViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivAvatar;
        private final TextView tvName;
        private final TextView tvPhone;
        private final TextView tvRestaurantName;
        private final TextView tvOrdersCompleted;
        private final TextView tvStatus;
        private final MaterialSwitch swIsActive;
        private final ImageButton btnEdit;
        private final ImageButton btnMore;

        ShipperViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_shipper_avatar);
            tvName = itemView.findViewById(R.id.tv_shipper_name);
            tvPhone = itemView.findViewById(R.id.tv_shipper_phone);
            tvRestaurantName = itemView.findViewById(R.id.tv_restaurant_name);
            tvOrdersCompleted = itemView.findViewById(R.id.tv_orders_completed);
            tvStatus = itemView.findViewById(R.id.tv_shipper_status);
            swIsActive = itemView.findViewById(R.id.sw_is_active);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnMore = itemView.findViewById(R.id.btn_more);
        }

        void bind(ShipperProfile shipper) {
            tvName.setText(shipper.getFullName());
            tvPhone.setText(TextUtils.isEmpty(shipper.getPhone()) ? "--" : shipper.getPhone());

            String restaurantName = shipper.getRestaurantName();
            if (!TextUtils.isEmpty(restaurantName)) {
                tvRestaurantName.setText(itemView.getContext().getString(R.string.shipper_restaurant, restaurantName));
                tvRestaurantName.setVisibility(View.VISIBLE);
            } else {
                tvRestaurantName.setText(itemView.getContext().getString(R.string.shipper_restaurant_none));
                tvRestaurantName.setVisibility(View.VISIBLE);
            }

            tvOrdersCompleted.setText(itemView.getContext().getString(R.string.shipper_orders_count, shipper.getTotalDelivered()));

            tvStatus.setText(shipper.isActive() ? R.string.shipper_status_available : R.string.shipper_status_unavailable);
            tvStatus.setBackgroundResource(shipper.isActive() ? R.drawable.admin_badge_success : R.drawable.admin_badge_pending);

            swIsActive.setOnCheckedChangeListener(null);
            swIsActive.setChecked(shipper.isActive());
            swIsActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onStatusChange(shipper, isChecked);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(shipper);
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(shipper);
                }
            });

            btnMore.setOnClickListener(v -> showPopupMenu(v, shipper));

            loadAvatar(shipper.getAvatarUrl());

            itemView.setContentDescription(shipper.getFullName() + ", " + (shipper.isActive() ? itemView.getContext().getString(R.string.status_active) : itemView.getContext().getString(R.string.status_inactive)));
        }

        private void showPopupMenu(View anchor, ShipperProfile shipper) {
            PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
            popupMenu.inflate(R.menu.menu_shipper_item);
            popupMenu.setOnMenuItemClickListener(menuItem -> handleMenuItem(menuItem, shipper));
            popupMenu.show();
        }

        private boolean handleMenuItem(MenuItem menuItem, ShipperProfile shipper) {
            if (listener == null) {
                return false;
            }
            int id = menuItem.getItemId();
            if (id == R.id.action_delete) {
                listener.onDelete(shipper);
                return true;
            }
            return false;
        }

        private void loadAvatar(String url) {
            if (TextUtils.isEmpty(url)) {
                ivAvatar.setImageResource(R.drawable.ic_shipper);
                return;
            }
            Glide.with(ivAvatar.getContext())
                    .load(url)
                    .placeholder(R.drawable.ic_shipper)
                    .error(R.drawable.ic_shipper)
                    .centerCrop()
                    .into(ivAvatar);

            ivAvatar.setOnClickListener(v -> {
                if (v.getContext() instanceof androidx.fragment.app.FragmentActivity) {
                    androidx.fragment.app.FragmentActivity activity = (androidx.fragment.app.FragmentActivity) v.getContext();
                    ImageZoomDialogFragment.newInstance(url).show(activity.getSupportFragmentManager(), "ImageZoomDialog");
                }
            });
        }
    }
}
