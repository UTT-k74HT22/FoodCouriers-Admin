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
/** Mục đích : Hiển thị danh sách đơn hàng.*/
public class OrderAdapter extends ListAdapter<Order, OrderAdapter.OrderViewHolder> {

    public interface OrderActionListener {
        void onOpenDetail(Order order);
        void onAccept(Order order);
        void onReject(Order order);
        void onNextStep(Order order);
        void onAssignShipper(Order order);
        void onPickup(Order order);
        void onComplete(Order order);
    }

    private OrderActionListener listener;
    private boolean isShipper = false;

    public OrderAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setListener(OrderActionListener listener) {
        this.listener = listener;
    }

    public void setShipperMode(boolean isShipper) {
        this.isShipper = isShipper;
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
    /** ViewHolder để hiển thị đơn hàng trong danh sách.
     * Mục đích : Hiển thị thông tin đơn hàng trong danh sách.*/
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
        private final MaterialButton btnPickup;
        private final MaterialButton btnComplete;

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
            btnPickup = btnNextStep; 
            btnComplete = btnNextStep;
        }

        /** Gắn dữ liệu đơn hàng vào ViewHolder.*/
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

            if (isShipper) {
                bindShipperButtons(order, status);
            } else {
                bindAdminButtons(order, status);
            }
        }
        /** Gắn các nút bấm trạng thái cho admin .*/
        private void bindAdminButtons(Order order, OrderStatus status) {
            boolean isPending = status == OrderStatus.PENDING;
            btnAccept.setVisibility(isPending ? View.VISIBLE : View.GONE);
            btnReject.setVisibility(isPending ? View.VISIBLE : View.GONE);

            boolean isLocked = status.isLocked();
            boolean isShipperActionTime = (status == OrderStatus.READY_FOR_PICKUP || status == OrderStatus.ASSIGNED || status == OrderStatus.DELIVERING);
            
            boolean hasShipper = order.getShipper() != null || !TextUtils.isEmpty(order.getShipperId());
            boolean isConfirmStep = status == OrderStatus.CONFIRMED;
            boolean showNext = !isLocked && !isPending && status != OrderStatus.DELIVERING && !(isConfirmStep && !hasShipper);
            
            btnNextStep.setVisibility(showNext ? View.VISIBLE : View.GONE);
            btnAssignShipper.setVisibility(!isLocked && !isPending && !isShipperActionTime ? View.VISIBLE : View.GONE);

            btnNextStep.setText(itemView.getContext().getString(R.string.order_action_next_template, status.next().getLabel()));
            btnAssignShipper.setText(order.getShipper() == null ? R.string.order_assign_shipper : R.string.order_change_shipper);

            btnAccept.setOnClickListener(v -> { if (listener != null) listener.onAccept(order); });
            btnReject.setOnClickListener(v -> { if (listener != null) listener.onReject(order); });
            btnNextStep.setOnClickListener(v -> { if (listener != null) listener.onNextStep(order); });
            btnAssignShipper.setOnClickListener(v -> { if (listener != null) listener.onAssignShipper(order); });

            ColorStateList tint = ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.primary));
            btnAssignShipper.setStrokeColor(tint);
        }
        /**
         * Gắn các nút bấm của đơn hàng do cho shipper  .*/
        private void bindShipperButtons(Order order, OrderStatus status) {
            btnAccept.setVisibility(View.GONE);
            btnReject.setVisibility(View.GONE);
            btnAssignShipper.setVisibility(View.GONE);

            if (order.getShipperId() == null) {
                btnNextStep.setVisibility(View.VISIBLE);
                btnNextStep.setText("Nhận đơn hàng");
                btnNextStep.setOnClickListener(v -> { if (listener != null) listener.onAccept(order); });
            } else if (status == OrderStatus.READY_FOR_PICKUP) {
                btnPickup.setVisibility(View.VISIBLE);
                btnPickup.setText("Xác nhận lấy hàng");
                btnPickup.setOnClickListener(v -> { if (listener != null) listener.onPickup(order); });
            } else if (status == OrderStatus.DELIVERING) {
                btnComplete.setVisibility(View.VISIBLE);
                btnComplete.setText("Hoàn thành giao hàng");
                btnComplete.setOnClickListener(v -> { if (listener != null) listener.onComplete(order); });
            } else {
                btnNextStep.setVisibility(View.GONE);
            }
        }
        /** Hàm này được sử dụng để xác định tên của shipper.*/
        private String resolveShipperName(Order order) {
            if (order.getShipper() != null && !TextUtils.isEmpty(order.getShipper().getFullName())) {
                return order.getShipper().getFullName();
            }
            if (!TextUtils.isEmpty(order.getShipperId())) {
                return "Đã gán shipper";
            }
            return itemView.getContext().getString(R.string.order_shipper_unassigned);
        }
    }
}
