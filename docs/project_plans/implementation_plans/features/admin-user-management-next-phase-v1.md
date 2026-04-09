---
title: "Admin User Management Next Phase - Implementation Plan"
description: "Kế hoạch triển khai tiếp module quản lý user, self-update và phạm vi quản lý của staff"
audience: [ai-agents, developers]
tags: [implementation, admin, user, staff, orders, planning]
created: 2026-04-08
updated: 2026-04-08
status: draft
---

# Admin User Management Next Phase - Implementation Plan

## Executive Summary

Code hiện tại đã có nền cho:

- login theo `admin/staff`
- danh sách user theo role `customer/staff`
- tạo account mới qua `AdminUserAccountRepository`
- bật/tắt `is_active` qua `UserRepository.update(...)`

Nhưng còn 3 gap lớn:

1. Toggle active đang commit ngay, chưa có bước xác nhận.
2. Chưa có flow `current user` tự cập nhật profile của chính mình.
3. Chưa có mô hình nghiệp vụ hợp lý cho `staff` quản lý client và đơn hàng theo phạm vi được giao.

Hướng đi phù hợp nhất là triển khai theo 3 boundary rõ ràng:

- `UserRepository`: chỉ CRUD profile trong `public.users`
- `AdminUserAccountRepository`: chỉ làm account action cần backend an toàn
- `staff scope`: chỉ được thao tác trên dữ liệu gắn với `restaurant_staff`, không quản lý toàn cục bảng `users`

## Current State Verified From Code

### User management

- `ui/user/UserFragment.java`
  - load danh sách user theo `role = customer/staff`
  - gọi update active trực tiếp trong `onStatusChange(...)`
- `ui/user/adapter/UserAdapter.java`
  - `MaterialSwitch` bắn callback ngay khi đổi trạng thái
- `data/repository/UserRepository.java`
  - đã có `getUsers`, `getById`, `update`, `delete`
- `data/request/UserUpdateRequest.java`
  - phù hợp cho patch profile và activate/deactivate

### Current session and role

- `utils/SessionManager.java`
  - đã lưu `user_id`, `email`, `name`, `role`
  - đã có `getCurrentUser()`, `isAdmin()`, `isStaff()`
- `ui/auth/LoginActivity.java`
  - chỉ cho `admin/staff` vào app
  - sau login có check `user.isActive()`

### Existing staff scope pattern

- `ui/menu/MenuItemFragment.java`
  - `admin` xem mọi restaurant
  - `staff` chỉ load nhà hàng qua `getRestaurantsForStaff(currentUser.getId())`
- `data/remote/RestaurantClient.java`
  - dùng bảng `restaurant_staff` để scope dữ liệu staff

### Security baseline

- `docs/supabase/migrations/006_fix_users_rls_recursion.sql`
  - hiện đã có helper cho `admin` select/update `users`
- `docs/supabase/migrations/003_rls_policies.sql`
  - orders hiện mới mở toàn cục cho `admin`
  - chưa có policy đúng nghĩa để `staff` xem/cập nhật order theo restaurant scope

## Recommended Product Decisions

## 1. Fix toggle active ngay

Không nên để switch là hành động commit trực tiếp.

Quyết định:

- Khi user bấm đổi trạng thái active:
  - hiện `MaterialAlertDialog`
  - nội dung khác nhau cho activate/deactivate
  - có 2 nút `Hủy` và `Xác nhận`
- Chỉ call `UserRepository.update(...)` sau khi xác nhận
- Nếu user bấm `Hủy`, UI phải trả switch về trạng thái cũ
- Nếu API lỗi, cũng rollback switch về trạng thái cũ

Lý do:

- Hành động này là thay đổi trạng thái tài khoản
- hiện tại quá dễ bấm nhầm
- phù hợp với flow admin hiện có và không cần đổi backend

## 2. Current user self-update phải là use case riêng

Không nên dùng màn hình admin edit người khác cho chính current user.

Quyết định phase đầu:

- current user được sửa:
  - `full_name`
  - `phone`
  - `avatar_url`
