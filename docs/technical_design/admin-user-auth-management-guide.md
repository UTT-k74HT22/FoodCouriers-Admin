---
title: "Admin User Auth Management Guide"
description: "Hướng dẫn triển khai admin tạo và cập nhật tài khoản client/staff với Supabase Auth + public.users"
audience: [developers, ai-agents]
tags: [technical, admin, user, auth, supabase]
created: 2026-04-07
status: draft
---

# Admin User Auth Management Guide

## 1. Mục tiêu

Tài liệu này hướng dẫn cách triển khai use case:

- Admin tạo tài khoản mới cho `client` hoặc `staff`
- Admin cập nhật thông tin tài khoản
- Admin khóa mềm tài khoản
- Admin reset hoặc đổi mật khẩu tạm thời cho tài khoản

Mục tiêu là giữ đúng ranh giới giữa:

- `Supabase Auth`: quản lý định danh đăng nhập và password
- `public.users`: quản lý profile nghiệp vụ của hệ thống

Tài liệu này bám theo code và schema hiện có trong project:

- `app/src/main/java/com/utt/foodcouriers_admin/data/remote/AuthClient.java`
- `app/src/main/java/com/utt/foodcouriers_admin/data/repository/UserRepository.java`
- `docs/supabase/migrations/001_initial_schema.sql`
- `docs/supabase/migrations/003_rls_policies.sql`

## 2. Kết luận ngắn trước khi triển khai

### 2.1 Password nằm ở đâu

`password` không nằm trong `public.users`.

`password` chỉ thuộc về `Supabase Auth`, tức `auth.users`.

Vì vậy:

- `UserCreateRequest` của bảng `public.users` không nên chứa password
- `UserUpdateRequest` cũng không nên chứa password
- Password phải đi qua Auth API hoặc Admin API của Supabase

### 2.2 Liên kết giữa auth và bảng users

Schema hiện tại đã đúng hướng:

```sql
auth_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE
```

Nghĩa là:

1. Tạo user ở `auth.users` trước
2. Lấy `auth.users.id`
3. Ghi vào `public.users.auth_id`

### 2.3 Không nên để app Android trực tiếp tạo Auth user bằng service role

Admin app Android không nên giữ `service_role key`.

Lý do:

- `service_role` có quyền rất cao
- Nếu nhúng vào mobile app thì key có thể bị extract
- Khi đó ai có app cũng có thể tạo, sửa, xóa Auth user ngoài ý muốn

Kết luận:

- App Android chỉ gọi `anon key` + access token như hiện tại
- Tạo tài khoản mới có password cho người khác phải đi qua backend an toàn
- Backend đó có thể là Supabase Edge Function hoặc server riêng

## 3. Kiến trúc nên dùng

## 3.1 Phân tầng đề xuất

```text
Admin UI
  -> AdminUserAccountRepository
      -> Edge Function / secure backend endpoint
          -> Supabase Admin Auth API
          -> public.users table

Admin UI
  -> UserRepository
      -> Supabase REST API (public.users)
```

## 3.2 Trách nhiệm từng phần

### UserRepository

Chỉ xử lý CRUD profile ở `public.users`:

- lấy danh sách user
- lấy detail user
- cập nhật profile
- khóa mềm bằng `is_active = false`

Không xử lý:

- tạo Auth user
- set password
- reset password admin-level

### AuthClient

Hiện tại `AuthClient` đang đúng cho:

- login bằng email/password
- self-signup của user hiện tại
- lấy current authenticated user

Không nên dùng `AuthClient.signUp(...)` cho use case:

- admin tạo tài khoản cho staff/client khác

Lý do:

- `signUp` là flow self-registration
- session có thể gắn vào user mới tạo
- behavior phụ thuộc setting `Confirm email`
- không phù hợp với use case “admin tạo user cho người khác”

### Backend an toàn

Phần này chịu trách nhiệm:

- tạo Auth user bằng admin API
- set password ban đầu hoặc password tạm
- tạo row trong `public.users`
- cập nhật password khi admin reset password
- disable Auth user nếu business cần khóa đăng nhập hoàn toàn

## 4. Thiết kế nghiệp vụ đúng cho admin

## 4.1 Use case A: Admin tạo tài khoản client/staff

### Input nghiệp vụ

Admin nhập:

- full name
- email
- phone
- role: `client` hoặc `staff`
- trạng thái active
- password tạm thời

Lưu ý:

- nếu schema role hiện tại đang dùng `customer/staff/admin` thì `client` ở UI phải map sang `customer` ở DB

### Luồng xử lý chuẩn

1. App gọi endpoint backend an toàn, ví dụ:
   - `POST /admin/user-accounts`
2. Backend validate input
3. Backend gọi Supabase Admin API để tạo user trong `auth.users`
4. Backend lấy `authUser.id`
5. Backend insert row vào `public.users`
6. Backend trả về object user profile cho app

### Pseudo flow

