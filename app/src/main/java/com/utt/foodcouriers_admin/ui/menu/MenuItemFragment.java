package com.utt.foodcouriers_admin.ui.menu;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.utt.foodcouriers_admin.R;
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

    private RecyclerView rvMenuItems;
    private MenuItemAdapter adapter;
    private View emptyState;
    private ExtendedFloatingActionButton fabAdd;
    private TextInputEditText etSearch, etFilterRestaurant, etFilterCategory;

    private MenuRepository menuRepository;
    private List<MenuItem> allMenuItems = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private List<Restaurant> restaurants = new ArrayList<>();
    private Category selectedCategory;
    private Restaurant selectedRestaurant;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_menu_item_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        menuRepository = MenuRepository.getInstance();
        currentUser = SessionManager.getInstance(requireContext()).getCurrentUser();

        rvMenuItems = view.findViewById(R.id.rv_menu_items);
        emptyState = view.findViewById(R.id.empty_state);
        fabAdd = view.findViewById(R.id.fab_add_menu_item);
        etSearch = view.findViewById(R.id.et_search);
        etFilterRestaurant = view.findViewById(R.id.et_filter_restaurant);
        etFilterCategory = view.findViewById(R.id.et_filter_category);

        setupRecyclerView();
        loadInitialData();
        setupListeners();

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

    private void loadInitialData() {
        // Load Categories
        menuRepository.getCategories(new BaseSupabaseClient.ApiCallback<Category[]>() {
            @Override
            public void onSuccess(Category[] result) {
                categories.clear();
                if (result != null) {
                    for (Category c : result) categories.add(c);
                    setupCategoryFilter();
                    // Pass categories to the adapter AFTER loading them
                    adapter.setCategories(categories);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Lỗi tải danh mục: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        // Load Restaurants
        if (currentUser.isAdmin()) {
            menuRepository.getRestaurants(new BaseSupabaseClient.ApiCallback<Restaurant[]>() {
                @Override
                public void onSuccess(Restaurant[] result) {
                    handleRestaurantsLoaded(result);
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Lỗi tải nhà hàng: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            menuRepository.getRestaurantsForStaff(currentUser.getId(), new BaseSupabaseClient.ApiCallback<Restaurant[]>() {
                @Override
                public void onSuccess(Restaurant[] result) {
                    handleRestaurantsLoaded(result);
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Lỗi tải nhà hàng: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    private void handleRestaurantsLoaded(Restaurant[] result) {
        restaurants.clear();
        if (result != null) {
            restaurants.addAll(Arrays.asList(result));
            setupRestaurantFilter();
            
            // If user is staff or only one restaurant available, pre-select it
            if (!currentUser.isAdmin() || restaurants.size() == 1) {
                if (!restaurants.isEmpty()) {
                    selectedRestaurant = restaurants.get(0);
                    etFilterRestaurant.setText(selectedRestaurant.getName());
                    loadMenuItems(); // Load items for the pre-selected restaurant
                }
            } else if (restaurants.isEmpty()) {
                 etFilterRestaurant.setHint("Không có nhà hàng nào");
                 etFilterRestaurant.setEnabled(false);
            }
        }
    }

    private void setupRestaurantFilter() {
        etFilterRestaurant.setFocusable(false); // Make it non-editable
        etFilterRestaurant.setClickable(true); // Make it clickable to show dialog
        etFilterRestaurant.setOnClickListener(v -> showRestaurantPicker());
        if (!restaurants.isEmpty() && restaurants.size() == 1 && !currentUser.isAdmin()) {
            etFilterRestaurant.setText(restaurants.get(0).getName());
            etFilterRestaurant.setEnabled(false);
        } else if (restaurants.isEmpty()) {
             etFilterRestaurant.setText("Không có nhà hàng");
             etFilterRestaurant.setEnabled(false);
        }
    }

    private void setupCategoryFilter() {
        etFilterCategory.setFocusable(false);
        etFilterCategory.setClickable(true);
        etFilterCategory.setOnClickListener(v -> showCategoryPicker());
        if (categories.isEmpty()) {
            etFilterCategory.setHint("Không có danh mục");
            etFilterCategory.setEnabled(false);
        }
    }

    private void setupListeners() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            loadMenuItems();
            return true;
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s == null || s.toString().trim().isEmpty()) {
                    loadMenuItems();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void showRestaurantPicker() {
        if (restaurants.isEmpty()) return;

        String[] names = new String[restaurants.size()];
        for (int i = 0; i < restaurants.size(); i++) {
            names[i] = restaurants.get(i).getName();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn nhà hàng")
                .setItems(names, (dialog, which) -> {
                    selectedRestaurant = restaurants.get(which);
                    etFilterRestaurant.setText(selectedRestaurant.getName());
                    loadMenuItems();
                })
                .show();
    }

    private void showCategoryPicker() {
        if (categories.isEmpty()) return;

        String[] names = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            names[i] = categories.get(i).getName();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn danh mục")
                .setItems(names, (dialog, which) -> {
                    selectedCategory = categories.get(which);
                    etFilterCategory.setText(selectedCategory.getName());
                    loadMenuItems();
                })
                .show();
    }

    private void loadMenuItems() {
        String restaurantId = (selectedRestaurant != null) ? selectedRestaurant.getId() : null;
        String categoryId = (selectedCategory != null) ? selectedCategory.getId() : null;
        String searchTerm = etSearch.getText() != null ? etSearch.getText().toString().trim() : null;

        if (!currentUser.isAdmin() && restaurantId == null && !restaurants.isEmpty()) {
             // If staff and no restaurant selected, default to first one if available
             restaurantId = restaurants.get(0).getId();
        }

        if (restaurantId == null && currentUser.isAdmin()) {
            renderMenuItems(new ArrayList<>());
            return;
        }

        menuRepository.getMenuItems(restaurantId, categoryId, new BaseSupabaseClient.ApiCallback<MenuItem[]>() {
            @Override
            public void onSuccess(MenuItem[] result) {
                allMenuItems.clear();
                if (result != null && result.length > 0) {
                    allMenuItems.addAll(Arrays.asList(result));
                    List<MenuItem> displayItems = new ArrayList<>(allMenuItems);
                    if (searchTerm != null && !searchTerm.isEmpty()) {
                        List<MenuItem> filteredList = new ArrayList<>();
                        for (MenuItem item : allMenuItems) {
                            String itemName = item.getName();
                            if (itemName != null && itemName.toLowerCase().contains(searchTerm.toLowerCase())) {
                                filteredList.add(item);
                            }
                        }
                        displayItems = filteredList;
                    }
                    renderMenuItems(displayItems);
                } else {
                    renderMenuItems(new ArrayList<>());
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Lỗi tải món ăn: " + error, Toast.LENGTH_SHORT).show();
                renderMenuItems(new ArrayList<>());
            }
        });
    }

    private void renderMenuItems(List<MenuItem> items) {
        adapter.setItems(items);
        if (items.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rvMenuItems.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvMenuItems.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onMenuItemClick(MenuItem item) {
        Intent intent = new Intent(getContext(), MenuItemFormActivity.class);
        intent.putExtra(MenuItemFormActivity.EXTRA_MENU_ITEM, item);
        startActivity(intent);
    }

    @Override
    public void onAvailabilityChange(MenuItem item, boolean isAvailable) {
        item.setAvailable(isAvailable); // Update local model first
        menuRepository.updateMenuItem(item, new BaseSupabaseClient.ApiCallback<MenuItem>() {
            @Override
            public void onSuccess(MenuItem updatedItem) {
                Toast.makeText(getContext(), "Trạng thái đã được cập nhật", Toast.LENGTH_SHORT).show();
                // Find the item in the adapter's list and update it
                int index = allMenuItems.indexOf(item);
                if (index != -1) {
                    allMenuItems.set(index, updatedItem);
                }
                adapter.notifyDataSetChanged(); // Refresh adapter
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Lỗi cập nhật trạng thái: " + error, Toast.LENGTH_SHORT).show();
                // Revert the change in UI if update fails
                item.setAvailable(!isAvailable); 
                adapter.notifyDataSetChanged();
            }
        });
    }
    
    @Override
    public void onDeleteClick(MenuItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa món ăn")
                .setMessage("Bạn có chắc chắn muốn xóa món '" + item.getName() + "' không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    menuRepository.deleteMenuItem(item.getId(), new BaseSupabaseClient.ApiCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(getContext(), "Món ăn đã được xóa", Toast.LENGTH_SHORT).show();
                            loadMenuItems(); // Reload the list after deletion
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(getContext(), "Lỗi xóa món ăn: " + error, Toast.LENGTH_SHORT).show();
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
        if (!currentUser.isAdmin() || selectedRestaurant != null) {
            loadMenuItems();
        }
    }
}
