# Module: Báo cáo - App Admin

## 1. Overview
Module báo cáo thống kê doanh thu và đơn hàng theo thời gian.

## 2. Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | Date Range Picker | Chọn: Hôm nay, Tuần, Tháng, Tùy chọn |
| 2 | Revenue Stats | Tổng doanh thu, số đơn, trung bình/đơn |
| 3 | Order Stats | Tổng đơn, hoàn thành, hủy, tỉ lệ hủy |
| 4 | Top Items | Top 5 món bán chạy |
| 5 | Top Restaurants | Top 5 nhà hàng |
| 6 | Chart (Phase 2) | Biểu đồ doanh thu theo ngày |

## 3. Data Flow

```
ViewModel.loadReport(dateFrom, dateTo)
        │
        ▼
Repository.getDailyStats(dateFrom, dateTo)
        │
        ▼
Supabase View: v_daily_stats
        │
        ▼
Repository.getTopItems(dateFrom, dateTo)
        │
        ▼
Supabase: order_items + orders join
        │
        ▼
Repository.getTopRestaurants(dateFrom, dateTo)
        │
        ▼
Supabase: orders + restaurants
        │
        ▼
LiveData<ReportData> ──▶ UI
```

## 4. API Endpoints

### 4.1 Daily Stats View
```sql
CREATE VIEW v_daily_stats AS
SELECT 
    DATE(created_at) as date,
    COUNT(*) as total_orders,
    SUM(total) as total_revenue,
    COUNT(CASE WHEN status = 'delivered' THEN 1 END) as completed_orders,
    COUNT(CASE WHEN status = 'cancelled' THEN 1 END) as cancelled_orders
FROM orders
GROUP BY DATE(created_at)
ORDER BY date DESC;
```

Query:
```
GET /rest/v1/v_daily_stats?date=gte.{dateFrom}&date=lte.{dateTo}
```

### 4.2 Top Items
```sql
SELECT 
    mi.id,
    mi.name,
    SUM(oi.quantity) as total_quantity,
    SUM(oi.subtotal) as total_revenue
FROM order_items oi
JOIN menu_items mi ON oi.menu_item_id = mi.id
JOIN orders o ON oi.order_id = o.id
WHERE o.created_at BETWEEN '{dateFrom}' AND '{dateTo}'
GROUP BY mi.id
ORDER BY total_quantity DESC
LIMIT 5;
```

### 4.3 Top Restaurants
```sql
SELECT 
    r.id,
    r.name,
    COUNT(o.id) as total_orders,
    SUM(o.total) as total_revenue
FROM restaurants r
JOIN orders o ON r.id = o.restaurant_id
WHERE o.created_at BETWEEN '{dateFrom}' AND '{dateTo}'
GROUP BY r.id
ORDER BY total_revenue DESC
LIMIT 5;
```

## 5. UI Components

### 5.1 Report Fragment (fragment_report.xml)
```xml
<LinearLayout android:orientation="vertical">
    <!-- Date Range Selector -->
    <Spinner android:id="@+id/spinnerDateRange"/>
    <LinearLayout android:id="@+id/customDateRange">
        <DatePicker android:id="@+id/dateFrom"/>
        <DatePicker android:id="@+id/dateTo"/>
    </LinearLayout>

    <!-- Revenue Summary -->
    <com.google.android.material.card.MaterialCardView>
        <LinearLayout>
            <TextView android:text="Tổng doanh thu"/>
            <TextView android:id="@+id/tvTotalRevenue"/>
            <TextView android:text="Tổng đơn hàng"/>
            <TextView android:id="@+id/tvTotalOrders"/>
            <TextView android:text="Trung bình/đơn"/>
            <TextView android:id="@+id/tvAverageOrder"/>
        </LinearLayout>
    </com.google.android.material.card.MaterialCardView>

    <!-- Order Stats -->
    <com.google.android.material.card.MaterialCardView>
        <LinearLayout>
            <TextView android:text="Hoàn thành"/>
            <TextView android:id="@+id/tvCompleted"/>
            <TextView android:text="Hủy"/>
            <TextView android:id="@+id/tvCancelled"/>
            <TextView android:text="Tỉ lệ hủy"/>
            <TextView android:id="@+id/tvCancelRate"/>
        </LinearLayout>
    </com.google.android.material.card.MaterialCardView>

    <!-- Top Items -->
    <TextView android:text="Món bán chạy"/>
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvTopItems"/>

    <!-- Top Restaurants -->
    <TextView android:text="Nhà hàng"/>
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvTopRestaurants"/>

    <!-- Chart (Phase 2) -->
    <com.github.mikephil.charting.charts.LineChart
        android:id="@+id/chartRevenue"/>
</LinearLayout>
```

## 6. ViewModel

```java
public class ReportViewModel extends ViewModel {
    private MutableLiveData<ReportData> reportData = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<String> selectedDateRange = new MutableLiveData<>("today");

    public void loadReport(String dateRange, String dateFrom, String dateTo) { }
}
```

## 7. Date Range Options

| Option | From | To |
|--------|------|-----|
| today | CURRENT_DATE | CURRENT_DATE |
| yesterday | CURRENT_DATE - 1 | CURRENT_DATE - 1 |
| this_week | DATE_TRUNC('week', NOW()) | NOW() |
| this_month | DATE_TRUNC('month', NOW()) | NOW() |
| custom | user selected | user selected |

## 8. Edge Cases

| Case | Handling |
|------|----------|
| No data in range | Show "Không có dữ liệu trong khoảng thời gian này" |
| Future date range | Disable, show "Không thể xem báo cáo tương lai" |
| Range too large | Limit to 1 year max |