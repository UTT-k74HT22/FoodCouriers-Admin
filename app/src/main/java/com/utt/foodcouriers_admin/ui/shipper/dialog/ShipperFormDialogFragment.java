package com.utt.foodcouriers_admin.ui.shipper.dialog;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;

import com.utt.foodcouriers_admin.utils.ToastBanner;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Restaurant;
import com.utt.foodcouriers_admin.data.model.Shipper;
import com.utt.foodcouriers_admin.data.repository.StorageRepository;
import com.utt.foodcouriers_admin.data.request.ShipperUpsertRequest;
import com.utt.foodcouriers_admin.ui.common.dialog.ImageZoomDialogFragment;
import java.util.List;

public class ShipperFormDialogFragment extends DialogFragment {

    public interface ShipperFormListener {
        void onSubmit(@Nullable String shipperId, ShipperUpsertRequest request, ShipperFormDialogFragment dialog);
    }

    private static final String ARG_SHIPPER = "arg_shipper";
    private static final String ARG_RESTAURANTS = "arg_restaurants";

    public static ShipperFormDialogFragment newInstance(@Nullable Shipper shipper, @Nullable List<Restaurant> restaurants) {
        ShipperFormDialogFragment fragment = new ShipperFormDialogFragment();
        Bundle bundle = new Bundle();
        if (shipper != null) {
            bundle.putSerializable(ARG_SHIPPER, shipper);
        }
        if (restaurants != null) {
            bundle.putSerializable(ARG_RESTAURANTS, new java.util.ArrayList<>(restaurants));
        }
        fragment.setArguments(bundle);
        return fragment;
    }

    private Shipper shipper;
    private List<Restaurant> restaurants;
    private ShipperFormListener listener;
    private StorageRepository storageRepository;
    private Uri selectedImageUri;
    private boolean isUploading = false;

    private TextInputLayout tilName;
    private TextInputLayout tilPhone;
    private TextInputLayout tilEmail;
    private TextInputLayout tilImage;
    private TextInputLayout tilRestaurant;
    private TextInputEditText etName;
    private TextInputEditText etPhone;
    private TextInputEditText etEmail;
    private TextInputEditText etImage;
    private AutoCompleteTextView etRestaurant;
    private MaterialSwitch switchActive;
    private MaterialButton btnSave;
    private MaterialButton btnCancel;
    private MaterialButton btnChooseImage;
    private CircularProgressIndicator progressSave;
    private ImageView ivPreview;
    private TextView tvStatusHelper;
    private TextView tvPreviewName;
    private TextView tvPreviewPhone;
    private TextView tvFormTitle;
    private TextView tvFormSubtitle;

