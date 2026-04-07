package com.utt.foodcouriers_admin.data.model;

import com.google.gson.annotations.SerializedName;

             /**
      * Model chứa dữ liệu báo cáo hàng ngày trả về từ Supabase View (v_daily_stats)
              *
              **/
             public class DailyStat { @SerializedName("date")
        private String date; // Ngày (YYYY-MM-DD)

                @SerializedName("total_revenue")
        private double revenue; // Doanh thu

                @SerializedName("total_orders")
        private int totalOrders; // Tổng số đơn

                @SerializedName("completed_orders")
        private int completedOrders; // Đơn hoàn thành

                @SerializedName("cancelled_orders")
        private int cancelledOrders; // Đơn bị hủy

                // --- Các hàm lấy dữ liệu (Getters) ---
                public String getDate() { return date; }
        public double getRevenue() { return revenue; }
        public int getTotalOrders() { return totalOrders; }
        public int getCompletedOrders() { return completedOrders; }
        public int getCancelledOrders() { return cancelledOrders; }

                // Hàm bổ trợ: Tính tỷ lệ hủy đơn (%)
                public double getCancelRate() {
                 if (totalOrders == 0) return 0;
                 return (double) cancelledOrders / totalOrders * 100;
               }
             }

