# Sơ đồ - Module Quản lý đơn hàng - App Admin

## 1. Order List Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       ORDER LIST FLOW                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────┐     ┌─────────────┐     ┌─────────────┐                  │
│   │ Order List  │     │  Order      │     │   Order     │                  │
│   │  Fragment   │     │  ViewModel  │     │ Repository  │                  │
│   └──────┬──────┘     └──────┬──────┘     └──────┬──────┘                  │
│          │                   │                   │                          │
│          │    onViewCreated()│                   │                          │
│          │──────────────────▶│                   │                          │
│          │                   │                   │                          │
│          │                   │   loadOrders()    │                          │
│          │                   │   param: status   │                          │
│          │                   │───────────────────▶│                          │
│          │                   │                   │                          │
│          │                   │                   │   GET /rest/v1/orders   │
│          │                   │                   │   ?status=eq.{status}   │
│          │                   │                   │   &select=*,user(*),   │
│          │                   │                   │   restaurant(*)         │
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
│   TAB SELECTION:                                                            │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                                                                     │  │
│   │   Tab "All"      ──▶ loadOrders(null)                             │  │
│   │   Tab "Pending"  ──▶ loadOrders("pending")                        │  │
│   │   Tab "Confirmed"──▶ loadOrders("confirmed")                      │  │
│   │   Tab "Preparing"──▶ loadOrders("preparing")                      │  │
│   │   Tab "Delivering"▶ loadOrders("delivering")                      │  │
│   │   Tab "Delivered" ─▶ loadOrders("delivered")                      │  │
│   │   Tab "Cancelled" ─▶ loadOrders("cancelled")                      │  │
│   │                                                                     │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
```

**Code Import:**

```java
// OrderViewModel.java
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.supabase.postgrest.Postgrest;
import com.supabase.postgrest.builder.QueryBuilder;
import com.supabase.postgrest.requests.GetRequest;
import java.util.List;
import java.util.Map;

// OrderRepository.java
import com.supabase.SupabaseClient;
import com.supabase.postgrest.requests.SelectRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

// Order Adapter
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

