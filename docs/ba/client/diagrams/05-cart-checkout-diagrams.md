# Sơ đồ - Module Giỏ hàng & Thanh toán - App Client

## 1. Add to Cart Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       ADD TO CART FLOW                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   User taps "+" on menu item                                                │
│          │                                                                   │
│          ▼                                                                   │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    VALIDATION CHECKS                                │  │
│   │                                                                     │  │
│   │   1. Is item available?                                             │  │
│   │      ├─▶ NO ──▶ Show "Món này hiện không có"                        │  │
│   │      │     ──▶ Disable add button, gray out                        │  │
│   │      │                                                              │  │
│   │      └─▶ YES ──▶ Continue                                          │  │
│   │                                                                     │  │
│   │   2. Is restaurant open?                                           │  │
│   │      ├─▶ NO ──▶ Show "Nhà hàng đóng cửa"                           │  │
│   │      │     ──▶ Disable checkout                                    │  │
│   │      │                                                              │  │
│   │      └─▶ YES ──▶ Continue                                          │  │
│   │                                                                     │  │
│   │   3. Is same restaurant in cart?                                    │  │
│   │      ├─▶ YES ──▶ Go to step 4                                      │  │
│   │      │                                                              │  │
│   │      └─▶ NO ──▶ Show confirm dialog                                │  │
│   │              "Giỏ hàng có món từ {other_restaurant}"              │  │
│   │              "Xóa và thêm món mới?"                                 │  │
│   │              │                                                      │  │
│   │         ┌────┴────┐                                                │  │
│   │         │         │                                                │  │
│   │       Yes        No                                                 │  │
│   │         │         │                                                │  │
│   │         ▼         ▼                                                │  │
│   │   Clear cart   Cancel                                               │  │
│   │   + Add item    (do nothing)                                        │  │
│   │                                                                     │  │
│   └────────────────────────────────────┬────────────────────────────────┘  │
│                                        │                                     │
│                                        ▼                                     │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    ADD TO ROOM DATABASE                             │  │
│   │                                                                     │  │
│   │   CartRepository.addToCart(cartItem)                               │  │
│   │          │                                                          │  │
│   │          ▼                                                          │  │
│   │   ┌─────────────────────────────────────────────────────────────┐  │  │
│   │   │                    ROOM DATABASE                            │  │  │
│   │   │                                                             │  │  │
│   │   │   Table: cart_items                                          │  │  │
│   │   │   Columns:                                                    │  │  │
│   │   │   • id (UUID)                                                │  │  │
│   │   │   • menu_item_id                                             │  │  │
│   │   │   • name                                                     │  │  │
│   │   │   • price                                                    │  │  │
│   │   │   • quantity                                                 │  │  │
│   │   │   • note (optional)                                          │  │  │
│   │   │   • restaurant_id                                            │  │  │
│   │   │   • created_at                                               │  │  │
│   │   │                                                             │  │  │
│   │   │   If item exists: quantity = quantity + newQuantity          │  │  │
│   │   │   If new: INSERT new row                                    │  │  │
│   │   │                                                             │  │  │
│   │   └─────────────────────────────────────────────────────────────┘  │  │
│   │          │                                                          │  │
│   │          ▼                                                          │  │
│   │   ┌─────────────────────────────────────────────────────────────┐  │  │
│   │   │                    UPDATE UI                                 │  │  │
│   │   │                                                             │  │  │
│   │   │   • Update cart badge (count of items)                     │  │  │
│   │   │   • Update total price in floating button                  │  │  │
│   │   │   • Show snackbar: "Đã thêm {item_name} vào giỏ hàng"       │  │  │
│   │   │   • Update quantity control on item                        │  │  │
│   │   │                                                             │  │  │
│   │   └─────────────────────────────────────────────────────────────┘  │  │
│   │                                                                     │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Code Import:**

```java
// CartRepository.java - Room Database
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// CartItem Entity
@Entity(tableName = "cart_items")
public class CartItem {
    @PrimaryKey
    private String id;
    private String menuItemId;
    private String name;
    private int price;
    private int quantity;
    private String note;
    private String restaurantId;
    private long createdAt;
}

// CartViewModel
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.List;

// Snackbar
import com.google.android.material.snackbar.Snackbar;
import android.view.View;

// Confirm Dialog
import androidx.appcompat.app.AlertDialog;
```

