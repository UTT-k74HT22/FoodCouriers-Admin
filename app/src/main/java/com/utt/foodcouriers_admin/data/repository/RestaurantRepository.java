package com.utt.foodcouriers_admin.data.repository;

import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Restaurant;
import com.utt.foodcouriers_admin.data.repository.base.BaseSupabaseRepository;
import com.utt.foodcouriers_admin.data.repository.base.CrudRepository;
import com.utt.foodcouriers_admin.data.request.RestaurantUpsertRequest;

import java.util.List;

public class RestaurantRepository extends BaseSupabaseRepository implements CrudRepository<Restaurant, RestaurantUpsertRequest> {

    private static final String TABLE = "restaurants";
    private static RestaurantRepository instance;

    public static synchronized RestaurantRepository getInstance() {
        if (instance == null) {
            instance = new RestaurantRepository();
        }
        return instance;
    }

    @Override
    public void getAll(RepositoryCallback<List<Restaurant>> callback) {
        fetchList(TABLE, "?select=*&order=created_at.desc", Restaurant[].class, callback);
    }

    @Override
    public void getById(String id, RepositoryCallback<Restaurant> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Restaurant id is required"));
            return;
        }
        fetchSingle(TABLE, eqIdFilter(id.trim()), Restaurant[].class, callback);
    }

    @Override
    public void create(RestaurantUpsertRequest request, RepositoryCallback<Restaurant> callback) {
        BaseResponse<Void> validation = validate(request, true);
        if (validation != null) {
            postResponse(callback, BaseResponse.error(validation.getError().getCode(), validation.getMessage()));
            return;
        }
        createItem(TABLE, request, Restaurant[].class, callback);
    }

    @Override
    public void update(String id, RestaurantUpsertRequest request, RepositoryCallback<Restaurant> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Restaurant id is required"));
            return;
        }

        BaseResponse<Void> validation = validate(request, false);
        if (validation != null) {
            postResponse(callback, BaseResponse.error(validation.getError().getCode(), validation.getMessage()));
            return;
        }

        updateItem(TABLE, eqIdFilter(id.trim()), request, Restaurant[].class, callback);
    }

    @Override
    public void delete(String id, RepositoryCallback<Void> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Restaurant id is required"));
            return;
        }
        deleteItem(TABLE, eqIdFilter(id.trim()), callback);
    }

    public void updateOpenStatus(String id, boolean isOpen, RepositoryCallback<Restaurant> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Restaurant id is required"));
            return;
        }

        RestaurantUpsertRequest request = new RestaurantUpsertRequest(
                null, null, null, null, null, null, isOpen, null, null, null, null
        );
        updateItem(TABLE, eqIdFilter(id.trim()), request, Restaurant[].class, callback);
    }

    public void getAll(String searchQuery, Boolean isActive, int limit, int offset, RepositoryCallback<List<Restaurant>> callback) {
        StringBuilder queryBuilder = new StringBuilder("?select=*&order=created_at.desc");

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            queryBuilder.append("&name=ilike.*").append(searchQuery.trim()).append("*");
        }
        if (isActive != null) {
            queryBuilder.append("&is_active=eq.").append(isActive);
        }
        if (limit > 0) {
            queryBuilder.append("&limit=").append(limit);
        }
        if (offset >= 0) {
            queryBuilder.append("&offset=").append(offset);
        }

        fetchList(TABLE, queryBuilder.toString(), Restaurant[].class, callback);
    }

    private BaseResponse<Void> validate(RestaurantUpsertRequest request, boolean requireMainFields) {
        if (request == null) {
            return BaseResponse.error("VALIDATION_ERROR", "Restaurant payload is required");
        }

        if (requireMainFields || request.getName() != null) {
            BaseResponse<Void> nameValidation = validateRequired(request.getName(), "Restaurant name");
            if (nameValidation != null) {
                return nameValidation;
            }
        }

        if (requireMainFields || request.getAddress() != null) {
            BaseResponse<Void> addressValidation = validateRequired(request.getAddress(), "Restaurant address");
            if (addressValidation != null) {
                return addressValidation;
            }
        }

        BaseResponse<Void> deliveryFeeValidation = validateNonNegative(request.getDeliveryFee(), "Delivery fee");
        if (deliveryFeeValidation != null) {
            return deliveryFeeValidation;
        }

        return validateNonNegative(request.getMinOrder(), "Min order");
    }
}
