package com.utt.foodcouriers_admin.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.ui.auth.LoginActivity;
import com.utt.foodcouriers_admin.ui.dashboard.DashboardFragment;
import com.utt.foodcouriers_admin.ui.menu.CategoryFragment;
import com.utt.foodcouriers_admin.ui.menu.MenuItemFragment;
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
            navigationView.setCheckedItem(R.id.nav_dashboard);
            loadFragment(new DashboardFragment(), "Dashboard");
        }

        String loginSuccessMessage = getIntent().getStringExtra(LoginActivity.EXTRA_LOGIN_SUCCESS_MESSAGE);
        if (loginSuccessMessage != null && !loginSuccessMessage.isBlank()) {
            Toast.makeText(this, loginSuccessMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void setupNavigationDrawer() {
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
                    title = "Quản lý đơn hàng";
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
                    Toast.makeText(MainActivity.this, "Module \"" + title + "\" đang được phát triển", Toast.LENGTH_SHORT).show();
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
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
    }
}
