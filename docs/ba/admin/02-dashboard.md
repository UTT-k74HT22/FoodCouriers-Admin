# Module: Dashboard - App Admin

## 1. Overview
Module Dashboard hiển thị tổng quan số liệu thống kê và đơn hàng mới cần xử lý.

## 2. Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | Stats Cards | 3 thẻ: Tổng đơn hôm nay, Doanh thu hôm nay, Đơn đang xử lý |
| 2 | New Orders | Danh sách đơn hàng mới (trạng thái pending) |
| 3 | Top Items | Top 5 món ăn bán chạy trong ngày |
| 4 | Quick Actions | Buttons điều hướng nhanh |
| 5 | Realtime | Tự động cập nhật khi có đơn mới |

## 3. Data Flow

```
┌──────────────────────────────────────────────────────────────┐
│                     DATA FLOW                                 │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  ViewModel.loadDashboard()                                    │
│           │                                                   │
│           ▼                                                   │
│  Repository.getDailyStats() ──▶ Supabase View               │
│           │                      v_daily_stats                │
│           │                                                   │
│  Repository.getNewOrders() ──▶ Supabase                     │
│           │                      orders (status='pending')   │
│           │                                                   │
│  Repository.getTopItems() ──▶ Supabase RPC                  │
│           │                      get_top_items()             │
│           │                                                   │
│           ▼                                                   │
│  LiveData<DashboardData> ──▶ UI update                       │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

## 4. API Calls

### 4.1 Daily Stats
```sql
-- View: v_daily_stats
SELECT 
    COUNT(*) as total_orders,
    SUM(total) as total_revenue,
    COUNT(CASE WHEN status = 'pending' THEN 1 END) as pending_orders
FROM orders
WHERE DATE(created_at) = CURRENT_DATE;
```

### 4.2 New Orders
```sql
SELECT o.*, u.full_name, u.phone, r.name as restaurant_name
FROM orders o
JOIN users u ON o.user_id = u.id
JOIN restaurants r ON o.restaurant_id = r.id
WHERE o.status = 'pending'
ORDER BY o.created_at DESC
LIMIT 10;
```

### 4.3 Top Items
```sql
SELECT mi.name, SUM(oi.quantity) as total_quantity, SUM(oi.subtotal) as total_revenue
FROM order_items oi
JOIN menu_items mi ON oi.menu_item_id = mi.id
JOIN orders o ON oi.order_id = o.id
WHERE DATE(o.created_at) = CURRENT_DATE
GROUP BY mi.id
ORDER BY total_quantity DESC
LIMIT 5;
```

## 5. UI Components

### Layout: fragment_dashboard.xml
```xml
<LinearLayout>
    <!-- Stats Cards Row -->
    <LinearLayout android:orientation="horizontal">
        <CardView android:id="@+id/cardOrders"
            android:layout_weight="1">
            <TextView android:id="@+id/tvOrderCount"/>
        </CardView>
        <CardView android:id="@+id/cardRevenue"
            android:layout_weight="1">
            <TextView android:id="@+id/tvRevenue"/>
        </CardView>
        <CardView android:id="@+id/cardProcessing"
            android:layout_weight="1">
            <TextView android:id="@+id/tvProcessing"/>
        </CardView>
    </LinearLayout>

    <!-- New Orders Section -->
    <TextView android:text="Đơn hàng mới"/>
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvNewOrders"/>

    <!-- Top Items Section -->
    <TextView android:text="Món bán chạy"/>
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvTopItems"/>
</LinearLayout>
```

## 6. ViewModel

```java
public class DashboardViewModel extends ViewModel {
    private MutableLiveData<DashboardData> dashboardData = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();
    private boolean isLoading = false;

    public void loadDashboard() {
        // Load all data concurrently
    }
}
```

## 7. Edge Cases

| Case | Handling |
|------|----------|
| No orders today | Display "0" for all stats |
| Network error | Show cached data + error banner |
| Empty new orders | Show "Không có đơn hàng mới" |
| Stats API fail | Show "Không thể tải số liệu" |

## 8. Real-time Updates

- Subscribe to `orders` table
- Filter: `status = 'pending'`
- Update UI when new order arrives
- Play notification sound
- Update badge count in NavigationDrawer