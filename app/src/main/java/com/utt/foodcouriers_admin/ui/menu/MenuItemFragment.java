package com.utt.foodcouriers_admin.ui.menu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.MenuItem;
import com.utt.foodcouriers_admin.data.remote.SupabaseClient;
import com.utt.foodcouriers_admin.ui.menu.adapter.MenuItemAdapter;

import java.util.Arrays;

public class MenuItemFragment extends Fragment implements MenuItemAdapter.OnMenuItemClickListener {

    private RecyclerView rvMenuItems;
    private MenuItemAdapter adapter;
    private View emptyState;
    private ExtendedFloatingActionButton fabAdd;
    private SupabaseClient supabaseClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_menu_item_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvMenuItems = view.findViewById(R.id.rv_menu_items);
        emptyState = view.findViewById(R.id.empty_state);
        fabAdd = view.findViewById(R.id.fab_add_menu_item);
        supabaseClient = SupabaseClient.getInstance();

        setupRecyclerView();
        loadMenuItems();

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> {
                // TODO: Open add menu item form
            });
        }
    }

    private void setupRecyclerView() {
        rvMenuItems.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MenuItemAdapter();
        adapter.setOnMenuItemClickListener(this);
        rvMenuItems.setAdapter(adapter);
    }

    private void loadMenuItems() {
        supabaseClient.getMenuItems(null, new SupabaseClient.ApiCallback<MenuItem[]>() {
            @Override
            public void onSuccess(MenuItem[] result) {
                if (result == null || result.length == 0) {
                    emptyState.setVisibility(View.VISIBLE);
                    rvMenuItems.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    rvMenuItems.setVisibility(View.VISIBLE);
                    adapter.setItems(Arrays.asList(result));
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMenuItemClick(MenuItem item) {
        // TODO: Open edit menu item form
    }

    @Override
    public void onAvailabilityChange(MenuItem item, boolean isAvailable) {
        // TODO: Update item availability in Supabase
    }
}
