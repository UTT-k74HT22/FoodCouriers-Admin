package com.utt.foodcouriers_admin.ui.dashboard.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.Order;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardOrderAdapter extends RecyclerView.Adapter<DashboardOrderAdapter.ViewHolder> {

    private List<Order> orders = new ArrayList<>();
    private OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public void setOnOrderClickListener(OnOrderClickListener listener) {
        this.listener = listener;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders != null ? orders : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order, listener);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderCode, tvCustomerName, tvRestaurantName, tvTotal, tvStatus, tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderCode = itemView.findViewById(R.id.order_code);
            tvCustomerName = itemView.findViewById(R.id.customer_name);
            tvRestaurantName = itemView.findViewById(R.id.restaurant_name);
            tvTotal = itemView.findViewById(R.id.order_total);
            tvStatus = itemView.findViewById(R.id.order_status);
            tvTime = itemView.findViewById(R.id.order_time);
        }

        void bind(Order order, OnOrderClickListener listener) {
            tvOrderCode.setText(order.getOrderCode() != null ? order.getOrderCode() : "#" + (order.getId() != null ? order.getId().substring(0, 8) : "N/A"));
            
            if (order.getUser() != null) {
                tvCustomerName.setText(order.getUser().getFullName());
            } else {
                tvCustomerName.setText("Khách hàng");
            }

            if (order.getRestaurant() != null) {
                tvRestaurantName.setText(order.getRestaurant().getName());
            } else {
                tvRestaurantName.setText("Nhà hàng");
            }

            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            tvTotal.setText(formatter.format(order.getTotal()));
            
            String statusStr = order.getStatus();
            tvStatus.setText(getStatusText(statusStr));
            
            if (order.getCreatedAt() != null && order.getCreatedAt().length() >= 16) {
                String timePart = order.getCreatedAt().substring(11, 16);
                tvTime.setText(timePart);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOrderClick(order);
            });
        }

        private String getStatusText(String status) {
            if (status == null) return "N/A";
            switch (status) {
                case "pending": return "Đang chờ";
                case "confirmed": return "Đã xác nhận";
                case "preparing": return "Chuẩn bị";
                case "delivered": return "Hoàn thành";
                case "cancelled": return "Đã hủy";
                default: return status;
            }
        }
    }
}
