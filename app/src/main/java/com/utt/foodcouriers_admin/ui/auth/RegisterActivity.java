package com.utt.foodcouriers_admin.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.utt.foodcouriers_admin.utils.ToastBanner;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.User;
import com.utt.foodcouriers_admin.data.repository.AuthRepository;
import com.utt.foodcouriers_admin.ui.main.MainActivity;
import com.utt.foodcouriers_admin.utils.SessionManager;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilEmail, tilPhone, tilPassword, tilConfirmPassword;
    private TextInputEditText etName, etEmail, etPhone, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvSwitchToLogin;
    private View loadingOverlay;
    
    private AuthRepository authRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authRepository = AuthRepository.getInstance();
        sessionManager = SessionManager.getInstance(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        tvSwitchToLogin = findViewById(R.id.tvSwitchToLogin);
        loadingOverlay = findViewById(R.id.loadingOverlay);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> {
            if (validateInput()) {
                performRegister();
            }
        });

        tvSwitchToLogin.setOnClickListener(v -> {
            finish();
        });
    }

    private boolean validateInput() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            tilName.setError("Full name is required");
            return false;
        } else if (name.length() < 2) {
            tilName.setError("Name is too short");
            return false;
        } else {
            tilName.setError(null);
        }

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Invalid email format");
            return false;
        } else {
            tilEmail.setError(null);
        }

        if (!TextUtils.isEmpty(phone)) {
            if (phone.length() < 10 || !phone.matches("^[0-9]+$")) {
                tilPhone.setError("Invalid phone number");
                return false;
            } else {
                tilPhone.setError(null);
            }
        } else {
            tilPhone.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            return false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            return false;
        } else {
            tilPassword.setError(null);
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Please confirm your password");
            return false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            return false;
        } else {
            tilConfirmPassword.setError(null);
        }

        return true;
    }

    private void performRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        showLoading(true);

        authRepository.register(name, email, phone, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                showLoading(false);
                
                String accessToken = com.utt.foodcouriers_admin.data.remote.AuthClient.getInstance().getAccessToken();
                String refreshToken = com.utt.foodcouriers_admin.data.remote.AuthClient.getInstance().getRefreshToken();
                
                sessionManager.saveSession(
                        accessToken,
                        refreshToken,
                        user,
                        com.utt.foodcouriers_admin.data.remote.AuthClient.getInstance().getCurrentExpiresInMillis()
                );
                
                // Sync session to all clients
                com.utt.foodcouriers_admin.data.remote.SupabaseClientManager.initializeClients(RegisterActivity.this);
                
                ToastBanner.showSuccess("Tài khoản đã được tạo thành công!");
                
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                ToastBanner.showError(error);
            }
        });
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnRegister.setEnabled(!show);
        btnRegister.setText(show ? "Creating Account..." : "Create Account");
    }
}