// Tab selection
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabListener;
```

---

## 2. Order Detail & Status Update Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                ORDER DETAIL & STATUS UPDATE FLOW                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    ORDER LIST FRAGMENT                              │  │
│   │                                                                     │  │
│   │   User taps on order item                                           │  │
│   │   ─────────────────────────────────────────────────────────────     │  │
│   │   Start OrderDetailActivity with orderId                           │  │
│   │                                                                     │  │
│   └────────────────────────────────────┬────────────────────────────────┘  │
│                                        │                                    │
│                                        ▼                                    │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    ORDER DETAIL ACTIVITY                            │  │
│   │                                                                     │  │
│   │   loadOrderDetail(orderId)                                          │  │
│   │        │                                                            │  │
│   │        │  ┌────────────────────────────────────────────────────┐   │  │
│   │        │  │ PARALLEL LOAD:                                    │   │  │
│   │        │  │ • Order header (orders table)                    │   │  │
│   │        │  │ • Order items (order_items table)                │   │  │
│   │        │  │ • Customer info (users table)                    │   │  │
│   │        │  │ • Restaurant info (restaurants table)            │   │  │
│   │        │  │ • Status logs (order_status_logs table)          │   │  │
│   │        │  └────────────────────────────────────────────────────┘   │  │
│   │        │                                                            │  │
│   │        ▼                                                            │  │
│   │   Display all information                                          │  │
│   │                                                                     │  │
│   └────────────────────────────────────┬────────────────────────────────┘  │
│                                        │                                    │
│                                        ▼                                    │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    UPDATE STATUS DIALOG                             │  │
│   │                                                                     │  │
│   │   User taps "Xác nhận" / "Hủy"                                     │  │
│   │        │                                                            │  │
│   │        ▼                                                            │  │
│   │   ┌────────────────────────────────────────────────────────────┐   │  │
│   │   │            VALIDATE STATE MACHINE                            │   │  │
│   │   │                                                             │   │  │
│   │   │   current: pending ──▶ target: confirmed  ✓ VALID         │   │  │
│   │   │   current: pending ──▶ target: preparing  ✗ INVALID       │   │  │
│   │   │   current: confirmed ──▶ target: preparing ✓ VALID        │   │  │
│   │   │   current: confirmed ──▶ target: delivered  ✗ INVALID     │   │  │
│   │   │                                                             │   │  │
│   │   └────────────────────────────────────────────────────────────┘   │  │
│   │        │                                                            │  │
│   │        ▼                                                            │  │
│   │   ┌────────────────────────────────────────────────────────────┐   │  │
│   │   │             CALL RPC: rpc_update_order_status              │   │  │
│   │   │                                                             │   │  │
│   │   │   Parameters:                                               │   │  │
│   │   │   • p_order_id: 'uuid'                                     │   │  │
│   │   │   • p_new_status: 'confirmed'                              │   │  │
│   │   │   • p_changed_by: 'current_user_id'                        │   │  │
│   │   │   • p_note: 'Admin confirmed order'                       │   │  │
│   │   │                                                             │   │  │
│   │   └─────────────────────────┬───────────────────────────────────┘   │  │
│   │                             │                                        │  │
│   │                   ┌─────────┴─────────┐                             │  │
│   │                   │                 │                             │  │
│   │               SUCCESS             ERROR                          │  │
│   │                   │                 │                             │  │
│   │                   ▼                 ▼                             │  │
│   │   ┌────────────────────┐  ┌────────────────────┐                  │  │
│   │   │ • Insert status   │  │ • Show error       │                  │  │
│   │   │   log             │  │ • Keep current     │                  │  │
│   │   │ • Create notif    │  │   status           │                  │  │
│   │   │ • Refresh list   │  │                    │                  │  │
│   │   │ • Show success   │  │                    │                  │  │
│   │   │   snackbar       │  │                    │                  │  │
│   │   └────────────────────┘  └────────────────────┘                  │  │
│   │                             │                                        │  │
│   └─────────────────────────────┴────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 3. Order Status State Machine

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      ORDER STATUS STATE MACHINE                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                                                                             │
│   ╔════════════════════════════════════════════════════════════════════╗  │
│   ║                                                                    ║  │
│   ║   ┌──────────┐     ┌───────────┐     ┌───────────┐     ┌─────────┐ ║  │
│   ║   │ pending │────▶│confirmed │────▶│preparing │────▶│deliver- │ ║  │
│   ║   │ (chờ)   │     │(xác nhận)│     │(đang nấu)│     │  ing    │ ║  │
│   ║   └────┬─────┘     └─────┬─────┘     └─────┬─────┘     │ (đang   │ ║  │
│   ║        │                 │                │           │  giao)  │ ║  │
│   ║        │                 │                │           └────┬────┘ ║  │
│   ║        │                 │                │                │     ║  │
│   ║        │                 │                │                ▼     ║  │
│   ║        │                 │                │         ┌─────────┐ ║  │
│   ║        │                 │                │         │delivered│ ║  │
│   ║        │                 │                │         │(hoàn    │ ║  │
│   ║        │                 │                │         │ thành)  │ ║  │
│   ║        │                 │                │         └────┬────┘ ║  │
│   ║        │                 │                │              FINAL   ║  │
│   ║        ▼                 ▼                ▼                         ║  │
│   ║   ┌─────────────────────────────────────────────────────────┐    ║  │
│   ║   │                    CANCELLED                             │    ║  │
│   ║   │            (hủy từ pending hoặc confirmed)               │    ║  │
│   ║   └─────────────────────────────────────────────────────────┘    ║  │
│   ║                         FINAL                                       ║  │
│   ║                                                                    ║  │
│   ╚════════════════════════════════════════════════════════════════════╝  │
│                                                                             │
│   VALID TRANSITIONS:                                                       │
│   ══════════════════════                                                    │
│                                                                             │
│   From         Allowed To                                                   │
│   ─────────────────────────────────────────────────────────────────────     │
│   pending      → confirmed, cancelled                                       │
│   confirmed    → preparing, cancelled                                       │
│   preparing    → delivering, cancelled                                      │
│   delivering   → delivered                                                  │
│   delivered   → (none - final state)                                       │
│   cancelled   → (none - final state)                                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Code Import:**

```java
// OrderStatus Enum
public enum OrderStatus {
    PENDING("pending", "Chờ xác nhận"),
    CONFIRMED("confirmed", "Đã xác nhận"),
    PREPARING("preparing", "Đang chuẩn bị"),
    DELIVERING("delivering", "Đang giao"),
    DELIVERED("delivered", "Hoàn thành"),
    CANCELLED("cancelled", "Đã hủy");

    private final String value;
    private final String label;
    
    OrderStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }
    
    public String getValue() { return value; }
    public String getLabel() { return label; }
    
    public static OrderStatus fromValue(String value) {
        for (OrderStatus status : values()) {
            if (status.value.equals(value)) return status;
        }
        return null;
    }
}

// State Machine Validation
public class OrderStateMachine {
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new HashMap<>();
    
