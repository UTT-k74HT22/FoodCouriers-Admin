# Tài liệu BA - App Client
## Hệ thống đặt đồ ăn

---

## 1. Project Overview

### 1.1 Project Information
| Item | Value |
|------|-------|
| Project Name | Food Ordering System |
| App Name | FoodDelivery Client |
| Platform | Android Native |
| Language | Java + XML |
| Backend | Supabase (PostgreSQL + Auth + Storage + Realtime) |
| Version | 1.0.0 |
| Target Users | Khách hàng đặt đồ ăn |

### 1.2 Technology Stack
| Layer | Technology |
|-------|------------|
| Mobile | Android 8.0+ (API 26), Target API 34 |
| UI | Material Design 3, XML Layout |
| Architecture | MVVM + Repository Pattern |
| Local Storage | Room Database (cart cache) |
| Image Loading | Glide 4.16.0 |
| Backend | Supabase SDK 2.x |

### 1.3 User Role
| Role | Description |
|------|-------------|
| **Customer** | Người dùng đặt đồ ăn, có thể xem restaurant, đặt hàng, theo dõi đơn |

---

## 2. Module Breakdown

### MODULE 2.1: AUTHENTICATION

#### 2.1.1 Module Description
Module xử lý đăng nhập, đăng ký, quên mật khẩu cho khách hàng.

#### 2.1.2 Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | Login | Đăng nhập bằng email + password |
| 2 | Register | Đăng ký tài khoản mới |
| 3 | Password Reset | Quên mật khẩu qua email |
| 4 | Session Management | Lưu JWT token, auto refresh |
| 5 | Logout | Đăng xuất |

#### 2.1.3 User Flow
```
┌─────────────────────────────────────────────────────────────┐
│                    LOGIN FLOW                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌──────────┐     ┌──────────────┐     ┌──────────────┐   │
│   │  Login   │ ──▶ │  Validate    │ ──▶ │  Auth API    │   │
│   │  Screen  │     │  Credentials │     │  Supabase    │   │
│   └──────────┘     └──────────────┘     └──────────────┘   │
│                                                 │            │
│                                                 ▼            │
│                              ┌────────────────────────────┐  │
│                              │         SUCCESS            │  │
│                              │  Save Token + Role        │  │
│                              │  Navigate to Home         │  │
│                              └────────────────────────────┘  │
│                                                 │            │
│                                                 ▼            │
│                              ┌────────────────────────────┐  │
│                              │         FAILED             │  │
│                              │  Display error message    │  │
│                              └────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   REGISTER FLOW                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌────────────┐     ┌───────────────┐     ┌───────────┐  │
│   │  Register  │ ──▶ │  Validate     │ ──▶ │  Supabase │  │
│   │   Screen   │     │  Form Data    │     │  Auth API │  │
│   └────────────┘     └───────────────┘     └───────────┘  │
│           │                                        │        │
│           ▼                                        ▼        │
│   ┌─────────────────────┐         ┌────────────────────┐   │
│   │  Validation Errors  │         │   SUCCESS          │   │
│   │  Show inline errors │         │  Email verification │   │
│   └─────────────────────┘         │  Navigate to Login │   │
│                                    └────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

#### 2.1.4 UI Components
| Screen | Component | Files |
|--------|-----------|-------|
| Login | LoginActivity | activity_login.xml |
| Register | RegisterActivity | activity_register.xml |
| Forgot Password | ResetPasswordActivity | activity_reset_password.xml |

#### 2.1.5 Data Flow
```
UI Layer          ViewModel          Repository         Supabase
  │                    │                    │                  │
  ▼                    ▼                    ▼                  ▼
Input ─────────▶ AuthViewModel ───▶ AuthRepository ───▶ Auth API
email/password    │                   │                  │
  │               │                   │                  │
  │               ▼                   ▼                  ▼
  │          LiveData           SessionManager       JWT Token
  │          (Result)           (Token + User)       (Return)
  │               │                   │                  │
  └───────────────▶│───────────────────▶│──────────────────▶
                  Result              Save                  
