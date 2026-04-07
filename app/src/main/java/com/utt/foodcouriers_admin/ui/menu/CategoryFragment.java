package com.utt.foodcouriers_admin.ui.menu;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Category;
import com.utt.foodcouriers_admin.data.remote.BaseSupabaseClient;
import com.utt.foodcouriers_admin.data.remote.MenuClient;
import com.utt.foodcouriers_admin.data.repository.CategoryRepository;
import com.utt.foodcouriers_admin.data.request.CategoryUpsertRequest;
import com.utt.foodcouriers_admin.ui.menu.adapter.CategoryAdapter;
import com.utt.foodcouriers_admin.ui.menu.dialog.CategoryFormDialogFragment;
import java.util.ArrayList;
import java.util.List;

public class CategoryFragment extends Fragment implements CategoryAdapter.CategoryActionListener {

    private static final int PAGE_LIMIT = 100;

    private RecyclerView rvCategories;
    private CategoryAdapter adapter;
    private ExtendedFloatingActionButton fabAdd;
    private MenuClient menuClient;
    private CategoryRepository categoryRepository;
    private SwipeRefreshLayout  swipeRefreshLayout;
    private SearchView searchView;
    private ChipGroup chipGroup;
    private MaterialToolbar toolbar;
    private View stateContainer;
    private View stateLoading;
    private View stateEmpty;
    private View stateError;
    private TextView tvErrorMessage;
    private MaterialButton btnRetry;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final List<Category> currentItems = new ArrayList<>();
    private String currentQuery = "";
    private StatusFilter statusFilter = StatusFilter.ALL;
    private Runnable searchRunnable;

    private enum StatusFilter {
        ALL,
        ACTIVE,
        HIDDEN
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_category_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        categoryRepository = CategoryRepository.getInstance();
        initViews(view);
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupFilters();
        setupSwipeRefresh();
        fabAdd.setOnClickListener(v -> openCategoryForm(null));
        btnRetry.setOnClickListener(v -> loadCategories(true));
        loadCategories(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        searchHandler.removeCallbacksAndMessages(null);
    }

    private void initViews(View view) {
        rvCategories = view.findViewById(R.id.rv_categories);
        fabAdd = view.findViewById(R.id.fab_add_category);
        menuClient = MenuClient.getInstance();
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        searchView = view.findViewById(R.id.search_view);
        chipGroup = view.findViewById(R.id.chip_group_filter);
        toolbar = view.findViewById(R.id.toolbar);
        stateContainer = view.findViewById(R.id.state_container);
        stateLoading = view.findViewById(R.id.state_loading);
        stateEmpty = view.findViewById(R.id.state_empty);
        stateError = view.findViewById(R.id.state_error);
        tvErrorMessage = view.findViewById(R.id.tv_state_error_message);
        btnRetry = view.findViewById(R.id.btn_retry);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setTitle(R.string.category_title);
            toolbar.setSubtitle(R.string.category_subtitle);
        }
    }

    private void setupRecyclerView() {
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CategoryAdapter();
        adapter.setListener(this);
        rvCategories.setAdapter(adapter);
    }

