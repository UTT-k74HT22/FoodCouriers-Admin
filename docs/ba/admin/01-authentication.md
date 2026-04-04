# Tài liệu BA - App Admin
## Hệ thống đặt đồ ăn

---

## 1. Project Overview

### 1.1 Project Information
| Item | Value |
|------|-------|
| Project Name | Food Ordering System |
| App Name | FoodDelivery Admin |
| Platform | Android Native |
| Language | Java + XML |
| Backend | Supabase (PostgreSQL + Auth + Storage + Realtime) |
| Version | 1.0.0 |
| Target Users | Admin và Staff quản lý nhà hàng |

### 1.2 Technology Stack
| Layer | Technology |
|-------|------------|
| Mobile | Android 8.0+ (API 26), Target API 34 |
| UI | Material Design 3, XML Layout |
| Architecture | MVVM + Repository Pattern |
| Local Storage | Room Database (cache) |
| Image Loading | Glide 4.16.0 |
| Backend | Supabase SDK 2.x |
| Network | Retrofit + OkHttp |

### 1.3 User Roles
| Role | Description | Permissions |
|------|-------------|--------------|
| **Admin** | Quản trị viên hệ thống | Full access: all restaurants, all orders, all users, settings |
| **Staff** | Nhân viên nhà hàng | Limited: assigned restaurant only |

---

## 2. Module Breakdown

### MODULE 2.1: AUTHENTICATION

#### 2.1.1 Module Description
Module xử lý đăng nhập, đăng xuất và quản lý phiên làm việc cho Admin/Staff.

#### 2.1.2 Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | Login | Đăng nhập bằng email + password |
| 2 | Session Management | Lưu JWT token, tự động refresh |
| 3 | Role Check | Kiểm tra role để hiển thị menu phù hợp |
| 4 | Logout | Đăng xuất, xóa token |

#### 2.1.3 User Flow
```
┌─────────────────────────────────────────────────────────────┐
│                    LOGIN FLOW                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌──────────┐     ┌──────────────┐     ┌──────────────┐   │
│   │  Login   │ ──▶ │  Validate    │ ──▶ │  Check Role  │   │
│   │  Screen  │     │  Credentials │     │  in DB        │   │
│   └──────────┘     └──────────────┘     └──────────────┘   │
│                                                 │            │
│                                                 ▼            │
│                              ┌────────────────────────────┐  │
│                              │         SUCCESS            │  │
│                              │  ┌─────────┐ ┌───────────┐  │  │
│                              │  │ Admin   │ │  Staff    │  │  │
│                              │  │Dashboard│ │ Dashboard │  │  │
│                              │  └─────────┘ └───────────┘  │  │
│                              └────────────────────────────┘  │
│                                                 │            │
│                                                 ▼            │
│                              ┌────────────────────────────┐  │
│                              │         FAILED             │  │
│                              │  Display error message     │  │
│                              └────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

#### 2.1.4 UI Components
| Screen | Component | Files |
|--------|-----------|-------|
| Login | AdminLoginActivity | activity_admin_login.xml |
| | EditText email | |
| | EditText password | |
| | Button login | |
| | ProgressBar loading | |

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
  │          (Result)           (Token + Role)       (Return)
  │               │                   │                  │
  └───────────────▶│───────────────────▶│──────────────────▶│
                  Result              Save                  
```

#### 2.1.6 Edge Cases
| Case | Handling |
|------|----------|
| Wrong password | Show error "Sai mật khẩu" |
| Account not exist | Show error "Tài khoản không tồn tại" |
| Account disabled | Show error "Tài khoản đã bị vô hiệu hóa" |
| Network error | Show "Không có kết nối mạng" |
| Token expired | Auto refresh, nếu fail thì logout |
| Not admin/staff role | Redirect về login |

---

### MODULE 2.2: DASHBOARD

#### 2.2.1 Module Description
Màn hình tổng quan hiển thị số liệu thống kê và đơn hàng mới.

#### 2.2.2 Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | Stats Cards | Hiển thị: Tổng đơn hôm nay, Doanh thu hôm nay, Đơn đang xử lý |
| 2 | New Orders List | Danh sách đơn hàng mới cần xử lý (realtime) |
| 3 | Top Items | Top 5 món ăn bán chạy |
| 4 | Quick Actions | Buttons: Xem đơn mới, Xem báo cáo |

