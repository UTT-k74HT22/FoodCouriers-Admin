# Sơ đồ - Module Xác thực - App Admin

## 1. Login Sequence Diagram

```
┌─────────┐     ┌────────────┐     ┌──────────────┐     ┌─────────┐     ┌─────────┐
│  User   │     │    UI      │     │  ViewModel   │     │Repository│    │Supabase │
│         │     │            │     │              │     │         │     │  Auth   │
└────┬────┘     └─────┬──────┘     └──────┬───────┘     └────┬────┘     └────┬────┘
     │               │                   │                  │              │
     │  Enter email  │                   │                  │              │
     │  + password   │                   │                  │              │
     │──────────────▶│                   │                  │              │
     │               │                   │                  │              │
     │               │  validate()       │                  │              │
     │               │──────────────────▶│                  │              │
     │               │                   │                  │              │
     │               │                   │  login()         │              │
     │               │                   │─────────────────▶│              │
     │               │                   │                  │              │
     │               │                   │                  │  signIn()   │
     │               │                   │                  │─────────────▶│
     │               │                   │                  │              │
     │               │                   │                  │              │ JWT Token
     │               │                   │                  │              │◀─────────
     │               │                   │                  │              │
     │               │                   │  Success         │              │
     │               │                   │◀─────────────────│              │
     │               │                   │                  │              │
     │               │  LiveData(Result) │                  │              │
     │               │◀──────────────────│                  │              │
     │               │                   │                  │              │
     │  Success      │                   │                  │              │
     │◀──────────────│                   │                  │              │
     │               │                   │                  │              │
     │               │                   │                  │              │
     │  Navigate to  │                   │                  │              │
     │  Main        │                   │                  │              │
     └───────────────┴───────────────────┴──────────────────┴──────────────┘
```

**Code Import:**

```java
// ViewModel
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.supabase.SupabaseClient;
import com.supabase.gotrue.Gotrue;
import com.supabase.gotrue.models.Session;

// AuthRepository
import com.supabase.gotrue.models.User;
import java.util.concurrent.ExecutorService;

// Activity
import androidx.lifecycle.ViewModelProvider;
import android.content.Intent;
```

---

## 2. Session Management Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     SESSION MANAGEMENT FLOW                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                         APP LAUNCH                                           │
│                              │                                               │
│                              ▼                                               │
│                    ┌──────────────────┐                                     │
│                    │ Check stored    │                                     │
│                    │ JWT token       │                                     │
│                    └────────┬─────────┘                                     │
│                             │                                               │
│                   ┌────────┴────────┐                                      │
│                   │                 │                                      │
│               Token exists      No token                                    │
│                   │                 │                                       │
│                   ▼                 ▼                                       │
│            ┌────────────┐    ┌──────────────┐                               │
│            │ Validate  │    │  Navigate    │                               │
│            │ token     │    │  to Login    │                               │
│            └─────┬──────┘    └──────────────┘                               │
│                  │                                                        │
│           ┌──────┴──────┐                                                  │
│           │             │                                                  │
│        Valid        Expired                                               │
│           │             │                                                  │
│           ▼             ▼                                                  │
│    ┌───────────┐  ┌──────────────┐                                          │
│    │  Get user │  │ Try refresh │                                          │
│    │  profile  │  │   token     │                                          │
│    └─────┬─────┘  └──────┬───────┘                                          │
│          │              │                                                   │
│          ▼              ▼                                                   │
│   ┌───────────┐  ┌──────────────┐                                           │
│   │  Success  │  │   Success   │                                           │
│   │           │  │              │                                           │
│   │  Navigate │  │  Navigate   │                                           │
│   │  to Main  │  │  to Main    │                                           │
│   └───────────┘  └──────────────┘                                           │
│                        │                                                   │
│                        ▼                                                   │
│                   ┌──────────────┐                                           │
│                   │    Fail     │                                           │
│                   │  (logout)    │                                           │
│                   │  to Login   │                                           │
│                   └──────────────┘                                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Code Import:**

```java
// SessionManager.java
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

// AuthViewModel
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.supabase.gotrue.gotrue_callbacks.Callback;

// Token refresh
import com.supabase.gotrue.requests.RefreshSessionRequest
```

---

