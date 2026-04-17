package com.utt.foodcouriers_admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import com.utt.foodcouriers_admin.utils.ToastBanner;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.utt.foodcouriers_admin.ui.auth.LoginActivity;
import com.utt.foodcouriers_admin.ui.dashboard.DashboardFragment;
import com.utt.foodcouriers_admin.ui.category.CategoryFragment;
import com.utt.foodcouriers_admin.ui.menu.MenuItemFragment;
import com.utt.foodcouriers_admin.ui.order.OrderFragment;
import com.utt.foodcouriers_admin.utils.SessionManager;
import com.utt.foodcouriers_admin.data.model.User;
import com.utt.foodcouriers_admin.data.remote.SupabaseRealtimeClient;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = SessionManager.getInstance(this);

        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Initialize Supabase clients with the saved session token
        com.utt.foodcouriers_admin.data.remote.SupabaseClientManager.initializeClients(this);

        setContentView(R.layout.activity_main_with_drawer);

        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        toolbar.setNavigationOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        setupNavigationDrawer();
        setupBackPressedCallback();

        if (savedInstanceState == null) {
            User user = sessionManager.getCurrentUser();
            if (user != null && "shipper".equalsIgnoreCase(user.getRole())) {
                navigationView.setCheckedItem(R.id.nav_shipper_profile);
                loadFragment(new com.utt.foodcouriers_admin.ui.shipper.ShipperProfileFragment(), "Hồ sơ của tôi");
            } else {
                navigationView.setCheckedItem(R.id.nav_dashboard);
                loadFragment(new DashboardFragment(), "Dashboard");
            }

            String loginSuccessMessage = getIntent().getStringExtra(LoginActivity.EXTRA_LOGIN_SUCCESS_MESSAGE);
            if (loginSuccessMessage != null && !loginSuccessMessage.isBlank()) {
                ToastBanner.showSuccess(loginSuccessMessage);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        sessionManager = SessionManager.getInstance(this);
        if (sessionManager.isLoggedIn() && sessionManager.isTokenExpired()) {
            refreshTokenIfNeeded();
        }
    }

    private void refreshTokenIfNeeded() {
        com.utt.foodcouriers_admin.data.remote.SupabaseClientManager.refreshTokenIfNeeded(
                new com.utt.foodcouriers_admin.data.remote.BaseSupabaseClient.ApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (result != null && result) {
                            ToastBanner.showSuccess("Phiên làm việc đã được làm mới");
                        }
                    }

                    @Override
                    public void onError(String error) {
                        sessionManager.clearSession();
                        com.utt.foodcouriers_admin.data.remote.SupabaseClientManager.clearAllClients();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    }
                }
        );
    }

    private void setupNavigationDrawer() {
        User user = sessionManager.getCurrentUser();
        String role = user != null ? user.getRole() : "";
        android.util.Log.d("MainActivity", "Current user role: " + role);

        boolean isShipper = "shipper".equalsIgnoreCase(role);

        // Hide/Show items based on role
        navigationView.getMenu().findItem(R.id.nav_shipper_profile).setVisible(isShipper);
        
        // Admin only items
        navigationView.getMenu().findItem(R.id.nav_dashboard).setVisible(!isShipper);
        navigationView.getMenu().findItem(R.id.nav_restaurants).setVisible(!isShipper);
        navigationView.getMenu().findItem(R.id.nav_categories).setVisible(!isShipper);
        navigationView.getMenu().findItem(R.id.nav_menu_items).setVisible(!isShipper);
        
        // Sections
        if (navigationView.getMenu().findItem(R.id.group_management) != null) {
            navigationView.getMenu().findItem(R.id.group_management).setVisible(!isShipper);
        }
        if (navigationView.getMenu().findItem(R.id.group_reports) != null) {
            navigationView.getMenu().findItem(R.id.group_reports).setVisible(!isShipper);
        }

        // Shared items (Always visible or handled specifically)
        navigationView.getMenu().findItem(R.id.nav_orders).setVisible(true);
        navigationView.getMenu().findItem(R.id.nav_notifications).setVisible(true);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment = null;
                String title = "";

                int id = item.getItemId();

                if (id == R.id.nav_dashboard) {
                    fragment = new DashboardFragment();
                    title = "Dashboard";
                } else if (id == R.id.nav_orders) {
                    fragment = new OrderFragment();
                    title = "Quản lý đơn hàng";
                    title = isShipper ? "Đơn hàng của tôi" : "Quản lý đơn hàng";
                } else if (id == R.id.nav_restaurants) {
                    title = "Quản lý nhà hàng";
                } else if (id == R.id.nav_categories) {
                    fragment = new CategoryFragment();
                    title = "Quản lý danh mục";
                } else if (id == R.id.nav_menu_items) {
                    fragment = new MenuItemFragment();
                    title = "Quản lý món ăn";
                } else if (id == R.id.nav_users) {
                    title = "Quản lý người dùng";
                } else if (id == R.id.nav_shippers) {
                    title = "Quản lý shipper";
                } else if (id == R.id.nav_shipper_profile) {
                    fragment = new com.utt.foodcouriers_admin.ui.shipper.ShipperProfileFragment();
                    title = "Hồ sơ của tôi";
                } else if (id == R.id.nav_promotions) {
                    title = "Khuyến mãi";
                } else if (id == R.id.nav_reports) {
                    title = "Báo cáo";
                } else if (id == R.id.nav_notifications) {
                    title = "Thông báo";
                } else if (id == R.id.nav_logout) {
                    performLogout();
                    return true;
                }

                if (fragment != null) {
                    loadFragment(fragment, title);
                } else {
                    ToastBanner.showWarning("Module \"" + title + "\" đang được phát triển");
                    if (toolbar != null) toolbar.setTitle(title);
                }

                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        if (navigationView.getHeaderView(0) != null) {
            TextView tvName = navigationView.getHeaderView(0).findViewById(R.id.nav_header_name);
            TextView tvEmail = navigationView.getHeaderView(0).findViewById(R.id.nav_header_email);
            if (tvName != null) {
                String userName = sessionManager.getUserName();
                tvName.setText(userName != null ? userName : "Admin");
            }
            if (tvEmail != null) {
                String userEmail = sessionManager.getUserEmail();
                tvEmail.setText(userEmail != null ? userEmail : "");
            }
        }
    }

    private void setupBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void loadFragment(Fragment fragment, String title) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        if (toolbar != null) {
            toolbar.setTitle(title);
        }
    }

    private void performLogout() {
        // Disconnect realtime first
        SupabaseRealtimeClient.getInstance().disconnect();
        
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        ToastBanner.showSuccess("Đã đăng xuất");
    }
}
