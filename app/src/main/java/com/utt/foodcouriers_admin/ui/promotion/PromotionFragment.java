package com.utt.foodcouriers_admin.ui.promotion;

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
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.button.MaterialButton;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Promotion;
import com.utt.foodcouriers_admin.data.repository.PromotionRepository;
import com.utt.foodcouriers_admin.data.request.PromotionUpsertRequest;
import com.utt.foodcouriers_admin.ui.promotion.adapter.PromotionAdapter;
import com.utt.foodcouriers_admin.ui.promotion.dialog.PromotionFormDialogFragment;
import java.util.ArrayList;
import java.util.List;

public class PromotionFragment extends Fragment implements PromotionAdapter.PromotionActionListener {

    private static final int PAGE_LIMIT = 100;

    private RecyclerView rvPromotions;
    private PromotionAdapter adapter;
    private ExtendedFloatingActionButton fabAdd;
    private PromotionRepository promotionRepository;
    private SwipeRefreshLayout swipeRefreshLayout;
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
    private final List<Promotion> currentItems = new ArrayList<>();
    private String currentQuery = "";
    private StatusFilter statusFilter = StatusFilter.ALL;
    private Runnable searchRunnable;

    private enum StatusFilter {
        ALL,
        ACTIVE,
        HIDDEN,
        VALID
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_promotion_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        promotionRepository = PromotionRepository.getInstance();
        initViews(view);
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupFilters();
        setupSwipeRefresh();
        fabAdd.setOnClickListener(v -> openPromotionForm(null));
        btnRetry.setOnClickListener(v -> loadPromotions(true));
        loadPromotions(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        searchHandler.removeCallbacksAndMessages(null);
    }

    private void initViews(View view) {
        rvPromotions = view.findViewById(R.id.rv_promotions);
        fabAdd = view.findViewById(R.id.fab_add_promotion);
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
            toolbar.setTitle(R.string.promotion_title);
            toolbar.setSubtitle(R.string.promotion_subtitle);
        }
    }

    private void setupRecyclerView() {
        rvPromotions.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PromotionAdapter();
        adapter.setListener(this);
        rvPromotions.setAdapter(adapter);
    }

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
            } else if (checkedId == R.id.chip_valid) {
                statusFilter = StatusFilter.VALID;
            } else {
                statusFilter = StatusFilter.ALL;
            }
            triggerSearch(false);
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> loadPromotions(false));
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
    }

    private void triggerSearch(boolean showLoader) {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        searchRunnable = () -> loadPromotions(showLoader);
        searchHandler.postDelayed(searchRunnable, 350);
    }

    private void loadPromotions(boolean showLoading) {
        if (showLoading) {
            showLoadingState();
        }
        Boolean isActiveFilter = getActiveFilterValue();
        Boolean isValidFilter = getValidFilterValue();
        promotionRepository.getPromotions(resolveQueryParam(), isActiveFilter, isValidFilter, PAGE_LIMIT, 0, new RepositoryCallback<List<Promotion>>() {
            @Override
            public void onComplete(BaseResponse<List<Promotion>> response) {
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

    private Boolean getActiveFilterValue() {
        switch (statusFilter) {
            case ACTIVE:
                return true;
            case HIDDEN:
                return false;
            case VALID:
                return true;
            default:
                return null;
        }
    }

    private Boolean getValidFilterValue() {
        return statusFilter == StatusFilter.VALID ? true : null;
    }

    private void openPromotionForm(@Nullable Promotion promotion) {
        PromotionFormDialogFragment dialog = PromotionFormDialogFragment.newInstance(promotion);
        dialog.setPromotionFormListener(this::handleFormSubmission);
        dialog.show(getChildFragmentManager(), "promotion_form");
    }

    private void handleFormSubmission(@Nullable String promotionId, PromotionUpsertRequest request, PromotionFormDialogFragment dialog) {
        dialog.setLoading(true);
        RepositoryCallback<Promotion> callback = new RepositoryCallback<Promotion>() {
            @Override
            public void onComplete(BaseResponse<Promotion> response) {
                dialog.setLoading(false);
                if (!response.isSuccess()) {
                    Context context = getContext();
                    if (context != null) {
                        Toast.makeText(context, response.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                dialog.dismissAllowingStateLoss();
                boolean isCreate = TextUtils.isEmpty(promotionId);
                Context context = getContext();
                if (context != null) {
                    String message = context.getString(isCreate ? R.string.toast_promotion_created : R.string.toast_promotion_updated);
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
                loadPromotions(true);
            }
        };

        if (TextUtils.isEmpty(promotionId)) {
            promotionRepository.create(request, callback);
        } else {
            promotionRepository.update(promotionId, request, callback);
        }
    }

    @Override
    public void onEdit(Promotion promotion) {
        openPromotionForm(promotion);
    }

    @Override
    public void onStatusChange(Promotion promotion, boolean isActive) {
        promotionRepository.updateStatus(promotion.getId(), isActive, new RepositoryCallback<Promotion>() {
            @Override
            public void onComplete(BaseResponse<Promotion> response) {
                Context context = getContext();
                if (!response.isSuccess()) {
                    if (context != null) {
                        Toast.makeText(context, response.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    loadPromotions(false);
                    return;
                }
                if (context != null) {
                    Toast.makeText(context, context.getString(R.string.toast_promotion_status_updated), Toast.LENGTH_SHORT).show();
                }
                loadPromotions(false);
            }
        });
    }

    @Override
    public void onDelete(Promotion promotion) {
        if (getContext() == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_promotion_title)
                .setMessage(getString(R.string.dialog_delete_promotion_message, promotion.getName()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deletePromotion(promotion))
                .show();
    }

    private void deletePromotion(Promotion promotion) {
        promotionRepository.delete(promotion.getId(), new RepositoryCallback<Void>() {
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
                    Toast.makeText(context, context.getString(R.string.toast_promotion_deleted), Toast.LENGTH_SHORT).show();
                }
                loadPromotions(true);
            }
        });
    }
}