## 3. Role-Based Navigation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    ROLE-BASED NAVIGATION FLOW                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    LOGIN SUCCESS                                    │  │
│   │                      Get user role                                   │  │
│   └───────────────────────────────┬─────────────────────────────────────┘  │
│                                    │                                         │
│                          ┌─────────┴─────────┐                              │
│                          │                   │                              │
│                          ▼                   ▼                              │
│                   ┌────────────┐      ┌────────────┐                       │
│                   │   ADMIN    │      │   STAFF    │                       │
│                   │   role     │      │   role     │                       │
│                   └─────┬──────┘      └─────┬──────┘                       │
│                         │                    │                               │
│                         ▼                    ▼                              │
│                   ┌────────────┐      ┌────────────┐                       │
│                   │ AdminMain  │      │ StaffMain  │                       │
│                   │   Activity │      │   Activity │                       │
│                   └─────┬──────┘      └─────┬──────┘                       │
│                         │                    │                               │
│                         ▼                    ▼                              │
│                   ┌────────────┐      ┌────────────┐                       │
│                   │Navigation  │      │Navigation  │                       │
│                   │  Drawer    │      │  Drawer    │                       │
│                   │            │      │            │                       │
│                   │ • Dashboard│      │ • Dashboard│                       │
│                   │ • Orders   │      │ • Orders   │                       │
│                   │ • Restaurants│    │ • Menu     │                       │
│                   │ • Categories│     └────────────┘                       │
│                   │ • Menu     │                                           │
│                   │ • Users    │                                           │
│                   │ • Reports  │                                           │
│                   │ • Settings │                                           │
│                   └────────────┘                                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Code Import:**

```java
// AdminMainActivity.java
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;

// Role check
import com.supabase.gotrue.models.User;

// Menu management
import android.view.SubMenu;
```

---

## 4. Error Handling Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     ERROR HANDLING FLOW                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Supabase Auth API ──▶ Response                                            │
│         │                                                                   │
│         ▼                                                                   │
│   ┌─────────────┬─────────────┬─────────────┬─────────────┐                │
│   │             │             │             │             │                │
│   ▼             ▼             ▼             ▼             ▼                │
│ Invalid   Account    Network   Token      Server      Success             │
│ credentials disabled  error   expired    error         │                   │
│   │          │          │        │          │            │                   │
│   ▼          ▼          ▼        ▼          ▼            ▼                   │
│ "Sai mật    "Tài      "Không   "Phiên    "Lỗi        Save                  │
│ khẩu"     khoản bị   có kết   hết hạn,   máy chủ"   token                  │
│           vô hiệu   nối mạng  đăng nhập  {msg}        +                    │
│           hóa"                   lại"                 Navigate             │
│                                                                   to Main    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Code Import:**

```java
// Error handling
import retrofit2.Response;
import retrofit2.HttpException;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;

// Custom exceptions
public class AuthException extends Exception {
    private final AuthErrorType type;
    public AuthException(String message, AuthErrorType type) {
        super(message);
        this.type = type;
    }
}

public enum AuthErrorType {
    INVALID_CREDENTIALS,
    ACCOUNT_DISABLED,
    NETWORK_ERROR,
    TOKEN_EXPIRED,
    SERVER_ERROR
}
┌─────────┐     ┌────────────┐     ┌──────────────┐     ┌─────────┐     ┌─────────┐
│  User   │     │    UI      │     │  ViewModel   │     │Repository│    │Supabase │
│         │     │            │     │              │     │         │     │  Auth   │
└────┬────┘     └─────┬──────┘     └──────┬───────┘     └────┬────┘     └────┬────┘
     │               │                   │                  │              │
     │  Enter email  │                   │                  │              │
     │  + password   │                   │                  │              │
     │──────────────▶│                   │                  │              │
     │               │                   │                  │              │
     │               │  validate()       │                  │              │
     │               │──────────────────▶│                  │              │
     │               │                   │                  │              │
     │               │                   │  login()         │              │
     │               │                   │─────────────────▶│              │
     │               │                   │                  │              │
     │               │                   │                  │  signIn()   │
     │               │                   │                  │─────────────▶│
     │               │                   │                  │              │
     │               │                   │                  │              │ JWT Token
     │               │                   │                  │              │◀─────────
     │               │                   │                  │              │
     │               │                   │  Success         │              │
     │               │                   │◀─────────────────│              │
     │               │                   │                  │              │
     │               │  LiveData(Result) │                  │              │
     │               │◀──────────────────│                  │              │
     │               │                   │                  │              │
     │  Success      │                   │                  │              │
     │◀──────────────│                   │                  │              │
     │               │                   │                  │              │
     │               │                   │                  │              │
     │  Navigate to  │                   │                  │              │
     │  Main        │                   │                  │              │
     └───────────────┴───────────────────┴──────────────────┴──────────────┘
```

