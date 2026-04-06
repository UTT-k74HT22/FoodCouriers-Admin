package com.utt.foodcouriers_admin.ui.dashboard;

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

public class DashboardFragment extends Fragment {

    private RecyclerView rvRecentOrders;
    private RecyclerView rvTopItems;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvRecentOrders = view.findViewById(R.id.rv_recent_orders);
        rvTopItems = view.findViewById(R.id.rv_top_items);

        setupRecentOrders();
        setupTopItems();
    }

    private void setupRecentOrders() {
        rvRecentOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Set adapter with real data from API
    }

    private void setupTopItems() {
        rvTopItems.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Set adapter with real data from API
    }
}
