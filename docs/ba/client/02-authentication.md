# Module: Xác thực - App Client

## 1. Overview
Module xử lý đăng nhập, đăng ký, quên mật khẩu cho khách hàng.

## 2. Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | Login | Đăng nhập bằng email + password |
| 2 | Register | Đăng ký tài khoản mới |
| 3 | Password Reset | Quên mật khẩu qua email |
| 4 | Session Management | Lưu JWT, auto refresh |
| 5 | Logout | Đăng xuất |

## 3. User Flow

```
┌──────────────────────────────────────────────────────────────┐
│                    LOGIN FLOW                                 │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│   User enters email + password                               │
│            │                                                 │
│            ▼                                                 │
│   Validate: email not empty, password min 6 chars           │
│            │                                                 │
│            ▼                                                 │
│   Call Supabase Auth: signInWithPassword                    │
│            │                                                 │
│      ┌─────┴─────┐                                           │
│      │           │                                           │
│   Success      Error                                         │
│      │           │                                           │
│      ▼           ▼                                           │
│   Get user    Show error                                     │
│   profile     message                                        │
│      │           │                                           │
│      ▼           │                                           │
│   Save token   │                                             │
│   to prefs     │                                             │
│      │           │                                           │
│      ▼           │                                           │
│   Navigate to                                                 │
│   MainActivity                                                │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

## 4. API Calls

### 4.1 Login
```java
supabase.auth.signInWithPassword(email, password)
```

### 4.2 Register
```java
supabase.auth.signUp(email, password)
```

### 4.3 Reset Password
```java
supabase.auth.resetPasswordForEmail(email)
```

## 5. UI Components

### 5.1 Login (activity_login.xml)
```xml
<LinearLayout android:orientation="vertical">
    <ImageView android:src="@logo"/>
    <TextView android:text="FoodDelivery"/>
    
    <com.google.android.material.textfield.TextInputLayout>
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etEmail"
            android:inputType="textEmailAddress"/>
    </com.google.android.material.textfield.TextInputLayout>
    
    <com.google.android.material.textfield.TextInputLayout>
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etPassword"
            android:inputType="textPassword"/>
    </com.google.android.material.textfield.TextInputLayout>
    
    <Button android:id="@+id/btnLogin"/>
    <TextView android:id="@+id/tvForgotPassword"/>
    <TextView android:id="@+id/tvRegister"/>
</LinearLayout>
```

### 5.2 Register (activity_register.xml)
```xml
<LinearLayout>
    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/tilFullName">
        <EditText android:id="@+id/etFullName"/>
    </com.google.android.material.textfield.TextInputLayout>
    
    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/tilEmail">
        <EditText android:id="@+id/etEmail"
            android:inputType="textEmailAddress"/>
    </com.google.android.material.textfield.TextInputLayout>
    
    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/tilPhone">
        <EditText android:id="@+id/etPhone"
            android:inputType="phone"/>
    </com.google.android.material.textfield.TextInputLayout>
    
    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/tilPassword">
        <EditText android:id="@+id/etPassword"
            android:inputType="textPassword"/>
    </com.google.android.material.textfield.TextInputLayout>
    
    <Button android:id="@+id/btnRegister"/>
    <TextView android:id="@+id/tvLogin"/>
</LinearLayout>
```

## 6. ViewModel

```java
public class AuthViewModel extends ViewModel {
    private MutableLiveData<AuthResult> authResult = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public void login(String email, String password) { }
    public void register(String fullName, String email, String phone, String password) { }
    public void resetPassword(String email) { }
    public void logout() { }
    public void checkSession() { }
}
```

## 7. Session Management

```
App Launch
    │
    ▼
Check stored token
    │
    ├─▶ Token exists:
    │       Validate token (not expired)
    │       │
    │       ├─▶ Valid: Get user profile, navigate to Main
    │       │
    │       └─▶ Expired: Try refresh token
    │               │
    │               ├─▶ Success: Navigate to Main
    │               └─▶ Fail: Navigate to Login
    │
    └─▶ No token: Navigate to Login
```

## 8. Edge Cases

| Case | Handling |
|------|----------|
| Invalid email format | Show "Email không hợp lệ" |
| Password < 6 chars | Show "Mật khẩu tối thiểu 6 ký tự" |
| Email already exists | Show "Email đã được sử dụng" |
| Network error | Show "Không có kết nối mạng" |
| Token expired | Auto logout, redirect to login |