# Sơ đồ - Module Lịch sử đơn hàng - App Client

## 1. Order History Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       ORDER HISTORY FLOW                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────┐     ┌─────────────┐     ┌─────────────┐                  │
│   │  Order      │     │   Order     │     │   Order     │                  │
│   │  History    │     │  ViewModel  │     │ Repository  │                  │
│   │  Fragment   │     └──────┬──────┘     └──────┬──────┘                  │
│   └──────┬──────┘            │                   │                          │
│          │                   │                   │                          │
│          │    onViewCreated()│                   │                          │
│          │──────────────────▶│                   │                          │
│          │                   │                   │                          │
│          │                   │   loadOrders()    │                          │
│          │                   │   param: tab      │                          │
│          │                   │───────────────────▶│                          │
│          │                   │                   │                          │
│          │                   │                   │   GET /rest/v1/orders  │
│          │                   │                   │   ?user_id=eq.{userId} │
│          │                   │                   │   &status=eq.{status}  │
│          │                   │                   │   &order=created_at    │
│          │                   │                   │   .desc                 │
│          │                   │                   │────────────────────────▶│
│          │                   │                   │                          │
│          │                   │                   │   Response: List<Order> │
│          │                   │                   │◀────────────────────────│
│          │                   │                   │                          │
│          │                   │   LiveData<List<Order>>                     │
│          │                   │◀──────────────────│                          │
│          │                   │                   │                          │
│          │   Update RecyclerView with adapter       │                          │
│          │◀─────────────────────────────────────────│                          │
│          │                   │                   │                          │
│                                                                             │
│   TAB FILTERING:                                                           │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                                                                     │  │
│   │   Tab "Tất cả"       ──▶ loadOrders("all")                        │  │
│   │   Tab "Đang xử lý"   ──▶ loadOrders("processing")                │  │
│   │                       (pending + confirmed + preparing + delivering)│  │
│   │   Tab "Hoàn thành"   ──▶ loadOrders("delivered")                 │  │
│   │   Tab "Đã hủy"       ──▶ loadOrders("cancelled")                 │  │
│   │                                                                     │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Code Import:**

```java
// OrderHistoryViewModel.java
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.supabase.SupabaseClient;
import com.supabase.postgrest.requests.SelectRequest;
import java.util.List;
import java.util.Map;

// OrderHistoryFragment
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.tabs.TabLayout;
import android.os.Bundle;
import android.view.View;

// OrderAdapter
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

// Tab filtering
import java.util.EnumMap;
import java.util.EnumSet;
```

---

