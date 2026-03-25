package com.utt.foodcouriers_admin.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.AdminProfile;
import com.utt.foodcouriers_admin.data.model.AdminSession;
import com.utt.foodcouriers_admin.data.repository.AuthRepository;
import com.utt.foodcouriers_admin.utils.SessionManager;

public class LoginViewModel extends AndroidViewModel {

    private final MutableLiveData<LoginUiState> uiState = new MutableLiveData<>(LoginUiState.idle());
    private final AuthRepository authRepository = new AuthRepository();
    private final SessionManager sessionManager;

    public LoginViewModel(@NonNull Application application) {
        super(application);
        sessionManager = new SessionManager(application.getApplicationContext());
    }

    public LiveData<LoginUiState> getUiState() {
        return uiState;
    }

    public boolean hasActiveSession() {
        return sessionManager.hasSession();
    }

    public AdminSession getStoredSession() {
        return sessionManager.getSession();
    }

    public String getRememberedEmail() {
        return sessionManager.getRememberedEmail();
    }

    public void signIn(String email, String password, boolean rememberEmail) {
        if (!authRepository.hasValidConfiguration()) {
            uiState.setValue(LoginUiState.error(getApplication().getString(R.string.error_missing_config)));
            return;
        }

        uiState.setValue(LoginUiState.loading());
        authRepository.signIn(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(AdminSession session) {
                sessionManager.saveSession(session);
                if (rememberEmail) {
                    sessionManager.rememberEmail(email);
                } else {
                    sessionManager.clearRememberedEmail();
                }
                uiState.setValue(LoginUiState.success(session.getProfile()));
            }

            @Override
            public void onError(String message) {
                uiState.setValue(LoginUiState.error(message));
            }
        });
    }

    public void logout() {
        sessionManager.clearSession();
    }

    public static class LoginUiState {
        private final boolean loading;
        private final String message;
        private final AdminProfile profile;

        private LoginUiState(boolean loading, String message, AdminProfile profile) {
            this.loading = loading;
            this.message = message;
            this.profile = profile;
        }

        public static LoginUiState idle() {
            return new LoginUiState(false, null, null);
        }

        public static LoginUiState loading() {
            return new LoginUiState(true, null, null);
        }

        public static LoginUiState error(String message) {
            return new LoginUiState(false, message, null);
        }

        public static LoginUiState success(AdminProfile profile) {
            return new LoginUiState(false, null, profile);
        }

        public boolean isLoading() {
            return loading;
        }

        public String getMessage() {
            return message;
        }

        public AdminProfile getProfile() {
            return profile;
        }
    }
}
