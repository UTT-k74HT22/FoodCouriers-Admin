# Module: Chi tiết nhà hàng - App Client

## 1. Overview
Xem chi tiết nhà hàng và menu. Thêm món vào giỏ hàng.

## 2. Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | Restaurant Info | Tên, ảnh, địa chỉ, đánh giá, giờ mở cửa |
| 2 | Menu by Category | TabLayout lọc theo danh mục |
| 3 | Add to Cart | Thêm món với số lượng |
| 4 | Quantity Control | Tăng/giảm số lượng |
| 5 | Item Note | Ghi chú cho món |
| 6 | Featured Badge | Hiển thị món nổi bật |

## 3. User Flow

```
┌──────────────────────────────────────────────────────────────┐
│              RESTAURANT DETAIL FLOW                           │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Load Restaurant + Menu                                      │
│         │                                                    │
│         ▼                                                    │
│  Display Header + Menu                                       │
│         │                                                    │
│         ▼                                                    │
│  User scrolls categories ──▶ Filter menu items              │
│         │                                                    │
│         ▼                                                    │
│  User taps "+" on item                                       │
│         │                                                    │
│         ▼                                                    │
│  Check: Is same restaurant as cart?                          │
│         │                                                    │
│     ┌────┴────┐                                              │
│     │         │                                              │
│   Yes       No                                               │
│     │         │                                              │
│     ▼         ▼                                              │
│  Add to    Show ConfirmDialog:                               │
│  Cart       "Giỏ hàng có món từ {restaurant}.              │
│             Xóa và thêm món mới?"                            │
│             │                                                │
│         ┌────┴────┐                                          │
│         │         │                                          │
│       Yes       No                                           │
│         │         │                                          │
│         ▼         ▼                                          │
│      Clear   Cancel                                          │
│      Cart +                                                  │
│      Add to                                                  │
│      Cart                                                    │
│                                                              │
│         │                                                    │
│         ▼                                                    │
│  Show Snackbar: "Đã thêm {name} vào giỏ hàng"              │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

## 4. API Endpoints

### 4.1 Restaurant Detail
```
GET /rest/v1/restaurants?id=eq.{id}
```

### 4.2 Menu Items
```
GET /rest/v1/menu_items?restaurant_id=eq.{id}&is_available=true&select=*,category:categories(*)
```

### 4.3 Restaurant Reviews
```
GET /rest/v1/reviews?restaurant_id=eq.{id}&order=created_at.desc&limit=10
```

## 5. UI Components

### 5.1 Layout (activity_restaurant_detail.xml)
```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout>
    <com.google.android.material.appbar.AppBarLayout>
        <com.google.android.material.appbar.CollapsingToolbarLayout>
            <ImageView android:id="@+id/ivRestaurant"/>
            <Toolbar android:id="@+id/toolbar"/>
        </com.google.android.material.appbar.CollapsingToolbarLayout>
        
        <LinearLayout>
            <TextView android:id="@+id/tvName"/>
            <TextView android:id="@+id/tvRating"/>
            <TextView android:id="@+id/tvAddress"/>
            <TextView android:id="@+id/tvInfo"/>
        </LinearLayout>
        
        <com.google.android.material.tabs.TabLayout
            android:id="@+id/tabLayout"/>
    </com.google.android.material.appbar.AppBarLayout>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvMenu"/>

    <!-- Cart Floating Button -->
    <com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
        android:id="@+id/fabCart"/>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### 5.2 Menu Item (item_menu.xml)
```xml
<com.google.android.material.card.MaterialCardView>
    <LinearLayout android:orientation="horizontal">
        <ImageView android:id="@+id/ivItem"/>
        <LinearLayout android:layout_weight="1">
            <TextView android:id="@+id/tvName"/>
            <TextView android:id="@+id/tvDescription"/>
            <TextView android:id="@+id/tvPrice"/>
            <TextView android:id="@+id/tvFeatured" android:visibility="gone"/>
        </LinearLayout>
        <LinearLayout android:gravity="center">
            <ImageButton android:id="@+id/btnMinus"/>
            <TextView android:id="@+id/tvQuantity"/>
            <ImageButton android:id="@+id/btnPlus"/>
        </LinearLayout>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

## 6. ViewModel

```java
public class RestaurantViewModel extends ViewModel {
    private MutableLiveData<Restaurant> restaurant = new MutableLiveData<>();
    private MutableLiveData<List<MenuByCategory>> menuByCategory = new MutableLiveData<>();
    private MutableLiveData<CartSummary> cartSummary = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();

    public void loadRestaurant(String restaurantId) { }
    public void loadMenu(String restaurantId) { }
    public void addToCart(MenuItem item, int quantity, String note) { }
    public void updateCartItemQuantity(String cartItemId, int quantity) { }
}
```

## 7. Add to Cart Logic

```java
public void addToCart(MenuItem item, int quantity, String note) {
    // 1. Check item available
    if (!item.isAvailable()) {
        error.postValue("Món này hiện không có");
        return;
    }

    // 2. Check restaurant open
    if (!restaurant.isOpen()) {
        error.postValue("Nhà hàng đóng cửa");
        return;
    }

    // 3. Check same restaurant in cart
    CartSummary currentCart = cartRepository.getCurrentCart();
    if (currentCart != null && !currentCart.getRestaurantId().equals(restaurantId)) {
        showConfirmDialog();
        return;
    }

    // 4. Add to cart (Room DB)
    cartRepository.addItem(item, quantity, note);
}
```

## 8. Edge Cases

| Case | Handling |
|------|----------|
| Item not available | Disable "+" button, show "Hết hàng" |
| Restaurant closed | Show overlay "Nhà hàng đóng cửa" |
| Cart from other restaurant | Confirm dialog |
| Quantity = 0 | Remove item from cart |