## 2. Order Detail Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      ORDER DETAIL FLOW                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   User taps order item in list                                              │
│          │                                                                   │
│          ▼                                                                   │
│   Start OrderDetailActivity with orderId                                    │
│          │                                                                   │
│          ▼                                                                   │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    LOAD ORDER DETAIL                                │  │
│   │                                                                     │  │
│   │   parallel calls:                                                   │  │
│   │   ┌──────────────────────────────────────────────────────────────┐ │  │
│   │   │ GET /rest/v1/orders?id=eq.{orderId}                          │ │  │
│   │   │   &select=*,restaurant:restaurants(*),user:users(*)         │ │  │
│   │   └──────────────────────────────────────────────────────────────┘ │  │
│   │   ┌──────────────────────────────────────────────────────────────┐ │  │
│   │   │ GET /rest/v1/order_items?order_id=eq.{orderId}              │ │  │
│   │   └──────────────────────────────────────────────────────────────┘ │  │
│   │   ┌──────────────────────────────────────────────────────────────┐ │  │
│   │   │ GET /rest/v1/order_status_logs?order_id=eq.{orderId}        │ │  │
│   │   │ &order=created_at.asc                                       │ │  │
│   │   └──────────────────────────────────────────────────────────────┘ │  │
│   │                                                                     │  │
│   └────────────────────────────────────┬────────────────────────────────┘  │
│                                        │                                     │
│                                        ▼                                     │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    DISPLAY INFORMATION                              │  │
│   │                                                                     │  │
│   │   Order Header:                                                    │  │
│   │   • Order code (ORD001)                                           │  │
│   │   • Status (with color + icon)                                    │  │
│   │   • Created date                                                  │  │
│   │                                                                     │  │
│   │   Restaurant Info:                                                │  │
│   │   • Restaurant name, address, phone                               │  │
│   │                                                                     │  │
│   │   Order Items:                                                     │  │
│   │   • List of items with quantity, price                            │  │
│   │                                                                     │  │
│   │   Summary:                                                         │  │
│   │   • Subtotal, delivery fee, discount, total                      │  │
│   │                                                                     │  │
│   │   Delivery:                                                        │  │
│   │   • Address, note                                                 │  │
│   │                                                                     │  │
│   │   Status Timeline:                                                 │  │
│   │   • Visual timeline with all status changes                      │  │
│   │                                                                     │  │
│   └────────────────────────────────────┬────────────────────────────────┘  │
│                                        │                                     │
│                                        ▼                                     │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    ACTION BUTTONS                                   │  │
│   │                                                                     │  │
│   │   status = pending OR confirmed:                                   │  │
│   │   ├── [Hủy đơn] ──▶ Show cancel dialog                            │  │
│   │   │       │                                                        │  │
│   │   │       │  Call RPC: rpc_update_order_status                    │  │
│   │   │       │  with status = 'cancelled'                           │  │
│   │   │       │                                                        │  │
│   │   │       ▼                                                        │  │
│   │   │  Refresh order detail, show success                          │  │
│   │   │                                                        │  │
│   │   └───────────────────────────────────────────────────────────     │  │
│   │                                                                     │  │
│   │   status = delivered AND not reviewed:                            │  │
│   │   └── [Đánh giá] ──▶ Show rating dialog                           │  │
│   │           │                                                        │  │
│   │           │  POST /rest/v1/reviews                                 │  │
│   │           │  {order_id, rating, comment}                         │  │
│   │           │                                                        │  │
│   │           ▼                                                        │  │
│   │      Show success, hide button                                     │  │
│   │                                                                     │  │
│   │   ─────────────────────────────────────────────────────────────     │  │
│   │                                                                     │  │
│   │   [Đặt lại] ──▶ Add all items to cart ──▶ Navigate to cart     │  │
│   │                                                                     │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 3. Cancel Order Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       CANCEL ORDER FLOW                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   User taps "Hủy đơn"                                                        │
│          │                                                                   │
│          ▼                                                                   │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                 SHOW CANCEL DIALOG                                  │  │
│   │                                                                     │  │
│   │   ┌─────────────────────────────────────────────────────────────┐  │  │
│   │   │                                                              │  │  │
│   │   │  Bạn có chắc chắn muốn hủy đơn hàng?                        │  │  │
│   │   │                                                              │  │  │
│   │   │  Lý do hủy (tùy chọn):                                      │  │  │
│   │   │  ┌──────────────────────────────────────────────────────┐ │  │  │
│   │   │  │ [Đổi ý không muốn đặt nữa            ]                │ │  │  │
│   │   │  │ [Nhà hàng giao chậm               ]                │ │  │  │
│   │   │  │ [Sản phẩm không đúng như hình     ]                │ │  │  │
│   │   │  │ [Khác...                          ]                │ │  │  │
│   │   │  └──────────────────────────────────────────────────────┘ │  │  │
│   │   │                                                              │  │  │
│   │   │  Hoặc nhập lý do khác:                                      │  │  │
│   │   │  [________________________________________]                │  │  │
│   │   │                                                              │  │  │
│   │   └────────────────────────────────────────────────────────────┘  │  │
│   │                                                                     │  │
│   │   [Không]                                   [Xác nhận hủy]        │  │
│   │                                                                     │  │
│   └────────────────────────────────────┬────────────────────────────────┘  │
│                                        │                                     │
│                                        ▼                                     │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    RPC: UPDATE ORDER STATUS                         │  │
│   │                                                                     │  │
│   │   SELECT rpc_update_order_status(                                  │  │
│   │       p_order_id := 'order_uuid',                                  │  │
│   │       p_new_status := 'cancelled',                                 │  │
│   │       p_changed_by := 'user_uuid',                                 │  │
│   │       p_note := 'Customer cancelled: {reason}'                    │  │
│   │   );                                                               │  │
│   │                                                                     │  │
│   │   Side effects in database:                                        │  │
│   │   • Update orders.status = 'cancelled'                            │  │
│   │   • Insert order_status_log (cancelled ← {prev_status})          │  │
│   │   • Create notification for user                                  │  │
│   │   • Update promotion usage (if applicable)                        │  │
│   │                                                                     │  │
│   └────────────────────────────┬────────────────────────────────────────┘  │
│                                 │                                          │
│                    ┌────────────┴────────────┐                              │
│                    │                         │                              │
│                SUCCESS                     ERROR                           │
│                    │                         │                              │
│                    ▼                         ▼                              │
│   ┌────────────────────────┐  ┌────────────────────────────────────────┐   │
│   │ • Refresh detail       │  │ • Show error message                  │   │
│   │ • Show success         │  │ • Keep current status                │   │
│   │   "Đã hủy đơn hàng"    │  │                                       │   │
│   │ • Update list         │  │                                       │   │
│   └────────────────────────┘  └────────────────────────────────────────┘   │
│                                                                             │
│   CANCEL ALLOWED FROM:                                                      │
│   ═══════════════════                                                       │
│                                                                             │
│   pending ──▶ cancelled ✓                                                  │
│   confirmed ──▶ cancelled ✓                                                 │
│   preparing ──▶ cancelled ✗ (already being prepared)                       │
│   delivering ──▶ cancelled ✗ (already on the way)                          │
│   delivered ──▶ cancelled ✗ (already completed)                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Code Import:**