#### 2.2.3 User Flow
```
┌─────────────────────────────────────────────────────────────┐
│                   DASHBOARD FLOW                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    DASHBOARD                         │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐    │   │
│  │  │ Orders      │ │ Revenue     │ │ Processing  │    │   │
│  │  │ Today: 25   │ │ Today: 2.5M │ │ 5 orders    │    │   │
│  │  └─────────────┘ └─────────────┘ └─────────────┘    │   │
│  │                                                      │   │
│  │  New Orders (Realtime)                               │   │
│  │  ┌─────────────────────────────────────────────┐   │   │
│  │  │ ORD001 - Restaurant A - 150k - Pending      │   │   │
│  │  │ ORD002 - Restaurant B - 200k - Confirmed    │   │   │
│  │  │ ...                                           │   │   │
│  │  └─────────────────────────────────────────────┘   │   │
│  │                                                      │   │
│  │  Top Items                                          │   │
│  │  ┌─────────────────────────────────────────────┐   │   │
│  │  │ 1. Phở Bò - 150 orders                       │   │   │
│  │  │ 2. Cơm Rang - 120 orders                     │   │   │
│  │  └─────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                  │
│                           ▼                                  │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐    │
│  │ Tap Order    │   │ Tap Revenue  │   │ Tap Top Items│    │
│  │ ────────────▶│   │ ────────────▶│   │ ────────────▶│
│  │ Order List   │   │ Report       │   │ Menu Item    │
│  └──────────────┘   └──────────────┘   └──────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

#### 2.2.4 UI Components
| Screen | Component | Files |
|--------|-----------|-------|
| Dashboard | DashboardFragment | fragment_dashboard.xml |
| | CardView stats (3 cards) | |
| | RecyclerView new orders | |
| | RecyclerView top items | |
| | SwipeRefreshLayout | |

#### 2.2.5 Data Flow
```
UI Layer          ViewModel          Repository         Supabase
  │                    │                    │                  │
  ▼                    ▼                    ▼                  ▼
Dashboard ───▶ DashboardViewModel ─▶ ReportRepository ──▶ RPC
  Load               │                   │              v_daily_stats
  │                  ▼                   ▼                  │
  │            LiveData<Dashboard>     Query               │
  │                  │                   │                  │
  └──────────────────▶│──────────────────▶│──────────────────│
                   Display             Result              
