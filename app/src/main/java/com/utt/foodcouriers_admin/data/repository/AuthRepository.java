package com.utt.foodcouriers_admin.data.repository;

import com.utt.foodcouriers_admin.data.model.User;
import com.utt.foodcouriers_admin.data.remote.AuthClient;

public class AuthRepository {
    
    private static AuthRepository instance;
    private final AuthClient authClient; // Use AuthClient
    
    private AuthRepository() {
        authClient = AuthClient.getInstance(); // Get instance of AuthClient
    }
    
    public static synchronized AuthRepository getInstance() {
        if (instance == null) {
            instance = new AuthRepository();
        }
        return instance;
    }
    
    public void login(String email, String password, AuthCallback callback) {
        if (email == null || email.trim().isEmpty()) {
            callback.onError("Email is required");
            return;
        }
        if (password == null || password.isEmpty()) {
            callback.onError("Password is required");
            return;
        }
        
        // Use AuthClient's signIn method
        authClient.signIn(email.trim(), password, new AuthClient.ApiCallback<User>() {
            @Override
            public void onSuccess(User user) {
                callback.onSuccess(user);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public void register(String name, String email, String phone, String password, AuthCallback callback) {
        if (name == null || name.trim().isEmpty()) {
            callback.onError("Full name is required");
            return;
        }
        if (email == null || email.trim().isEmpty()) {
            callback.onError("Email is required");
            return;
        }
        if (password == null || password.isEmpty()) {
            callback.onError("Password is required");
            return;
        }
        if (password.length() < 6) {
            callback.onError("Password must be at least 6 characters");
            return;
        }
        
        // Use AuthClient's signUp method
        authClient.signUp(email.trim(), password, name.trim(), phone, new AuthClient.ApiCallback<User>() {
            @Override
            public void onSuccess(User user) {
                callback.onSuccess(user);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public void logout(LogoutCallback callback) {
        // Use AuthClient's signOut method
        authClient.signOut(new AuthClient.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                callback.onSuccess();
            }
            
            @Override
            public void onError(String error) {
                // Log error but still consider logout successful locally
                callback.onSuccess(); 
            }
        });
    }
    
    public void getCurrentUser(GetUserCallback callback) {
        if (!authClient.isAuthenticated()) { // Check authentication status via AuthClient
            callback.onError("Not authenticated");
            return;
        }
        
        // Use AuthClient's getCurrentUser method
        authClient.getCurrentUser(new AuthClient.ApiCallback<User>() {
            @Override
            public void onSuccess(User user) {
                callback.onSuccess(user);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public boolean isAuthenticated() {
        return authClient.isAuthenticated(); // Check authentication status via AuthClient
    }
    
    // --- Callback Interfaces ---
    
    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String error);
    }
    
    public interface LogoutCallback {
        void onSuccess();
    }
    
    public interface GetUserCallback {
        void onSuccess(User user);
        void onError(String error);
    }
}
