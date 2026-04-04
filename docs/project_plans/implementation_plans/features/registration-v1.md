# Registration Feature Implementation Plan

**Feature:** Registration & Authentication  
**Module:** Auth  
**Priority:** High  
**Estimate:** 3 days

## Overview

Triển khai tính năng đăng ký và đăng nhập cho ứng dụng Food Ordering sử dụng Supabase Auth.

## Tasks

### Task 1: Database Setup
- **Type:** database
- **Estimate:** 0.5d
- **Description:** Setup database trigger cho user creation
- **Action:**
  1. Tạo function `handle_new_user` trigger sau khi tạo auth user
  2. Function sẽ insert vào `public.users` table
  3. Setup RLS policies cho auth
- **Verify:** Auth user tự động tạo profile trong users table

### Task 2: Supabase Auth Client Setup
- **Type:** android
- **Estimate:** 0.5d
- **Description:** Setup Supabase client trong Android app
- **Action:**
  1. Thêm dependency supabase-kt
  2. Khởi tạo SupabaseClient với URL và anon key
  3. Configure auth settings
- **Verify:** Client có thể kết nối Supabase

### Task 3: Registration UI
- **Type:** android
- **Estimate:** 1d
- **Description:** Tạo màn hình đăng ký
- **Action:**
  1. Tạo RegisterActivity/Fragment
  2. Form fields: email, phone, password, full_name
  3. Input validation
  4. Gọi Supabase signUp API
  5. Xử lý loading state và errors
- **Verify:** User có thể đăng ký thành công

### Task 4: Login UI
- **Type:** android
- **Estimate:** 0.5d
- **Description:** Tạo màn hình đăng nhập
- **Action:**
  1. Tạo LoginActivity/Fragment
  2. Form fields: email/phone, password
  3. Remember me option
  4. Gọi Supabase signIn API
- **Verify:** User có thể đăng nhập

### Task 5: Password Reset
- **Type:** android
- **Estimate:** 0.5d
- **Description:** Tính năng quên mật khẩu
- **Action:**
  1. Tạo màn hình nhập email
  2. Gọi resetPasswordForEmail
  3. Tạo màn hình đặt lại password
- **Verify:** User có thể reset password

### Task 6: Session Management
- **Type:** android
- **Estimate:** 0.5d
- **Description:** Quản lý session và token
- **Action:**
  1. Lưu session vào secure storage
  2. Implement auto-refresh token
  3. Handle session expiration
  4. Logout functionality
- **Verify:** App nhớ đăng nhập sau khi tắt app

### Task 7: Email Verification
- **Type:** android
- **Estimate:** 0.5d
- **Description:** Xử lý email verification
- **Action:**
  1. Handle deep link từ email
  2. Show verification status
  3. Resend verification email option
- **Verify:** Email verification hoạt động

## Technical Implementation

### Database Trigger

```sql
-- Function to create user profile after auth signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.users (auth_id, email, phone, full_name, role)
    VALUES (
        NEW.id,
        NEW.email,
        NEW.raw_user_meta_data->>'phone',
        NEW.raw_user_meta_data->>'full_name',
        COALESCE(NEW.raw_user_meta_data->>'role', 'customer')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
```

### Android Data Layer

```kotlin
// SupabaseClient setup
val supabase = createSupabaseClient(
    supabaseUrl = "https://xxx.supabase.co",
    supabaseKey = "anon_key"
) {
    install(GoTrue)
    install(Auth)
}

// Registration
suspend fun register(
    email: String,
    phone: String,
    password: String,
    fullName: String
): AuthResult {
    return supabase.auth.signUpWith(
        email = email,
        password = password,
        options = {
            data = mapOf(
                "phone" to phone,
                "full_name" to fullName
            )
        }
    )
}
```

## Dependencies

```
Task 1 → Task 2 → Task 3 → Task 4
                      ↓
                 Task 5
Task 4 → Task 6 → Task 7
```

## Verification Checklist

- [ ] Auth user tự động tạo profile trong users table
- [ ] User có thể đăng ký với email + phone
- [ ] Email verification được gửi sau đăng ký
- [ ] User có thể đăng nhập bằng email/phone
- [ ] User có thể reset password
- [ ] Session được lưu và refresh tự động
- [ ] Logout xóa toàn bộ session

## Error Messages

| Scenario | Message |
|----------|---------|
| Email exists | "Email đã được sử dụng" |
| Phone exists | "Số điện thoại đã được sử dụng" |
| Invalid email | "Email không hợp lệ" |
| Weak password | "Mật khẩu phải có ít nhất 8 ký tự" |
| Network error | "Không có kết nối mạng" |
| Verification failed | "Xác thực email thất bại, vui lòng thử lại" |
