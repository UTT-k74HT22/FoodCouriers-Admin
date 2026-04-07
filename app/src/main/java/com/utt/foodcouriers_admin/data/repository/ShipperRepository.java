package com.utt.foodcouriers_admin.data.repository;

import android.text.TextUtils;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Shipper;
import com.utt.foodcouriers_admin.data.repository.base.BaseSupabaseRepository;
import com.utt.foodcouriers_admin.data.repository.base.CrudRepository;
import com.utt.foodcouriers_admin.data.request.ShipperUpsertRequest;
import java.util.List;

public class ShipperRepository extends BaseSupabaseRepository implements CrudRepository<Shipper, ShipperUpsertRequest> {

    private static final String TABLE = "restaurant_staff";
    private static ShipperRepository instance;

    public static synchronized ShipperRepository getInstance() {
        if (instance == null) {
            instance = new ShipperRepository();
        }
        return instance;
    }

    @Override
    public void getAll(RepositoryCallback<List<Shipper>> callback) {
        fetchList(TABLE, "?select=*,user:users(full_name,phone,email,avatar_url),restaurant:restaurants(name)&role_in_restaurant=eq.shipper&order=created_at.desc", Shipper[].class, callback);
    }

    public void getByRestaurant(String restaurantId, RepositoryCallback<List<Shipper>> callback) {
        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Restaurant id is required"));
            return;
        }
        fetchList(TABLE, "?select=*,user:users(full_name,phone,email,avatar_url),restaurant:restaurants(name)&role_in_restaurant=eq.shipper&restaurant_id=eq." + restaurantId.trim() + "&order=created_at.desc", Shipper[].class, callback);
    }

    @Override
    public void getById(String id, RepositoryCallback<Shipper> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Shipper id is required"));
            return;
        }
        fetchSingle(TABLE, eqIdFilter(id.trim()), Shipper[].class, callback);
    }

    @Override
    public void create(ShipperUpsertRequest request, RepositoryCallback<Shipper> callback) {
        BaseResponse<Void> validation = validate(request, true);
        if (validation != null) {
            postResponse(callback, BaseResponse.error(validation.getError().getCode(), validation.getMessage()));
            return;
        }
        createItem(TABLE, request, Shipper[].class, callback);
    }

    @Override
    public void update(String id, ShipperUpsertRequest request, RepositoryCallback<Shipper> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Shipper id is required"));
            return;
        }
        BaseResponse<Void> validation = validate(request, false);
        if (validation != null) {
            postResponse(callback, BaseResponse.error(validation.getError().getCode(), validation.getMessage()));
            return;
        }
        updateItem(TABLE, eqIdFilter(id.trim()), request, Shipper[].class, callback);
    }

    @Override
    public void delete(String id, RepositoryCallback<Void> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Shipper id is required"));
            return;
        }
        deleteItem(TABLE, eqIdFilter(id.trim()), callback);
    }

    public void updateStatus(String id, boolean isActive, RepositoryCallback<Shipper> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Shipper id is required"));
            return;
        }
        ShipperUpsertRequest request = new ShipperUpsertRequest(null, null, null, null, null, null, isActive);
        updateItem(TABLE, eqIdFilter(id.trim()), request, Shipper[].class, callback);
    }

    private BaseResponse<Void> validate(ShipperUpsertRequest request, boolean requireMainFields) {
        if (request == null) {
            return BaseResponse.error("VALIDATION_ERROR", "Shipper payload is required");
        }
        if (requireMainFields || request.getUserId() != null) {
            BaseResponse<Void> validation = validateRequired(request.getUserId(), "User id");
            if (validation != null) return validation;
        }
        if (requireMainFields || request.getRestaurantId() != null) {
            BaseResponse<Void> validation = validateRequired(request.getRestaurantId(), "Restaurant id");
            if (validation != null) return validation;
        }
        return null;
    }
}
