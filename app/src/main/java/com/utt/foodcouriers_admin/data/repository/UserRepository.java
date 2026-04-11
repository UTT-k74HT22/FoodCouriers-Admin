package com.utt.foodcouriers_admin.data.repository;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.User;
import com.utt.foodcouriers_admin.data.repository.base.BaseSupabaseRepository;
import com.utt.foodcouriers_admin.data.request.UserCreateRequest;
import com.utt.foodcouriers_admin.data.request.UserUpdateRequest;

import java.util.List;

public class UserRepository extends BaseSupabaseRepository {

    private static final String TABLE = "users";
    private static UserRepository instance;

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    public void getAll(RepositoryCallback<List<User>> callback) {
        getUsers(null, null, null, 20, 0, callback);
    }

    public void getById(String id, RepositoryCallback<User> callback) {

        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "User id is required"));
            return;
        }
        fetchSingle(TABLE, eqIdFilter(id.trim()), User[].class, callback);
    }

    public void create(UserCreateRequest request, RepositoryCallback<User> callback) {
        BaseResponse<Void> validation = validateCreate(request);
        if (validation != null) {
            postResponse(callback, BaseResponse.error(validation.getError().getCode(), validation.getMessage()));
            return;
        }
        createItem(TABLE, request, User[].class, callback);
    }

    public void update(String id, UserUpdateRequest request, RepositoryCallback<User> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "User id is required"));
            return;
        }

        BaseResponse<Void> validation = validateUpdate(request);
        if (validation != null) {
            postResponse(callback, BaseResponse.error(validation.getError().getCode(), validation.getMessage()));
            return;
        }

        updateItem(TABLE, eqIdFilter(id.trim()), request, User[].class, callback);
    }

    public void delete(String id, RepositoryCallback<Void> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "User id is required"));
            return;
        }

        UserUpdateRequest softDeleteRequest = new UserUpdateRequest();
        softDeleteRequest.setIsActive(false);

        updateItem(TABLE, eqIdFilter(id.trim()), softDeleteRequest, User[].class, new RepositoryCallback<User>() {
            @Override
            public void onComplete(BaseResponse<User> response) {
                if (response.isSuccess()) {
                    postResponse(callback, BaseResponse.success(null, "User deactivated successfully"));
                    return;
                }

                String errorCode = response.getError() != null
                        ? response.getError().getCode()
                        : "SOFT_DELETE_FAILED";
                postResponse(callback, BaseResponse.error(errorCode, response.getMessage()));
            }
        });
    }

    public void getUsers(String query, String role, Boolean isActive, int limit, int offSet, RepositoryCallback<List<User>> callback) {

        Log.d("UserRepository", "getUsers: query=" + query + ", role=" + role + ", limit=" + limit + ", offset=" + offSet);
        StringBuilder sql = new StringBuilder("?select=*&order=created_at.desc");
        if (limit > 0) {
            sql.append("&limit=").append(limit);
        }

        if (offSet > 0) {
            sql.append("&offset=").append(offSet);
        }

        if (!TextUtils.isEmpty(role)) {
            sql.append("&role=eq.").append(Uri.encode(role.trim()));
        }

        if (isActive != null) {
            sql.append("&is_active=eq.").append(isActive ? "true" : "false");
        }

        if (!TextUtils.isEmpty(query)) {
            String encodedQuery = query.trim().replace(",", "\\,");
            sql.append("&or=(")
                    .append("full_name.ilike.*").append(encodedQuery).append("*,")
                    .append("email.ilike.*").append(encodedQuery).append("*,")
                    .append("phone.ilike.*").append(encodedQuery).append("*)");
        }
        fetchList(TABLE, sql.toString(), User[].class, callback);
    }

    private BaseResponse<Void> validateCreate(UserCreateRequest request) {
        if (request == null) {
            return BaseResponse.error("VALIDATION_ERROR", "User payload is required");
        }

        BaseResponse<Void> fullNameValidation = validateRequired(request.getFullName(), "Full name");
        if (fullNameValidation != null) {
            return fullNameValidation;
        }

        BaseResponse<Void> phoneValidation = validateRequired(request.getPhone(), "Phone");
        if (phoneValidation != null) {
            return phoneValidation;
        }

        return null;
    }

    private BaseResponse<Void> validateUpdate(UserUpdateRequest request) {
        if (request == null) {
            return BaseResponse.error("VALIDATION_ERROR", "User payload is required");
        }

        if (request.getFullName() != null) {
            BaseResponse<Void> fullNameValidation = validateRequired(request.getFullName(), "Full name");
            if (fullNameValidation != null) {
                return fullNameValidation;
            }
        }

        if (request.getPhone() != null) {
            BaseResponse<Void> phoneValidation = validateRequired(request.getPhone(), "Phone");
            if (phoneValidation != null) {
                return phoneValidation;
            }
        }

        if (request.getEmail() != null) {
            BaseResponse<Void> emailValidation = validateRequired(request.getEmail(), "Email");
            if (emailValidation != null) {
                return emailValidation;
            }
        }

        return null;
    }
}
