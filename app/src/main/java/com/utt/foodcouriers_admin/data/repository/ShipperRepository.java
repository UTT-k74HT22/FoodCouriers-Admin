package com.utt.foodcouriers_admin.data.repository;

import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.ShipperProfile;
import com.utt.foodcouriers_admin.data.model.User;
import com.utt.foodcouriers_admin.data.repository.base.BaseSupabaseRepository;
import com.utt.foodcouriers_admin.data.repository.base.CrudRepository;
import com.utt.foodcouriers_admin.data.request.ShipperUpsertRequest;
import com.utt.foodcouriers_admin.data.request.AdminCreateUserAccountRequest;
import java.util.List;

public class ShipperRepository extends BaseSupabaseRepository implements CrudRepository<ShipperProfile, ShipperUpsertRequest> {

    private static final String TABLE = "shippers";
    private static ShipperRepository instance;

    public static synchronized ShipperRepository getInstance() {
        if (instance == null) {
            instance = new ShipperRepository();
        }
        return instance;
    }

    @Override
    public void getAll(RepositoryCallback<List<ShipperProfile>> callback) {
        fetchList(TABLE, "?select=*,user:users!shippers_user_id_fkey(full_name,phone,email,avatar_url),restaurant:restaurants(name)&order=created_at.desc", ShipperProfile[].class, callback);
    }

