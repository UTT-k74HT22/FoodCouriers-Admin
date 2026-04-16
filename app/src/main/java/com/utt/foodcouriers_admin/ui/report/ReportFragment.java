package com.utt.foodcouriers_admin.ui.report;

import android.app.DatePickerDialog;
import android.os.Bundle;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.ui.dashboard.adapter.DashboardTopItemAdapter;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import androidx.core.content.ContextCompat;

public class ReportFragment extends Fragment {

    private ReportViewModel viewModel;
    private TextInputEditText etStartDate, etEndDate;
    private MaterialButton btnApply;
    private View cardRevenue, cardTotalOrders, cardSuccessful, cardCancelled;
    private RecyclerView rvTopRestaurants, rvTopItems;
    private TopRestaurantAdapter restaurantAdapter;
    private DashboardTopItemAdapter itemAdapter;

    private final Calendar calendarStart = Calendar.getInstance();
    private final Calendar calendarEnd = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupDatePicker();
        setupStatsCards();
        setupRecyclerViews();

        viewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        observeViewModel();

        // Default: Today
        updateDateFields();
        loadReport();

        btnApply.setOnClickListener(v -> loadReport());
    }

    private void initViews(View view) {
        etStartDate = view.findViewById(R.id.et_start_date);
        etEndDate = view.findViewById(R.id.et_end_date);
        btnApply = view.findViewById(R.id.btn_apply_date);

        cardRevenue = view.findViewById(R.id.stat_total_revenue);
        cardTotalOrders = view.findViewById(R.id.stat_total_orders);
        cardSuccessful = view.findViewById(R.id.stat_successful_orders);
        cardCancelled = view.findViewById(R.id.stat_cancelled_orders);
        
        rvTopRestaurants = view.findViewById(R.id.rv_top_restaurants);
        rvTopItems = view.findViewById(R.id.rv_top_items);
    }

    private void setupDatePicker() {
        etStartDate.setOnClickListener(v -> showDatePicker(true));
        etEndDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isStart) {
        Calendar cal = isStart ? calendarStart : calendarEnd;
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateFields();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateFields() {
        etStartDate.setText(dateFormat.format(calendarStart.getTime()));
        etEndDate.setText(dateFormat.format(calendarEnd.getTime()));
    }

    private void setupStatsCards() {
        if (getContext() == null) return;
        updateStatCard(cardRevenue, R.drawable.ic_revenue, "Tổng doanh thu", "0đ", R.color.success);
        updateStatCard(cardTotalOrders, R.drawable.ic_orders, "Tổng đơn hàng", "0", R.color.primary);
        updateStatCard(cardSuccessful, R.drawable.ic_completed, "Đơn thành công", "0", R.color.secondary);
        updateStatCard(cardCancelled, R.drawable.ic_orders, "Đơn bị hủy", "0", R.color.error);
    }

    private void updateStatCard(View card, int iconRes, String label, String value, int colorRes) {
        if (getContext() == null || card == null) return;
        ImageView icon = card.findViewById(R.id.stat_icon);
        TextView tvLabel = card.findViewById(R.id.stat_label);
        TextView tvValue = card.findViewById(R.id.stat_value);

        int color = ContextCompat.getColor(getContext(), colorRes);
        if (icon != null) {
            icon.setImageResource(iconRes);
            icon.setColorFilter(color);
        }
        if (tvLabel != null) tvLabel.setText(label);
        if (tvValue != null) {
            tvValue.setText(value);
            tvValue.setTextColor(color);
        }
    }

    private void setupRecyclerViews() {
        rvTopRestaurants.setLayoutManager(new LinearLayoutManager(getContext()));
        restaurantAdapter = new TopRestaurantAdapter();
        rvTopRestaurants.setAdapter(restaurantAdapter);

        rvTopItems.setLayoutManager(new LinearLayoutManager(getContext()));
        itemAdapter = new DashboardTopItemAdapter();
        rvTopItems.setAdapter(itemAdapter);
    }

    private void loadReport() {
        if (etStartDate.getText() != null && etEndDate.getText() != null) {
            viewModel.loadReport(etStartDate.getText().toString(), etEndDate.getText().toString());
        }
    }

    private void observeViewModel() {
        viewModel.getOrders().observe(getViewLifecycleOwner(), orders -> {
            if (orders != null) {
                calculateAndDisplaySummary(orders);
            }
        });

        viewModel.getTopRestaurants().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                restaurantAdapter.setStats(stats);
            }
        });

        viewModel.getTopItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                itemAdapter.setItems(items);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateAndDisplaySummary(List<Order> orders) {
        double totalRevenue = 0;
        int totalOrdersCount = orders.size();
        int completedOrders = 0;
        int cancelledOrders = 0;

        for (Order o : orders) {
            String status = o.getStatus();
            if ("delivered".equals(status)) {
                completedOrders++;
                totalRevenue += o.getTotal();
            } else if ("cancelled".equals(status)) {
                cancelledOrders++;
            }
        }

        updateStatValue(cardRevenue, formatCurrency(totalRevenue));
        updateStatValue(cardTotalOrders, String.valueOf(totalOrdersCount));
        updateStatValue(cardSuccessful, String.valueOf(completedOrders));
        updateStatValue(cardCancelled, String.valueOf(cancelledOrders));
    }

    private void updateStatValue(View card, String value) {
        if (card == null) return;
        TextView tvValue = card.findViewById(R.id.stat_value);
        if (tvValue != null) tvValue.setText(value);
    }

    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }
}
