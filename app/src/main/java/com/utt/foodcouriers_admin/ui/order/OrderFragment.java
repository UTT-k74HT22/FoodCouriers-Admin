package com.utt.foodcouriers_admin.ui.order;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.data.model.OrderStatus;
import com.utt.foodcouriers_admin.data.model.Restaurant;
import com.utt.foodcouriers_admin.data.model.Shipper;
import com.utt.foodcouriers_admin.data.model.User;
import com.utt.foodcouriers_admin.data.realtime.OrderRealtimeManager;
import com.utt.foodcouriers_admin.data.repository.OrderRepository;
import com.utt.foodcouriers_admin.ui.order.adapter.OrderAdapter;
import com.utt.foodcouriers_admin.utils.SessionManager;
import com.utt.foodcouriers_admin.utils.ToastBanner;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

/**
 * OrderFragment: Màn hình danh sách đơn hàng.
 * Fragment này đóng vai trò là "View" trong mô hình MVVM.
 * Nó chỉ lo việc hiển thị và phản hồi các sự kiện từ người dùng.
 */
public class OrderFragment extends Fragment implements OrderAdapter.OrderActionListener {

    private OrderViewModel viewModel;
    private OrderAdapter adapter;
    private SessionManager sessionManager;
    private boolean isShipper = false;
    // Các thành phần UI
    private TextInputEditText etSearch;
    private TextInputLayout tilRestaurantFilter;
    private AutoCompleteTextView acRestaurantFilter;
    private TabLayout tabLayout;
    private RecyclerView rvOrders;
    private View emptyState, progressBar;
    private TextView tvEmptyTitle, tvEmptyMessage;
    private String currentQuery = "";
    private String currentRestaurantId = "";
    private OrderStatus currentStatus = OrderStatus.PENDING;
    private boolean isViewingAvailable = false;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private OrderRealtimeManager realtimeManager;
    private OrderRealtimeManager.OrderRealtimeCallback realtimeCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Nạp layout XML cho Fragment
        return inflater.inflate(R.layout.activity_order_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        sessionManager = com.utt.foodcouriers_admin.utils.SessionManager.getInstance(requireContext());
        com.utt.foodcouriers_admin.data.model.User currentUser = sessionManager.getCurrentUser();
        isShipper = currentUser != null && "shipper".equalsIgnoreCase(currentUser.getRole());

        // 1. Khởi tạo ViewModel (Sử dụng ViewModelProvider để ViewModel sống sót khi quay màn hình)
        viewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        
        initViews(view);
        setupRecycler();
        setupTabs();
        setupSearch();
        setupRestaurantFilter();
        observeViewModel();
        
        if (!isShipper) {
            viewModel.fetchRestaurants();
        }
        // 3. Khởi tạo realtime subscription
        initRealtime();

        // 4. Gọi dữ liệu lần đầu
        reloadOrders();
    }
    // ánh xạ các thành phần UI từ layout
    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            reloadOrders();
        }
    }

    private void initViews(View view) {
        etSearch = view.findViewById(R.id.et_search);
        tilRestaurantFilter = view.findViewById(R.id.til_restaurant_filter);
        acRestaurantFilter = view.findViewById(R.id.ac_restaurant_filter);
        tabLayout = view.findViewById(R.id.tab_order_status);
        rvOrders = view.findViewById(R.id.rv_orders);
        emptyState = view.findViewById(R.id.empty_state);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmptyTitle = emptyState.findViewById(R.id.tvEmptyTitle);
        tvEmptyMessage = emptyState.findViewById(R.id.tvEmptyMessage);

        if (!isShipper) {
            tilRestaurantFilter.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Quan sát (Observe) các biến LiveData trong ViewModel.
     * Đây là cầu nối giúp UI tự động cập nhật khi dữ liệu thay đổi.
     */
    private void observeViewModel() {
        // Quan sát danh sách đơn hàng
        viewModel.orders.observe(getViewLifecycleOwner(), orders -> {
            adapter.submitList(orders);
            boolean isEmpty = orders == null || orders.isEmpty();
            emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            rvOrders.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            
            if (isEmpty) {
                tvEmptyTitle.setText(R.string.order_empty_title);
                String msg = isViewingAvailable ? "Không có đơn hàng mới nào quanh đây" : getString(R.string.order_empty_message, currentStatus.getLabel());
                tvEmptyMessage.setText(msg);
            }
        });

        // Quan sát danh sách nhà hàng
        viewModel.restaurants.observe(getViewLifecycleOwner(), restaurants -> {
            if (restaurants != null) {
                List<String> restaurantNames = new ArrayList<>();
                restaurantNames.add("Tất cả nhà hàng");
                for (Restaurant r : restaurants) {
                    restaurantNames.add(r.getName());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, restaurantNames);
                acRestaurantFilter.setAdapter(adapter);
                acRestaurantFilter.setText(restaurantNames.get(0), false);
            }
        });

        // Quan sát trạng thái đang tải (Loading)
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        viewModel.errorMessage.observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                ToastBanner.showError(message);
            } else {
                reloadOrders();
            }
        });
    }

    private void setupRestaurantFilter() {
        acRestaurantFilter.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                currentRestaurantId = "";
            } else {
                List<Restaurant> restaurants = viewModel.restaurants.getValue();
                if (restaurants != null && position <= restaurants.size()) {
                    currentRestaurantId = restaurants.get(position - 1).getId();
                }
            }
            reloadOrders();
        });
    }

    private void setupRecycler() {
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrderAdapter();
        adapter.setShipperMode(isShipper);
        adapter.setListener(this); // Fragment đóng vai trò xử lý các nút bấm trên mỗi Item
        rvOrders.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayout.removeAllTabs();
        
        if (isShipper) {
            tabLayout.addTab(tabLayout.newTab().setText("Đơn hàng mới").setTag("AVAILABLE"));
            tabLayout.addTab(tabLayout.newTab().setText("Đang giao").setTag("ACTIVE"));
            tabLayout.addTab(tabLayout.newTab().setText("Đã hoàn thành").setTag("HISTORY"));
            isViewingAvailable = true;
        } else {
            for (OrderStatus status : OrderStatus.values()) {
                if (status == OrderStatus.ASSIGNED) continue;
                tabLayout.addTab(tabLayout.newTab().setText(status.getLabel()).setTag(status));
            }
            currentStatus = OrderStatus.PENDING;
            isViewingAvailable = false;
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Object tag = tab.getTag();
                if (tag instanceof OrderStatus) {
                    currentStatus = (OrderStatus) tag;
                    isViewingAvailable = false;
                    reloadOrders();
                } else if ("AVAILABLE".equals(tag)) {
                    isViewingAvailable = true;
                    reloadOrders();
                } else if ("ACTIVE".equals(tag)) {
                    String shipperId = sessionManager.getCurrentUser().getId();
                    viewModel.fetchActiveDeliveries(shipperId);
                } else if ("HISTORY".equals(tag)) {
                    String shipperId = sessionManager.getCurrentUser().getId();
                    viewModel.fetchDeliveryHistory(shipperId);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) { reloadOrders(); }
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString().trim() : "";
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                reloadOrders();
                return true;
            }
            return false;
        });
    }

    private void reloadOrders() {
        viewModel.setCurrentFilter(currentStatus, currentQuery, isShipper ? sessionManager.getCurrentUser().getId() : null, currentRestaurantId);
        if (isViewingAvailable) {
            viewModel.fetchAvailableOrders();
        } else {
            String shipperId = isShipper ? sessionManager.getCurrentUser().getId() : null;
            viewModel.fetchOrders(currentStatus, currentQuery, shipperId, currentRestaurantId);
        }
    }

    @Override
    public void onOpenDetail(Order order) {
        Intent intent = new Intent(requireContext(), OrderDetailActivity.class);
        intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.getId());
        startActivity(intent);
    }
    @Override
    public void onAccept(Order order) {
        if (isShipper) {
            viewModel.acceptOrder(order.getId(), sessionManager.getCurrentUser().getId());
        } else {
            viewModel.updateStatus(order.getId(), OrderStatus.CONFIRMED);
        }
    }

    @Override
    public void onReject(Order order) {
        viewModel.updateStatus(order.getId(), OrderStatus.CANCELLED);
    }

    @Override
    public void onNextStep(Order order) {
        viewModel.updateStatus(order.getId(), order.getOrderStatus().next());
    }

    @Override
    public void onPickup(Order order) {
        viewModel.pickupOrder(order.getId(), sessionManager.getCurrentUser().getId());
    }

    @Override
    public void onComplete(Order order) {
        viewModel.completeOrder(order.getId(), sessionManager.getCurrentUser().getId());
    }

    @Override
    public void onAssignShipper(Order order) {
        String restaurantId = order.getRestaurantId();
        OrderRepository.getInstance().getAssignableShippers(restaurantId, new RepositoryCallback<List<Shipper>>() {
            @Override
            public void onComplete(BaseResponse<List<Shipper>> response) {
                if (response.isSuccess() && response.getData() != null) {
                    showShipperSelectionDialog(order, response.getData());
                }
            }
        });
    }

    private void showShipperSelectionDialog(Order order, List<Shipper> shippers) {
        String[] names = new String[shippers.size()];
        for (int i = 0; i < shippers.size(); i++) {
            names[i] = shippers.get(i).getFullName() + " (" + shippers.get(i).getPhone() + ")";
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.order_assign_shipper)
                .setItems(names, (dialog, which) -> {
                    viewModel.assignShipper(order.getId(), shippers.get(which));
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    // ========== Realtime Integration ==========

    private void initRealtime() {
        realtimeManager = OrderRealtimeManager.getInstance();

        // Lưu current filter vào ViewModel
        viewModel.setShipperMode(isShipper);
        viewModel.setCurrentFilter(currentStatus, currentQuery, isShipper ? sessionManager.getCurrentUser().getId() : null, currentRestaurantId);

        // Subscribe
        realtimeCallback = new OrderRealtimeManager.OrderRealtimeCallback() {
            @Override
            public void onNewOrder(JsonObject order) {
                if (getActivity() == null) return;
                String orderCode = order.has("order_code") ? order.get("order_code").getAsString() : "mới";
                ToastBanner.showInfo("Đơn hàng mới: " + orderCode);
                viewModel.onNewOrder(order);
            }

            @Override
            public void onOrderUpdated(JsonObject newOrder, JsonObject oldOrder) {
                if (getActivity() == null) return;
                viewModel.onOrderUpdated(newOrder, oldOrder);
            }

            @Override
            public void onOrderDeleted(JsonObject oldOrder) {
                if (getActivity() == null) return;
                String orderCode = oldOrder.has("order_code") ? oldOrder.get("order_code").getAsString() : "";
                ToastBanner.showInfo("Đơn hàng đã bị xóa: " + orderCode);
                viewModel.onOrderDeleted(oldOrder);
            }

            @Override
            public void onRealtimeConnected() {
                Log.d("OrderFragment", "Realtime connected");
            }

            @Override
            public void onRealtimeDisconnected() {
                Log.w("OrderFragment", "Realtime disconnected");
            }

            @Override
            public void onRealtimeError(String error) {
                Log.e("OrderFragment", "Realtime error: " + error);
            }
        };
        realtimeManager.subscribe(realtimeCallback);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hủy các callback của Handler để tránh rò rỉ bộ nhớ
        searchHandler.removeCallbacksAndMessages(null);

        // Unsubscribe realtime
        if (realtimeManager != null && realtimeCallback != null) {
            realtimeManager.unsubscribe(realtimeCallback);
            realtimeCallback = null;
        }
    }
}