- current user không được tự sửa:
  - `role`
  - `is_active`
  - auth email
  - password trong cùng flow

Luồng đề xuất:

1. Từ `MainActivity`, thêm entry `Tài khoản của tôi`
2. Mở `CurrentUserProfileActivity` hoặc `CurrentUserProfileFragment`
3. Load profile bằng `SessionManager.getUserId()` + `UserRepository.getById(...)`
4. Save bằng `UserRepository.update(...)`
5. Sau update thành công:
   - refresh `SessionManager.updateUserInfo(...)`
   - refresh navigation header name/email nếu cần

Lý do:

- boundary rõ giữa self-service và admin-service
- tránh vô tình cho current staff/admin sửa role hoặc active của chính mình
- dùng lại được `UserRepository`

## 3. Staff không nên “quản lý client” theo nghĩa CRUD toàn cục

Đây là quyết định quan trọng nhất.

`staff` chỉ nên quản lý client trong phạm vi nghiệp vụ của nhà hàng được gán, không phải:

- xem toàn bộ khách hàng của hệ thống
- sửa profile toàn cục của `users`
- activate/deactivate tài khoản client toàn hệ thống

Mô hình hợp lý hơn là:

- `staff` quản lý tập khách hàng phát sinh đơn tại nhà hàng mình phụ trách
- dữ liệu client list của staff là dữ liệu dẫn xuất từ `orders`
- mọi thao tác của staff phải scope theo `restaurant_staff`

## 4. Order là trục trung tâm của staff

Với role `staff`, nghiệp vụ chính nên xoay quanh:

- xem đơn thuộc nhà hàng được gán
- cập nhật trạng thái đơn trong state machine hợp lệ
- xem khách đã từng đặt tại nhà hàng đó
- xem chỉ số như:
  - số đơn
  - tổng chi tiêu
  - đơn gần nhất
  - trạng thái đơn gần nhất

Không nên bắt đầu từ “staff quản lý user”.
Nên bắt đầu từ “staff quản lý order, từ order mới nhìn ra client”.

## Target Permission Matrix

| Capability | Admin | Staff | Ghi chú |
|---|---|---|---|
| Xem toàn bộ `customer/staff` | Yes | No | Staff chỉ xem client trong scope |
| Tạo `customer/staff` account | Yes | No | Qua backend an toàn |
| Sửa profile `customer/staff` toàn cục | Yes | No | Chỉ admin |
| Activate/deactivate `customer/staff` | Yes | No | Phase đầu chỉ admin |
| Tự sửa profile của chính mình | Yes | Yes | Self-update riêng |
| Xem order toàn hệ thống | Yes | No | Staff chỉ xem order theo restaurant |
| Cập nhật order status | Yes | Yes, scoped | Theo restaurant được gán |
| Xem danh sách client theo order history | Yes | Yes, scoped | Từ orders join users |
| Xem số đơn của từng client | Yes | Yes, scoped | Aggregation theo restaurant |

## Delivery Plan

## Phase 0: UX bugfix cho active/deactivate

### Goal

Chặn thao tác nhầm khi đổi trạng thái tài khoản.

### Tasks

| # | Task | Module | Type | Estimate |
|---|------|--------|------|----------|
| 0.1 | Thêm confirm dialog cho toggle active | Android | bugfix | 0.5d |
| 0.2 | Rollback UI nếu hủy hoặc API lỗi | Android | bugfix | 0.25d |
| 0.3 | Bổ sung string cho confirm activate/deactivate | Android | resources | 0.25d |

### Implementation Notes

- Sửa ở `UserFragment` thay vì để `UserAdapter` tự quyết định nghiệp vụ
- Adapter chỉ emit intent `user + targetState`
- Fragment chịu trách nhiệm:
  - mở dialog xác nhận
  - gọi repository
  - rollback trạng thái list item nếu fail

### Acceptance Criteria

- Bấm switch không update ngay
- Có dialog xác nhận rõ ràng
- Bấm `Hủy` giữ nguyên trạng thái cũ
- API fail thì UI trở về trạng thái cũ và hiện lỗi

