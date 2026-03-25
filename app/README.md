# 🛠️ FoodDelivery — App Quản Trị (Admin)

> Ứng dụng quản trị hệ thống giao đồ ăn dành cho nhân viên vận hành.  
> Nền tảng: **Java Android · XML Layout · Firebase**

---

## 📋 Mục Lục

- [Tổng quan](#-tổng-quan)
- [Vai trò & Phân quyền](#-vai-trò--phân-quyền)
- [Tính năng](#-tính-năng)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Cấu trúc project](#-cấu-trúc-project)
- [Hướng dẫn cài đặt](#-hướng-dẫn-cài-đặt)
- [Kiến trúc ứng dụng](#-kiến-trúc-ứng-dụng)
- [Màn hình chính](#-màn-hình-chính)
- [Luồng xử lý đơn hàng](#-luồng-xử-lý-đơn-hàng)
- [Thuật toán tìm shipper](#-thuật-toán-tìm-shipper)

---

## 📱 Tổng Quan

**FoodDelivery Admin** là ứng dụng Android nội bộ cho phép nhân viên vận hành:
- Theo dõi và xử lý đơn hàng theo thời gian thực
- Quản lý toàn bộ nhà hàng, thực đơn, shipper
- Xem báo cáo doanh thu và xuất file
- Gửi thông báo đến khách hàng

> ⚠️ **Lưu ý bảo mật:** App này chỉ dành cho nhân viên nội bộ.  
> Không public lên CH Play. Phân phối qua Firebase App Distribution hoặc file APK nội bộ.

| Thông tin | Chi tiết |
|-----------|----------|
| Min SDK | Android 7.0 (API 24) |
| Target SDK | Android 14 (API 34) |
| Ngôn ngữ | Java |
| Layout | XML |
| Architecture | MVVM + LiveData |

---

## 👥 Vai Trò & Phân Quyền

| Vai trò | Quyền hạn |
|---------|----------|
| **Super Admin** | Toàn quyền: xem báo cáo, quản lý tất cả, tạo tài khoản admin |
| **Manager** | Quản lý đơn hàng, nhà hàng, shipper, xem báo cáo |
| **Operator** | Chỉ xử lý đơn hàng (xác nhận, assign shipper, hủy) |

Phân quyền được kiểm soát qua `role` field trong Firestore collection `admins`.

---

## ✨ Tính Năng

### 📊 Dashboard
- KPI tổng quan: đơn hôm nay, doanh thu, shipper online, nhà hàng đang mở
- So sánh ngày hôm nay vs hôm qua (% tăng/giảm)
- LineChart: doanh thu 7 ngày gần nhất
- PieChart: phân bố trạng thái đơn hàng
- BarChart: top 5 nhà hàng doanh thu cao nhất
- Badge realtime đơn hàng chờ xử lý trên menu

### 📦 Quản Lý Đơn Hàng
- Danh sách đơn hàng realtime chia theo tab trạng thái
- Tìm kiếm theo mã đơn, tên khách
- Lọc theo nhà hàng / shipper / ngày
- Xác nhận, hủy đơn bằng swipe gesture
- Assign shipper thủ công hoặc tự động
- Xem chi tiết đơn: thông tin khách, món đặt, địa chỉ, thanh toán

### 🏪 Quản Lý Nhà Hàng
- Danh sách nhà hàng với toggle bật/tắt realtime
- Thêm / sửa / xóa nhà hàng
- Upload logo & banner (Firebase Storage)
- Chọn vị trí trên Google Maps
- Cài đặt giờ mở cửa từng ngày trong tuần
- Xem thống kê theo từng nhà hàng

### 🍽️ Quản Lý Thực Đơn
- Menu builder: tổ chức theo danh mục
- Thêm / sửa / xóa món ăn
- Cấu hình nhóm tùy chọn (size, topping...)
- Kéo thả để sắp xếp thứ tự món
- Toggle ẩn/hiện từng món không cần xóa

### 🏍️ Quản Lý Shipper
- Bản đồ realtime vị trí tất cả shipper đang online
- Danh sách shipper với trạng thái: online / offline / đang giao
- Xem chi tiết: lịch sử giao hàng, rating, thu nhập
- Block / unblock tài khoản shipper
- Thống kê hiệu suất từng shipper

### 🎫 Quản Lý Khuyến Mãi
- Tạo / sửa / xóa mã giảm giá
- Loại giảm: % hoặc số tiền cố định
- Giới hạn: số lần dùng, đơn tối thiểu, giảm tối đa
- Áp dụng cho tất cả hoặc chọn nhà hàng cụ thể
- Đặt thời gian hiệu lực

### 📈 Báo Cáo Doanh Thu
- Chọn khoảng thời gian tùy ý (DateRangePicker)
- Tổng doanh thu, số đơn, trung bình/đơn, tỉ lệ hủy
- Biểu đồ chi tiết theo ngày / tuần / tháng
- Phân tích theo phương thức thanh toán
- Top món ăn bán chạy
- Xuất báo cáo PDF hoặc CSV

### 🔔 Gửi Thông Báo
- Gửi đến tất cả người dùng hoặc nhóm cụ thể
- Đính kèm ảnh, deeplink đến màn hình trong app
- Đặt lịch gửi
- Xem lịch sử thông báo đã gửi

---

## 🛠️ Công Nghệ Sử Dụng

### Firebase
| Dịch vụ | Mục đích |
|---------|----------|
| Firebase Authentication | Đăng nhập tài khoản admin |
| Cloud Firestore | Database chính |
| Firebase Storage | Upload ảnh nhà hàng, món ăn |
| Firebase Cloud Messaging | Gửi push notification đến user |

### Third-party Libraries
| Thư viện | Version | Mục đích |
|----------|---------|----------|
| MPAndroidChart | 3.1.0 | Vẽ biểu đồ Line / Bar / Pie |
| Google Maps SDK | 18.2.0 | Bản đồ quản lý shipper |
| Glide | 4.16.0 | Tải và cache hình ảnh |
| Material Design 3 | 1.10.0 | UI Components |
| iTextPDF | 7.x | Xuất file báo cáo PDF |

---

## 📁 Cấu Trúc Project

```
app/
├── src/main/
│   ├── java/com/fooddelivery/admin/
│   │   │
│   │   ├── data/                              # Tầng dữ liệu
│   │   │   ├── model/                         # Java Model classes
│   │   │   │   ├── AdminUser.java             # Tài khoản admin + role
│   │   │   │   ├── Order.java                 # Đơn hàng (dùng chung với client)
│   │   │   │   ├── Restaurant.java
│   │   │   │   ├── MenuItem.java
│   │   │   │   ├── Shipper.java
│   │   │   │   ├── Promotion.java
│   │   │   │   └── RevenueReport.java         # Model tổng hợp báo cáo
│   │   │   │
│   │   │   ├── repository/                    # Logic truy vấn Firebase
│   │   │   │   ├── AuthRepository.java        # Đăng nhập admin
│   │   │   │   ├── OrderRepository.java       # Quản lý đơn hàng
│   │   │   │   ├── RestaurantRepository.java
│   │   │   │   ├── MenuRepository.java
│   │   │   │   ├── ShipperRepository.java
│   │   │   │   ├── PromotionRepository.java
│   │   │   │   └── ReportRepository.java      # Tổng hợp dữ liệu báo cáo
│   │   │   │
│   │   │   └── remote/
│   │   │       ├── FirestoreHelper.java
│   │   │       ├── StorageHelper.java
│   │   │       └── FcmApiHelper.java          # Gọi FCM HTTP API gửi notification
│   │   │
│   │   ├── ui/                                # Tầng giao diện
│   │   │   ├── auth/
│   │   │   │   └── AdminLoginActivity.java    # Đăng nhập admin (email/pass)
│   │   │   │
│   │   │   ├── main/
│   │   │   │   └── AdminMainActivity.java     # NavigationDrawer container
│   │   │   │
│   │   │   ├── dashboard/                     # Trang tổng quan
│   │   │   │   ├── DashboardFragment.java     # KPI cards + Charts
│   │   │   │   └── adapter/
│   │   │   │       └── TopRestaurantAdapter.java
│   │   │   │
│   │   │   ├── order/                         # Quản lý đơn hàng
│   │   │   │   ├── OrderListFragment.java     # Danh sách realtime theo tab
│   │   │   │   ├── OrderDetailActivity.java   # Chi tiết + thay đổi trạng thái
│   │   │   │   ├── AssignShipperDialog.java   # Chọn shipper thủ công
│   │   │   │   └── adapter/
│   │   │   │       └── OrderAdapter.java
│   │   │   │
│   │   │   ├── restaurant/                    # Quản lý nhà hàng
│   │   │   │   ├── RestaurantListFragment.java
│   │   │   │   ├── AddEditRestaurantActivity.java
│   │   │   │   ├── RestaurantDetailActivity.java  # Thống kê theo nhà hàng
│   │   │   │   └── adapter/
│   │   │   │       └── RestaurantAdapter.java
│   │   │   │
│   │   │   ├── menu/                          # Quản lý thực đơn
│   │   │   │   ├── MenuManagerFragment.java   # Danh sách món theo category
│   │   │   │   ├── AddEditMenuItemActivity.java
│   │   │   │   ├── OptionGroupBuilderView.java # Custom view tạo nhóm tùy chọn
│   │   │   │   └── adapter/
│   │   │   │       └── MenuItemAdapter.java   # Hỗ trợ drag & drop
│   │   │   │
│   │   │   ├── shipper/                       # Quản lý shipper
│   │   │   │   ├── ShipperListFragment.java   # Danh sách + bản đồ realtime
│   │   │   │   ├── ShipperDetailActivity.java # Thống kê, lịch sử, thu nhập
│   │   │   │   ├── AddEditShipperActivity.java
│   │   │   │   └── adapter/
│   │   │   │       └── ShipperAdapter.java
│   │   │   │
│   │   │   ├── promotion/                     # Quản lý khuyến mãi
│   │   │   │   ├── PromotionListFragment.java
│   │   │   │   ├── AddEditPromotionActivity.java
│   │   │   │   └── adapter/
│   │   │   │       └── PromotionAdapter.java
│   │   │   │
│   │   │   ├── report/                        # Báo cáo doanh thu
│   │   │   │   ├── RevenueReportFragment.java
│   │   │   │   ├── ChartHelper.java           # Khởi tạo MPAndroidChart
│   │   │   │   └── PdfExporter.java           # Xuất báo cáo PDF (iText)
│   │   │   │
│   │   │   ├── notification/                  # Gửi thông báo
│   │   │   │   └── SendNotificationFragment.java
│   │   │   │
│   │   │   └── common/
│   │   │       ├── BaseActivity.java
│   │   │       ├── BaseFragment.java
│   │   │       ├── LoadingDialog.java
│   │   │       └── ConfirmDialog.java         # Dialog xác nhận thao tác quan trọng
│   │   │
│   │   ├── viewmodel/
│   │   │   ├── DashboardViewModel.java
│   │   │   ├── OrderViewModel.java
│   │   │   ├── RestaurantViewModel.java
│   │   │   ├── MenuViewModel.java
│   │   │   ├── ShipperViewModel.java
│   │   │   └── ReportViewModel.java
│   │   │
│   │   └── utils/
│   │       ├── Constants.java
│   │       ├── SharedPrefManager.java
│   │       ├── CurrencyUtils.java
│   │       ├── DateTimeUtils.java
│   │       ├── ImageUtils.java
│   │       ├── DistanceUtils.java             # Tính khoảng cách + tìm shipper gần nhất
│   │       └── PermissionUtils.java
│   │
│   └── res/
│       ├── layout/
│       │   ├── activity_*.xml
│       │   ├── fragment_*.xml
│       │   └── item_*.xml
│       ├── drawable/
│       ├── anim/
│       ├── menu/                              # Navigation Drawer menu XML
│       └── values/
│           ├── colors.xml                     # Navy #1A237E + Orange #FF6B35
│           ├── strings.xml
│           └── styles.xml
│
├── google-services.json                       # ⚠️ Không commit lên git
└── build.gradle
```

---

## 🚀 Hướng Dẫn Cài Đặt

### Yêu cầu
- Android Studio **Hedgehog** (2023.1.1) trở lên
- JDK 17
- Gradle 8.x
- Thiết bị / emulator Android API 24+
- Tài khoản admin đã được tạo trong Firebase

### Các bước

```bash
# 1. Clone repository
git clone https://github.com/your-org/food-delivery-admin.git
cd food-delivery-admin

# 2. Mở bằng Android Studio
# File → Open → chọn thư mục vừa clone

# 3. Thêm file cấu hình Firebase
# Tải google-services.json từ Firebase Console
# Đặt vào thư mục: app/google-services.json

# 4. Thêm API keys vào local.properties
echo "MAPS_API_KEY=your_google_maps_key" >> local.properties
echo "FCM_SERVER_KEY=your_fcm_server_key" >> local.properties

# 5. Sync Gradle và chạy
# Nhấn "Sync Now" → Run (Shift + F10)
```

### Tạo tài khoản Admin đầu tiên

Chạy script sau trên Firebase Console (Firestore):
```javascript
// Tạo document trong collection "admins"
{
  uid: "firebase_auth_uid",
  email: "admin@fooddelivery.vn",
  fullName: "Super Admin",
  role: "super_admin",   // super_admin | manager | operator
  isActive: true,
  createdAt: Timestamp.now()
}
```

---

## 🏗️ Kiến Trúc Ứng Dụng

```
┌──────────────────────────────────────────────┐
│                  UI Layer                     │
│   Fragment / Activity / Adapter               │
│   NavigationDrawer điều hướng các module      │
└─────────────────┬────────────────────────────┘
                  │ observe / call
┌─────────────────▼────────────────────────────┐
│              ViewModel Layer                  │
│   Xử lý business logic, giữ trạng thái UI    │
│   DashboardVM / OrderVM / ReportVM...         │
└─────────────────┬────────────────────────────┘
                  │ call
┌─────────────────▼────────────────────────────┐
│            Repository Layer                   │
│   Tổng hợp & transform dữ liệu từ Firebase   │
│   OrderRepo / ShipperRepo / ReportRepo...     │
└─────────────────┬────────────────────────────┘
                  │ query / listen
┌─────────────────▼────────────────────────────┐
│           Firebase Services                   │
│   Firestore  │  Storage  │  FCM HTTP API      │
│   Auth       │  Maps SDK │  (gửi notification)│
└──────────────────────────────────────────────┘
```

**Điểm đặc biệt của Admin App:**
- Dùng **NavigationDrawer** thay vì BottomNavigation (phù hợp UX quản trị)
- Firestore **realtime listeners** trên màn hình đơn hàng (tự cập nhật khi có đơn mới)
- Dùng **Firestore batch writes** khi cập nhật nhiều document cùng lúc (vd: assign shipper)

---

## 📲 Màn Hình Chính

| Màn hình | File | Mô tả |
|----------|------|-------|
| Đăng nhập | `AdminLoginActivity` | Email + password, kiểm tra role |
| Dashboard | `DashboardFragment` | KPI + 3 biểu đồ MPAndroidChart |
| Danh sách đơn | `OrderListFragment` | TabLayout 5 trạng thái, realtime |
| Chi tiết đơn | `OrderDetailActivity` | Xử lý, assign shipper |
| Nhà hàng | `RestaurantListFragment` | Toggle bật/tắt, CRUD |
| Thêm nhà hàng | `AddEditRestaurantActivity` | Form + Maps + giờ mở cửa |
| Thực đơn | `MenuManagerFragment` | Menu theo danh mục, drag & drop |
| Thêm món | `AddEditMenuItemActivity` | Form + option groups builder |
| Shipper | `ShipperListFragment` | Danh sách + Maps realtime |
| Chi tiết shipper | `ShipperDetailActivity` | Lịch sử, thống kê, thu nhập |
| Khuyến mãi | `PromotionListFragment` | CRUD voucher |
| Báo cáo | `RevenueReportFragment` | Chart + xuất PDF/CSV |
| Gửi thông báo | `SendNotificationFragment` | FCM push notification |

---

## 🔄 Luồng Xử Lý Đơn Hàng

```
Khách đặt đơn (App Client)
          ↓
  Firestore: orders/{id}
  status = "PENDING"
          ↓
  [Admin nhận realtime listener]
  Badge tăng trên menu
          ↓
  Admin xem và XÁC NHẬN đơn
  status = "CONFIRMED"
  → FCM notification → khách
          ↓
  Hệ thống tự động tìm shipper
  [DistanceUtils.findNearestShipper()]
          ↓
  Assign shipper (Firestore batch write):
  - orders/{id}.shipperId = shipperId
  - shippers/{shipperId}.isAvailable = false
  - orders/{id}.status = "PREPARING"
  → FCM notification → shipper
          ↓
  Shipper nhận hàng
  status = "PICKED_UP"
  → FCM notification → khách
          ↓
  Shipper đang giao
  status = "DELIVERING"
  [Khách theo dõi realtime trên Maps]
          ↓
  Khách xác nhận nhận hàng
  status = "DELIVERED"
  - shippers/{id}.isAvailable = true
  - shippers/{id}.totalDeliveries++
  → Mở màn hình đánh giá (App Client)
```

---

## 🧮 Thuật Toán Tìm Shipper

Khi admin xác nhận đơn, hệ thống tự động tìm shipper phù hợp nhất.

**File:** `utils/DistanceUtils.java`

```java
// Bước 1: Lấy tất cả shipper đang online và available
List<Shipper> available = shippers.stream()
    .filter(s -> s.isOnline() && s.isAvailable())
    .collect(Collectors.toList());

// Bước 2: Tính khoảng cách từ từng shipper đến nhà hàng
// Công thức Haversine: tính khoảng cách đường chim bay giữa 2 tọa độ

// Bước 3: Chọn shipper gần nhất
Shipper nearest = Collections.min(available,
    Comparator.comparingDouble(s ->
        calculateDistance(s.getLat(), s.getLng(),
                         restaurant.getLat(), restaurant.getLng())));

// Bước 4: Ước tính thời gian giao
// = khoảng cách (km) / 25 km/h * 60 phút + 10 phút chuẩn bị
int estimatedMinutes = (int)(distanceKm / 25.0 * 60) + 10;
```

---

## 📊 Biểu Đồ (MPAndroidChart)

Cấu hình trong `ChartHelper.java`:

| Biểu đồ | Dữ liệu | Màu |
|---------|---------|-----|
| LineChart | Doanh thu 7 ngày | `#FF6B35` |
| PieChart | Tỉ lệ trạng thái đơn | Theo từng status |
| BarChart | Top 5 nhà hàng | `#1A237E` |
| BarChart (báo cáo) | Doanh thu theo ngày | `#FF6B35` |

---

## 🔑 Biến Môi Trường

Tạo / cập nhật file `local.properties`:

```properties
# Google Maps
MAPS_API_KEY=AIzaSy...

# FCM Server Key (gửi notification từ admin)
FCM_SERVER_KEY=AAAAa...

# (Optional) Nếu dùng Firebase Cloud Functions
CLOUD_FUNCTION_BASE_URL=https://us-central1-your-project.cloudfunctions.net
```

---

## 🎨 Design System

| Token | Giá trị | Dùng cho |
|-------|---------|----------|
| `colorPrimary` | `#1A237E` | Header, navigation, nút chính |
| `colorAccent` | `#FF6B35` | Highlight, badge đơn mới, biểu đồ |
| `colorBackground` | `#F0F2F5` | Nền màn hình |
| `colorSurface` | `#FFFFFF` | Card, dialog |
| `colorSuccess` | `#4CAF50` | Đơn hoàn thành, shipper online |
| `colorWarning` | `#FFC107` | Đơn đang chờ xử lý |
| `colorError` | `#F44336` | Đơn hủy, shipper offline |
| `colorInfo` | `#2196F3` | Đơn đang giao |

---

## 🔐 Bảo Mật

- App Admin **không public** trên Play Store — phân phối nội bộ qua Firebase App Distribution
- Mọi request đến Firestore đều được kiểm tra qua **Firestore Security Rules**
- Tài khoản admin được lưu riêng trong collection `admins`, tách biệt với `users`
- Token FCM Server Key **không được** để trong source code → dùng `local.properties`
- Thực hiện thao tác nhạy cảm (xóa, batch update) qua **Firebase Cloud Functions** để tránh expose logic client-side

---

## 📝 Quy Tắc Code

- Comment tiếng Việt cho logic nghiệp vụ phức tạp
- Mọi thao tác thay đổi dữ liệu cần hiển thị **ConfirmDialog** trước khi thực hiện
- Dùng **Firestore batch writes** hoặc **transaction** khi cập nhật nhiều document liên quan
- Log đầy đủ các thao tác admin để audit: `AdminLogger.log(action, adminId, targetId)`
- Xử lý lỗi mạng và hiển thị thông báo rõ ràng bằng Snackbar

---

*FoodDelivery Admin App · Java Android · Phiên bản 1.0.0 · Nội bộ*