```

#### 2.2.6 Edge Cases
| Case | Handling |
|------|----------|
| No orders today | Show "Không có đơn hàng nào" |
| Network error | Show cached data + error banner |
| Empty stats | Show "Chưa có dữ liệu" |

---

### MODULE 2.3: ORDER MANAGEMENT

#### 2.3.1 Module Description
Quản lý toàn bộ đơn hàng: xem danh sách, chi tiết, cập nhật trạng thái.

#### 2.3.2 Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | Order List | Danh sách đơn hàng với tab lọc theo trạng thái |
| 2 | Search/Filter | Tìm theo mã đơn, tên khách, số điện thoại |
| 3 | Order Detail | Xem chi tiết đơn: khách, món, địa chỉ, tổng tiền |
| 4 | Update Status | Cập nhật trạng thái đơn (có validate state machine) |
| 5 | Cancel Order | Hủy đơn với lý do |
| 6 | Status Timeline | Hiển thị lịch sử thay đổi trạng thái |
| 7 | Realtime Updates | Nhận đơn mới qua Supabase Realtime |

#### 2.3.3 Order Status Flow
```
┌─────────────────────────────────────────────────────────────────┐
│                    ORDER STATUS MACHINE                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│    ┌─────────┐     ┌───────────┐     ┌───────────┐            │
│    │pending │────▶│ confirmed │────▶│preparing │            │
│    │ (chờ)  │     │ (xác nhận)│     │(đang nấu)│            │
│    └────┬────┘     └─────┬─────┘     └─────┬─────┘            │
│         │                │                │                    │
│         │                │                │                    │
│         │                │                ▼                    │
│         │                │          ┌───────────┐              │
│         │                │          │delivering │              │
│         │                │          │(đang giao)│              │
│         │                │          └─────┬─────┘              │
│         │                │                │                    │
│         ▼                ▼                ▼                    │
│    ┌──────────────────────────────────────────┐               │
│    │           CANCELLED (hủy)                │               │
│    │   (từ pending hoặc confirmed)           │               │
│    └──────────────────────────────────────────┘               │
│                                                  │              │
│                                                  ▼              │
│                                           ┌──────────┐        │
│                                           │delivered │        │
│                                           │(hoàn thành)│       │
│                                           └───────────┘        │
└─────────────────────────────────────────────────────────────────┘
```

#### 2.3.4 User Flow
```
┌─────────────────────────────────────────────────────────────┐
│                  ORDER LIST FLOW                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              ORDER LIST SCREEN                       │   │
│  │  ┌─────────────────────────────────────────────────┐│   │
│  │  │ TabLayout: All | Pending | Confirmed | ...     ││   │
│  │  ├─────────────────────────────────────────────────┤│   │
│  │  │ ┌───────────────────────────────────────────┐   ││   │
│  │  │ │ Order: ORD001 - Nguyễn Văn A - 150k      │   ││   │
│  │  │ │ Status: Pending - 10:30 AM                │   ││   │
│  │  │ └───────────────────────────────────────────┘   ││   │
│  │  │ ┌───────────────────────────────────────────┐   ││   │
│  │  │ │ Order: ORD002 - Trần Thị B - 200k        │   ││   │
│  │  │ │ Status: Confirmed - 10:15 AM             │   ││   │
│  │  │ └───────────────────────────────────────────┘   ││   │
│  │  └─────────────────────────────────────────────────┘│   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                  │
│                           ▼                                  │
│  ┌──────────────────┐    ┌──────────────────┐                │
│  │ Tap Order Item  │    │ Pull to Refresh │                │
│  └────────┬─────────┘    └────────┬─────────┘                │
│           │                        │                           │
│           ▼                        │                           │
│  ┌─────────────────────────────────┴───────────────────────┐ │
│  │              ORDER DETAIL SCREEN                        │ │
│  │  Order Code: ORD001                                     │ │
│  │  Customer: Nguyễn Văn A - 0901234567                   │ │
│  │  Address: 123 Nguyễn Trãi, Q1, HCM                    │ │
│  │  ─────────────────────────────────────────────         │ │
│  │  Items:                                                 │ │
│  │  - Phở Bò (x2) - 60k                                   │ │
│  │  - Trà đá (x2) - 10k                                   │ │
│  │  ─────────────────────────────────────────────         │ │
│  │  Subtotal: 70k                                         │ │
│  │  Delivery: 10k                                         │ │
│  │  Total: 80k                                            │ │
│  │  ─────────────────────────────────────────────         │ │
│  │  Note: Gọi trước khi giao                             │ │
│  │  ─────────────────────────────────────────────         │ │
│  │  [Confirm] [Cancel]                    [Status Log]  │ │
│  └─────────────────────────────────────────────────────────┘ │
│                           │                                  │
│                           ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │           STATUS UPDATE DIALOG                          │ │
│  │  Current: Pending                                      │ │
│  │  ┌─────────────────────────────────────────────────┐   │ │
│  │  │ Select new status:                             │   │ │
│  │  │ ○ Confirmed                                    │   │ │
│  │  │ ○ Cancel                                       │   │ │
│  │  └─────────────────────────────────────────────────┘   │ │
│  │  Reason (optional): [_________________________]       │ │
│  │  [Cancel]                          [Confirm]            │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

#### 2.3.5 UI Components
| Screen | Component | Files |
|--------|-----------|-------|
| Order List | OrderListFragment | fragment_order_list.xml |
| | TabLayout (6 tabs) | |
| | RecyclerView orders | |
| | SearchView | |
| | SwipeRefreshLayout | |
| Order Detail | OrderDetailActivity | activity_order_detail.xml |
| | TextView order info | |
| | RecyclerView items | |
| | Button confirm/cancel | |
| | Timeline view | |

#### 2.3.6 Data Flow
```
Order List:
UI Layer          ViewModel          Repository         Supabase
  │                    │                    │                  │
Load ─────────▶ OrderViewModel ───▶ OrderRepository ───▶ orders
  │               │                   │              (query + RLS)
  │               ▼                   ▼                  │
  │          LiveData<List<Order>>   Subscribe          │
  │               │                   │                  │
  └───────────────▶│──────────────────▶│──────────────────│
                Display              Realtime            

Order Status Update:
UI Layer          ViewModel          Repository         Supabase
  │                    │                    │                  │
  ▼                    ▼                    ▼                  ▼
Update ───▶ OrderViewModel ───▶ OrderRepository ──▶ RPC
Status     Validate        rpc_update_      update_order
  │       State Machine    OrderStatus       Status
  │               │                   │                  │
  │               ▼                   ▼                  │
  │          LiveData<Result>   Success/Fail           │
  │               │                   │                  │
  └───────────────▶│──────────────────▶│──────────────────▶
                Display              Refresh List        
```