## Phase 1: Self-update cho current user

### Goal

Cho phép `admin/staff` chỉnh sửa hồ sơ cá nhân an toàn.

### Tasks

| # | Task | Module | Type | Estimate |
|---|------|--------|------|----------|
| 1.1 | Tạo screen hồ sơ hiện tại | Android | feature | 1d |
| 1.2 | Load current profile từ `UserRepository.getById` | Android | feature | 0.5d |
| 1.3 | Save patch profile self-update | Android | feature | 0.5d |
| 1.4 | Đồng bộ `SessionManager` sau update | Android | feature | 0.25d |

### Allowed Fields

- `full_name`
- `phone`
- `avatar_url`

### Forbidden Fields

- `role`
- `is_active`
- `email`
- `password`

### Acceptance Criteria

- current user mở được hồ sơ của mình
- sửa tên/sđt/avatar thành công
- header drawer cập nhật lại tên nếu cần
- không có control sửa role hoặc active

## Phase 2: Hoàn thiện admin update user

### Goal

Hoàn tất use case admin sửa `customer/staff` trên `public.users`.

### Tasks

| # | Task | Module | Type | Estimate |
|---|------|--------|------|----------|
| 2.1 | Tạo `UserDetailActivity` hoặc `EditUserDialog` | Android | feature | 1d |
| 2.2 | Load detail bằng `getById(id)` | Android | feature | 0.5d |
| 2.3 | Save patch bằng `UserRepository.update(...)` | Android | feature | 0.5d |
| 2.4 | Giới hạn admin chỉ quản lý `customer/staff` | Android | rule | 0.25d |

### Recommended Rule Set

- Admin được sửa:
  - `full_name`
  - `phone`
  - `avatar_url`
  - `role`
  - `is_active`
- Phase đầu:
  - email hiển thị read-only
  - không sửa `admin` account

### Acceptance Criteria

- Admin mở detail user từ list
- Update customer/staff thành công
- Không cho sửa tài khoản `admin` trong module này

## Phase 3: Staff client management theo restaurant scope

### Goal

Cho `staff` xem và quản lý tập client phục vụ cho vận hành nhà hàng, không biến staff thành admin thu nhỏ.

### Business Shape

Danh sách client của staff không lấy trực tiếp từ `users` table theo role.

Nguồn dữ liệu nên là:

- `orders`
- join `users`
- filter theo `restaurant_id IN (restaurants assigned to current staff)`

### Minimum feature set

| # | Feature | Why |
|---|---------|-----|
| 3.1 | Client list theo restaurant scope | Giúp staff nhìn đúng tệp khách phụ trách |
| 3.2 | Search theo tên/SĐT/email | Hỗ trợ CSKH và xử lý đơn |
| 3.3 | Client metrics: tổng đơn, đơn gần nhất, tổng chi tiêu | Đủ cho vận hành |
| 3.4 | Open order history của client trong scope | Liên kết tự nhiên với order management |

### Explicitly out of scope in phase đầu

- staff sửa profile toàn cục của client
- staff activate/deactivate account client
- staff đổi role user
- staff xem toàn bộ client của hệ thống

### Data contract recommendation

Ưu tiên tạo một view hoặc RPC riêng thay vì query chắp vá từ app:

- `staff_client_overview`
  - `client_id`
  - `full_name`
  - `phone`
  - `email`
  - `order_count`
  - `last_order_at`
  - `last_order_status`
  - `total_spent`
  - `restaurant_id`

Hoặc:

- RPC `get_staff_clients(p_staff_user_id uuid, p_restaurant_id uuid default null, p_query text default null)`

Lý do:

- gom business logic vào DB/backend
- dễ enforce permission
- app Android chỉ cần render

## Phase 4: Order management cho staff theo scope

### Goal

Làm order management trước hoặc song song với staff client list vì đây là nguồn dữ liệu chuẩn cho staff.

### Tasks