    public void getByRestaurant(String restaurantId, RepositoryCallback<List<ShipperProfile>> callback) {
        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Restaurant id is required"));
            return;
        }
        fetchList(TABLE, "?select=*,user:users!shippers_user_id_fkey(full_name,phone,email,avatar_url),restaurant:restaurants(name)&restaurant_id=eq." + restaurantId.trim() + "&order=created_at.desc", ShipperProfile[].class, callback);
    }

    @Override
    public void getById(String id, RepositoryCallback<ShipperProfile> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Shipper id is required"));
            return;
        }
        fetchSingle(TABLE, eqIdFilter(id.trim()), ShipperProfile[].class, callback);
    }

    @Override
    public void create(ShipperUpsertRequest request, RepositoryCallback<ShipperProfile> callback) {
        BaseResponse<Void> validation = validate(request, true);
        if (validation != null) {
            postResponse(callback, BaseResponse.error(validation.getError().getCode(), validation.getMessage()));
            return;
        }

        final String fullName = request.getFullName();
        final String phone = request.getPhone();
        final String email = request.getEmail();
        final String password = request.getPassword();
        final String avatarUrl = request.getAvatarUrl();
        final String restaurantId = request.getRestaurantId();
        final String licensePlate = request.getLicensePlate();
        final String vehicleType = request.getVehicleType();
        final Boolean isActive = request.getIsActive();

        AdminCreateUserAccountRequest userRequest = new AdminCreateUserAccountRequest(
                email,
                password,
                fullName,
                phone,
                avatarUrl,
                "shipper",
                isActive,
                true
        );

        createUserAndShipper(userRequest, restaurantId, licensePlate, vehicleType, isActive, callback);
    }

    private void createUserAndShipper(AdminCreateUserAccountRequest userRequest, String restaurantId, 
                                       String licensePlate, String vehicleType, Boolean isActive,
                                       RepositoryCallback<ShipperProfile> callback) {
        final AdminUserAccountRepository adminUserRepository = AdminUserAccountRepository.getInstance();

        adminUserRepository.createAccount(userRequest, new RepositoryCallback<User>() {
            @Override
            public void onComplete(BaseResponse<User> response) {
                if (!response.isSuccess()) {
                    postResponse(callback, BaseResponse.error(response.getError() != null ? response.getError().getCode() : "USER_CREATE_FAILED", "Failed to create user: " + response.getMessage()));
                    return;
                }

                String userId = response.getData().getId();

                ShipperUpsertRequest shipperRequest = new ShipperUpsertRequest();
                shipperRequest.setUserId(userId);
                shipperRequest.setRestaurantId(restaurantId);
                shipperRequest.setLicensePlate(licensePlate);
                shipperRequest.setVehicleType(vehicleType);
                shipperRequest.setIsAvailable(true);
                shipperRequest.setIsActive(isActive);
                shipperRequest.setTotalDelivered(0);
                shipperRequest.setTotalRevenue(0L);

                createItem(TABLE, shipperRequest, ShipperProfile[].class, new RepositoryCallback<ShipperProfile>() {
                    @Override
                    public void onComplete(BaseResponse<ShipperProfile> response) {
                        if (!response.isSuccess()) {
                            UserRepository.getInstance().delete(userId, new RepositoryCallback<Void>() {
                                @Override
                                public void onComplete(BaseResponse<Void> deleteResponse) {
                                }
                            });
                            postResponse(callback, BaseResponse.error(response.getError() != null ? response.getError().getCode() : "SHIPPER_CREATE_FAILED", "Failed to create shipper: " + response.getMessage()));
                            return;
                        }
                        postResponse(callback, response);
                    }
                });
            }
        });
    }

    @Override
    public void update(String id, ShipperUpsertRequest request, RepositoryCallback<ShipperProfile> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Shipper id is required"));
            return;
        }
        BaseResponse<Void> validation = validate(request, false);
        if (validation != null) {
            postResponse(callback, BaseResponse.error(validation.getError().getCode(), validation.getMessage()));
            return;
        }

        getById(id, new RepositoryCallback<ShipperProfile>() {
            @Override
            public void onComplete(BaseResponse<ShipperProfile> response) {
                if (!response.isSuccess()) {
                    postResponse(callback, response);
                    return;
                }

                final String currentUserId = response.getData().getUserId();

                if (currentUserId != null && (request.getFullName() != null || request.getPhone() != null || request.getEmail() != null || request.getAvatarUrl() != null)) {
                    com.utt.foodcouriers_admin.data.request.UserUpdateRequest userRequest = new com.utt.foodcouriers_admin.data.request.UserUpdateRequest();
                    if (request.getFullName() != null) userRequest.setFullName(request.getFullName());
                    if (request.getPhone() != null) userRequest.setPhone(request.getPhone());
                    if (request.getEmail() != null) userRequest.setEmail(request.getEmail());
                    if (request.getAvatarUrl() != null) userRequest.setAvatarUrl(request.getAvatarUrl());
                    if (request.getIsActive() != null) userRequest.setIsActive(request.getIsActive());

                    UserRepository.getInstance().update(currentUserId, userRequest, new RepositoryCallback<User>() {
                        @Override
                        public void onComplete(BaseResponse<User> userResponse) {
                            if (!userResponse.isSuccess()) {
                                postResponse(callback, BaseResponse.error(userResponse.getError() != null ? userResponse.getError().getCode() : "USER_UPDATE_FAILED", "Failed to update user: " + userResponse.getMessage()));
                                return;
                            }
                            updateShipper();
                        }
                    });
                } else {
                    updateShipper();
                }
            }

            private void updateShipper() {
                ShipperUpsertRequest shipperOnlyRequest = new ShipperUpsertRequest();
                shipperOnlyRequest.setRestaurantId(request.getRestaurantId());
                shipperOnlyRequest.setLicensePlate(request.getLicensePlate());
                shipperOnlyRequest.setVehicleType(request.getVehicleType());
                shipperOnlyRequest.setIsAvailable(request.getIsAvailable());
                shipperOnlyRequest.setIsActive(request.getIsActive());
                updateItem(TABLE, eqIdFilter(id.trim()), shipperOnlyRequest, ShipperProfile[].class, callback);
            }
        });
    }

    @Override
    public void delete(String id, RepositoryCallback<Void> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Shipper id is required"));
            return;
        }
        
        getById(id, new RepositoryCallback<ShipperProfile>() {
            @Override
            public void onComplete(BaseResponse<ShipperProfile> response) {
                if (!response.isSuccess()) {
                    deleteItem(TABLE, eqIdFilter(id.trim()), callback);
                    return;
                }
                
                final String userId = response.getData().getUserId();
                
                deleteItem(TABLE, eqIdFilter(id.trim()), new RepositoryCallback<Void>() {
                    @Override
                    public void onComplete(BaseResponse<Void> deleteResponse) {
                        if (deleteResponse.isSuccess() && userId != null) {
                            UserRepository.getInstance().delete(userId, new RepositoryCallback<Void>() {
                                @Override
                                public void onComplete(BaseResponse<Void> userDeleteResponse) {
                                }
                            });
                        }
                        callback.onComplete(deleteResponse);
                    }
                });
            }
        });
    }

    public void updateStatus(String id, boolean isActive, RepositoryCallback<ShipperProfile> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Shipper id is required"));
            return;
        }
        ShipperUpsertRequest request = new ShipperUpsertRequest();
        request.setIsActive(isActive);
        updateItem(TABLE, eqIdFilter(id.trim()), request, ShipperProfile[].class, callback);
    }

    public void getProfileByUserId(String userId, RepositoryCallback<ShipperProfile> callback) {
        if (userId == null || userId.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "User id is required"));
            return;
        }
        fetchSingle(TABLE, "?user_id=eq." + userId + "&select=*,user:users!shippers_user_id_fkey(full_name,phone,email,avatar_url),restaurant:restaurants(name)", ShipperProfile[].class, callback);
    }

    /**
     * Shipper tự cập nhật thông tin cá nhân (biển số xe, loại xe)
     */
    public void updateSelf(String shipperId, String licensePlate, String vehicleType, RepositoryCallback<ShipperProfile> callback) {
        ShipperUpsertRequest request = new ShipperUpsertRequest();
        request.setLicensePlate(licensePlate);
        request.setVehicleType(vehicleType);
        updateItem(TABLE, eqIdFilter(shipperId), request, ShipperProfile[].class, callback);
    }

    /**
     * Cập nhật trạng thái sẵn sàng giao hàng (Online/Offline)
     */
    public void updateAvailability(String shipperId, boolean isAvailable, RepositoryCallback<ShipperProfile> callback) {
        ShipperUpsertRequest request = new ShipperUpsertRequest();
        request.setIsAvailable(isAvailable);
        updateItem(TABLE, eqIdFilter(shipperId), request, ShipperProfile[].class, callback);
    }

    private BaseResponse<Void> validate(ShipperUpsertRequest request, boolean requireMainFields) {
        if (request == null) {
            return BaseResponse.error("VALIDATION_ERROR", "Shipper payload is required");
        }
        if (requireMainFields || request.getRestaurantId() != null) {
            BaseResponse<Void> validation = validateRequired(request.getRestaurantId(), "Restaurant id");
            if (validation != null) return validation;
        }
        return null;
    }
}