```text
Admin app
  -> POST /admin/user-accounts
     { email, password, fullName, phone, role, isActive }

Secure backend
  -> auth.admin.createUser(...)
  -> insert public.users(auth_id, full_name, phone, email, role, is_active)
  -> return created profile
```

## 4.2 Use case B: Admin cập nhật profile

Admin có thể sửa:

- full name
- phone
- email
- avatar
- role
- is_active

Use case này không cần đụng đến password nếu chỉ sửa profile.

Nên tách riêng:

- `UserUpdateRequest`: dùng cho `public.users`
- `AdminResetPasswordRequest`: dùng cho auth/password

## 4.3 Use case C: Admin khóa mềm tài khoản

Có 2 mức khóa, cần phân biệt rõ:

### Mức 1: khóa nghiệp vụ

Chỉ set:

```json
{ "is_active": false }
```

Trong `public.users`

Tác dụng:

- tài khoản bị đánh dấu inactive
- app business logic có thể chặn truy cập

Nhược điểm:

- nếu login chỉ dựa vào `auth.users` mà không check `public.users.is_active`, user vẫn có thể đăng nhập

### Mức 2: khóa đăng nhập thực sự

Ngoài `public.users.is_active = false`, backend còn phải disable auth user ở tầng auth.

Tùy cách triển khai backend, có thể:

- cập nhật auth user metadata để đánh dấu disabled
- hoặc áp dụng policy/login gate dựa trên `public.users.is_active`

Trong project này, cách thực dụng hơn là:

1. vẫn soft delete ở `public.users`
2. sau login, app luôn fetch profile
3. nếu `is_active = false` thì logout ngay và báo tài khoản bị khóa

Cách này phù hợp với code hiện có trong `AuthClient.fetchUserProfile(...)`.

## 4.4 Use case D: Admin reset password

Không đi qua `UserRepository`.

Phải đi qua backend an toàn.

Admin gửi:

- target user id hoặc email
- password mới tạm thời

Backend:

1. tìm `public.users`
2. lấy `auth_id`
3. gọi admin API để update password cho auth user
4. có thể đánh dấu `must_change_password = true` trong metadata hoặc trong bảng profile nếu muốn

## 5. Vì sao không nên dùng signUp hiện tại cho admin create

Hiện trong `AuthClient.signUp(...)`, code đang:

1. gọi `/auth/v1/signup`
2. lấy auth user
3. tạo profile trong `public.users`

Flow này phù hợp hơn với self-register.

Không phù hợp cho admin create vì:

- signup có thể phụ thuộc vào xác nhận email
- signup có thể tạo session cho user vừa tạo
- không phản ánh đúng use case “admin tạo tài khoản cho người khác”
- khó kiểm soát password policy và email confirmation

Kết luận:

- self-register: dùng `signUp`
- admin create account: dùng admin API qua backend an toàn

## 6. Cấu trúc request/response nên có

## 6.1 Request cho CRUD profile

### UserCreateRequest

Chỉ dùng nếu bạn tạo row `public.users` trực tiếp ở server-side.

Chứa:

- authId
- fullName
- phone
- email
- avatarUrl
- role
- isActive

Không chứa:

- password

### UserUpdateRequest

Chứa các field profile có thể cập nhật:

- fullName
- phone
- email
- avatarUrl
- role
- isActive
- updatedAt nếu cần

Không chứa:

- password

## 6.2 Request cho admin create account

Tạo request riêng cho tầng backend, ví dụ:

### AdminCreateUserAccountRequest

Chứa:

- email
- password
- fullName
- phone
- role
- isActive

Đây là request nghiệp vụ cấp cao, không phải request PATCH/POST trực tiếp cho `public.users`.

## 6.3 Request cho admin reset password

Tạo request riêng:

### AdminResetPasswordRequest

Chứa:

- userId hoặc authId
- newPassword

## 7. Validation nên tách thế nào

## 7.1 Validate create account

Use case admin tạo tài khoản nên yêu cầu bắt buộc:

- fullName
- email
- password
- role

`phone` có thể optional hoặc required tùy business.

Validate nên chia theo tầng:

### Tầng UI

Kiểm tra nhanh:

- không để trống
- email format cơ bản
- password length tối thiểu

### Tầng backend

Kiểm tra chắc chắn:

- email chưa tồn tại
- role hợp lệ
- password policy
- mapping role đúng với DB

### Tầng repository profile

Chỉ validate field profile nếu endpoint backend hoặc REST cần.

Không gánh validation password tại `UserRepository`.

## 7.2 Validate update profile

Khi update:

- không bắt buộc tất cả field
- field nào gửi lên thì validate field đó

Ví dụ:

- nếu có `email` thì validate email format
- nếu có `fullName` thì không được rỗng
- nếu có `role` thì phải thuộc tập hợp cho phép

## 7.3 Validate reset password

Tách riêng hoàn toàn.

Không để lẫn vào `update user profile`.

## 8. Đề xuất thiết kế file trong project

## 8.1 App Android

### Repository

- `UserRepository`
  - list/detail/update profile/soft delete
- `AdminUserAccountRepository`
  - gọi endpoint backend an toàn để create account và reset password

