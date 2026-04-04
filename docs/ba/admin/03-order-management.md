# Module: Quản lý đơn hàng - App Admin

## 1. Overview
Module quản lý đơn hàng cho phép admin/staff xem danh sách, chi tiết và cập nhật trạng thái đơn hàng.

## 2. Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | Order List | Danh sách đơn với tab lọc theo trạng thái |
| 2 | Search | Tìm theo mã đơn, tên khách, SĐT |
| 3 | Order Detail | Xem chi tiết đầy đủ |
| 4 | Update Status | Cập nhật trạng thái với validation |
| 5 | Cancel Order | Hủy đơn với lý do |
| 6 | Status Timeline | Lịch sử thay đổi trạng thái |
| 7 | Realtime | Nhận đơn mới tự động |

## 3. Order Status Machine

```
┌─────────────────────────────────────────────────────────────┐
│              ORDER STATUS TRANSITIONS                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   pending ──────────▶ confirmed ──────────▶ preparing    │
│   (chờ xác nhận)      (đã xác nhận)       (đang chuẩn bị) │
│        │                    │                    │          │
│        │                    │                    ▼          │
│        │                    │              delivering        │
│        │                    │             (đang giao)        │
│        │                    │                    │          │
│        ▼                    ▼                    ▼          │
│   ┌───────────────────────────────────────────┐            │
│   │              CANCELLED                    │            │
│   │       (hủy từ pending/confirmed)         │            │
│   └───────────────────────────────────────────┘            │
│                          │                                  │
│                          ▼                                  │
│                   ┌────────────┐                           │
│                   │ delivered  │                           │
│                   │ (hoàn thành)│                           │
│                   └────────────┘                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 4. Data Flow

### 4.1 Order List
```
Activity/Fragment
       │
       ▼
OrderViewModel.loadOrders(status)
       │
       ▼
OrderRepository.getOrders(status)
       │
       ▼
Supabase: orders?status=eq.{status}&order=created_at.desc
       │
       ▼
LiveData<List<Order>> ──▶ UI Update
```

### 4.2 Update Status
```
User clicks "Confirm"
       │
       ▼
Validate State Machine
(pending → confirmed? YES)
       │
       ▼
OrderRepository.updateStatus(orderId, "confirmed")
       │
       ▼
Supabase RPC: rpc_update_order_status
       │
       ▼
On Success:
  - Insert order_status_logs
  - Create notification
  - Refresh list
  - Show success message
On Error:
  - Show error from server
```

## 5. API Endpoints

### 5.1 Get Orders by Status
```
GET /rest/v1/orders?status=eq.pending&select=*,user:users(*),restaurant:restaurants(*)
```

### 5.2 Get Order Detail
```
GET /rest/v1/orders?id=eq.{orderId}&select=*,user:users(*),restaurant:restaurants(*),items:order_items(*)
```

### 5.3 Update Status (RPC)
```sql
SELECT rpc_update_order_status(
    p_order_id := 'uuid',
    p_new_status := 'confirmed',
    p_changed_by := 'uuid',
    p_note := null
);
```

### 5.4 Get Status Logs
```
GET /rest/v1/order_status_logs?order_id=eq.{orderId}&order=created_at.asc
```

## 6. UI Components

### 6.1 Order List (fragment_order_list.xml)
```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout>
    <com.google.android.material.appbar.AppBarLayout>
        <com.google.android.material.tabs.TabLayout>
            <Tab android:text="All"/>
            <Tab android:text="Chờ xác nhận"/>
            <Tab android:text="Đã xác nhận"/>
            <Tab android:text="Đang nấu"/>
            <Tab android:text="Đang giao"/>
            <Tab android:text="Hoàn thành"/>
            <Tab android:text="Đã hủy"/>
        </com.google.android.material.tabs.TabLayout>
    </com.google.android.material.appbar.AppBarLayout>

    <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
        android:id="@+id/swipeRefresh">
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rvOrders"/>
    </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### 6.2 Order Item Layout (item_order.xml)
