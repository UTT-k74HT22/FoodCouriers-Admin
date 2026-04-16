package com.utt.foodcouriers_admin.ui.order.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.OrderItem;
import com.utt.foodcouriers_admin.ui.order.OrderUiFormatter;

import java.util.ArrayList;
import java.util.List;
/** Hiển thị danh sách các sản phẩm trong đơn hàng.*/
public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder> {

    private final List<OrderItem> items = new ArrayList<>();

    public void submitList(List<OrderItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_detail_item, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvItemName;
        private final TextView tvQuantity;
        private final TextView tvItemSubtotal;

        OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tv_item_name);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            tvItemSubtotal = itemView.findViewById(R.id.tv_item_subtotal);
        }

        void bind(OrderItem item) {
            tvItemName.setText(item.getMenuItemName());
            tvQuantity.setText("x" + item.getQuantity());
            tvItemSubtotal.setText(OrderUiFormatter.formatCurrency(item.getSubtotal()));
        }
    }
}