```

#### 2.1.6 Edge Cases
| Case | Handling |
|------|----------|
| Wrong password | Show error "Sai mật khẩu" |
| Email not registered | Show error "Email chưa đăng ký" |
| Email already exists | Show error "Email đã được sử dụng" |
| Network error | Show "Không có kết nối mạng" |
| Token expired | Auto refresh, nếu fail thì logout |

---

### MODULE 2.2: HOME SCREEN

#### 2.2.1 Module Description
Màn hình chính hiển thị banners, danh mục, nhà hàng nổi bật.

#### 2.2.2 Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | Banners | Carousel banner quảng cáo |
| 2 | Categories | Danh sách danh mục món ăn |
| 3 | Featured Restaurants | Nhà hàng nổi bật (rating cao) |
| 4 | Near Restaurants | Nhà hàng gần bạn |
| 5 | Search Bar | Tìm kiếm nhà hàng/món ăn |
| 6 | Pull to Refresh | Cập nhật dữ liệu mới |

#### 2.2.3 User Flow
```
┌─────────────────────────────────────────────────────────────┐
│                    HOME SCREEN FLOW                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  ┌─────────────────────────────────────────────┐   │   │
│  │  │        BANNER CAROUSEL (ViewPager2)         │   │   │
│  │  │   [Banner 1]  [Banner 2]  [Banner 3]         │   │   │
│  │  └─────────────────────────────────────────────┘   │   │
│  │                                                      │   │
│  │  Search: [🔍 Tìm nhà hàng, món ăn...]           │   │
│  │                                                      │   │
│  │  ┌─────────────────────────────────────────────┐   │   │
│  │  │        CATEGORIES (Horizontal Scroll)       │   │   │
│  │  │   [🍔] [🍜] [🍕] [🥤] [🍰]                  │   │   │
│  │  │   Món chính  Phở  Pizza  Đồ uống  Tráng miệng│   │   │
│  │  └─────────────────────────────────────────────┘   │   │
│  │                                                      │   │
│  │  Nổi bật                                     Xem thêm│   │
│  │  ┌────────┐ ┌────────┐ ┌────────┐                  │   │
│  │  │ Restaurant A   │ Restaurant B   │ ...          │   │
│  │  │   ⭐ 4.5      │   ⭐ 4.8      │               │   │
│  │  │  2km        │  3km        │                  │   │
│  │  └────────┘ └────────┘ └────────┘                  │   │
│  │                                                      │   │
│  │  Gần bạn                                      Xem thêm│   │
│  │  ┌────────┐ ┌────────┐ ┌────────┐                  │   │
│  │  │ Restaurant C   │ Restaurant D   │ ...          │   │
│  │  └────────┘ └────────┘ └────────┘                  │   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                  │
│                           ▼                                  │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐    │
│  │ Tap Banner   │   │ Tap Category │   │ Tap Restaurant│   │
│  │ ────────────▶│   │ ────────────▶│   │ ────────────▶│    │
│  │ Restaurant   │   │ Category     │   │ Restaurant   │    │
│  │ Detail       │   │ Menu         │   │ Detail       │    │
│  └──────────────┘   └──────────────┘   └──────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

#### 2.2.4 UI Components
| Screen | Component | Files |
|--------|-----------|-------|
| Home | HomeFragment | fragment_home.xml |
| | ViewPager2 banners | |
| | RecyclerView categories (horizontal) | |
| | RecyclerView restaurants | |
| | SwipeRefreshLayout | |

