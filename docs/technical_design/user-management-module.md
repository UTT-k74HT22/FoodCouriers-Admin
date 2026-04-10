---
title: "User Management Module - Technical Design"
description: "Thiết kế kỹ thuật cho module quản lý người dùng"
audience: [ai-agents, developers]
tags: [technical, admin, user]
created: 2026-04-07
---

# User Management Module - Technical Design

## 1. Architecture

### Layer Overview
```
┌─────────────────────────────────────┐
│           UI Layer                  │
│  (UserFragment, UserDetailActivity) │
└─────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│        Repository Layer             │
│         (UserRepository)             │
└─────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│       BaseSupabaseRepository        │
│    (fetchList, fetchSingle, etc.)    │
└─────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│        Supabase REST API            │
└─────────────────────────────────────┘
```

### Reference Pattern
- Sử dụng pattern từ `CategoryRepository` và `CategoryFragment`
- Repository extends `BaseSupabaseRepository`
- UI sử dụng `RepositoryCallback<T>` để handle responses

## 2. Database Schema

### 2.1 Users Table
```sql
CREATE TABLE public.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    phone TEXT,
    email TEXT NOT NULL,
    avatar_url TEXT,
    role TEXT NOT NULL CHECK (role IN ('customer', 'staff', 'admin')) DEFAULT 'customer',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_role ON public.users(role);
CREATE INDEX idx_users_email ON public.users(email);
CREATE INDEX idx_users_phone ON public.users(phone);
```

### 2.2 Orders Table
```sql
CREATE TABLE public.orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_code TEXT NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE SET NULL,
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE SET NULL,
    delivery_address TEXT NOT NULL,
    delivery_latitude DOUBLE PRECISION,
    delivery_longitude DOUBLE PRECISION,
    note TEXT,
    subtotal INTEGER NOT NULL,
    delivery_fee INTEGER NOT NULL DEFAULT 0,
    discount INTEGER NOT NULL DEFAULT 0,
    total INTEGER NOT NULL,
    payment_method TEXT NOT NULL CHECK (payment_method IN ('cod', 'online')) DEFAULT 'cod',
    payment_status TEXT NOT NULL CHECK (payment_status IN ('pending', 'paid', 'failed', 'refunded')) DEFAULT 'pending',
    status TEXT NOT NULL CHECK (status IN ('pending', 'confirmed', 'preparing', 'delivering', 'delivered', 'cancelled')) DEFAULT 'pending',
    cancelled_reason TEXT,
    promotion_id UUID REFERENCES promotions(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user ON public.orders(user_id);
CREATE INDEX idx_orders_created_at ON public.orders(created_at DESC);
```

## 3. API Contracts

### 3.1 Get Users List
```
GET /rest/v1/users?role=eq.customer&select=*&order=created_at.desc&limit=50

Query Parameters:
- role: eq.customer (filter chỉ lấy customer)
- select: * (lấy all columns)
- order: created_at.desc (sort by newest)
- limit: 50 (pagination)

Response:
[
    {
        "id": "uuid",
        "auth_id": "uuid",
        "full_name": "Nguyen Van A",
        "phone": "0123456789",
        "email": "a@example.com",
        "avatar_url": "https://...",
        "role": "customer",
        "is_active": true,
        "created_at": "2026-04-01T10:00:00Z",
        "updated_at": "2026-04-01T10:00:00Z"
    }
]
```

### 3.2 Search Users
```
GET /rest/v1/users?role=eq.customer&or=(full_name.ilike.*${search}*,email.ilike.*${search}*,phone.ilike.*${search}*)&select=*

URL encoded query example:
full_name.ilike.*a*,email.ilike.*a*,phone.ilike.*a*
```

### 3.3 Filter Users by Active Status
```
GET /rest/v1/users?role=eq.customer&is_active=eq.true&select=*
GET /rest/v1/users?role=eq.customer&is_active=eq.false&select=*
```

### 3.4 Get User Detail
```
GET /rest/v1/users?id=eq.${userId}&select=*

Response:
{
    "id": "uuid",
    "auth_id": "uuid",
    "full_name": "Nguyen Van A",
    "phone": "0123456789",
    "email": "a@example.com",
    "avatar_url": "https://...",
    "role": "customer",
    "is_active": true,
    "created_at": "2026-04-01T10:00:00Z",
    "updated_at": "2026-04-01T10:00:00Z"
}
```

