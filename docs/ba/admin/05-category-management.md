# Module: Quản lý danh mục - App Admin

## 1. Overview
Module quản lý danh mục món ăn (Category): CRUD và sắp xếp thứ tự hiển thị.

## 2. Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | Category List | Danh sách danh mục theo sort_order |
| 2 | Add Category | Tạo danh mục mới |
| 3 | Edit Category | Sửa tên, ảnh, thứ tự |
| 4 | Delete Category | Xóa danh mục |
| 5 | Reorder | Kéo thả sắp xếp thứ tự |
| 6 | Upload Image | Upload ảnh danh mục |

## 3. Data Flow

```
LIST:
ViewModel.loadCategories()
        │
        ▼
Repository.getCategories() ──▶ Supabase
GET /rest/v1/categories?order=sort_order.asc
        │
        ▼
LiveData<List<Category>> ──▶ UI

CREATE/UPDATE:
ViewModel.saveCategory(category)
        │
        ▼
Repository.saveCategory() ──▶ Supabase
POST/PATCH /rest/v1/categories
        │
        ▼
Return to list
```

## 4. API Endpoints

### 4.1 Get Categories
```
GET /rest/v1/categories?select=*&order=sort_order.asc
```

### 4.2 Create Category
```
POST /rest/v1/categories
{
    "name": "Món chính",
    "image_url": "https://...",
    "sort_order": 1,
    "is_active": true
}
```

### 4.3 Update Category
```
PATCH /rest/v1/categories?id=eq.{id}
{
    "name": "...",
    "sort_order": 2
}
```

### 4.4 Delete Category
```
DELETE /rest/v1/categories?id=eq.{id}
```

## 5. UI Components

### 5.1 Category List (fragment_category_list.xml)
```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/rvCategories"/>

<com.google.android.material.floatingactionbutton.FloatingActionButton
    android:id="@+id/fabAdd"/>
```

### 5.2 Category Item (item_category.xml)
```xml
<com.google.android.material.card.MaterialCardView>
    <LinearLayout android:orientation="horizontal">
        <ImageView android:id="@+id/ivCategory"/>
        <TextView android:id="@+id/tvName"/>
        <TextView android:id="@+id/tvSortOrder"/>
        <ImageButton android:id="@+id/btnEdit"/>
        <ImageButton android:id="@+id/btnDelete"/>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

### 5.3 Category Form (activity_category_form.xml)
```xml
<LinearLayout>
    <ImageView android:id="@+id/ivImage"/>
    <Button android:id="@+id/btnSelectImage"/>

    <com.google.android.material.textfield.TextInputLayout>
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etName"/>
    </com.google.android.material.textfield.TextInputLayout>

    <com.google.android.material.textfield.TextInputLayout>
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etSortOrder"
            android:inputType="number"/>
    </com.google.android.material.textfield.TextInputLayout>

    <Switch android:id="@+id/switchActive"/>

    <Button android:id="@+id/btnSave"/>
</LinearLayout>
```

## 6. ViewModel

```java
public class CategoryViewModel extends ViewModel {
    private MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public void loadCategories() { }
    public void saveCategory(Category category) { }
    public void deleteCategory(String id) { }
    public void reorder(List<Category> categories) { }
}
```

## 7. Validation Rules

| Field | Rule |
|-------|------|
| name | Required, unique, max 50 chars |
| sort_order | Required, >= 0 |
| image_url | Optional |

## 8. Edge Cases

| Case | Handling |
|------|----------|
| Name already exists | Show error "Tên danh mục đã tồn tại" |
| Delete with menu items | Show warning "Danh mục có món ăn, xác nhận xóa?" |
| Duplicate sort_order | Auto-adjust on save |