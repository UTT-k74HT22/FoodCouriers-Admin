---
title: "User Management - PRD"
description: "Module quản lý người dùng cho Admin app"
audience: [ai-agents, developers]
tags: [requirements, admin, user]
created: 2026-04-07
status: draft
---

# User Management - PRD

## 1. Overview

Module quản lý tài khoản khách hàng trong Admin app: xem danh sách, tìm kiếm, xem chi tiết, xem lịch sử đơn hàng, vô hiệu hóa/kích hoạt tài khoản.

## 2. Problem Statement

Admin cần quản lý tài khoản khách hàng để:
- Theo dõi và tìm kiếm khách hàng
- Xem chi tiết thông tin khách hàng
- Kiểm tra lịch sử đơn hàng của khách
- Vô hiệu hóa tài khoản vi phạm

## 3. User Stories

| # | Story | Priority |
|---|-------|----------|
| US1 | Là admin, tôi muốn xem danh sách tất cả khách hàng để quản lý | P0 |
| US2 | Là admin, tôi muốn tìm kiếm khách hàng theo tên, email, số điện thoại | P0 |
| US3 | Là admin, tôi muốn xem chi tiết thông tin khách hàng | P0 |
| US4 | Là admin, tôi muốn xem lịch sử đơn hàng của khách hàng | P0 |
| US5 | Là admin, tôi muốn vô hiệu hóa/kích hoạt tài khoản khách hàng | P1 |

## 4. Functional Requirements

### 4.1 User List Screen

- **FR1.1**: Hiển thị danh sách khách hàng với pagination (limit 50)
- **FR1.2**: SearchView để tìm kiếm theo: full_name, email, phone (sử dụng OR query)
- **FR1.3**: Filter theo trạng thái is_active (Tất cả/Active/Inactive)
- **FR1.4**: Mỗi item hiển thị: avatar, full_name, email, phone, is_active, switch toggle
- **FR1.5**: Pull-to-refresh để reload dữ liệu
- **FR1.6**: Click vào item để mở User Detail
- **FR1.7**: Trạng thái loading/empty/error với retry button

### 4.2 User Detail Screen

- **FR2.1**: Hiển thị thông tin chi tiết: avatar, full_name, email, phone, role, is_active, created_at
- **FR2.2**: Hiển thị danh sách đơn hàng của user (orders table)
- **FR2.3**: Nút vô hiệu hóa/kích hoạt tài khoản với confirmation dialog
- **FR2.4**: Hiển thị warning nếu user có đơn hàng đang active

### 4.3 Toggle User Status

- **FR3.1**: API PATCH /rest/v1/users với payload {"is_active": boolean}
- **FR3.2**: Confirmation dialog trước khi thay đổi
- **FR3.3**: Toast thông báo kết quả
- **FR3.4**: Refresh lại list sau khi thay đổi

## 5. Non-Functional Requirements

- **NFR1**: Thời gian phản hồi API < 3s
- **NFR1**: Offline handling với error message rõ ràng
- **NFR2**: Tuân thủ pattern code hiện có (CategoryRepository, CategoryFragment)

## 6. Data Model

### User (từ database schema)
```java
- id: UUID
- auth_id: UUID
- full_name: String
- phone: String
- email: String
- avatar_url: String
- role: String (customer/staff/admin)
- is_active: Boolean
- created_at: Timestamp
- updated_at: Timestamp
```

### Order (cần tạo)
```java
- id: UUID
- order_code: String
- user_id: UUID
- restaurant_id: UUID
- total: Integer
- status: String
- created_at: Timestamp
```

## 7. API Contracts

### Get Users
```
GET /rest/v1/users?role=eq.customer&select=*&order=created_at.desc&limit=50

With search:
GET /rest/v1/users?role=eq.customer&or=(full_name.ilike.*%s*,email.ilike.*%s*,phone.ilike.*%s*)&select=*

With filter:
GET /rest/v1/users?role=eq.customer&is_active=eq.true&select=*
```

### Get User Detail
```
GET /rest/v1/users?id=eq.{id}&select=*
```

### Get User Orders
```
GET /rest/v1/orders?user_id=eq.{id}&select=*&order=created_at.desc&limit=20
```

### Update User Status
```
PATCH /rest/v1/users?id=eq.{id}
{
    "is_active": false
}
```

## 8. UI/UX Design

### User List Layout
- MaterialToolbar với title "Quản lý người dùng"
- SearchView (androidx.appcompat.widget.SearchView)
- ChipGroup filter: Tất cả | Hoạt động | Bị khóa
- SwipeRefreshLayout + RecyclerView
- Item layout: CardView với avatar, text columns, switch
- FAB không cần (không có chức năng thêm user)

### User Detail Layout
- ScrollView chứa:
  - Avatar image
  - Info section: full_name, email, phone, role, status, created_at
  - Divider
  - "Lịch sử đơn hàng" header
  - RecyclerView for orders
  - MaterialButton "Khóa tài khoản" / "Kích hoạt tài khoản"

### Order Item Layout
- CardView: order_code, total (format VND), status, created_at

## 9. Edge Cases

| Case | Handling |
|------|----------|
| Không có kết quả tìm kiếm | Hiển thị empty state "Không tìm thấy người dùng" |
| User có đơn hàng đang active | Warning dialog khi deactivate |
| Network error | Error state với retry button |
| User đã bị vô hiệu hóa | Hiển thị status khác màu, button "Kích hoạt" |

## 10. Dependencies

- Supabase REST API
- Existing User model (`app/src/main/java/com/utt/foodcouriers_admin/data/model/User.java`)
- Existing CategoryRepository pattern
- Material Components

## 11. Out of Scope

- Tạo mới user (khách hàng tự đăng ký)
- Chỉnh sửa thông tin user
- Xóa vĩnh viễn user
- Quản lý staff/admin accounts
