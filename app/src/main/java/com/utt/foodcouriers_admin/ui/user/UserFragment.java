package com.utt.foodcouriers_admin.ui.user;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.User;
import com.utt.foodcouriers_admin.data.repository.AdminUserAccountRepository;
import com.utt.foodcouriers_admin.data.repository.UserRepository;
import com.utt.foodcouriers_admin.data.request.AdminCreateUserAccountRequest;
import com.utt.foodcouriers_admin.data.request.UserUpdateRequest;
import com.utt.foodcouriers_admin.ui.user.adapter.UserAdapter;
import com.utt.foodcouriers_admin.ui.user.dialog.CreateUserDialogFragment;
import com.utt.foodcouriers_admin.ui.user.dialog.EditUserDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class UserFragment extends Fragment implements UserAdapter.UserActionListener {

    private static final String TAG = "UserFragment";

    private RecyclerView rvUsers;
    private EditText etSearch;
    private TabLayout tabRole;
    private View emptyState;
    private TextView tvEmptyTitle;
    private TextView tvEmptyMessage;
    private FloatingActionButton fabAddUser;

    private UserAdapter adapter;
    private UserRepository userRepository;
    private AdminUserAccountRepository adminUserAccountRepository;

    private String currentQuery = null;
    private String currentRole = "customer";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_user_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userRepository = UserRepository.getInstance();
        adminUserAccountRepository = AdminUserAccountRepository.getInstance();
        initViews(view);
        setupRecyclerView();
        setupFilters();
        setupSearch();
        fabAddUser.setOnClickListener(v -> openCreateDialog());
        loadUsers();
    }

    private void initViews(View view) {
        rvUsers = view.findViewById(R.id.rv_users);
        etSearch = view.findViewById(R.id.et_search);
        tabRole = view.findViewById(R.id.tab_user_role);
        emptyState = view.findViewById(R.id.empty_state);
        tvEmptyTitle = emptyState.findViewById(R.id.tvEmptyTitle);
        tvEmptyMessage = emptyState.findViewById(R.id.tvEmptyMessage);
        fabAddUser = view.findViewById(R.id.fab_add_user);
    }

    private void setupRecyclerView() {
        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new UserAdapter();
        adapter.setListener(this);
        rvUsers.setAdapter(adapter);
    }

    private void setupFilters() {
        tabRole.addTab(tabRole.newTab().setText(R.string.user_role_client));
        tabRole.addTab(tabRole.newTab().setText(R.string.user_role_staff));
        tabRole.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentRole = tab.getPosition() == 1 ? "staff" : "customer";
                loadUsers();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = TextUtils.isEmpty(s) ? null : s.toString().trim();
                loadUsers();
            }

            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void openCreateDialog() {
        CreateUserDialogFragment dialog = CreateUserDialogFragment.newInstance();
        dialog.setCreateUserListener(this::handleCreateUser);
        dialog.show(getChildFragmentManager(), "create_user_dialog");
    }

    private void handleCreateUser(AdminCreateUserAccountRequest request, String avatarUrl, CreateUserDialogFragment dialog) {
        Log.d(TAG, "handleCreateUser() called");
        Log.d(TAG, "Request role=" + request.getRole());
        Log.d(TAG, "Request email=" + request.getEmail());
        Log.d(TAG, "Avatar URL=" + avatarUrl);

        dialog.setLoading(true);

        adminUserAccountRepository.createAccount(request, new RepositoryCallback<User>() {
            @Override
            public void onComplete(BaseResponse<User> response) {
                Log.d(TAG, "createAccount callback received");
                Log.d(TAG, "success=" + response.isSuccess() + ", message=" + response.getMessage());

                dialog.setLoading(false);

                if (!isAdded()) {
                    Log.w(TAG, "Fragment is not attached anymore, skip UI update");
                    return;
                }

                if (!response.isSuccess()) {
                    Log.e(TAG, "Create user failed: " + response.getMessage());
                    Toast.makeText(requireContext(), response.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                User createdUser = response.getData();
                if (createdUser != null) {
                    Log.d(TAG, "Created user id=" + createdUser.getId() + ", email=" + createdUser.getEmail());
                } else {
                    Log.w(TAG, "Create success but response data is null");
                }

                dialog.dismissAllowingStateLoss();
                Toast.makeText(requireContext(), R.string.user_create_success, Toast.LENGTH_SHORT).show();

                currentRole = request.getRole();
                Log.d(TAG, "Reloading list with currentRole=" + currentRole);

                if ("staff".equals(currentRole) && tabRole.getSelectedTabPosition() != 1) {
                    TabLayout.Tab tab = tabRole.getTabAt(1);
                    if (tab != null) {
                        Log.d(TAG, "Selecting staff tab");
                        tab.select();
                    }
                } else if (!"staff".equals(currentRole) && tabRole.getSelectedTabPosition() != 0) {
                    TabLayout.Tab tab = tabRole.getTabAt(0);
                    if (tab != null) {
                        Log.d(TAG, "Selecting customer tab");
                        tab.select();
                    }
                } else {
                    Log.d(TAG, "Calling loadUsers()");
                    loadUsers();
                }
            }
        });
    }

    private void loadUsers() {
        Log.d(TAG, "loadUsers() called. query=" + currentQuery + ", role=" + currentRole);

        userRepository.getUsers(currentQuery, currentRole, null, 100, 0, new RepositoryCallback<List<User>>() {
            @Override
            public void onComplete(BaseResponse<List<User>> response) {
                Log.d(TAG, "loadUsers callback. success=" + response.isSuccess() + ", message=" + response.getMessage());

                if (!response.isSuccess() || response.getData() == null) {
                    Log.e(TAG, "loadUsers failed: " + response.getMessage());
                    showEmptyState(getString(R.string.user_error_load_title), response.getMessage());
                    return;
                }

                List<User> users = response.getData();
                Log.d(TAG, "loadUsers success. size=" + users.size());

                adapter.submitList(new ArrayList<>(users));
                emptyState.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
                rvUsers.setVisibility(users.isEmpty() ? View.GONE : View.VISIBLE);

                if (users.isEmpty()) {
                    tvEmptyTitle.setText(R.string.user_empty_title);
                    tvEmptyMessage.setText(R.string.user_empty_message);
                }
            }
        });
    }

    private void showEmptyState(String title, String message) {
        rvUsers.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        tvEmptyTitle.setText(title);
        tvEmptyMessage.setText(TextUtils.isEmpty(message) ? getString(R.string.error_generic) : message);
    }

    @Override
    public void onUserClick(User user) {
        EditUserDialogFragment dialog = EditUserDialogFragment.newInstance(user);
        dialog.setEditUserListener(this::loadUsers);
        dialog.show(getChildFragmentManager(), "edit_user_dialog");
    }

    @Override
    public void onStatusChange(User user, boolean isActive) {
        String message = isActive
                ? getString(R.string.user_confirm_activate, user.getFullName())
                : getString(R.string.user_confirm_deactivate, user.getFullName());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_status_title)
                .setMessage(message)
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> loadUsers())
                .setPositiveButton(R.string.action_confirm, (dialog, which) -> performStatusChange(user, isActive))
                .show();
    }

    private void performStatusChange(User user, boolean isActive) {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setIsActive(isActive);
        userRepository.update(user.getId(), request, new RepositoryCallback<User>() {
            @Override
            public void onComplete(BaseResponse<User> response) {
                if (!isAdded()) return;
                if (!response.isSuccess()) {
                    Toast.makeText(requireContext(), response.getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), isActive ? R.string.user_activate_success : R.string.user_deactivate_success, Toast.LENGTH_SHORT).show();
                }
                loadUsers();
            }
        });
    }
}
