package com.utt.foodcouriers_admin.ui.category.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.Category;
import com.utt.foodcouriers_admin.ui.common.dialog.ImageZoomDialogFragment;

public class CategoryAdapter extends ListAdapter<Category, CategoryAdapter.CategoryViewHolder> {

    public interface CategoryActionListener {
        void onEdit(Category category);
        void onStatusChange(Category category, boolean isActive);
        void onDelete(Category category);
        void onViewItems(Category category);
    }

    private CategoryActionListener listener;

    public CategoryAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setListener(CategoryActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    private static final DiffUtil.ItemCallback<Category> DIFF_CALLBACK = new DiffUtil.ItemCallback<Category>() {
        @Override
        public boolean areItemsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
            if (oldItem.getId() == null || newItem.getId() == null) {
                return false;
            }
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
            return TextUtils.equals(oldItem.getName(), newItem.getName())
                    && TextUtils.equals(oldItem.getDescription(), newItem.getDescription())
                    && TextUtils.equals(oldItem.getImageUrl(), newItem.getImageUrl())
                    && oldItem.getSortOrder() == newItem.getSortOrder()
                    && oldItem.isActive() == newItem.isActive()
                    && TextUtils.equals(oldItem.getUpdatedAt(), newItem.getUpdatedAt());
        }
    };

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView ivImage;
        private final TextView tvName;
        private final TextView tvDescription;
        private final TextView tvSortOrder;
        private final TextView tvUpdatedAt;
        private final TextView tvStatusBadge;
        private final MaterialSwitch swIsActive;
        private final ImageButton btnEdit;
        private final ImageButton btnMore;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_category_image);
            tvName = itemView.findViewById(R.id.tv_category_name);
            tvDescription = itemView.findViewById(R.id.tv_category_description);
            tvSortOrder = itemView.findViewById(R.id.tv_sort_order);
            tvUpdatedAt = itemView.findViewById(R.id.tv_updated_at);
            tvStatusBadge = itemView.findViewById(R.id.tv_status_badge);
            swIsActive = itemView.findViewById(R.id.sw_is_active);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnMore = itemView.findViewById(R.id.btn_more);
        }

        void bind(Category category) {
            tvName.setText(category.getName());
            
            if (!TextUtils.isEmpty(category.getDescription())) {
                tvDescription.setText(category.getDescription());
                tvDescription.setVisibility(View.VISIBLE);
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            tvSortOrder.setText(itemView.getContext().getString(R.string.label_sort_order) + " #" + category.getSortOrder());
            tvUpdatedAt.setText(itemView.getContext().getString(R.string.label_updated_at) + ": " + formatTimestamp(category.getUpdatedAt(), category.getCreatedAt()));

            tvStatusBadge.setText(category.isActive() ? R.string.status_active : R.string.status_hidden);
            tvStatusBadge.setBackgroundResource(category.isActive() ? R.drawable.admin_badge_success : R.drawable.admin_badge_pending);

            swIsActive.setOnCheckedChangeListener(null);
            swIsActive.setChecked(category.isActive());
            final Category thisCategory = category;
            swIsActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String title = isChecked ? "Kích hoạt danh mục" : "Ẩn danh mục";
                String message = isChecked ? "Hiển thị danh mục này trên menu?" : "Danh mục sẽ bị ẩn khỏi menu. Bạn có chắc chắn?";
                new MaterialAlertDialogBuilder(itemView.getContext())
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("Đồng ý", (dialog, which) -> {
                            if (listener != null) {
                                listener.onStatusChange(thisCategory, isChecked);
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
                    listener.onEdit(category);
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(category);
                }
            });

            btnMore.setOnClickListener(v -> showPopupMenu(v, category));

            loadImage(category.getImageUrl());

            itemView.setContentDescription(category.getName() + ", " + (category.isActive() ? itemView.getContext().getString(R.string.status_active) : itemView.getContext().getString(R.string.status_hidden)));
        }

        private void showPopupMenu(View anchor, Category category) {
            PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
            popupMenu.inflate(R.menu.menu_category_item);
            popupMenu.setOnMenuItemClickListener(menuItem -> handleMenuItem(menuItem, category));
            popupMenu.show();
        }

        private boolean handleMenuItem(MenuItem menuItem, Category category) {
            if (listener == null) {
                return false;
            }
            int id = menuItem.getItemId();
            if (id == R.id.action_view_items) {
                listener.onViewItems(category);
                return true;
            } else if (id == R.id.action_delete) {
                listener.onDelete(category);
                return true;
            }
            return false;
        }

        private void loadImage(String url) {
            if (TextUtils.isEmpty(url)) {
                ivImage.setImageResource(R.drawable.ic_category);
                return;
            }
            Glide.with(ivImage.getContext())
                    .load(url)
                    .placeholder(R.drawable.ic_category)
                    .error(R.drawable.ic_category)
                    .centerCrop()
                    .into(ivImage);

            ivImage.setOnClickListener(v -> {
                if (v.getContext() instanceof androidx.fragment.app.FragmentActivity) {
                    androidx.fragment.app.FragmentActivity activity = (androidx.fragment.app.FragmentActivity) v.getContext();
                    ImageZoomDialogFragment.newInstance(url).show(activity.getSupportFragmentManager(), "ImageZoomDialog");
                }
            });
        }

        private String formatTimestamp(String updatedAt, String createdAt) {
            String value = !TextUtils.isEmpty(updatedAt) ? updatedAt : createdAt;
            if (TextUtils.isEmpty(value)) {
                return "--";
            }
            if (value.length() > 10) {
                return value.substring(0, 10);
            }
            return value;
        }
    }
}
