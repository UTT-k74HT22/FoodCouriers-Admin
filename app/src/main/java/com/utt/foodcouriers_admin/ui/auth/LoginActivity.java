package com.utt.foodcouriers_admin.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.User;
import com.utt.foodcouriers_admin.data.repository.AuthRepository;
import com.utt.foodcouriers_admin.ui.main.MainActivity;
import com.utt.foodcouriers_admin.utils.ToastBanner;
import com.utt.foodcouriers_admin.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_LOGIN_SUCCESS_MESSAGE = "extra_login_success_message";

    private static final String TEST_ADMIN_EMAIL = "admin@foodcouriers.com";
    private static final String LEGACY_ADMIN_EMAIL = "admin@appfood.local";
    private static final String LEGACY_ADMIN_USERNAME = "admin";

    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private Button btnLogin;
    private TextView tvSwitchToRegister;
    private TextView tvForgotPassword;
    private View loadingOverlay;

    private AuthRepository authRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force Light Mode
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        
        super.onCreate(savedInstanceState);

        sessionManager = SessionManager.getInstance(this);
        if (sessionManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        authRepository = AuthRepository.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSwitchToRegister = findViewById(R.id.tvSwitchToRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        tilEmail.setHelperText("Test admin: " + TEST_ADMIN_EMAIL);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> {
            if (validateInput()) {
                performLogin();
            }
        });

        tvSwitchToRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );

        tvForgotPassword.setOnClickListener(v ->
                ToastBanner.showError("Liên hệ quản trị viên để đặt lại mật khẩu")
        );
    }

    private boolean validateInput() {
        String email = normalizeLoginIdentifier(String.valueOf(etEmail.getText()));
        String password = String.valueOf(etPassword.getText()).trim();

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            return false;
        }
        tilEmail.setError(null);

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Invalid email format");
            return false;
        }
        tilEmail.setError(null);

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            return false;
        }
        tilPassword.setError(null);

        if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            return false;
        }
        tilPassword.setError(null);

        return true;
    }

    private void performLogin() {
        String originalInput = String.valueOf(etEmail.getText()).trim();
        String email = normalizeLoginIdentifier(originalInput);
        String password = String.valueOf(etPassword.getText()).trim();

        if (!email.equalsIgnoreCase(originalInput)) {
            etEmail.setText(email);
        }

        showLoading(true);

        authRepository.login(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                showLoading(false);

                if (!user.isActive()) {
                    authRepository.logout(() -> {});
                    ToastBanner.showError("Tài khoản của bạn đang bị khóa.");
                    return;
                }

                if (user.isAdmin() || user.isStaff()) {
                    String accessToken = com.utt.foodcouriers_admin.data.remote.AuthClient.getInstance().getAccessToken();
                    String refreshToken = com.utt.foodcouriers_admin.data.remote.AuthClient.getInstance().getRefreshToken();

                    sessionManager.saveSession(
                            accessToken,
                            refreshToken,
                            user
                    );

                    // Sync session to all clients
                    com.utt.foodcouriers_admin.data.remote.SupabaseClientManager.updateAllClients(accessToken, refreshToken);

                    navigateToMain("Chào mừng bạn quay lại hệ thống.");
                    return;
                }

                authRepository.logout(() -> {});
                ToastBanner.showError("Bạn không có quyền truy cập hệ thống quản trị.");
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                ToastBanner.showError(error);
            }
        });
    }

    private String normalizeLoginIdentifier(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.equalsIgnoreCase(LEGACY_ADMIN_EMAIL)
                || normalized.equalsIgnoreCase(LEGACY_ADMIN_USERNAME)) {
            return TEST_ADMIN_EMAIL;
        }

        return normalized;
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnLogin.setEnabled(!show);
        btnLogin.setText(show ? "Loading..." : "Sign In");
    }

    private void navigateToMain() {
        navigateToMain(null);
    }

    private void navigateToMain(String successMessage) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        if (successMessage != null && !successMessage.isBlank()) {
            intent.putExtra(EXTRA_LOGIN_SUCCESS_MESSAGE, successMessage);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