#### 2.3.7 Edge Cases
| Case | Handling |
|------|----------|
| Invalid status transition | Show error "Không thể chuyển sang trạng thái này" |
| Network error | Show error, giữ nguyên trạng thái |
| Order already cancelled | Disable all status buttons |
| Empty order list | Show "Không có đơn hàng" |

---

### MODULE 2.4: RESTAURANT MANAGEMENT

#### 2.4.1 Module Description
CRUD thông tin nhà hàng: tạo, sửa, xóa, bật/tắt trạng thái.

#### 2.4.2 Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | Restaurant List | Danh sách tất cả nhà hàng với toggle active |
| 2 | Add Restaurant | Thêm nhà hàng mới |
| 3 | Edit Restaurant | Sửa thông tin nhà hàng |
| 4 | Delete Restaurant | Xóa nhà hàng (soft delete) |
| 5 | Toggle Status | Bật/tắt is_active, is_open |
| 6 | Upload Image | Upload ảnh nhà hàng lên Supabase Storage |

#### 2.4.3 User Flow
```
┌─────────────────────────────────────────────────────────────┐
│               RESTAURANT MANAGEMENT FLOW                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              RESTAURANT LIST SCREEN                  │   │
│  │  ┌─────────────────────────────────────────────────┐│   │
│  │  │ [+] Add Restaurant (FAB)                        ││   │
│  │  ├─────────────────────────────────────────────────┤│   │
│  │  │ ┌────────┐ ┌────────────────────────────────┐  ││   │
│  │  │ │  Img   │ │ Restaurant A                   │  ││   │
│  │  │ │        │ │ 123 Main St - Open - Active   │  ││   │
│  │  │ └────────┘ └────────────────────────────────┘  ││   │
│  │  │ ┌────────┐ ┌────────────────────────────────┐  ││   │
│  │  │ │  Img   │ │ Restaurant B                   │  ││   │
│  │  │ │        │ │ 456 Oak St - Closed            │  ││   │
│  │  │ └────────┘ └────────────────────────────────┘  ││   │
│  │  └─────────────────────────────────────────────────┘│   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                  │
│         ┌─────────────────┼─────────────────┐               │
│         ▼                 ▼                 ▼               │
│  ┌────────────┐   ┌────────────┐   ┌────────────┐          │
│  │  Tap FAB   │   │ Tap Item   │   │  Toggle    │          │
│  │ ──────────▶│   │ ──────────▶│   │ ──────────▶│          │
│  │ Add Form   │   │ Edit Form  │   │ Update     │          │
│  └────────────┘   └────────────┘   └────────────┘          │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              RESTAURANT FORM SCREEN                  │   │
│  │  ┌─────────────────────────────────────────────────┐│   │
│  │  │ [Image Picker - Tap to upload]                 ││   │
│  │  └─────────────────────────────────────────────────┘│   │
│  │  Name: [________________________]                  │   │
│  │  Address: [________________________]               │   │
│  │  Phone: [________________________]                │   │
│  │  Open Time: [08:00] - Close Time: [22:00]         │   │
│  │  Delivery Fee: [________] VND                      │   │
│  │  Min Order: [________] VND                         │   │
│  │  [x] Active  [ ] Open                              │   │
│  │                                                      │   │
│  │  [Cancel]                        [Save]            │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

#### 2.4.4 UI Components
| Screen | Component | Files |
|--------|-----------|-------|
| Restaurant List | RestaurantListFragment | fragment_restaurant_list.xml |
| | RecyclerView | |
| | FAB add | |
| | Toggle switch | |
| Restaurant Form | RestaurantFormActivity | activity_restaurant_form.xml |
| | ImageView picker | |
| | EditText fields | |
| | TimePicker | |
| | Switch active/open | |

#### 2.4.5 Data Flow
```
UI Layer          ViewModel          Repository         Supabase
  │                    │                    │                  │
