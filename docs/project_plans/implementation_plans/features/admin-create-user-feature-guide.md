---
title: "Admin Create User Feature Guide"
description: "Hướng dẫn triển khai chi tiết tính năng admin tạo tài khoản client/staff"
audience: [developers, ai-agents]
tags: [implementation, admin, user, create, auth, supabase]
created: 2026-04-07
status: draft
---

# Admin Create User Feature Guide

## 1. Mục tiêu tính năng

Tính năng này cho phép admin tạo mới tài khoản `client/staff` trong hệ thống.

Khi admin bấm tạo user, hệ thống phải tạo được đồng thời:

1. Auth account trong `Supabase Auth`
2. Profile record trong `public.users`

Tính năng này không phải chỉ là insert vào bảng `users`.

Nó là một flow nghiệp vụ gồm 2 phần:

- phần auth
- phần profile

## 2. Kết quả mong muốn sau khi hoàn thành

Admin có thể:

- nhập thông tin user mới
- nhập password tạm
- chọn role `client/staff`
- chọn active/inactive
- bấm tạo
- thấy user mới xuất hiện trong danh sách

Hệ thống phải đảm bảo:

- email login được tạo trong `auth.users`
- `public.users.auth_id` trỏ đúng tới `auth.users.id`
- profile lưu đúng role, phone, email, full name

## 3. Boundary phải giữ

## 3.1 App Admin làm gì

App admin chỉ làm:

- validate form cơ bản
- tạo `AdminCreateUserAccountRequest`
- gọi endpoint backend an toàn
- nhận kết quả trả về
- refresh list/detail

App admin không được:

- giữ `service_role`
- tự gọi Supabase Admin API trực tiếp
- tự tạo auth user từ mobile client

## 3.2 Backend an toàn làm gì

Backend hoặc Edge Function làm:

- xác thực admin hiện tại
- validate business
- tạo auth user
- tạo profile user
- rollback nếu lỗi giữa chừng

## 4. Input form ở app Admin

## 4.1 Fields nên có

Form create user nên có:

- full name
- email
- phone
- password tạm
- confirm password
- role
- active switch
- avatar URL nếu business cần

## 4.2 Role mapping

Nếu UI dùng nhãn:

- `Client`
- `Staff`

thì khi submit cần map:

- `Client` -> `customer`
- `Staff` -> `staff`

Không gửi `client` xuống DB nếu schema chưa hỗ trợ giá trị đó.

## 5. DTO dùng cho tính năng này

## 5.1 DTO ở app

Sử dụng:

- [`AdminCreateUserAccountRequest.java`](C:\UTT\AppFood\FoodCouriers-Admin\app\src\main\java\com\utt\foodcouriers_admin\data\request\AdminCreateUserAccountRequest.java)

Field đang phù hợp:

- `email`
- `password`
- `full_name`
- `phone`
- `avatar_url`
- `role`
- `is_active`
- `email_confirm`

DTO này là DTO nghiệp vụ cấp tính năng.

Nó không phải DTO cho bảng `public.users`.

## 5.2 DTO response nên mong đợi

App nên nhận về `BaseResponse<User>` hoặc JSON tương đương:

```json
{
  "success": true,
  "message": "User account created successfully",
  "data": {
    "id": "public-user-id",
    "auth_id": "auth-user-id",
    "full_name": "Tran Thi C",
    "phone": "0911222333",
    "email": "client01@example.com",
    "avatar_url": null,
    "role": "customer",
    "is_active": true,
    "created_at": "2026-04-07T09:00:00Z",
    "updated_at": "2026-04-07T09:00:00Z"
  }
}
```

## 6. Validation nên làm thế nào

## 6.1 Validation ở UI

Validate trước khi gọi API:

- full name không được rỗng
- email không được rỗng
- phone không được rỗng nếu business đang yêu cầu
- password không được rỗng
- confirm password không được rỗng
- password và confirm password phải khớp
- password tối thiểu 6 hoặc 8 ký tự
- role phải được chọn

Validation message nên rõ ràng:

- `Full name is required`
- `Email is required`
- `Phone is required`
- `Password is required`
- `Confirm password does not match`
- `Role is required`

