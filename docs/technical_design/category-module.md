---
title: "Category Module - Technical Design"
description: "Kỹ thuật chi tiết cho module quản lý danh mục (Category)"
audience: [ai-agents, developers]
tags: [technical, category, android]
created: 2026-04-06
status: draft
---

# Category Module - Technical Design

## 1. Architecture

### Package Structure (Admin App)
```
com.utt.foodcouriers_admin/
├── api/
│   ├── CategoryApiService.java
│   └── ApiClient.java
├── model/
│   ├── Category.java
│   └── CategoryResponse.java
├── ui/
│   ├── category/
│   │   ├── CategoryListFragment.java
│   │   ├── CategoryAdapter.java
│   │   └── CategoryFormDialog.java
│   └── adapter/
│       └── CategoryViewHolder.java
└── repository/
    └── CategoryRepository.java
```

### Package Structure (Client App)
```
com.utt.foodcouriers/
├── api/
│   └── CategoryApiService.java
├── model/
│   └── Category.java
├── repository/
│   └── CategoryRepository.java
└── ui/
    └── home/
        └── HomeFragment.java (hiển thị categories)
```

---

## 2. Database Schema

### categories table (đã có trong schema)

```sql
CREATE TABLE public.categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    image_url TEXT,
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_categories_sort_order ON public.categories(sort_order);
```

### RLS Policies

```sql
-- Enable RLS
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;

-- Admin: Full access
CREATE POLICY "Admin full access" ON public.categories
    FOR ALL
    TO authenticated
    USING (auth.jwt() ->> 'role' = 'admin');

-- Client: Read only active categories
CREATE POLICY "Client read active" ON public.categories
    FOR SELECT
    TO authenticated
    USING (is_active = true);
```

---

## 3. API Contracts

### 3.1 Admin - Get Categories

**Request:**
```
GET /rest/v1/categories
?select=id,name,image_url,sort_order,is_active,created_at,menu_items(count)
&order=sort_order.asc
&limit={pageSize}
&offset={offset}
```

**Response:**
```json
[
  {
    "id": "uuid",
    "name": "Món chính",
    "image_url": "https://...",
    "sort_order": 1,
    "is_active": true,
    "created_at": "2026-04-06T00:00:00Z",
    "menu_items": [{"count": 15}]
  }
]
```

### 3.2 Admin - Create Category

**Request:**
```
POST /rest/v1/categories
Content-Type: application/json

{
  "name": "Món chính",
  "image_url": "https://...",
  "sort_order": 1
}
```

**Response:** 201 Created

### 3.3 Admin - Update Category

**Request:**
```
PATCH /rest/v1/categories?id=eq.{id}
Content-Type: application/json

{
  "name": "Món chính Updated",
  "sort_order": 2,
  "is_active": false
}
```

**Response:** 200 OK

### 3.4 Admin - Delete Category

**Request:**
```
DELETE /rest/v1/categories?id=eq.{id}
```

**Response:** 204 No Content

**Error:** 400 nếu category có menu_items

### 3.5 Client - Get Active Categories

**Request:**
```
GET /rest/v1/categories
?select=id,name,image_url,sort_order
&is_active=eq.true
&order=sort_order.asc
```

**Response:**
```json
[
  {
    "id": "uuid",
    "name": "Món chính",
    "image_url": "https://...",
    "sort_order": 1
  }
]
```

---

## 4. Model Classes

### Category.java (Admin & Client)
```java
public class Category {
    private UUID id;
    private String name;
    private String imageUrl;
    private Integer sortOrder;
    private Boolean isActive;
    private Timestamp createdAt;
    private Integer itemCount; // Computed field
    
    // Getters & Setters
}
```

---

## 5. UI Components

### 5.1 CategoryListFragment (Admin)

**Layout:** `category_list.xml`
```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout>
    <com.google.android.material.appbar.AppBarLayout>
        <androidx.appcompat.widget.Toolbar android:id="@+id/toolbar"/>
    </com.google.android.material.appbar.AppBarLayout>
    
    <LinearLayout>
        <androidx.appcompat.widget.SearchView android:id="@+id/searchView"/>
        <androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
            <androidx.recyclerview.widget.RecyclerView 
                android:id="@+id/recyclerView"/>
        </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
    </LinearLayout>
    
    <com.google.android.material.floatingactionbutton.FloatingActionButton 
        android:id="@+id/fabAdd"/>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### 5.2 CategoryAdapter

**ViewHolder Layout:** `item_category.xml`
```xml
<LinearLayout>
    <ImageView android:id="@+id/ivImage"/>
    <LinearLayout>
        <TextView android:id="@+id/tvName"/>
        <TextView android:id="@+id/tvItemCount"/>
    </LinearLayout>
    <Switch android:id="@+id/switchActive"/>
</LinearLayout>
```

### 5.3 CategoryFormDialog

**Dialog Layout:** `dialog_category_form.xml`
```xml
<LinearLayout orientation="vertical">
    <com.google.android.material.textfield.TextInputLayout>
        <com.google.android.material.textfield.TextInputEditText android:id="@+id/etName"/>
    </com.google.android.material.textfield.TextInputLayout>
    
    <ImageView android:id="@+id/ivPreview"/>
    <Button android:id="@+id/btnPickImage"/>
    
    <com.google.android.material.textfield.TextInputLayout>
        <com.google.android.material.textfield.TextInputEditText android:id="@+id/etSortOrder"/>
    </com.google.android.material.textfield.TextInputLayout>
</LinearLayout>
```

---

## 6. Edge Cases & Error Handling

| Scenario | Handling |
|----------|-----------|
| Delete category with items | Show dialog: "Chuyển items sang category khác?" |
| Image upload fail | Show snackbar error, allow retry |
| Network error | Show error state, retry button |
| Duplicate name | Show validation error on form |
| Empty list | Show empty state illustration |

---

## 7. Security Considerations

- RLS policies ngăn client truy cập inactive categories
- Admin role check trước mọi write operations
- Image upload validate file type (jpg, png, webp) và size (<2MB)