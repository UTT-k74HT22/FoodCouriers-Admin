package com.utt.foodcouriers_admin.ui.menu;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.Category;
import com.utt.foodcouriers_admin.data.model.MenuItem;
import com.utt.foodcouriers_admin.data.model.Restaurant;
import com.utt.foodcouriers_admin.data.model.User;
import com.utt.foodcouriers_admin.data.remote.BaseSupabaseClient;
import com.utt.foodcouriers_admin.data.repository.MenuRepository;
import com.utt.foodcouriers_admin.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class MenuItemFormActivity extends AppCompatActivity {

    public static final String EXTRA_MENU_ITEM = "extra_menu_item";
    public static final String EXTRA_RESTAURANT_ID = "extra_restaurant_id";

    private TextInputLayout tilName, tilDescription, tilPrice, tilCategory, tilRestaurant, tilSortOrder;
    private TextInputEditText etName, etDescription, etPrice, etCategory, etRestaurant, etSortOrder;
    private MaterialSwitch swAvailable, swFeatured;
    private MaterialButton btnSave;

    private MenuRepository menuRepository;
    private MenuItem currentItem;
    private String preSelectedRestaurantId;
    
    private List<Category> categories = new ArrayList<>();
    private List<Restaurant> restaurants = new ArrayList<>();
    private Category selectedCategory;
    private Restaurant selectedRestaurant;
    
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_item_form);

        menuRepository = MenuRepository.getInstance();
        
        initViews();
        handleIntent();
        loadInitialData();
        setupListeners();
    }

    private void initViews() {
        tilName = findViewById(R.id.til_name);
        tilDescription = findViewById(R.id.til_description);
        tilPrice = findViewById(R.id.til_price);
        tilCategory = findViewById(R.id.til_category);
        tilRestaurant = findViewById(R.id.til_restaurant);
        tilSortOrder = findViewById(R.id.til_sort_order);

        etName = findViewById(R.id.et_name);
        etDescription = findViewById(R.id.et_description);
        etPrice = findViewById(R.id.et_price);
        etCategory = findViewById(R.id.et_category);
        etRestaurant = findViewById(R.id.et_restaurant);
        etSortOrder = findViewById(R.id.et_sort_order);

        swAvailable = findViewById(R.id.sw_is_available);
        swFeatured = findViewById(R.id.sw_is_featured);
        btnSave = findViewById(R.id.btn_save);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void handleIntent() {
        currentItem = (MenuItem) getIntent().getSerializableExtra(EXTRA_MENU_ITEM);
        preSelectedRestaurantId = getIntent().getStringExtra(EXTRA_RESTAURANT_ID);

        if (currentItem != null) {
            isEditMode = true;
            setTitle("Sửa món ăn");
            fillData();
        } else {
            isEditMode = false;
            setTitle("Thêm món ăn");
            swAvailable.setChecked(true);
        }
    }

    private void fillData() {
        etName.setText(currentItem.getName());
        etDescription.setText(currentItem.getDescription());
        etPrice.setText(String.valueOf(currentItem.getPrice()));
        etSortOrder.setText(String.valueOf(currentItem.getSortOrder()));
        swAvailable.setChecked(currentItem.isAvailable());
        swFeatured.setChecked(currentItem.isFeatured());
        
        // Category and Restaurant will be set after loading lists
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
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MenuItemFormActivity.this, "Lỗi tải danh mục: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        // Load Restaurants
        User currentUser = SessionManager.getInstance(this).getCurrentUser();
        if (currentUser.isAdmin()) {
            menuRepository.getRestaurants(new BaseSupabaseClient.ApiCallback<Restaurant[]>() {
                @Override
                public void onSuccess(Restaurant[] result) {
                    handleRestaurantsLoaded(result);
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(MenuItemFormActivity.this, "Lỗi tải nhà hàng: " + error, Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(MenuItemFormActivity.this, "Lỗi tải nhà hàng: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void handleRestaurantsLoaded(Restaurant[] result) {
        restaurants.clear();
        if (result != null) {
            for (Restaurant r : result) restaurants.add(r);
            
            if (isEditMode) {
                for (Restaurant r : restaurants) {
                    if (r.getId().equals(currentItem.getRestaurantId())) {
                        selectedRestaurant = r;
                        etRestaurant.setText(r.getName());
                        break;
                    }
                }
            } else if (preSelectedRestaurantId != null) {
                for (Restaurant r : restaurants) {
                    if (r.getId().equals(preSelectedRestaurantId)) {
                        selectedRestaurant = r;
                        etRestaurant.setText(r.getName());
                        break;
                    }
                }
            } else if (restaurants.size() == 1) {
                selectedRestaurant = restaurants.get(0);
                etRestaurant.setText(selectedRestaurant.getName());
            }
        }
    }

    private void setupListeners() {
        etCategory.setOnClickListener(v -> showCategoryPicker());
        etRestaurant.setOnClickListener(v -> showRestaurantPicker());
        btnSave.setOnClickListener(v -> saveMenuItem());
    }

    private void setupCategoryFilter() {
        etCategory.setFocusable(false);
        etCategory.setClickable(true);
        if (categories.isEmpty()) {
            etCategory.setHint("Không có danh mục");
            etCategory.setEnabled(false);
        }
    }

    private void showCategoryPicker() {
        if (categories.isEmpty()) return;
        
        String[] names = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) names[i] = categories.get(i).getName();
        
        new AlertDialog.Builder(this)
                .setTitle("Chọn danh mục")
                .setItems(names, (dialog, which) -> {
                    selectedCategory = categories.get(which);
                    etCategory.setText(selectedCategory.getName());
                })
                .show();
    }

    private void showRestaurantPicker() {
        if (restaurants.isEmpty()) return;
        if (!SessionManager.getInstance(this).getCurrentUser().isAdmin() && restaurants.size() <= 1) return;

        String[] names = new String[restaurants.size()];
        for (int i = 0; i < restaurants.size(); i++) names[i] = restaurants.get(i).getName();

        new AlertDialog.Builder(this)
                .setTitle("Chọn nhà hàng")
                .setItems(names, (dialog, which) -> {
                    selectedRestaurant = restaurants.get(which);
                    etRestaurant.setText(selectedRestaurant.getName());
                })
                .show();
    }

    private void saveMenuItem() {
        if (!validateInput()) return;

        if (!isEditMode) currentItem = new MenuItem();
        
        currentItem.setName(etName.getText().toString().trim());
        currentItem.setDescription(etDescription.getText().toString().trim());
        currentItem.setPrice(Integer.parseInt(etPrice.getText().toString()));
        currentItem.setCategoryId(selectedCategory.getId());
        currentItem.setRestaurantId(selectedRestaurant.getId());
        currentItem.setAvailable(swAvailable.isChecked());
        currentItem.setFeatured(swFeatured.isChecked());
        
        String sortOrderStr = etSortOrder.getText().toString();
        currentItem.setSortOrder(TextUtils.isEmpty(sortOrderStr) ? 0 : Integer.parseInt(sortOrderStr));

        btnSave.setEnabled(false);
        if (isEditMode) {
            menuRepository.updateMenuItem(currentItem, new BaseSupabaseClient.ApiCallback<MenuItem>() {
                @Override
                public void onSuccess(MenuItem result) {
                    Toast.makeText(MenuItemFormActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String error) {
                    btnSave.setEnabled(true);
                    Toast.makeText(MenuItemFormActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            menuRepository.createMenuItem(currentItem, new BaseSupabaseClient.ApiCallback<MenuItem>() {
                @Override
                public void onSuccess(MenuItem result) {
                    Toast.makeText(MenuItemFormActivity.this, "Thêm thành công", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String error) {
                    btnSave.setEnabled(true);
                    Toast.makeText(MenuItemFormActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private boolean validateInput() {
        boolean isValid = true;
        
        if (TextUtils.isEmpty(etName.getText())) {
            tilName.setError("Vui lòng nhập tên");
            isValid = false;
        } else tilName.setError(null);

        if (TextUtils.isEmpty(etPrice.getText())) {
            tilPrice.setError("Vui lòng nhập giá");
            isValid = false;
        } else tilPrice.setError(null);

        if (selectedCategory == null) {
            tilCategory.setError("Vui lòng chọn danh mục");
            isValid = false;
        } else tilCategory.setError(null);

        if (selectedRestaurant == null) {
            tilRestaurant.setError("Vui lòng chọn nhà hàng");
            isValid = false;
        } else tilRestaurant.setError(null);

        return isValid;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}