## 6.2 Validation ở backend

Backend cần validate lại toàn bộ:

- email hợp lệ
- email chưa tồn tại trong auth
- role chỉ cho phép `customer/staff`
- password đúng policy
- phone format nếu business có rule
- admin hiện tại có quyền tạo user

## 6.3 Validation business

Nên chốt thêm:

- admin app không được tạo thêm `admin` nếu chưa có use case rõ ràng
- chỉ cho phép tạo `customer` hoặc `staff`

Khuyến nghị giai đoạn đầu:

- không cho tạo role `admin` từ UI

## 7. Flow xử lý chuẩn

## 7.1 Flow tổng quan

```text
CreateUserActivity / Dialog
  -> validate form
  -> build AdminCreateUserAccountRequest
  -> AdminUserAccountRepository.createAccount(request)
  -> secure backend endpoint
  -> create auth user
  -> insert public.users
  -> return created user profile
  -> UI success message
  -> refresh user list
```

## 7.2 Flow chi tiết từng bước

### Bước 1: User nhập form

Admin nhập:

- họ tên
- email
- phone
- password
- confirm password
- role
- trạng thái active

### Bước 2: UI validate

Nếu invalid:

- show lỗi tại field
- không call API

Nếu valid:

- map dữ liệu sang `AdminCreateUserAccountRequest`

### Bước 3: Repository app gọi backend

App gọi endpoint ví dụ:

`POST /admin/user-accounts`

### Bước 4: Backend xác thực quyền admin

Backend lấy JWT hiện tại và check:

- user đã login
- role là admin

### Bước 5: Backend tạo auth user

Backend gọi Supabase Admin API tương đương:

```text
auth.admin.createUser({
  email,
  password,
  email_confirm,
  user_metadata: {
    full_name,
    phone,
    role
  }
})
```

Kết quả nhận được:

- `authUser.id`

### Bước 6: Backend tạo profile ở `public.users`

Insert:

```text
auth_id = authUser.id
full_name = request.fullName
phone = request.phone
email = request.email
avatar_url = request.avatarUrl
role = mapped role
is_active = request.isActive
```

### Bước 7: Backend trả response

Nếu thành công:

- trả profile object đã tạo

Nếu thất bại:

- trả message rõ ràng

### Bước 8: UI xử lý success

Nên làm:

- đóng form
- show toast/snackbar
- reload list
- optionally scroll tới user mới tạo

## 8. Contract backend nên chốt

## 8.1 Endpoint

Khuyến nghị:

`POST /admin/user-accounts`

## 8.2 Request body

```json
{
  "email": "staff02@example.com",
  "password": "Temp@123",
  "full_name": "Le Van D",
  "phone": "0988111222",
  "avatar_url": null,
  "role": "staff",
  "is_active": true,
  "email_confirm": true
}
```

## 8.3 Response success

```json
{
  "success": true,
  "message": "User account created successfully",
  "data": {
    "id": "uuid",
    "auth_id": "uuid",
    "full_name": "Le Van D",
    "phone": "0988111222",
    "email": "staff02@example.com",
    "avatar_url": null,
    "role": "staff",
    "is_active": true
  }
}
```

## 8.4 Response error

Ví dụ:

```json
{
  "success": false,
  "message": "Email already registered",
  "error": {
    "code": "EMAIL_EXISTS",
    "message": "Email already registered"
  }
}
```

## 9. Rollback và consistency

## 9.1 Vấn đề cần xử lý

Case xấu:

1. tạo `auth.users` thành công
2. insert `public.users` thất bại

Khi đó dữ liệu bị lệch:

- auth user có
- profile không có

## 9.2 Cách xử lý nên chọn

### Cách A: cleanup auth user nếu insert profile fail

Ưu điểm:

- dữ liệu sạch

Nhược điểm:

- backend phải xử lý thêm bước delete auth user

### Cách B: log lỗi và trả thông báo để admin xử lý thủ công

Ưu điểm:

- dễ code hơn

Nhược điểm:

- dữ liệu lệch

Khuyến nghị:

- dùng Cách A

Flow:

1. create auth user
2. try insert profile
3. nếu insert fail:
   - delete auth user vừa tạo
   - trả lỗi về app

## 10. Các class/file nên có trong app

## 10.1 Request DTO

Đã có:

- `AdminCreateUserAccountRequest`

## 10.2 Repository mới

Nên tạo:

- `AdminUserAccountRepository`

Chức năng:

- gửi request tạo account tới backend
- parse response về `BaseResponse<User>`

Nó không thay thế `UserRepository`.

## 10.3 UI

Bạn có thể chọn 1 trong 2 kiểu:

### Cách 1: Activity riêng

- `CreateUserActivity`

Phù hợp nếu form dài.

### Cách 2: DialogFragment

- `CreateUserDialogFragment`

Phù hợp nếu bạn muốn mở nhanh từ danh sách user.

Khuyến nghị:

- nếu screen user management đơn giản, dùng `DialogFragment`
- nếu còn nhiều field hoặc step, dùng `Activity`

## 10.4 Listener/Callback

UI create user nên có callback:

- `onUserCreated(User user)`

Để list screen biết mà refresh.

## 11. Logic UI nên triển khai

## 11.1 Trạng thái nút submit

Khi đang tạo:

- disable nút save
- show loading
- chặn double click

Khi xong:

- enable lại nút nếu fail
- đóng form nếu success

## 11.2 Hiển thị lỗi

Ưu tiên:

- field validation lỗi thì set error trực tiếp lên field
- lỗi API thì show dialog/toast/snackbar

Ví dụ:

- `Email already registered`
- `You do not have permission to create users`
- `Password does not meet security policy`

## 11.3 Sau khi tạo thành công

Nên làm:

- clear form
- dismiss dialog
- show message
- gọi reload list

## 12. Pseudo implementation ở app

## 12.1 UI layer

```text
collectForm()
validateForm()
mapRoleForDatabase()
request = AdminCreateUserAccountRequest(...)
repository.createAccount(request, callback)
```

## 12.2 Repository layer

```text
validate request object != null
serialize request JSON
POST secure endpoint
parse User object response
return BaseResponse<User>
```

## 12.3 Success path

```text
show "User created successfully"
refresh list
```

## 12.4 Failure path

```text
show backend message
keep form opened
allow admin chỉnh dữ liệu rồi submit lại
```

## 13. Test cases cho create user

## 13.1 Happy path

- tạo `staff` active thành công
- tạo `customer` inactive thành công
- tạo user có avatar URL thành công

## 13.2 Validation fail ở UI

- thiếu full name
- thiếu email
- thiếu phone nếu field này required
- thiếu password
- confirm password không khớp
- chưa chọn role

## 13.3 Validation fail ở backend

- email đã tồn tại
- role không hợp lệ
- password quá yếu
- token admin không hợp lệ

## 13.4 Partial failure

- auth user tạo được nhưng profile insert fail
- backend rollback thành công
- app nhận error rõ ràng

## 14. Những quyết định bạn nên chốt trước khi code

1. `phone` là required hay optional
2. `email_confirm` mặc định là `true` hay `false`
3. Có cho tạo `admin` từ UI hay không
4. Sau khi tạo user có gửi password tạm cho người dùng bằng kênh khác không
5. Có bắt user đổi password ở lần đăng nhập đầu hay không

## 15. Khuyến nghị triển khai thực tế cho project này

Nếu mục tiêu là làm nhanh nhưng vẫn đúng kiến trúc:

### Phase 1

- làm UI create user
- làm `AdminUserAccountRepository`
- làm endpoint backend `POST /admin/user-accounts`
- tạo được `staff/customer`

### Phase 2

- thêm reset password
- thêm activate/deactivate riêng
- thêm business rule nâng cao

## 16. Tóm tắt

Tính năng `create user` trong app Admin phải được coi là một flow tạo account hoàn chỉnh:

- app admin thu thập dữ liệu
- backend tạo `auth.users`
- backend tạo `public.users`
- app chỉ nhận kết quả và refresh UI

Nếu bạn giữ đúng flow này, phần create user sẽ không bị nhầm sang CRUD profile thông thường.
