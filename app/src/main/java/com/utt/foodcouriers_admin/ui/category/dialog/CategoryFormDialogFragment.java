package com.utt.foodcouriers_admin.ui.category.dialog;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.utt.foodcouriers_admin.data.model.Category;
import com.utt.foodcouriers_admin.data.repository.StorageRepository;
import com.utt.foodcouriers_admin.data.request.CategoryUpsertRequest;
import com.utt.foodcouriers_admin.ui.common.dialog.ImageZoomDialogFragment;

public class CategoryFormDialogFragment extends DialogFragment {

    public interface CategoryFormListener {
        void onSubmit(@Nullable String categoryId, CategoryUpsertRequest request, CategoryFormDialogFragment dialog);
    }

    private static final String ARG_CATEGORY = "arg_category";
    private static final int PICK_IMAGE_REQUEST = 1001;

    public static CategoryFormDialogFragment newInstance(@Nullable Category category) {
        CategoryFormDialogFragment fragment = new CategoryFormDialogFragment();
        Bundle bundle = new Bundle();
        if (category != null) {
            bundle.putSerializable(ARG_CATEGORY, category);
        }
        fragment.setArguments(bundle);
        return fragment;
    }

    private Category category;
    private CategoryFormListener listener;
    private StorageRepository storageRepository;
    private Uri selectedImageUri;
    private boolean isUploading = false;

    private TextInputLayout tilName;
    private TextInputLayout tilSortOrder;
    private TextInputEditText etName;

    private TextInputEditText etImage;
    private TextInputEditText etSortOrder;
    private MaterialSwitch switchActive;
    private MaterialButton btnSave;
    private MaterialButton btnCancel;
    private MaterialButton btnChooseImage;
    private CircularProgressIndicator progressSave;
    private ImageView ivPreview;
    private TextView tvStatusHelper;
    private TextView tvPreviewName;
    private TextView tvPreviewDescription;
    private TextView tvFormTitle;
    private TextView tvFormSubtitle;

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
            category = (Category) getArguments().getSerializable(ARG_CATEGORY);
        }
        storageRepository = StorageRepository.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_category_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        bindCategory();
        setupListeners();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (etName != null) {
            etName.removeTextChangedListener(previewWatcher);
        }

        if (etImage != null) {
            etImage.removeTextChangedListener(previewWatcher);
        }
    }

    public void setCategoryFormListener(CategoryFormListener listener) {
        this.listener = listener;
    }

    public void setLoading(boolean loading) {
        if (btnSave != null) {
            btnSave.setEnabled(!loading && !isUploading);
            btnSave.setText(loading ? getString(R.string.category_saving) : getString(R.string.action_save));
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
        tilSortOrder = view.findViewById(R.id.til_sort_order);
        etName = view.findViewById(R.id.et_name);
        etImage = view.findViewById(R.id.et_image);
        etSortOrder = view.findViewById(R.id.et_sort_order);
        switchActive = view.findViewById(R.id.switch_active);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnChooseImage = view.findViewById(R.id.btn_choose_image);
        progressSave = view.findViewById(R.id.progress_save);
        ivPreview = view.findViewById(R.id.iv_form_image);
        tvStatusHelper = view.findViewById(R.id.tv_status_helper);
        tvPreviewName = view.findViewById(R.id.tv_preview_name);
        tvPreviewDescription = view.findViewById(R.id.tv_preview_description);
        tvFormTitle = view.findViewById(R.id.tv_form_title);
        tvFormSubtitle = view.findViewById(R.id.tv_form_subtitle);
    }

    private void bindCategory() {
        boolean isEdit = category != null && !TextUtils.isEmpty(category.getId());
        tvFormTitle.setText(isEdit ? R.string.dialog_category_title_edit : R.string.dialog_category_title_create);
        tvFormSubtitle.setText(isEdit ? category.getName() : getString(R.string.label_name_vi_en));
        switchActive.setChecked(isEdit ? category.isActive() : true);
        if (isEdit) {
            etName.setText(category.getName());
            etImage.setText(category.getImageUrl());
            etSortOrder.setText(String.valueOf(category.getSortOrder()));
            loadPreviewImage(category.getImageUrl());
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

        storageRepository.uploadImage(requireContext(), selectedImageUri, "categories", new RepositoryCallback<String>() {
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
        String imageUrl = etImage.getText() != null ? etImage.getText().toString().trim() : null;
        Integer sortOrder = parseSortOrder();
        CategoryUpsertRequest request = new CategoryUpsertRequest(name, imageUrl, sortOrder, switchActive.isChecked());
        if (listener != null) {
            listener.onSubmit(category != null ? category.getId() : null, request, this);
        }
    }

    private boolean validate() {
        boolean isValid = true;
        tilName.setError(null);
        tilSortOrder.setError(null);
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        if (TextUtils.isEmpty(name)) {
            tilName.setError(getString(R.string.error_field_required));
            isValid = false;
        }
        String sortOrderValue = etSortOrder.getText() != null ? etSortOrder.getText().toString().trim() : "";
        Integer sortOrder = parseSortOrder();
        if (!TextUtils.isEmpty(sortOrderValue) && sortOrder == null) {
            tilSortOrder.setError(getString(R.string.error_generic));
            isValid = false;
        }
        if (sortOrder != null && sortOrder < 0) {
            tilSortOrder.setError(getString(R.string.error_generic));
            isValid = false;
        }
        return isValid;
    }

    private Integer parseSortOrder() {
        if (etSortOrder.getText() == null) {
            return null;
        }
        String value = etSortOrder.getText().toString().trim();
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            tilSortOrder.setError(getString(R.string.error_generic));
            return null;
        }
    }

    private void updatePreview() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        tvPreviewName.setText(TextUtils.isEmpty(name) ? getString(R.string.label_name_vi_en) : name);
        tvPreviewDescription.setText("");
    }

    private void loadPreviewImage(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            ivPreview.setImageResource(R.drawable.ic_category);
            return;
        }
        Glide.with(ivPreview.getContext())
                .load(url)
                .placeholder(R.drawable.ic_category)
                .error(R.drawable.ic_category)
                .centerCrop()
                .into(ivPreview);
    }
}
