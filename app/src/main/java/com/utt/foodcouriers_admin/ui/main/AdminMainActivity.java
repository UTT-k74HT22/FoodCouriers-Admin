package com.utt.foodcouriers_admin.ui.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.AdminProfile;
import com.utt.foodcouriers_admin.databinding.ActivityAdminMainBinding;
import com.utt.foodcouriers_admin.ui.auth.AdminLoginActivity;
import com.utt.foodcouriers_admin.utils.SessionManager;

public class AdminMainActivity extends AppCompatActivity {

    private ActivityAdminMainBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        if (!sessionManager.hasSession()) {
            openLogin();
            return;
        }

        bindProfile(sessionManager.getProfile());
        binding.logoutButton.setOnClickListener(v -> {
            sessionManager.clearSession();
            openLogin();
        });
    }

    private void bindProfile(AdminProfile profile) {
        if (profile == null) {
            openLogin();
            return;
        }

        String name = profile.getDisplayName();
        binding.welcomeTextView.setText(getString(R.string.main_welcome_title));
        binding.subtitleTextView.setText(
                "Xin chào, " + name + ". Bạn đang đăng nhập với quyền " + safeValue(profile.getRole()) + "."
        );
        binding.profileNameTextView.setText(name);
        binding.profileRoleTextView.setText(
                getString(R.string.label_role) + ": " + safeValue(profile.getRole())
        );
        binding.profileEmailTextView.setText(
                getString(R.string.label_email_compact) + ": " + safeValue(profile.getEmail())
        );
        binding.profileDepartmentTextView.setText(
                getString(R.string.label_department) + ": " + safeValue(profile.getDepartment())
        );
        binding.profilePhoneTextView.setText(
                getString(R.string.label_phone) + ": " + safeValue(profile.getPhone())
        );
    }

    private String safeValue(String value) {
        return value == null || value.trim().isEmpty()
                ? getString(R.string.value_unavailable)
                : value;
    }

    private void openLogin() {
        Intent intent = new Intent(this, AdminLoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
