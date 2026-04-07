package com.utt.foodcouriers_admin.ui.user.dialog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.request.AdminCreateUserAccountRequest;

public class CreateUserDialogFragment extends DialogFragment {

    public interface CreateUserListener {
        void onSubmit(AdminCreateUserAccountRequest request, CreateUserDialogFragment dialog);
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
    private TextInputEditText etAvatarUrl;
    private AutoCompleteTextView actRole;
    private MaterialSwitch switchActive;
    private MaterialButton btnSave;
    private MaterialButton btnCancel;
    private CircularProgressIndicator progressSave;

    private CreateUserListener listener;

    public static CreateUserDialogFragment newInstance() {
        return new CreateUserDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog_Alert);
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
        btnSave.setEnabled(!loading);
        btnCancel.setEnabled(!loading);
        progressSave.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setText(loading ? getString(R.string.user_action_creating) : getString(R.string.user_action_create_account));
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
        etAvatarUrl = view.findViewById(R.id.et_user_avatar_url);
        actRole = view.findViewById(R.id.act_user_role);
        switchActive = view.findViewById(R.id.switch_user_active);
        btnSave = view.findViewById(R.id.btn_create_user);
        btnCancel = view.findViewById(R.id.btn_cancel_create_user);
        progressSave = view.findViewById(R.id.progress_create_user);
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
    }

    private void submit() {
        if (!validate()) {
            return;
        }
        String uiRole = valueOf(etOrEmpty(actRole));
        String mappedRole = getString(R.string.user_role_staff).equals(uiRole) ? "staff" : "customer";
        AdminCreateUserAccountRequest request = new AdminCreateUserAccountRequest(
                valueOf(etOrEmpty(etEmail)),
                valueOf(etOrEmpty(etPassword)),
                valueOf(etOrEmpty(etFullName)),
                valueOf(etOrEmpty(etPhone)),
                valueOf(etOrEmpty(etAvatarUrl)),
                mappedRole,
                switchActive.isChecked(),
                true
        );
        if (listener != null) {
            listener.onSubmit(request, this);
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