## 2. Session Management Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     SESSION MANAGEMENT FLOW                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                         APP LAUNCH                                           │
│                              │                                               │
│                              ▼                                               │
│                    ┌──────────────────┐                                     │
│                    │ Check stored    │                                     │
│                    │ JWT token       │                                     │
│                    └────────┬─────────┘                                     │
│                             │                                               │
│                   ┌────────┴────────┐                                      │
│                   │                 │                                      │
│               Token exists      No token                                    │
│                   │                 │                                       │
│                   ▼                 ▼                                       │
│            ┌────────────┐    ┌──────────────┐                               │
│            │ Validate  │    │  Navigate    │                               │
│            │ token     │    │  to Login    │                               │
│            └─────┬──────┘    └──────────────┘                               │
│                  │                                                        │
│           ┌──────┴──────┐                                                  │
│           │             │                                                  │
│        Valid        Expired                                               │
│           │             │                                                  │
│           ▼             ▼                                                  │
│    ┌───────────┐  ┌──────────────┐                                          │
│    │  Get user │  │ Try refresh │                                          │
│    │  profile  │  │   token     │                                          │
│    └─────┬─────┘  └──────┬───────┘                                          │
│          │              │                                                   │
│          ▼              ▼                                                   │
│   ┌───────────┐  ┌──────────────┐                                           │
│   │  Success  │  │   Success   │                                           │
│   │           │  │              │                                           │
│   │  Navigate │  │  Navigate   │                                           │
│   │  to Main  │  │  to Main    │                                           │
│   └───────────┘  └──────────────┘                                           │
│                        │                                                   │
│                        ▼                                                   │
│                   ┌──────────────┐                                           │
│                   │    Fail     │                                           │
│                   │  (logout)    │                                           │
│                   │  to Login   │                                           │
│                   └──────────────┘                                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 3. Role-Based Navigation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    ROLE-BASED NAVIGATION FLOW                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    LOGIN SUCCESS                                    │  │
│   │                      Get user role                                   │  │
│   └───────────────────────────────┬─────────────────────────────────────┘  │
│                                    │                                         │
│                          ┌─────────┴─────────┐                              │
│                          │                   │                              │
│                          ▼                   ▼                              │
│                   ┌────────────┐      ┌────────────┐                       │
│                   │   ADMIN    │      │   STAFF    │                       │
│                   │   role     │      │   role     │                       │
│                   └─────┬──────┘      └─────┬──────┘                       │
│                         │                    │                               │
│                         ▼                    ▼                              │
│                   ┌────────────┐      ┌────────────┐                       │
│                   │ AdminMain  │      │ StaffMain  │                       │
│                   │   Activity │      │   Activity │                       │
│                   └─────┬──────┘      └─────┬──────┘                       │
│                         │                    │                               │
│                         ▼                    ▼                              │
│                   ┌────────────┐      ┌────────────┐                       │
│                   │Navigation  │      │Navigation  │                       │
│                   │  Drawer    │      │  Drawer    │                       │
│                   │            │      │            │                       │
│                   │ • Dashboard│      │ • Dashboard│                       │
│                   │ • Orders   │      │ • Orders   │                       │
│                   │ • Restaurants│    │ • Menu     │                       │
│                   │ • Categories│     └────────────┘                       │
│                   │ • Menu     │                                           │
│                   │ • Users    │                                           │
│                   │ • Reports  │                                           │
│                   │ • Settings │                                           │
│                   └────────────┘                                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 4. Error Handling Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     ERROR HANDLING FLOW                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Supabase Auth API ──▶ Response                                            │
│         │                                                                   │
│         ▼                                                                   │
│   ┌─────────────┬─────────────┬─────────────┬─────────────┐                │
│   │             │             │             │             │                │
│   ▼             ▼             ▼             ▼             ▼                │
│ Invalid   Account    Network   Token      Server      Success             │
│ credentials disabled  error   expired    error         │                   │
│   │          │          │        │          │            │                   │
│   ▼          ▼          ▼        ▼          ▼            ▼                   │
│ "Sai mật    "Tài      "Không   "Phiên    "Lỗi        Save                  │
│ khẩu"     khoản bị   có kết   hết hạn,   máy chủ"   token                  │
│           vô hiệu   nối mạng  đăng nhập  {msg}        +                    │
│           hóa"                   lại"                 Navigate             │
│                                                                   to Main    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```