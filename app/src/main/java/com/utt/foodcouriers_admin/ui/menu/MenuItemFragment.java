package com.utt.foodcouriers_admin.ui.menu;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.utt.foodcouriers_admin.utils.ToastBanner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Category;
import com.utt.foodcouriers_admin.data.model.MenuItem;
import com.utt.foodcouriers_admin.data.model.Restaurant;
import com.utt.foodcouriers_admin.data.model.User;
import com.utt.foodcouriers_admin.data.remote.BaseSupabaseClient;
import com.utt.foodcouriers_admin.data.repository.MenuRepository;
import com.utt.foodcouriers_admin.ui.menu.adapter.MenuItemAdapter;
import com.utt.foodcouriers_admin.utils.SessionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MenuItemFragment extends Fragment implements MenuItemAdapter.OnMenuItemClickListener {

    private static final String ARG_RESTAURANT = "arg_restaurant";

    public static MenuItemFragment newInstance(@Nullable Restaurant restaurant) {
        MenuItemFragment fragment = new MenuItemFragment();
        Bundle bundle = new Bundle();
        if (restaurant != null) {
            bundle.putSerializable(ARG_RESTAURANT, restaurant);
        }
        fragment.setArguments(bundle);
        return fragment;
    }

    private RecyclerView rvMenuItems;
    private MenuItemAdapter adapter;
    private View emptyState;
    private ExtendedFloatingActionButton fabAdd;
    private TextInputEditText etSearch, etFilterRestaurant, etFilterCategory;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MaterialToolbar toolbar;

    private MenuRepository menuRepository;
    private List<MenuItem> allMenuItems = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private List<Restaurant> restaurants = new ArrayList<>();
    private Category selectedCategory;
    private Restaurant selectedRestaurant;
    private User currentUser;
    
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_menu_item_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            selectedRestaurant = (Restaurant) getArguments().getSerializable(ARG_RESTAURANT);
        }

        menuRepository = MenuRepository.getInstance();
        currentUser = SessionManager.getInstance(requireContext()).getCurrentUser();

        initViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        setupFilters();
        setupSearch();
        
        loadInitialData();
    }

    private void initViews(View view) {
        rvMenuItems = view.findViewById(R.id.rv_menu_items);
        emptyState = view.findViewById(R.id.empty_state);
        fabAdd = view.findViewById(R.id.fab_add_menu_item);
        etSearch = view.findViewById(R.id.et_search);
        etFilterRestaurant = view.findViewById(R.id.et_filter_restaurant);
        etFilterCategory = view.findViewById(R.id.et_filter_category);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        toolbar = view.findViewById(R.id.toolbar);

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), MenuItemFormActivity.class);
                if (selectedRestaurant != null) {
                    intent.putExtra(MenuItemFormActivity.EXTRA_RESTAURANT_ID, selectedRestaurant.getId());
                }
                startActivity(intent);
            });
        }
    }


    private void setupRecyclerView() {
        rvMenuItems.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MenuItemAdapter();
        adapter.setOnMenuItemClickListener(this);
        rvMenuItems.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> loadMenuItems(false));
            swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        }
    }

    private void setupSearch() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    triggerSearch();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void triggerSearch() {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        searchRunnable = () -> loadMenuItems(false);
        searchHandler.postDelayed(searchRunnable, 350);
    }

    private void setupFilters() {
        setupRestaurantFilter();
        setupCategoryFilter();
    }

    private void loadInitialData() {
        // Load Categories
        menuRepository.getCategories(new RepositoryCallback<List<Category>>() {
            @Override
            public void onComplete(BaseResponse<List<Category>> response) {
                if (!isAdded()) return;
                
                if (response.isSuccess() && response.getData() != null) {
                    categories.clear();
                    categories.addAll(response.getData());
                    adapter.setCategories(categories);
                }
            }
        });

        // Load Restaurants
        BaseSupabaseClient.ApiCallback<Restaurant[]> restaurantCallback = new BaseSupabaseClient.ApiCallback<Restaurant[]>() {
            @Override
            public void onSuccess(Restaurant[] result) {
                if (isAdded()) handleRestaurantsLoaded(result);
            }

            @Override
            public void onError(String error) {
                if (isAdded()) ToastBanner.showError("Lỗi tải nhà hàng: " + error);
            }
        };

        if (currentUser.isAdmin()) {
            menuRepository.getRestaurants(restaurantCallback);
        } else {
            menuRepository.getRestaurantsForStaff(currentUser.getId(), restaurantCallback);
        }
    }
    
    private void handleRestaurantsLoaded(Restaurant[] result) {
        restaurants.clear();
        if (result != null) {
            restaurants.addAll(Arrays.asList(result));
            
            // Auto-select restaurant if passed from arguments
            if (selectedRestaurant != null) {
                for (Restaurant r : restaurants) {
                    if (r.getId() != null && r.getId().equals(selectedRestaurant.getId())) {
                        selectedRestaurant = r;
                        etFilterRestaurant.setText(selectedRestaurant.getName());
                        break;
                    }
                }
            } else if (!currentUser.isAdmin() || restaurants.size() == 1) {
                if (!restaurants.isEmpty()) {
                    selectedRestaurant = restaurants.get(0);
                    etFilterRestaurant.setText(selectedRestaurant.getName());
                }
            }
            loadMenuItems(true);
        }
    }

    private void setupRestaurantFilter() {
        if (etFilterRestaurant == null) return;
        etFilterRestaurant.setFocusable(false);
        etFilterRestaurant.setOnClickListener(v -> showRestaurantPicker());
        
        if (!currentUser.isAdmin() && !restaurants.isEmpty() && restaurants.size() == 1) {
            etFilterRestaurant.setText(restaurants.get(0).getName());
            etFilterRestaurant.setEnabled(false);
        }
    }

    private void setupCategoryFilter() {
        if (etFilterCategory == null) return;
        etFilterCategory.setFocusable(false);
        etFilterCategory.setOnClickListener(v -> showCategoryPicker());
    }

    private void showRestaurantPicker() {
        if (restaurants.isEmpty()) return;

        String[] names = new String[restaurants.size() + 1];
        names[0] = "Tất cả nhà hàng";
        for (int i = 0; i < restaurants.size(); i++) {
            names[i+1] = restaurants.get(i).getName();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn nhà hàng")
                .setItems(names, (dialog, which) -> {
                    if (which == 0) {
                        selectedRestaurant = null;
                        etFilterRestaurant.setText("Tất cả nhà hàng");
                    } else {
                        selectedRestaurant = restaurants.get(which - 1);
                        etFilterRestaurant.setText(selectedRestaurant.getName());
                    }
                    loadMenuItems(true);
                })
                .show();
    }

    private void showCategoryPicker() {
        if (categories.isEmpty()) return;

        String[] names = new String[categories.size() + 1];
        names[0] = "Tất cả danh mục";
        for (int i = 0; i < categories.size(); i++) {
            names[i+1] = categories.get(i).getName();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn danh mục")
                .setItems(names, (dialog, which) -> {
                    if (which == 0) {
                        selectedCategory = null;
                        etFilterCategory.setText("Tất cả danh mục");
                    } else {
                        selectedCategory = categories.get(which - 1);
                        etFilterCategory.setText(selectedCategory.getName());
                    }
                    loadMenuItems(true);
                })
                .show();
    }

    private void loadMenuItems(boolean showLoading) {
        if (showLoading && swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        
        String restaurantId = (selectedRestaurant != null) ? selectedRestaurant.getId() : null;
        String categoryId = (selectedCategory != null) ? selectedCategory.getId() : null;
        String searchTerm = etSearch != null && etSearch.getText() != null ? etSearch.getText().toString().trim() : "";

        // Staff default to their first restaurant
        if (!currentUser.isAdmin() && restaurantId == null && !restaurants.isEmpty()) {
             restaurantId = restaurants.get(0).getId();
        }

        menuRepository.getMenuItems(restaurantId, categoryId, new RepositoryCallback<List<MenuItem>>() {
            @Override
            public void onComplete(BaseResponse<List<MenuItem>> response) {
                if (!isAdded()) return;
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

                if (!response.isSuccess()) {
                    ToastBanner.showError("Lỗi tải món ăn: " + response.getMessage());
                    renderMenuItems(new ArrayList<>());
                    return;
                }
                
                allMenuItems.clear();
                List<MenuItem> result = response.getData();
                if (result != null) {
                    allMenuItems.addAll(result);
                    
                    List<MenuItem> displayItems = new ArrayList<>();
                    if (searchTerm.isEmpty()) {
                        displayItems.addAll(allMenuItems);
                    } else {
                        String lowerSearch = searchTerm.toLowerCase();
                        for (MenuItem item : allMenuItems) {
                            if (item.getName() != null && item.getName().toLowerCase().contains(lowerSearch)) {
                                displayItems.add(item);
                            }
                        }
                    }
                    renderMenuItems(displayItems);
                } else {
                    renderMenuItems(new ArrayList<>());
                }
            }
        });
    }

    private void renderMenuItems(List<MenuItem> items) {
        adapter.setItems(items);
        boolean isEmpty = items.isEmpty();
        if (emptyState != null) emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (rvMenuItems != null) rvMenuItems.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onMenuItemClick(MenuItem item) {
        Intent intent = new Intent(getContext(), MenuItemFormActivity.class);
        intent.putExtra(MenuItemFormActivity.EXTRA_MENU_ITEM, item);
        startActivity(intent);
    }

    @Override
    public void onAvailabilityChange(MenuItem item, boolean isAvailable) {
        String msg = isAvailable ? "Đã hiển thị món ăn" : "Đã ẩn món ăn";
        ToastBanner.showSuccess(msg);
        item.setAvailable(isAvailable); 
        menuRepository.updateAvailability(item.getId(), isAvailable, new RepositoryCallback<MenuItem>() {
            @Override
            public void onComplete(BaseResponse<MenuItem> response) {
                if (!isAdded()) return;

                if (response.isSuccess()) {
                    ToastBanner.showSuccess("Trạng thái đã được cập nhật");
                    loadMenuItems(false);
                } else {
                    ToastBanner.showError("Lỗi cập nhật trạng thái: " + response.getMessage());
                    loadMenuItems(false);
                }
            }
        });
    }
    
    @Override
    public void onDeleteClick(MenuItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa món ăn")
                .setMessage("Bạn có chắc chắn muốn xóa món '" + item.getName() + "' không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    menuRepository.delete(item.getId(), new RepositoryCallback<Void>() {
                        @Override
                        public void onComplete(BaseResponse<Void> response) {
                            if (!isAdded()) return;
                            if (response.isSuccess()) {
                                ToastBanner.showSuccess("Món ăn đã được xóa");
                                loadMenuItems(true);
                            } else {
                                ToastBanner.showError("Lỗi xóa món ăn: " + response.getMessage());
                            }
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMenuItems(false);
    }
}
