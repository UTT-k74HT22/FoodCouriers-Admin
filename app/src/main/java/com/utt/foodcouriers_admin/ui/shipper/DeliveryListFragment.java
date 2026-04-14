package com.utt.foodcouriers_admin.ui.shipper;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.data.model.OrderStatus;
import com.utt.foodcouriers_admin.data.repository.DeliveryRepository;
import com.utt.foodcouriers_admin.ui.order.OrderDetailActivity;
import com.utt.foodcouriers_admin.ui.order.adapter.OrderAdapter;
import com.utt.foodcouriers_admin.utils.SessionManager;
import com.utt.foodcouriers_admin.utils.ToastBanner;

import java.util.ArrayList;
import java.util.List;

public class DeliveryListFragment extends Fragment implements OrderAdapter.OrderActionListener {

    private static final String ARG_TYPE = "type";
    public static final int TYPE_AVAILABLE = 0;
    public static final int TYPE_ONGOING = 1;
    public static final int TYPE_HISTORY = 2;

    private int type;
    private RecyclerView recyclerView;
    private View emptyState;
    private View progressBar;
    private OrderAdapter adapter;
    private DeliveryRepository repository;
    private SessionManager sessionManager;

    public static DeliveryListFragment newInstance(int type) {
        DeliveryListFragment fragment = new DeliveryListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            type = getArguments().getInt(ARG_TYPE);
        }
        repository = DeliveryRepository.getInstance();
        sessionManager = SessionManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_order_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        recyclerView = view.findViewById(R.id.rv_orders);
        emptyState = view.findViewById(R.id.empty_state);
        progressBar = view.findViewById(R.id.progress_bar);
        
        // Hide UI elements from shared layout that are not needed for shipper tabs
        View etSearch = view.findViewById(R.id.et_search);
        if (etSearch != null && etSearch.getParent() != null && etSearch.getParent().getParent() instanceof View) {
            ((View) etSearch.getParent().getParent()).setVisibility(View.GONE);
        }
        
        View tabLayout = view.findViewById(R.id.tab_order_status);
        if (tabLayout != null) tabLayout.setVisibility(View.GONE);

        setupRecyclerView();
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrderAdapter();
        adapter.setShipperMode(true);
        adapter.setListener(this);
        recyclerView.setAdapter(adapter);
    }

    private void loadData() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        String userId = sessionManager.getUserId();

        RepositoryCallback<List<Order>> callback = new RepositoryCallback<List<Order>>() {
            @Override
            public void onComplete(BaseResponse<List<Order>> response) {
                if (!isAdded()) return;
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (response.isSuccess() && response.getData() != null) {
                    renderOrders(response.getData());
                } else {
                    renderOrders(new ArrayList<>());
                }
            }
        };

        if (type == TYPE_AVAILABLE) {
            repository.getAvailableOrders(callback);
        } else if (type == TYPE_ONGOING) {
            repository.getActiveDeliveries(userId, callback);
        } else {
            repository.getDeliveryHistory(userId, callback);
        }
    }

    private void renderOrders(List<Order> orders) {
        adapter.submitList(orders);
        if (orders.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onOpenDetail(Order order) {
        Intent intent = new Intent(requireContext(), ShipperOrderDetailActivity.class);
        intent.putExtra(ShipperOrderDetailActivity.EXTRA_ORDER_ID, order.getId());
        startActivity(intent);
    }

    @Override
    public void onAccept(Order order) {
        if (type == TYPE_AVAILABLE) {
            acceptOrder(order);
        }
    }

    @Override
    public void onReject(Order order) {
    }

    @Override
    public void onNextStep(Order order) {
        if (type == TYPE_AVAILABLE) {
            acceptOrder(order);
        } else if (type == TYPE_ONGOING) {
            if (order.getOrderStatus() == OrderStatus.READY_FOR_PICKUP || order.getOrderStatus() == OrderStatus.PREPARING) {
                pickupOrder(order);
            } else if (order.getOrderStatus() == OrderStatus.DELIVERING) {
                completeDelivery(order);
            }
        }
    }

    @Override
    public void onAssignShipper(Order order) {
    }

    @Override
    public void onPickup(Order order) {
        pickupOrder(order);
    }

    @Override
    public void onComplete(Order order) {
        completeDelivery(order);
    }

    private void acceptOrder(Order order) {
        repository.acceptOrder(order.getId(), sessionManager.getUserId(), new RepositoryCallback<Void>() {
            @Override
            public void onComplete(BaseResponse<Void> response) {
                if (!isAdded()) return;
                if (response.isSuccess()) {
                    ToastBanner.showSuccess("Đã nhận đơn hàng!");
                    loadData();
                } else {
                    ToastBanner.showError(response.getMessage());
                }
            }
        });
    }

    private void pickupOrder(Order order) {
        repository.pickupOrder(order.getId(), sessionManager.getUserId(), new RepositoryCallback<Void>() {
            @Override
            public void onComplete(BaseResponse<Void> response) {
                if (!isAdded()) return;
                if (response.isSuccess()) {
                    ToastBanner.showSuccess("Đã lấy hàng thành công!");
                    loadData();
                } else {
                    ToastBanner.showError(response.getMessage());
                }
            }
        });
    }

    private void completeDelivery(Order order) {
        repository.completeOrder(order.getId(), sessionManager.getUserId(), new RepositoryCallback<Void>() {
            @Override
            public void onComplete(BaseResponse<Void> response) {
                if (!isAdded()) return;
                if (response.isSuccess()) {
                    ToastBanner.showSuccess("Đã giao hàng thành công!");
                    loadData();
                } else {
                    ToastBanner.showError(response.getMessage());
                }
            }
        });
    }
}