    private void loadCategories() {
        menuClient.getCategories(new BaseSupabaseClient.ApiCallback<Category[]>() {
    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentQuery = query;
                triggerSearch(false);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText;
                triggerSearch(false);
                return true;
            }
        });
    }

    private void setupFilters() {
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_active) {
                statusFilter = StatusFilter.ACTIVE;
            } else if (checkedId == R.id.chip_hidden) {
                statusFilter = StatusFilter.HIDDEN;
            } else {
                statusFilter = StatusFilter.ALL;
            }
            triggerSearch(false);
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> loadCategories(false));
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
    }

    private void triggerSearch(boolean showLoader) {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        searchRunnable = () -> loadCategories(showLoader);
        searchHandler.postDelayed(searchRunnable, 350);
    }

    private void loadCategories(boolean showLoading) {
        if (showLoading) {
            showLoadingState();
        }
        Boolean isActiveFilter = getFilterValue();
        categoryRepository.getCategories(resolveQueryParam(), isActiveFilter, PAGE_LIMIT, 0, new RepositoryCallback<List<Category>>() {
            @Override
            public void onComplete(BaseResponse<List<Category>> response) {
                swipeRefreshLayout.setRefreshing(false);
                if (!response.isSuccess() || response.getData() == null) {
                    showErrorState(response.getMessage());
                    return;
                }
                currentItems.clear();
                currentItems.addAll(response.getData());
                if (currentItems.isEmpty()) {
                    showEmptyState();
                } else {
                    showContentState();
                    adapter.submitList(new ArrayList<>(currentItems));
                }
            }
        });
    }

    private void showLoadingState() {
        stateContainer.setVisibility(View.VISIBLE);
        stateLoading.setVisibility(View.VISIBLE);
        stateEmpty.setVisibility(View.GONE);
        stateError.setVisibility(View.GONE);
        swipeRefreshLayout.setVisibility(View.GONE);
    }

    private void showContentState() {
        stateContainer.setVisibility(View.GONE);
        swipeRefreshLayout.setVisibility(View.VISIBLE);
        stateLoading.setVisibility(View.GONE);
        stateEmpty.setVisibility(View.GONE);
        stateError.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        stateContainer.setVisibility(View.VISIBLE);
        stateEmpty.setVisibility(View.VISIBLE);
        stateLoading.setVisibility(View.GONE);
        stateError.setVisibility(View.GONE);
        swipeRefreshLayout.setVisibility(View.GONE);
    }

    private void showErrorState(String message) {
        stateContainer.setVisibility(View.VISIBLE);
        stateError.setVisibility(View.VISIBLE);
        stateLoading.setVisibility(View.GONE);
        stateEmpty.setVisibility(View.GONE);
        swipeRefreshLayout.setVisibility(View.GONE);
        tvErrorMessage.setText(TextUtils.isEmpty(message) ? getString(R.string.error_generic) : message);
    }

    private String resolveQueryParam() {
        return TextUtils.isEmpty(currentQuery) ? null : currentQuery;
    }

    private Boolean getFilterValue() {
        switch (statusFilter) {
            case ACTIVE:
                return true;
            case HIDDEN:
                return false;
            default:
                return null;
        }
    }

    private void openCategoryForm(@Nullable Category category) {
        CategoryFormDialogFragment dialog = CategoryFormDialogFragment.newInstance(category);
        dialog.setCategoryFormListener(this::handleFormSubmission);
        dialog.show(getChildFragmentManager(), "category_form");
    }

    private void handleFormSubmission(@Nullable String categoryId, CategoryUpsertRequest request, CategoryFormDialogFragment dialog) {
        dialog.setLoading(true);
        RepositoryCallback<Category> callback = new RepositoryCallback<Category>() {
            @Override
            public void onComplete(BaseResponse<Category> response) {
                dialog.setLoading(false);
                if (!response.isSuccess()) {
                    Context context = getContext();
                    if (context != null) {
                        Toast.makeText(context, response.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                dialog.dismissAllowingStateLoss();
                boolean isCreate = TextUtils.isEmpty(categoryId);
                Context context = getContext();
                if (context != null) {
                    String message = context.getString(isCreate ? R.string.toast_category_created : R.string.toast_category_updated);
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
                loadCategories(true);
            }
        };

        if (TextUtils.isEmpty(categoryId)) {
            categoryRepository.create(request, callback);
        } else {
            categoryRepository.update(categoryId, request, callback);
        }
    }

    @Override
    public void onEdit(Category category) {
        openCategoryForm(category);
    }

    @Override
    public void onStatusChange(Category category, boolean isActive) {
        categoryRepository.updateStatus(category.getId(), isActive, new RepositoryCallback<Category>() {
            @Override
            public void onComplete(BaseResponse<Category> response) {
                Context context = getContext();
                if (!response.isSuccess()) {
                    if (context != null) {
                        Toast.makeText(context, response.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    loadCategories(false);
                    return;
                }
                if (context != null) {
                    Toast.makeText(context, context.getString(R.string.toast_category_updated), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDelete(Category category) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_category_title)
                .setMessage(getString(R.string.dialog_delete_category_message, category.getName()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteCategory(category))
                .show();
    }

    private void deleteCategory(Category category) {
        categoryRepository.delete(category.getId(), new RepositoryCallback<Void>() {
            @Override
            public void onComplete(BaseResponse<Void> response) {
                Context context = getContext();
                if (!response.isSuccess()) {
                    if (context != null) {
                        Toast.makeText(context, response.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                if (context != null) {
                    Toast.makeText(context, context.getString(R.string.toast_category_deleted), Toast.LENGTH_SHORT).show();
                }
                loadCategories(true);
            }
        });
    }

    @Override
    public void onViewItems(Category category) {
        Context context = getContext();
        if (context != null) {
            String label = context.getString(R.string.action_view_items) + " - " + category.getName();
            Toast.makeText(context, label, Toast.LENGTH_SHORT).show();
        }
    }
}