---

## 2. Cart Data Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       CART DATA FLOW                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────┐     ┌─────────────┐     ┌─────────────┐                  │
│   │  Restaurant │     │  Cart       │     │    Cart     │                  │
│   │   Detail    │     │  Fragment   │     │  ViewModel  │                  │
│   └──────┬──────┘     └──────┬──────┘     └──────┬──────┘                  │
│          │                   │                   │                          │
│          │   addToCart()    │                   │                          │
│          │──────────────────▶│                   │                          │
│          │                   │                   │                          │
│          │                   │   addToCart()     │                          │
│          │                   │───────────────────▶│                          │
│          │                   │                   │                          │
│          │                   │                   │   CartRepository.add()   │
│          │                   │                   │─────────────────────────▶│
│          │                   │                   │                          │
│          │                   │                   │    ROOM DATABASE         │
│          │                   │                   │    cart_items table       │
│          │                   │                   │◀─────────────────────────│
│          │                   │                   │                          │
│          │                   │   LiveData<CartSummary>                    │
│          │                   │◀──────────────────│                          │
│          │                   │                   │                          │
│          │   Update badge    │                   │                          │
│          │◀──────────────────│                   │                          │
│          │                   │                   │                          │
│                                                                             │
│   CART SUMMARY CALCULATION:                                                 │
│   ═══════════════════════════                                                │
│                                                                             │
│   for each cart_item:                                                       │
│       subtotal += price * quantity                                         │
│   delivery_fee = restaurant.delivery_fee                                    │
│   total = subtotal + delivery_fee - discount                                │
│                                                                             │
│   LiveData<CartSummary> {                                                   │
│       List<CartItem> items                                                  │
│       int itemCount                                                        │
│       int subtotal                                                         │
│       int deliveryFee                                                      │
│       int discount                                                          │
│       int total                                                            │
│       String restaurantId                                                  │
│       String restaurantName                                                │
│   }                                                                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Code Import:**

```java
// CartSummary model
public class CartSummary {
    private List<CartItem> items;
    private int itemCount;
    private int subtotal;
    private int deliveryFee;
    private int discount;
    private int total;
    private String restaurantId;
    private String restaurantName;
    
    // Getters and setters
    public static CartSummary calculate(List<CartItem> items, Restaurant restaurant, int discount) {
        int subtotal = items.stream().mapToInt(i -> i.getPrice() * i.getQuantity()).sum();
        int deliveryFee = restaurant != null ? restaurant.getDeliveryFee() : 0;
        int total = subtotal + deliveryFee - discount;
        return new CartSummary(items, items.size(), subtotal, deliveryFee, discount, total, 
            restaurant != null ? restaurant.getId() : null, restaurant != null ? restaurant.getName() : null);
    }
}

// Repository
import com.supabase.SupabaseClient;
import com.supabase.postgrest.requests.SelectRequest;
import com.google.gson.Gson;
```

---