Load ─────────▶ RestaurantVM ───▶ RestaurantRepo ──▶ restaurants
  │               │                   │              (PostgREST)
  │               ▼                   ▼                  │
  │          LiveData<List<Restaurant>>   CRUD          │
  │               │                   │                  │
  └───────────────▶│──────────────────▶│──────────────────▶
                Display              Result              

Upload Image:
UI Layer          ViewModel          Repository         Supabase
  │                    │                    │                  │
Select ─────────▶ ImageUtils ───▶ RestaurantRepo ──▶ Storage
Image             compress           upload             restaurants/
  │               │                   │                  │
  │               ▼                   ▼                  │
  │          LiveData<String>    image URL             │
  │               │                   │                  │
  └───────────────▶│──────────────────▶│──────────────────▶
                Display              Save to Form        
```

#### 2.4.6 Edge Cases
| Case | Handling |
|------|----------|
| Image upload fail | Show error, cho phép nhập URL thủ công |
| Name already exists | Show error "Tên nhà hàng đã tồn tại" |
| Delete with active orders | Show warning "Nhà hàng có đơn hàng, xác nhận xóa?" |
| Network error | Show error, retry option |

---

### MODULE 2.5: CATEGORY MANAGEMENT

#### 2.5.1 Module Description
CRUD danh mục món ăn (Cơm, Phở, Pizza, Đồ uống...).

#### 2.5.2 Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | Category List | Danh sách danh mục với thứ tự sắp xếp |
| 2 | Add Category | Thêm danh mục mới |
| 3 | Edit Category | Sửa tên, ảnh, thứ tự |
| 4 | Delete Category | Xóa danh mục |
| 5 | Reorder | Kéo thả sắp xếp thứ tự |

#### 2.5.3 User Flow
```
┌─────────────────────────────────────────────────────────────┐
│                CATEGORY MANAGEMENT FLOW                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              CATEGORY LIST SCREEN                    │   │
│  │  ┌─────────────────────────────────────────────────┐│   │
│  │  │ [+] Add Category (FAB)                          ││   │
│  │  ├─────────────────────────────────────────────────┤│   │
│  │  │ ┌────┐ ┌──────────────────────┐ ┌──────────┐   ││   │
│  │  │ │Img │ │ 1. Món chính         │ │ Edit/Del │   ││   │
│  │  │ └────┘ └──────────────────────┘ └──────────┘   ││   │
│  │  │ ┌────┐ ┌──────────────────────┐ ┌──────────┐   ││   │
│  │  │ │Img │ │ 2. Món phụ           │ │ Edit/Del │   ││   │
│  │  │ └────┘ └──────────────────────┘ └──────────┘   ││   │
│  │  └─────────────────────────────────────────────────┘│   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              CATEGORY FORM SCREEN                    │   │
│  │  ┌─────────────────────────────────────────────────┐│   │
│  │  │ [Image Picker - Tap to upload]                 ││   │
│  │  └─────────────────────────────────────────────────┘│   │
│  │  Name: [________________________]                  │   │
│  │  Sort Order: [1]                                    │   │
│  │  [x] Active                                        │   │
│  │                                                      │   │
│  │  [Cancel]                        [Save]            │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

#### 2.5.4 UI Components
| Screen | Component | Files |
|--------|-----------|-------|
| Category List | CategoryListFragment | fragment_category_list.xml |
| | RecyclerView + drag | |
| Category Form | CategoryFormActivity | activity_category_form.xml |

---

### MODULE 2.6: MENU ITEM MANAGEMENT

#### 2.6.1 Module Description
CRUD món ăn trong thực đơn nhà hàng.

#### 2.6.2 Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | Menu List | Danh sách món ăn theo nhà hàng, lọc theo danh mục |
| 2 | Add Menu Item | Thêm món mới |
| 3 | Edit Menu Item | Sửa thông tin món |
| 4 | Delete Menu Item | Xóa món |
| 5 | Toggle Available | Bật/tắt is_available (hết món) |
| 6 | Toggle Featured | Đánh dấu món nổi bật |
| 7 | Upload Image | Upload ảnh món ăn |

