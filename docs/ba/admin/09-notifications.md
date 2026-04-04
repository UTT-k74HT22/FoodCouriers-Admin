# Module: Thông báo - App Admin

## 1. Overview
Module quản lý thông báo trong app cho Admin/Staff.

## 2. Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | Notification List | Danh sách tất cả thông báo |
| 2 | Filter by Type | Lọc: All, Orders, System |
| 3 | Mark as Read | Đánh dấu đã đọc |
| 4 | Mark All as Read | Đánh dấu tất cả đã đọc |
| 5 | Badge Count | Hiển thị số thông báo chưa đọc trên menu |
| 6 | Realtime | Nhận thông báo mới tự động |

## 3. Data Flow

```
LIST:
ViewModel.loadNotifications(type)
        │
        ▼
Repository.getNotifications(type)
        │
        ▼
Supabase: GET /rest/v1/notifications?order=created_at.desc
        │
        ▼
LiveData<List<Notification>> ──▶ UI

MARK READ:
ViewModel.markAsRead(notificationId)
        │
        ▼
Repository.markAsRead() ──▶ Supabase
PATCH /rest/v1/notifications?id=eq.{id}
        │
        ▼
Update list, update badge count
```

## 4. API Endpoints

### 4.1 Get Notifications
```
GET /rest/v1/notifications?select=*&order=created_at.desc
```

With type filter:
```
GET /rest/v1/notifications?type=eq.order&order=created_at.desc
```

Get unread count:
```
GET /rest/v1/notifications?is_read=eq.false&select=id
```

### 4.2 Mark as Read
```
PATCH /rest/v1/notifications?id=eq.{id}
{
    "is_read": true
}
```

### 4.3 Mark All as Read
```
PATCH /rest/v1/notifications?is_read=eq.false
{
    "is_read": true
}
```

## 5. UI Components

### 5.1 Notification List (fragment_notifications.xml)
```xml
<LinearLayout android:orientation="vertical">
    <LinearLayout>
        <Spinner android:id="@+id/spinnerFilter"/>
        <Button android:id="@+id/btnMarkAllRead"/>
    </LinearLayout>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvNotifications"/>
</LinearLayout>
```

### 5.2 Notification Item (item_notification.xml)
```xml
<com.google.android.material.card.MaterialCardView>
    <LinearLayout>
        <ImageView android:id="@+id/ivIcon"/>
        <LinearLayout>
            <TextView android:id="@+id/tvTitle"/>
            <TextView android:id="@+id/tvBody"/>
            <TextView android:id="@+id/tvTime"/>
        </LinearLayout>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

## 6. Notification Types

| Type | Icon | Description |
|------|------|-------------|
| order | 🔔 | New order, status update |
| system | 📢 | System announcements |
| promotion | 🎁 | New promotions |

## 7. Realtime Subscription

```java
supabase.channel("notifications")
    .on("postgres_changes",
        filter: "table=notifications"
    )
    .subscribe((event) {
        // Add to list
        // Update badge count
        // Play sound
    });
```

## 8. Edge Cases

| Case | Handling |
|------|----------|
| Empty list | Show "Không có thông báo" |
| Network error | Show cached notifications |