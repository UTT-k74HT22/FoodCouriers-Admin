package com.utt.foodcouriers_admin.ui.dashboard.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.OrderItem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * DashboardTopItemAdapter - Optimized for ranking and popularity.
 */
public class DashboardTopItemAdapter extends RecyclerView.Adapter<DashboardTopItemAdapter.ViewHolder> {

    private List<OrderItem> items = new ArrayList<>();
    private int maxQuantity = 1;

    public void setItems(List<OrderItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        
        // Find max quantity to set progress ratio
        maxQuantity = 1;
        for (OrderItem item : this.items) {
            if (item.getQuantity() > maxQuantity) {
                maxQuantity = item.getQuantity();
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_menu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderItem item = items.get(position);
        holder.bind(item, position + 1, maxQuantity);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvProductName, tvSalesCount, tvRevenue;
        LinearProgressIndicator progressPopularity;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_rank);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            tvSalesCount = itemView.findViewById(R.id.tv_sales_count);
            tvRevenue = itemView.findViewById(R.id.tv_product_revenue);
            progressPopularity = itemView.findViewById(R.id.progress_popularity);
        }

        void bind(OrderItem item, int rank, int maxQty) {
            tvRank.setText(String.valueOf(rank));
            tvProductName.setText(item.getMenuItemName());
            tvSalesCount.setText("Đã bán: " + item.getQuantity() + " suất");

            // Calculate revenue and format
            double revenue = item.getQuantity() * item.getMenuItemPrice();
            if (revenue >= 1000000) {
                tvRevenue.setText(String.format(Locale.US, "%.1fM", revenue / 1000000.0));
            } else {
                tvRevenue.setText(String.format(Locale.US, "%.0fk", revenue / 1000.0));
            }

            // Set progress based on relative popularity
            int progress = (int) ((item.getQuantity() / (float) maxQty) * 100);
            progressPopularity.setProgress(progress);
            
            // Highlight top 3 ranks
            if (rank == 1) {
                tvRank.setTextColor(itemView.getContext().getColor(R.color.primary));
            } else {
                tvRank.setTextColor(itemView.getContext().getColor(R.color.text_primary));
            }
        }
    }
}