#### 2.2.5 Data Flow
```
HomeFragment.onViewCreated()
       │
       ▼
HomeViewModel.loadHomeData()
       │
       ├─▶ Repository.getBanners()
       │       │
       │       ▼
       │    Supabase: banners?is_active=true&...
       │
       ├─▶ Repository.getCategories()
       │       │
       │       ▼
       │    Supabase: categories?is_active=true&order=sort_order
       │
       ├─▶ Repository.getFeaturedRestaurants()
       │       │
       │       ▼
       │    Supabase: restaurants?is_featured=true&is_open=true
       │
       └─▶ Repository.getNearRestaurants()
               │
               ▼
            Supabase: restaurants?is_open=true&order=distance
               │
               ▼
        LiveData<HomeData> ──▶ UI
```

#### 2.2.6 Edge Cases
| Case | Handling |
|------|----------|
| No banners | Hide banner section |
| No categories | Show "Chưa có danh mục" |
| No restaurants | Show "Không có nhà hàng nào" |
| Location permission denied | Show "Cho phép vị trí để tìm nhà hàng gần" |
| Network error | Show cached data + error banner |

---

### MODULE 2.3: RESTAURANT DETAIL

#### 2.3.1 Module Description
Xem chi tiết nhà hàng, menu và đặt món.

#### 2.3.2 Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | Restaurant Info | Tên, ảnh, địa chỉ, đánh giá, giờ mở cửa |
| 2 | Menu by Category | Danh sách món theo danh mục |
| 3 | Add to Cart | Thêm món vào giỏ hàng |
| 4 | Search in Restaurant | Tìm món trong nhà hàng |
| 5 | Reviews | Xem đánh giá nhà hàng |

#### 2.3.3 User Flow
```
┌─────────────────────────────────────────────────────────────┐
│                RESTAURANT DETAIL FLOW                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                 HEADER                               │   │
│  │  ┌─────────────────────────────────────────────┐   │   │
│  │  │            Restaurant Image                 │   │   │
│  │  └─────────────────────────────────────────────┘   │   │
│  │  Restaurant A                                      │   │
│  │  ⭐ 4.5 (120 đánh giá)                            │   │
│  │  📍 123 Main St - 2km                            │   │
│  │  🕒 08:00 - 22:00 | 🚚 15k                       │   │
│  │  ─────────────────────────────────────────────     │   │
│  │  Description: Nhà hàng chuyên phở bò...           │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Categories TabLayout                                        │
│  ┌─────────┬─────────┬─────────┬─────────┐                  │
│  │ Món chính│ Món phụ │ Đồ uống │ Tráng miệng│           │
│  └─────────┴─────────┴─────────┴─────────┘                  │
│                                                             │
│  Menu Items (by Category)                                   │
│  ┌────────┐ ┌────────────────────────────────────────┐    │
│  │  Img   │ │ Phở Bò - 50k                          │    │
│  │        │ │ Description: Phở bò nấu...            │    │
│  │        │ │                    [+] ─── [1] ── [+] │    │
│  └────────┘ └────────────────────────────────────────┘    │
│  ┌────────┐ ┌────────────────────────────────────────┐    │
│  │  Img   │ │ Cơm Rang - 35k                        │    │
│  │        │ │                    [+] ─── [2] ── [+]  │    │
│  └────────┘ └────────────────────────────────────────┘    │
│                                                             │
│  Cart Floating Button (shows count)                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 🛒 3 món - 120k              [Xem giỏ hàng]       │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

#### 2.3.4 UI Components
| Screen | Component | Files |
|--------|-----------|-------|
| Restaurant Detail | RestaurantDetailActivity | activity_restaurant_detail.xml |
| | CollapsingToolbarLayout | |
| | TabLayout categories | |
| | RecyclerView menu items | |
| | FloatingCartButton | |

#### 2.3.5 Add to Cart Flow
```
User taps "+" on menu item
       │
       ▼
Check: Is item available?
       │ No ──▶ Show "Món này hiện không có"
       │
       Yes
       │
       ▼
Check: Is same restaurant as current cart?
       │ No ──▶ Show ConfirmDialog:
       │       "Giỏ hàng có món từ restaurant khác. Xóa và thêm món mới?"
       │
       Yes
       │
       ▼
Add to Room DB (local cart)
       │
       ▼
Update Cart Badge + Total
       │
       ▼