### Request models

- `UserUpdateRequest`
- `AdminCreateUserAccountRequest`
- `AdminResetPasswordRequest`

### UI

- form tạo account admin
- form sửa profile
- dialog reset password

## 8.2 Backend an toàn

Nếu dùng Supabase Edge Function:

- `create-admin-user-account`
- `reset-admin-user-password`

Mỗi function:

- nhận JWT của admin hiện tại
- kiểm tra role admin
- dùng service role trong môi trường server
- gọi admin API

## 9. Quyết định thiết kế nên chốt

## 9.1 Role mapping

Schema hiện tại có:

- `customer`
- `staff`
- `admin`

Nếu UI/business gọi là `client`, cần chốt một rule cố định:

- UI label `Client`
- DB value `customer`

Không nên để lúc thì `client`, lúc thì `customer`.

## 9.2 Khi khóa user có chặn login không

Cần chốt 1 trong 2 cách:

### Cách A

Chỉ set `public.users.is_active = false`

Ưu điểm:

- dễ làm

Nhược điểm:

- phải chặn sau login

### Cách B

Ngoài set inactive còn disable ở Auth

Ưu điểm:

- chặn đăng nhập chặt hơn

Nhược điểm:

- cần backend admin API phức tạp hơn

Khuyến nghị giai đoạn đầu:

- dùng Cách A
- sau login luôn fetch profile
- nếu inactive thì từ chối vào app

## 9.3 Có dùng trigger tự tạo profile không

Bạn có 2 lựa chọn:

### Lựa chọn 1: Backend create cả auth user và public.users

Ưu điểm:

- dễ hiểu
- flow explicit

Nhược điểm:

- backend phải làm 2 bước

### Lựa chọn 2: Trigger sau khi insert `auth.users`

Ưu điểm:

- liên kết tự động
- self-signup và admin-create có thể dùng chung

Nhược điểm:

- debug khó hơn
- nếu trigger fail có thể làm signup fail

Với giai đoạn hiện tại, tôi khuyên:

- admin create account: backend tạo auth user rồi insert `public.users` luôn
- chưa cần trigger nếu bạn muốn dễ debug

## 10. Flow triển khai cụ thể theo thứ tự

## Phase 1: Chốt mô hình dữ liệu

1. Chốt role DB dùng `customer/staff/admin`
2. Chốt `client` trên UI sẽ map thành `customer`
3. Chốt create/update không chứa password trong profile request

## Phase 2: Chốt boundary

1. `UserRepository` chỉ làm profile CRUD
2. `AuthClient` chỉ làm auth của user đang đăng nhập
3. admin create/reset password đi qua backend an toàn

## Phase 3: Thiết kế endpoint backend

Tối thiểu cần:

1. `POST /admin/user-accounts`
2. `PATCH /admin/user-accounts/{id}`
3. `POST /admin/user-accounts/{id}/reset-password`
4. `POST /admin/user-accounts/{id}/deactivate`
5. `POST /admin/user-accounts/{id}/activate`

## Phase 4: App Android tích hợp

1. Form create account gọi endpoint backend
2. Form edit profile gọi `UserRepository.update(...)`
3. Nút khóa mềm gọi `UserRepository.delete(...)` hoặc method rõ nghĩa hơn như `deactivate(...)`
4. Nút reset password gọi endpoint backend riêng

## 11. Những chỗ nên tránh

- Không nhét `service_role` vào app Android
- Không lưu password vào `public.users`
- Không dùng `UserCreateRequest` cho soft delete
- Không gộp reset password vào `update profile`
- Không dùng self-signup flow để mô phỏng admin-create-user

## 12. Khuyến nghị cuối cùng cho codebase này

Nếu bám theo kiến trúc hiện tại của repo, hướng an toàn và dễ maintain nhất là:

### Trong app

- `UserRepository`: profile CRUD + soft delete
- `AuthRepository`: login/logout/current user
- thêm repository mới cho admin account actions cần password

### Trong backend an toàn

- create auth user
- update auth password
- nếu cần, quản lý activation ở tầng auth

### Trong DB

- tiếp tục dùng `public.users.auth_id -> auth.users.id`
- profile không chứa password

## 13. Checklist triển khai

- [ ] Chốt role mapping `client -> customer`
- [ ] Tạo tài liệu API cho `POST /admin/user-accounts`
- [ ] Tạo request model riêng cho create account cấp admin
- [ ] Tạo request model riêng cho reset password
- [ ] Giữ `UserUpdateRequest` chỉ cho profile
- [ ] Chốt logic inactive có chặn login ngay sau sign-in hay không
- [ ] Review lại RLS cho admin operations ngoài REST thường
- [ ] Test các case: create, update profile, deactivate, reactivate, reset password

## 14. Tóm tắt một câu

Admin tạo tài khoản cho client/staff phải là một use case riêng đi qua backend an toàn để tạo `auth.users` và set password; còn `UserRepository` trong app chỉ nên quản lý profile ở `public.users`.
