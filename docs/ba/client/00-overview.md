# Tổng quan - App Client
## Food Ordering System

---

## 1. Thông tin dự án

| Item | Value |
|------|-------|
| Tên dự án | Food Ordering System |
| Tên app | FoodDelivery Client |
| Nền tảng | Android Native |
| Ngôn ngữ | Java + XML |
| Backend | Supabase |
| Version | 1.0.0 |
| Người dùng | Khách hàng đặt đồ ăn |

---

## 2. User Role

| Role | Description |
|------|-------------|
| **Customer** | Người dùng đặt đồ ăn: browse restaurant, order, track, review |

---

## 3. Module tổng quan

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CLIENT APP MODULES                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                         AUTH MODULE                                  │  │
│   │   • Login (email + password)                                        │  │
│   │   • Register (email, phone, password)                               │  │
│   │   • Forgot password (email reset)                                  │  │
│   │   • Session management (JWT token)                                 │  │
│   │   • Logout                                                          │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                       HOME MODULE                                   │  │
│   │   • Banner carousel (ViewPager2)                                    │  │
│   │   • Categories horizontal scroll                                    │  │
│   │   • Featured restaurants (is_featured)                             │  │
│   │   • Near restaurants (by location)                                  │  │
│   │   • Search bar → Search screen                                     │  │
│   │   • Pull to refresh                                                 │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                   RESTAURANT DETAIL MODULE                          │  │
│   │   • Restaurant header (image, name, rating, address)               │  │
│   │   • Menu by category (TabLayout)                                   │  │
│   │   • Add to cart with quantity                                      │  │
│   │   • Search within restaurant                                        │  │
│   │   • Item note (optional)                                           │  │
│   │   • Featured badge                                                 │  │
│   │   • Cart floating button                                            │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    CART & CHECKOUT MODULE                           │  │
│   │   • Cart list (Room DB local)                                     │  │
│   │   • Update quantity                                                 │  │
│   │   • Remove item                                                    │  │
│   │   • Apply promotion code                                           │  │
│   │   • Select delivery address                                         │  │
│   │   • Add new address                                                │  │
│   │   • Order note                                                     │  │
│   │   • Place order (COD)                                             │  │
│   │   • Order success screen                                           │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    ORDER HISTORY MODULE                             │  │
│   │   • Order list (tabs: All, Processing, Completed, Cancelled)       │  │
│   │   • Order detail                                                   │  │
│   │   • Status timeline (visual)                                        │  │
│   │   • Cancel order (if pending/confirmed)                            │  │
│   │   • Reorder (add items to cart)                                     │  │
│   │   • Submit review (after delivered)                                │  │
│   │   • Realtime status updates                                        │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                       SEARCH MODULE                                 │  │
│   │   • Search bar (restaurants + menu items)                          │  │
│   │   • Recent searches                                                │  │
│   │   • Results (combine restaurants + menu items)                    │  │
│   │   • Filter by category                                              │  │
│   │   • Sort (default, rating, distance)                               │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                      PROFILE MODULE                                 │  │
│   │   • View profile (name, email, phone, avatar)                     │  │
│   │   • Edit profile                                                   │  │
│   │   • Address list                                                  │  │
│   │   • Add/Edit/Delete address                                       │  │
│   │   • Set default address                                           │  │
│   │   • Order history link                                            │  │
│   │   • Logout                                                         │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    NOTIFICATIONS MODULE                             │  │
│   │   • Notification list                                              │  │
│   │   • Order status notifications                                     │  │
│   │   • Promotion notifications                                        │  │
│   │   • Mark as read                                                  │  │
│   │   • Badge count                                                   │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Sơ đồ luồng chính

### 4.1 Order Flow
```
Home → Restaurant Detail → Add to Cart → Cart → 
Checkout → Select Address → Place Order → 
Order Success → Order History (track)
```

### 4.2 Cart Management
```
Restaurant Detail → Add Item → Check same restaurant → 
  → Yes: Add to Cart
  → No: Confirm Dialog → Clear + Add
```

### 4.3 Order Status Flow
```
pending → confirmed → preparing → delivering → delivered
    ↓
cancelled (from pending/confirmed)
```

---

## 5. Navigation

- **BottomNavigationView** với 4 tabs:
  1. **Home** (Trang chủ)
  2. **Search** (Tìm kiếm)
  3. **Orders** (Đơn hàng)
  4. **Profile** (Tài khoản)

