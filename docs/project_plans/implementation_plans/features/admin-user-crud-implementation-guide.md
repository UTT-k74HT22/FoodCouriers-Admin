---
title: "Admin User CRUD Implementation Guide"
description: "Hướng dẫn triển khai hoàn chỉnh CRUD tài khoản client/staff cho app Admin"
audience: [developers, ai-agents]
tags: [implementation, admin, user, auth, supabase]
created: 2026-04-07
status: draft
---

# Admin User CRUD Implementation Guide

## 1. Mục tiêu

Triển khai hoàn chỉnh CRUD cho tài khoản `client/staff` trong app Admin với 2 boundary rõ ràng:

- Boundary 1: `public.users` cho profile nghiệp vụ
- Boundary 2: `Supabase Auth` cho email/password và định danh đăng nhập

Mục tiêu của guide này là để bạn có thể tự code lại theo từng phần, không nhầm giữa:

- create profile
- create auth account
- update profile
- reset password
- soft delete

## 2. Quyết định thiết kế chốt

## 2.1 Role dùng trong DB

Theo schema hiện tại:

- `customer`
- `staff`
- `admin`

Nếu UI muốn hiện chữ `client`, hãy map:

- UI `client` -> DB `customer`

Không lưu `client` trực tiếp xuống DB nếu schema hiện tại chưa hỗ trợ.

## 2.2 Password không nằm trong `public.users`

Không bao giờ thêm `password` vào:

- `User`
- `UserCreateRequest`
- `UserUpdateRequest`

Password chỉ đi qua:

- `AdminCreateUserAccountRequest`
- `AdminResetUserPasswordRequest`
- backend an toàn / Edge Function

## 2.3 Soft delete là deactivate

Trong module này, `delete` không phải xóa cứng.

`delete` = cập nhật:

```json
{ "is_active": false }
```

Nghĩa là:

- dữ liệu profile vẫn còn
- user bị vô hiệu hóa ở tầng nghiệp vụ

## 2.4 Không dùng `AuthClient.signUp()` cho admin create account

`AuthClient.signUp()` hiện phù hợp với self-register.

Admin tạo account cho user khác phải đi qua backend an toàn có quyền admin-level.

## 3. DTO nên dùng

## 3.1 DTO cho profile table

### `UserCreateRequest`

Mục đích:

- dùng khi backend hoặc service nội bộ cần insert vào `public.users`

Fields:

- `auth_id`
- `full_name`
- `phone`
- `email`
- `avatar_url`
- `role`
- `is_active`

### `UserUpdateRequest`

Mục đích:

- patch profile
- soft delete
- activate/deactivate

Fields:

- `full_name`
- `phone`
- `email`
- `avatar_url`
- `role`
- `is_active`
- `updated_at` nếu cần

## 3.2 DTO cho admin account actions

### `AdminCreateUserAccountRequest`

Mục đích:

- gửi lên backend an toàn để tạo auth user + profile

Fields:

- `email`
- `password`
- `full_name`
- `phone`
- `avatar_url`
- `role`
- `is_active`
- `email_confirm`

### `AdminResetUserPasswordRequest`

Mục đích:

- gửi lên backend an toàn để đổi password

Fields:

- `user_id`
- `auth_id`
- `new_password`
- `force_change_password`

## 4. Trách nhiệm từng repository

## 4.1 `UserRepository`

Chỉ làm việc với `public.users`.

Methods nên có:

1. `getUsers(search, role, isActive, limit, offset)`
2. `getById(id)`
3. `create(UserCreateRequest request)`
4. `update(id, UserUpdateRequest request)`
5. `delete(id)` hoặc `deactivate(id)`
6. `activate(id)`

Không nên thêm vào `UserRepository`:

- `createAccountWithPassword`
- `resetPassword`
- `changeAuthEmail`
- các hàm gọi Supabase Admin API

## 4.2 `AdminUserAccountRepository`

Repository mới, chuyên gọi backend an toàn.

Methods nên có:

1. `createAccount(AdminCreateUserAccountRequest request)`
2. `resetPassword(AdminResetUserPasswordRequest request)`
3. `activateAccount(userId)` nếu backend cần xử lý cả auth
4. `deactivateAccount(userId)` nếu backend cần xử lý cả auth

Repository này không gọi trực tiếp `rest/v1/users` nếu action đụng password hoặc auth identity.

## 5. CRUD logic hoàn chỉnh

## 5.1 C - Create account

### Mục tiêu

Admin tạo tài khoản mới cho `customer/staff`.

### Input từ UI

- full name
- email
- phone
- avatar URL nếu có
- role
- is active
- password tạm
- email confirm hay không

### Flow đúng

1. UI validate input cơ bản
2. UI tạo `AdminCreateUserAccountRequest`
3. Gọi `AdminUserAccountRepository.createAccount(...)`
4. Backend nhận request
5. Backend validate business
6. Backend gọi Supabase Admin API để tạo `auth.users`
7. Backend lấy `authUser.id`
8. Backend insert row vào `public.users`
9. Backend trả về profile đã tạo
10. UI refresh danh sách user

