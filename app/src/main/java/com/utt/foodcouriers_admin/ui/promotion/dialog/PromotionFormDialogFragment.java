package com.utt.foodcouriers_admin.ui.promotion.dialog;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.Promotion;
import com.utt.foodcouriers_admin.data.request.PromotionUpsertRequest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PromotionFormDialogFragment extends DialogFragment {

    public interface PromotionFormListener {
        void onSubmit(@Nullable String promotionId, PromotionUpsertRequest request, PromotionFormDialogFragment dialog);
    }

    private static final String ARG_PROMOTION = "arg_promotion";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));

    public static PromotionFormDialogFragment newInstance(@Nullable Promotion promotion) {
        PromotionFormDialogFragment fragment = new PromotionFormDialogFragment();
        Bundle bundle = new Bundle();
        if (promotion != null) {
            bundle.putSerializable(ARG_PROMOTION, promotion);
        }
        fragment.setArguments(bundle);
        return fragment;
    }

    private Promotion promotion;
    private PromotionFormListener listener;

    private TextInputLayout tilCode, tilName, tilDescription, tilDiscountType, tilDiscountValue;
    private TextInputLayout tilMinOrder, tilMaxDiscount, tilStartDate, tilEndDate, tilUsageLimit;
    private TextInputEditText etCode, etName, etDescription, etDiscountType, etDiscountValue;
    private TextInputEditText etMinOrder, etMaxDiscount, etStartDate, etEndDate, etUsageLimit;
    private MaterialSwitch swIsActive;
    private MaterialButton btnSave;
    private MaterialButton btnCancel;
    private CircularProgressIndicator progressSave;
    private TextView tvFormTitle;

    private String selectedDiscountType = "percent";
    private Calendar startDateCalendar = Calendar.getInstance();
    private Calendar endDateCalendar = Calendar.getInstance();
    private boolean isStartDateSelected = false;
    private boolean isEndDateSelected = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog_Alert);
        if (getArguments() != null) {
            promotion = (Promotion) getArguments().getSerializable(ARG_PROMOTION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_promotion_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        bindPromotion();
        setupListeners();
    }

    public void setPromotionFormListener(PromotionFormListener listener) {
        this.listener = listener;
    }

    public void setLoading(boolean loading) {
        if (btnSave != null) {
            btnSave.setEnabled(!loading);
            btnSave.setText(loading ? getString(R.string.promotion_saving) : getString(R.string.action_save));
        }
        if (progressSave != null) {
            progressSave.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (btnCancel != null) {
            btnCancel.setEnabled(!loading);
        }
    }

    private void initViews(View view) {
        tilCode = view.findViewById(R.id.til_code);
        tilName = view.findViewById(R.id.til_name);
        tilDescription = view.findViewById(R.id.til_description);
        tilDiscountType = view.findViewById(R.id.til_discount_type);
        tilDiscountValue = view.findViewById(R.id.til_discount_value);
        tilMinOrder = view.findViewById(R.id.til_min_order);
        tilMaxDiscount = view.findViewById(R.id.til_max_discount);
        tilStartDate = view.findViewById(R.id.til_start_date);
        tilEndDate = view.findViewById(R.id.til_end_date);
        tilUsageLimit = view.findViewById(R.id.til_usage_limit);

        etCode = view.findViewById(R.id.et_code);
        etName = view.findViewById(R.id.et_name);
        etDescription = view.findViewById(R.id.et_description);
        etDiscountType = view.findViewById(R.id.et_discount_type);
        etDiscountValue = view.findViewById(R.id.et_discount_value);
        etMinOrder = view.findViewById(R.id.et_min_order);
        etMaxDiscount = view.findViewById(R.id.et_max_discount);
        etStartDate = view.findViewById(R.id.et_start_date);
        etEndDate = view.findViewById(R.id.et_end_date);
        etUsageLimit = view.findViewById(R.id.et_usage_limit);

        swIsActive = view.findViewById(R.id.sw_is_active);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);
        progressSave = view.findViewById(R.id.progress_save);
        tvFormTitle = view.findViewById(R.id.tv_form_title);
    }

    private void bindPromotion() {
        boolean isEdit = promotion != null && !TextUtils.isEmpty(promotion.getId());
        
        if (etCode != null && tilCode != null && isEdit) {
            etCode.setText(promotion.getCode());
            etCode.setEnabled(false);
            tilCode.setEnabled(false);
        }
        if (etName != null && tilName != null && isEdit) {
            etName.setText(promotion.getName());
        }
        if (etDescription != null && tilDescription != null && isEdit) {
            etDescription.setText(promotion.getDescription());
        }
        if (swIsActive != null) {
            swIsActive.setChecked(isEdit ? promotion.isActive() : true);
        }
        
        if (isEdit) {
            selectedDiscountType = promotion.getDiscountType();
            if (etDiscountType != null) {
                etDiscountType.setText("percent".equals(selectedDiscountType) ? 
                    getString(R.string.label_discount_percent) : getString(R.string.label_discount_fixed));
            }
            
            if (etDiscountValue != null && promotion.getDiscountValue() > 0) {
                etDiscountValue.setText(String.valueOf(promotion.getDiscountValue()));
            }
            if (etMinOrder != null && promotion.getMinOrder() > 0) {
                etMinOrder.setText(String.valueOf(promotion.getMinOrder()));
            }
            if (etMaxDiscount != null && promotion.getMaxDiscount() != null && promotion.getMaxDiscount() > 0) {
                etMaxDiscount.setText(String.valueOf(promotion.getMaxDiscount()));
            }
            if (promotion.getStartDate() != null && etStartDate != null) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                    java.util.Date start = inputFormat.parse(promotion.getStartDate());
                    if (start != null) {
                        startDateCalendar.setTime(start);
                        etStartDate.setText(DISPLAY_FORMAT.format(start));
                        isStartDateSelected = true;
                    }
                } catch (Exception e) { }
            }
            if (promotion.getEndDate() != null && etEndDate != null) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                    java.util.Date end = inputFormat.parse(promotion.getEndDate());
                    if (end != null) {
                        endDateCalendar.setTime(end);
                        etEndDate.setText(DISPLAY_FORMAT.format(end));
                        isEndDateSelected = true;
                    }
                } catch (Exception e) { }
            }
            if (etUsageLimit != null && promotion.getUsageLimit() != null && promotion.getUsageLimit() > 0) {
                etUsageLimit.setText(String.valueOf(promotion.getUsageLimit()));
            }
        }
    }

    private void setupListeners() {
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> handleSubmit());
        }
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismiss());
        }
        if (etDiscountType != null) {
            etDiscountType.setOnClickListener(v -> showDiscountTypeDialog());
        }
        if (etStartDate != null) {
            etStartDate.setOnClickListener(v -> showDatePicker(true));
        }
        if (etEndDate != null) {
            etEndDate.setOnClickListener(v -> showDatePicker(false));
        }
    }

    private void showDiscountTypeDialog() {
        if (getContext() == null) return;
        String[] options = {getString(R.string.label_discount_percent), getString(R.string.label_discount_fixed)};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.label_discount_type)
                .setItems(options, (dialog, which) -> {
                    selectedDiscountType = which == 0 ? "percent" : "fixed";
                    if (etDiscountType != null) {
                        etDiscountType.setText(options[which]);
                    }
                })
                .show();
    }

    private void showDatePicker(boolean isStartDate) {
        if (getContext() == null) return;
        Calendar calendar = isStartDate ? startDateCalendar : endDateCalendar;
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth, 0, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    
                    if (isStartDate) {
                        startDateCalendar = selected;
                        isStartDateSelected = true;
                        if (etStartDate != null) {
                            etStartDate.setText(DISPLAY_FORMAT.format(selected.getTime()));
                        }
                    } else {
                        endDateCalendar = selected;
                        isEndDateSelected = true;
                        if (etEndDate != null) {
                            etEndDate.setText(DISPLAY_FORMAT.format(selected.getTime()));
                        }
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void handleSubmit() {
        if (!validate()) {
            return;
        }

        String code = etCode.getText() != null ? etCode.getText().toString().trim().toUpperCase() : null;
        String name = etName.getText() != null ? etName.getText().toString().trim() : null;
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : null;
        
        Integer discountValue = parseInteger(etDiscountValue.getText());
        Integer minOrder = parseInteger(etMinOrder.getText());
        Integer maxDiscount = parseInteger(etMaxDiscount.getText());
        Integer usageLimit = parseInteger(etUsageLimit.getText());
        
        String startDate = isStartDateSelected ? DATE_FORMAT.format(startDateCalendar.getTime()) : null;
        String endDate = isEndDateSelected ? DATE_FORMAT.format(endDateCalendar.getTime()) : null;

        PromotionUpsertRequest request = new PromotionUpsertRequest(
                code, name, description,
                selectedDiscountType, discountValue,
                minOrder, maxDiscount,
                startDate, endDate,
                usageLimit, swIsActive.isChecked()
        );

        if (listener != null) {
            listener.onSubmit(promotion != null ? promotion.getId() : null, request, this);
        }
    }

    private boolean validate() {
        boolean isValid = true;
        if (tilCode != null) tilCode.setError(null);
        if (tilName != null) tilName.setError(null);
        if (tilDiscountType != null) tilDiscountType.setError(null);
        if (tilDiscountValue != null) tilDiscountValue.setError(null);
        if (tilStartDate != null) tilStartDate.setError(null);
        if (tilEndDate != null) tilEndDate.setError(null);

        if (etCode == null || TextUtils.isEmpty(etCode.getText())) {
            if (tilCode != null) tilCode.setError(getString(R.string.error_field_required));
            isValid = false;
        }

        if (etName == null || TextUtils.isEmpty(etName.getText())) {
            if (tilName != null) tilName.setError(getString(R.string.error_field_required));
            isValid = false;
        }

        if (etDiscountValue == null || TextUtils.isEmpty(etDiscountValue.getText())) {
            if (tilDiscountValue != null) tilDiscountValue.setError(getString(R.string.error_field_required));
            isValid = false;
        } else {
            Integer value = parseInteger(etDiscountValue.getText());
            if (value == null || value <= 0) {
                if (tilDiscountValue != null) tilDiscountValue.setError(getString(R.string.error_discount_value_invalid));
                isValid = false;
            }
            if ("percent".equals(selectedDiscountType) && value != null && value > 100) {
                if (tilDiscountValue != null) tilDiscountValue.setError(getString(R.string.error_discount_value_invalid));
                isValid = false;
            }
        }

        if (!isStartDateSelected) {
            if (tilStartDate != null) tilStartDate.setError(getString(R.string.error_field_required));
            isValid = false;
        }

        if (!isEndDateSelected) {
            if (tilEndDate != null) tilEndDate.setError(getString(R.string.error_field_required));
            isValid = false;
        }

        if (isStartDateSelected && isEndDateSelected && startDateCalendar.after(endDateCalendar)) {
            if (tilEndDate != null) tilEndDate.setError(getString(R.string.error_invalid_date_range));
            isValid = false;
        }

        return isValid;
    }

    private Integer parseInteger(Editable text) {
        if (text == null) return null;
        String value = text.toString().trim();
        if (TextUtils.isEmpty(value)) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}