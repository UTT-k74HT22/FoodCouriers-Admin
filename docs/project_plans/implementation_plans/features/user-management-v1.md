---
title: "User Management - Implementation Plan"
description: "Chi tiết các tasks để implement module quản lý người dùng"
audience: [ai-agents, developers]
tags: [implementation, admin, user]
created: 2026-04-07
---

# User Management - Implementation Plan

## Executive Summary

Implement module quản lý người dùng cho Admin app theo pattern của Category module. Bao gồm User List screen, User Detail screen, và toggle user status.

## Phase 1: Data Layer (Model + Repository)

### Tasks

| # | Task | Module | Type | Estimate |
|---|------|--------|------|----------|
| 1.1 | Tạo Order model | Client | model | 0.5d |
| 1.2 | Tạo UserUpsertRequest DTO | Client | model | 0.5d |
| 1.3 | Tạo UserRepository | Client | repository | 1d |

### Subtasks

- [ ] 1.3.1 Tạo `UserRepository.java` extends `BaseSupabaseRepository`
- [ ] 1.3.2 Implement `getUsers(searchQuery, isActive, limit, offset)`
- [ ] 1.3.3 Implement `getUserById(id)`
- [ ] 1.3.4 Implement `getUserOrders(userId, limit, offset)`
- [ ] 1.3.5 Implement `updateUserStatus(id, isActive)`

**File locations:**
- Model: `app/src/main/java/com/utt/foodcouriers_admin/data/model/Order.java`
- DTO: `app/src/main/java/com/utt/foodcouriers_admin/data/request/UserUpsertRequest.java`
- Repository: `app/src/main/java/com/utt/foodcouriers_admin/data/repository/UserRepository.java`

## Phase 2: UI Layer (Fragments + Activities)

### Tasks

| # | Task | Module | Type | Estimate |
|---|------|--------|------|----------|
| 2.1 | Tạo layout user_list.xml | Client | layout | 0.5d |
| 2.2 | Tạo layout item_user.xml | Client | layout | 0.5d |
| 2.3 | Tạo layout user_detail.xml | Client | layout | 0.5d |
| 2.4 | Tạo layout item_order.xml | Client | layout | 0.5d |
| 2.5 | Tạo UserAdapter | Client | adapter | 1d |
| 2.6 | Tạo OrderAdapter | Client | adapter | 1d |
| 2.7 | Tạo UserFragment | Client | fragment | 1d |
| 2.8 | Tạo UserDetailActivity | Client | activity | 1d |

### Subtasks

**Layouts:**
- [ ] 2.1.1 `res/layout/activity_user_list.xml` - SwipeRefreshLayout + RecyclerView + SearchView + ChipGroup + states
- [ ] 2.2.1 `res/layout/item_user.xml` - MaterialCardView với avatar, info, switch
- [ ] 2.3.1 `res/layout/activity_user_detail.xml` - ScrollView với user info, orders list, action button
- [ ] 2.4.1 `res/layout/item_order.xml` - CardView với order_code, total, status, date

**Adapters:**
- [ ] 2.5.1 `UserAdapter.java` - RecyclerView.Adapter với UserAdapter.UserActionListener interface
- [ ] 2.6.1 `OrderAdapter.java` - RecyclerView.Adapter với click listener

**UI Components:**
- [ ] 2.7.1 `UserFragment.java` - Fragment cho list (copy pattern từ CategoryFragment)
- [ ] 2.8.1 `UserDetailActivity.java` - Activity hiển thị chi tiết user + orders

## Phase 3: Navigation + Resources

### Tasks

| # | Task | Module | Type | Estimate |
|---|------|--------|------|----------|
| 3.1 | Thêm menu item cho User | Client | menu | 0.5d |
| 3.2 | Thêm string resources | Client | resources | 0.5d |
| 3.3 | Thêm color/style resources | Client | resources | 0.5d |

### Subtasks

- [ ] 3.1.1 Thêm item vào `res/menu/menu_navigation_drawer.xml`
- [ ] 3.1.2 Thêm navigation trong `MainActivity.java`
- [ ] 3.2.1 Thêm strings cho user management
- [ ] 3.3.1 Thêm colors nếu cần

## Phase 4: Testing + Build

### Tasks

| # | Task | Module | Type | Estimate |
|---|------|--------|------|----------|
| 4.1 | Build debug APK | Client | build | 0.5d |
| 4.2 | Verify manual | Client | test | 1d |

### Subtasks

- [ ] 4.1.1 Chạy `./gradlew.bat :app:assembleDebug`
- [ ] 4.2.1 Test load user list
- [ ] 4.2.2 Test search
- [ ] 4.2.3 Test filter
- [ ] 4.2.4 Test view detail
- [ ] 4.2.5 Test view orders
- [ ] 4.2.6 Test toggle status

## File Structure

```
app/src/main/java/com/utt/foodcouriers_admin/
├── data/
│   ├── model/
│   │   ├── User.java (exists)
│   │   └── Order.java (new)
│   ├── request/
│   │   └── UserUpsertRequest.java (new)
│   └── repository/
│       ├── CategoryRepository.java (reference)
│       └── UserRepository.java (new)
├── ui/
│   ├── user/
│   │   ├── UserFragment.java (new)
│   │   ├── UserDetailActivity.java (new)
│   │   └── adapter/
│   │       ├── UserAdapter.java (new)
│   │       └── OrderAdapter.java (new)
│   └── main/
│       └── MainActivity.java (modify - add navigation)

res/
├── layout/
│   ├── activity_user_list.xml (new)
│   ├── activity_user_detail.xml (new)
│   ├── item_user.xml (new)
│   └── item_order.xml (new)
├── menu/
│   └── menu_navigation_drawer.xml (modify)
└── values/
    └── strings.xml (modify)
```

## API Summary

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/rest/v1/users?role=eq.customer&select=*&order=created_at.desc&limit=50` | GET | List users |
| `/rest/v1/users?role=eq.customer&or=(full_name.ilike.*,email.ilike.*,phone.ilike.*)&select=*` | GET | Search users |
| `/rest/v1/users?id=eq.{id}&select=*` | GET | Get user detail |
| `/rest/v1/orders?user_id=eq.{id}&select=*&order=created_at.desc&limit=20` | GET | Get user orders |
| `/rest/v1/users?id=eq.{id}` | PATCH | Update user status |

## Dependencies

- Supabase REST API (exists)
- BaseSupabaseRepository (exists)
- User model (exists)
- CategoryRepository pattern (reference)
- Material Components (exists)

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Order model chưa có trong codebase | Medium | Tạo mới theo schema |
| API endpoint không đúng | Medium | Verify với Supabase dashboard |
| RLS policy chưa cho phép admin access | High | Kiểm tra migrations |