### Validation ở UI

Bắt buộc:

- full name không rỗng
- email không rỗng
- password không rỗng
- password tối thiểu 6 hoặc 8 ký tự
- role hợp lệ

Phone:

- bắt buộc hoặc không là quyết định business
- nếu hiện tại bạn đang validate create user cần 3 field bắt buộc thì có thể giữ:
  - full name
  - phone
  - email

Nhưng với admin create account thực tế, tôi khuyên bắt buộc thêm:

- password
- role

### Validation ở backend

- email chưa tồn tại trong auth
- role nằm trong tập cho phép
- password đủ policy
- mapping `client -> customer`
- nếu insert profile lỗi sau khi tạo auth user thì phải rollback hoặc cleanup auth user

### Contract backend đề xuất

`POST /admin/user-accounts`

Request:

```json
{
  "email": "staff01@example.com",
  "password": "Temp@123",
  "full_name": "Nguyen Van B",
  "phone": "0901234567",
  "avatar_url": null,
  "role": "staff",
  "is_active": true,
  "email_confirm": true
}
```

Response success:

```json
{
  "success": true,
  "message": "User account created successfully",
  "data": {
    "id": "public-user-id",
    "auth_id": "auth-user-id",
    "full_name": "Nguyen Van B",
    "phone": "0901234567",
    "email": "staff01@example.com",
    "role": "staff",
    "is_active": true
  }
}
```

## 5.2 R - Read list

### Mục tiêu

Admin xem danh sách `customer/staff`, có search/filter/pagination.

### Method đề xuất

`getUsers(String query, String role, Boolean isActive, int limit, int offset, RepositoryCallback<List<User>> callback)`

### Query logic

Base query:

```text
?select=*&order=created_at.desc
```

Thêm filter role:

```text
&role=eq.staff
```

Hoặc:

```text
&role=eq.customer
```

Thêm filter active:

```text
&is_active=eq.true
```

Thêm search:

Nên search theo nhiều field:

```text
&or=(full_name.ilike.*abc*,email.ilike.*abc*,phone.ilike.*abc*)
```

Lưu ý hiện `UserRepository` đang chỉ search `full_name`.

Khi bạn hoàn thiện CRUD, nên mở rộng search sang:

- full_name
- email
- phone

### Trạng thái UI nên có

- loading
- empty
- content
- error

## 5.3 R - Read detail

### Mục tiêu

Xem chi tiết profile và có thể mở màn hình edit.

### Method

`getById(String id, RepositoryCallback<User> callback)`

### Bổ sung nên có

Nếu màn detail cần hiển thị thêm:

- lịch sử đơn hàng
- số đơn đã đặt
- trạng thái tài khoản

thì tách riêng repository/order call, không nhồi tất cả vào `UserRepository`.

## 5.4 U - Update profile

### Mục tiêu

Admin sửa thông tin nghiệp vụ của user, không đụng password.

### Input

Từ form edit:

- full name
- phone
- email
- avatar URL
- role
- is active

### Flow

1. UI tạo `UserUpdateRequest`
2. Chỉ set những field cho phép cập nhật
3. Gọi `UserRepository.update(userId, request, callback)`
4. Repository validate field nào có mặt thì validate field đó
5. PATCH `public.users`
6. Refresh detail/list

### Validation

Field nào có mặt thì validate field đó:

- `full_name`: không blank
- `phone`: không blank nếu được gửi
- `email`: không blank nếu được gửi
- `role`: phải là `customer/staff/admin`

### Điều cần chốt

Nếu email được cập nhật ở `public.users`, bạn phải quyết định:

- có cho email profile khác email auth không
- hay khi đổi email phải đồng bộ cả ở auth

Khuyến nghị:

- giai đoạn đầu: khóa sửa email trong admin app
- hoặc tách use case `change login email` sang backend an toàn riêng

## 5.5 D - Soft delete / deactivate

### Mục tiêu

Admin vô hiệu hóa tài khoản mà không mất dữ liệu.

### Flow hiện tại đúng nên dùng

1. UI gọi `delete(userId)` hoặc tốt hơn là `deactivate(userId)`
2. Repository tạo `UserUpdateRequest`
3. set `is_active = false`
4. PATCH `public.users`

### Khuyến nghị về tên method

Nếu ưu tiên đọc code dễ hiểu, nên đổi:

- `delete(...)` -> `deactivate(...)`

và có thêm:

- `activate(...)`

Nếu vẫn giữ tên `delete()` vì thuận CRUD thì cần comment rõ:

- đây là soft delete, không phải hard delete

### Sau khi deactivate cần làm gì

Trong luồng login hiện tại, sau khi auth thành công app sẽ fetch profile bằng `auth_id`.

Khi lấy được profile:

- nếu `is_active = false`
  - không cho vào app
  - logout local
  - hiển thị thông báo tài khoản bị vô hiệu hóa