#### 2.6.3 User Flow
```
┌─────────────────────────────────────────────────────────────┐
│                  MENU ITEM MANAGEMENT FLOW                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              MENU LIST SCREEN                        │   │
│  │  Restaurant: [Dropdown: Select Restaurant]          │   │
│  │  Category: [Dropdown: All Categories]              │   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  ┌─────────────────────────────────────────────────┐│   │
│  │  │ [+] Add Menu Item (FAB)                         ││   │
│  │  ├─────────────────────────────────────────────────┤│   │
│  │  │ ┌────┐ ┌────────────────┐ ┌──────┐ ┌────────┐ │   │
│  │  │ │Img │ │ Phở Bò - 50k   │ │ ★    │ │ On/Off │ │   │
│  │  │ │    │ │ Món chính      │ │      │ │        │ │   │
│  │  │ └────┘ └────────────────┘ └──────┘ └────────┘ │   │
│  │  │ ┌────┐ ┌────────────────┐ ┌──────┐ ┌────────┐ │   │
│  │  │ │Img │ │ Cơm Rang - 35k │ │      │ │ On/Off │ │   │
│  │  │ │    │ │ Món chính      │ │      │ │        │ │   │
│  │  │ └────┘ └────────────────┘ └──────┘ └────────┘ │   │
│  │  └─────────────────────────────────────────────────┘│   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              MENU ITEM FORM SCREEN                   │   │
│  │  ┌─────────────────────────────────────────────────┐│   │
│  │  │ [Image Picker - Tap to upload]                 ││   │
│  │  └─────────────────────────────────────────────────┘│   │
│  │  Name: [________________________]                  │   │
│  │  Description: [________________________]          │   │
│  │  Category: [Dropdown: Select Category]             │   │
│  │  Price (VND): [________________________]          │   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  [x] Available  [ ] Featured                       │   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  [Cancel]                        [Save]            │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

#### 2.6.4 UI Components
| Screen | Component | Files |
|--------|-----------|-------|
| Menu List | MenuListFragment | fragment_menu_list.xml |
| | Spinner restaurant | |
| | Spinner category | |
| | RecyclerView items | |
| | Switch available | |
| Menu Item Form | MenuItemFormActivity | activity_menu_item_form.xml |

---

### MODULE 2.7: USER MANAGEMENT

#### 2.7.1 Module Description
Quản lý tài khoản khách hàng: xem, tìm kiếm, vô hiệu hóa.

#### 2.7.2 Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | User List | Danh sách tất cả khách hàng |
| 2 | Search | Tìm theo tên, email, số điện thoại |
| 3 | User Detail | Xem thông tin và lịch sử đơn hàng |
| 4 | Deactivate | Vô hiệu hóa/kích hoạt tài khoản |

#### 2.7.3 User Flow
```
┌─────────────────────────────────────────────────────────────┐
│                  USER MANAGEMENT FLOW                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              USER LIST SCREEN                        │   │
│  │  Search: [________________________]                  │   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  ┌─────────────────────────────────────────────────┐│   │
│  │  │ Avatar │ Name - Email              │ Active    ││   │
│  │  │        │ Phone                    │ [Toggle]  ││   │
│  │  └─────────────────────────────────────────────────┘│   │
│  │  ┌─────────────────────────────────────────────────┐│   │
│  │  │ Avatar │ Name - Email              │ Active    ││   │
│  │  │        │ Phone                    │ [Toggle]  ││   │
│  │  └─────────────────────────────────────────────────┘│   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                  │
│                           ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              USER DETAIL SCREEN                      │   │
│  │  Avatar: [____]   Name: Nguyễn Văn A                │   │
│  │  Email: a@example.com                                │   │
│  │  Phone: 0901234567                                   │   │
│  │  Status: Active                                      │   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  Order History:                                       │   │
│  │  ┌─────────────────────────────────────────────────┐│   │
│  │  │ ORD001 - 150k - Delivered - 2024-01-15         ││   │
│  │  │ ORD002 - 200k - Cancelled - 2024-01-10         ││   │
│  │  └─────────────────────────────────────────────────┘│   │
│  │                                                      │   │
│  │  [Deactivate Account]                               │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

#### 2.7.4 UI Components
| Screen | Component | Files |
|--------|-----------|-------|
| User List | UserListActivity | activity_user_list.xml |
| | SearchView | |
| | RecyclerView | |
| User Detail | UserDetailActivity | activity_user_detail.xml |
| | RecyclerView orders | |

---

### MODULE 2.8: REPORTS

#### 2.8.1 Module Description
Báo cáo doanh thu và thống kê theo thời gian.

