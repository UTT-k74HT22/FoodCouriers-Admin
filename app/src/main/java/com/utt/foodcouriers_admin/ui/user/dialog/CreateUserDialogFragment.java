package com.utt.foodcouriers_admin.ui.user.dialog;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

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
import com.utt.foodcouriers_admin.data.repository.StorageRepository;
import com.utt.foodcouriers_admin.data.request.AdminCreateUserAccountRequest;

public class CreateUserDialogFragment extends DialogFragment {

    public interface CreateUserListener {
        void onSubmit(AdminCreateUserAccountRequest request, String avatarUrl, CreateUserDialogFragment dialog);
    }

    private TextInputLayout tilFullName;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPhone;
    private TextInputLayout tilPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputLayout tilRole;
    private TextInputEditText etFullName;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private AutoCompleteTextView actRole;
    private MaterialSwitch switchActive;
    private MaterialButton btnSave;
    private MaterialButton btnCancel;
    private MaterialButton btnChooseAvatar;
    private CircularProgressIndicator progressSave;
    private CircularProgressIndicator progressAvatar;
    private ImageView ivAvatarPreview;

    private CreateUserListener listener;
    private StorageRepository storageRepository;
    private Uri selectedAvatarUri;
    private String currentAvatarUrl;
    private boolean isUploading = false;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedAvatarUri = uri;
                    uploadSelectedAvatar();
                }
            }
    );

    public static CreateUserDialogFragment newInstance() {
        return new CreateUserDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_FoodCouriersAdmin_Dialog);
        storageRepository = StorageRepository.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_create_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRoleDropdown();
        setupActions();
    }

    public void setCreateUserListener(CreateUserListener listener) {
        this.listener = listener;
    }

    public void setLoading(boolean loading) {
        if (btnSave != null) {
            btnSave.setEnabled(!loading && !isUploading);
            btnSave.setText(loading ? getString(R.string.user_action_creating) : getString(R.string.user_action_create_account));
        }
        if (progressSave != null) {
            progressSave.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (btnCancel != null) {
            btnCancel.setEnabled(!loading);
        }
        if (btnChooseAvatar != null) {
            btnChooseAvatar.setEnabled(!loading && !isUploading);
        }
    }

    private void initViews(View view) {
        tilFullName = view.findViewById(R.id.til_user_full_name);
        tilEmail = view.findViewById(R.id.til_user_email);
        tilPhone = view.findViewById(R.id.til_user_phone);
        tilPassword = view.findViewById(R.id.til_user_password);
        tilConfirmPassword = view.findViewById(R.id.til_user_confirm_password);
        tilRole = view.findViewById(R.id.til_user_role);
        etFullName = view.findViewById(R.id.et_user_full_name);
        etEmail = view.findViewById(R.id.et_user_email);
        etPhone = view.findViewById(R.id.et_user_phone);
        etPassword = view.findViewById(R.id.et_user_password);
        etConfirmPassword = view.findViewById(R.id.et_user_confirm_password);
        actRole = view.findViewById(R.id.act_user_role);
        switchActive = view.findViewById(R.id.switch_user_active);
        btnSave = view.findViewById(R.id.btn_create_user);
        btnCancel = view.findViewById(R.id.btn_cancel_create_user);
        btnChooseAvatar = view.findViewById(R.id.btn_choose_avatar);
        progressSave = view.findViewById(R.id.progress_create_user);
        progressAvatar = view.findViewById(R.id.progress_avatar);
        ivAvatarPreview = view.findViewById(R.id.iv_avatar_preview);
        switchActive.setChecked(true);
    }

    private void setupRoleDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                new String[]{getString(R.string.user_role_client), getString(R.string.user_role_staff)}
        );
        actRole.setAdapter(adapter);
    }

    private void setupActions() {
        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> submit());
        btnChooseAvatar.setOnClickListener(v -> openImagePicker());
    }

    private void openImagePicker() {
        pickImageLauncher.launch("image/*");
    }

    private void uploadSelectedAvatar() {
        if (selectedAvatarUri == null) return;

        isUploading = true;
        setLoading(false);
        Toast.makeText(requireContext(), R.string.toast_uploading, Toast.LENGTH_SHORT).show();

        storageRepository.uploadImage(requireContext(), selectedAvatarUri, "avatars", new RepositoryCallback<String>() {
            @Override
            public void onComplete(BaseResponse<String> response) {
                isUploading = false;
                if (response.isSuccess()) {
                    currentAvatarUrl = response.getData();
                    loadAvatarPreview(currentAvatarUrl);
                    Toast.makeText(requireContext(), R.string.toast_upload_success, Toast.LENGTH_SHORT).show();
                } else {
                    String errorMsg = response.getMessage();
                    Toast.makeText(requireContext(), getString(R.string.toast_upload_failed, errorMsg), Toast.LENGTH_LONG).show();
                }
                setLoading(false);
            }
        });
    }

    private void loadAvatarPreview(@Nullable String url) {
        if (ivAvatarPreview == null) return;
        
        if (TextUtils.isEmpty(url)) {
            ivAvatarPreview.setImageResource(R.drawable.ic_person);
            return;
        }
        Glide.with(ivAvatarPreview.getContext())
                .load(url)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .centerCrop()
                .into(ivAvatarPreview);
    }

    private void submit() {
        if (!validate()) {
            return;
        }
        if (isUploading) {
            Toast.makeText(requireContext(), R.string.toast_uploading, Toast.LENGTH_SHORT).show();
            return;
        }
        String uiRole = valueOf(etOrEmpty(actRole));
        String mappedRole = getString(R.string.user_role_staff).equals(uiRole) ? "staff" : "customer";
        AdminCreateUserAccountRequest request = new AdminCreateUserAccountRequest(
                valueOf(etOrEmpty(etEmail)),
                valueOf(etOrEmpty(etPassword)),
                valueOf(etOrEmpty(etFullName)),
                valueOf(etOrEmpty(etPhone)),
                currentAvatarUrl,
                mappedRole,
                switchActive.isChecked(),
                true
        );
        if (listener != null) {
            listener.onSubmit(request, currentAvatarUrl, this);
        }
    }

    private boolean validate() {
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        tilRole.setError(null);

        boolean valid = true;
        String fullName = valueOf(etOrEmpty(etFullName));
        String email = valueOf(etOrEmpty(etEmail));
        String phone = valueOf(etOrEmpty(etPhone));
        String password = valueOf(etOrEmpty(etPassword));
        String confirmPassword = valueOf(etOrEmpty(etConfirmPassword));
        String role = valueOf(etOrEmpty(actRole));

        if (TextUtils.isEmpty(fullName)) {
            tilFullName.setError(getString(R.string.error_field_required));
            valid = false;
        }
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_field_required));
            valid = false;
        }
        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError(getString(R.string.error_field_required));
            valid = false;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_field_required));
            valid = false;
        } else if (password.length() < 6) {
            tilPassword.setError(getString(R.string.user_error_password_min));
            valid = false;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_field_required));
            valid = false;
        } else if (!TextUtils.equals(password, confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.user_error_password_mismatch));
            valid = false;
        }
        if (TextUtils.isEmpty(role)) {
            tilRole.setError(getString(R.string.error_field_required));
            valid = false;
        }
        return valid;
    }

    private CharSequence etOrEmpty(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private CharSequence etOrEmpty(AutoCompleteTextView editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String valueOf(CharSequence value) {
        return value == null ? null : value.toString().trim();
    }
}