Show Snackbar: "Đã thêm {item_name} vào giỏ hàng"
```

#### 2.3.6 Data Flow
```
Load Restaurant:
ViewModel.loadRestaurant(restaurantId)
       │
       ▼
Repository.getRestaurant(restaurantId)
       │
       ▼
Supabase: restaurants?id=eq.{id}
       │
       ▼
LiveData<Restaurant>

Load Menu:
ViewModel.loadMenuItems(restaurantId)
       │
       ▼
Repository.getMenuItems(restaurantId)
       │
       ▼
Supabase: menu_items?restaurant_id=eq.{id}&is_available=true
       │
       ▼
Group by category ──▶ LiveData<List<MenuByCategory>>

Add to Cart:
ViewModel.addToCart(menuItem, quantity)
       │
       ▼
CartRepository.addItem(item)
       │
       ▼
Room DB: cart_items table
       │
       ▼
LiveData<CartSummary>
```

#### 2.3.7 Edge Cases
| Case | Handling |
|------|----------|
| Item not available | Disable add button, show "Hết hàng" |
| Restaurant closed | Show "Nhà hàng đóng cửa", disable add |
| Restaurant changed | Confirm dialog, clear cart |
| Cart exists from other restaurant | Confirm before add |

---

### MODULE 2.4: CART & CHECKOUT

#### 2.4.1 Module Description
Quản lý giỏ hàng và thanh toán đơn hàng.

#### 2.4.2 Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | View Cart | Xem danh sách món trong giỏ |
| 2 | Update Quantity | Tăng/giảm số lượng |
| 3 | Remove Item | Xóa món khỏi giỏ |
| 4 | Apply Promo | Nhập mã khuyến mãi |
| 5 | Select Address | Chọn địa chỉ giao hàng |
| 6 | Add New Address | Thêm địa chỉ mới |
| 7 | Order Note | Ghi chú cho đơn hàng |
| 8 | Place Order | Đặt hàng (COD) |
| 9 | Order Success | Màn hình thành công |

#### 2.4.3 Cart Flow
```
┌─────────────────────────────────────────────────────────────┐
│                      CART FLOW                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                 CART SCREEN                          │   │
│  │  Restaurant: Restaurant A                           │   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  ┌────────┐ ┌──────────────────────────────────┐   │   │
│  │  │  Img   │ │ Phở Bò (x2)          100k        │   │   │
│  │  │        │ │ [+][1][-]              🗑️        │   │   │
│  │  └────────┘ └──────────────────────────────────┘   │   │
│  │  ┌────────┐ ┌──────────────────────────────────┐   │   │
│  │  │  Img   │ │ Trà đá (x2)           20k        │   │   │
│  │  │        │ │ [+][2][-]              🗑️        │   │   │
│  │  └────────┘ └──────────────────────────────────┘   │   │
│  │                                                      │   │
│  │  Promo Code: [___________] [Áp dụng]              │   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  Tạm tính: 120k                                   │   │
│  │  Phí giao hàng: 15k                                │   │
│  │  Giảm giá: -10k                                    │   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  Tổng cộng: 125k                                   │   │
│  │                                                      │   │
│  │  ┌─────────────────────────────────────────────┐   │   │
│  │  │  📍 Địa chỉ giao hàng                       │   │   │
│  │  │  [Chọn địa chỉ ▼]                          │   │   │
│  │  └─────────────────────────────────────────────┘   │   │
│  │                                                      │   │
│  │  Ghi chú: [________________________]               │   │
│  │                                                      │   │
│  │  [Đặt hàng (125k)]                                  │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

#### 2.4.4 Checkout Flow
```
User taps "Đặt hàng"
       │
       ▼
Validate:
  - Cart not empty? ──▶ Error: "Giỏ hàng trống"
  - Restaurant open? ──▶ Error: "Nhà hàng đóng cửa"
  - Address selected? ──▶ Error: "Chọn địa chỉ giao hàng"
  - Min order met? ──▶ Error: "Đơn tối thiểu {min_order}k"
       │
       ▼
Call RPC: rpc_create_order
       │
       ├─▶ Success:
       │    - Clear local cart (Room)
       │    - Navigate to Order Success
       │    - Show order code
       │
       └─▶ Error:
            - Show error message
            - Keep cart
```