#### 2.8.2 Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | Date Range Picker | Chọn: Hôm nay, Tuần, Tháng, Tùy chọn |
| 2 | Revenue Stats | Tổng doanh thu, số đơn, trung bình/đơn |
| 3 | Order Stats | Tổng đơn, đơn hoàn thành, đơn hủy, tỉ lệ hủy |
| 4 | Top Items | Top 5 món bán chạy |
| 5 | Chart (Phase 2) | Biểu đồ doanh thu theo ngày |

#### 2.8.3 User Flow
```
┌─────────────────────────────────────────────────────────────┐
│                    REPORTS FLOW                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              REPORTS SCREEN                         │   │
│  │  Date Range: [Hôm nay ▼] [From] - [To]             │   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  ┌─────────────────────────────────────────────────┐│   │
│  │  │            REVENUE SUMMARY                      ││   │
│  │  │  Total Revenue: 15,000,000 VND                  ││   │
│  │  │  Total Orders: 150                              ││   │
│  │  │  Average/Order: 100,000 VND                     ││   │
│  │  └─────────────────────────────────────────────────┘│   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  ┌─────────────────────────────────────────────────┐│   │
│  │  │            ORDER STATS                          ││   │
│  │  │  Completed: 130 (87%)                          ││   │
│  │  │  Cancelled: 20 (13%)                          ││   │
│  │  └─────────────────────────────────────────────────┘│   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  ┌─────────────────────────────────────────────────┐│   │
│  │  │            TOP ITEMS                            ││   │
│  │  │  1. Phở Bò - 150 orders - 7,500,000 VND        ││   │
│  │  │  2. Cơm Rang - 120 orders - 4,200,000 VND      ││   │
│  │  │  3. Bún Chả - 100 orders - 3,000,000 VND       ││   │
│  │  └─────────────────────────────────────────────────┘│   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

#### 2.8.4 UI Components
| Screen | Component | Files |
|--------|-----------|-------|
| Reports | ReportFragment | fragment_report.xml |
| | Spinner date range | |
| | CardView stats | |
| | RecyclerView top items | |

---

### MODULE 2.9: NOTIFICATIONS

#### 2.9.1 Module Description
Quản lý thông báo trong app cho Admin/Staff.

#### 2.9.2 Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | Notification List | Danh sách tất cả thông báo |
| 2 | Filter | Lọc theo loại (order, system) |
| 3 | Mark as Read | Đánh dấu đã đọc |
| 4 | Badge Count | Hiển thị số thông báo chưa đọc trên menu |

#### 2.9.3 User Flow
```
┌─────────────────────────────────────────────────────────────┐
│                 NOTIFICATIONS FLOW                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           NOTIFICATION LIST SCREEN                  │   │
│  │  Filter: [All ▼]                                   │   │
│  │  ─────────────────────────────────────────────────   │   │
│  │  ┌─────────────────────────────────────────────────┐│   │
│  │  │ 🔔 New Order - ORD001 - 2 min ago             ││   │
│  │  │    Restaurant A - 150k                         ││   │
│  │  ├─────────────────────────────────────────────────┤│   │
│  │  │ 🔔 Order Delivered - ORD099 - 1 hour ago       ││   │
│  │  │    Customer: Nguyễn Văn A                      ││   │
│  │  ├─────────────────────────────────────────────────┤│   │
│  │  │ 📢 System - New promotion available            ││   │
│  │  └─────────────────────────────────────────────────┘│   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

### MODULE 2.10: SETTINGS (ADMIN ONLY)

#### 2.10.1 Module Description
Cấu hình hệ thống và quản lý Staff.

#### 2.10.2 Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | Staff Management | Tạo/tắt tài khoản Staff |
| 2 | Assign Restaurant | Gán nhà hàng cho Staff |
| 3 | App Settings | Cấu hình app (thông báo, theme) |

---

## 3. Common Components

### 3.1 Base Classes
| Class | Purpose |
|-------|---------|
| BaseActivity | Base cho tất cả Activity |
| BaseFragment | Base cho tất cả Fragment |
| BaseViewModel | Base cho tất cả ViewModel |
| LoadingDialog | Hiển thị loading overlay |
| ConfirmDialog | Dialog xác nhận thao tác quan trọng |

