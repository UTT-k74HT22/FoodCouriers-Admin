package com.utt.foodcouriers_admin.ui.shipper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.ShipperProfile;
import com.utt.foodcouriers_admin.data.model.User;
import com.utt.foodcouriers_admin.data.repository.ShipperRepository;
import com.utt.foodcouriers_admin.data.repository.StorageRepository;
import com.utt.foodcouriers_admin.data.repository.UserRepository;
import com.utt.foodcouriers_admin.data.request.UserUpdateRequest;
import com.utt.foodcouriers_admin.ui.auth.LoginActivity;
import com.utt.foodcouriers_admin.ui.common.dialog.ImageZoomDialogFragment;
import com.utt.foodcouriers_admin.utils.SessionManager;
import com.utt.foodcouriers_admin.utils.ToastBanner;

import java.text.NumberFormat;
import java.util.Locale;

public class ShipperProfileFragment extends Fragment {

    private ImageView ivAvatar;
    private FloatingActionButton fabEditAvatar;
    private TextView tvNameDisplay, tvEmailDisplay, tvStatusDesc;
    private Chip chipRestaurant;
    private MaterialSwitch swAvailability;
    private View statOrders, statRevenue;
    private TextInputEditText etFullName, etPhone, etVehicleType, etLicensePlate;
    private MaterialButton btnSaveAll;

    private ShipperRepository shipperRepository;
    private UserRepository userRepository;
    private StorageRepository storageRepository;
    private SessionManager sessionManager;
    private ShipperProfile currentProfile;
    private User currentUser;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadAvatar(uri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shipper_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        shipperRepository = ShipperRepository.getInstance();
        userRepository = UserRepository.getInstance();
        storageRepository = StorageRepository.getInstance();
        sessionManager = SessionManager.getInstance(requireContext());
        currentUser = sessionManager.getCurrentUser();

        initViews(view);
        setupListeners();
        loadProfile();
    }

    private void initViews(View view) {
        ivAvatar = view.findViewById(R.id.iv_avatar);
        fabEditAvatar = view.findViewById(R.id.fab_edit_avatar);
        tvNameDisplay = view.findViewById(R.id.tv_name_display);
        tvEmailDisplay = view.findViewById(R.id.tv_email_display);
        tvStatusDesc = view.findViewById(R.id.tv_status_desc);
        chipRestaurant = view.findViewById(R.id.chip_restaurant);
        swAvailability = view.findViewById(R.id.sw_availability);
        
        statOrders = view.findViewById(R.id.stat_orders);
        statRevenue = view.findViewById(R.id.stat_revenue);
        
        etFullName = view.findViewById(R.id.et_full_name);
        etPhone = view.findViewById(R.id.et_phone);
        etVehicleType = view.findViewById(R.id.et_vehicle_type);
        etLicensePlate = view.findViewById(R.id.et_license_plate);
        
        btnSaveAll = view.findViewById(R.id.btn_save_all);

        // Setup Stat Cards (using the fixed IDs)
        setupStatCard(statOrders, "Đơn đã giao", "0", R.drawable.ic_completed, R.color.success);
        setupStatCard(statRevenue, "Doanh thu", "0đ", R.drawable.ic_revenue, R.color.primary);
    }

    private void setupStatCard(View card, String title, String value, int iconRes, int colorRes) {
        TextView tvTitle = card.findViewById(R.id.stat_label);
        TextView tvValue = card.findViewById(R.id.stat_value);
        ImageView ivIcon = card.findViewById(R.id.stat_icon);
        
        tvTitle.setText(title);
        tvValue.setText(value);
        ivIcon.setImageResource(iconRes);
        ivIcon.setColorFilter(requireContext().getColor(colorRes));
    }

