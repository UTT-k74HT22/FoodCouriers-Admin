# FoodDelivery — App Quản Trị (Admin)

> Ứng dụng Android quản trị hệ thống giao đồ ăn dành cho Admin và Staff vận hành.
> Nền tảng: **Java Android · XML Layout · Supabase**

---

## Mục Lục

- [Tổng quan](#tổng-quan)
- [Vai trò & Phân quyền](#vai-trò--phân-quyền)
- [Tính năng](#tính-năng)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Cấu trúc project](#cấu-trúc-project)
- [Hướng dẫn cài đặt](#hướng-dẫn-cài-đặt)
- [Kiến trúc ứng dụng](#kiến-trúc-ứng-dụng)
- [Màn hình chính](#màn-hình-chính)
- [Luồng xử lý đơn hàng](#luồng-xử-lý-đơn-hàng)
- [Phân quyền & RLS](#phân-quyền--rls)
- [Design System](#design-system)
- [Roadmap & Phases](#roadmap--phases)

---

## Tổng Quan

**FoodDelivery Admin** là ứng dụng Android nội bộ cho phép Admin hệ thống và Staff quản lý quán thực hiện toàn bộ vận hành:

- Theo dõi và xử lý đơn hàng theo thời gian thực
- Quản lý nhà hàng, thực đơn, danh mục
- Quản lý tài khoản người dùng
- Xem báo cáo doanh thu cơ bản

> **Lưu ý:** App này chỉ dành cho nội bộ. Không public lên CH Play. Phân phối qua APK file nội bộ.

| Thông tin | Chi tiết |
|---|---|
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 14 (API 34) |
| Ngôn ngữ | Java |
| Layout | XML |
| Architecture | MVVM + Repository Pattern |
| Backend | Supabase (PostgreSQL + Auth + Storage + Realtime) |

---

## Vai Trò & Phân Quyền

Hệ thống có 2 role dành cho Admin App. Phân quyền được enforce ở **cả tầng app lẫn Supabase RLS** — không chỉ ở client.

| Vai trò | Quyền hạn |
|---|---|
| **Admin** | Toàn quyền: quản lý tất cả nhà hàng, menu, đơn hàng, người dùng, báo cáo, danh mục |
| **Staff** | Quản lý menu và đơn hàng của **nhà hàng mình được gán** — không thấy dữ liệu nhà hàng khác |

> Role được lưu trong field `role` của bảng `users` (PostgreSQL). Supabase RLS tự động giới hạn data trả về theo role.

---

## Tính Năng

### Dashboard

- Thẻ số liệu: tổng đơn hôm nay, doanh thu hôm nay, đơn đang xử lý
- Danh sách đơn hàng mới cần xử lý (realtime)
- Top món ăn bán chạy

> MVP: hiển thị dạng text/số. Phase 2 bổ sung biểu đồ MPAndroidChart.

### Quản Lý Đơn Hàng

- Danh sách đơn hàng chia theo tab trạng thái: Chờ xác nhận / Đã xác nhận / Đang chuẩn bị / Đang giao / Hoàn thành / Đã hủy
- Tìm kiếm theo mã đơn, tên khách hàng
- Xem chi tiết đơn: thông tin khách, danh sách món, địa chỉ giao, ghi chú, tổng tiền
- Cập nhật trạng thái đơn theo thứ tự hợp lệ
- Nhận thông báo realtime khi có đơn mới (Supabase Realtime)

**Trạng thái đơn hàng hợp lệ:**

```
pending → confirmed → preparing → delivering → delivered
                                              ↘ cancelled (từ pending hoặc confirmed)
```

### Quản Lý Nhà Hàng

- Danh sách nhà hàng với toggle bật/tắt
- Thêm / sửa / xóa nhà hàng
- Upload ảnh nhà hàng (Supabase Storage)
- Thông tin: tên, địa chỉ, số điện thoại, giờ mở cửa, phí giao hàng tối thiểu

### Quản Lý Danh Mục

- Danh sách danh mục toàn hệ thống (Cơm, Phở, Pizza, ...)
- Thêm / sửa / xóa danh mục
- Upload ảnh danh mục
- Sắp xếp thứ tự hiển thị

### Quản Lý Thực Đơn

- Danh sách món ăn theo nhà hàng, lọc theo danh mục
- Thêm / sửa / xóa món ăn
- Upload ảnh món ăn (Supabase Storage)
- Toggle bật/tắt hiển thị từng món (is_available)
- Đánh dấu món nổi bật (is_featured)

### Quản Lý Người Dùng

- Danh sách tài khoản khách hàng
- Tìm kiếm theo tên, email, số điện thoại
- Xem chi tiết người dùng và lịch sử đơn hàng
- Kích hoạt / vô hiệu hóa tài khoản

### Báo Cáo

- Chọn khoảng thời gian (hôm nay / tuần / tháng / tùy chọn)
- Tổng doanh thu, tổng số đơn, trung bình/đơn, tỉ lệ hủy
- Top 5 món ăn bán chạy
- Biểu đồ doanh thu theo ngày (Phase 2 — MPAndroidChart)

### Quản Lý Banner & Khuyến Mãi (Phase 2)

- Tạo / sửa / xóa banner hiển thị trên Client App
- Tạo mã giảm giá: theo % hoặc số tiền cố định
- Cài đặt giới hạn sử dụng, ngày hiệu lực, đơn tối thiểu

---

## Công Nghệ Sử Dụng

### Supabase Platform

| Dịch vụ | Mục đích |
|---|---|
| **Supabase Auth** | Đăng nhập admin/staff, JWT token, session management |
| **PostgreSQL** | Database chính: users, orders, restaurants, menu_items, ... |
| **Row Level Security (RLS)** | Phân quyền dữ liệu theo role tại tầng database |
| **Supabase Storage** | Upload ảnh nhà hàng, món ăn |
| **Supabase Realtime** | Nhận đơn hàng mới theo thời gian thực (WebSocket) |
| **PostgREST** | REST API tự động từ schema — không cần viết API endpoint |
| **PostgreSQL Functions (RPC)** | Business logic phức tạp: đặt hàng, báo cáo |

### Android Libraries

| Thư viện | Version | Mục đích |
|---|---|---|
| Supabase Kotlin SDK | 2.x | Client giao tiếp với Supabase |
| Glide | 4.16.0 | Tải và cache hình ảnh |
| Material Design 3 | 1.10.0 | UI Components |
| Room | 2.x | Local cache (nếu cần offline) |
| MPAndroidChart | 3.1.0 | Biểu đồ báo cáo (Phase 2) |
| Navigation Component | 2.x | Điều hướng Fragment |

> **Lưu ý về SDK:** Supabase SDK chính thức viết bằng Kotlin nhưng tương thích với Java project qua interop. Nếu gặp vấn đề tương thích, fallback là gọi Supabase REST API trực tiếp qua OkHttp.

---

## Cấu Trúc Project

```
app/
└── src/main/
    └── java/com/fooddelivery/admin/
        │
        ├── data/
        │   ├── model/                          # Domain models (Java POJOs)
        │   │   ├── User.java
        │   │   ├── Restaurant.java
        │   │   ├── Category.java
        │   │   ├── MenuItem.java
        │   │   ├── Order.java
        │   │   ├── OrderItem.java
        │   │   ├── OrderStatusLog.java
        │   │   └── RevenueReport.java
        │   │
        │   ├── remote/
        │   │   ├── SupabaseClient.java          # Singleton Supabase client
        │   │   └── dto/                         # Data Transfer Objects (JSON mapping)
        │   │       ├── OrderDto.java
        │   │       ├── MenuItemDto.java
        │   │       └── RestaurantDto.java
        │   │
        │   └── repository/
        │       ├── AuthRepository.java          # Đăng nhập, session, role check
        │       ├── OrderRepository.java         # Lấy/cập nhật đơn hàng
        │       ├── RestaurantRepository.java    # CRUD nhà hàng
        │       ├── MenuRepository.java          # CRUD món ăn
        │       ├── CategoryRepository.java      # CRUD danh mục
        │       ├── UserRepository.java          # Quản lý user
        │       └── ReportRepository.java        # Báo cáo (gọi RPC)
        │
        ├── domain/
        │   └── usecase/
        │       ├── UpdateOrderStatusUseCase.java  # Validate transition + log
        │       └── GetOrderReportUseCase.java
        │
        ├── ui/
        │   ├── auth/
        │   │   ├── AdminLoginActivity.java
        │   │   └── activity_admin_login.xml
        │   │
        │   ├── main/
        │   │   ├── AdminMainActivity.java        # NavigationDrawer container
        │   │   └── activity_admin_main.xml
        │   │
        │   ├── dashboard/
        │   │   ├── DashboardFragment.java
        │   │   ├── fragment_dashboard.xml
        │   │   └── adapter/
        │   │       └── RecentOrderAdapter.java
        │   │
        │   ├── order/
        │   │   ├── OrderListFragment.java        # TabLayout 6 trạng thái
        │   │   ├── OrderDetailActivity.java      # Xem chi tiết + cập nhật status
        │   │   ├── fragment_order_list.xml
        │   │   ├── activity_order_detail.xml
        │   │   └── adapter/
        │   │       └── OrderAdapter.java
        │   │
        │   ├── restaurant/
        │   │   ├── RestaurantListFragment.java
        │   │   ├── RestaurantFormActivity.java   # Thêm / sửa nhà hàng
        │   │   └── adapter/
        │   │       └── RestaurantAdapter.java
        │   │
        │   ├── menu/
        │   │   ├── MenuListFragment.java         # Lọc theo nhà hàng & category
        │   │   ├── MenuItemFormActivity.java     # Thêm / sửa món ăn + upload ảnh
        │   │   └── adapter/
        │   │       └── MenuItemAdapter.java
        │   │
        │   ├── category/
        │   │   ├── CategoryListFragment.java
        │   │   ├── CategoryFormActivity.java
        │   │   └── adapter/
        │   │       └── CategoryAdapter.java
        │   │
        │   ├── user/
        │   │   ├── UserListActivity.java
        │   │   ├── UserDetailActivity.java       # Lịch sử đơn + deactivate
        │   │   └── adapter/
        │   │       └── UserAdapter.java
        │   │
        │   ├── report/
        │   │   ├── ReportFragment.java
        │   │   └── fragment_report.xml
        │   │
        │   └── common/
        │       ├── BaseActivity.java
        │       ├── BaseFragment.java
        │       ├── BaseViewModel.java
        │       ├── LoadingDialog.java
        │       └── ConfirmDialog.java            # Dialog xác nhận thao tác quan trọng
        │
        ├── viewmodel/
        │   ├── AuthViewModel.java
        │   ├── DashboardViewModel.java
        │   ├── OrderViewModel.java
        │   ├── RestaurantViewModel.java
        │   ├── MenuViewModel.java
        │   ├── CategoryViewModel.java
        │   ├── UserViewModel.java
        │   └── ReportViewModel.java
        │
        └── utils/
            ├── Constants.java
            ├── SessionManager.java              # Lưu JWT token (EncryptedSharedPreferences)
            ├── CurrencyUtils.java
            ├── DateTimeUtils.java
            └── ImageUtils.java                  # Compress ảnh trước khi upload
```

---

## Hướng Dẫn Cài Đặt

### Yêu cầu

- Android Studio **Hedgehog** (2023.1.1) trở lên
- JDK 17
- Gradle 8.x
- Thiết bị / emulator Android API 26+
- Tài khoản admin đã được tạo trong Supabase

### Các bước

```bash
# 1. Clone repository
git clone https://github.com/your-org/food-delivery-admin.git
cd food-delivery-admin

# 2. Mở bằng Android Studio
# File → Open → chọn thư mục vừa clone

# 3. Tạo file local.properties (không commit lên git)
echo "SUPABASE_URL=https://your-project.supabase.co" >> local.properties
echo "SUPABASE_ANON_KEY=eyJ..." >> local.properties

# 4. Sync Gradle và chạy
# Nhấn "Sync Now" → Run (Shift + F10)
```

### Tạo tài khoản Admin đầu tiên

Chạy trên **Supabase Dashboard → SQL Editor**:

```sql
-- Bước 1: Tạo auth user qua Supabase Auth Dashboard
-- (Authentication → Users → Add user)

-- Bước 2: Cập nhật role thành admin trong bảng users
UPDATE users
SET role = 'admin'
WHERE email = 'admin@fooddelivery.vn';
```

### Biến môi trường (`local.properties`)

```properties
# Supabase project URL
SUPABASE_URL=https://xxxxxxxxxxxx.supabase.co

# Supabase anon/public key
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

> **Quan trọng:** Không commit `local.properties` lên git. File này đã được thêm vào `.gitignore`.

---

## Kiến Trúc Ứng Dụng

```
┌────────────────────────────────────────────────┐
│                   UI Layer                      │
│   Activity / Fragment / Adapter / XML Layout    │
│   NavigationDrawer điều hướng các module        │
└───────────────────┬────────────────────────────┘
                    │ observe LiveData / call
┌───────────────────▼────────────────────────────┐
│              ViewModel Layer                    │
│   Xử lý business logic, giữ trạng thái UI      │
│   Không phụ thuộc Android framework             │
│   DashboardVM / OrderVM / MenuVM / ReportVM...  │
└───────────────────┬────────────────────────────┘
                    │ call
┌───────────────────▼────────────────────────────┐
│   Domain Layer (UseCase — chỉ khi cần thiết)   │
│   UpdateOrderStatusUseCase                      │
│   GetOrderReportUseCase                         │
└───────────────────┬────────────────────────────┘
                    │ call
┌───────────────────▼────────────────────────────┐
│            Repository Layer                     │
│   Tổng hợp & transform dữ liệu từ Supabase     │
│   OrderRepo / RestaurantRepo / MenuRepo...      │
└───────────────────┬────────────────────────────┘
                    │ query / subscribe
┌───────────────────▼────────────────────────────┐
│              Supabase Platform                  │
│   Auth (JWT)  │  PostgreSQL  │  Storage         │
│   PostgREST   │  Realtime    │  RPC Functions   │
└────────────────────────────────────────────────┘
```

**Điểm thiết kế quan trọng:**

- Dùng **NavigationDrawer** thay vì BottomNavigation — phù hợp UX quản trị nhiều module
- Supabase **Realtime subscription** trên màn hình đơn hàng — tự cập nhật khi có đơn mới từ client
- **UpdateOrderStatusUseCase** validate state machine trước khi gọi API (tránh transition không hợp lệ)
- Toàn bộ write operation quan trọng (đặt hàng, cập nhật status) qua **PostgreSQL RPC** để đảm bảo atomic transaction

---

## Màn Hình Chính

| Màn hình | File | Mô tả |
|---|---|---|
| Đăng nhập | `AdminLoginActivity` | Email + password, kiểm tra role admin/staff |
| Dashboard | `DashboardFragment` | Số liệu tổng quan, đơn hàng mới cần xử lý |
| Danh sách đơn | `OrderListFragment` | TabLayout 6 trạng thái, cập nhật realtime |
| Chi tiết đơn | `OrderDetailActivity` | Xem chi tiết + nút cập nhật trạng thái |
| Nhà hàng | `RestaurantListFragment` | Danh sách + toggle bật/tắt |
| Thêm/sửa nhà hàng | `RestaurantFormActivity` | Form thông tin + upload ảnh |
| Thực đơn | `MenuListFragment` | Lọc theo nhà hàng và danh mục |
| Thêm/sửa món | `MenuItemFormActivity` | Form + upload ảnh lên Supabase Storage |
| Danh mục | `CategoryListFragment` | CRUD danh mục hệ thống |
| Người dùng | `UserListActivity` | Tìm kiếm, xem lịch sử, deactivate |
| Chi tiết user | `UserDetailActivity` | Thông tin + lịch sử đơn hàng |
| Báo cáo | `ReportFragment` | Doanh thu, thống kê theo khoảng thời gian |

---

## Luồng Xử Lý Đơn Hàng

```
Khách đặt đơn (Client App)
         |
         ↓
[Supabase] orders.status = 'pending'
         |
         ↓ Supabase Realtime event
[Admin App nhận notification]
Badge tăng trên menu Drawer
         |
         ↓
Admin xem đơn → nhấn "Xác nhận"
[UpdateOrderStatusUseCase.execute(orderId, 'confirmed')]
  - Validate: pending → confirmed ✓
  - UPDATE orders SET status = 'confirmed'
  - INSERT order_status_logs (audit trail)
  - INSERT notifications cho customer
         |
         ↓
Admin cập nhật: "Đang chuẩn bị" → "Đang giao" → "Hoàn thành"
(Mỗi bước đều ghi vào order_status_logs)
         |
         ↓
[Client App nhận Realtime event]
Màn hình tracking tự động cập nhật trạng thái
```

**State machine hợp lệ:**

```
pending ──→ confirmed ──→ preparing ──→ delivering ──→ delivered
   │              │
   └──────────────┴──→ cancelled
```

> Mọi transition không nằm trong sơ đồ trên sẽ bị **UpdateOrderStatusUseCase từ chối** trước khi gọi API.

---

## Phân Quyền & RLS

Phân quyền được áp dụng tại **2 tầng**:

### Tầng App (ViewModel)

```java
// AdminMainActivity.java
if (!session.getRole().equals("admin") && !session.getRole().equals("staff")) {
    // Redirect về login, không cho vào
}

// Staff chỉ thấy menu "Đơn hàng" và "Thực đơn"
if (session.getRole().equals("staff")) {
    hideAdminOnlyMenuItems(); // Ẩn User management, Category, Report
}
```

### Tầng Database (Supabase RLS)

```sql
-- Admin thấy và sửa tất cả orders
CREATE POLICY "Admin full access orders"
ON orders FOR ALL
USING (
    EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
);

-- Staff chỉ thấy orders của nhà hàng mình
CREATE POLICY "Staff access own restaurant orders"
ON orders FOR SELECT
USING (
    restaurant_id IN (
        SELECT restaurant_id FROM staff_restaurants
        WHERE user_id = (SELECT id FROM users WHERE auth_id = auth.uid())
    )
);

-- Chỉ admin mới CRUD được categories
CREATE POLICY "Admin only manage categories"
ON categories FOR ALL
USING (
    EXISTS (SELECT 1 FROM users WHERE auth_id = auth.uid() AND role = 'admin')
);
```

> RLS đảm bảo dù client gửi request sai, database cũng không trả về dữ liệu không được phép.

---

## Design System

| Token | Giá trị | Dùng cho |
|---|---|---|
| `colorPrimary` | `#1A237E` | Header, Navigation Drawer, nút chính |
| `colorAccent` | `#FF6B35` | Highlight, badge đơn mới, biểu đồ |
| `colorBackground` | `#F0F2F5` | Nền màn hình |
| `colorSurface` | `#FFFFFF` | Card, dialog |
| `colorSuccess` | `#4CAF50` | Đơn hoàn thành, nhà hàng đang mở |
| `colorWarning` | `#FFC107` | Đơn đang chờ xử lý (pending) |
| `colorError` | `#F44336` | Đơn đã hủy, tài khoản bị vô hiệu hóa |
| `colorInfo` | `#2196F3` | Đơn đang giao |

---

## Quy Tắc Code

- Comment tiếng Việt cho logic nghiệp vụ phức tạp
- Mọi thao tác xóa hoặc thay đổi trạng thái cần hiển thị **ConfirmDialog** trước khi thực hiện
- Không bao giờ gọi Supabase trực tiếp từ Activity/Fragment — luôn qua ViewModel → Repository
- Validate state machine của đơn hàng ở **UseCase layer**, không validate ở UI
- Mọi lỗi network cần hiển thị thông báo rõ ràng bằng Snackbar, không để app crash im lặng
- Ảnh upload phải được compress về tối đa 1MB trước khi gửi lên Supabase Storage
- JWT token lưu trong `EncryptedSharedPreferences`, không lưu plain SharedPreferences

---

## Roadmap & Phases

### Phase 1 — MVP (Tuần 1–10)

- [x] Auth: đăng nhập admin/staff với Supabase Auth
- [x] Dashboard: số liệu cơ bản (count cards)
- [x] Quản lý đơn hàng: xem + cập nhật trạng thái
- [x] Realtime: nhận đơn mới qua Supabase Realtime
- [x] Quản lý nhà hàng: CRUD + upload ảnh
- [x] Quản lý danh mục: CRUD
- [x] Quản lý thực đơn: CRUD + upload ảnh + toggle
- [x] Quản lý người dùng: xem + deactivate

### Phase 2 — Enhancement (Tuần 11–13)

- [ ] Dashboard: biểu đồ doanh thu (MPAndroidChart)
- [ ] Báo cáo nâng cao: chọn date range, top món bán chạy
- [ ] Quản lý banner / khuyến mãi
- [ ] Quản lý tài khoản Staff (tạo, gán nhà hàng)
- [ ] In-app notification list

### Phase 3 — Polish (Tuần 14–16)

- [ ] Filter đơn hàng nâng cao (theo ngày, nhà hàng)
- [ ] Export báo cáo CSV
- [ ] Dark mode
- [ ] Performance optimization

---

## Bảo Mật

- App Admin không public trên Play Store — phân phối nội bộ qua APK file
- Phân quyền được enforce tại **cả tầng app lẫn Supabase RLS** — không chỉ client-side
- `SUPABASE_ANON_KEY` và `SUPABASE_URL` lưu trong `local.properties`, không commit lên git
- Supabase JWT token lưu trong `EncryptedSharedPreferences`
- Staff chỉ được thấy dữ liệu nhà hàng mình qua RLS policy — không thể bypass bằng cách sửa app

---

*FoodDelivery Admin App · Java Android · Supabase · Version 1.0.0 · Nội bộ*
