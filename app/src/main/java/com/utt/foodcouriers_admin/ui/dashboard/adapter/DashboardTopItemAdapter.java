package com.utt.foodcouriers_admin.ui.dashboard.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.OrderItem;

import java.util.ArrayList;
import java.util.List;

public class DashboardTopItemAdapter extends RecyclerView.Adapter<DashboardTopItemAdapter.ViewHolder> {

    private List<OrderItem> items = new ArrayList<>();

    public void setItems(List<OrderItem> items) {
        this.items = items != null ? items : new ArrayList<>();
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
        holder.bind(item, position + 1);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvItemName, tvItemSales;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.item_rank);
            tvItemName = itemView.findViewById(R.id.item_name);
            tvItemSales = itemView.findViewById(R.id.item_sold_count);
        }

        void bind(OrderItem item, int rank) {
            tvRank.setText("#" + rank);
            
            if (item.getMenuItemName() != null) {
                tvItemName.setText(item.getMenuItemName());
            } else {
                tvItemName.setText("Món ăn");
            }

            tvItemSales.setText("Đã bán: " + item.getQuantity());
        }
    }
}