```java
// Order Detail Activity
import android.os.Bundle;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

// Status Timeline
import android.widget.LinearLayout;
import android.widget.ImageView;
import java.util.List;
import java.util.ArrayList;

// Cancel Dialog
import androidx.appcompat.app.AlertDialog;
import android.widget.RadioGroup;
import android.widget.RadioButton;

// Order Detail ViewModel
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.MutableLiveData;
```

---

## 4. Submit Review Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       SUBMIT REVIEW FLOW                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   User taps "Đánh giá" (only available after delivered)                    │
│          │                                                                   │
│          ▼                                                                   │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                 SHOW RATING DIALOG                                   │  │
│   │                                                                     │  │
│   │   ┌─────────────────────────────────────────────────────────────┐  │  │
│   │   │                                                              │  │  │
│   │   │   ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐                      │  │  │
│   │   │   │ ☆  │ │ ☆  │ │ ☆  │ │ ☆  │ │ ☆  │   Tap to rate       │  │  │
│   │   │   └────┘ └────┘ └────┘ └────┘ └────┘                      │  │  │
│   │   │     1      2      3      4      5                          │  │  │
│   │   │                                                              │  │  │
│   │   │   Restaurant: Restaurant A                                  │  │  │
│   │   │                                                              │  │  │
│   │   └────────────────────────────────────────────────────────────┘  │  │
│   │                                                                     │  │
│   │   ┌─────────────────────────────────────────────────────────────┐  │  │
│   │   │ Đánh giá của bạn (tùy chọn):                                │  │  │
│   │   │ [                                                 ]        │  │  │
│   │   │                                                          │  │  │
│   │   │                                                          │  │  │
│   │   └────────────────────────────────────────────────────────────┘  │  │
│   │                                                                     │  │
│   │   [Hủy]                                     [Gửi đánh giá]          │  │
│   │                                                                     │  │
│   └────────────────────────────────────┬────────────────────────────────┘  │
│                                        │                                     │
│                                        ▼                                     │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    POST REVIEW                                       │  │
│   │                                                                     │  │
│   │   POST /rest/v1/reviews                                            │  │
│   │   {                                                                │  │
│   │       order_id: 'order_uuid',                                      │  │
│   │       user_id: 'user_uuid',                                        │  │
│   │       restaurant_id: 'restaurant_uuid',                            │  │
│   │       rating: 5,                                                   │  │
│   │       comment: '...'                                               │  │
│   │   }                                                                │  │
│   │                                                                     │  │
│   │   Database triggers:                                               │  │
│   │   • Insert review                                                  │  │
│   │   • Update restaurant.rating (recalculate average)                │  │
│   │   • Update restaurant.review_count                                │  │
│   │                                                                     │  │
│   └────────────────────────────┬────────────────────────────────────────┘  │
│                                 │                                          │
│                    ┌────────────┴────────────┐                              │
│                    │                         │                              │
│                SUCCESS                     ERROR                           │
│                    │                         │                              │
│                    ▼                         ▼                              │
│   ┌────────────────────────┐  ┌────────────────────────────────────────┐   │
│   │ • Hide review button   │  │ • Show error message                  │   │
│   │ • Show success         │  │ • Keep dialog open                    │   │
│   │   "Cảm ơn bạn đã       │  │                                       │   │
│   │    đánh giá!"         │  │                                       │   │
│   │ • Close dialog         │  │                                       │   │
│   └────────────────────────┘  └────────────────────────────────────────┘   │
│                                                                             │
│   VALIDATION:                                                              │
│   ═══════════                                                              │
│                                                                             │
│   rating: required, 1-5                                                    │
│   comment: optional, max 500 chars                                         │
│   order_id: must be unique (one review per order)                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Code Import:**

