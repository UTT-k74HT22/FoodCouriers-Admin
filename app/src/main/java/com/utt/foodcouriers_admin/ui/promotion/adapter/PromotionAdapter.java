package com.utt.foodcouriers_admin.ui.promotion.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.model.Promotion;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class PromotionAdapter extends ListAdapter<Promotion, PromotionAdapter.PromotionViewHolder> {

    public interface PromotionActionListener {
        void onEdit(Promotion promotion);
        void onStatusChange(Promotion promotion, boolean isActive);
        void onDelete(Promotion promotion);
    }

    private PromotionActionListener listener;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public PromotionAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setListener(PromotionActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_promotion, parent, false);
        return new PromotionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromotionViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    private static final DiffUtil.ItemCallback<Promotion> DIFF_CALLBACK = new DiffUtil.ItemCallback<Promotion>() {
        @Override
        public boolean areItemsTheSame(@NonNull Promotion oldItem, @NonNull Promotion newItem) {
            if (oldItem.getId() == null || newItem.getId() == null) {
                return false;
            }
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Promotion oldItem, @NonNull Promotion newItem) {
            return TextUtils.equals(oldItem.getCode(), newItem.getCode())
                    && TextUtils.equals(oldItem.getName(), newItem.getName())
                    && TextUtils.equals(oldItem.getDescription(), newItem.getDescription())
                    && TextUtils.equals(oldItem.getDiscountType(), newItem.getDiscountType())
                    && oldItem.getDiscountValue() == newItem.getDiscountValue()
                    && oldItem.getUsageCount() == newItem.getUsageCount()
                    && oldItem.isActive() == newItem.isActive()
                    && TextUtils.equals(oldItem.getStartDate(), newItem.getStartDate())
                    && TextUtils.equals(oldItem.getEndDate(), newItem.getEndDate());
        }
    };

    class PromotionViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCode;
        private final TextView tvStatus;
        private final TextView tvName;
        private final TextView tvDescription;
        private final TextView tvDiscountInfo;
        private final TextView tvMinOrder;
        private final TextView tvDateRange;
        private final TextView tvUsageInfo;
        private final MaterialSwitch swIsActive;
        private final ImageButton btnEdit;
        private final ImageButton btnMore;

        PromotionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tv_promotion_code);
            tvStatus = itemView.findViewById(R.id.tv_promotion_status);
            tvName = itemView.findViewById(R.id.tv_promotion_name);
            tvDescription = itemView.findViewById(R.id.tv_promotion_description);
            tvDiscountInfo = itemView.findViewById(R.id.tv_discount_info);
            tvMinOrder = itemView.findViewById(R.id.tv_min_order);
            tvDateRange = itemView.findViewById(R.id.tv_date_range);
            tvUsageInfo = itemView.findViewById(R.id.tv_usage_info);
            swIsActive = itemView.findViewById(R.id.sw_is_active);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnMore = itemView.findViewById(R.id.btn_more);
        }

        void bind(Promotion promotion) {
            tvCode.setText(promotion.getCode());

            boolean isActive = promotion.isActive();
            boolean isValid = isPromotionValid(promotion);

            tvStatus.setText(isValid ? (isActive ? R.string.status_active : R.string.status_hidden) : R.string.status_expired);
            tvStatus.setBackgroundResource(isValid ? (isActive ? R.drawable.admin_badge_success : R.drawable.admin_badge_pending) : R.drawable.admin_badge_danger);

            tvName.setText(promotion.getName());
            tvDescription.setText(promotion.getDescription());
            tvDescription.setVisibility(TextUtils.isEmpty(promotion.getDescription()) ? View.GONE : View.VISIBLE);

            String discountText = promotion.isPercent()
                    ? itemView.getContext().getString(R.string.discount_percent, promotion.getDiscountValue())
                    : itemView.getContext().getString(R.string.discount_fixed, currencyFormat.format(promotion.getDiscountValue()));
            tvDiscountInfo.setText(discountText);

            if (promotion.getMinOrder() > 0) {
                tvMinOrder.setText(itemView.getContext().getString(R.string.min_order, currencyFormat.format(promotion.getMinOrder())));
                tvMinOrder.setVisibility(View.VISIBLE);
            } else {
                tvMinOrder.setVisibility(View.GONE);
            }

            tvDateRange.setText(formatDateRange(promotion.getStartDate(), promotion.getEndDate()));

            int used = promotion.getUsageCount();
            Integer limit = promotion.getUsageLimit();
            if (limit != null && limit > 0) {
                tvUsageInfo.setText(itemView.getContext().getString(R.string.usage_count, used, limit));
                tvUsageInfo.setVisibility(View.VISIBLE);
            } else {
                String unlimited = itemView.getContext().getString(R.string.label_usage_unlimited);
                tvUsageInfo.setText(used + " (" + unlimited + ")");
                tvUsageInfo.setVisibility(View.VISIBLE);
            }

            swIsActive.setOnCheckedChangeListener(null);
            swIsActive.setChecked(isActive);
            final var thisListener = listener;
            swIsActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String title = isChecked ? "Kích hoạt khuyến mãi" : "Vô hiệu khuyến mãi";
                String message = isChecked ? "Bạn có muốn kích hoạt khuyến mãi này?"
                        : "Khuyến mãi sẽ bị vô hiệu hóa. Bạn có chắc chắn?";
                new MaterialAlertDialogBuilder(itemView.getContext())
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("Đồng ý", (dialog, which) -> {
                            if (thisListener != null) {
                                thisListener.onStatusChange(promotion, isChecked);
                            }
                        })
                        .setNegativeButton("Hủy", (dialog, which) -> {
                            swIsActive.setOnCheckedChangeListener(null);
                            swIsActive.setChecked(!isChecked);
                        })
                        .setOnCancelListener(dialog -> {
                            swIsActive.setOnCheckedChangeListener(null);
                            swIsActive.setChecked(!isChecked);
                        })
                        .show();
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(promotion);
                }
            });

            btnMore.setOnClickListener(v -> showPopupMenu(v, promotion));

            itemView.setContentDescription(promotion.getCode() + " - " + promotion.getName());
        }

        private void showPopupMenu(View anchor, Promotion promotion) {
            PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
            popupMenu.inflate(R.menu.menu_promotion_item);
            popupMenu.setOnMenuItemClickListener(menuItem -> handleMenuItem(menuItem, promotion));
            popupMenu.show();
        }

        private boolean handleMenuItem(MenuItem menuItem, Promotion promotion) {
            if (listener == null) {
                return false;
            }
            int id = menuItem.getItemId();
            if (id == R.id.action_delete) {
                listener.onDelete(promotion);
                return true;
            }
            return false;
        }

        private boolean isPromotionValid(Promotion promotion) {
            if (promotion.getStartDate() == null || promotion.getEndDate() == null) {
                return false;
            }
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date now = new Date();
                Date start = sdf.parse(promotion.getStartDate());
                Date end = sdf.parse(promotion.getEndDate());
                return start != null && end != null && now.after(start) && now.before(end);
            } catch (Exception e) {
                return false;
            }
        }

        private String formatDateRange(String startDate, String endDate) {
            if (TextUtils.isEmpty(startDate) || TextUtils.isEmpty(endDate)) {
                return "--";
            }
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
                Date start = inputFormat.parse(startDate);
                Date end = inputFormat.parse(endDate);
                if (start != null && end != null) {
                    return outputFormat.format(start) + " - " + outputFormat.format(end);
                }
            } catch (Exception e) {
            }
            return startDate + " - " + endDate;
        }
    }
}