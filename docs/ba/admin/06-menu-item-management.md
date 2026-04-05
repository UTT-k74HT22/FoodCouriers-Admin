# Module: Quản lý món ăn - App Admin

## 1. Overview
Module quản lý món ăn (Menu Item): CRUD món ăn trong thực đơn nhà hàng.

## 2. Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | Menu List | Danh sách món theo nhà hàng, lọc theo danh mục |
| 2 | Add Menu Item | Thêm món mới |
| 3 | Edit Menu Item | Sửa thông tin món |
| 4 | Delete Menu Item | Xóa món |
| 5 | Toggle Available | Bật/tắt is_available (hết món) |
| 6 | Toggle Featured | Đánh dấu món nổi bật |
| 7 | Upload Image | Upload ảnh món ăn |

## 3. Data Flow

```
LIST:
ViewModel.loadMenuItems(restaurantId, categoryId)
        │
        ▼
Repository.getMenuItems(restaurantId, categoryId)
        │
        ▼
Supabase: GET /rest/v1/menu_items?restaurant_id=eq.{id}
        │
        ▼
LiveData<List<MenuItem>> ──▶ UI

CREATE/UPDATE:
ViewModel.saveMenuItem(menuItem)
        │
        ▼
Repository.saveMenuItem() ──▶ Supabase
POST/PATCH /rest/v1/menu_items
        │
        ▼
Return to list, show success
```

## 4. API Endpoints

### 4.1 Get Menu Items
```
GET /rest/v1/menu_items?restaurant_id=eq.{id}&select=*,category:categories(*)
```

With category filter:
```
GET /rest/v1/menu_items?restaurant_id=eq.{id}&category_id=eq.{catId}
```

### 4.2 Create Menu Item
```
POST /rest/v1/menu_items
{
    "restaurant_id": "uuid",
    "category_id": "uuid",
    "name": "Phở Bò",
    "description": "Phở bò nấu theo công thức truyền thống",
    "price": 50000,
    "image_url": "https://...",
    "is_available": true,
    "is_featured": false,
    "sort_order": 1
}
```

### 4.3 Update Menu Item
```
PATCH /rest/v1/menu_items?id=eq.{id}
{
    "name": "...",
    "is_available": false
}
```

### 4.4 Delete Menu Item
```
DELETE /rest/v1/menu_items?id=eq.{id}
```

### 4.5 Toggle Available (Quick Update)
```
PATCH /rest/v1/menu_items?id=eq.{id}
{
    "is_available": true/false
}
```

## 5. UI Components

### 5.1 Menu List (fragment_menu_list.xml)
```xml
<LinearLayout android:orientation="vertical">
    <!-- Filters -->
    <Spinner android:id="@+id/spinnerRestaurant"/>
    <Spinner android:id="@+id/spinnerCategory"/>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvMenuItems"/>

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabAdd"/>
</LinearLayout>
```

### 5.2 Menu Item (item_menu_item.xml)
```xml
<com.google.android.material.card.MaterialCardView>
    <LinearLayout android:orientation="horizontal">
        <ImageView android:id="@+id/ivMenuItem"/>
        <LinearLayout>
            <TextView android:id="@+id/tvName"/>
            <TextView android:id="@+id/tvDescription"/>
            <TextView android:id="@+id/tvPrice"/>
            <LinearLayout>
                <TextView android:text="Nổi bật"/>
                <CheckBox android:id="@+id/cbFeatured"/>
                <TextView android:text="Còn hàng"/>
                <Switch android:id="@+id/switchAvailable"/>
            </LinearLayout>
        </LinearLayout>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

### 5.3 Menu Item Form (activity_menu_item_form.xml)
```xml
<ScrollView>
    <LinearLayout>
        <ImageView android:id="@+id/ivImage"/>
        <Button android:id="@+id/btnSelectImage"/>

        <com.google.android.material.textfield.TextInputLayout>
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etName"/>
        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout>
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etDescription"/>
        </com.google.android.material.textfield.TextInputLayout>

        <Spinner android:id="@+id/spinnerCategory"/>

        <com.google.android.material.textfield.TextInputLayout>
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etPrice"
                android:inputType="number"/>
        </com.google.android.material.textfield.TextInputLayout>

        <Switch android:id="@+id/switchAvailable"/>
        <Switch android:id="@+id/switchFeatured"/>

        <Button android:id="@+id/btnSave"/>
    </LinearLayout>
</ScrollView>
```

## 6. ViewModel

```java
public class MenuViewModel extends ViewModel {
    private MutableLiveData<List<MenuItem>> menuItems = new MutableLiveData<>();
    private MutableLiveData<List<Restaurant>> restaurants = new MutableLiveData<>();
    private MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private MutableLiveData<MenuItem> currentItem = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public void loadRestaurants() { }
    public void loadCategories() { }
    public void loadMenuItems(String restaurantId, String categoryId) { }
    public void saveMenuItem(MenuItem item) { }
    public void deleteMenuItem(String id) { }
    public void toggleAvailable(String id, boolean available) { }
    public void toggleFeatured(String id, boolean featured) { }
}
```

## 7. Validation Rules

| Field | Rule |
|-------|------|
| name | Required, max 100 chars |
| description | Optional, max 500 chars |
| price | Required, >= 0 |
| restaurant_id | Required |
| category_id | Required |
| is_available | Default true |
| is_featured | Default false |

## 8. Edge Cases

| Case | Handling |
|------|----------|
| Price = 0 | Allow (combo/deal items) |
| Delete with active orders | Show warning, allow delete |
| Image upload fail | Allow save without image |
| Category deleted | Show "Chưa phân loại" |

## 9. Staff Role Restriction

- Staff chỉ thấy menu_items của restaurant được gán
- RLS policy tự động lọc dữ liệu