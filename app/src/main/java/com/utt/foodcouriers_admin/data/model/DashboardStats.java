package com.utt.foodcouriers_admin.data.model;

import com.google.gson.annotations.SerializedName;

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

    // --- New Fields for Professional Dashboard ---
    private double orderTrend;    // Ví dụ: +12.5 (là 12.5%)
    private double revenueTrend;  // Ví dụ: -5.0 (là giảm 5%)
    private double pendingTrend;
    private double completedTrend;

    // Getters and Setters
    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }

    public int getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(int pendingOrders) { this.pendingOrders = pendingOrders; }

    public int getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(int completedOrders) { this.completedOrders = completedOrders; }

    public int getCancelledOrders() { return cancelledOrders; }
    public void setCancelledOrders(int cancelledOrders) { this.cancelledOrders = cancelledOrders; }

    public double getOrderTrend() { return orderTrend; }
    public void setOrderTrend(double orderTrend) { this.orderTrend = orderTrend; }

    public double getRevenueTrend() { return revenueTrend; }
    public void setRevenueTrend(double revenueTrend) { this.revenueTrend = revenueTrend; }

    public double getPendingTrend() { return pendingTrend; }
    public void setPendingTrend(double pendingTrend) { this.pendingTrend = pendingTrend; }

    public double getCompletedTrend() { return completedTrend; }
    public void setCompletedTrend(double completedTrend) { this.completedTrend = completedTrend; }
}