### 3.2 Navigation
- Sử dụng **NavigationDrawer** cho menu chính
- Menu items hiển thị theo role:
  - **Admin**: Dashboard, Orders, Restaurants, Categories, Menu, Users, Reports, Notifications, Settings
  - **Staff**: Dashboard, Orders, Menu (assigned restaurant only)

### 3.3 Error Handling
- Network errors: Show Snackbar với retry option
- Validation errors: Show inline error dưới field
- Server errors: Show dialog với message từ server
- Timeout: Show "Thao tác quá thời gian, vui lòng thử lại"

---

## 4. Data Models

### 4.1 Key Entities
```
User {
  id: UUID
  auth_id: UUID
  full_name: String
  phone: String
  email: String
  role: Enum (admin, staff, customer)
  is_active: Boolean
}

Restaurant {
  id: UUID
  name: String
  address: String
  phone: String
  image_url: String
  rating: Decimal
  is_active: Boolean
  is_open: Boolean
  delivery_fee: Integer
  min_order: Integer
}

Order {
  id: UUID
  order_code: String (unique)
  user_id: UUID
  restaurant_id: UUID
  delivery_address: String
  subtotal: Integer
  delivery_fee: Integer
  discount: Integer
  total: Integer
  payment_method: Enum (cod, online)
  status: Enum (pending, confirmed, preparing, delivering, delivered, cancelled)
}

MenuItem {
  id: UUID
  restaurant_id: UUID
  category_id: UUID
  name: String
  description: String
  price: Integer
  image_url: String
  is_available: Boolean
  is_featured: Boolean
}
```

---

## 5. Supabase Integration

### 5.1 Tables Used
- users, user_addresses
- restaurants, restaurant_staff
- categories
- menu_items
- orders, order_items, order_status_logs
- promotions, promotion_usages
- reviews
- banners
- notifications
- admin_logs

### 5.2 RPC Functions
- `rpc_create_order`: Tạo đơn hàng
- `rpc_update_order_status`: Cập nhật trạng thái
- `rpc_apply_promotion`: Áp dụng khuyến mãi

### 5.3 Realtime Channels
- Orders: Subscribe để nhận đơn mới
- Notifications: Subscribe để nhận thông báo

---

## 6. Security

### 6.1 RLS Policies
| Table | Admin | Staff | Customer |
|-------|-------|-------|----------|
| users | Full | Assigned restaurant | Own |
| restaurants | Full | Assigned | Read |
| orders | Full | Assigned restaurant | Own |
| menu_items | Full | Assigned restaurant | Read |
| categories | Full | Read | Read |
| promotions | Full | Read | Read |

### 6.2 App-Level Security
- JWT token lưu trong EncryptedSharedPreferences
- Role check tại mỗi màn hình
- Menu ẩn cho Staff không được phép

---

## 7. Appendix

### 7.1 Screen List
| Screen | Type | Description |
|--------|------|-------------|
| AdminLoginActivity | Activity | Login screen |
| AdminMainActivity | Activity | Main container + NavigationDrawer |
| DashboardFragment | Fragment | Dashboard với stats |
| OrderListFragment | Fragment | Danh sách đơn hàng |
| OrderDetailActivity | Activity | Chi tiết đơn hàng |
| RestaurantListFragment | Fragment | Danh sách nhà hàng |
| RestaurantFormActivity | Activity | Form thêm/sửa nhà hàng |
| CategoryListFragment | Fragment | Danh sách danh mục |
| CategoryFormActivity | Activity | Form thêm/sửa danh mục |
| MenuListFragment | Fragment | Danh sách món ăn |
| MenuItemFormActivity | Activity | Form thêm/sửa món ăn |
| UserListActivity | Activity | Danh sách khách hàng |
| UserDetailActivity | Activity | Chi tiết khách hàng |
| ReportFragment | Fragment | Báo cáo doanh thu |
| NotificationFragment | Fragment | Danh sách thông báo |

### 7.2 Color Scheme
| Token | Color | Usage |
|-------|-------|-------|
| colorPrimary | #1A237E | Header, Navigation |
| colorAccent | #FF6B35 | Highlight, buttons |
| colorSuccess | #4CAF50 | Completed, active |
| colorWarning | #FFC107 | Pending |
| colorError | #F44336 | Cancelled, error |
| colorInfo | #2196F3 | Delivering |

---

*Document Version: 1.0*
*Created: 2026-04-04*
*Author: BA Team*