#### 2.4.5 UI Components
| Screen | Component | Files |
|--------|-----------|-------|
| Cart | CartFragment | fragment_cart.xml |
| | RecyclerView items | |
| | Quantity controls | |
| | Promo input | |
| | Address selector | |
| Checkout | CheckoutActivity | activity_checkout.xml |
| Order Success | OrderSuccessActivity | activity_order_success.xml |

#### 2.4.6 Data Flow

**Cart (Local - Room DB):**
```
UI Layer          ViewModel          Repository         Room DB
  │                    │                    │                  │
  ▼                    ▼                    ▼                  ▼
Load ─────────▶ CartViewModel ───▶ CartRepository ───▶ cart_items
  │               │                   │              table
  │               ▼                   ▼                  │
  │          LiveData<List<CartItem>> CRUD                │
  │               │                   │                  │
  └───────────────▶│──────────────────▶│──────────────────▶
                Display              Result              
```

**Checkout (Server):**
```
UI Layer          ViewModel          Repository         Supabase
  │                    │                    │                  │
  ▼                    ▼                    ▼                  ▼
Order ─────────▶ CheckoutViewModel ─▶ OrderRepository ──▶ RPC
          Validate    │                   │              rpc_create
          cart         │                   │              _order
          │           │                   │                  │
          │           ▼                   ▼                  │
          │      LiveData<Result>    Success/Fail          │
          │           │                   │                  │
          └───────────▶│──────────────────▶│──────────────────▶
                    Display              Clear Cart         
```

#### 2.4.7 Edge Cases
| Case | Handling |
|------|----------|
| Cart empty | Show "Giỏ hàng trống", hide checkout |
| Restaurant closed | Disable checkout, show "Nhà hàng đóng cửa" |
| Min order not met | Show error, disable checkout |
| Promo invalid | Show "Mã khuyến mãi không hợp lệ" |
| Network error | Show error, giữ cart |
| Concurrent update | Handle race condition |

---

### MODULE 2.5: ORDER HISTORY

#### 2.5.1 Module Description
Xem lịch sử đơn hàng và theo dõi trạng thái.

#### 2.5.2 Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | Order List | Danh sách đơn hàng đã đặt |
| 2 | Order Detail | Xem chi tiết đơn |
| 3 | Order Status | Theo dõi trạng thái realtime |
| 4 | Cancel Order | Hủy đơn (nếu chưa xác nhận) |
| 5 | Reorder | Đặt lại đơn cũ |
| 6 | Reviews | Đánh giá sau khi nhận hàng |

