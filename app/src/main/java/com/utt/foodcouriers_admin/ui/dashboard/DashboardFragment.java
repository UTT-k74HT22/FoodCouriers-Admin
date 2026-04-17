package com.utt.foodcouriers_admin.ui.dashboard;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.DashboardStats;
import com.utt.foodcouriers_admin.data.realtime.OrderRealtimeManager;
import com.utt.foodcouriers_admin.utils.ToastBanner;

import java.text.NumberFormat;
import java.util.Locale;

import androidx.core.content.ContextCompat;

import com.google.gson.JsonObject;

public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;
    private com.utt.foodcouriers_admin.ui.dashboard.adapter.DashboardOrderAdapter orderAdapter;
    private com.utt.foodcouriers_admin.ui.dashboard.adapter.DashboardTopItemAdapter topItemAdapter;
    private OrderRealtimeManager realtimeManager;
    private OrderRealtimeManager.OrderRealtimeCallback realtimeCallback;
    
    // Khai báo các view thống kê
    private View cardOrders, cardRevenue, cardProcessing, cardCompleted;
    private RecyclerView rvRecentOrders;
    private RecyclerView rvTopItems;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Nạp giao diện activity_dashboard.xml
        return inflater.inflate(R.layout.activity_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Ánh xạ các View từ XML
        initViews(view);
        
        // 2. Cấu hình ban đầu cho các thẻ (Đặt icon và nhãn)
        setupStatsCards();
        setupRecentOrders();
        setupTopItems();

        // 3. Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // 4. "Đăng ký" lắng nghe dữ liệu từ ViewModel
        observeViewModel();

        // 5. Bắt đầu tải dữ liệu từ Supabase
        viewModel.loadDashboardData();
        
        // 6. Khởi tạo realtime để auto refresh dashboard khi có đơn mới
        initRealtime();
    }

    private void initViews(View view) {
        // Ánh xạ các thẻ include từ XML
        cardOrders = view.findViewById(R.id.stat_orders_today);
        cardRevenue = view.findViewById(R.id.stat_revenue);
        cardProcessing = view.findViewById(R.id.stat_processing);
        cardCompleted = view.findViewById(R.id.stat_completed);
        
        rvRecentOrders = view.findViewById(R.id.rv_recent_orders);
        rvTopItems = view.findViewById(R.id.rv_top_items);
    }

    private void setupStatsCards() {
        if (getContext() == null) return;
        // Cấu hình thẻ Tổng đơn
        updateStatCard(cardOrders, R.drawable.ic_orders, "Tổng đơn hôm nay", "0", R.color.primary);
        // Cấu hình thẻ Doanh thu
        updateStatCard(cardRevenue, R.drawable.ic_revenue, "Doanh thu", "0đ", R.color.success);
        // Cấu hình thẻ Đang xử lý
        updateStatCard(cardProcessing, R.drawable.ic_pending, "Đang xử lý", "0", R.color.warning);
        // Cấu hình thẻ Hoàn thành
        updateStatCard(cardCompleted, R.drawable.ic_completed, "Hoàn thành", "0", R.color.info);
    }

    private void observeViewModel() {
        // Khi có dữ liệu thống kê mới
        viewModel.getStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                displayStats(stats);
            }
        });

        // Khi có danh sách đơn hàng mới
        viewModel.getRecentOrders().observe(getViewLifecycleOwner(), orders -> {
            if (orders != null) {
                orderAdapter.setOrders(orders);
            }
        });

        // Khi có danh sách món ăn bán chạy
        viewModel.getTopItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                topItemAdapter.setItems(items);
            }
        });

        // Khi có lỗi xảy ra
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                // Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayStats(DashboardStats stats) {
        // Cập nhật con số thực tế vào các thẻ
        updateStatValue(cardOrders, String.valueOf(stats.getTotalOrders()));
        updateStatValue(cardRevenue, formatCurrency(stats.getTotalRevenue()));
        updateStatValue(cardProcessing, String.valueOf(stats.getPendingOrders()));
        updateStatValue(cardCompleted, String.valueOf(stats.getCompletedOrders()));
    }

    /**
     * Hàm tiện ích để cập nhật nội dung cho 1 thẻ thống kê (stat_card)
     */
    private void updateStatCard(View card, int iconRes, String label, String value, int colorRes) {
        if (getContext() == null) return;
        
        ImageView icon = card.findViewById(R.id.stat_icon);
        TextView tvLabel = card.findViewById(R.id.stat_label);
        TextView tvValue = card.findViewById(R.id.stat_value);

        int color = ContextCompat.getColor(getContext(), colorRes);

        icon.setImageResource(iconRes);
        icon.setColorFilter(color);
        tvLabel.setText(label);
        tvValue.setText(value);
        tvValue.setTextColor(color);
    }

    private void updateStatValue(View card, String value) {
        TextView tvValue = card.findViewById(R.id.stat_value);
        tvValue.setText(value);
    }

    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }

    private void setupRecentOrders() {
        rvRecentOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        orderAdapter = new com.utt.foodcouriers_admin.ui.dashboard.adapter.DashboardOrderAdapter();
        rvRecentOrders.setAdapter(orderAdapter);
    }

    private void setupTopItems() {
        rvTopItems.setLayoutManager(new LinearLayoutManager(getContext()));
        topItemAdapter = new com.utt.foodcouriers_admin.ui.dashboard.adapter.DashboardTopItemAdapter();
        rvTopItems.setAdapter(topItemAdapter);
    }

    private void initRealtime() {
        realtimeManager = OrderRealtimeManager.getInstance();
        
        realtimeCallback = new OrderRealtimeManager.OrderRealtimeCallback() {
            @Override
            public void onNewOrder(JsonObject order) {
                if (getActivity() == null) return;
                Log.d("DashboardFragment", "New order received via realtime");
                String orderCode = order.has("order_code") ? order.get("order_code").getAsString() : "mới";
                ToastBanner.showInfo("Đơn hàng mới: " + orderCode);
                // Auto refresh dashboard
                viewModel.loadDashboardData();
            }

            @Override
            public void onOrderUpdated(JsonObject newOrder, JsonObject oldOrder) {
                if (getActivity() == null) return;
                Log.d("DashboardFragment", "Order updated via realtime");
                // Auto refresh dashboard
                viewModel.loadDashboardData();
            }

            @Override
            public void onOrderDeleted(JsonObject oldOrder) {
                if (getActivity() == null) return;
                Log.d("DashboardFragment", "Order deleted via realtime");
                viewModel.loadDashboardData();
            }
        };
        realtimeManager.subscribe(realtimeCallback);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (realtimeManager != null && realtimeCallback != null) {
            realtimeManager.unsubscribe(realtimeCallback);
            realtimeCallback = null;
        }
    }
}
