# Tổng quan - App Admin
## Food Ordering System

---

## 1. Thông tin dự án

| Item | Value |
|------|-------|
| Tên dự án | Food Ordering System |
| Tên app | FoodDelivery Admin |
| Nền tảng | Android Native |
| Ngôn ngữ | Java + XML |
| Backend | Supabase |
| Version | 1.0.0 |

---

## 2. User Roles

| Role | Description | Permissions |
|------|-------------|-------------|
| **Admin** | Quản trị viên hệ thống | Full access: all restaurants, all orders, all users, settings |
| **Staff** | Nhân viên nhà hàng | Limited: assigned restaurant only |

---

## 3. Module tổng quan

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ADMIN APP MODULES                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                         AUTH MODULE                                  │  │
│   │   • Login (email + password)                                        │  │
│   │   • Session management (JWT token)                                 │  │
│   │   • Role-based navigation                                          │  │
│   │   • Logout                                                          │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                       DASHBOARD MODULE                              │  │
│   │   • Stats cards: orders today, revenue today, processing          │  │
│   │   • New orders list (realtime)                                     │  │
│   │   • Top items (daily)                                              │  │
│   │   • Quick actions                                                  │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    ORDER MANAGEMENT MODULE                          │  │
│   │   • Order list with tabs (status filter)                           │  │
│   │   • Search by order code, customer name, phone                    │  │
│   │   • Order detail view                                              │  │
│   │   • Update status with validation (state machine)                 │  │
│   │   • Cancel order with reason                                       │  │
│   │   • Status timeline/history                                        │  │
│   │   • Realtime updates (new orders)                                  │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                  RESTAURANT MANAGEMENT MODULE                     │  │
│   │   • Restaurant list with toggle                                   │  │
│   │   • Add/Edit restaurant                                            │  │
│   │   • Upload image to Supabase Storage                               │  │
│   │   • Toggle active/open status                                      │  │
│   │   • Delivery fee, min order settings                               │  │
│   │   • Open/close time                                                │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    CATEGORY MANAGEMENT MODULE                      │  │
│   │   • Category list                                                  │  │
│   │   • Add/Edit category                                             │  │
│   │   • Upload image                                                   │  │
│   │   • Sort order                                                     │  │
│   │   • Delete category                                               │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    MENU ITEM MANAGEMENT MODULE                      │  │
│   │   • Menu list (filter by restaurant + category)                  │  │
│   │   • Add/Edit menu item                                            │  │
│   │   • Upload image                                                  │  │
│   │   • Toggle available (stock)                                       │  │
│   │   • Toggle featured                                               │  │
│   │   • Price, description                                             │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                     USER MANAGEMENT MODULE                           │  │
│   │   • Customer list                                                  │  │
│   │   • Search by name, email, phone                                   │  │
│   │   • View customer detail                                           │  │
│   │   • View customer order history                                    │  │
│   │   • Deactivate/activate account                                    │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                        REPORTS MODULE                               │  │
│   │   • Date range picker (today, week, month, custom)                │  │
│   │   • Revenue stats: total, orders, average/order                   │  │
│   │   • Order stats: completed, cancelled, rate                       │  │
│   │   • Top items (by quantity/revenue)                                │  │
│   │   • Top restaurants                                                │  │
│   │   • Chart (Phase 2 - MPAndroidChart)                              │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    NOTIFICATIONS MODULE                             │  │
│   │   • Notification list                                             │  │
│   │   • Filter by type (order, system)                                │  │
│   │   • Mark as read                                                  │  │
│   │   • Badge count on drawer                                         │  │
│   │   • Realtime updates                                              │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Sơ đồ luồng chính

### 4.1 Login Flow
```
User → Login Screen → Validate → Supabase Auth → 
  → Success: Save token → Navigate to Main
  → Error: Show message
```

### 4.2 Order Processing Flow
```
New Order (Realtime) → Order List → Tap Order → 
Order Detail → Update Status → RPC → 
  → Success: Update DB + Notify → Refresh List
```

### 4.3 Restaurant Management Flow
```
Restaurant List → Add/Edit Form → Validate → 
Supabase (PostgREST) → 
  → Success: Return to list
```

---

## 5. Navigation

- **NavigationDrawer** với menu theo role:
  - **Admin**: Dashboard, Orders, Restaurants, Categories, Menu, Users, Reports, Notifications, Settings
  - **Staff**: Dashboard, Orders, Menu (assigned restaurant only)

---

## 6. Data Flow tổng quan

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DATA FLOW - ADMIN APP                               │
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
│   Supabase (Database + Storage + Auth + Realtime)                          │
│          │                                                                  │
│          ├── PostgREST (CRUD operations)                                    │
│          ├── Storage (Images)                                              │
│          ├── Auth (JWT tokens)                                              │
│          ├── Realtime (WebSocket subscriptions)                             │
│          └── RPC Functions (Complex operations)                            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. RLS Policies

| Table | Admin | Staff | Customer |
|-------|-------|-------|----------|
| users | Full | Assigned restaurant | Own |
| user_addresses | Full | - | Own |
| restaurants | Full | Assigned | Read |
| menu_items | Full | Assigned restaurant | Read |
| orders | Full | Assigned restaurant | Own |
| order_items | Full | Assigned restaurant | Own |
| promotions | Full | Read | Read |
| reviews | Full | Read | Own |
| notifications | Full | Own | Own |
| admin_logs | Full | Read | - |

---

## 8. Edge Cases chính

| Module | Edge Case | Handling |
|--------|-----------|----------|
| Auth | Token expired | Auto refresh, fail则logout |
| Orders | Invalid status transition | Show error message |
| Orders | Concurrent update | Show "đang xử lý" |
| Restaurant | Delete with active orders | Warning dialog |
| Cart (Client) | Restaurant changed | Confirm dialog |
| Checkout | Min order not met | Error message |

---

## 9. Screen List

| Screen | Type | Module |
|--------|------|--------|
| AdminLoginActivity | Activity | Auth |
| AdminMainActivity | Activity | Container |
| DashboardFragment | Fragment | Dashboard |
| OrderListFragment | Fragment | Orders |
| OrderDetailActivity | Activity | Orders |
| RestaurantListFragment | Fragment | Restaurant |
| RestaurantFormActivity | Activity | Restaurant |
| CategoryListFragment | Fragment | Category |
| CategoryFormActivity | Activity | Category |
| MenuListFragment | Fragment | Menu |
| MenuItemFormActivity | Activity | Menu |
| UserListActivity | Activity | User |
| UserDetailActivity | Activity | User |
| ReportFragment | Fragment | Reports |
| NotificationFragment | Fragment | Notifications |

---

## 10. Dependencies

```
Phase 1: Foundation
├── Auth Module
├── Database Schema
├── RLS Policies
└── Project Setup

Phase 2: Order Flow
├── Order Management (Admin)
├── RPC Functions
├── Cart (Client - Week 5-6)
└── Checkout (Client - Week 6)

Phase 3: Engagement
├── Order History (Client)
├── Restaurant/Category/Menu CRUD (Admin)
├── Promotions
└── Reviews
```

---

*Document Version: 1.0*
*Created: 2026-04-04*
*Author: BA Team*