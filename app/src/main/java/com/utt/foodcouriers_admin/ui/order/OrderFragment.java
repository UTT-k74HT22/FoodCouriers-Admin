package com.utt.foodcouriers_admin.ui.order;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.data.model.OrderStatus;
import com.utt.foodcouriers_admin.data.model.Shipper;
import com.utt.foodcouriers_admin.data.repository.OrderRepository;
import com.utt.foodcouriers_admin.ui.order.adapter.OrderAdapter;
import com.utt.foodcouriers_admin.utils.ToastBanner;

import java.util.List;

public class OrderFragment extends Fragment implements OrderAdapter.OrderActionListener {

    private static final long AUTO_REFRESH_MS = 15000L;

    private TextInputEditText etSearch;
    private TabLayout tabLayout;
    private RecyclerView rvOrders;
    private View emptyState;
    private View progressBar;
    private TextView tvEmptyTitle;
    private TextView tvEmptyMessage;
    private OrderAdapter adapter;
    private OrderRepository orderRepository;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = this::reloadOrders;

    private String currentQuery = "";
    private OrderStatus currentStatus = OrderStatus.PENDING;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_order_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        orderRepository = OrderRepository.getInstance();
        setupRecycler();
        setupTabs();
        setupSearch();
        reloadOrders();
    }

    @Override
    public void onResume() {
        super.onResume();
        scheduleRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }

    private void initViews(View view) {
        etSearch = view.findViewById(R.id.et_search);
        tabLayout = view.findViewById(R.id.tab_order_status);
        rvOrders = view.findViewById(R.id.rv_orders);
        emptyState = view.findViewById(R.id.empty_state);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmptyTitle = emptyState.findViewById(R.id.tvEmptyTitle);
        tvEmptyMessage = emptyState.findViewById(R.id.tvEmptyMessage);
    }

    private void setupRecycler() {
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrderAdapter();
        adapter.setListener(this);
        rvOrders.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayout.removeAllTabs();
        for (OrderStatus status : OrderStatus.values()) {
            tabLayout.addTab(tabLayout.newTab().setText(status.getLabel()).setTag(status));
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Object tag = tab.getTag();
                if (tag instanceof OrderStatus) {
                    currentStatus = (OrderStatus) tag;
                    reloadOrders();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                reloadOrders();
            }
        });
        TabLayout.Tab firstTab = tabLayout.getTabAt(0);
        if (firstTab != null) {
            firstTab.select();
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString().trim() : "";
                handler.removeCallbacks(refreshRunnable);
                handler.postDelayed(() -> {
                    reloadOrders();
                    scheduleRefresh();
                }, 250L);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void reloadOrders() {
        showLoading(true);
        orderRepository.getOrders(currentStatus, currentQuery, new RepositoryCallback<List<Order>>() {
            @Override
            public void onComplete(BaseResponse<List<Order>> response) {
                showLoading(false);
                List<Order> orders = response.isSuccess() && response.getData() != null ? response.getData() : java.util.Collections.emptyList();
                adapter.submitList(orders);
                boolean isEmpty = orders.isEmpty();
                emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                rvOrders.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                tvEmptyTitle.setText(R.string.order_empty_title);
                tvEmptyMessage.setText(getString(R.string.order_empty_message, currentStatus.getLabel()));
                scheduleRefresh();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void scheduleRefresh() {
        handler.removeCallbacks(refreshRunnable);
        handler.postDelayed(refreshRunnable, AUTO_REFRESH_MS);
    }

    @Override
    public void onOpenDetail(Order order) {
        Intent intent = new Intent(requireContext(), OrderDetailActivity.class);
        intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.getId());
        startActivity(intent);
    }

    @Override
    public void onAccept(Order order) {
        updateStatus(order, OrderStatus.CONFIRMED);
    }

    @Override
    public void onReject(Order order) {
        updateStatus(order, OrderStatus.CANCELLED);
    }

    @Override
    public void onNextStep(Order order) {
        updateStatus(order, order.getOrderStatus().next());
    }

    @Override
    public void onAssignShipper(Order order) {
        orderRepository.getAssignableShippers(new RepositoryCallback<List<Shipper>>() {
            @Override
            public void onComplete(BaseResponse<List<Shipper>> response) {
                List<Shipper> shippers = response.isSuccess() && response.getData() != null ? response.getData() : java.util.Collections.emptyList();
                String[] shipperNames = new String[shippers.size()];
                for (int i = 0; i < shippers.size(); i++) {
                    shipperNames[i] = shippers.get(i).getFullName() + " - " + shippers.get(i).getPhone();
                }
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.order_assign_shipper)
                        .setItems(shipperNames, (dialog, which) -> {
                            orderRepository.assignShipper(order.getId(), shippers.get(which), new RepositoryCallback<Order>() {
                                @Override
                                public void onComplete(BaseResponse<Order> response) {
                                    ToastBanner.showSuccess(getString(R.string.order_assign_shipper_success));
                                    reloadOrders();
                                }
                            });
                        })
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();
            }
        });
    }

    private void updateStatus(Order order, OrderStatus status) {
        if (order.isLocked()) {
            ToastBanner.showWarning(getString(R.string.order_locked_message));
            return;
        }
        orderRepository.updateStatus(order.getId(), status, new RepositoryCallback<Order>() {
            @Override
            public void onComplete(BaseResponse<Order> response) {
                ToastBanner.showSuccess(getString(R.string.order_base_mock_updated));
                reloadOrders();
            }
        });
    }
}