```xml
<com.google.android.material.card.MaterialCardView>
    <LinearLayout>
        <TextView android:id="@+id/tvOrderCode"/>
        <TextView android:id="@+id/tvCustomerName"/>
        <TextView android:id="@+id/tvRestaurantName"/>
        <TextView android:id="@+id/tvTotal"/>
        <TextView android:id="@+id/tvStatus"/>
        <TextView android:id="@+id/tvCreatedAt"/>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

### 6.3 Order Detail (activity_order_detail.xml)
```xml
<ScrollView>
    <!-- Order Info Section -->
    <LinearLayout android:id="@+id/orderInfoSection">
        <TextView android:id="@+id/tvOrderCode"/>
        <TextView android:id="@+id/tvStatus"/>
        <TextView android:id="@+id/tvCreatedAt"/>
    </LinearLayout>

    <!-- Customer Info -->
    <LinearLayout android:id="@+id/customerSection">
        <TextView android:text="Thông tin khách hàng"/>
        <TextView android:id="@+id/tvCustomerName"/>
        <TextView android:id="@+id/tvCustomerPhone"/>
        <TextView android:id="@+id/tvDeliveryAddress"/>
    </LinearLayout>

    <!-- Items List -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvItems"/>

    <!-- Summary -->
    <LinearLayout android:id="@+id/summarySection">
        <TextView android:id="@+id/tvSubtotal"/>
        <TextView android:id="@+id/tvDeliveryFee"/>
        <TextView android:id="@+id/tvDiscount"/>
        <TextView android:id="@+id/tvTotal"/>
    </LinearLayout>

    <!-- Note -->
    <TextView android:id="@+id/tvNote"/>

    <!-- Action Buttons -->
    <LinearLayout android:id="@+id/actionButtons">
        <Button android:id="@+id/btnConfirm"/>
        <Button android:id="@+id/btnCancel"/>
    </LinearLayout>

    <!-- Status Timeline -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvStatusLogs"/>
</ScrollView>
```

## 7. ViewModel

```java
public class OrderViewModel extends ViewModel {
    private MutableLiveData<List<Order>> orders = new MutableLiveData<>();
    private MutableLiveData<Order> selectedOrder = new MutableLiveData<>();
    private MutableLiveData<List<OrderStatusLog>> statusLogs = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<UpdateStatusResult> updateResult = new MutableLiveData<>();

    public void loadOrders(String status) { }
    public void loadOrderDetail(String orderId) { }
    public void updateStatus(String orderId, String newStatus, String note) { }
    public void cancelOrder(String orderId, String reason) { }
}
```

## 8. Validation Rules

| Current Status | Allowed Next Status |
|----------------|---------------------|
| pending | confirmed, cancelled |
| confirmed | preparing, cancelled |
| preparing | delivering, cancelled |
| delivering | delivered |
| delivered | (none - final) |
| cancelled | (none - final) |

## 9. Edge Cases

| Case | Handling |
|------|----------|
| Invalid transition | Show error: "Không thể chuyển sang trạng thái này" |
| Network error | Show error, giữ nguyên trạng thái |
| Order already cancelled | Disable all buttons, show "Đơn đã hủy" |
| Empty list | Show "Không có đơn hàng" |
| Concurrent update | Show "Đơn hàng đang được xử lý" |
| Cancel with active orders | Allow, no restriction |

## 10. Realtime Subscription

```java
// Subscribe to orders table
supabase.channel("orders")
    .on("postgres_changes", 
        filter: "table=orders", 
        filter: "status=eq.pending"
    )
    .subscribe((event) {
        // Add new order to list
        // Play notification sound
        // Update badge count
    });
```

## 11. Error Messages

| Error | Message |
|-------|---------|
| Network error | "Không thể kết nối máy chủ" |
| Invalid transition | "Không thể chuyển sang trạng thái này" |
| Server error | "Lỗi máy chủ: {message}" |
| Token expired | "Phiên làm việc hết hạn, vui lòng đăng nhập lại" |