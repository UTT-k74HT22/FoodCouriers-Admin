package com.utt.foodcouriers_admin.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.databinding.ActivityAdminLoginBinding;
import com.utt.foodcouriers_admin.ui.main.AdminMainActivity;
import com.utt.foodcouriers_admin.viewmodel.LoginViewModel;

public class AdminLoginActivity extends AppCompatActivity {

    private ActivityAdminLoginBinding binding;
    private LoginViewModel loginViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loginViewModel = new ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())
        ).get(LoginViewModel.class);

        if (loginViewModel.hasActiveSession()) {
            openMainScreen();
            return;
        }

        setupForm();
        observeState();
    }

    private void setupForm() {
        String rememberedEmail = loginViewModel.getRememberedEmail();
        if (!rememberedEmail.isBlank()) {
            binding.emailEditText.setText(rememberedEmail);
            binding.rememberMeCheckBox.setChecked(true);
        }

        TextWatcher resetErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.emailLayout.setError(null);
                binding.passwordLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        binding.emailEditText.addTextChangedListener(resetErrorWatcher);
        binding.passwordEditText.addTextChangedListener(resetErrorWatcher);
        binding.loginButton.setOnClickListener(v -> attemptLogin());
        binding.passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            boolean isEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (actionId == EditorInfo.IME_ACTION_DONE || isEnter) {
                attemptLogin();
                return true;
            }
            return false;
        });
    }

    private void observeState() {
        loginViewModel.getUiState().observe(this, state -> {
            setLoading(state.isLoading());

            if (state.getMessage() != null && !state.getMessage().isBlank()) {
                Snackbar.make(binding.getRoot(), state.getMessage(), Snackbar.LENGTH_LONG).show();
            }

            if (state.getProfile() != null) {
                openMainScreen();
            }
        });
    }

    private void attemptLogin() {
        String email = binding.emailEditText.getText() != null
                ? binding.emailEditText.getText().toString().trim()
                : "";
        String password = binding.passwordEditText.getText() != null
                ? binding.passwordEditText.getText().toString()
                : "";

        if (!validate(email, password)) {
            return;
        }

        loginViewModel.signIn(email, password, binding.rememberMeCheckBox.isChecked());
    }

    private boolean validate(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            binding.emailLayout.setError(getString(R.string.error_email_required));
            binding.emailEditText.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError(getString(R.string.error_invalid_email));
            binding.emailEditText.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            binding.passwordLayout.setError(getString(R.string.error_password_required));
            binding.passwordEditText.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            binding.passwordLayout.setError(getString(R.string.error_password_length));
            binding.passwordEditText.requestFocus();
            return false;
        }

        return true;
    }

    private void setLoading(boolean loading) {
        binding.loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.loginButton.setEnabled(!loading);
        binding.emailEditText.setEnabled(!loading);
        binding.passwordEditText.setEnabled(!loading);
        binding.rememberMeCheckBox.setEnabled(!loading);
    }

    private void openMainScreen() {
        startActivity(new Intent(this, AdminMainActivity.class));
        finish();
    }
}
