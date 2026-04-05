# Sơ đồ - Module Dashboard - App Admin

## 1. Dashboard Data Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     DASHBOARD DATA FLOW                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────┐     ┌─────────────┐     ┌─────────────┐                  │
│   │  Dashboard │     │  Dashboard  │     │  Dashboard  │                  │
│   │   Fragment │     │  ViewModel  │     │ Repository  │                  │
│   └──────┬──────┘     └──────┬──────┘     └──────┬──────┘                  │
│          │                   │                   │                          │
│          │    onViewCreated()│                   │                          │
│          │──────────────────▶│                   │                          │
│          │                   │                   │                          │
│          │                   │   loadDashboard() │                          │
│          │                   │───────────────────▶│                          │
│          │                   │                   │                          │
│          │                   │                   │    ┌────────────┐       │
│          │                   │                   │    │            │       │
│          │                   │                   │    │  getDaily  │       │
│          │                   │                   ├───▶│   Stats    │       │
│          │                   │                   │    │ (View)     │       │
│          │                   │                   │    └────────────┘       │
│          │                   │                   │          │              │
│          │                   │                   │          ▼              │
│          │                   │                   │    ┌────────────┐       │
│          │                   │                   │    │ getNewOrders│      │
│          │                   │                   ├───▶│            │       │
│          │                   │                   │    │ (Query)    │       │
│          │                   │                   │    └────────────┘       │
│          │                   │                   │          │              │
│          │                   │                   │          ▼              │
│          │                   │                   │    ┌────────────┐       │
│          │                   │                   │    │ getTopItems │      │
│          │                   │                   ├───▶│  (RPC/SQL)  │       │
│          │                   │                   │    └────────────┘       │
│          │                   │                   │          │              │
│          │                   │                   │          ▼              │
│          │                   │                   │    ┌────────────┐       │
│          │                   │                   │    │   Results  │       │
│          │                   │                   │    │            │       │
│          │                   │                   │◀───│ (Combine)  │       │
│          │                   │                   │    └────────────┘       │
│          │                   │                   │                          │
│          │                   │  LiveData<DashboardData>                    │
│          │                   │◀──────────────────│                          │
│          │                   │                   │                          │
│          │  Update UI        │                   │                          │
│          │◀──────────────────│                   │                          │
│          │                   │                   │                          │
└──────────┴───────────────────┴───────────────────┴──────────────────────────┘
```

**Code Import:**

```java
// DashboardViewModel.java
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.supabase.postgrest.Postgrest;
import com.supabase.postgrest.builder.QueryBuilder;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;

// DashboardRepository.java
import com.supabase.SupabaseClient;
import com.supabase.postgrest.requests.GetRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

// DashboardData model
import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Type;
```

---

## 2. Realtime Updates Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    REAL TIME UPDATES FLOW                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Supabase ──▶ Realtime Channel ──▶ Order List                              │
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │                     SUPABASE REALTIME                               │  │
│   │                                                                     │  │
│   │   Channel: "orders-realtime"                                       │  │
│   │   Filter: "status=eq.pending"                                      │  │
│   │                                                                     │  │
│   │         │                                                           │  │
│   │         │  New order created (INSERT)                              │  │
│   │         │  ─────────────────────────────────────────────────────▶ │  │
│   │         │                                                           │  │
│   │         ▼                                                           │  │
│   │   ┌─────────────────────────────────────────────────────────────┐  │  │
│   │   │                     CLIENT APP                               │  │  │
│   │   │                                                              │  │  │
│   │   │   ┌──────────────────────────────────────────────────────┐  │  │  │
│   │   │   │                 OrderListFragment                      │  │  │  │
│   │   │   │                                                       │  │  │  │
│   │   │   │   1. Receive event                                    │  │  │  │
│   │   │   │   2. Add new order to list (position 0)              │  │  │  │
│   │   │   │   3. Update badge count                              │  │  │  │
│   │   │   │   4. Play notification sound                         │  │  │  │
│   │   │   │   5. Show toast: "Có đơn hàng mới"                  │  │  │  │
│   │   │   │                                                       │  │  │  │
│   │   │   └──────────────────────────────────────────────────────┘  │  │  │
│   │   │                              │                                │  │  │
│   │   └──────────────────────────────┴────────────────────────────────┘  │  │
│   │                                                                     │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │                     NAVIGATION DRAWER BADGE                        │  │
│   │                                                                     │  │
│   │   Badge Count = count(status='pending')                            │  │
│   │                                                                     │  │
│   │   On new order:                                                    │  │
│   │   - Badge count + 1                                               │  │
│   │   - Animate badge                                                 │  │
│   │                                                                     │  │
│   └────────────────────────────────────────────────────────────────────┘  │
```