    private void setupListeners() {
        fabEditAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        
        ivAvatar.setOnClickListener(v -> {
            if (currentProfile != null && !TextUtils.isEmpty(currentProfile.getAvatarUrl())) {
                ImageZoomDialogFragment.newInstance(currentProfile.getAvatarUrl())
                        .show(getChildFragmentManager(), "ImageZoom");
            }
        });
        swAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> updateAvailability(isChecked));
        btnSaveAll.setOnClickListener(v -> saveAllChanges());
    }

    private void loadProfile() {
        if (currentUser == null) return;

        shipperRepository.getProfileByUserId(currentUser.getId(), new RepositoryCallback<ShipperProfile>() {
            @Override
            public void onComplete(BaseResponse<ShipperProfile> response) {
                if (!isAdded()) return;
                
                if (response.isSuccess() && response.getData() != null) {
                    currentProfile = response.getData();
                    bindProfileData();
                } else {
                    ToastBanner.showError("Không thể tải thông tin shipper: " + response.getMessage());
                }
            }
        });
    }

    private void bindProfileData() {
        tvNameDisplay.setText(currentProfile.getFullName());
        tvEmailDisplay.setText(currentProfile.getEmail());
        
        if (!TextUtils.isEmpty(currentProfile.getRestaurantName())) {
            chipRestaurant.setText(currentProfile.getRestaurantName());
            chipRestaurant.setVisibility(View.VISIBLE);
        } else {
            chipRestaurant.setVisibility(View.GONE);
        }

        swAvailability.setOnCheckedChangeListener(null);
        swAvailability.setChecked(currentProfile.isAvailable());
        updateStatusText(currentProfile.isAvailable());
        swAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> updateAvailability(isChecked));

        // Set values to edit fields
        etFullName.setText(currentProfile.getFullName());
        etPhone.setText(currentProfile.getPhone());
        etVehicleType.setText(currentProfile.getVehicleType());
        etLicensePlate.setText(currentProfile.getLicensePlate());

        // Update stats
        updateStatValue(statOrders, String.valueOf(currentProfile.getTotalDelivered()));
        
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        updateStatValue(statRevenue, formatter.format(currentProfile.getTotalRevenue()));

        if (!TextUtils.isEmpty(currentProfile.getAvatarUrl())) {
            Glide.with(this)
                    .load(currentProfile.getAvatarUrl())
                    .placeholder(R.drawable.ic_shipper)
                    .circleCrop()
                    .into(ivAvatar);
        }
    }

    private void updateStatValue(View card, String value) {
        TextView tvValue = card.findViewById(R.id.stat_value);
        tvValue.setText(value);
    }

    private void updateStatusText(boolean isAvailable) {
        tvStatusDesc.setText(isAvailable ? "Đang trực tuyến - Sẵn sàng nhận đơn" : "Đang ngoại tuyến - Nghỉ ngơi");
        tvStatusDesc.setTextColor(requireContext().getColor(isAvailable ? R.color.success : R.color.text_secondary));
    }

    private void updateAvailability(boolean isAvailable) {
        if (currentProfile == null) return;

        shipperRepository.updateAvailability(currentProfile.getId(), isAvailable, new RepositoryCallback<ShipperProfile>() {
            @Override
            public void onComplete(BaseResponse<ShipperProfile> response) {
                if (!isAdded()) return;
                
                if (response.isSuccess()) {
                    currentProfile.setAvailable(isAvailable);
                    updateStatusText(isAvailable);
                    ToastBanner.showSuccess(isAvailable ? "Đã bật trạng thái trực tuyến" : "Đã tắt trạng thái trực tuyến");
                } else {
                    swAvailability.setOnCheckedChangeListener(null);
                    swAvailability.setChecked(!isAvailable);
                    swAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> updateAvailability(isChecked));
                    ToastBanner.showError("Lỗi cập nhật trạng thái: " + response.getMessage());
                }
            }
        });
    }

    private void saveAllChanges() {
        if (currentProfile == null) return;

        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String vehicleType = etVehicleType.getText().toString().trim();
        String licensePlate = etLicensePlate.getText().toString().trim();

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(phone) || 
            TextUtils.isEmpty(vehicleType) || TextUtils.isEmpty(licensePlate)) {
            ToastBanner.showWarning("Vui lòng nhập đầy đủ thông tin");
            return;
        }

        btnSaveAll.setEnabled(false);
        btnSaveAll.setText("Đang lưu...");

        // Step 1: Update User Table (Full Name & Phone)
        UserUpdateRequest userRequest = new UserUpdateRequest();
        userRequest.setFullName(fullName);
        userRequest.setPhone(phone);

        userRepository.update(currentUser.getId(), userRequest, new RepositoryCallback<User>() {
            @Override
            public void onComplete(BaseResponse<User> userResponse) {
                if (!isAdded()) return;

                if (userResponse.isSuccess()) {
                    // Update session name if changed
                    sessionManager.saveUserName(fullName);
                    
                    // Step 2: Update Shipper Table (Vehicle info)
                    shipperRepository.updateSelf(currentProfile.getId(), licensePlate, vehicleType, new RepositoryCallback<ShipperProfile>() {
                        @Override
                        public void onComplete(BaseResponse<ShipperProfile> shipperResponse) {
                            if (!isAdded()) return;
                            btnSaveAll.setEnabled(true);
                            btnSaveAll.setText("Lưu tất cả thay đổi");

                            if (shipperResponse.isSuccess()) {
                                ToastBanner.showSuccess("Đã cập nhật thông tin thành công");
                                // Update UI displays
                                tvNameDisplay.setText(fullName);
                                currentProfile = shipperResponse.getData();
                            } else {
                                ToastBanner.showError("Lỗi cập nhật thông tin xe: " + shipperResponse.getMessage());
                            }
                        }
                    });
                } else {
                    btnSaveAll.setEnabled(true);
                    btnSaveAll.setText("Lưu tất cả thay đổi");
                    ToastBanner.showError("Lỗi cập nhật thông tin cá nhân: " + userResponse.getMessage());
                }
            }
        });
    }

    private void uploadAvatar(Uri uri) {
        ToastBanner.showWarning("Đang tải ảnh lên...");
        storageRepository.uploadImage(requireContext(), uri, "shippers", new RepositoryCallback<String>() {
            @Override
            public void onComplete(BaseResponse<String> response) {
                if (!isAdded()) return;
                
                if (response.isSuccess()) {
                    String imageUrl = response.getData();
                    updateProfileAvatar(imageUrl);
                } else {
                    ToastBanner.showError("Lỗi tải ảnh: " + response.getMessage());
                }
            }
        });
    }

    private void updateProfileAvatar(String imageUrl) {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setAvatarUrl(imageUrl);
        
        userRepository.update(currentUser.getId(), request, new RepositoryCallback<User>() {
            @Override
            public void onComplete(BaseResponse<User> response) {
                if (!isAdded()) return;
                if (response.isSuccess()) {
                    ToastBanner.showSuccess("Đã cập nhật ảnh đại diện");
                    Glide.with(ShipperProfileFragment.this)
                            .load(imageUrl)
                            .circleCrop()
                            .into(ivAvatar);
                    if (currentProfile != null) currentProfile.setAvatarUrl(imageUrl);
                } else {
                    ToastBanner.showError("Lỗi lưu ảnh: " + response.getMessage());
                }
            }
        });
    }
}
