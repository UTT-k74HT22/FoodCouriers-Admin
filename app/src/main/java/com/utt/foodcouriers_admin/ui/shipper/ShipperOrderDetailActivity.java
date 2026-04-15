package com.utt.foodcouriers_admin.ui.shipper;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.data.model.OrderStatus;
import com.utt.foodcouriers_admin.data.repository.DeliveryRepository;
import com.utt.foodcouriers_admin.ui.order.OrderUiFormatter;
import com.utt.foodcouriers_admin.ui.order.adapter.OrderItemAdapter;
import com.utt.foodcouriers_admin.utils.SessionManager;
import com.utt.foodcouriers_admin.utils.ToastBanner;

public class ShipperOrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "extra_order_id";

    private TextView tvOrderCode;
    private TextView tvStatus;
    private TextView tvOrderDate;
    private TextView tvRestaurantName;
    private TextView tvCustomerName;
    private TextView tvCustomerPhone;
    private TextView tvDeliveryAddress;
    private TextView tvNote;
    private TextView tvSubtotal;
    private TextView tvDeliveryFee;
    private TextView tvDiscount;
    private TextView tvTotal;
    private MaterialButton btnPickup;
    private MaterialButton btnDelivered;
    private OrderItemAdapter itemAdapter;
    private DeliveryRepository repository;
    private SessionManager sessionManager;
    private Order currentOrder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipper_order_detail);

        repository = DeliveryRepository.getInstance();
        sessionManager = SessionManager.getInstance(this);
        
        initViews();
        setupToolbar();
        setupList();
        loadOrder();
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
        tvStatus = findViewById(R.id.tv_status);
        tvOrderDate = findViewById(R.id.tv_order_date);
        tvRestaurantName = findViewById(R.id.tv_restaurant_name);
        tvCustomerName = findViewById(R.id.tv_customer_name);
        tvCustomerPhone = findViewById(R.id.tv_customer_phone);
        tvDeliveryAddress = findViewById(R.id.tv_delivery_address);
        tvNote = findViewById(R.id.tv_note);
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvDeliveryFee = findViewById(R.id.tv_delivery_fee);
        tvDiscount = findViewById(R.id.tv_discount);
        tvTotal = findViewById(R.id.tv_total);
        btnPickup = findViewById(R.id.btn_pickup);
        btnDelivered = findViewById(R.id.btn_delivered);
    }

    private void setupList() {
        RecyclerView rvOrderItems = findViewById(R.id.rv_order_items);
        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new OrderItemAdapter();
        rvOrderItems.setAdapter(itemAdapter);
    }

    private void loadOrder() {
        String orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        repository.getOrderById(orderId, new RepositoryCallback<Order>() {
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
        tvRestaurantName.setText(currentOrder.getRestaurant() != null ? currentOrder.getRestaurant().getName() : "");
        
        tvCustomerName.setText(currentOrder.getUser() != null ? currentOrder.getUser().getFullName() : getString(R.string.order_unknown_customer));
        tvCustomerPhone.setText(currentOrder.getUser() != null ? currentOrder.getUser().getPhone() : "");
        tvDeliveryAddress.setText(currentOrder.getDeliveryAddress());
        
        if (!TextUtils.isEmpty(currentOrder.getNote())) {
            tvNote.setVisibility(android.view.View.VISIBLE);
            tvNote.setText(currentOrder.getNote());
        } else {
            tvNote.setVisibility(android.view.View.GONE);
        }
        
        tvSubtotal.setText(OrderUiFormatter.formatCurrency(currentOrder.getSubtotal()));
        tvDeliveryFee.setText(OrderUiFormatter.formatCurrency(currentOrder.getDeliveryFee()));
        tvDiscount.setText("-" + OrderUiFormatter.formatCurrency(currentOrder.getDiscount()));
        tvTotal.setText(OrderUiFormatter.formatCurrency(currentOrder.getTotal()));
        
        itemAdapter.submitList(currentOrder.getItems());

        tvStatus.setText(status.getLabel());
        tvStatus.setBackgroundResource(OrderUiFormatter.badgeBackgroundRes(status));
        tvStatus.setTextColor(ContextCompat.getColor(this, OrderUiFormatter.badgeTextColorRes(status)));

        updateButtons(status);
    }

    private void updateButtons(OrderStatus status) {
        switch (status) {
            case CONFIRMED:
            case PREPARING:
            case READY_FOR_PICKUP:
                btnPickup.setVisibility(android.view.View.VISIBLE);
                btnDelivered.setVisibility(android.view.View.GONE);
                btnPickup.setOnClickListener(v -> pickupOrder());
                break;
                
            case DELIVERING:
                btnPickup.setVisibility(android.view.View.GONE);
                btnDelivered.setVisibility(android.view.View.VISIBLE);
                btnDelivered.setOnClickListener(v -> completeOrder());
                break;
                
            case DELIVERED:
                btnPickup.setVisibility(android.view.View.GONE);
                btnDelivered.setVisibility(android.view.View.GONE);
                break;
                
            default:
                btnPickup.setVisibility(android.view.View.GONE);
                btnDelivered.setVisibility(android.view.View.GONE);
                break;
        }
    }

    private void pickupOrder() {
        repository.pickupOrder(currentOrder.getId(), sessionManager.getUserId(), new RepositoryCallback<Void>() {
            @Override
            public void onComplete(BaseResponse<Void> response) {
                if (response.isSuccess()) {
                    ToastBanner.showSuccess("Đã lấy hàng thành công!");
                    loadOrder();
                } else {
                    ToastBanner.showError(response.getMessage());
                }
            }
        });
    }

    private void completeOrder() {
        repository.completeOrder(currentOrder.getId(), sessionManager.getUserId(), new RepositoryCallback<Void>() {
            @Override
            public void onComplete(BaseResponse<Void> response) {
                if (response.isSuccess()) {
                    ToastBanner.showSuccess("Đã giao hàng thành công!");
                    finish();
                } else {
                    ToastBanner.showError(response.getMessage());
                }
            }
        });
    }
}