**Code Import:**

```java
// Supabase Realtime
import com.supabase.realtime.channels.Channel;
import com.supabase.realtime.channels.RealtimeChannel;
import com.supabase.realtime.events.PgInsertEvent;
import com.supabase.realtime.events.PgUpdateEvent;

// RealtimeManager.java
import android.content.Context;
import android.media.MediaPlayer;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.os.Build;

// Event handling
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.HashMap;
```

---

## 3. Dashboard UI Layout

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      DASHBOARD UI LAYOUT                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  Toolbar: Dashboard                               [Refresh]       │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐    │  │
│   │  │    ORDERS      │  │    REVENUE     │  │   PROCESSING   │    │  │
│   │  │   TODAY       │  │   TODAY        │  │                │    │  │
│   │  │     25        │  │   2,500,000    │  │       5        │    │  │
│   │  │  [📊]         │  │   [💰]         │  │   [⏳]         │    │  │
│   │  └────────────────┘  └────────────────┘  └────────────────┘    │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   ──────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   NEW ORDERS                                      [View All]              │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  ┌────────────────────────────────────────────────────────────────┐│  │
│   │  │ ORD001  │ Restaurant A  │ 150,000  │ ● Pending │ 10:30 AM  ││  │
│   │  ├────────────────────────────────────────────────────────────────┤│  │
│   │  │ ORD002  │ Restaurant B  │ 200,000  │ ● Confirmed│ 10:15 AM ││  │
│   │  ├────────────────────────────────────────────────────────────────┤│  │
│   │  │ ORD003  │ Restaurant C  │  80,000  │ ● Pending │ 09:45 AM ││  │
│   │  └────────────────────────────────────────────────────────────────┘│  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   ──────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   TOP ITEMS                                         [View All]              │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  1. Phở Bò Nạm        │ 150 lần │ 7,500,000 VNĐ                    │  │
│   │  2. Cơm Rang Dưa Bò  │ 120 lần │ 4,200,000 VNĐ                    │  │
│   │  3. Bún Chả Hà Nội    │ 100 lần │ 3,000,000 VNĐ                    │  │
│   │  4. Bánh Mì Pate      │  80 lần │ 2,400,000 VNĐ                    │  │
│   │  5. Trà Đá            │ 200 lần │ 1,000,000 VNĐ                    │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Code Import:**

