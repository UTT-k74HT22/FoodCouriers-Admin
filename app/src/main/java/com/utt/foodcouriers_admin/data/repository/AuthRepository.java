package com.utt.foodcouriers_admin.data.repository;

import com.utt.foodcouriers_admin.data.model.User;
import com.utt.foodcouriers_admin.data.remote.SupabaseClient;

public class AuthRepository {
    
    private static AuthRepository instance;
    private final SupabaseClient supabaseClient;
    
    private AuthRepository() {
        supabaseClient = SupabaseClient.getInstance();
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
        
        supabaseClient.signIn(email.trim(), password, new SupabaseClient.ApiCallback<User>() {
            @Override
            public void onSuccess(User result) {
                callback.onSuccess(result);
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
        
        supabaseClient.signUp(email.trim(), password, name.trim(), phone, new SupabaseClient.ApiCallback<User>() {
            @Override
            public void onSuccess(User result) {
                callback.onSuccess(result);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public void logout(LogoutCallback callback) {
        supabaseClient.signOut(new SupabaseClient.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                callback.onSuccess();
            }
            
            @Override
            public void onError(String error) {
                callback.onSuccess();
            }
        });
    }
    
    public void getCurrentUser(GetUserCallback callback) {
        supabaseClient.getCurrentUser(new SupabaseClient.ApiCallback<User>() {
            @Override
            public void onSuccess(User result) {
                callback.onSuccess(result);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public boolean isAuthenticated() {
        return supabaseClient.isAuthenticated();
    }
    
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