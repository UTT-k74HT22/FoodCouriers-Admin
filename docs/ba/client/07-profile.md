# Module: Hồ sơ cá nhân - App Client

## 1. Overview
Quản lý thông tin cá nhân và địa chỉ giao hàng.

## 2. Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | View Profile | Xem thông tin tài khoản |
| 2 | Edit Profile | Cập nhật tên, phone, avatar |
| 3 | Address List | Danh sách địa chỉ |
| 4 | Add Address | Thêm địa chỉ mới |
| 5 | Edit Address | Sửa địa chỉ |
| 6 | Delete Address | Xóa địa chỉ |
| 7 | Set Default | Đặt địa chỉ mặc định |
| 8 | Logout | Đăng xuất |

## 3. User Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    PROFILE FLOW                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              PROFILE FRAGMENT                        │   │
│  │  ┌──────┐                                           │   │
│  │  │ Avatar│  Nguyễn Văn A                           │   │
│  │  │  ✏️  │  a@example.com                         │   │
│  │  │      │  0901234567                             │   │
│  │  └──────┘                                           │   │
│  │                                                      │   │
│  │  📍 Địa chỉ giao hàng                    [+]       │   │
│  │  ┌─────────────────────────────────────────────┐   │   │
│  │  │ 🏠 Nhà (Mặc định) - 123 Nguyễn Trãi       │   │   │
│  │  │    [Sửa] [Xóa]                              │   │   │
│  │  ├─────────────────────────────────────────────┤   │   │
│  │  │ 💼 Công ty - 456 Lê Lợi                     │   │   │
│  │  │    [Sửa] [Xóa] [Đặt mặc định]             │   │   │
│  │  └─────────────────────────────────────────────┘   │   │
│  │                                                      │   │
│  │  📋 Lịch sử đơn hàng                               │   │
│  │  ❓ Trợ giúp & FAQ                                 │   │
│  │  ❎ Đăng xuất                                      │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 4. API Endpoints

### 4.1 Get Profile
```
GET /rest/v1/users?id=eq.{userId}
```

### 4.2 Update Profile
```
PATCH /rest/v1/users?id=eq.{userId}
{
    "full_name": "...",
    "phone": "..."
}
```

### 4.3 Get Addresses
```
GET /rest/v1/user_addresses?user_id=eq.{userId}
```

### 4.4 Add Address
```
POST /rest/v1/user_addresses
{
    "user_id": "uuid",
    "label": "Nhà",
    "full_address": "123 Main St",
    "district": "Q1",
    "is_default": false
}
```

### 4.5 Set Default Address
```
PATCH /rest/v1/user_addresses?id=eq.{addressId}
{
    "is_default": true
}
```

### 4.6 Delete Address
```
DELETE /rest/v1/user_addresses?id=eq.{addressId}
```

## 5. UI Components

### 5.1 Profile (fragment_profile.xml)
```xml
<LinearLayout android:orientation="vertical">
    <!-- Avatar Section -->
    <ImageView android:id="@+id/ivAvatar"/>
    <TextView android:id="@+id/tvName"/>
    <TextView android:id="@+id/tvEmail"/>
    <TextView android:id="@+id/tvPhone"/>
    <Button android:id="@+id/btnEditProfile"/>

    <!-- Addresses Section -->
    <TextView android:text="Địa chỉ giao hàng"/>
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvAddresses"/>
    <Button android:id="@+id/btnAddAddress"/>

    <!-- Menu Items -->
    <Button android:id="@+id/btnOrderHistory"/>
    <Button android:id="@+id/btnHelp"/>
    <Button android:id="@+id/btnLogout"/>
</LinearLayout>
```

### 5.2 Address Item (item_address.xml)
```xml
<com.google.android.material.card.MaterialCardView>
    <LinearLayout>
        <ImageView android:id="@+id/ivIcon"/>
        <LinearLayout android:layout_weight="1">
            <TextView android:id="@+id/tvLabel"/>
            <TextView android:id="@+id/tvAddress"/>
            <TextView android:id="@+id/tvDefault" android:visibility="gone"/>
        </LinearLayout>
        <ImageButton android:id="@+id/btnEdit"/>
        <ImageButton android:id="@+id/btnDelete"/>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

### 5.3 Address Form (activity_address_form.xml)
```xml
<LinearLayout>
    <Spinner android:id="@+id/spinnerLabel"/>
    
    <com.google.android.material.textfield.TextInputLayout>
        <EditText android:id="@+id/etAddress"/>
    </com.google.android.material.textfield.TextInputLayout>
    
    <com.google.android.material.textfield.TextInputLayout>
        <EditText android:id="@+id/etDistrict"/>
    </com.google.android.material.textfield.TextInputLayout>
    
    <Switch android:id="@+id/switchDefault"/>
    
    <Button android:id="@+id/btnSave"/>
</LinearLayout>
```

## 6. ViewModel

```java
public class ProfileViewModel extends ViewModel {
    private MutableLiveData<User> user = new MutableLiveData<>();
    private MutableLiveData<List<Address>> addresses = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public void loadProfile() { }
    public void updateProfile(String name, String phone) { }
    public void loadAddresses() { }
    public void addAddress(Address address) { }
    public void updateAddress(Address address) { }
    public void deleteAddress(String addressId) { }
    public void setDefaultAddress(String addressId) { }
    public void logout() { }
}
```

## 7. Edge Cases

| Case | Handling |
|------|----------|
| No addresses | Show "Chưa có địa chỉ" + Add button |
| Delete last address | Allow, show warning |
| Logout with pending order | Confirm dialog |