### 3.5 Get User Orders
```
GET /rest/v1/orders?user_id=eq.${userId}&select=*&order=created_at.desc&limit=20

Response:
[
    {
        "id": "uuid",
        "order_code": "ORD202604070001",
        "user_id": "uuid",
        "restaurant_id": "uuid",
        "total": 150000,
        "status": "delivered",
        "created_at": "2026-04-07T10:00:00Z"
    }
]
```

### 3.6 Update User Status
```
PATCH /rest/v1/users?id=eq.${userId}
Header: Prefer: return=representation
Body: { "is_active": false }

Response:
{
    "id": "uuid",
    "full_name": "Nguyen Van A",
    "is_active": false,
    ...
}
```

## 4. Security Considerations

### 4.1 RLS Policies
- Admin có full access
- Staff có thể read users nhưng không thể modify
- Customer chỉ có thể read own profile

### 4.2 API Key
- Sử dụng `SUPABASE_ANON_KEY` trong header
- Bearer token từ session cho authenticated requests

### 4.3 Input Validation
- Validate userId không empty trước khi gọi API
- Handle null responses gracefully
- Sanitize search query (URL encode)

## 5. Data Models

### 5.1 User Model (exists)
File: `app/src/main/java/com/utt/foodcouriers_admin/data/model/User.java`

### 5.2 Order Model (new)
```java
public class Order {
    @SerializedName("id")
    private String id;
    
    @SerializedName("order_code")
    private String orderCode;
    
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("restaurant_id")
    private String restaurantId;
    
    @SerializedName("total")
    private Integer total;
    
    @SerializedName("status")
    private String status;
    
    @SerializedName("created_at")
    private String createdAt;
    
    // getters, setters
}
```

### 5.3 UserUpsertRequest DTO (new)
```java
public class UserUpsertRequest {
    @SerializedName("full_name")
    private String fullName;
    
    @SerializedName("phone")
    private String phone;
    
    @SerializedName("avatar_url")
    private String avatarUrl;
    
    @SerializedName("is_active")
    private Boolean isActive;
    
    // constructors, getters, setters
}
```

## 6. Repository Pattern

### UserRepository Interface
```java
public class UserRepository extends BaseSupabaseRepository implements CrudRepository<User, UserUpsertRequest> {
    
    private static final String TABLE = "users";
    private static final String ORDER_TABLE = "orders";
    
    // getUsers(searchQuery, isActive, limit, offset)
    // getUserById(id)
    // getUserOrders(userId, limit, offset)
    // updateUserStatus(id, isActive)
}
```

## 7. UI Components

### 7.1 UserFragment
- Similar structure to CategoryFragment
- Uses SwipeRefreshLayout
- SearchView with debounce
- ChipGroup for filters
- RecyclerView with UserAdapter
- States: loading, empty, error, content

### 7.2 UserDetailActivity
- ScrollView layout
- User info section
- Orders RecyclerView
- MaterialButton for status toggle

### 7.3 Adapters
- UserAdapter: with switch toggle for is_active
- OrderAdapter: display-only (no edit)

## 8. State Management

### LiveData pattern (if using ViewModel)
```java
public class UserViewModel extends ViewModel {
    private MutableLiveData<List<User>> users = new MutableLiveData<>();
    private MutableLiveData<User> currentUser = new MutableLiveData<>();
    private MutableLiveData<List<Order>> userOrders = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
}
```

### Direct callback pattern (current project style)
Sử dụng `RepositoryCallback<T>` như pattern hiện tại của project.

## 9. Error Handling

| Error Code | Handling |
|------------|----------|
| NETWORK_ERROR | Show error state với retry button |
| FETCH_FAILED | Parse và show error message |
| NOT_FOUND | Show "Không tìm thấy người dùng" |
| UPDATE_FAILED | Show toast error, không refresh |

## 10. File Locations

| File | Path |
|------|------|
| User model | `data/model/User.java` (exists) |
| Order model | `data/model/Order.java` (new) |
| UserUpsertRequest | `data/request/UserUpsertRequest.java` (new) |
| UserRepository | `data/repository/UserRepository.java` (new) |
| UserFragment | `ui/user/UserFragment.java` (new) |
| UserDetailActivity | `ui/user/UserDetailActivity.java` (new) |
| UserAdapter | `ui/user/adapter/UserAdapter.java` (new) |
| OrderAdapter | `ui/user/adapter/OrderAdapter.java` (new) |
| Layouts | `res/layout/` (new XMLs) |
| Menu | `res/menu/menu_navigation_drawer.xml` (modify) |