Đây là cách chặn đăng nhập thực dụng nhất với code hiện tại.

## 5.6 Reset password

### Mục tiêu

Admin cấp password tạm mới cho client/staff.

### Flow đúng

1. UI mở dialog reset password
2. UI tạo `AdminResetUserPasswordRequest`
3. Gọi `AdminUserAccountRepository.resetPassword(...)`
4. Backend xác thực admin hiện tại
5. Backend tìm `auth_id`
6. Backend gọi Supabase Admin API update password
7. Trả success

### Không làm trong `UserRepository`

Lý do:

- đụng auth
- đụng password
- không phải profile CRUD

## 6. Repository method set đề xuất

## 6.1 `UserRepository`

```text
getUsers(query, role, isActive, limit, offset, callback)
getById(id, callback)
create(request, callback)
update(id, request, callback)
deactivate(id, callback)
activate(id, callback)
```

Có thể giữ thêm:

```text
delete(id, callback)
```

nhưng chỉ là alias của `deactivate`.

## 6.2 `AdminUserAccountRepository`

```text
createAccount(request, callback)
resetPassword(request, callback)
```

Nếu backend của bạn sau này xử lý cả deactivate/activate ở auth layer, có thể thêm:

```text
deactivateAccount(userId, callback)
activateAccount(userId, callback)
```

## 7. UI screens nên có

## 7.1 User List Screen

Features:

- tabs/filter theo role: `Customer | Staff`
- search theo tên/email/phone
- filter trạng thái: all/active/inactive
- click item để sang detail
- button add user

## 7.2 Create User Screen

Fields:

- full name
- email
- phone
- password tạm
- role
- active switch
- avatar URL nếu cần

Actions:

- save
- cancel

## 7.3 Edit User Screen

Fields:

- full name
- phone
- avatar URL
- role
- active switch

Khuyến nghị:

- email có thể chỉ hiển thị read-only trong phase đầu

## 7.4 User Detail Screen

Actions:

- edit profile
- deactivate / activate
- reset password

## 8. Validation matrix

| Use case | DTO | Required fields |
|---|---|---|
| Create profile row | `UserCreateRequest` | `authId`, `fullName`, `phone`, `email` |
| Update profile | `UserUpdateRequest` | không bắt buộc toàn bộ, validate field nào có mặt |
| Admin create account | `AdminCreateUserAccountRequest` | `email`, `password`, `fullName`, `role` và tùy business thêm `phone` |
| Reset password | `AdminResetUserPasswordRequest` | `newPassword` + `userId` hoặc `authId` |

## 9. Error handling

## 9.1 Create account

Case:

- email đã tồn tại
- password yếu
- role không hợp lệ
- tạo auth user thành công nhưng insert profile thất bại

Cần hiển thị:

- message rõ ràng cho admin
- không chỉ show `UNKNOWN_ERROR`

## 9.2 Update profile

Case:

- user id không hợp lệ
- email trùng
- role không hợp lệ

## 9.3 Deactivate

Case:

- user đang là admin cuối cùng
- user có nghiệp vụ cần chặn deactivate

Business này nếu có thì nên check ở backend hoặc policy nghiệp vụ.

## 10. Test cases nên tự kiểm

## 10.1 Create

- tạo staff active thành công
- tạo customer inactive thành công
- email trùng báo lỗi
- password quá ngắn báo lỗi
- role không hợp lệ báo lỗi

## 10.2 Read

- load list customer
- load list staff
- search theo full name
- search theo email
- search theo phone
- filter active/inactive

## 10.3 Update

- sửa full name thành công
- sửa phone thành công
- đổi role customer -> staff thành công
- cập nhật request rỗng bị reject

## 10.4 Soft delete

- deactivate thành công
- activate lại thành công
- user inactive không vào app được sau login

## 10.5 Reset password

- reset thành công
- password mới yếu bị reject
- reset cho user không tồn tại bị reject

## 11. Trình tự triển khai nên làm

1. Hoàn thiện DTO và validation matrix
2. Hoàn thiện `UserRepository` cho list/detail/update/deactivate/activate
3. Tạo contract backend cho `createAccount` và `resetPassword`
4. Tạo `AdminUserAccountRepository`
5. Làm UI User List
6. Làm UI Create/Edit User
7. Làm User Detail + reset password dialog
8. Test luồng inactive sau login

## 12. Kết luận

CRUD hoàn chỉnh cho admin user management trong project này không phải chỉ là `UserRepository`.

Nó phải được chia làm 2 nhóm logic:

- Nhóm profile CRUD trong `public.users`
- Nhóm auth account management cho password và auth identity

Nếu giữ boundary này ngay từ đầu, code sẽ dễ đọc hơn, DTO đúng nghĩa hơn, và bạn sẽ tránh được lỗi thiết kế kiểu:

- dùng `UserCreateRequest` cho soft delete
- nhét password vào profile request
- cố dùng self-signup flow cho admin create account
