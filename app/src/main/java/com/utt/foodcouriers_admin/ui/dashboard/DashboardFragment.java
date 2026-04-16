package com.utt.foodcouriers_admin.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.DashboardStats;
import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.ui.dashboard.adapter.DashboardOrderAdapter;
import com.utt.foodcouriers_admin.ui.dashboard.adapter.DashboardTopItemAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * DashboardFragment - Hiển thị biểu đồ doanh thu thật.
 */
public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;
    private DashboardOrderAdapter orderAdapter;
    private DashboardTopItemAdapter topItemAdapter;
    
    private View cardOrders, cardRevenue, cardProcessing, cardCompleted;
    private TextView tvWelcome, tvDate;
    private RecyclerView rvRecentOrders, rvTopItems;
    
    // Chart Views
    private View[] bars = new View[7];
    private TextView[] labels = new TextView[7];

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupWelcomeHeader();
        setupRecyclerViews();
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        observeViewModel();
        viewModel.loadDashboardData();
    }

    private void initViews(View view) {
        tvWelcome = view.findViewById(R.id.tv_welcome);
        tvDate = view.findViewById(R.id.tv_date);
        cardOrders = view.findViewById(R.id.stat_orders_today);
        cardRevenue = view.findViewById(R.id.stat_revenue);
        cardProcessing = view.findViewById(R.id.stat_processing);
        cardCompleted = view.findViewById(R.id.stat_completed);
        rvRecentOrders = view.findViewById(R.id.rv_recent_orders);
        rvTopItems = view.findViewById(R.id.rv_top_items);

        // Map Chart Views
        for (int i = 0; i < 7; i++) {
            int barId = getResources().getIdentifier("bar_day_" + (i + 1), "id", getContext().getPackageName());
            int labelId = getResources().getIdentifier("label_day_" + (i + 1), "id", getContext().getPackageName());
            bars[i] = view.findViewById(barId);
            labels[i] = view.findViewById(labelId);
        }
    }

    private void observeViewModel() {
        viewModel.getStats().observe(getViewLifecycleOwner(), this::displayStats);
        viewModel.getRecentOrders().observe(getViewLifecycleOwner(), orders -> {
            if (orders != null) orderAdapter.setOrders(orders);
        });
        viewModel.getTopItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null) topItemAdapter.setItems(items);
        });
        
        // Lắng nghe dữ liệu biểu đồ
        viewModel.getWeeklyRevenue().observe(getViewLifecycleOwner(), this::updateRevenueChart);
    }

    private void updateRevenueChart(double[] data) {
        if (data == null || data.length < 7) return;

        // 1. Tìm doanh thu lớn nhất để tính tỉ lệ (Scale)
        double maxRevenue = 0;
        for (double d : data) if (d > maxRevenue) maxRevenue = d;
        if (maxRevenue == 0) maxRevenue = 1; // Tránh chia cho 0

        // 2. Cập nhật độ cao các thanh (Max height = 120dp trong XML)
        float maxHeightPx = 120 * getResources().getDisplayMetrics().density;
        for (int i = 0; i < 7; i++) {
            float ratio = (float) (data[i] / maxRevenue);
            int height = (int) (ratio * maxHeightPx);
            if (height < 10) height = 10; // Chiều cao tối thiểu để vẫn thấy thanh
            
            ViewGroup.LayoutParams params = bars[i].getLayoutParams();
            params.height = height;
            bars[i].setLayoutParams(params);
        }

        // 3. Cập nhật nhãn Thứ (T2, T3...)
        SimpleDateFormat sdf = new SimpleDateFormat("EE", new Locale("vi", "VN"));
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            if (i < 6) { // Ngày cuối cùng (i=6) đã để là "HN" trong XML
                labels[i].setText(sdf.format(cal.getTime()).replace("Th ", "T"));
            }
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
    }

    private void displayStats(DashboardStats stats) {
        updateStatCard(cardOrders, R.drawable.ic_orders, "Tổng đơn", String.valueOf(stats.getTotalOrders()), stats.getOrderTrend(), R.color.primary);
        updateStatCard(cardRevenue, R.drawable.ic_revenue, "Doanh thu", formatCompactCurrency(stats.getTotalRevenue()), stats.getRevenueTrend(), R.color.success);
        updateStatCard(cardProcessing, R.drawable.ic_pending, "Đang xử lý", String.valueOf(stats.getPendingOrders()), stats.getPendingTrend(), R.color.warning);
        updateStatCard(cardCompleted, R.drawable.ic_completed, "Hoàn thành", String.valueOf(stats.getCompletedOrders()), stats.getCompletedTrend(), R.color.info);
    }

    private void updateStatCard(View card, int iconRes, String label, String value, double trend, int colorRes) {
        if (getContext() == null || card == null) return;
        ImageView icon = card.findViewById(R.id.stat_icon);
        TextView tvLabel = card.findViewById(R.id.stat_label);
        TextView tvValue = card.findViewById(R.id.stat_value);
        View layoutTrend = card.findViewById(R.id.layout_trend);
        ImageView imgTrend = card.findViewById(R.id.img_trend);
        TextView tvTrendPercent = card.findViewById(R.id.tv_trend_percent);

        int color = ContextCompat.getColor(getContext(), colorRes);
        icon.setImageResource(iconRes);
        icon.setColorFilter(color);
        tvLabel.setText(label);
        tvValue.setText(value);

        if (trend == 0) {
            layoutTrend.setVisibility(View.GONE);
        } else {
            layoutTrend.setVisibility(View.VISIBLE);
            boolean isPositive = trend > 0;
            int trendColor = ContextCompat.getColor(getContext(), isPositive ? R.color.success : R.color.error);
            tvTrendPercent.setText(String.format(Locale.US, "%s%.1f%%", isPositive ? "+" : "", trend));
            tvTrendPercent.setTextColor(trendColor);
            imgTrend.setImageResource(isPositive ? R.drawable.admin_badge_success : R.drawable.admin_badge_cancel);
            imgTrend.setColorFilter(trendColor);
        }
    }

    private String formatCompactCurrency(double amount) {
        if (amount >= 1000000) return String.format(Locale.US, "%.1fM", amount / 1000000.0);
        if (amount >= 1000) return String.format(Locale.US, "%.0fk", amount / 1000.0);
        return String.valueOf((int)amount);
    }

    private void setupWelcomeHeader() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM, yyyy", new Locale("vi", "VN"));
        tvDate.setText(sdf.format(new Date()));
        tvWelcome.setText("Chào buổi sáng, Admin!");
    }

    private void setupRecyclerViews() {
        rvRecentOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        orderAdapter = new DashboardOrderAdapter();
        rvRecentOrders.setAdapter(orderAdapter);
        rvTopItems.setLayoutManager(new LinearLayoutManager(getContext()));
        topItemAdapter = new DashboardTopItemAdapter();
        rvTopItems.setAdapter(topItemAdapter);
    }

    @Override public void onResume() { super.onResume(); if (viewModel != null) viewModel.startAutoRefresh(); }
    @Override public void onPause() { super.onPause(); if (viewModel != null) viewModel.stopAutoRefresh(); }
}
