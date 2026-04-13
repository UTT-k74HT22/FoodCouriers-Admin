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
import androidx.lifecycle.ViewModelProvider;
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

/**
 * OrderFragment: Màn hình danh sách đơn hàng.
 * Fragment này đóng vai trò là "View" trong mô hình MVVM.
 * Nó chỉ lo việc hiển thị và phản hồi các sự kiện từ người dùng.
 */
public class OrderFragment extends Fragment implements OrderAdapter.OrderActionListener {

    private OrderViewModel viewModel;
    private OrderAdapter adapter;
    private com.utt.foodcouriers_admin.utils.SessionManager sessionManager;
    private boolean isShipper = false;

    // Các thành phần UI
    private TextInputEditText etSearch;
    private TabLayout tabLayout;
    private RecyclerView rvOrders;
    private View emptyState, progressBar;
    private TextView tvEmptyTitle, tvEmptyMessage;

    // Trạng thái cục bộ để quản lý việc tìm kiếm và lọc
    private String currentQuery = "";
    private OrderStatus currentStatus = OrderStatus.PENDING;
    private boolean isViewingAvailable = false;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());

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
        
        // 2. "Đăng ký" lắng nghe sự thay đổi dữ liệu từ ViewModel
        observeViewModel();
        
        // 3. Gọi dữ liệu lần đầu
        reloadOrders();
    }
    // ánh xạ các thành phần UI từ layout
    private void initViews(View view) {
        etSearch = view.findViewById(R.id.et_search);
        tabLayout = view.findViewById(R.id.tab_order_status);
        rvOrders = view.findViewById(R.id.rv_orders);
        emptyState = view.findViewById(R.id.empty_state);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmptyTitle = emptyState.findViewById(R.id.tvEmptyTitle);
        tvEmptyMessage = emptyState.findViewById(R.id.tvEmptyMessage);
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

        // Quan sát trạng thái đang tải (Loading)
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Quan sát thông báo lỗi/thành công
        viewModel.errorMessage.observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                ToastBanner.showError(message);
            } else {
                // message == null là tín hiệu một thao tác update thành công
                reloadOrders();
            }
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
            // Shippers only care about specific statuses
            tabLayout.addTab(tabLayout.newTab().setText("Đơn hàng mới").setTag("AVAILABLE"));
            tabLayout.addTab(tabLayout.newTab().setText("Đang giao").setTag(OrderStatus.DELIVERING));
            tabLayout.addTab(tabLayout.newTab().setText("Đã hoàn thành").setTag(OrderStatus.DELIVERED));
            isViewingAvailable = true;
        } else {
            for (OrderStatus status : OrderStatus.values()) {
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
                // Cơ chế Debounce: Đợi 300ms sau khi người dùng ngừng gõ mới gọi API
                searchHandler.removeCallbacksAndMessages(null);
                searchHandler.postDelayed(() -> reloadOrders(), 300L);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void reloadOrders() {
        if (isViewingAvailable) {
            viewModel.fetchAvailableOrders();
        } else {
            String shipperId = isShipper ? sessionManager.getCurrentUser().getId() : null;
            viewModel.fetchOrders(currentStatus, currentQuery, shipperId);
        }
    }

    // --- Triển khai các hành động từ giao diện (OrderActionListener) ---

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
        // Mở Dialog chọn Shipper
        OrderRepository.getInstance().getAssignableShippers(new RepositoryCallback<List<Shipper>>() {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hủy các callback của Handler để tránh rò rỉ bộ nhớ
        searchHandler.removeCallbacksAndMessages(null);
    }
}