## 3. Checkout Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       CHECKOUT FLOW                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   User taps "Tiến hành thanh toán"                                          │
│          │                                                                   │
│          ▼                                                                   │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    VALIDATION                                        │  │
│   │                                                                     │  │
│   │   1. Cart not empty?                                                │  │
│   │      ├─▶ NO ──▶ Error: "Giỏ hàng trống"                            │  │
│   │      │     ──▶ Go back                                             │  │
│   │      │                                                              │  │
│   │      └─▶ YES ──▶ Continue                                          │  │
│   │                                                                     │  │
│   │   2. Restaurant still open?                                        │  │
│   │      ├─▶ NO ──▶ Error: "Nhà hàng đóng cửa"                        │  │
│   │      │     ──▶ Clear cart, go back                                │  │
│   │      │                                                              │  │
│   │      └─▶ YES ──▶ Continue                                          │  │
│   │                                                                     │  │
│   │   3. Address selected?                                             │  │
│   │      ├─▶ NO ──▶ Error: "Chọn địa chỉ giao hàng"                   │  │
│   │      │     ──▶ Show address picker                                │  │
│   │      │                                                              │  │
│   │      └─▶ YES ──▶ Continue                                          │  │
│   │                                                                     │  │
│   │   4. Min order met?                                                 │  │
│   │      ├─▶ NO ──▶ Error: "Đơn tối thiểu {min_order}k"              │  │
│   │      │                                                              │  │
│   │      └─▶ YES ──▶ Continue                                          │  │
│   │                                                                     │  │
│   └────────────────────────────────────┬────────────────────────────────┘  │
│                                        │                                     │
│                                        ▼                                     │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                 BUILD ORDER DATA                                    │  │
│   │                                                                     │  │
│   │   {                                                                │  │
│   │       user_id: currentUser.id,                                     │  │
│   │       restaurant_id: cart.restaurantId,                          │  │
│   │       delivery_address: selectedAddress.fullAddress,             │  │
│   │       delivery_latitude: selectedAddress.latitude,               │  │
│   │       delivery_longitude: selectedAddress.longitude,             │  │
│   │       note: inputNote,                                            │  │
│   │       payment_method: 'cod',                                      │  │
│   │       promotion_code: appliedPromo?.code,                        │  │
│   │       items: cartItems.map(item => {                              │  │
│   │           menu_item_id: item.menuItemId,                         │  │
│   │           quantity: item.quantity,                               │  │
│   │           note: item.note                                         │  │
│   │       })                                                          │  │
│   │   }                                                                │  │
│   │                                                                     │  │
│   └────────────────────────────────────┬────────────────────────────────┘  │
│                                        │                                     │
│                                        ▼                                     │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    RPC: CREATE_ORDER                               │  │
│   │                                                                     │  │
│   │   SELECT rpc_create_order(                                        │  │
│   │       p_user_id := 'uuid',                                         │  │
│   │       p_restaurant_id := 'uuid',                                   │  │
│   │       p_delivery_address := '123 Main St',                         │  │
│   │       p_delivery_latitude := 10.7769,                              │  │
│   │       p_delivery_longitude := 106.7000,                            │  │
│   │       p_note := 'Gọi trước khi giao',                              │  │
│   │       p_payment_method := 'cod',                                   │  │
│   │       p_promotion_code := 'PROMO10',                               │  │
│   │       p_items := '[...]'::jsonb                                    │  │
│   │   );                                                               │  │
│   │                                                                     │  │
│   └────────────────────────────┬────────────────────────────────────────┘  │
│                                 │                                          │
│                    ┌────────────┴────────────┐                              │
│                    │                         │                              │
│                SUCCESS                     ERROR                           │
│                    │                         │                              │
│                    ▼                         ▼                              │
│   ┌────────────────────────┐  ┌────────────────────────────────────────┐   │
│   │ • Clear local cart    │  │ • Show error message                  │   │
│   │   (Room DB)           │  │ • Keep cart intact                    │   │
│   │ • Navigate to         │  │ • Allow retry                         │   │
│   │   OrderSuccess        │  │                                       │   │
│   │ • Show order code     │  │                                       │   │
│   │ • Send local notif   │  │                                       │   │
│   └────────────────────────┘  └────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Code Import:**

```java
// CheckoutViewModel
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.supabase.SupabaseClient;
import com.supabase.postgrest.requests.RpcRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

// Order creation
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

// CheckoutActivity
import android.content.Intent;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

// Validation
import android.text.TextUtils;
import java.util.regex.Pattern;

// Address selection
import android.widget.AdapterView;
import androidx.recyclerview.widget.RecyclerView;
```

---

