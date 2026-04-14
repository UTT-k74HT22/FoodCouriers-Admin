package com.utt.foodcouriers_admin.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import com.utt.foodcouriers_admin.utils.ToastBanner;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.Restaurant;
import com.utt.foodcouriers_admin.ui.auth.LoginActivity;
import com.utt.foodcouriers_admin.ui.dashboard.DashboardFragment;
import com.utt.foodcouriers_admin.ui.category.CategoryFragment;
import com.utt.foodcouriers_admin.ui.menu.MenuItemFragment;
import com.utt.foodcouriers_admin.ui.order.OrderFragment;
import com.utt.foodcouriers_admin.ui.shipper.ShipperFragment;
import com.utt.foodcouriers_admin.ui.shipper.ShipperProfileFragment;
import com.utt.foodcouriers_admin.ui.promotion.PromotionFragment;
import com.utt.foodcouriers_admin.ui.restaurant.RestaurantFragment;
import com.utt.foodcouriers_admin.ui.user.UserFragment;
import com.utt.foodcouriers_admin.utils.SessionManager;

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
        setupNavigationHeader();
        setupBackPressedCallback();

        if (savedInstanceState == null) {
            String userRole = sessionManager.getUserRole();
            if ("shipper".equalsIgnoreCase(userRole)) {
                navigateToCurrentUserProfile();
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
        String userRole = sessionManager.getUserRole();
        boolean isShipper = "shipper".equalsIgnoreCase(userRole);

        Menu menu = navigationView.getMenu();

        // Shared self-service profile
        menu.findItem(R.id.nav_shipper_profile).setVisible(true);
        menu.findItem(R.id.nav_dashboard).setVisible(!isShipper);
        menu.findItem(R.id.nav_restaurants).setVisible(!isShipper);
        menu.findItem(R.id.nav_categories).setVisible(!isShipper);
        menu.findItem(R.id.nav_menu_items).setVisible(!isShipper);
        menu.findItem(R.id.nav_users).setVisible(!isShipper);
        menu.findItem(R.id.nav_shippers).setVisible(!isShipper);
        menu.findItem(R.id.nav_promotions).setVisible(!isShipper);
        
        // Group visibility
        if (menu.findItem(R.id.group_management) != null) {
            menu.findItem(R.id.group_management).setVisible(!isShipper);
        }
        if (menu.findItem(R.id.group_reports) != null) {
            menu.findItem(R.id.group_reports).setVisible(!isShipper);
        }

        // Shared items
        menu.findItem(R.id.nav_orders).setVisible(true);
        menu.findItem(R.id.nav_notifications).setVisible(true);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment = null;
                String title = "";

                int id = item.getItemId();

                if (id == R.id.nav_dashboard) {
                    fragment = new DashboardFragment();
                    title = "Dashboard";
                } else if (id == R.id.nav_shipper_profile) {
                    navigateToCurrentUserProfile();
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                } else if (id == R.id.nav_orders) {
                    if (isShipper) {
                        fragment = new com.utt.foodcouriers_admin.ui.shipper.DeliveryManagementFragment();
                        title = "Đơn hàng của tôi";
                    } else {
                        fragment = new OrderFragment();
                        title = "Quản lý đơn hàng";
                    }
                } else if (id == R.id.nav_restaurants) {
                    fragment = new RestaurantFragment();
                    title = "Quản lý nhà hàng";
                } else if (id == R.id.nav_categories) {
                    fragment = new CategoryFragment();
                    title = "Quản lý danh mục";
                } else if (id == R.id.nav_menu_items) {
                    fragment = new MenuItemFragment();
                    title = "Quản lý món ăn";
                } else if (id == R.id.nav_users) {
                    fragment = new UserFragment();
                    title = "Quản lý người dùng";
                } else if (id == R.id.nav_shippers) {
                    fragment = new ShipperFragment();
                    title = "Quản lý shipper";
                } else if (id == R.id.nav_promotions) {
                    fragment = new PromotionFragment();
                    title = "Khuyến mãi";
                } else if (id == R.id.nav_reports) {
                    fragment = new com.utt.foodcouriers_admin.ui.report.ReportFragment();
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

        refreshNavigationHeader();
    }

    private void setupNavigationHeader() {
        if (navigationView.getHeaderView(0) == null) {
            return;
        }

        navigationView.getHeaderView(0).setOnClickListener(v -> {
            navigateToCurrentUserProfile();
            drawerLayout.closeDrawer(GravityCompat.START);
        });
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

    private void navigateToCurrentUserProfile() {
        navigationView.setCheckedItem(R.id.nav_shipper_profile);
        loadFragment(new ShipperProfileFragment(), "Hồ sơ của tôi");
    }

    public void refreshNavigationHeader() {
        if (navigationView.getHeaderView(0) == null) {
            return;
        }

        ImageView ivAvatar = navigationView.getHeaderView(0).findViewById(R.id.nav_header_avatar);
        TextView tvName = navigationView.getHeaderView(0).findViewById(R.id.nav_header_name);
        TextView tvEmail = navigationView.getHeaderView(0).findViewById(R.id.nav_header_email);

        String userRole = sessionManager.getUserRole();
        int avatarPlaceholder = "shipper".equalsIgnoreCase(userRole) ? R.drawable.ic_shipper : R.drawable.ic_admin_avatar;

        if (ivAvatar != null) {
            String userAvatar = sessionManager.getUserAvatar();
            if (!TextUtils.isEmpty(userAvatar)) {
                Glide.with(this)
                        .load(userAvatar)
                        .placeholder(avatarPlaceholder)
                        .error(avatarPlaceholder)
                        .circleCrop()
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(avatarPlaceholder);
            }
        }
        if (tvName != null) {
            String userName = sessionManager.getUserName();
            tvName.setText(userName != null ? userName : "Admin");
        }
        if (tvEmail != null) {
            String userEmail = sessionManager.getUserEmail();
            tvEmail.setText(userEmail != null ? userEmail : "");
        }
    }

    public void navigateToMenuWithRestaurant(Restaurant restaurant) {
        Fragment fragment = MenuItemFragment.newInstance(restaurant);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        if (toolbar != null) {
            toolbar.setTitle("Menu: " + restaurant.getName());
        }
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void performLogout() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        ToastBanner.showSuccess("Đã đăng xuất");
    }
}