#### 2.5.3 User Flow
```
┌─────────────────────────────────────────────────────────────┐
│                  ORDER HISTORY FLOW                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              ORDER LIST SCREEN                       │   │
│  │  ┌─────────────────────────────────────────────┐   │   │
│  │  │ ORD001 - Restaurant A - 150k - 🟢 Delivered │   │   │
│  │  │ 2024-01-15 10:30                            │   │   │
│  │  ├─────────────────────────────────────────────┤   │   │
│  │  │ ORD002 - Restaurant B - 200k - 🟡 Delivering│   │   │
│  │  │ 2024-01-15 11:00                            │   │   │
│  │  ├─────────────────────────────────────────────┤   │   │
│  │  │ ORD003 - Restaurant C - 80k - 🔴 Cancelled  │   │   │
│  │  │ 2024-01-14 18:00                            │   │   │
│  │  └─────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                  │
│                           ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              ORDER DETAIL SCREEN                    │   │
│  │  Order: ORD001                                     │   │
│  │  Status: ⭐ Delivered (Hoàn thành)                │   │
│  │  ─────────────────────────────────────────────     │   │
│  │  Restaurant: Restaurant A                         │   │
│  │  Items:                                            │   │
│  │  - Phở Bò (x2) - 100k                            │   │
│  │  - Trà đá (x2) - 20k                             │   │
│  │  ─────────────────────────────────────────────     │   │
│  │  Tổng: 120k + 15k (ship) - 15k (giảm) = 120k     │   │
│  │  Address: 123 Nguyễn Trãi, Q1                    │   │
│  │  ─────────────────────────────────────────────     │   │
│  │  [Đánh giá] [Đặt lại]                             │   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                  │
│                           ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              ORDER STATUS TIMELINE                  │   │
│  │  ┌────┐ ─── ┌────┐ ─── ┌────┐ ─── ┌────┐           │   │
│  │  │ ✅ │ ─── │ ✅ │ ─── │ ✅ │ ─── │ ✅ │           │   │
│  │  │Pending│ │Confirm│ │Prep │ │Deliver│           │   │
│  │  └────┘     └────┘     └────┘     └────┘           │   │
│  │  10:30      10:35     10:50      11:15             │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

#### 2.5.4 UI Components
| Screen | Component | Files |
|--------|-----------|-------|
| Order List | OrderHistoryFragment | fragment_order_history.xml |
| | TabLayout (All, Processing, Completed, Cancelled) | |
| Order Detail | OrderDetailActivity | activity_order_detail.xml |
| | Status timeline | |
| | Action buttons | |

#### 2.5.5 Data Flow
```
Load Orders:
ViewModel.loadOrders()
       │
       ▼
Repository.getMyOrders()
       │
       ▼
Supabase: orders?user_id=eq.{userId}&order=created_at.desc
       │
       ▼
LiveData<List<Order>> ──▶ UI

Realtime Update:
Supabase Realtime ──▶ orders table
       │
       ▼
Update order status in list
       │
       ▼
Show notification: "Order {code} status changed to {status}"
```

#### 2.5.6 Edge Cases
| Case | Handling |
|------|----------|
| No orders | Show "Bạn chưa có đơn hàng nào" |
| Order cancelled by restaurant | Show reason in detail |
| Network error | Show cached orders |
| Realtime disconnect | Poll to sync on reconnect |

---

### MODULE 2.6: USER PROFILE

#### 2.6.1 Module Description
Quản lý thông tin cá nhân và địa chỉ.

#### 2.6.2 Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | View Profile | Xem thông tin tài khoản |
| 2 | Edit Profile | Cập nhật tên, phone, avatar |
| 3 | Address List | Danh sách địa chỉ giao hàng |
| 4 | Add Address | Thêm địa chỉ mới |
| 5 | Edit Address | Sửa địa chỉ |
| 6 | Delete Address | Xóa địa chỉ |
| 7 | Set Default Address | Đặt làm địa chỉ mặc định |
| 8 | Logout | Đăng xuất |

#### 2.6.3 User Flow
```
┌─────────────────────────────────────────────────────────────┐
│                    PROFILE FLOW                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              PROFILE SCREEN                          │   │
│  │  ┌──────┐                                           │   │
│  │  │ Avatar│  Nguyễn Văn A                          │   │
│  │  │      │  a@example.com                          │   │
│  │  │ Edit │  0901234567                             │   │
│  │  └──────┘                                           │   │
│  │                                                      │   │
│  │  📍 Địa chỉ giao hàng                    [+]       │   │
│  │  ┌─────────────────────────────────────────────┐   │   │
│  │  │ 🏠 Nhà - 123 Nguyễn Trãi (Mặc định)        │   │   │
│  │  │ 💼 Công ty - 456 Lê Lợi                      │   │   │
│  │  └─────────────────────────────────────────────┘   │   │
│  │                                                      │   │
│  │  📋 Lịch sử đơn hàng                               │   │
│  │  ❓ Trợ giúp & FAQ                                 │   │
│  │  ❎ Đăng xuất                                      │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              ADD/EDIT ADDRESS SCREEN               │   │
│  │  Label: [Nhà/Công ty/Khác]                        │   │
│  │  Address: [________________________]              │   │
│  │  ─────────────────────────────────────────────     │   │
│  │  [ ] Đặt làm mặc định                              │   │
│  │  [Hủy]                        [Lưu]                │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