## 4. Order Success Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     ORDER SUCCESS FLOW                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │                     ORDER SUCCESS SCREEN                           │  │
│   │                                                                     │  │
│   │   ┌──────────────────────────────────────────────────────────────┐  │  │
│   │   │                                                              │  │  │
│   │   │                    ✅                                         │  │  │
│   │   │           Đặt hàng thành công!                              │  │  │
│   │   │                                                              │  │  │
│   │   └──────────────────────────────────────────────────────────────┘  │  │
│   │                                                                     │  │
│   │   ───────────────────────────────────────────────────────────────    │  │
│   │                                                                     │  │
│   │   Mã đơn hàng: ORD20260404001                                     │  │
│   │   Nhà hàng: Restaurant A                                          │  │
│   │   Tổng tiền: 125,000 VNĐ                                         │  │
│   │   Phương thức: Tiền mặt (COD)                                    │  │
│   │                                                                     │  │
│   │   ───────────────────────────────────────────────────────────────    │  │
│   │                                                                     │  │
│   │   📍 Giao đến: 123 Nguyễn Trãi, Quận 1, HCM                      │  │
│   │                                                                     │  │
│   │   🕒 Dự kiến giao: 30-45 phút                                    │  │
│   │                                                                     │  │
│   │   ───────────────────────────────────────────────────────────────    │  │
│   │                                                                     │  │
│   │   ┌─────────────────────┐  ┌─────────────────────┐                 │  │
│   │   │   [Xem đơn hàng]   │  │  [Về trang chủ]    │                 │  │
│   │   └─────────────────────┘  └─────────────────────┘                 │  │
│   │                                                                     │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   BUTTON ACTIONS:                                                          │
│   ═════════════════                                                        │
│                                                                             │
│   [Xem đơn hàng] ──▶ Navigate to OrderDetailActivity                       │
│   [Về trang chủ] ──▶ Navigate to HomeFragment (Tab 0)                      │
│                                                                             │
│   REALTIME SUBSCRIPTION (Background):                                      │
│   ═══════════════════════════════════                                      │
│                                                                             │
│   Continue listening to order status changes                               │
│   Even after leaving this screen                                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 5. Cart UI Layout

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CART UI LAYOUT                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  Toolbar: Giỏ hàng                        [Chỉnh sửa]              │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  Restaurant: Restaurant A                    [Thay đổi]           │  │
│   │  🕒 08:00 - 22:00 • 🚚 15,000 VNĐ                               │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   ──────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  ┌────────┐  ┌─────────────────────────────────────────────────┐   │  │
│   │  │        │  │  Phở Bò Nạm (x2)                    100,000  │   │  │
│   │  │  img   │  │  [+][2][-]                                   │   │  │
│   │  │        │  │  Note: Nhiều rau, ít nước                   │   │  │
│   │  └────────┘  └─────────────────────────────────────────────────┘   │  │
│   │                                                              [🗑️] │  │
│   │  ┌────────┐  ┌─────────────────────────────────────────────────┐   │  │
│   │  │        │  │  Trà Đá (x2)                       20,000    │   │  │
│   │  │  img   │  │  [+][2][-]                                   │   │  │
│   │  │        │  │                                               │   │  │
│   │  └────────┘  └─────────────────────────────────────────────────┘   │  │
│   │                                                              [🗑️] │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   ──────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   Mã giảm giá                                                               │
│   ┌────────────────────────────────┐  ┌──────────────┐                     │
│   │ [PROMO10                    ] │  │ [Áp dụng]   │                     │
│   └────────────────────────────────┘  └──────────────┘                     │
│   Giảm giá: -10,000 VNĐ ✓                                                │
│                                                                             │
│   ──────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  Tạm tính:            120,000 VNĐ                                │  │
│   │  Phí giao hàng:        15,000 VNĐ                                │  │
│   │  Giảm giá:            -10,000 VNĐ                                │  │
│   │  ─────────────────────────────────────                             │  │
│   │  TỔNG CỘNG:         125,000 VNĐ                                  │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  📍 Giao đến:                                                      │  │
│   │  ┌────────────────────────────────────────────────────────────┐  │  │
│   │  │ 🏠 Nhà (Mặc định) - 123 Nguyễn Trãi, Q1      [Thay đổi]  │  │  │
│   │  └────────────────────────────────────────────────────────────┘  │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │  Ghi chú cho đơn hàng (tùy chọn)                                 │  │
│   │  [________________________________________]                      │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   ┌────────────────────────────────────────────────────────────────────┐  │
│   │                                                                     │  │
│   │                  [💳 Đặt hàng (125,000 VNĐ)]                      │  │
│   │                                                                     │  │
│   └────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```