---

## 6. Data Flow tổng quan

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DATA FLOW - CLIENT APP                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   UI Layer (Activity/Fragment)                                             │
│          │                                                                  │
│          ▼                                                                  │
│   ViewModel Layer (Business Logic, State Management)                        │
│          │                                                                  │
│          ▼                                                                  │
│   Repository Layer (Data transformation)                                   │
│          │                                                                  │
│          ▼                                                                  │
│   ┌────────────────────────────────────────────────────────────────────┐   │
│   │                    DATA SOURCES                                    │   │
│   │                                                                     │   │
│   │   ┌───────────────────────┐   ┌────────────────────────┐          │   │
│   │   │    SUPABASE          │   │    ROOM DATABASE       │          │   │
│   │   │                      │   │                        │          │   │
│   │   │  • PostgREST (CRUD)  │   │  • Cart items (local) │          │   │
│   │   │  • Auth (JWT)        │   │  • Cache              │          │   │
│   │   │  • Storage (Images)  │   │                        │          │   │
│   │   │  • Realtime          │   │                        │          │   │
│   │   │  • RPC (Orders)     │   │                        │          │   │
│   │   │                      │   │                        │          │   │
│   │   └───────────────────────┘   └────────────────────────┘          │   │
│   │                                                                     │   │
│   └────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Local Cart (Room DB)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         LOCAL CART STRUCTURE                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Table: cart_items                                                         │
│   ─────────────────                                                        │
│   Columns:                                                                  │
│   • id (UUID) - Primary Key                                               │
│   • menu_item_id (UUID) - Reference to menu_items                        │
│   • name (String) - Snapshot of item name                                 │
│   • price (int) - Snapshot of item price                                  │
│   • quantity (int) - User selected quantity                               │
│   • note (String) - Optional note for item                               │
│   • restaurant_id (UUID) - For cart validation                            │
│   • created_at (Timestamp) -                                              │
│                                                                             │
│   Logic:                                                                    │
│   • One cart per app (singleton)                                          │
│   • Items from one restaurant only                                         │
│   • On checkout: clear cart, create order on server                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. RLS Policies (Client)

| Table | Customer Access |
|-------|-----------------|
| users | Own profile only (read/write) |
| user_addresses | Own addresses (CRUD) |
| restaurants | Read all (is_active=true) |
| menu_items | Read available (is_available=true) |
| orders | Own orders only (read/write) |
| order_items | Own orders only (read) |
| promotions | Read active |
| reviews | Create own, read all |
| banners | Read active |
| notifications | Own only (read/write) |

---

## 9. Payment Method

| Method | Status | Notes |
|--------|--------|-------|
| **COD** (Tiền mặt) | ✅ MVP | Default and only method |
| Online Payment | ❌ Phase 2 | Not in MVP scope |

---

## 10. Screen List

| Screen | Type | Module | Tab |
|--------|------|--------|-----|
| SplashActivity | Activity | - | - |
| LoginActivity | Activity | Auth | - |
| RegisterActivity | Activity | Auth | - |
| ResetPasswordActivity | Activity | Auth | - |
| MainActivity | Activity | Container | - |
| HomeFragment | Fragment | Home | 0 |
| RestaurantDetailActivity | Activity | Restaurant Detail | - |
| SearchFragment | Fragment | Search | 1 |
| CartFragment | Fragment | Cart | - |
| CheckoutActivity | Activity | Checkout | - |
| OrderSuccessActivity | Activity | Checkout | - |
| OrderHistoryFragment | Fragment | Orders | 2 |
| OrderDetailActivity | Activity | Orders | - |
| ProfileFragment | Fragment | Profile | 3 |
| EditProfileActivity | Activity | Profile | - |
| AddressListActivity | Activity | Profile | - |
| AddressFormActivity | Activity | Profile | - |
| NotificationFragment | Fragment | Notifications | - |

---

## 11. Dependencies

```
Phase 1: Foundation
├── Auth Module
├── Database Schema
└── Project Setup

Phase 2: Order Flow
├── Home Screen
├── Restaurant Detail + Menu
├── Cart (Room DB local)
├── Checkout (COD)
├── Order Success
└── RPC: create_order

Phase 3: Engagement
├── Order History
├── Search
├── Profile
├── Reviews
└── Notifications
```

---

*Document Version: 1.0*
*Created: 2026-04-04*
*Author: BA Team*