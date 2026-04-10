---
title: "Admin Create User Flow - Implementation and Supabase Setup"
description: "Tài liệu vận hành thực tế cho flow admin tạo client/staff bằng Android app + Supabase Edge Function"
audience: [developers, ai-agents]
tags: [android, supabase, auth, users, edge-function]
created: 2026-04-07
status: draft
---

# Admin Create User Flow - Implementation and Supabase Setup

## 1. Kết luận thực tế từ codebase và project Supabase

Flow `admin create user` trong app Android hiện đi theo kiến trúc đúng:

1. UI thu thập input tạo account
2. `AdminUserAccountRepository` gọi backend an toàn
3. Backend an toàn tạo `auth.users`
4. Backend an toàn insert `public.users`
5. App reload lại danh sách user qua `UserRepository`

Vấn đề production hiện tại không nằm ở UI form hay bảng `public.users`, mà ở chỗ project Supabase `xgpmxfujvjgebtohujgk` chưa có Edge Function `admin-user-accounts` được deploy.

## 2. Chuỗi class và method tham gia vào flow

## 2.1 Android UI layer

### `CreateUserDialogFragment`

File:
- `app/src/main/java/com/utt/foodcouriers_admin/ui/user/dialog/CreateUserDialogFragment.java`

Method tham gia:
- `setupRoleDropdown()`
  - Render role UI với 2 giá trị `Client` và `Staff`
- `submit()`
  - Map `Client -> customer`
  - Build `AdminCreateUserAccountRequest`
  - Gửi request ngược về `CreateUserListener`
- `validate()`
  - Validate đầy đủ các field bắt buộc ở UI:
    - `full_name`
    - `email`
    - `phone`
    - `password`
    - `confirmPassword`
    - `role`

### `UserFragment`

File:
- `app/src/main/java/com/utt/foodcouriers_admin/ui/user/UserFragment.java`

Method tham gia:
- `openCreateDialog()`
  - Mở dialog tạo user
- `handleCreateUser(AdminCreateUserAccountRequest request, CreateUserDialogFragment dialog)`
  - Gọi `adminUserAccountRepository.createAccount(...)`
  - Nhận callback thành công/thất bại
  - Nếu thành công thì chuyển tab phù hợp và reload list
- `loadUsers()`
  - Gọi `userRepository.getUsers(...)` để đồng bộ lại UI sau khi tạo account

## 2.2 Android request/repository layer

### `AdminCreateUserAccountRequest`

File:
- `app/src/main/java/com/utt/foodcouriers_admin/data/request/AdminCreateUserAccountRequest.java`

Field dùng trong flow create:
- `email`
- `password`
- `full_name`
- `phone`
- `avatar_url`
- `role`
- `is_active`
- `email_confirm`

Đây là request nghiệp vụ cho endpoint backend, không phải DTO insert trực tiếp vào `public.users`.

### `AdminUserAccountRepository`

File:
- `app/src/main/java/com/utt/foodcouriers_admin/data/repository/AdminUserAccountRepository.java`

Method tham gia:
- `createAccount(AdminCreateUserAccountRequest request, RepositoryCallback<User> callback)`
  - Validate cấu hình `ADMIN_API_BASE_URL`
  - Lấy access token từ `AuthClient`
  - Gửi `POST {ADMIN_API_BASE_URL}/admin-user-accounts`
  - Header:
    - `apikey: SUPABASE_ANON_KEY`
    - `Authorization: Bearer <admin_access_token>`
  - Parse response theo envelope `{ data: User }` hoặc object `User` trực tiếp

Method phụ:
- `resolveUrl(String path)`
  - Nối `ADMIN_API_BASE_URL` với path function
- `parseCreateAccountError(...)`
  - Chuyển lỗi 404 function sang message rõ nghĩa cho app
- `maskHeaders(...)`
  - Ẩn token trong log

### `UserRepository`