| # | Task | Module | Type | Estimate |
|---|------|--------|------|----------|
| 4.1 | Tạo `OrderRepository` cho admin/staff | Android | feature | 1d |
| 4.2 | Tạo list/detail order | Android | feature | 1.5d |
| 4.3 | Tạo update status flow có validation | Android | feature | 1d |
| 4.4 | Scope order cho staff theo restaurant được gán | DB + Android | security | 1d |

### Recommended sequencing

Nếu phải ưu tiên, làm theo thứ tự:

1. order list scoped cho staff
2. order detail + update status
3. staff client overview

Lý do:

- `staff` sống bằng order, không sống bằng user CRUD
- client overview chỉ là lớp tổng hợp từ order

## Database / Security Work Needed

## 1. Users policies

Giữ nguyên tinh thần:

- admin quản lý toàn cục `public.users`
- current user tự sửa profile của chính mình

Nếu cần chặt hơn, nên thêm policy update own với field-level guard ở backend hoặc tách endpoint self-update.

## 2. Orders policies for staff

Hiện docs chỉ thể hiện:

- admin xem/cập nhật toàn bộ orders
- chưa có rule đúng cho staff theo assigned restaurants

Nên bổ sung:

- `orders_select_staff_scoped`
- `orders_update_staff_scoped`

Điều kiện:

- current auth user có role `staff`
- `orders.restaurant_id` thuộc tập restaurant trong `restaurant_staff`

## 3. Aggregation source for staff client list

Không khuyến nghị mở select toàn cục trên `users` cho staff chỉ để build danh sách khách.

Khuyến nghị:

- build qua SQL view / RPC scoped từ `orders`
- expose read-only contract cho staff

## Suggested File-Level Work Breakdown

### Android

- `app/src/main/java/com/utt/foodcouriers_admin/ui/user/UserFragment.java`
  - confirm toggle active
  - điều hướng sang detail/edit
- `app/src/main/java/com/utt/foodcouriers_admin/ui/user/adapter/UserAdapter.java`
  - giữ callback ở mức UI intent
- `app/src/main/java/com/utt/foodcouriers_admin/utils/SessionManager.java`
  - đồng bộ self-update nếu cần mở rộng persisted fields
- `app/src/main/java/com/utt/foodcouriers_admin/ui/main/MainActivity.java`
  - thêm entry `Tài khoản của tôi`
- `app/src/main/java/com/utt/foodcouriers_admin/data/repository/UserRepository.java`
  - tái sử dụng cho self-update và admin-update

### DB / Supabase

- migration mới cho staff-scoped order policies
- view hoặc RPC cho staff client overview

## Recommended Implementation Order

1. Fix confirm active/deactivate
2. Làm self-update cho current user
3. Hoàn thiện admin update/detail user
4. Làm order management scoped cho staff
5. Làm staff client overview từ orders

Đây là thứ tự có tỷ lệ thành công cao nhất vì:

- giải quyết bug UX ngay
- hoàn thiện capability cá nhân trước
- giữ module admin user đơn giản
- không mở quyền staff quá sớm trên `users`

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Cho staff sửa trực tiếp `users` | High | Chỉ cho staff xem client derived từ orders |
| Scope staff chỉ làm ở UI, không làm ở RLS | High | Bắt buộc bổ sung policy/view/RPC ở DB |
| Self-update dùng chung form admin edit | Medium | Tách use case và disable field nhạy cảm |
| Active toggle rollback UI không chuẩn | Medium | Giữ source of truth ở list reload sau callback |

## Final Recommendation

Hướng đi tốt nhất cho hệ thống hiện tại là:

- giữ `admin user management` là module quản trị profile toàn cục
- thêm `self profile` như một use case riêng cho current user
- không mở `staff` thành role quản lý user toàn hệ thống
- dùng `order scope by restaurant_staff` làm trung tâm cho toàn bộ capability của staff

Nếu bám hướng này, hệ thống sẽ:

- dễ bảo trì hơn
- ít rủi ro phân quyền sai hơn
- khớp với pattern staff scope đã có ở menu module
- mở đường tự nhiên cho order dashboard, client metrics và vận hành nhà hàng
