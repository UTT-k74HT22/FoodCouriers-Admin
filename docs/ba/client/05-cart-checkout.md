# Module: Giỏ hàng & Thanh toán - App Client

## 1. Overview
Quản lý giỏ hàng và thanh toán đơn hàng (COD).

## 2. Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | View Cart | Xem danh sách món trong giỏ |
| 2 | Update Quantity | Tăng/giảm số lượng |
| 3 | Remove Item | Xóa món khỏi giỏ |
| 4 | Apply Promo | Nhập mã khuyến mãi |
| 5 | Select Address | Chọn địa chỉ giao hàng |
| 6 | Add Address | Thêm địa chỉ mới |
| 7 | Order Note | Ghi chú cho đơn |
| 8 | Place Order | Đặt hàng COD |
| 9 | Order Success | Màn hình thành công |

## 3. Cart Flow

```
┌──────────────────────────────────────────────────────────────┐
│                       CART FLOW                               │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │                 CART FRAGMENT                           │  │
│  │  Restaurant: Restaurant A                              │  │
│  │  ─────────────────────────────────────────────────────  │  │
│  │  ┌────────┐ ┌──────────────────────────────────────┐   │  │
│  │  │  Img   │ │ Phở Bò (x2)  100k                  │   │  │
│  │  │        │ │ [+][1][-]              🗑️           │   │  │
│  │  └────────┘ └──────────────────────────────────────┘   │  │
│  │  ┌────────┐ ┌──────────────────────────────────────┐   │  │
│  │  │  Img   │ │ Trà đá (x2)   20k                   │   │  │
│  │  │        │ │ [+][2][-]              🗑️           │   │  │
│  │  └────────┘ └──────────────────────────────────────┘   │  │
│  │                                                        │  │
│  │  Mã giảm giá: [___________] [Áp dụng]                │  │
│  │  ─────────────────────────────────────────────────────  │  │
│  │  Tạm tính: 120k                                       │  │
│  │  Phí giao hàng: 15k                                   │  │
│  │  Giảm giá: -10k                                       │  │
│  │  ─────────────────────────────────────────────────────  │  │
│  │  Tổng cộng: 125k                                      │  │
│  │                                                        │  │
│  │  [Đặt hàng (125k)]                                    │  │
│  └────────────────────────────────────────────────────────┘  │
│                           │                                   │
│                           ▼                                   │
│  ┌────────────────────────────────────────────────────────┐  │
│  │               CHECKOUT ACTIVITY                        │  │
│  │  ┌────────────────────────────────────────────────────┐│  │
│  │  │  📍 Địa chỉ giao hàng                             ││  │
│  │  │  [Chọn địa chỉ ▼]                                ││  │
│  │  │  [+] Thêm địa chỉ mới                            ││  │
│  │  └────────────────────────────────────────────────────┘│  │
│  │  ─────────────────────────────────────────────────────  │  │
│  │  Ghi chú: [________________________]                   │  │
│  │  ─────────────────────────────────────────────────────  │  │
│  │  Phương thức thanh toán: Tiền mặt (COD)              │  │
│  │  ─────────────────────────────────────────────────────  │  │
│  │  Tổng: 125k                                            │  │
│  │                                                        │  │
│  │  [Đặt hàng]                                            │  │
│  └────────────────────────────────────────────────────────┘  │
│                           │                                   │
│                           ▼                                   │
│  ┌────────────────────────────────────────────────────────┐  │
│  │               ORDER SUCCESS                            │  │
│  │  ✅ Đặt hàng thành công!                              │  │
│  │  Mã đơn hàng: ORD20240404001                          │  │
│  │  Tổng tiền: 125k                                      │  │
│  │  Dự kiến giao: 30-45 phút                             │  │
│  │                                                        │  │
│  │  [Xem đơn hàng] [Về trang chủ]                        │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

## 4. Data Flow

### 4.1 Cart (Local - Room)
```
Load Cart:
ViewModel.getCart()
       │
       ▼
CartRepository.getCart()
       │
       ▼
Room DB: SELECT * FROM cart_items
       │
       ▼
LiveData<List<CartItem>> + calculate total
```

### 4.2 Checkout (Server)
```
User taps "Đặt hàng"
       │
       ▼
Validate:
  - Cart not empty?
  - Restaurant open?
  - Address selected?
  - Min order met?
       │
       ▼