#### 2.6.4 UI Components
| Screen | Component | Files |
|--------|-----------|-------|
| Profile | ProfileFragment | fragment_profile.xml |
| Edit Profile | EditProfileActivity | activity_edit_profile.xml |
| Address List | AddressListActivity | activity_address_list.xml |
| Add/Edit Address | AddressFormActivity | activity_address_form.xml |

#### 2.6.5 Data Flow
```
Load Profile:
ViewModel.loadProfile()
       │
       ▼
Repository.getUser(userId)
       │
       ▼
Supabase: users?id=eq.{id}
       │
       ▼
LiveData<User>

Load Addresses:
ViewModel.loadAddresses()
       │
       ▼
Repository.getAddresses(userId)
       │
       ▼
Supabase: user_addresses?user_id=eq.{id}
       │
       ▼
LiveData<List<Address>>
```

#### 2.6.6 Edge Cases
| Case | Handling |
|------|----------|
| No addresses | Show "Chưa có địa chỉ, thêm ngay?" |
| Delete last address | Allow, show warning |
| Update profile error | Show error message |

---

### MODULE 2.7: SEARCH

#### 2.7.1 Module Description
Tìm kiếm nhà hàng và món ăn.

#### 2.7.2 Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | Search Bar | Nhập từ khóa tìm kiếm |
| 2 | Recent Searches | Lịch sử tìm kiếm |
| 3 | Search Results | Kết quả: Restaurants + Menu Items |
| 4 | Filter | Lọc theo: Danh mục, Giá, Đánh giá |
| 5 | Sort | Sắp xếp: Mặc định, Gần nhất, Đánh giá cao |

#### 2.7.3 User Flow
```
┌─────────────────────────────────────────────────────────────┐
│                    SEARCH FLOW                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              SEARCH SCREEN                           │   │
│  │  🔍 [Tìm nhà hàng, món ăn...]                      │   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  Recent: [Phở] [Pizza] [Cơm] [Xóa]                 │   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  Kết quả: 15 nhà hàng, 8 món                        │   │
│  │  Filter: [Danh mục ▼] [Giá ▼] [Đánh giá ▼]        │   │
│  │  Sort: [Gần nhất ▼]                                │   │
│  │                                                      │   │
│  │  ┌────────┐ ┌────────────────────────────────┐    │   │
│  │  │        │ │ Restaurant A - ⭐ 4.5          │    │   │
│  │  │  Img   │ │ 2km - Món chính, Đồ uống      │    │   │
│  │  │        │ └────────────────────────────────┘    │   │
│  │  └────────┘                                         │   │
│  │  ┌────────┐ ┌────────────────────────────────┐    │   │
│  │  │        │ │ Phở Bò - 50k - Restaurant B     │    │   │
│  │  │  Img   │ └────────────────────────────────┘    │   │
│  │  └────────┘                                         │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

#### 2.7.4 Data Flow
```
User types search query
       │
       ▼ (after 300ms debounce)
ViewModel.search(query)
       │
       ▼
Repository.search(query)
       │
       ├─▶ Supabase: restaurants?name.ilike.%query%
       │
       └─▶ Supabase: menu_items?name.ilike.%query%
       │
       ▼
