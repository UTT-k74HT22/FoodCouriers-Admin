# Module: Quản lý nhà hàng - App Admin

## 1. Overview
Module quản lý nhà hàng: CRUD thông tin nhà hàng, upload ảnh, bật/tắt trạng thái.

## 2. Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | Restaurant List | Danh sách tất cả nhà hàng |
| 2 | Add Restaurant | Tạo nhà hàng mới |
| 3 | Edit Restaurant | Sửa thông tin nhà hàng |
| 4 | Delete Restaurant | Xóa (soft delete) |
| 5 | Toggle Active | Bật/tắt is_active |
| 6 | Toggle Open | Bật/tắt is_open (giờ mở cửa) |
| 7 | Upload Image | Upload ảnh lên Supabase Storage |

## 3. Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    RESTAURANT CRUD FLOW                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  LIST:                                                     │
│  ViewModel.loadRestaurants()                               │
│        │                                                   │
│        ▼                                                   │
│  Repository.getRestaurants() ──▶ Supabase                 │
│  GET /rest/v1/restaurants?order=created_at.desc            │
│        │                                                   │
│        ▼                                                   │
│  LiveData<List<Restaurant>> ──▶ UI                       │
│                                                             │
│  ─────────────────────────────────────────────────────     │
│                                                             │
│  CREATE/UPDATE:                                            │
│  ViewModel.saveRestaurant(data)                           │
│        │                                                   │
│        ▼                                                   │
│  Repository.saveRestaurant() ──▶ Supabase                 │
│  POST /rest/v1/restaurants (create)                        │
│  PATCH /rest/v1/restaurants?id=eq.{id} (update)            │
│        │                                                   │
│        ▼                                                   │
│  On Success: Return to list, show success                 │
│  On Error: Show error message                             │
│                                                             │
│  ─────────────────────────────────────────────────────     │
│                                                             │
│  UPLOAD IMAGE:                                            │
│  ViewModel.uploadImage(file)                              │
│        │                                                   │
│        ▼                                                   │
│  ImageUtils.compress()                                     │
│        │                                                   │
│        ▼                                                   │
│  Repository.uploadImage() ──▶ Supabase Storage            │
│  POST /storage/v1/object/restaurants/{filename}           │
│        │                                                   │
│        ▼                                                   │
│  Return: image_url                                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 4. API Endpoints

### 4.1 Get Restaurants
```
GET /rest/v1/restaurants?select=*&order=created_at.desc
```

### 4.2 Create Restaurant
```
POST /rest/v1/restaurants
{
    "name": "Restaurant A",
    "address": "123 Main St",
    "phone": "0901234567",
    "image_url": "https://...",
    "is_active": true,
    "is_open": true,
    "open_time": "08:00:00",
    "close_time": "22:00:00",
    "delivery_fee": 15000,
    "min_order": 50000
}
```

### 4.3 Update Restaurant
```
PATCH /rest/v1/restaurants?id=eq.{id}
{
    "name": "...",
    "is_active": false
}
```

### 4.4 Delete Restaurant
```
PATCH /rest/v1/restaurants?id=eq.{id}
{
    "is_active": false
}
```

## 5. UI Components

### 5.1 Restaurant List (fragment_restaurant_list.xml)
```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout>
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvRestaurants"/>

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabAdd"/>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### 5.2 Restaurant Item (item_restaurant.xml)
```xml
<com.google.android.material.card.MaterialCardView>
    <LinearLayout android:orientation="horizontal">
        <ImageView android:id="@+id/ivRestaurant"/>
        <LinearLayout>
            <TextView android:id="@+id/tvName"/>
            <TextView android:id="@+id/tvAddress"/>
            <LinearLayout>
                <TextView android:id="@+id/tvStatus"/>
                <Switch android:id="@+id/switchActive"/>
            </LinearLayout>
        </LinearLayout>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

### 5.3 Restaurant Form (activity_restaurant_form.xml)
```xml
<ScrollView>
    <LinearLayout>
        <!-- Image Picker -->
        <ImageView android:id="@+id/ivImage"/>
        <Button android:id="@+id/btnSelectImage" android:text="Chọn ảnh"/>

        <!-- Form Fields -->
        <com.google.android.material.textfield.TextInputLayout>
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etName"/>
        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout>
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etAddress"/>
        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout>
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etPhone"/>
        </com.google.android.material.textfield.TextInputLayout>

        <!-- Time Picker -->
        <TextView android:text="Giờ mở cửa"/>
        <TimePicker android:id="@+id/timePickerOpen"/>

        <TextView android:text="Giờ đóng cửa"/>
        <TimePicker android:id="@+id/timePickerClose"/>

        <!-- Fee Settings -->
        <com.google.android.material.textfield.TextInputLayout>
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etDeliveryFee"/>
        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout>
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etMinOrder"/>
        </com.google.android.material.textfield.TextInputLayout>

        <!-- Switches -->
        <Switch android:id="@+id/switchActive"/>
        <Switch android:id="@+id/switchOpen"/>

        <!-- Buttons -->
        <Button android:id="@+id/btnSave"/>
    </LinearLayout>
</ScrollView>
```

## 6. ViewModel

```java
public class RestaurantViewModel extends ViewModel {
    private MutableLiveData<List<Restaurant>> restaurants = new MutableLiveData<>();
    private MutableLiveData<Restaurant> currentRestaurant = new MutableLiveData<>();
    private MutableLiveData<String> imageUrl = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public void loadRestaurants() { }
    public void loadRestaurant(String id) { }
    public void saveRestaurant(Restaurant restaurant) { }
    public void deleteRestaurant(String id) { }
    public void toggleActive(String id, boolean isActive) { }
    public void uploadImage(File file) { }
}
```

## 7. Image Upload Flow

```
User taps "Chọn ảnh"
       │
       ▼
System image picker opens
       │
       ▼
User selects image
       │
       ▼
ImageUtils.compress(image, maxSize: 1MB)
       │
       ▼
Repository.uploadImage(file, bucket: "restaurants")
       │
       ▼
Supabase Storage: /restaurants/{uuid}.jpg
       │
       ▼
Return: public_url
       │
       ▼
Set to ivImage, store in form data
```

## 8. Validation Rules

| Field | Rule |
|-------|------|
| name | Required, min 2 chars, max 100 chars |
| address | Required, min 5 chars |
| phone | Optional, valid phone format |
| delivery_fee | Optional, >= 0 |
| min_order | Optional, >= 0 |
| open_time | Required |
| close_time | Required, must be after open_time |

## 9. Edge Cases

| Case | Handling |
|------|----------|
| Name already exists | Show error "Tên nhà hàng đã tồn tại" |
| Image upload fail | Show error, allow manual URL input |
| Delete with active orders | Show warning, allow delete |
| Network error | Show error with retry option |
| Form validation fail | Show inline errors |

## 10. Staff Role Restriction

- Staff chỉ thấy restaurants được gán trong `restaurant_staff`
- RLS policy tự động lọc dữ liệu