    static {
        ALLOWED_TRANSITIONS.put(OrderStatus.PENDING, 
            EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.CONFIRMED, 
            EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.PREPARING, 
            EnumSet.of(OrderStatus.DELIVERING, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.DELIVERING, 
            EnumSet.of(OrderStatus.DELIVERED));
        // delivered and cancelled are final states
    }
    
    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }
}
```

---

## 4. Realtime Order Updates

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    REALTIME ORDER UPDATES                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   SUPABASE REALTIME SUBSCRIPTION:                                           │
│   ═════════════════════════════════════════                                 │
│                                                                             │
│   Channel: "orders-channel"                                                 │
│   Events: INSERT, UPDATE                                                    │
│   Filters: (no filter - receive all order changes)                         │
│                                                                             │
│         ┌───────────────────────────────────────────────────────────┐     │
│         │                     NEW ORDER (INSERT)                      │     │
│         │                                                               │     │
│         │   Client creates order ──▶ Supabase ──▶ Admin App          │     │
│         │            │                         │                     │     │
│         │            │                         │                     │     │
│         │            │                   ┌─────┴─────┐              │     │
│         │            │                   │           │              │     │
│         │            │               NEW ORDER    NOTIFY            │     │
│         │            │               to list       user              │     │
│         │            │                   │           │              │     │
│         │            │                   ▼           │              │     │
│         │            │           ┌───────────────┐   │              │     │
│         │            │           │ • Add to top │   │              │     │
│         │            │           │ • Update      │   │              │     │
│         │            │           │   badge       │   │              │     │
│         │            │           │ • Play sound │   │              │     │
│         │            │           │ • Toast msg  │   │              │     │
│         │            │           └───────────────┘   │              │     │
│         │            │                               │              │     │
│         └────────────┴───────────────────────────────┴──────────────┘     │
│                                                                             │
│         ┌───────────────────────────────────────────────────────────┐     │
│         │                   STATUS UPDATE (UPDATE)                   │     │
│         │                                                               │     │
│         │   Admin updates status ──▶ Supabase ──▶ Client App        │     │
│         │            │                         │                     │     │
│         │            │                         │                     │     │
│         │            │                   ┌─────┴─────┐              │     │
│         │            │                   │           │              │     │
│         │            │               UPDATE       NOTIFY           │     │
│         │            │               in list       user             │     │
│         │            │                   │           │              │     │
│         │            │                   ▼           │              │     │
│         │            │           ┌───────────────┐   │              │     │
│         │            │           │ • Update      │   │              │     │
│         │            │           │   status      │   │              │     │
│         │            │           │ • Refresh     │   │              │     │
│         │            │           │   timeline    │   │              │     │
│         │            │           │ • Toast:      │   │              │     │
│         │            │           │   "Đang giao" │   │              │     │
│         │            │           └───────────────┘   │              │     │
│         │            │                               │              │     │
│         └────────────┴───────────────────────────────┴──────────────┘     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 5. Order List UI Layout

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      ORDER LIST UI LAYOUT                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  Toolbar: Quản lý đơn hàng                        [🔍 Search]     │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  TabLayout:                                                         │  │
│   │  ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┐                       │  │
│   │  │ All │Pend │Conf │Prep │Deli │Deli │Canc │                       │  │
│   │  │     │     │     │     │     │ver  │el   │                       │  │
│   │  └─────┴─────┴─────┴─────┴─────┴─────┴─────┘                       │  │
│   │     25   5    10   3    2    3    2                               │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  SwipeRefreshLayout                                                 │  │
│   │  ┌────────────────────────────────────────────────────────────┐   │  │
│   │  │                                                            │   │  │
│   │  │  ┌────────────────────────────────────────────────────┐  │   │  │
│   │  │  │ 📦 ORD001                      🔵 Pending           │  │   │  │
│   │  │  │ Restaurant A                                    │  │   │  │
│   │  │  │ Nguyễn Văn A - 0901234567                        │  │   │  │
│   │  │  │ 150,000 VNĐ • 10:30 AM                          │  │   │  │
│   │  │  └────────────────────────────────────────────────────┘  │   │  │
│   │  │                                                            │   │  │
│   │  │  ┌────────────────────────────────────────────────────┐  │   │  │
│   │  │  │ 📦 ORD002                      🟢 Confirmed         │  │   │  │
│   │  │  │ Restaurant B                                    │  │   │  │
│   │  │  │ Trần Thị B - 0909876543                        │  │   │  │
│   │  │  │ 200,000 VNĐ • 10:15 AM                          │  │   │  │
│   │  │  └────────────────────────────────────────────────────┘  │   │  │
│   │  │                                                            │   │  │
│   │  │  ┌────────────────────────────────────────────────────┐  │   │  │
│   │  │  │ 📦 ORD003                      🟡 Preparing         │  │   │  │
│   │  │  │ Restaurant C                                    │  │   │  │
│   │  │  │ Lê Văn C - 0901111222                          │  │   │  │
│   │  │  │ 80,000 VNĐ • 09:45 AM                           │  │   │  │
│   │  │  └────────────────────────────────────────────────────┘  │   │  │
│   │  │                                                            │   │  │
│   │  └────────────────────────────────────────────────────────────┘   │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   Status Colors:                                                            │
│   🔵 Pending = #FFC107 (Vàng)                                               │
│   🟢 Confirmed = #2196F3 (Xanh dương)                                      │
│   🟡 Preparing = #FF9800 (Cam)                                             │
│   🔷 Delivering = #03A9F4 (Xanh nhạt)                                      │
│   🟢 Delivered = #4CAF50 (Xanh)                                            │
│   🔴 Cancelled = #F44336 (Đỏ)                                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Code Import:**

```java
// OrderListFragment.java
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.tabs.TabLayout;
import androidx.appcompat.widget.Toolbar;
import android.widget.SearchView;

// OrderAdapter.java
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

// Order item status colors
import android.graphics.Color;
import android.content.res.Resources;
import androidx.core.content.ContextCompat;

// Status badge
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.content.res.ColorStateList;
```
```