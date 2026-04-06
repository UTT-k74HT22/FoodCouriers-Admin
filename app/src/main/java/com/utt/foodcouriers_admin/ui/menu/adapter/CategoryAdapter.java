package com.utt.foodcouriers_admin.ui.menu.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private List<Category> categories = new ArrayList<>();
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
        void onStatusChange(Category category, boolean isActive);
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvItemCount, tvSortOrder;
        MaterialSwitch swIsActive;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_category_image);
            tvName = itemView.findViewById(R.id.tv_category_name);
            tvItemCount = itemView.findViewById(R.id.tv_item_count);
            tvSortOrder = itemView.findViewById(R.id.tv_sort_order);
            swIsActive = itemView.findViewById(R.id.sw_is_active);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCategoryClick(categories.get(pos));
                }
            });

            swIsActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onStatusChange(categories.get(pos), isChecked);
                }
            });
        }

        public void bind(Category category) {
            tvName.setText(category.getName());
            tvSortOrder.setText("#" + category.getSortOrder());
            swIsActive.setChecked(category.isActive());
            // tvItemCount.setText(...) // This might need another API call or join
            
            // TODO: Load image with Glide/Picasso if URL is not null
        }
    }
}
