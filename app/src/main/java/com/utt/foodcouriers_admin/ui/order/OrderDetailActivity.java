package com.utt.foodcouriers_admin.ui.order;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.data.model.OrderStatus;
import com.utt.foodcouriers_admin.data.model.Shipper;
import com.utt.foodcouriers_admin.data.repository.OrderRepository;
import com.utt.foodcouriers_admin.ui.order.adapter.OrderItemAdapter;
import com.utt.foodcouriers_admin.utils.ToastBanner;

import java.util.List;

public class OrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "extra_order_id";

    private TextView tvOrderCode;
    private TextView tvOrderStatus;
    private TextView tvOrderDate;
    private TextView tvCustomerName;
    private TextView tvCustomerPhone;
    private TextView tvDeliveryAddress;
    private TextView tvRestaurantName;
    private TextView tvSubtotal;
    private TextView tvDeliveryFee;
    private TextView tvDiscount;
    private TextView tvTotal;
    private TextView tvNote;
    private TextView tvAssignedShipper;
    private MaterialButton btnCancelOrder;
    private MaterialButton btnAssignShipper;
    private MaterialButton btnUpdateStatus;
    private OrderItemAdapter itemAdapter;
    private OrderRepository orderRepository;

    private Order currentOrder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        orderRepository = OrderRepository.getInstance();
        initViews();
        setupToolbar();
        setupList();
        loadOrder();
        bindActions();
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initViews() {
        tvOrderCode = findViewById(R.id.tv_order_code);
        tvOrderStatus = findViewById(R.id.tv_order_status);
        tvOrderDate = findViewById(R.id.tv_order_date);
        tvCustomerName = findViewById(R.id.tv_customer_name);
        tvCustomerPhone = findViewById(R.id.tv_customer_phone);
        tvDeliveryAddress = findViewById(R.id.tv_delivery_address);
        tvRestaurantName = findViewById(R.id.tv_restaurant_name);
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvDeliveryFee = findViewById(R.id.tv_delivery_fee);
        tvDiscount = findViewById(R.id.tv_discount);
        tvTotal = findViewById(R.id.tv_total);
        tvNote = findViewById(R.id.tv_note);
        tvAssignedShipper = findViewById(R.id.tv_assigned_shipper);
        btnCancelOrder = findViewById(R.id.btn_cancel_order);
        btnAssignShipper = findViewById(R.id.btn_assign_shipper);
        btnUpdateStatus = findViewById(R.id.btn_update_status);
    }

    private void setupList() {
        RecyclerView rvOrderItems = findViewById(R.id.rv_order_items);
        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new OrderItemAdapter();
        rvOrderItems.setAdapter(itemAdapter);
    }

    private void loadOrder() {
        String orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        orderRepository.getOrderById(orderId, new RepositoryCallback<Order>() {
            @Override
            public void onComplete(BaseResponse<Order> response) {
                currentOrder = response.getData();
                if (currentOrder == null) {
                    ToastBanner.showError(getString(R.string.order_not_found));
                    finish();
                    return;
                }
                bindOrder();
            }
        });
    }

    private void bindOrder() {
        OrderStatus status = currentOrder.getOrderStatus();
        tvOrderCode.setText(currentOrder.getOrderCode());
        tvOrderDate.setText(OrderUiFormatter.formatTime(currentOrder.getCreatedAt()));
        tvOrderStatus.setText(status.getLabel());
        tvOrderStatus.setBackgroundResource(OrderUiFormatter.badgeBackgroundRes(status));
        tvOrderStatus.setTextColor(ContextCompat.getColor(this, OrderUiFormatter.badgeTextColorRes(status)));

        tvCustomerName.setText(currentOrder.getUser() != null ? currentOrder.getUser().getFullName() : getString(R.string.order_unknown_customer));
        tvCustomerPhone.setText(currentOrder.getUser() != null ? currentOrder.getUser().getPhone() : getString(R.string.label_not_provided));
        tvDeliveryAddress.setText(currentOrder.getDeliveryAddress());
        tvRestaurantName.setText(currentOrder.getRestaurant() != null ? currentOrder.getRestaurant().getName() : getString(R.string.order_unknown_restaurant));
        tvSubtotal.setText(OrderUiFormatter.formatCurrency(currentOrder.getSubtotal()));
        tvDeliveryFee.setText(OrderUiFormatter.formatCurrency(currentOrder.getDeliveryFee()));
        tvDiscount.setText("-" + OrderUiFormatter.formatCurrency(currentOrder.getDiscount()));
        tvTotal.setText(OrderUiFormatter.formatCurrency(currentOrder.getTotal()));
        tvNote.setText(TextUtils.isEmpty(currentOrder.getNote()) ? getString(R.string.order_note_empty) : currentOrder.getNote());
        tvAssignedShipper.setText(resolveShipperName());
        itemAdapter.submitList(currentOrder.getItems());

        btnCancelOrder.setEnabled(!status.isLocked());
        btnAssignShipper.setEnabled(!status.isLocked());
        btnUpdateStatus.setEnabled(!status.isLocked());
        btnUpdateStatus.setText(getString(R.string.order_action_next_template, status.next().getLabel()));
    }

    private void bindActions() {
        btnCancelOrder.setOnClickListener(v -> {
            if (currentOrder == null || currentOrder.isLocked()) {
                return;
            }
            orderRepository.updateStatus(currentOrder.getId(), OrderStatus.CANCELLED, new RepositoryCallback<Void>() {
                @Override
                public void onComplete(BaseResponse<Void> response) {
                    if (response.isSuccess()) {
                        ToastBanner.showWarning(getString(R.string.order_status_cancelled));
                        loadOrder();
                    } else {
                        ToastBanner.showError(response.getMessage());
                    }
                }
            });
        });

        btnUpdateStatus.setOnClickListener(v -> {
            if (currentOrder == null || currentOrder.isLocked()) {
                return;
            }
            orderRepository.updateStatus(currentOrder.getId(), currentOrder.getOrderStatus().next(), new RepositoryCallback<Void>() {
                @Override
                public void onComplete(BaseResponse<Void> response) {
                    if (response.isSuccess()) {
                        ToastBanner.showSuccess(getString(R.string.order_status_updated));
                        loadOrder();
                    } else {
                        ToastBanner.showError(response.getMessage());
                    }
                }
            });
        });

        btnAssignShipper.setOnClickListener(v -> showShipperDialog());
    }

    private void showShipperDialog() {
        if (currentOrder == null || currentOrder.isLocked()) {
            return;
        }
        String restaurantId = currentOrder.getRestaurantId();
        orderRepository.getAssignableShippers(restaurantId, new RepositoryCallback<List<Shipper>>() {
            @Override
            public void onComplete(BaseResponse<List<Shipper>> response) {
                List<Shipper> shippers = response.isSuccess() && response.getData() != null ? response.getData() : java.util.Collections.emptyList();
                if (shippers.isEmpty()) {
                    ToastBanner.showWarning("Không có shipper nào sẵn sàng");
                    return;
                }
                String[] names = new String[shippers.size()];
                for (int i = 0; i < shippers.size(); i++) {
                    names[i] = shippers.get(i).getFullName() + " - " + shippers.get(i).getPhone();
                }
                new MaterialAlertDialogBuilder(OrderDetailActivity.this)
                        .setTitle(R.string.order_assign_shipper)
                        .setItems(names, (dialog, which) -> orderRepository.assignShipper(currentOrder.getId(), shippers.get(which), new RepositoryCallback<Void>() {
                            @Override
                            public void onComplete(BaseResponse<Void> response) {
                                if (response.isSuccess()) {
                                    ToastBanner.showSuccess(getString(R.string.order_assign_shipper_success));
                                    loadOrder();
                                } else {
                                    ToastBanner.showError(response.getMessage());
                                }
                            }
                        }))
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();
            }
        });
    }

    private String resolveShipperName() {
        if (currentOrder.getShipper() != null && !TextUtils.isEmpty(currentOrder.getShipper().getFullName())) {
            return currentOrder.getShipper().getFullName();
        }
        return getString(R.string.order_shipper_unassigned);
    }
}