```java
// Review submission
import com.supabase.SupabaseClient;
import com.supabase.postgrest.requests.InsertRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

// Rating Dialog
import android.app.Dialog;
import android.widget.RatingBar;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View.OnClickListener;

// RatingBar listener
import android.widget.RatingBar.OnRatingBarChangeListener;

// Review model
public class Review {
    private String id;
    private String orderId;
    private String userId;
    private String restaurantId;
    private int rating;
    private String comment;
    private long createdAt;
}
```

---

## 5. Order History UI Layout

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     ORDER HISTORY UI LAYOUT                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  Toolbar: Đơn hàng của tôi                       [🔔] [🔍]       │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  TabLayout:                                                         │  │
│   │  ┌───────┬───────────┬─────────────┬────────────┐                    │  │
│   │  │Tất cả │Đang xử lý │ Hoàn thành  │  Đã hủy   │                    │  │
│   │  │   10  │     4     │      5     │     1     │                    │  │
│   │  └───────┴───────────┴─────────────┴────────────┘                    │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  SwipeRefreshLayout                                                 │  │
│   │  ┌────────────────────────────────────────────────────────────┐   │  │
│   │  │                                                            │   │  │
│   │  │  ┌────────────────────────────────────────────────────┐  │   │  │
│   │  │  │  🟢 Hoàn thành          ORD001    150,000 VNĐ     │  │   │  │
│   │  │  │  Restaurant A                                    │  │   │  │
│   │  │  │  15/01/2024 10:30 • 2 món                       │  │   │  │
│   │  │  └────────────────────────────────────────────────────┘  │   │  │
│   │  │                                                            │   │  │
│   │  │  ┌────────────────────────────────────────────────────┐  │   │  │
│   │  │  │  🔷 Đang giao          ORD002    200,000 VNĐ     │  │   │  │
│   │  │  │  Restaurant B                                    │  │   │  │
│   │  │  │  15/01/2024 11:00 • 3 món                       │  │   │  │
│   │  │  └────────────────────────────────────────────────────┘  │   │  │
│   │  │                                                            │   │  │
│   │  │  ┌────────────────────────────────────────────────────┐  │   │  │
│   │  │  │  🟡 Chuẩn bị          ORD003     80,000 VNĐ      │  │   │  │
│   │  │  │  Restaurant C                                    │  │   │  │
│   │  │  │  14/01/2024 18:30 • 1 món                       │  │   │  │
│   │  │  └────────────────────────────────────────────────────┘  │   │  │
│   │  │                                                            │   │  │
│   │  │  ┌────────────────────────────────────────────────────┐  │   │  │
│   │  │  │  🔴 Đã hủy          ORD004    120,000 VNĐ       │  │   │  │
│   │  │  │  Restaurant A                                    │  │   │  │
│   │  │  │  14/01/2024 12:00 • 2 món                       │  │   │  │
│   │  │  │  Lý do: Khách hàng hủy                         │  │   │  │
│   │  │  └────────────────────────────────────────────────────┘  │   │  │
│   │  │                                                            │   │  │
│   │  └────────────────────────────────────────────────────────────┘   │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   Status Icons:                                                            │
│   🔵 Pending = Chờ xác nhận (Vàng)                                         │
│   🟢 Confirmed = Đã xác nhận (Xanh dương)                                  │
│   🟡 Preparing = Đang chuẩn bị (Cam)                                       │
│   🔷 Delivering = Đang giao (Xanh nhạt)                                    │
│   🟢 Delivered = Hoàn thành (Xanh)                                            │
│   🔴 Cancelled = Đã hủy (Đỏ)                                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Code Import:**

```java
// Layout resources
import android.widget.TextView;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.tabs.TabLayout;
import androidx.appcompat.widget.Toolbar;

// Status icon colors
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import android.graphics.drawable.GradientDrawable;

// Number formatting
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

// Order item layout
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
```