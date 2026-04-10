package com.utt.foodcouriers_admin.data.repository;

import android.net.Uri;
import android.text.TextUtils;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Promotion;
import com.utt.foodcouriers_admin.data.repository.base.BaseSupabaseRepository;
import com.utt.foodcouriers_admin.data.repository.base.CrudRepository;
import com.utt.foodcouriers_admin.data.request.PromotionUpsertRequest;
import java.util.List;

public class PromotionRepository extends BaseSupabaseRepository implements CrudRepository<Promotion, PromotionUpsertRequest> {

    private static final String TABLE = "promotions";
    private static PromotionRepository instance;

    public static synchronized PromotionRepository getInstance() {
        if (instance == null) {
            instance = new PromotionRepository();
        }
        return instance;
    }

    @Override
    public void getAll(RepositoryCallback<List<Promotion>> callback) {
        getPromotions(null, null, null, 50, 0, callback);
    }

    public void getPromotions(String searchQuery,
                              Boolean isActive,
                              Boolean isValid,
                              int limit,
                              int offset,
                              RepositoryCallback<List<Promotion>> callback) {
        StringBuilder query = new StringBuilder("?select=*&order=created_at.desc");
        if (limit > 0) {
            query.append("&limit=").append(limit);
        }
        if (offset > 0) {
            query.append("&offset=").append(offset);
        }
        if (!TextUtils.isEmpty(searchQuery)) {
            String encoded = Uri.encode("%" + searchQuery.trim() + "%");
            query.append("&or=(code.ilike.").append(encoded).append(",name.ilike.").append(encoded).append(")");
        }
        if (isActive != null) {
            query.append("&is_active=eq.").append(isActive ? "true" : "false");
        }
        if (isValid != null && isValid) {
            query.append("&start_date=lte.").append(Uri.encode(getCurrentTimestamp()));
            query.append("&end_date=gte.").append(Uri.encode(getCurrentTimestamp()));
        }
        fetchList(TABLE, query.toString(), Promotion[].class, callback);
    }

    @Override
    public void getById(String id, RepositoryCallback<Promotion> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Promotion id is required"));
            return;
        }
        fetchSingle(TABLE, eqIdFilter(id.trim()), Promotion[].class, callback);
    }

    @Override
    public void create(PromotionUpsertRequest request, RepositoryCallback<Promotion> callback) {
        BaseResponse<Void> validation = validate(request, true);
        if (validation != null) {
            postResponse(callback, BaseResponse.error(validation.getError().getCode(), validation.getMessage()));
            return;
        }
        createItem(TABLE, request, Promotion[].class, callback);
    }

    @Override
    public void update(String id, PromotionUpsertRequest request, RepositoryCallback<Promotion> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Promotion id is required"));
            return;
        }

        BaseResponse<Void> validation = validate(request, false);
        if (validation != null) {
            postResponse(callback, BaseResponse.error(validation.getError().getCode(), validation.getMessage()));
            return;
        }

        updateItem(TABLE, eqIdFilter(id.trim()), request, Promotion[].class, callback);
    }

    @Override
    public void delete(String id, RepositoryCallback<Void> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Promotion id is required"));
            return;
        }
        deleteItem(TABLE, eqIdFilter(id.trim()), callback);
    }

    public void updateStatus(String id, boolean isActive, RepositoryCallback<Promotion> callback) {
        if (id == null || id.trim().isEmpty()) {
            postResponse(callback, BaseResponse.error("VALIDATION_ERROR", "Promotion id is required"));
            return;
        }

        updateItem(
                TABLE,
                eqIdFilter(id.trim()),
                new PromotionUpsertRequest(null, null, null, null, null, null, null, null, null, null, isActive),
                Promotion[].class,
                callback
        );
    }

    private BaseResponse<Void> validate(PromotionUpsertRequest request, boolean requireAll) {
        if (request == null) {
            return BaseResponse.error("VALIDATION_ERROR", "Promotion payload is required");
        }

        if (requireAll || request.getCode() != null) {
            BaseResponse<Void> codeValidation = validateRequired(request.getCode(), "Code");
            if (codeValidation != null) {
                return codeValidation;
            }
        }

        if (requireAll || request.getName() != null) {
            BaseResponse<Void> nameValidation = validateRequired(request.getName(), "Name");
            if (nameValidation != null) {
                return nameValidation;
            }
        }

        if (requireAll || request.getDiscountType() != null) {
            BaseResponse<Void> typeValidation = validateRequired(request.getDiscountType(), "Discount type");
            if (typeValidation != null) {
                return typeValidation;
            }
            if (!"percent".equals(request.getDiscountType()) && !"fixed".equals(request.getDiscountType())) {
                return BaseResponse.error("VALIDATION_ERROR", "Discount type must be 'percent' or 'fixed'");
            }
        }

        if (requireAll || request.getDiscountValue() != null) {
            BaseResponse<Void> valueValidation = validateNonNegative(request.getDiscountValue(), "Discount value");
            if (valueValidation != null) {
                return valueValidation;
            }
        }

        if (requireAll || request.getStartDate() != null) {
            BaseResponse<Void> startValidation = validateRequired(request.getStartDate(), "Start date");
            if (startValidation != null) {
                return startValidation;
            }
        }

        if (requireAll || request.getEndDate() != null) {
            BaseResponse<Void> endValidation = validateRequired(request.getEndDate(), "End date");
            if (endValidation != null) {
                return endValidation;
            }
        }

        BaseResponse<Void> minOrderValidation = validateNonNegative(request.getMinOrder(), "Min order");
        if (minOrderValidation != null) {
            return minOrderValidation;
        }

        BaseResponse<Void> maxDiscountValidation = validateNonNegative(request.getMaxDiscount(), "Max discount");
        if (maxDiscountValidation != null) {
            return maxDiscountValidation;
        }

        return validateNonNegative(request.getUsageLimit(), "Usage limit");
    }

    private String getCurrentTimestamp() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new java.util.Date());
    }
}