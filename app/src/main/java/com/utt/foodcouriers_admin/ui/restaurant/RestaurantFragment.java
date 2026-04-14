package com.utt.foodcouriers_admin.ui.restaurant;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputEditText;
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
import com.utt.foodcouriers_admin.data.model.Restaurant;
import com.utt.foodcouriers_admin.data.repository.RestaurantRepository;
import com.utt.foodcouriers_admin.data.request.RestaurantUpsertRequest;
import com.utt.foodcouriers_admin.ui.main.MainActivity;
import com.utt.foodcouriers_admin.ui.menu.MenuItemFragment;
import com.utt.foodcouriers_admin.ui.restaurant.adapter.RestaurantAdapter;
import com.utt.foodcouriers_admin.ui.restaurant.dialog.RestaurantFormDialogFragment;
import com.utt.foodcouriers_admin.utils.ToastBanner;
import java.util.ArrayList;
import java.util.List;

public class RestaurantFragment extends Fragment implements RestaurantAdapter.RestaurantActionListener {

    private static final int PAGE_LIMIT = 100;

    private RecyclerView rvRestaurants;
    private RestaurantAdapter adapter;
    private ExtendedFloatingActionButton fabAdd;
    private RestaurantRepository restaurantRepository;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextInputEditText etSearch;
    private ChipGroup chipGroup;
    private MaterialToolbar toolbar;
    private View stateContainer;
    private View stateLoading;
    private View stateEmpty;
    private View stateError;
    private TextView tvErrorMessage;
    private MaterialButton btnRetry;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final List<Restaurant> currentItems = new ArrayList<>();
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
        return inflater.inflate(R.layout.activity_restaurant_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        restaurantRepository = RestaurantRepository.getInstance();
        initViews(view);
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupFilters();
        setupSwipeRefresh();
        fabAdd.setOnClickListener(v -> openRestaurantForm(null));
        btnRetry.setOnClickListener(v -> loadRestaurants(true));
        loadRestaurants(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        searchHandler.removeCallbacksAndMessages(null);
    }

    private void initViews(View view) {
        rvRestaurants = view.findViewById(R.id.rv_restaurants);
        fabAdd = view.findViewById(R.id.fab_add_restaurant);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        etSearch = view.findViewById(R.id.et_search);
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
            toolbar.setTitle(R.string.restaurant_title);
            toolbar.setSubtitle(R.string.restaurant_subtitle);
        }
    }

    private void setupRecyclerView() {
        rvRestaurants.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RestaurantAdapter();
        adapter.setListener(this);
        rvRestaurants.setAdapter(adapter);
    }

    private void setupSearch() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentQuery = s.toString();
                    triggerSearch(false);
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }
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
        swipeRefreshLayout.setOnRefreshListener(() -> loadRestaurants(false));
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
    }

    private void triggerSearch(boolean showLoader) {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        searchRunnable = () -> loadRestaurants(showLoader);
        searchHandler.postDelayed(searchRunnable, 350);
    }

    private void loadRestaurants(boolean showLoading) {
        if (showLoading) {
            showLoadingState();
        }
        Boolean isActiveFilter = getFilterValue();
        restaurantRepository.getAll(resolveQueryParam(), isActiveFilter, PAGE_LIMIT, 0, new RepositoryCallback<List<Restaurant>>() {
            @Override
            public void onComplete(BaseResponse<List<Restaurant>> response) {
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
        tvErrorMessage.setText(message != null ? message : getString(R.string.error_generic));
    }

    private String resolveQueryParam() {
        return currentQuery.isEmpty() ? null : currentQuery;
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

    private void openRestaurantForm(@Nullable Restaurant restaurant) {
        RestaurantFormDialogFragment dialog = RestaurantFormDialogFragment.newInstance(restaurant);
        dialog.setRestaurantFormListener(this::handleFormSubmission);
        dialog.show(getChildFragmentManager(), "restaurant_form");
    }

    private void handleFormSubmission(@Nullable String restaurantId, RestaurantUpsertRequest request, RestaurantFormDialogFragment dialog) {
        dialog.setLoading(true);
        RepositoryCallback<Restaurant> callback = new RepositoryCallback<Restaurant>() {
            @Override
            public void onComplete(BaseResponse<Restaurant> response) {
                dialog.setLoading(false);
                if (!response.isSuccess()) {
                    ToastBanner.showError(response.getMessage());
                    return;
                }
                dialog.dismissAllowingStateLoss();
                boolean isCreate = restaurantId == null || restaurantId.isEmpty();
                String message = requireContext().getString(isCreate ? R.string.toast_restaurant_created : R.string.toast_restaurant_updated);
                ToastBanner.showSuccess(message);
                loadRestaurants(true);
            }
        };

        if (restaurantId == null || restaurantId.isEmpty()) {
            restaurantRepository.create(request, callback);
        } else {
            restaurantRepository.update(restaurantId, request, callback);
        }
    }

    @Override
    public void onEdit(Restaurant restaurant) {
        openRestaurantForm(restaurant);
    }

    @Override
    public void onStatusChange(Restaurant restaurant, boolean isActive) {
        String message = isActive ? 
                getString(R.string.dialog_confirm_activate, restaurant.getName()) :
                getString(R.string.dialog_confirm_deactivate, restaurant.getName());
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_status_title)
                .setMessage(message)
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> loadRestaurants(false))
                .setPositiveButton(R.string.action_confirm, (dialog, which) -> performStatusChange(restaurant, isActive))
                .show();
    }

    private void performStatusChange(Restaurant restaurant, boolean isActive) {
        RestaurantUpsertRequest request = new RestaurantUpsertRequest(
                null, null, null, null, null, isActive, null, null, null, null, null
        );
        restaurantRepository.update(restaurant.getId(), request, new RepositoryCallback<Restaurant>() {
            @Override
            public void onComplete(BaseResponse<Restaurant> response) {
                if (!response.isSuccess()) {
                    ToastBanner.showError(response.getMessage());
                    loadRestaurants(false);
                    return;
                }
                ToastBanner.showSuccess(requireContext().getString(R.string.toast_restaurant_updated));
                loadRestaurants(false);
            }
        });
    }

    @Override
    public void onDelete(Restaurant restaurant) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_restaurant_title)
                .setMessage(getString(R.string.dialog_delete_restaurant_message, restaurant.getName()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteRestaurant(restaurant))
                .show();
    }

    private void deleteRestaurant(Restaurant restaurant) {
        restaurantRepository.delete(restaurant.getId(), new RepositoryCallback<Void>() {
            @Override
            public void onComplete(BaseResponse<Void> response) {
                if (!response.isSuccess()) {
                    ToastBanner.showError(response.getMessage());
                    return;
                }
                ToastBanner.showSuccess(requireContext().getString(R.string.toast_restaurant_deleted));
                loadRestaurants(true);
            }
        });
    }

    @Override
    public void onViewMenu(Restaurant restaurant) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToMenuWithRestaurant(restaurant);
        }
    }
}