    private Restaurant selectedRestaurant;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    uploadSelectedImage();
                }
            }
    );

    private final TextWatcher previewWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updatePreview();
        }

        @Override
        public void afterTextChanged(Editable s) { }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog_Alert);
        if (getArguments() != null) {
            shipper = (Shipper) getArguments().getSerializable(ARG_SHIPPER);
            restaurants = (List<Restaurant>) getArguments().getSerializable(ARG_RESTAURANTS);
        }
        storageRepository = StorageRepository.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_shipper_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRestaurantDropdown();
        bindShipper();
        setupListeners();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (etName != null) {
            etName.removeTextChangedListener(previewWatcher);
        }
        if (etPhone != null) {
            etPhone.removeTextChangedListener(previewWatcher);
        }
        if (etEmail != null) {
            etEmail.removeTextChangedListener(previewWatcher);
        }
    }

    public void setShipperFormListener(ShipperFormListener listener) {
        this.listener = listener;
    }

    public void setLoading(boolean loading) {
        if (btnSave != null) {
            btnSave.setEnabled(!loading && !isUploading);
            btnSave.setText(loading ? getString(R.string.shipper_saving) : getString(R.string.action_save));
        }
        if (progressSave != null) {
            progressSave.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (btnCancel != null) {
            btnCancel.setEnabled(!loading);
        }
        if (btnChooseImage != null) {
            btnChooseImage.setEnabled(!loading && !isUploading);
        }
    }

    private void initViews(View view) {
        tilName = view.findViewById(R.id.til_name);
        tilPhone = view.findViewById(R.id.til_phone);
        tilEmail = view.findViewById(R.id.til_email);
        tilImage = view.findViewById(R.id.til_image);
        tilRestaurant = view.findViewById(R.id.til_restaurant);
        etName = view.findViewById(R.id.et_name);
        etPhone = view.findViewById(R.id.et_phone);
        etEmail = view.findViewById(R.id.et_email);
        etImage = view.findViewById(R.id.et_image);
        etRestaurant = view.findViewById(R.id.et_restaurant);
        switchActive = view.findViewById(R.id.switch_active);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnChooseImage = view.findViewById(R.id.btn_choose_image);
        progressSave = view.findViewById(R.id.progress_save);
        ivPreview = view.findViewById(R.id.iv_form_avatar);
        tvStatusHelper = view.findViewById(R.id.tv_status_helper);
        tvPreviewName = view.findViewById(R.id.tv_preview_name);
        tvPreviewPhone = view.findViewById(R.id.tv_preview_phone);
        tvFormTitle = view.findViewById(R.id.tv_form_title);
        tvFormSubtitle = view.findViewById(R.id.tv_form_subtitle);
    }

    private void setupRestaurantDropdown() {
        if (restaurants == null || restaurants.isEmpty()) {
            return;
        }
        String[] restaurantNames = new String[restaurants.size()];
        for (int i = 0; i < restaurants.size(); i++) {
            restaurantNames[i] = restaurants.get(i).getName();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, restaurantNames);
        etRestaurant.setAdapter(adapter);
        etRestaurant.setOnItemClickListener((parent, view, position, id) -> {
            selectedRestaurant = restaurants.get(position);
        });
    }

    private void bindShipper() {
        boolean isEdit = shipper != null && !TextUtils.isEmpty(shipper.getId());
        tvFormTitle.setText(isEdit ? R.string.dialog_shipper_title_edit : R.string.dialog_shipper_title_create);
        tvFormSubtitle.setText(isEdit ? shipper.getFullName() : getString(R.string.label_shipper_info));
        switchActive.setChecked(isEdit ? shipper.isActive() : true);
        if (isEdit) {
            etName.setText(shipper.getFullName());
            etPhone.setText(shipper.getPhone());
            etEmail.setText(shipper.getEmail());
            etImage.setText(shipper.getAvatarUrl());
            loadPreviewImage(shipper.getAvatarUrl());
            if (!TextUtils.isEmpty(shipper.getRestaurantId()) && restaurants != null) {
                for (int i = 0; i < restaurants.size(); i++) {
                    if (restaurants.get(i).getId().equals(shipper.getRestaurantId())) {
                        selectedRestaurant = restaurants.get(i);
                        etRestaurant.setText(restaurants.get(i).getName(), false);
                        break;
                    }
                }
            }
        }
        tvStatusHelper.setText(switchActive.isChecked() ? R.string.label_active_vi_en : R.string.label_inactive_vi_en);
        updatePreview();
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> handleSubmit());
        btnCancel.setOnClickListener(v -> dismiss());
        btnChooseImage.setOnClickListener(v -> openImagePicker());
        ivPreview.setOnClickListener(v -> handleImageClick());

        etName.addTextChangedListener(previewWatcher);
        etPhone.addTextChangedListener(previewWatcher);
        etEmail.addTextChangedListener(previewWatcher);
        etImage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadPreviewImage(s != null ? s.toString() : null);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        switchActive.setOnCheckedChangeListener((buttonView, isChecked) ->
                tvStatusHelper.setText(isChecked ? R.string.label_active_vi_en : R.string.label_inactive_vi_en));
    }

    private void handleImageClick() {
        String currentUrl = etImage.getText() != null ? etImage.getText().toString().trim() : null;
        if (!TextUtils.isEmpty(currentUrl)) {
            ImageZoomDialogFragment.newInstance(currentUrl).show(getParentFragmentManager(), "ImageZoomDialog");
        }
    }

    private void openImagePicker() {
        pickImageLauncher.launch("image/*");
    }

    private void uploadSelectedImage() {
        if (selectedImageUri == null) return;

        isUploading = true;
        setLoading(false);
        ToastBanner.showWarning(getString(R.string.toast_uploading));

        storageRepository.uploadImage(requireContext(), selectedImageUri, "shippers", new RepositoryCallback<String>() {
            @Override
            public void onComplete(BaseResponse<String> response) {
                isUploading = false;
                if (response.isSuccess()) {
                    String imageUrl = response.getData();
                    etImage.setText(imageUrl);
                    loadPreviewImage(imageUrl);
                    ToastBanner.showSuccess(getString(R.string.toast_upload_success));
                } else {
                    String errorMsg = response.getMessage();
                    ToastBanner.showError(getString(R.string.toast_upload_failed, errorMsg));
                }
                setLoading(false);
            }
        });
    }

    private void handleSubmit() {
        if (!validate()) {
            return;
        }
        if (isUploading) {
            ToastBanner.showWarning(getString(R.string.toast_uploading));
            return;
        }
        String name = etName.getText() != null ? etName.getText().toString().trim() : null;
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : null;
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : null;
        String imageUrl = etImage.getText() != null ? etImage.getText().toString().trim() : null;
        String restaurantId = selectedRestaurant != null ? selectedRestaurant.getId() : null;
        ShipperUpsertRequest request = new ShipperUpsertRequest(null, restaurantId, name, phone, email, imageUrl, switchActive.isChecked());
        if (listener != null) {
            listener.onSubmit(shipper != null ? shipper.getId() : null, request, this);
        }
    }

    private boolean validate() {
        boolean isValid = true;
        tilName.setError(null);
        tilPhone.setError(null);
        tilRestaurant.setError(null);
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        if (TextUtils.isEmpty(name)) {
            tilName.setError(getString(R.string.error_field_required));
            isValid = false;
        }
        if (selectedRestaurant == null) {
            tilRestaurant.setError(getString(R.string.error_field_required));
            isValid = false;
        }
        return isValid;
    }

    private void updatePreview() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        tvPreviewName.setText(TextUtils.isEmpty(name) ? getString(R.string.label_shipper_name) : name);
        tvPreviewPhone.setText(TextUtils.isEmpty(phone) ? getString(R.string.label_phone) : phone);
    }

    private void loadPreviewImage(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            ivPreview.setImageResource(R.drawable.ic_shipper);
            return;
        }
        Glide.with(ivPreview.getContext())
                .load(url)
                .placeholder(R.drawable.ic_shipper)
                .error(R.drawable.ic_shipper)
                .centerCrop()
                .into(ivPreview);
    }
}
