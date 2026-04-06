package com.utt.foodcouriers_admin.data.repository;

import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Category;
import com.utt.foodcouriers_admin.data.repository.base.BaseSupabaseRepository;
import com.utt.foodcouriers_admin.data.repository.base.CrudRepository;
import com.utt.foodcouriers_admin.data.request.CategoryUpsertRequest;

import java.util.List;

public class CategoryRepository extends BaseSupabaseRepository implements CrudRepository<Category, CategoryUpsertRequest> {

    private static final String TABLE = "categories";
    private static CategoryRepository instance;

    public static synchronized CategoryRepository getInstance() {
        if (instance == null) {
            instance = new CategoryRepository();
        }
        return instance;
    }

    @Override
    public void getAll(RepositoryCallback<List<Category>> callback) {
        fetchList(TABLE, "?select=*&order=sort_order.asc", Category[].class, callback);
    }

    @Override
    public void getById(String id, RepositoryCallback<Category> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Category id is required"));
            return;
        }
        fetchSingle(TABLE, eqIdFilter(id.trim()), Category[].class, callback);
    }

    @Override
    public void create(CategoryUpsertRequest request, RepositoryCallback<Category> callback) {
        BaseResponse<Void> validation = validate(request, true);
        if (validation != null) {
            postResponse(callback, BaseResponse.error(validation.getError().getCode(), validation.getMessage()));
            return;
        }
        createItem(TABLE, request, Category[].class, callback);
    }

    @Override
    public void update(String id, CategoryUpsertRequest request, RepositoryCallback<Category> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Category id is required"));
            return;
        }

        BaseResponse<Void> validation = validate(request, false);
        if (validation != null) {
            postResponse(callback, BaseResponse.error(validation.getError().getCode(), validation.getMessage()));
            return;
        }

        updateItem(TABLE, eqIdFilter(id.trim()), request, Category[].class, callback);
    }

    @Override
    public void delete(String id, RepositoryCallback<Void> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Category id is required"));
            return;
        }
        deleteItem(TABLE, eqIdFilter(id.trim()), callback);
    }

    public void updateStatus(String id, boolean isActive, RepositoryCallback<Category> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Category id is required"));
            return;
        }

        updateItem(
                TABLE,
                eqIdFilter(id.trim()),
                new CategoryUpsertRequest(null, null, null, isActive),
                Category[].class,
                callback
        );
    }

    private BaseResponse<Void> validate(CategoryUpsertRequest request, boolean requireName) {
        if (request == null) {
            return BaseResponse.error("VALIDATION_ERROR", "Category payload is required");
        }

        if (requireName || request.getName() != null) {
            BaseResponse<Void> nameValidation = validateRequired(request.getName(), "Category name");
            if (nameValidation != null) {
                return nameValidation;
            }
        }

        return validateNonNegative(request.getSortOrder(), "Sort order");
    }
}
