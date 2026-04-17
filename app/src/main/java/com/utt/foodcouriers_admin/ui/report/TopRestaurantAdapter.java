package com.utt.foodcouriers_admin.ui.report;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.utt.foodcouriers_admin.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TopRestaurantAdapter extends RecyclerView.Adapter<TopRestaurantAdapter.ViewHolder> {

    public static class RestaurantStats {
        public String name;
        public int orderCount;
        public double revenue;

        public RestaurantStats(String name, int orderCount, double revenue) {
            this.name = name;
            this.orderCount = orderCount;
            this.revenue = revenue;
        }
    }

    private List<RestaurantStats> statsList = new ArrayList<>();

    public void setStats(List<RestaurantStats> stats) {
        this.statsList = stats != null ? stats : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_restaurant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(statsList.get(position), position + 1);
    }

    @Override
    public int getItemCount() {
        return statsList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvName, tvCount, tvRevenue;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_rank);
            tvName = itemView.findViewById(R.id.tv_restaurant_name);
            tvCount = itemView.findViewById(R.id.tv_order_count);
            tvRevenue = itemView.findViewById(R.id.tv_total_revenue);
        }

        void bind(RestaurantStats stats, int rank) {
            tvRank.setText(String.valueOf(rank));
            tvName.setText(stats.name);
            tvCount.setText("Tổng: " + stats.orderCount + " đơn hàng");
            
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            tvRevenue.setText(formatter.format(stats.revenue));
        }
    }
}
