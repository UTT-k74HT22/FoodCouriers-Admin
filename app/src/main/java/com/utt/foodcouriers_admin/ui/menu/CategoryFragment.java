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
import com.utt.foodcouriers_admin.data.model.Category;
import com.utt.foodcouriers_admin.data.remote.BaseSupabaseClient;
import com.utt.foodcouriers_admin.data.remote.MenuClient;
import com.utt.foodcouriers_admin.ui.menu.adapter.CategoryAdapter;

import java.util.Arrays;

public class CategoryFragment extends Fragment implements CategoryAdapter.OnCategoryClickListener {

    private RecyclerView rvCategories;
    private CategoryAdapter adapter;
    private View emptyState;
    private ExtendedFloatingActionButton fabAdd;
    private MenuClient menuClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_category_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCategories = view.findViewById(R.id.rv_categories);
        emptyState = view.findViewById(R.id.empty_state);
        fabAdd = view.findViewById(R.id.fab_add_category);
        menuClient = MenuClient.getInstance();

        setupRecyclerView();
        loadCategories();

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> {
                // TODO: Open add category form
            });
        }
    }

    private void setupRecyclerView() {
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CategoryAdapter();
        adapter.setOnCategoryClickListener(this);
        rvCategories.setAdapter(adapter);
    }

    private void loadCategories() {
        menuClient.getCategories(new BaseSupabaseClient.ApiCallback<Category[]>() {
            @Override
            public void onSuccess(Category[] result) {
                if (result == null || result.length == 0) {
                    emptyState.setVisibility(View.VISIBLE);
                    rvCategories.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    rvCategories.setVisibility(View.VISIBLE);
                    adapter.setCategories(Arrays.asList(result));
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCategoryClick(Category category) {
        // TODO: Open edit category form
    }

    @Override
    public void onStatusChange(Category category, boolean isActive) {
        // TODO: Update category status in Supabase
    }
}