File:
- `app/src/main/java/com/utt/foodcouriers_admin/data/repository/UserRepository.java`

Method liên quan sau create:
- `getUsers(...)`
  - Tải lại danh sách `public.users`
- `update(...)`
  - Dùng cho sửa profile hoặc bật/tắt `is_active`
- `delete(...)`
  - Soft delete qua `is_active = false`

`UserRepository` không tạo `auth.users` và không nên xử lý password.

## 2.3 Android auth/session layer

### `AuthClient`

File:
- `app/src/main/java/com/utt/foodcouriers_admin/data/remote/AuthClient.java`

Method liên quan:
- `signIn(...)`
  - Login admin
  - Sau đó gọi `fetchUserProfile(authId, ...)`
- `fetchUserProfile(String authId, ApiCallback<User> callback)`
  - Từ `auth.users.id` lấy `public.users`
- `getAccessToken()`
  - Được `AdminUserAccountRepository` dùng để gọi backend an toàn

Lưu ý:
- `signUp(...)` và `createUserProfile(...)` trong `AuthClient` là self-signup flow cũ.
- Không dùng hai method này cho admin create client/staff.

### `SessionManager`

File:
- `app/src/main/java/com/utt/foodcouriers_admin/utils/SessionManager.java`

Method liên quan:
- `saveSession(...)`
- `getAccessToken()`
- `getUserRole()`
- `isAdmin()`

Session admin đang login là điều kiện bắt buộc để function backend xác thực quyền tạo user.

## 2.4 Build/runtime configuration

### `SupabaseConfig`

File:
- `app/src/main/java/com/utt/foodcouriers_admin/data/remote/SupabaseConfig.java`

Field chính:
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `ADMIN_API_BASE_URL`
- `AUTH_URL`
- `REST_URL`

### `app/build.gradle.kts`

File:
- `app/build.gradle.kts`

Runtime rule hiện tại:
- `ADMIN_API_BASE_URL` lấy từ `local.properties`
- nếu thiếu thì default:

```text
{SUPABASE_URL}/functions/v1
```

Nghĩa là function phải được deploy đúng tên:

```text
admin-user-accounts
```

để app gọi ra:

```text
https://<project-ref>.supabase.co/functions/v1/admin-user-accounts
```

## 3. Edge Function đã bổ sung vào repo

File mới:
- `supabase/functions/admin-user-accounts/index.ts`
- `supabase/functions/deno.json`

## 3.1 Nhiệm vụ của function

Function `admin-user-accounts` xử lý:

1. Nhận `POST` từ Android app
2. Validate JWT caller bằng access token admin hiện tại
3. Từ `public.users` xác minh caller có role `admin` và `is_active = true`
4. Validate payload tạo user
5. Map `client -> customer`
6. Dùng `service_role` gọi `auth.admin.createUser(...)`
7. Insert row vào `public.users`
8. Nếu insert profile fail thì xóa rollback auth user vừa tạo
9. Trả về envelope:

```json
{
  "message": "User account created successfully",
  "data": { ...user_row }
}
```

## 3.2 Những method/hàm chính trong function

### Entry handler
- `Deno.serve(async (req) => { ... })`

### Validate request
- `validatePayload(payload)`
  - kiểm tra email, password, full name, role

### Role normalization
- `normalizeRole(role)`
  - map `client -> customer`
  - chỉ cho phép `customer` hoặc `staff`

### Caller authentication
- `resolveCaller(userClient)`
  - dùng JWT của request để xác định admin đang gọi

### Caller authorization
- `ensureAdminCaller(serviceClient, authUserId)`
  - lookup `public.users`
  - chặn non-admin hoặc admin inactive

### Duplicate profile lookup
- `findExistingProfileByEmail(serviceClient, email)`
  - chặn trùng email trong `public.users`

### HTTP response helpers
- `jsonResponse(...)`
- `addCorsHeaders(...)`
- `corsHeaders(...)`

