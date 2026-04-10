package com.utt.foodcouriers_admin.ui.order.adapter;

import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.data.model.OrderStatus;
import com.utt.foodcouriers_admin.ui.order.OrderUiFormatter;

public class OrderAdapter extends ListAdapter<Order, OrderAdapter.OrderViewHolder> {

    public interface OrderActionListener {
        void onOpenDetail(Order order);
        void onAccept(Order order);
        void onReject(Order order);
        void onNextStep(Order order);
        void onAssignShipper(Order order);
    }

    private OrderActionListener listener;

    public OrderAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setListener(OrderActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    private static final DiffUtil.ItemCallback<Order> DIFF_CALLBACK = new DiffUtil.ItemCallback<Order>() {
        @Override
        public boolean areItemsTheSame(@NonNull Order oldItem, @NonNull Order newItem) {
            return TextUtils.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Order oldItem, @NonNull Order newItem) {
            return TextUtils.equals(oldItem.getUpdatedAt(), newItem.getUpdatedAt())
                    && TextUtils.equals(oldItem.getStatus(), newItem.getStatus())
                    && TextUtils.equals(oldItem.getShipperId(), newItem.getShipperId())
                    && oldItem.getTotal() == newItem.getTotal();
        }
    };

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvOrderCode;
        private final TextView tvCustomerName;
        private final TextView tvRestaurantName;
        private final TextView tvOrderTotal;
        private final TextView tvOrderStatus;
        private final TextView tvOrderTime;
        private final TextView tvOrderMeta;
        private final MaterialButton btnAccept;
        private final MaterialButton btnReject;
        private final MaterialButton btnNextStep;
        private final MaterialButton btnAssignShipper;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderCode = itemView.findViewById(R.id.tv_order_code);
            tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
            tvRestaurantName = itemView.findViewById(R.id.tv_restaurant);
            tvOrderTotal = itemView.findViewById(R.id.tv_total);
            tvOrderStatus = itemView.findViewById(R.id.tv_status);
            tvOrderTime = itemView.findViewById(R.id.tv_time);
            tvOrderMeta = itemView.findViewById(R.id.tv_order_meta);
            btnAccept = itemView.findViewById(R.id.btn_accept_order);
            btnReject = itemView.findViewById(R.id.btn_reject_order);
            btnNextStep = itemView.findViewById(R.id.btn_next_step);
            btnAssignShipper = itemView.findViewById(R.id.btn_assign_shipper);
        }

        void bind(Order order) {
            OrderStatus status = order.getOrderStatus();
            tvOrderCode.setText(order.getOrderCode());
            tvCustomerName.setText(order.getUser() != null ? order.getUser().getFullName() : itemView.getContext().getString(R.string.order_unknown_customer));
            tvRestaurantName.setText(order.getRestaurant() != null ? order.getRestaurant().getName() : itemView.getContext().getString(R.string.order_unknown_restaurant));
            tvOrderTotal.setText(OrderUiFormatter.formatCurrency(order.getTotal()));
            tvOrderTime.setText(OrderUiFormatter.formatTime(order.getCreatedAt()));
            tvOrderMeta.setText(itemView.getContext().getString(R.string.order_meta_summary, order.getItemCount(), resolveShipperName(order)));

            tvOrderStatus.setText(status.getLabel());
            tvOrderStatus.setBackgroundResource(OrderUiFormatter.badgeBackgroundRes(status));
            tvOrderStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), OrderUiFormatter.badgeTextColorRes(status)));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOpenDetail(order);
                }
            });

            bindButtons(order, status);
        }

        private void bindButtons(Order order, OrderStatus status) {
            btnAccept.setVisibility(status == OrderStatus.PENDING ? View.VISIBLE : View.GONE);
            btnReject.setVisibility(status == OrderStatus.PENDING ? View.VISIBLE : View.GONE);

            boolean showNext = status != OrderStatus.PENDING && !status.isLocked();
            btnNextStep.setVisibility(showNext ? View.VISIBLE : View.GONE);
            btnAssignShipper.setVisibility(status == OrderStatus.PREPARING || status == OrderStatus.DELIVERING ? View.VISIBLE : View.GONE);

            btnNextStep.setText(itemView.getContext().getString(R.string.order_action_next_template, status.next().getLabel()));
            btnAssignShipper.setText(order.getShipper() == null ? R.string.order_assign_shipper : R.string.order_change_shipper);

            btnAccept.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAccept(order);
                }
            });
            btnReject.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReject(order);
                }
            });
            btnNextStep.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNextStep(order);
                }
            });
            btnAssignShipper.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAssignShipper(order);
                }
            });

            ColorStateList tint = ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.primary));
            btnAssignShipper.setStrokeColor(tint);
        }

        private String resolveShipperName(Order order) {
            if (order.getShipper() != null && !TextUtils.isEmpty(order.getShipper().getFullName())) {
                return order.getShipper().getFullName();
            }
            return itemView.getContext().getString(R.string.order_shipper_unassigned);
        }
    }
}
