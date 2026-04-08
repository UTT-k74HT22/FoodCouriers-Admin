package com.utt.foodcouriers_admin.ui.menu;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Category;
import com.utt.foodcouriers_admin.data.model.MenuItem;
import com.utt.foodcouriers_admin.data.model.Restaurant;
import com.utt.foodcouriers_admin.data.model.User;
import com.utt.foodcouriers_admin.data.remote.BaseSupabaseClient;
import com.utt.foodcouriers_admin.data.repository.MenuRepository;
import com.utt.foodcouriers_admin.data.repository.StorageRepository;
import com.utt.foodcouriers_admin.ui.common.dialog.ImageZoomDialogFragment;
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
    private MaterialButton btnChooseImage;
    private ImageView ivMenuImage;
    private TextInputEditText etImage;

    private MenuRepository menuRepository;
    private StorageRepository storageRepository;
    private MenuItem currentItem;
    private String preSelectedRestaurantId;
    private Uri selectedImageUri;
    private boolean isUploading = false;
    
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
        storageRepository = StorageRepository.getInstance();
        
        initViews();
        handleIntent();
        loadInitialData();
        setupListeners();
    }

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    uploadSelectedImage();
                }
            }
    );

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
        btnChooseImage = findViewById(R.id.btn_choose_image);
        ivMenuImage = findViewById(R.id.iv_menu_image);
        etImage = findViewById(R.id.et_image);

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
        etImage.setText(currentItem.getImageUrl());
        
        loadPreviewImage(currentItem.getImageUrl());
    }

    private void loadPreviewImage(String url) {
        if (TextUtils.isEmpty(url) && etImage != null) {
            url = etImage.getText() != null ? etImage.getText().toString().trim() : null;
        }
        if (TextUtils.isEmpty(url)) {
            ivMenuImage.setImageResource(R.drawable.ic_menu_item);
            return;
        }
        Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_menu_item)
                .error(R.drawable.ic_menu_item)
                .centerCrop()
                .into(ivMenuImage);
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
        btnChooseImage.setOnClickListener(v -> openImagePicker());
        ivMenuImage.setOnClickListener(v -> handleImageClick());
        
        if (etImage != null) {
            etImage.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    loadPreviewImage(s != null ? s.toString() : null);
                }

                @Override
                public void afterTextChanged(Editable s) { }
            });
        }
    }

    private void openImagePicker() {
        pickImageLauncher.launch("image/*");
    }

    private void handleImageClick() {
        String currentUrl = null;
        if (currentItem != null) {
            currentUrl = currentItem.getImageUrl();
        }
        if (TextUtils.isEmpty(currentUrl) && etImage != null) {
            currentUrl = etImage.getText() != null ? etImage.getText().toString().trim() : null;
        }
        if (!TextUtils.isEmpty(currentUrl)) {
            ImageZoomDialogFragment.newInstance(currentUrl).show(getSupportFragmentManager(), "ImageZoomDialog");
        }
    }

    private void uploadSelectedImage() {
        if (selectedImageUri == null) return;

        isUploading = true;
        setLoading(true);
        Toast.makeText(this, R.string.toast_uploading, Toast.LENGTH_SHORT).show();

        storageRepository.uploadImage(this, selectedImageUri, "menu_items", new RepositoryCallback<String>() {
            @Override
            public void onComplete(BaseResponse<String> response) {
                isUploading = false;
                if (response.isSuccess()) {
                    String imageUrl = response.getData();
                    if (currentItem != null) {
                        currentItem.setImageUrl(imageUrl);
                    }
                    if (etImage != null) {
                        etImage.setText(imageUrl);
                    }
                    loadPreviewImage(imageUrl);
                    Toast.makeText(MenuItemFormActivity.this, R.string.toast_upload_success, Toast.LENGTH_SHORT).show();
                } else {
                    String errorMsg = response.getMessage();
                    Toast.makeText(MenuItemFormActivity.this, getString(R.string.toast_upload_failed, errorMsg), Toast.LENGTH_LONG).show();
                }
                setLoading(false);
            }
        });
    }

    private void setLoading(boolean loading) {
        if (btnSave != null) {
            btnSave.setEnabled(!loading && !isUploading);
        }
        if (btnChooseImage != null) {
            btnChooseImage.setEnabled(!loading && !isUploading);
        }
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
        
        if (isUploading) {
            Toast.makeText(this, R.string.toast_uploading, Toast.LENGTH_SHORT).show();
            return;
        }

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
        
        String imageUrl = etImage.getText() != null ? etImage.getText().toString().trim() : null;
        currentItem.setImageUrl(imageUrl);

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