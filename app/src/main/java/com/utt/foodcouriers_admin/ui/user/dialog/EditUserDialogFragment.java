package com.utt.foodcouriers_admin.ui.user.dialog;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.User;
import com.utt.foodcouriers_admin.data.repository.StorageRepository;
import com.utt.foodcouriers_admin.data.repository.UserRepository;
import com.utt.foodcouriers_admin.data.request.UserUpdateRequest;

public class EditUserDialogFragment extends DialogFragment {

    public interface EditUserListener {
        void onUserUpdated();
    }

    private static final String ARG_USER_JSON = "user_json";

    private TextInputLayout tilFullName;
    private TextInputLayout tilPhone;
    private TextInputLayout tilRole;
    private TextInputEditText etFullName;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;
    private AutoCompleteTextView actRole;
    private MaterialSwitch switchActive;
    private MaterialButton btnSave;
    private MaterialButton btnCancel;
    private MaterialButton btnChooseAvatar;
    private CircularProgressIndicator progressSave;
    private CircularProgressIndicator progressAvatar;
    private ShapeableImageView ivAvatarPreview;

    private User user;
    private EditUserListener listener;
    private StorageRepository storageRepository;
    private UserRepository userRepository;
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

    public static EditUserDialogFragment newInstance(User user) {
        EditUserDialogFragment fragment = new EditUserDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_JSON, new Gson().toJson(user));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_FoodCouriersAdmin_Dialog);
        storageRepository = StorageRepository.getInstance();
        userRepository = UserRepository.getInstance();

        if (getArguments() != null) {
            String userJson = getArguments().getString(ARG_USER_JSON);
            if (!TextUtils.isEmpty(userJson)) {
                user = new Gson().fromJson(userJson, User.class);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_edit_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRoleDropdown();
        populateUserData();
        setupActions();
    }

    public void setEditUserListener(EditUserListener listener) {
        this.listener = listener;
    }

    private void initViews(View view) {
        tilFullName = view.findViewById(R.id.til_user_full_name);
        tilPhone = view.findViewById(R.id.til_user_phone);
        tilRole = view.findViewById(R.id.til_user_role);
        etFullName = view.findViewById(R.id.et_user_full_name);
        etEmail = view.findViewById(R.id.et_user_email);
        etPhone = view.findViewById(R.id.et_user_phone);
        actRole = view.findViewById(R.id.act_user_role);
        switchActive = view.findViewById(R.id.switch_user_active);
        btnSave = view.findViewById(R.id.btn_save_user);
        btnCancel = view.findViewById(R.id.btn_cancel_edit_user);
        btnChooseAvatar = view.findViewById(R.id.btn_choose_avatar);
        progressSave = view.findViewById(R.id.progress_save_user);
        progressAvatar = view.findViewById(R.id.progress_avatar);
        ivAvatarPreview = view.findViewById(R.id.iv_avatar_preview);
    }

    private void setupRoleDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                new String[]{getString(R.string.user_role_client), getString(R.string.user_role_staff)}
        );
        actRole.setAdapter(adapter);
    }

    private void populateUserData() {
        if (user == null) return;

        etFullName.setText(user.getFullName());
        etEmail.setText(user.getEmail());
        etPhone.setText(user.getPhone());
        switchActive.setChecked(user.isActive());

        if ("staff".equalsIgnoreCase(user.getRole())) {
            actRole.setText(getString(R.string.user_role_staff), false);
        } else {
            actRole.setText(getString(R.string.user_role_client), false);
        }

        currentAvatarUrl = user.getAvatarUrl();
        loadAvatarPreview(currentAvatarUrl);
    }

    private void setupActions() {
        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> submit());
        btnChooseAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    private void uploadSelectedAvatar() {
        if (selectedAvatarUri == null) return;

        isUploading = true;
        setLoading(false);
        if (progressAvatar != null) progressAvatar.setVisibility(View.VISIBLE);
        if (ivAvatarPreview != null) ivAvatarPreview.setAlpha(0.4f);

        storageRepository.uploadImage(requireContext(), selectedAvatarUri, "avatars", new RepositoryCallback<String>() {
            @Override
            public void onComplete(BaseResponse<String> response) {
                isUploading = false;
                if (progressAvatar != null) progressAvatar.setVisibility(View.GONE);
                if (ivAvatarPreview != null) ivAvatarPreview.setAlpha(1.0f);

                if (!isAdded()) return;

                if (response.isSuccess()) {
                    currentAvatarUrl = response.getData();
                    loadAvatarPreview(currentAvatarUrl);
                    Toast.makeText(requireContext(), R.string.toast_upload_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), getString(R.string.toast_upload_failed, response.getMessage()), Toast.LENGTH_LONG).show();
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

    public void setLoading(boolean loading) {
        if (btnSave != null) {
            btnSave.setEnabled(!loading && !isUploading);
            btnSave.setText(loading ? getString(R.string.user_action_saving) : getString(R.string.user_action_save_changes));
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

    private void submit() {
        if (!validate()) return;
        if (isUploading) {
            Toast.makeText(requireContext(), R.string.toast_uploading, Toast.LENGTH_SHORT).show();
            return;
        }

        String uiRole = actRole.getText() != null ? actRole.getText().toString().trim() : "";
        String mappedRole = getString(R.string.user_role_staff).equals(uiRole) ? "staff" : "customer";

        UserUpdateRequest request = new UserUpdateRequest();
        request.setFullName(etFullName.getText() != null ? etFullName.getText().toString().trim() : null);
        request.setPhone(etPhone.getText() != null ? etPhone.getText().toString().trim() : null);
        request.setAvatarUrl(currentAvatarUrl);
        request.setRole(mappedRole);
        request.setIsActive(switchActive.isChecked());

        setLoading(true);

        userRepository.update(user.getId(), request, new RepositoryCallback<User>() {
            @Override
            public void onComplete(BaseResponse<User> response) {
                if (!isAdded()) return;
                setLoading(false);

                if (!response.isSuccess()) {
                    Toast.makeText(requireContext(), response.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(requireContext(), R.string.user_update_success, Toast.LENGTH_SHORT).show();
                dismissAllowingStateLoss();

                if (listener != null) {
                    listener.onUserUpdated();
                }
            }
        });
    }

    private boolean validate() {
        tilFullName.setError(null);
        tilPhone.setError(null);
        tilRole.setError(null);

        boolean valid = true;

        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String role = actRole.getText() != null ? actRole.getText().toString().trim() : "";

        if (TextUtils.isEmpty(fullName)) {
            tilFullName.setError(getString(R.string.error_field_required));
            valid = false;
        }
        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError(getString(R.string.error_field_required));
            valid = false;
        }
        if (TextUtils.isEmpty(role)) {
            tilRole.setError(getString(R.string.error_field_required));
            valid = false;
        }

        return valid;
    }
}
