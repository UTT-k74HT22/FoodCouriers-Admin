# Module: Quản lý người dùng - App Admin

## 1. Overview
Module quản lý tài khoản khách hàng: xem, tìm kiếm, xem lịch sử đơn, vô hiệu hóa.

## 2. Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | User List | Danh sách tất cả khách hàng |
| 2 | Search | Tìm theo tên, email, số điện thoại |
| 3 | User Detail | Xem thông tin chi tiết |
| 4 | Order History | Xem lịch sử đơn hàng của user |
| 5 | Deactivate | Vô hiệu hóa/kích hoạt tài khoản |

## 3. Data Flow

```
LIST:
ViewModel.loadUsers(searchQuery)
        │
        ▼
Repository.getUsers(searchQuery) ──▶ Supabase
GET /rest/v1/users?role=eq.customer&or=(name.ilike.*,email.ilike.*,phone.ilike.*)
        │
        ▼
LiveData<List<User>> ──▶ UI

DETAIL:
ViewModel.loadUserDetail(userId)
        │
        ▼
Repository.getUser(userId) ──▶ Supabase
Repository.getUserOrders(userId) ──▶ Supabase
        │
        ▼
LiveData<UserDetail> ──▶ UI

TOGGLE ACTIVE:
ViewModel.toggleUserActive(userId, isActive)
        │
        ▼
Repository.updateUser() ──▶ Supabase
PATCH /rest/v1/users?id=eq.{id}
        │
        ▼
Refresh list
```

## 4. API Endpoints

### 4.1 Get Users
```
GET /rest/v1/users?role=eq.customer&select=*&order=created_at.desc
```

With search:
```
GET /rest/v1/users?role=eq.customer&or=(full_name.ilike.*%s*,email.ilike.*%s*,phone.ilike.*%s*)&select=*
```

### 4.2 Get User Detail
```
GET /rest/v1/users?id=eq.{id}
```

### 4.3 Get User Orders
```
GET /rest/v1/orders?user_id=eq.{id}&select=*&order=created_at.desc
```

### 4.4 Update User Status
```
PATCH /rest/v1/users?id=eq.{id}
{
    "is_active": false
}
```

## 5. UI Components

### 5.1 User List (activity_user_list.xml)
```xml
<LinearLayout android:orientation="vertical">
    <androidx.appcompat.widget.SearchView
        android:id="@+id/searchView"/>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvUsers"/>
</LinearLayout>
```

### 5.2 User Item (item_user.xml)
```xml
<com.google.android.material.card.MaterialCardView>
    <LinearLayout android:orientation="horizontal">
        <ImageView android:id="@+id/ivAvatar"/>
        <LinearLayout>
            <TextView android:id="@+id/tvName"/>
            <TextView android:id="@+id/tvEmail"/>
            <TextView android:id="@+id/tvPhone"/>
            <TextView android:id="@+id/tvStatus"/>
            <Switch android:id="@+id/switchActive"/>
        </LinearLayout>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

### 5.3 User Detail (activity_user_detail.xml)
```xml
<ScrollView>
    <LinearLayout>
        <ImageView android:id="@+id/ivAvatar"/>
        <TextView android:id="@+id/tvName"/>
        <TextView android:id="@+id/tvEmail"/>
        <TextView android:id="@+id/tvPhone"/>
        <TextView android:id="@+id/tvStatus"/>
        <TextView android:id="@+id/tvCreatedAt"/>

        <TextView android:text="Lịch sử đơn hàng"/>
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rvOrders"/>

        <Button android:id="@+id/btnDeactivate"/>
    </LinearLayout>
</ScrollView>
```

## 6. ViewModel

```java
public class UserViewModel extends ViewModel {
    private MutableLiveData<List<User>> users = new MutableLiveData<>();
    private MutableLiveData<User> currentUser = new MutableLiveData<>();
    private MutableLiveData<List<Order>> userOrders = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public void loadUsers(String searchQuery) { }
    public void loadUserDetail(String userId) { }
    public void toggleUserActive(String userId, boolean isActive) { }
}
```

## 7. Edge Cases

| Case | Handling |
|------|----------|
| No search results | Show "Không tìm thấy người dùng" |
| User has active orders | Allow deactivate with warning |
| Network error | Show error with retry |