---
title: "Admin Update User Feature Guide"
description: "Hướng dẫn triển khai chi tiết tính năng admin cập nhật tài khoản client/staff"
audience: [developers, ai-agents]
tags: [implementation, admin, user, update, auth, supabase]
created: 2026-04-07
status: draft
---

# Admin Update User Feature Guide

## 1. Mục tiêu tính năng

Tính năng này cho phép admin cập nhật thông tin tài khoản `client/staff` đã tồn tại.

Phạm vi của feature này là:

- cập nhật profile nghiệp vụ trong `public.users`
- không xử lý reset password
- không xử lý tạo auth account mới

Những gì được phép sửa trong phase đầu:

- full name
- phone
- avatar URL
- role
- is active

Những gì không nên gộp vào feature này:

- password
- auth email
- auth metadata phức tạp

## 2. Boundary phải giữ

## 2.1 Feature update user thuộc tầng nào

Feature này thuộc boundary:

- `public.users`

Tức là nó nên chạy qua:

- `UserRepository`

Không nên chạy qua:

- `AuthClient`
- Supabase Admin Auth API

## 2.2 Khi nào update user không còn là profile update nữa

Nếu bạn muốn sửa:

- password
- email đăng nhập thật sự ở auth
- trạng thái auth-level

thì đó không còn là `update profile`.

Nó phải là feature khác, đi qua backend an toàn riêng.

## 3. DTO dùng cho feature này

Sử dụng:

- [`UserUpdateRequest.java`](C:\UTT\AppFood\FoodCouriers-Admin\app\src\main\java\com\utt\foodcouriers_admin\data\request\UserUpdateRequest.java)

DTO này phù hợp cho:

- patch từng field
- soft delete
- activate/deactivate

Không thêm password vào DTO này.

## 4. Fields cho phép cập nhật

## 4.1 Nên cho cập nhật trong phase đầu

- `full_name`
- `phone`
- `avatar_url`
- `role`
- `is_active`

## 4.2 Nên cân nhắc khóa trong phase đầu

- `email`

Lý do:

- email trong `public.users` có thể lệch với email thật ở `auth.users`
- nếu app cho sửa email profile nhưng không đồng bộ auth thì dễ gây sai nghĩa

Khuyến nghị:

- phase đầu hiển thị email dạng read-only
- nếu cần sửa email, làm feature riêng qua backend an toàn

## 4.3 Role mapping

Nếu UI vẫn dùng:

- `Client`
- `Staff`

thì map trước khi update:

- `Client` -> `customer`
- `Staff` -> `staff`

## 5. Input UI nên có

Form edit user nên có:

- full name
- phone
- email read-only hoặc editable tùy quyết định
- avatar URL
- role dropdown
- active switch

Buttons:

- save
- cancel

## 6. Validation nên làm

## 6.1 Validation ở UI

Chỉ validate những field admin được phép sửa:

- nếu full name editable thì không được rỗng
- nếu phone editable và required theo business thì không được rỗng
- nếu role editable thì phải có giá trị hợp lệ

Nếu cho sửa email:

- email không được rỗng
- email format hợp lệ

## 6.2 Validation ở repository

Repository phải validate theo kiểu patch:

- field nào có mặt thì validate field đó
- không ép toàn bộ field phải có

Ví dụ:

- request chỉ có `is_active = false` thì hợp lệ
- request chỉ có `full_name` thì chỉ validate `full_name`
- request có `phone = ""` thì reject

## 6.3 Validation business

Nên chốt rule:

- có cho đổi `staff -> customer` không
- có cho đổi `customer -> staff` không
- có cho đổi `admin` không

Khuyến nghị phase đầu:

- chỉ quản lý `customer/staff`
- không cho sửa `admin`

## 7. Flow xử lý chuẩn

## 7.1 Flow tổng quan

```text
User detail / Edit user screen
  -> load current profile
  -> admin chỉnh field
  -> validate input
  -> build UserUpdateRequest
  -> UserRepository.update(userId, request)
  -> PATCH public.users
  -> return updated profile
  -> refresh detail/list
```

## 7.2 Flow chi tiết

### Bước 1: Load dữ liệu hiện tại

Mở màn edit:

- gọi `getById(id)`
- fill dữ liệu hiện tại lên form

### Bước 2: Admin chỉnh thông tin

Admin chỉnh các field được phép.

### Bước 3: Build request patch

Chỉ set field thực sự cần update.

Ví dụ:

- nếu chỉ đổi role, request chỉ cần chứa `role`
- nếu chỉ khóa user, request chỉ cần `is_active = false`

### Bước 4: Gọi repository

Call:

```text
UserRepository.update(userId, request, callback)
```

### Bước 5: Repository validate

Logic validate đúng là:

- field nào != null thì validate field đó

### Bước 6: PATCH thành công

UI nên:

- đóng form
- show success
- reload detail/list

## 8. Method set nên có trong UserRepository

## 8.1 Các method chính

```text
getUsers(query, role, isActive, limit, offset, callback)
getById(id, callback)
update(id, request, callback)
deactivate(id, callback)
activate(id, callback)
```

## 8.2 Có nên giữ delete không

Có thể giữ:

```text
delete(id, callback)
```

nhưng chỉ là alias của:

```text
deactivate(id, callback)
```

Nếu muốn code dễ hiểu hơn, nên expose ra UI bằng `deactivate`.

## 9. Query và payload cập nhật

## 9.1 PATCH endpoint

```text
PATCH /rest/v1/users?id=eq.{id}
Prefer: return=representation
```

## 9.2 Payload ví dụ

### Update full name + phone

```json
{
  "full_name": "Pham Thi E",
  "phone": "0909988776"
}
```

### Update role

```json
{
  "role": "staff"
}
```

### Deactivate

```json
{
  "is_active": false
}
```

### Reactivate

```json
{
  "is_active": true
}
```

## 10. Email update decision

## 10.1 Vấn đề

Trong schema của bạn:

- `public.users.email` là profile email
- `auth.users.email` là login email

Nếu chỉ update `public.users.email`:

- UI có thể hiển thị email mới
- nhưng user vẫn login bằng email cũ ở auth

Khi đó dữ liệu gây hiểu nhầm.

## 10.2 Khuyến nghị

Phase đầu:

- không cho sửa email trong feature update profile

Nếu cần:

- hiển thị email read-only
- thêm note: email login được quản lý bởi auth system

## 10.3 Nếu business bắt buộc phải sửa email

Làm feature riêng:

- `AdminChangeUserEmailRequest`
- backend an toàn update cả auth + profile

Không gộp vào `UserRepository.update(...)`.

## 11. Activate / Deactivate logic

## 11.1 Deactivate

Flow:

1. admin bấm deactivate
2. show confirmation dialog
3. gọi `deactivate(userId)`
4. repository patch `is_active = false`
5. refresh UI

## 11.2 Activate

Flow:

1. admin bấm activate
2. gọi `activate(userId)`
3. repository patch `is_active = true`
4. refresh UI

## 11.3 Sau login cần check gì

Dù deactivate chỉ ở profile layer, app vẫn phải chặn user inactive sau login:

1. sign in thành công
2. fetch profile theo `auth_id`
3. nếu `is_active = false`
4. logout local
5. báo tài khoản bị vô hiệu hóa

## 12. UI logic nên triển khai

## 12.1 Edit screen state

State nên có:

- idle
- loading current user
- saving
- success
- error

## 12.2 Save button

Khi save:

- disable button
- show loading
- chặn submit nhiều lần

## 12.3 Dirty-check

Khuyến nghị:

- chỉ gọi update nếu có thay đổi thật

Nếu không có thay đổi:

- disable save
- hoặc show `No changes`

## 13. Test cases

## 13.1 Happy path

- update full name thành công
- update phone thành công
- update avatar URL thành công
- update role `customer -> staff` thành công
- deactivate thành công
- activate lại thành công

## 13.2 Validation fail

- update full name rỗng
- update phone rỗng khi business yêu cầu
- role invalid
- request null

## 13.3 Edge cases

- user id không tồn tại
- user đã inactive từ trước
- admin sửa user nhưng dữ liệu bị stale

## 13.4 Consistency

- sau update list reload đúng
- detail screen hiển thị đúng giá trị mới
- inactive user không vào app được sau login

## 14. Những quyết định cần chốt trước khi code

1. Có cho sửa email không
2. Có cho đổi role hai chiều không
3. Có cho chỉnh `admin` không
4. Có bắt phone là required khi edit không
5. Save có gửi patch tối thiểu hay luôn gửi full form

Khuyến nghị:

- email: không sửa
- admin: không sửa trong phase đầu
- patch tối thiểu: có

## 15. Trình tự triển khai nên làm

1. Chốt fields editable
2. Chốt business rule role
3. Hoàn thiện `UserUpdateRequest`
4. Hoàn thiện `UserRepository.update(...)`
5. Thêm `activate(...)` và `deactivate(...)`
6. Làm edit screen/dialog
7. Test update + activate/deactivate

## 16. Tóm tắt

Feature `update user` nên được giữ rất sạch:

- chỉ update profile ở `public.users`
- không đụng password
- không đụng auth email
- `deactivate` chỉ là một trường hợp riêng của `update`

Giữ boundary này sẽ làm code dễ đọc, dễ test và không lẫn logic auth vào CRUD profile.
