package com.utt.foodcouriers_admin.ui.shipper;

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
import com.google.android.material.textfield.TextInputLayout;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.ShipperProfile;
import com.utt.foodcouriers_admin.data.model.User;
import com.utt.foodcouriers_admin.data.repository.AuthRepository;
import com.utt.foodcouriers_admin.data.repository.ShipperRepository;
import com.utt.foodcouriers_admin.data.repository.StorageRepository;
import com.utt.foodcouriers_admin.data.repository.UserRepository;
import com.utt.foodcouriers_admin.data.request.UserUpdateRequest;
import com.utt.foodcouriers_admin.ui.common.dialog.ImageZoomDialogFragment;
import com.utt.foodcouriers_admin.ui.main.MainActivity;
import com.utt.foodcouriers_admin.utils.SessionManager;
import com.utt.foodcouriers_admin.utils.ToastBanner;

import java.text.NumberFormat;
import java.util.Locale;

public class ShipperProfileFragment extends Fragment {

    private ImageView ivAvatar;
    private FloatingActionButton fabEditAvatar;
    private TextView tvNameDisplay;
    private TextView tvEmailDisplay;
    private TextView tvProfileSummary;
    private TextView tvStatusDesc;
    private Chip chipRole;
    private Chip chipAccountStatus;
    private Chip chipRestaurant;
    private MaterialSwitch swAvailability;
    private View cardAvailability;
    private View layoutShipperStats;
    private View statOrders;
    private View statRevenue;
    private View statTotalRevenue;
    private TextView tvVehicleSectionTitle;
    private View cardVehicleInfo;
    private TextInputLayout tilNewPassword;
    private TextInputLayout tilConfirmNewPassword;
    private TextInputEditText etFullName;
    private TextInputEditText etPhone;
    private TextInputEditText etEmailReadonly;
    private TextInputEditText etVehicleType;
    private TextInputEditText etLicensePlate;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmNewPassword;
    private MaterialButton btnToggleEditProfile;
    private MaterialButton btnSaveAll;
    private MaterialButton btnChangePassword;

    private AuthRepository authRepository;
    private ShipperRepository shipperRepository;
    private UserRepository userRepository;
    private StorageRepository storageRepository;
    private SessionManager sessionManager;
    private ShipperProfile currentProfile;
    private User currentUser;
    private boolean isShipper;
    private boolean isEditMode;

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

        authRepository = AuthRepository.getInstance();
        shipperRepository = ShipperRepository.getInstance();
        userRepository = UserRepository.getInstance();
        storageRepository = StorageRepository.getInstance();
        sessionManager = SessionManager.getInstance(requireContext());
        currentUser = sessionManager.getCurrentUser();
        isShipper = currentUser != null && currentUser.isShipper();

