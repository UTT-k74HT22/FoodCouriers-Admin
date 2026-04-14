package com.utt.foodcouriers_admin.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model chứa dữ liệu thống kê tổng quan cho Dashboard.
 * Tên các trường @SerializedName phải khớp với tên cột trong View v_daily_stats của Supabase.
 */
public class DashboardStats {
    @SerializedName("total_orders")
    private int totalOrders;

    @SerializedName("total_revenue")
    private double totalRevenue;

    @SerializedName("pending_orders")
    private int pendingOrders;

    @SerializedName("completed_orders")
    private int completedOrders;

    @SerializedName("cancelled_orders")
    private int cancelledOrders;

    // Getters
    public int getTotalOrders() { return totalOrders; }
    public double getTotalRevenue() { return totalRevenue; }
    public int getPendingOrders() { return pendingOrders; }
    public int getCompletedOrders() { return completedOrders; }
    public int getCancelledOrders() { return cancelledOrders; }

    // Setters (nếu cần)
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }
    public void setPendingOrders(int pendingOrders) { this.pendingOrders = pendingOrders; }
    public void setCompletedOrders(int completedOrders) { this.completedOrders = completedOrders; }
    public void setCancelledOrders(int cancelledOrders) { this.cancelledOrders = cancelledOrders; }
}