Merge results ──▶ LiveData<SearchResults>
```

---

### MODULE 2.8: NOTIFICATIONS

#### 2.8.1 Module Description
Thông báo về đơn hàng và khuyến mãi.

#### 2.8.2 Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | Notification List | Danh sách thông báo |
| 2 | Order Notifications | Cập nhật trạng thái đơn |
| 3 | Promotion Notifications | Khuyến mãi mới |
| 4 | Mark as Read | Đánh dấu đã đọc |
| 5 | Badge Count | Số thông báo chưa đọc |

---

## 3. Common Components (Client)

### 3.1 Base Classes
| Class | Purpose |
|-------|---------|
| BaseActivity | Base cho tất cả Activity |
| BaseFragment | Base cho tất cả Fragment |
| BaseViewModel | Base cho tất cả ViewModel |
| LoadingDialog | Hiển thị loading overlay |

### 3.2 Navigation
- Sử dụng **BottomNavigationView** với 4 tabs:
  - Home (Trang chủ)
  - Search (Tìm kiếm)
  - Orders (Đơn hàng)
  - Profile (Tài khoản)

### 3.3 Cart Badge
- Hiển thị số món trong giỏ hàng trên BottomNavigation

---

## 4. Data Models (Client)

### 4.1 Cart Item (Local - Room)
```java
public class CartItem {
    private String id;
    private String menuItemId;
    private String name;
    private int price;
    private int quantity;
    private String note;
    private String restaurantId;
}
```

### 4.2 Order (Server)
```java
public class Order {
    private String id;
    private String orderCode;
    private String userId;
    private String restaurantId;
    private String deliveryAddress;
    private int subtotal;
    private int deliveryFee;
    private int discount;
    private int total;
    private String paymentMethod; // cod
    private String status;
    private String note;
    private Date createdAt;
}
```

---

## 5. Supabase Integration (Client)

### 5.1 Tables Used
- users, user_addresses
- restaurants, categories, menu_items
- orders, order_items, order_status_logs
- promotions, promotion_usages
- reviews, banners
- notifications

### 5.2 RPC Functions
- `rpc_create_order`: Tạo đơn hàng
- `rpc_apply_promotion`: Áp dụng khuyến mãi
- `rpc_cancel_order`: Hủy đơn (client)

### 5.3 Local Cart
- Sử dụng **Room Database** để lưu cart offline
- Sync với server khi checkout

---

## 6. Security

### 6.1 RLS Policies (Client)
| Table | Customer Access |
|-------|-----------------|
| users | Own profile only |
| user_addresses | Own addresses |
| restaurants | Read all |
| menu_items | Read available |
| orders | Own orders only |
| promotions | Read active |
| banners | Read active |

### 6.2 App-Level Security
- JWT token lưu trong EncryptedSharedPreferences
- Validate session on app resume

---

## 7. Appendix

### 7.1 Screen List (Client)
| Screen | Type | Description |
|--------|------|-------------|
| SplashActivity | Activity | Splash screen |
| LoginActivity | Activity | Login |
| RegisterActivity | Activity | Register |
| MainActivity | Activity | Main container + BottomNav |
| HomeFragment | Fragment | Home screen |
| RestaurantDetailActivity | Activity | Restaurant info + menu |
| CartFragment | Fragment | Cart view |
| CheckoutActivity | Activity | Checkout form |
| OrderSuccessActivity | Activity | Order success |
| OrderHistoryFragment | Fragment | Order list |
| OrderDetailActivity | Activity | Order detail |
| SearchFragment | Fragment | Search |
| ProfileFragment | Fragment | User profile |
| EditProfileActivity | Activity | Edit profile |
| AddressListActivity | Activity | Address list |
| AddressFormActivity | Activity | Add/edit address |
| NotificationFragment | Fragment | Notifications |

### 7.2 Color Scheme
| Token | Color | Usage |
|-------|-------|-------|
| colorPrimary | #1A237E | Header, primary buttons |
| colorAccent | #FF6B35 | CTA buttons, highlights |
| colorSuccess | #4CAF50 | Completed, available |
| colorWarning | #FFC107 | Pending, preparing |
| colorError | #F44336 | Error, cancelled |
| colorInfo | #2196F3 | Delivering, info |

---

*Document Version: 1.0*
*Created: 2026-04-04*
*Author: BA Team*