        initViews(view);
        configureUiForRole();
        setupListeners();
        loadProfile();
    }

    private void initViews(View view) {
        ivAvatar = view.findViewById(R.id.iv_avatar);
        fabEditAvatar = view.findViewById(R.id.fab_edit_avatar);
        tvNameDisplay = view.findViewById(R.id.tv_name_display);
        tvEmailDisplay = view.findViewById(R.id.tv_email_display);
        tvProfileSummary = view.findViewById(R.id.tv_profile_summary);
        tvStatusDesc = view.findViewById(R.id.tv_status_desc);
        chipRole = view.findViewById(R.id.chip_role);
        chipAccountStatus = view.findViewById(R.id.chip_account_status);
        chipRestaurant = view.findViewById(R.id.chip_restaurant);
        swAvailability = view.findViewById(R.id.sw_availability);
        cardAvailability = view.findViewById(R.id.card_availability);
        layoutShipperStats = view.findViewById(R.id.layout_shipper_stats);
        statOrders = view.findViewById(R.id.stat_orders);
        statRevenue = view.findViewById(R.id.stat_revenue);
        statTotalRevenue = view.findViewById(R.id.stat_total_revenue);
        tvVehicleSectionTitle = view.findViewById(R.id.tv_vehicle_section_title);
        cardVehicleInfo = view.findViewById(R.id.card_vehicle_info);
        tilNewPassword = view.findViewById(R.id.til_new_password);
        tilConfirmNewPassword = view.findViewById(R.id.til_confirm_new_password);
        etFullName = view.findViewById(R.id.et_full_name);
        etPhone = view.findViewById(R.id.et_phone);
        etEmailReadonly = view.findViewById(R.id.et_email_readonly);
        etVehicleType = view.findViewById(R.id.et_vehicle_type);
        etLicensePlate = view.findViewById(R.id.et_license_plate);
        etNewPassword = view.findViewById(R.id.et_new_password);
        etConfirmNewPassword = view.findViewById(R.id.et_confirm_new_password);
        btnToggleEditProfile = view.findViewById(R.id.btn_toggle_edit_profile);
        btnSaveAll = view.findViewById(R.id.btn_save_all);
        btnChangePassword = view.findViewById(R.id.btn_change_password);

        setupStatCard(statOrders, "Đơn đã giao", "0", R.drawable.ic_completed, R.color.success);
        setupStatCard(statRevenue, "Doanh thu", "0đ", R.drawable.ic_revenue, R.color.primary);
    }

    private void configureUiForRole() {
        int shipperVisibility = isShipper ? View.VISIBLE : View.GONE;
        cardAvailability.setVisibility(shipperVisibility);
        layoutShipperStats.setVisibility(shipperVisibility);
        tvVehicleSectionTitle.setVisibility(shipperVisibility);
        cardVehicleInfo.setVisibility(shipperVisibility);
        chipRestaurant.setVisibility(View.GONE);
        applyEditMode(false);
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
        ivAvatar.setOnClickListener(v -> openAvatarPreview());
        swAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isShipper && isEditMode) {
                updateAvailability(isChecked);
            }
        });
        btnToggleEditProfile.setOnClickListener(v -> toggleEditMode());
        btnSaveAll.setOnClickListener(v -> saveAllChanges());
        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    private void openAvatarPreview() {
        String avatarUrl = currentProfile != null ? currentProfile.getAvatarUrl()
                : currentUser != null ? currentUser.getAvatarUrl() : null;
        if (!TextUtils.isEmpty(avatarUrl)) {
            ImageZoomDialogFragment.newInstance(avatarUrl)
                    .show(getChildFragmentManager(), "ImageZoom");
        }
    }

    private void loadProfile() {
        if (currentUser == null) {
            return;
        }

        userRepository.getById(currentUser.getId(), new RepositoryCallback<User>() {
            @Override
            public void onComplete(BaseResponse<User> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccess() && response.getData() != null) {
                    currentUser = response.getData();
                    sessionManager.updateUserInfo(currentUser);
                    refreshHostHeader();
                    bindCommonUserData();
                    if (isShipper) {
                        loadShipperProfile();
                    }
                } else {
                    ToastBanner.showError("Không thể tải thông tin cá nhân: " + response.getMessage());
                }
            }
        });
    }

    private void loadShipperProfile() {
        shipperRepository.getProfileByUserId(currentUser.getId(), new RepositoryCallback<ShipperProfile>() {
            @Override
            public void onComplete(BaseResponse<ShipperProfile> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccess() && response.getData() != null) {
                    currentProfile = response.getData();
                    bindShipperData();
                } else {
                    ToastBanner.showError("Không thể tải thông tin shipper: " + response.getMessage());
                }
            }
        });
    }

    private void bindCommonUserData() {
        if (currentUser == null) {
            return;
        }

        tvNameDisplay.setText(valueOrFallback(currentUser.getFullName()));
        tvEmailDisplay.setText(valueOrFallback(currentUser.getEmail()));
        etFullName.setText(currentUser.getFullName());
        etPhone.setText(currentUser.getPhone());
        etEmailReadonly.setText(currentUser.getEmail());
        tvProfileSummary.setText(isShipper
                ? R.string.profile_summary_shipper
                : R.string.profile_summary_staff);

        chipRole.setText(getRoleLabel(currentUser));
        chipAccountStatus.setText(currentUser.isActive()
                ? R.string.profile_chip_active
                : R.string.profile_chip_inactive);
        chipAccountStatus.setChipStrokeColorResource(currentUser.isActive() ? R.color.secondary : R.color.warning);
        chipAccountStatus.setChipBackgroundColorResource(currentUser.isActive() ? R.color.secondary_light : R.color.primary_light);

        int avatarPlaceholder = isShipper ? R.drawable.ic_shipper : R.drawable.ic_admin_avatar;
        if (!TextUtils.isEmpty(currentUser.getAvatarUrl())) {
            Glide.with(this)
                    .load(currentUser.getAvatarUrl())
                    .placeholder(avatarPlaceholder)
                    .error(avatarPlaceholder)
                    .circleCrop()
                    .into(ivAvatar);
        } else {
            ivAvatar.setImageResource(avatarPlaceholder);
        }
    }

    private void bindShipperData() {
        if (currentProfile == null) {
            return;
        }

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

        etVehicleType.setText(currentProfile.getVehicleType());
        etLicensePlate.setText(currentProfile.getLicensePlate());

        updateStatValue(statOrders, String.valueOf(currentProfile.getTotalDelivered()));
        
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        setupStatCard(statTotalRevenue, "Tổng doanh thu", formatter.format(currentProfile.getTotalRevenue()), R.drawable.ic_revenue, R.color.success);
        
        // Tải doanh thu trong ngày
        loadDailyRevenue();
    }

    private void loadDailyRevenue() {
        if (currentProfile == null) return;
        
        shipperRepository.getTodayOrdersByShipper(currentProfile.getId(), new RepositoryCallback<java.util.List<com.utt.foodcouriers_admin.data.model.Order>>() {
            @Override
            public void onComplete(BaseResponse<java.util.List<com.utt.foodcouriers_admin.data.model.Order>> response) {
                if (!isAdded()) return;
                
                long dailyRevenue = 0;
                if (response.isSuccess() && response.getData() != null) {
                    for (com.utt.foodcouriers_admin.data.model.Order order : response.getData()) {
                        if ("delivered".equals(order.getStatus())) {
                            dailyRevenue += order.getTotal();
                        }
                    }
                }
                
                currentProfile.setDailyRevenue(dailyRevenue);
                NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                setupStatCard(statRevenue, "Doanh thu hôm nay", formatter.format(dailyRevenue), R.drawable.ic_revenue, R.color.success);
            }
        });
    }

    private void updateStatValue(View card, String value) {
        TextView tvValue = card.findViewById(R.id.stat_value);
        tvValue.setText(value);
    }

    private void updateStatusText(boolean isAvailable) {
        tvStatusDesc.setText(isAvailable
                ? "Đang trực tuyến - Sẵn sàng nhận đơn"
                : "Đang ngoại tuyến - Nghỉ ngơi");
        tvStatusDesc.setTextColor(requireContext().getColor(isAvailable ? R.color.success : R.color.text_secondary));
    }

    private void toggleEditMode() {
        applyEditMode(!isEditMode);
    }

    private void applyEditMode(boolean enabled) {
        isEditMode = enabled;

        fabEditAvatar.setVisibility(enabled ? View.VISIBLE : View.GONE);
        btnSaveAll.setVisibility(enabled ? View.VISIBLE : View.GONE);
        btnToggleEditProfile.setText(enabled ? R.string.profile_action_cancel_edit : R.string.profile_action_edit_profile);

        setEditable(etFullName, enabled);
        setEditable(etPhone, enabled);

        boolean shipperEditable = enabled && isShipper;
        setEditable(etVehicleType, shipperEditable);
        setEditable(etLicensePlate, shipperEditable);
        swAvailability.setEnabled(shipperEditable);

        if (!enabled) {
            clearProfileFieldFocus();
            bindCommonUserData();
            if (isShipper && currentProfile != null) {
                bindShipperData();
            }
        }
    }

    private void setEditable(TextInputEditText editText, boolean editable) {
        editText.setEnabled(editable);
        editText.setFocusable(editable);
        editText.setFocusableInTouchMode(editable);
        editText.setClickable(editable);
        editText.setLongClickable(editable);
    }

    private void clearProfileFieldFocus() {
        etFullName.clearFocus();
        etPhone.clearFocus();
        etVehicleType.clearFocus();
        etLicensePlate.clearFocus();
    }

    private void updateAvailability(boolean isAvailable) {
        if (!isShipper || currentProfile == null) {
            return;
        }

        shipperRepository.updateAvailability(currentProfile.getId(), isAvailable, new RepositoryCallback<ShipperProfile>() {
            @Override
            public void onComplete(BaseResponse<ShipperProfile> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccess()) {
                    currentProfile.setAvailable(isAvailable);
                    updateStatusText(isAvailable);
                    ToastBanner.showSuccess(isAvailable
                            ? "Đã bật trạng thái trực tuyến"
                            : "Đã tắt trạng thái trực tuyến");
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
        if (currentUser == null) {
            return;
        }

        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String vehicleType = etVehicleType.getText() != null ? etVehicleType.getText().toString().trim() : "";
        String licensePlate = etLicensePlate.getText() != null ? etLicensePlate.getText().toString().trim() : "";

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(phone)) {
            ToastBanner.showWarning("Vui lòng nhập đầy đủ thông tin");
            return;
        }

        if (isShipper && (TextUtils.isEmpty(vehicleType) || TextUtils.isEmpty(licensePlate))) {
            ToastBanner.showWarning(getString(R.string.profile_error_vehicle_required));
            return;
        }

        btnSaveAll.setEnabled(false);
        btnSaveAll.setText(R.string.user_action_saving);

        UserUpdateRequest userRequest = new UserUpdateRequest();
        userRequest.setFullName(fullName);
        userRequest.setPhone(phone);

        userRepository.update(currentUser.getId(), userRequest, new RepositoryCallback<User>() {
            @Override
            public void onComplete(BaseResponse<User> userResponse) {
                if (!isAdded()) {
                    return;
                }

                if (userResponse.isSuccess() && userResponse.getData() != null) {
                    currentUser = userResponse.getData();
                    sessionManager.updateUserInfo(currentUser);
                    refreshHostHeader();
                    bindCommonUserData();

                    if (isShipper && currentProfile != null) {
                        updateShipperVehicleInfo(licensePlate, vehicleType);
                    } else {
                        restoreSaveButton();
                        applyEditMode(false);
                        ToastBanner.showSuccess("Đã cập nhật thông tin thành công");
                    }
                } else {
                    restoreSaveButton();
                    ToastBanner.showError("Lỗi cập nhật thông tin cá nhân: " + userResponse.getMessage());
                }
            }
        });
    }

    private void updateShipperVehicleInfo(String licensePlate, String vehicleType) {
        shipperRepository.updateSelf(currentProfile.getId(), licensePlate, vehicleType, new RepositoryCallback<ShipperProfile>() {
            @Override
            public void onComplete(BaseResponse<ShipperProfile> shipperResponse) {
                if (!isAdded()) {
                    return;
                }

                restoreSaveButton();
                if (shipperResponse.isSuccess() && shipperResponse.getData() != null) {
                    currentProfile = shipperResponse.getData();
                    bindShipperData();
                    applyEditMode(false);
                    ToastBanner.showSuccess("Đã cập nhật thông tin thành công");
                } else {
                    ToastBanner.showError("Lỗi cập nhật thông tin xe: " + shipperResponse.getMessage());
                }
            }
        });
    }

    private void changePassword() {
        clearPasswordErrors();

        String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";
        String confirmPassword = etConfirmNewPassword.getText() != null ? etConfirmNewPassword.getText().toString() : "";

        if (TextUtils.isEmpty(newPassword.trim())) {
            tilNewPassword.setError(getString(R.string.error_field_required));
            return;
        }
        if (newPassword.trim().length() < 6) {
            tilNewPassword.setError(getString(R.string.user_error_password_min));
            return;
        }
        if (TextUtils.isEmpty(confirmPassword.trim())) {
            tilConfirmNewPassword.setError(getString(R.string.error_field_required));
            return;
        }
        if (!TextUtils.equals(newPassword.trim(), confirmPassword.trim())) {
            tilConfirmNewPassword.setError(getString(R.string.user_error_password_mismatch));
            return;
        }

        btnChangePassword.setEnabled(false);
        btnChangePassword.setText(R.string.profile_action_changing_password);

        authRepository.changePassword(newPassword, confirmPassword, new AuthRepository.PasswordChangeCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) {
                    return;
                }
                restorePasswordButton();
                clearPasswordFields();
                ToastBanner.showSuccess(getString(R.string.profile_password_changed));
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }
                restorePasswordButton();
                ToastBanner.showError(error);
            }
        });
    }

    private void clearPasswordErrors() {
        tilNewPassword.setError(null);
        tilConfirmNewPassword.setError(null);
    }

    private void clearPasswordFields() {
        etNewPassword.setText(null);
        etConfirmNewPassword.setText(null);
        clearPasswordErrors();
    }

    private void restoreSaveButton() {
        btnSaveAll.setEnabled(true);
        btnSaveAll.setText(R.string.profile_action_save_profile);
    }

    private void restorePasswordButton() {
        btnChangePassword.setEnabled(true);
        btnChangePassword.setText(R.string.profile_action_change_password);
    }

    private void uploadAvatar(Uri uri) {
        ToastBanner.showWarning("Đang tải ảnh lên...");
        storageRepository.uploadImage(requireContext(), uri, "avatars", new RepositoryCallback<String>() {
            @Override
            public void onComplete(BaseResponse<String> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccess() && response.getData() != null) {
                    updateProfileAvatar(response.getData());
                } else {
                    ToastBanner.showError("Lỗi tải ảnh: " + response.getMessage());
                }
            }
        });
    }

    private void updateProfileAvatar(String imageUrl) {
        if (currentUser == null) {
            return;
        }

        UserUpdateRequest request = new UserUpdateRequest();
        request.setAvatarUrl(imageUrl);

        userRepository.update(currentUser.getId(), request, new RepositoryCallback<User>() {
            @Override
            public void onComplete(BaseResponse<User> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccess() && response.getData() != null) {
                    currentUser = response.getData();
                    sessionManager.updateUserInfo(currentUser);
                    refreshHostHeader();
                    bindCommonUserData();
                    if (currentProfile != null) {
                        currentProfile.setAvatarUrl(imageUrl);
                    }
                    applyEditMode(false);
                    ToastBanner.showSuccess("Đã cập nhật ảnh đại diện");
                } else {
                    ToastBanner.showError("Lỗi lưu ảnh: " + response.getMessage());
                }
            }
        });
    }

    private String getRoleLabel(User user) {
        if (user == null || TextUtils.isEmpty(user.getRole())) {
            return "--";
        }
        if (user.isShipper()) {
            return "Shipper";
        }
        if (user.isStaff()) {
            return "Staff";
        }
        if (user.isAdmin()) {
            return "Admin";
        }
        return user.getRole();
    }

    private String valueOrFallback(String value) {
        return TextUtils.isEmpty(value) ? "--" : value;
    }

    private void refreshHostHeader() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshNavigationHeader();
        }
    }
}
