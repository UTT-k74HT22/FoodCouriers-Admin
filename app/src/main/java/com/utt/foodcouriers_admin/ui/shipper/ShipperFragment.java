package com.utt.foodcouriers_admin.ui.shipper;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Restaurant;
import com.utt.foodcouriers_admin.data.model.Shipper;
import com.utt.foodcouriers_admin.data.repository.RestaurantRepository;
import com.utt.foodcouriers_admin.data.repository.ShipperRepository;
import com.utt.foodcouriers_admin.data.request.ShipperUpsertRequest;
import com.utt.foodcouriers_admin.ui.shipper.adapter.ShipperAdapter;
import com.utt.foodcouriers_admin.ui.shipper.dialog.ShipperFormDialogFragment;
import java.util.ArrayList;
import java.util.List;

public class ShipperFragment extends Fragment implements ShipperAdapter.ShipperActionListener {

    private RecyclerView rvShippers;
    private ShipperAdapter adapter;
    private View emptyState;
    private TextInputEditText etSearch;
    private FloatingActionButton fabAdd;
    private ShipperRepository shipperRepository;
    private RestaurantRepository restaurantRepository;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final List<Shipper> currentItems = new ArrayList<>();
    private List<Restaurant> restaurantList = new ArrayList<>();
    private String currentQuery = "";
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_shipper_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        shipperRepository = ShipperRepository.getInstance();
        restaurantRepository = RestaurantRepository.getInstance();
        initViews(view);
        setupRecyclerView();
        setupSearch();
        fabAdd.setOnClickListener(v -> openShipperForm(null));
        loadRestaurants();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        searchHandler.removeCallbacksAndMessages(null);
    }

    private void initViews(View view) {
        rvShippers = view.findViewById(R.id.rv_shippers);
        emptyState = view.findViewById(R.id.empty_state);
        etSearch = view.findViewById(R.id.et_search);
        fabAdd = view.findViewById(R.id.fab_add_shipper);
    }

    private void setupRecyclerView() {
        rvShippers.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ShipperAdapter();
        adapter.setListener(this);
        rvShippers.setAdapter(adapter);
    }

    private void setupSearch() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentQuery = s != null ? s.toString() : "";
                    triggerSearch(false);
                }
                @Override
                public void afterTextChanged(android.text.Editable s) { }
            });
        }
    }

    private void loadRestaurants() {
        restaurantRepository.getAll(new RepositoryCallback<List<Restaurant>>() {
            @Override
            public void onComplete(BaseResponse<List<Restaurant>> response) {
                if (response.isSuccess() && response.getData() != null) {
                    restaurantList = response.getData();
                }
                loadShippers(true);
            }
        });
    }

    private void triggerSearch(boolean showLoader) {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        searchRunnable = () -> loadShippers(showLoader);
        searchHandler.postDelayed(searchRunnable, 350);
    }

    private void loadShippers(boolean showLoading) {
        shipperRepository.getAll(new RepositoryCallback<List<Shipper>>() {
            @Override
            public void onComplete(BaseResponse<List<Shipper>> response) {
                if (!response.isSuccess() || response.getData() == null) {
                    Context context = getContext();
                    if (context != null) {
                        Toast.makeText(context, response.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                currentItems.clear();
                List<Shipper> filtered = filterList(response.getData());
                currentItems.addAll(filtered);
                if (currentItems.isEmpty()) {
                    if (emptyState != null) {
                        emptyState.setVisibility(View.VISIBLE);
                    }
                    adapter.submitList(new ArrayList<>());
                } else {
                    if (emptyState != null) {
                        emptyState.setVisibility(View.GONE);
                    }
                    adapter.submitList(new ArrayList<>(currentItems));
                }
            }
        });
    }

    private List<Shipper> filterList(List<Shipper> items) {
        List<Shipper> result = new ArrayList<>();
        for (Shipper shipper : items) {
            if (!TextUtils.isEmpty(currentQuery)) {
                String query = currentQuery.toLowerCase();
                boolean matchName = shipper.getFullName() != null && shipper.getFullName().toLowerCase().contains(query);
                boolean matchPhone = shipper.getPhone() != null && shipper.getPhone().toLowerCase().contains(query);
                boolean matchEmail = shipper.getEmail() != null && shipper.getEmail().toLowerCase().contains(query);
                boolean matchRestaurant = shipper.getRestaurantName() != null && shipper.getRestaurantName().toLowerCase().contains(query);
                if (!matchName && !matchPhone && !matchEmail && !matchRestaurant) continue;
            }
            result.add(shipper);
        }
        return result;
    }

    private void openShipperForm(@Nullable Shipper shipper) {
        ShipperFormDialogFragment dialog = ShipperFormDialogFragment.newInstance(shipper, restaurantList);
        dialog.setShipperFormListener(this::handleFormSubmission);
        dialog.show(getChildFragmentManager(), "shipper_form");
    }

    private void handleFormSubmission(@Nullable String shipperId, ShipperUpsertRequest request, ShipperFormDialogFragment dialog) {
        dialog.setLoading(true);
        RepositoryCallback<Shipper> callback = new RepositoryCallback<Shipper>() {
            @Override
            public void onComplete(BaseResponse<Shipper> response) {
                dialog.setLoading(false);
                if (!response.isSuccess()) {
                    Context context = getContext();
                    if (context != null) {
                        Toast.makeText(context, response.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                dialog.dismissAllowingStateLoss();
                boolean isCreate = TextUtils.isEmpty(shipperId);
                Context context = getContext();
                if (context != null) {
                    String message = context.getString(isCreate ? R.string.toast_shipper_created : R.string.toast_shipper_updated);
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
                loadShippers(true);
            }
        };

        if (TextUtils.isEmpty(shipperId)) {
            shipperRepository.create(request, callback);
        } else {
            shipperRepository.update(shipperId, request, callback);
        }
    }

    @Override
    public void onEdit(Shipper shipper) {
        openShipperForm(shipper);
    }

    @Override
    public void onStatusChange(Shipper shipper, boolean isActive) {
        shipperRepository.updateStatus(shipper.getId(), isActive, new RepositoryCallback<Shipper>() {
            @Override
            public void onComplete(BaseResponse<Shipper> response) {
                Context context = getContext();
                if (!response.isSuccess()) {
                    if (context != null) {
                        Toast.makeText(context, response.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    loadShippers(false);
                    return;
                }
                if (context != null) {
                    Toast.makeText(context, context.getString(R.string.toast_shipper_updated), Toast.LENGTH_SHORT).show();
                }
                loadShippers(false);
            }
        });
    }

    @Override
    public void onDelete(Shipper shipper) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_shipper_title)
                .setMessage(getString(R.string.dialog_delete_shipper_message, shipper.getFullName()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteShipper(shipper))
                .show();
    }

    private void deleteShipper(Shipper shipper) {
        shipperRepository.delete(shipper.getId(), new RepositoryCallback<Void>() {
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
                    Toast.makeText(context, context.getString(R.string.toast_shipper_deleted), Toast.LENGTH_SHORT).show();
                }
                loadShippers(true);
            }
        });
    }
}
