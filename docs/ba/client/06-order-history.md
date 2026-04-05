# Module: Lịch sử đơn hàng - App Client

## 1. Overview
Xem lịch sử đơn hàng, theo dõi trạng thái, hủy đơn, đánh giá.

## 2. Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | Order List | Danh sách đơn hàng theo tab |
| 2 | Order Detail | Xem chi tiết đơn |
| 3 | Status Timeline | Theo dõi trạng thái |
| 4 | Cancel Order | Hủy đơn (nếu chưa xác nhận) |
| 5 | Reorder | Đặt lại đơn cũ |
| 6 | Reviews | Đánh giá sau khi nhận hàng |
| 7 | Realtime Updates | Cập nhật trạng thái tự động |

## 3. Order Status Timeline

```
┌─────────────────────────────────────────────────────────────┐
│                 STATUS TIMELINE                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  pending ──▶ confirmed ──▶ preparing ──▶ delivering ──▶ delivered
│  (chờ)       (xác nhận)      (nấu)         (giao)          │
│     │                                                    │
│     └──────────────────▶ cancelled                        │
│                        (hủy)                               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 4. User Flow

```
┌─────────────────────────────────────────────────────────────┐
│                  ORDER HISTORY FLOW                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              ORDER LIST (TabLayout)                  │   │
│  │  [Tất cả] [Đang xử lý] [Hoàn thành] [Đã hủy]       │   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  ┌─────────────────────────────────────────────┐   │   │
│  │  │ 🟢 ORD001 - Restaurant A - 150k - Hoàn thành│   │   │
│  │  │    15/01/2024 10:30                         │   │   │
│  │  ├─────────────────────────────────────────────┤   │   │
│  │  │ 🟡 ORD002 - Restaurant B - 200k - Đang giao │   │   │
│  │  │    15/01/2024 11:00                         │   │   │
│  │  ├─────────────────────────────────────────────┤   │   │
│  │  │ 🔴 ORD003 - Restaurant C - 80k - Đã hủy     │   │   │
│  │  │    14/01/2024 18:00                         │   │   │
│  │  └─────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                  │
│                           ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              ORDER DETAIL                           │   │
│  │  Mã đơn: ORD001                                     │   │
│  │  Trạng thái: ✅ Hoàn thành                          │   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  Nhà hàng: Restaurant A                             │   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  Món đã đặt:                                         │   │
│  │  - Phở Bò (x2) - 100k                              │   │
│  │  - Trà đá (x2) - 20k                               │   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  Tổng: 120k + 15k (ship) - 15k (giảm) = 120k      │   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  Giao tại: 123 Nguyễn Trãi, Q1                     │   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  [Đánh giá] [Đặt lại]                               │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           RATING DIALOG                              │   │
│  │  Chọn số sao: ☆ ☆ ☆ ☆ ☆                            │   │
│  │  Đánh giá: [________________________]              │   │
│  │  [Hủy]                        [Gửi]                │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 5. API Endpoints

### 5.1 Get My Orders
```
GET /rest/v1/orders?user_id=eq.{userId}&order=created_at.desc
```

### 5.2 Get Order Detail
```
GET /rest/v1/orders?id=eq.{orderId}&select=*,restaurant:restaurants(*),items:order_items(*)
```

### 5.3 Cancel Order (RPC)
```sql
SELECT rpc_update_order_status(
    p_order_id := 'uuid',
    p_new_status := 'cancelled',
    p_changed_by := 'uuid',
    p_note := 'Customer cancelled'
);
```

### 5.4 Create Review
```
POST /rest/v1/reviews
{
    "order_id": "uuid",
    "rating": 5,
    "comment": "Ngon lắm!"
}
```

## 6. UI Components

### 6.1 Order List (fragment_order_history.xml)
```xml
<LinearLayout android:orientation="vertical">
    <com.google.android.material.tabs.TabLayout
        android:id="@+id/tabLayout">
        <Tab android:text="Tất cả"/>
        <Tab android:text="Đang xử lý"/>
        <Tab android:text="Hoàn thành"/>
        <Tab android:text="Đã hủy"/>
    </com.google.android.material.tabs.TabLayout>

    <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
        android:id="@+id/swipeRefresh">
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rvOrders"/>
    </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
</LinearLayout>
```

### 6.2 Order Detail (activity_order_detail.xml)
```xml
<ScrollView>
    <!-- Status -->
    <TextView android:id="@+id/tvStatus"/>
    <TextView android:id="@+id/tvOrderCode"/>

    <!-- Restaurant Info -->
    <TextView android:id="@+id/tvRestaurantName"/>

    <!-- Items -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvItems"/>

    <!-- Summary -->
    <TextView android:id="@+id/tvSubtotal"/>
    <TextView android:id="@+id/tvDeliveryFee"/>
    <TextView android:id="@+id/tvDiscount"/>
    <TextView android:id="@+id/tvTotal"/>

    <!-- Address -->
    <TextView android:id="@+id/tvAddress"/>

    <!-- Actions -->
    <Button android:id="@+id/btnReview"/>
    <Button android:id="@+id/btnReorder"/>
    <Button android:id="@+id/btnCancel"/>
</ScrollView>
```

## 7. ViewModel

```java
public class OrderHistoryViewModel extends ViewModel {
    private MutableLiveData<List<Order>> orders = new MutableLiveData<>();
    private MutableLiveData<Order> currentOrder = new MutableLiveData<>();
    private MutableLiveData<List<OrderStatusLog>> statusLogs = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();

    public void loadOrders(String tab) { }
    public void loadOrderDetail(String orderId) { }
    public void cancelOrder(String orderId, String reason) { }
    public void submitReview(String orderId, int rating, String comment) { }
}
```

## 8. Realtime Subscription

```java
supabase.channel("orders")
    .on("postgres_changes",
        filter: "table=orders",
        filter: "user_id=eq." + currentUserId
    )
    .subscribe((event) {
        // Update order status in list
        // Show notification
    });
```

## 9. Edge Cases

| Case | Handling |
|------|----------|
| No orders | Show "Bạn chưa có đơn hàng nào" |
| Already reviewed | Hide "Đánh giá" button |
| Cannot cancel | Show "Không thể hủy đơn này" |
| Order cancelled by restaurant | Show reason |