## 4. Setup trên Supabase

## 4.1 Secrets bắt buộc

Function cần các env sau trong Supabase:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_SERVICE_ROLE_KEY`

Hai biến đầu thường đã có sẵn trong môi trường function. Biến cần kiểm tra và set rõ là:

- `SUPABASE_SERVICE_ROLE_KEY`

## 4.2 Deploy function

Tên function phải đúng:

```text
admin-user-accounts
```

Vì app Android đã hardcode path:

```text
/admin-user-accounts
```

Nếu đổi tên function thì phải đổi cả app.

## 4.3 Verify JWT

Function phải bật `verify_jwt = true`.

Lý do:
- app gửi access token admin hiện tại
- function dùng token này để xác thực caller
- không chấp nhận anonymous create user

## 4.4 Kiểm tra sau deploy

Checklist trên Supabase:

1. Project URL đúng là:
   - `https://xgpmxfujvjgebtohujgk.supabase.co`
2. Edge Function có tên:
   - `admin-user-accounts`
3. Secret đã có:
   - `SUPABASE_SERVICE_ROLE_KEY`
4. Admin login hiện tại phải tồn tại trong:
   - `auth.users`
   - `public.users`
5. Admin profile phải có:
   - `role = 'admin'`
   - `is_active = true`

## 4.5 Payload mẫu để test

```json
{
  "email": "staff02@example.com",
  "password": "password123",
  "full_name": "Staff 02",
  "phone": "0901234567",
  "avatar_url": null,
  "role": "staff",
  "is_active": true,
  "email_confirm": true
}
```

Hoặc tạo client:

```json
{
  "email": "client01@example.com",
  "password": "password123",
  "full_name": "Client 01",
  "phone": "0901234568",
  "role": "client",
  "is_active": true,
  "email_confirm": true
}
```

Function sẽ tự map `client` thành `customer`.

## 5. Trạng thái schema thật đã kiểm tra

Project Supabase hiện tại có:

- bảng `auth.users`
- bảng `public.users`
- khóa ngoại:

```sql
public.users.auth_id -> auth.users.id
```

Constraint role thực tế của `public.users`:

```sql
CHECK (role = ANY (ARRAY['customer', 'staff', 'admin']))
```

Vì vậy:
- app label `Client`
- backend request có thể nhận `client`
- nhưng row insert cuối cùng phải là `customer`

## 6. Lý do 404 trước đây

Lỗi `404` không phải vì `auth.users` không tồn tại.

Lỗi thực tế là:
- app gọi `https://xgpmxfujvjgebtohujgk.supabase.co/functions/v1/admin-user-accounts`
- project chưa có function này
- nên request fail trước khi tới bước tạo auth user

## 7. Các lỗi runtime cần theo dõi tiếp

Sau khi deploy function, các lỗi còn có thể gặp:

### `401 Unauthorized`
- access token hết hạn
- app chưa login
- request không gửi `Authorization: Bearer ...`

### `403 Forbidden`
- caller không có role `admin`
- admin profile inactive
- admin có `auth.users` nhưng thiếu row ở `public.users`

### `409 Conflict`
- email đã tồn tại ở `public.users`
- hoặc auth báo duplicate email

### `500 Internal Server Error`
- thiếu `SUPABASE_SERVICE_ROLE_KEY`
- insert `public.users` fail
- rollback delete auth user fail

## 8. Khuyến nghị maintain tiếp theo

1. Giữ `AdminUserAccountRepository` chỉ gọi backend an toàn.
2. Không đưa `service_role` vào Android app.
3. Không dùng `AuthClient.signUp(...)` cho flow admin create.
4. Nếu làm tiếp reset password, nên dùng endpoint riêng như:
   - `POST /admin-user-accounts/reset-password`
5. Nếu làm deactivate chặn login hoàn toàn, cần thêm logic auth-level ngoài `public.users.is_active`.