```java
// Layout files
import android.widget.TextView;
import android.widget.CardView;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.widget.Toolbar;

// RecyclerView Adapter
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

// Card animation
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

// Formatter
import java.text.NumberFormat;
import java.util.Locale;
import java.text.SimpleDateFormat;
```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     DASHBOARD DATA FLOW                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────┐     ┌─────────────┐     ┌─────────────┐                  │
│   │  Dashboard │     │  Dashboard  │     │  Dashboard  │                  │
│   │   Fragment │     │  ViewModel  │     │ Repository  │                  │
│   └──────┬──────┘     └──────┬──────┘     └──────┬──────┘                  │
│          │                   │                   │                          │
│          │    onViewCreated()│                   │                          │
│          │──────────────────▶│                   │                          │
│          │                   │                   │                          │
│          │                   │   loadDashboard() │                          │
│          │                   │───────────────────▶│                          │
│          │                   │                   │                          │
│          │                   │                   │    ┌────────────┐       │
│          │                   │                   │    │            │       │
│          │                   │                   │    │  getDaily  │       │
│          │                   │                   ├───▶│   Stats    │       │
│          │                   │                   │    │ (View)     │       │
│          │                   │                   │    └────────────┘       │
│          │                   │                   │          │              │
│          │                   │                   │          ▼              │
│          │                   │                   │    ┌────────────┐       │
│          │                   │                   │    │ getNewOrders│      │
│          │                   │                   ├───▶│            │       │
│          │                   │                   │    │ (Query)    │       │
│          │                   │                   │    └────────────┘       │
│          │                   │                   │          │              │
│          │                   │                   │          ▼              │
│          │                   │                   │    ┌────────────┐       │
│          │                   │                   │    │ getTopItems │      │
│          │                   │                   ├───▶│  (RPC/SQL)  │       │
│          │                   │                   │    └────────────┘       │
│          │                   │                   │          │              │
│          │                   │                   │          ▼              │
│          │                   │                   │    ┌────────────┐       │
│          │                   │                   │    │   Results  │       │
│          │                   │                   │    │            │       │
│          │                   │                   │◀───│ (Combine)  │       │
│          │                   │                   │    └────────────┘       │
│          │                   │                   │                          │
│          │                   │  LiveData<DashboardData>                    │
│          │                   │◀──────────────────│                          │
│          │                   │                   │                          │
│          │  Update UI        │                   │                          │
│          │◀──────────────────│                   │                          │
│          │                   │                   │                          │
└──────────┴───────────────────┴───────────────────┴──────────────────────────┘
```

## 2. Realtime Updates Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    REAL TIME UPDATES FLOW                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Supabase ──▶ Realtime Channel ──▶ Order List                              │
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │                     SUPABASE REALTIME                               │  │
│   │                                                                     │  │
│   │   Channel: "orders-realtime"                                       │  │
│   │   Filter: "status=eq.pending"                                      │  │
│   │                                                                     │  │
│   │         │                                                           │  │
│   │         │  New order created (INSERT)                              │  │
│   │         │  ─────────────────────────────────────────────────────▶ │  │
│   │         │                                                           │  │
│   │         ▼                                                           │  │
│   │   ┌─────────────────────────────────────────────────────────────┐  │  │
│   │   │                     CLIENT APP                               │  │  │
│   │   │                                                              │  │  │
│   │   │   ┌──────────────────────────────────────────────────────┐  │  │  │
│   │   │   │                 OrderListFragment                      │  │  │  │
│   │   │   │                                                       │  │  │  │
│   │   │   │   1. Receive event                                    │  │  │  │
│   │   │   │   2. Add new order to list (position 0)              │  │  │  │
│   │   │   │   3. Update badge count                              │  │  │  │
│   │   │   │   4. Play notification sound                         │  │  │  │
│   │   │   │   5. Show toast: "Có đơn hàng mới"                  │  │  │  │
│   │   │   │                                                       │  │  │  │
│   │   │   └──────────────────────────────────────────────────────┘  │  │  │
│   │   │                              │                                │  │  │
│   │   └──────────────────────────────┴────────────────────────────────┘  │  │
│   │                                                                     │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │                     NAVIGATION DRAWER BADGE                        │  │
│   │                                                                     │  │
│   │   Badge Count = count(status='pending')                            │  │
│   │                                                                     │  │
│   │   On new order:                                                    │  │
│   │   - Badge count + 1                                               │  │
│   │   - Animate badge                                                 │  │
│   │                                                                     │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 3. Dashboard UI Layout

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      DASHBOARD UI LAYOUT                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  Toolbar: Dashboard                               [Refresh]       │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐    │  │
│   │  │    ORDERS      │  │    REVENUE     │  │   PROCESSING   │    │  │
│   │  │   TODAY       │  │   TODAY        │  │                │    │  │
│   │  │     25        │  │   2,500,000    │  │       5        │    │  │
│   │  │  [📊]         │  │   [💰]         │  │   [⏳]         │    │  │
│   │  └────────────────┘  └────────────────┘  └────────────────┘    │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   ──────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   NEW ORDERS                                      [View All]              │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  ┌────────────────────────────────────────────────────────────────┐│  │
│   │  │ ORD001  │ Restaurant A  │ 150,000  │ ● Pending │ 10:30 AM  ││  │
│   │  ├────────────────────────────────────────────────────────────────┤│  │
│   │  │ ORD002  │ Restaurant B  │ 200,000  │ ● Confirmed│ 10:15 AM ││  │
│   │  ├────────────────────────────────────────────────────────────────┤│  │
│   │  │ ORD003  │ Restaurant C  │  80,000  │ ● Pending │ 09:45 AM ││  │
│   │  └────────────────────────────────────────────────────────────────┘│  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   ──────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   TOP ITEMS                                         [View All]              │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  1. Phở Bò Nạm        │ 150 lần │ 7,500,000 VNĐ                    │  │
│   │  2. Cơm Rang Dưa Bò  │ 120 lần │ 4,200,000 VNĐ                    │  │
│   │  3. Bún Chả Hà Nội    │ 100 lần │ 3,000,000 VNĐ                    │  │
│   │  4. Bánh Mì Pate      │  80 lần │ 2,400,000 VNĐ                    │  │
│   │  5. Trà Đá            │ 200 lần │ 1,000,000 VNĐ                    │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```