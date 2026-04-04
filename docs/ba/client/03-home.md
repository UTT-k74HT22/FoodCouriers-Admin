# Module: Trang chủ - App Client

## 1. Overview
Màn hình chính hiển thị banners, danh mục, nhà hàng nổi bật và gần bạn.

## 2. Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | Banner Carousel | ViewPager2 slides |
| 2 | Categories | Danh mục món ăn (horizontal scroll) |
| 3 | Featured Restaurants | Nhà hàng nổi bật (is_featured) |
| 4 | Near Restaurants | Nhà hàng gần vị trí user |
| 5 | Search Bar | Chuyển đến màn hình search |
| 6 | Pull to Refresh | Cập nhật dữ liệu |

## 3. Data Flow

```
onViewCreated()
       │
       ▼
loadHomeData() ── Parallel calls:
       │
       ├─▶ getBanners() ──▶ Supabase: banners?is_active=true
       │
       ├─▶ getCategories() ──▶ Supabase: categories?is_active=true&order=sort_order
       │
       ├─▶ getFeaturedRestaurants() ──▶ Supabase: restaurants?is_featured=true&is_open=true
       │
       └─▶ getNearRestaurants() ──▶ Supabase: restaurants?is_open=true&order=rating.desc
               │
               ▼
       LiveData<HomeData> ──▶ Combine all ──▶ UI Update
```

## 4. API Endpoints

### 4.1 Banners
```
GET /rest/v1/banners?is_active=true&start_date=lte.now()&end_date=gte.now()&order=sort_order
```

### 4.2 Categories
```
GET /rest/v1/categories?is_active=true&order=sort_order.asc
```

### 4.3 Featured Restaurants
```
GET /rest/v1/restaurants?is_featured=true&is_open=true&is_active=true&order=rating.desc&limit=10
```

### 4.4 Near Restaurants
```
GET /rest/v1/restaurants?is_open=true&is_active=true&order=rating.desc&limit=20
```

## 5. UI Components

### 5.1 Layout (fragment_home.xml)
```xml
<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
    <androidx.core.widget.NestedScrollView>
        <LinearLayout android:orientation="vertical">
            <!-- Search Bar -->
            <com.google.android.material.card.MaterialCardView
                android:id="@+id/searchCard">
                <TextView android:text="🔍 Tìm nhà hàng, món ăn..."/>
            </com.google.android.material.card.MaterialCardView>

            <!-- Banner Carousel -->
            <androidx.viewpager2.widget.ViewPager2
                android:id="@+id/vpBanners"/>

            <!-- Categories -->
            <TextView android:text="Danh mục"/>
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rvCategories"
                android:orientation="horizontal"/>

            <!-- Featured Section -->
            <LinearLayout>
                <TextView android:text="Nổi bật"/>
                <TextView android:text="Xem thêm" android:id="@+id/tvSeeAllFeatured"/>
            </LinearLayout>
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rvFeatured"/>

            <!-- Near Section -->
            <LinearLayout>
                <TextView android:text="Gần bạn"/>
                <TextView android:text="Xem thêm" android:id="@+id/tvSeeAllNear"/>
            </LinearLayout>
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rvNear"/>
        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
</androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
```

## 6. ViewModel

```java
public class HomeViewModel extends ViewModel {
    private MutableLiveData<List<Banner>> banners = new MutableLiveData<>();
    private MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private MutableLiveData<List<Restaurant>> featuredRestaurants = new MutableLiveData<>();
    private MutableLiveData<List<Restaurant>> nearRestaurants = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public void loadHomeData() { }
}
```

## 7. Banner Adapter

```java
public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.ViewHolder> {
    // ViewPager2 with auto-scroll (every 5 seconds)
    // Dot indicators below
}
```

## 8. Edge Cases

| Case | Handling |
|------|----------|
| No banners | Hide banner section |
| No categories | Show "Chưa có danh mục" |
| No restaurants | Show "Không có nhà hàng nào" |
| Location denied | Show all restaurants (not filtered by location) |