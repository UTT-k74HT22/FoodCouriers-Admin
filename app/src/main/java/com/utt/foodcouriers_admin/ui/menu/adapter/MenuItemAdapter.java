package com.utt.foodcouriers_admin.ui.menu.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.Category;
import com.utt.foodcouriers_admin.data.model.MenuItem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.ViewHolder> {

    private List<MenuItem> items = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private OnMenuItemClickListener listener;
    private final NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public interface OnMenuItemClickListener {
        void onMenuItemClick(MenuItem item); // For editing
        void onAvailabilityChange(MenuItem item, boolean isAvailable);
        void onDeleteClick(MenuItem item); // For deleting
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<MenuItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }
    
    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage, ivDelete;
        TextView tvName, tvPrice, tvCategory;
        CheckBox swAvailable;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_item_image);
            ivDelete = itemView.findViewById(R.id.iv_delete_item);
            tvName = itemView.findViewById(R.id.tv_item_name);
            tvPrice = itemView.findViewById(R.id.tv_item_price);
            tvCategory = itemView.findViewById(R.id.tv_item_category);
            swAvailable = itemView.findViewById(R.id.sw_available);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMenuItemClick(items.get(pos));
                }
            });

            swAvailable.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAvailabilityChange(items.get(pos), isChecked);
                }
            });
            
            if (ivDelete != null) {
                ivDelete.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && listener != null) {
                        listener.onDeleteClick(items.get(pos));
                    }
                });
            }
        }

        public void bind(MenuItem item) {
            tvName.setText(item.getName());
            tvPrice.setText(formatter.format(item.getPrice()));
            
            // Find and display category name
            String categoryName = "Không rõ danh mục";
            if (item.getCategoryId() != null) {
                for (Category cat : categories) {
                    if (item.getCategoryId().equals(cat.getId())) {
                        categoryName = cat.getName();
                        break;
                    }
                }
            }
            tvCategory.setText(categoryName);
            
            // Prevent listener from triggering during bind
            swAvailable.setOnCheckedChangeListener(null);
            swAvailable.setChecked(item.isAvailable());
            swAvailable.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAvailabilityChange(items.get(pos), isChecked);
                }
            });
            
            // TODO: Load image with Glide/Picasso if URL is not null
        }
    }
}