Build order data:
{
    user_id: currentUser.id,
    restaurant_id: cart.restaurantId,
    delivery_address: selectedAddress.fullAddress,
    note: inputNote,
    payment_method: 'cod',
    promotion_code: appliedCode,
    items: cartItems.map(...)
}
       │
       ▼
Call RPC: rpc_create_order(items)
       │
       ├─▶ Success:
       │    - Clear cart (Room)
       │    - Navigate OrderSuccess
       │
       └─▶ Error:
            - Show error message
```

## 5. API Endpoints

### 5.1 Apply Promotion
```sql
SELECT rpc_apply_promotion(
    p_code := 'PROMO10',
    p_order_total := 120000
);
```

### 5.2 Create Order (RPC)
```sql
SELECT rpc_create_order(
    p_user_id := 'uuid',
    p_restaurant_id := 'uuid',
    p_delivery_address := '123 Main St',
    p_delivery_latitude := 10.7769,
    p_delivery_longitude := 106.7000,
    p_note := 'Gọi trước khi giao',
    p_payment_method := 'cod',
    p_promotion_code := 'PROMO10',
    p_items := '[{"menu_item_id":"uuid","quantity":2,"note":""}]'::jsonb
);
```

## 6. UI Components

### 6.1 Cart Fragment (fragment_cart.xml)
```xml
<LinearLayout android:orientation="vertical">
    <TextView android:id="@+id/tvRestaurantName"/>
    
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvCartItems"/>
    
    <!-- Promo Section -->
    <LinearLayout>
        <EditText android:id="@+id/etPromoCode"/>
        <Button android:id="@+id/btnApplyPromo"/>
    </LinearLayout>
    
    <!-- Summary -->
    <TextView android:id="@+id/tvSubtotal"/>
    <TextView android:id="@+id/tvDeliveryFee"/>
    <TextView android:id="@+id/tvDiscount"/>
    <TextView android:id="@+id/tvTotal"/>
    
    <Button android:id="@+id/btnCheckout"/>
</LinearLayout>
```

### 6.2 Checkout Activity (activity_checkout.xml)
```xml
<ScrollView>
    <LinearLayout>
        <!-- Address Section -->
        <TextView android:text="Địa chỉ giao hàng"/>
        <Spinner android:id="@+id/spinnerAddress"/>
        <Button android:id="@+id/btnAddAddress" android:text="Thêm địa chỉ"/>
        
        <!-- Note -->
        <EditText android:id="@+id/etNote" android:hint="Ghi chú cho đơn hàng"/>
        
        <!-- Payment -->
        <TextView android:text="Thanh toán"/>
        <TextView android:text="Tiền mặt (COD)"/>
        
        <!-- Summary -->
        <TextView android:id="@+id/tvTotal"/>
        
        <Button android:id="@+id/btnPlaceOrder"/>
    </LinearLayout>
</ScrollView>
```

## 7. ViewModel

```java
public class CartViewModel extends ViewModel {
    private MutableLiveData<List<CartItem>> cartItems = new MutableLiveData<>();
    private MutableLiveData<CartSummary> cartSummary = new MutableLiveData<>();
    private MutableLiveData<List<Address>> addresses = new MutableLiveData<>();
    private MutableLiveData<Promotion> appliedPromo = new MutableLiveData<>();
    private MutableLiveData<OrderResult> orderResult = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();

    public void loadCart() { }
    public void updateQuantity(String cartItemId, int quantity) { }
    public void removeItem(String cartItemId) { }
    public void applyPromo(String code) { }
    public void loadAddresses() { }
    public void placeOrder(String addressId, String note) { }
}
```

## 8. Validation Rules

| Check | Rule |
|-------|------|
| Cart empty | Error: "Giỏ hàng trống" |
| Restaurant closed | Error: "Nhà hàng đóng cửa" |
| No address | Error: "Chọn địa chỉ giao hàng" |
| Min order | Must >= restaurant.min_order |
| Promo invalid | Show "Mã không hợp lệ" |
| Promo expired | Show "Mã đã hết hạn" |

## 9. Edge Cases

| Case | Handling |
|------|----------|
| Cart empty | Show empty state, hide checkout button |
| Network error | Show error, keep cart |
| Order timeout | Allow retry |
| Concurrent modification | Handle gracefully |