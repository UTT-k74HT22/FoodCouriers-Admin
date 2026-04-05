    # Tổng quan - App Admin
## Hệ thống đặt đồ ăn

---

## 1. Thông tin dự án

| Item | Value |
|------|-------|
| Tên dự án | Food Ordering System |
| Tên app | FoodDelivery Admin |
| Nền tảng | Android Native |
| Ngôn ngữ | Java + XML |
| Backend | Supabase |
| Version | 1.0.0 |
| Người dùng mục tiêu | Admin và Staff quản lý nhà hàng |

---

## 2. User Roles

| Role | Description | Quyền hạn |
|------|-------------|------------|
| **Admin** | Quản trị viên hệ thống | Toàn quyền: quản lý tất cả nhà hàng, menu, đơn hàng, người dùng, báo cáo, cài đặt |
| **Staff** | Nhân viên nhà hàng | Giới hạn: chỉ quản lý nhà hàng được giao, xem đơn hàng của nhà hàng đó |

---

## 3. Module tổng quan

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ADMIN APP MODULES                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    1. XÁC THỰC (AUTHENTICATION)                     │  │
│   │   • Login, Session Management, Role Check, Logout                  │  │
│   │   • Bảo vệ ứng dụng, phân quyền Admin/Staff                         │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    2. DASHBOARD (BẢNG ĐIỀU KHIỂN)                    │  │
│   │   • Stats cards, New orders list, Top items, Realtime updates      │  │
│   │   • Tổng quan hoạt động kinh doanh                                   │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │               3. QUẢN LÝ ĐƠN HÀNG (ORDER MANAGEMENT)                  │  │
│   │   • Order list, Search, Detail, Update status, Cancel               │  │
│   │   • Theo dõi và xử lý đơn hàng từ khách                            │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │              4. QUẢN LÝ NHÀ HÀNG (RESTAURANT MANAGEMENT)            │  │
│   │   • CRUD nhà hàng, Upload ảnh, Toggle active/open                 │  │
│   │   • Quản lý thông tin các nhà hàng trong hệ thống                  │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │               5. QUẢN LÝ DANH MỤC (CATEGORY MANAGEMENT)              │  │
│   │   • CRUD danh mục món ăn (Món chính, Món phụ, Đồ uống...)          │  │
│   │   • Sắp xếp thứ tự hiển thị                                          │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │               6. QUẢN LÝ MÓN ĂN (MENU ITEM MANAGEMENT)                │  │
│   │   • CRUD món ăn theo nhà hàng và danh mục                           │  │
│   │   • Toggle available/featured, Upload ảnh                          │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │               7. QUẢN LÝ NGƯỜI DÙNG (USER MANAGEMENT)                 │  │
│   │   • Danh sách khách hàng, Tìm kiếm, Xem chi tiết                   │  │
│   │   • Deactivate/Activate tài khoản                                    │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    8. BÁO CÁO (REPORTS)                             │  │
│   │   • Thống kê doanh thu, đơn hàng, Top items, Top restaurants       │  │
│   │   • Biểu đồ doanh thu theo thời gian (Phase 2)                     │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │               9. THÔNG BÁO (NOTIFICATIONS)                           │  │
│   │   • Danh sách thông báo, Lọc theo loại, Đánh dấu đã đọc             │  │
│   │   • Badge count trên menu                                           │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Mô tả chi tiết từng Module

### Module 1: Xác thực (Authentication)
**Mục đích:** Xử lý đăng nhập và quản lý phiên làm việc cho Admin và Staff.

**Chức năng:**
- **Login:** Đăng nhập bằng email và mật khẩu
- **Session Management:** Lưu trữ JWT token an toàn trong EncryptedSharedPreferences, tự động làm mới khi hết hạn
- **Role Check:** Kiểm tra quyền (Admin hay Staff) để hiển thị menu phù hợp
  - Admin: Thấy tất cả menu
  - Staff: Chỉ thấy Dashboard, Orders, Menu (của nhà hàng được giao)
- **Logout:** Xóa token và đăng xuất

**Màn hình:**
- AdminLoginActivity

**Tác động:** Bảo vệ hệ thống, chỉ cho phép người được ủy quyền truy cập và phân chia quyền hạn rõ ràng.

---

### Module 2: Dashboard (Bảng điều khiển)
**Mục đích:** Hiển thị tổng quan các số liệu thống kê quan trọng và đơn hàng mới cần xử lý.

**Chức năng:**
- **Stats Cards:** Ba thẻ thông tin:
  - Tổng số đơn hàng hôm nay
  - Doanh thu hôm nay
  - Số đơn đang xử lý
- **New Orders List:** Danh sách đơn hàng mới có trạng thái "pending" cần xử lý ngay
- **Top Items:** Top 5 món ăn bán chạy nhất trong ngày
- **Realtime Updates:** Tự động cập nhật khi có đơn mới thông qua Supabase Realtime (WebSocket)

**Màn hình:**
- DashboardFragment

**Tác động:** Giúp Admin/Staff nhanh chóng nắm bắt tình hình kinh doanh và phát hiện đơn hàng mới kịp thời.

---

### Module 3: Quản lý đơn hàng (Order Management)
**Mục đích:** Theo dõi và xử lý toàn bộ đơn hàng từ khi khách đặt đến khi hoàn thành.

**Chức năng:**
- **Order List:** Danh sách đơn hàng với TabLayout lọc theo trạng thái:
  - Tất cả, Chờ xác nhận, Đã xác nhận, Đang nấu, Đang giao, Hoàn thành, Đã hủy
- **Search:** Tìm kiếm theo mã đơn, tên khách hàng hoặc số điện thoại
- **Order Detail:** Xem chi tiết đơn hàng (thông tin khách, món đã đặt, địa chỉ, tổng tiền, ghi chú)
- **Update Status:** Cập nhật trạng thái đơn với kiểm tra State Machine:
  - Chỉ cho phép chuyển đổi hợp lệ (pending→confirmed→preparing→delivering→delivered)
  - Có thể hủy từ trạng thái pending hoặc confirmed
- **Cancel Order:** Hủy đơn kèm lý do
- **Status Timeline:** Hiển thị lịch sử thay đổi trạng thái đơn hàng

**Màn hình:**
- OrderListFragment
- OrderDetailActivity

**Tác động:** Quản lý toàn bộ lifecycle của đơn hàng, đảm bảo trạng thái chính xác, ghi log thay đổi.

---

### Module 4: Quản lý nhà hàng (Restaurant Management)
**Mục đích:** CRUD (Create, Read, Update, Delete) thông tin các nhà hàng trong hệ thống.

**Chức năng:**
- **Restaurant List:** Danh sách tất cả nhà hàng với toggle bật/tắt trạng thái hoạt động
- **Add Restaurant:** Thêm nhà hàng mới với các thông tin:
  - Tên nhà hàng, địa chỉ, số điện thoại
  - Giờ mở cửa, giờ đóng cửa
  - Phí giao hàng, đơn tối thiểu
  - Hình ảnh nhà hàng (upload lên Supabase Storage)
- **Edit Restaurant:** Sửa tất cả thông tin trên
- **Toggle Active/Open:** Bật/tắt is_active và is_open (nhà hàng có đang mở cửa hay không)

**Màn hình:**
- RestaurantListFragment
- RestaurantFormActivity

**Lưu ý:** Staff chỉ thấy các nhà hàng được giao trong bảng restaurant_staff (RLS policy).

**Tác động:** Quản lý danh sách các nhà hàng, điều chỉnh trạng thái hoạt động.

---

### Module 5: Quản lý danh mục (Category Management)
**Mục đích:** Tổ chức và phân loại món ăn theo các danh mục (Món chính, Món phụ, Đồ uống, Tráng miệng...).

**Chức năng:**
- **Category List:** Danh sách danh mục sắp xếp theo sort_order
- **Add Category:** Thêm danh mục mới (tên, hình ảnh, thứ tự)
- **Edit Category:** Sửa tên, hình ảnh, thứ tự hiển thị
- **Delete Category:** Xóa danh mục (chỉ khi chưa có món nào thuộc danh mục này)

**Màn hình:**
- CategoryListFragment
- CategoryFormActivity

**Tác động:** Tổ chức thực đơn giúp khách hàng dễ tìm kiếm món ăn.

---

### Module 6: Quản lý món ăn (Menu Item Management)
**Mục đích:** Quản lý các món ăn trong thực đơn của từng nhà hàng.

**Chức năng:**
- **Menu List:** Danh sách món ăn theo nhà hàng với bộ lọc theo danh mục
- **Add Menu Item:** Thêm món mới (tên, mô tả, giá, hình ảnh, danh mục)
- **Edit Menu Item:** Sửa thông tin món ăn
- **Toggle Available:** Bật/tắt trạng thái "còn hàng" / "hết hàng"
- **Toggle Featured:** Đánh dấu món nổi bật (hiển thị ở đầu danh sách)
- **Upload Image:** Tải hình ảnh món ăn lên Supabase Storage

**Màn hình:**
- MenuListFragment
- MenuItemFormActivity

**Lưu ý:** Staff chỉ thấy menu của nhà hàng được giao.

**Tác động:** Quản lý thực đơn các nhà hàng, cập nhật trạng thái món ăn theo thời gian thực.

---

### Module 7: Quản lý người dùng (User Management)
**Mục đích:** Quản lý tài khoản khách hàng trong hệ thống.

**Chức năng:**
- **User List:** Danh sách tất cả khách hàng (role = 'customer')
- **Search:** Tìm kiếm theo tên, email hoặc số điện thoại
- **User Detail:** Xem thông tin chi tiết và lịch sử đơn hàng của khách
- **Deactivate/Activate:** Vô hiệu hóa hoặc kích hoạt tài khoản khách hàng

**Màn hình:**
- UserListActivity
- UserDetailActivity

**Tác động:** Quản lý tài khoản khách hàng, xử lý các vấn đề liên quan đến tài khoản.

---

### Module 8: Báo cáo (Reports)
**Mục đích:** Thống kê và báo cáo doanh thu, đơn hàng theo thời gian.

**Chức năng:**
- **Date Range Picker:** Chọn khoảng thời gian báo cáo:
  - Hôm nay, Hôm qua, 7 ngày qua, 30 ngày qua, Tùy chọn
- **Revenue Stats:** 
  - Tổng doanh thu
  - Tổng số đơn hàng
  - Trung bình giá trị/đơn
- **Order Stats:**
  - Số đơn hoàn thành
  - Số đơn hủy
  - Tỉ lệ hủy (%)
- **Top Items:** Top 5 món bán chạy nhất (theo số lượng và doanh thu)
- **Top Restaurants:** Top 5 nhà hàng có doanh thu cao nhất
- **Chart:** Biểu đồ đường doanh thu theo ngày (Phase 2 - MPAndroidChart)

**Màn hình:**
- ReportFragment

**Tác động:** Cung cấp số liệu để phân tích hiệu suất kinh doanh và đưa ra quyết định.

---

### Module 9: Thông báo (Notifications)
**Mục đích:** Quản lý các thông báo trong app dành cho Admin/Staff.

**Chức năng:**
- **Notification List:** Danh sách tất cả thông báo (thông báo đơn hàng mới, cập nhật trạng thái...)
- **Filter:** Lọc theo loại (Order, System)
- **Mark as Read:** Đánh dấu đã đọc từng thông báo hoặc tất cả
- **Badge Count:** Hiển thị số thông báo chưa đọc trên NavigationDrawer

**Màn hình:**
- NotificationFragment

**Tác động:** Thông báo kịp thời về các sự kiện quan trọng (đơn hàng mới, thay đổi trạng thái...).

---

## 5. Luồng dữ liệu chính

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│    Admin     │      │   Supabase   │      │    Client    │
│     App      │ ───▶ │   Backend    │ ◀──▶ │     App      │
└──────────────┘      └──────────────┘      └──────────────┘
      │                     │                      │
      │   Nhận đơn mới     │    Đặt hàng         │
      │ ◀──────────────────│────────────────────▶│
      │                     │                      │
      │   Cập nhật trạng  │    Cập nhật trạng    │
      │       thái         │        thái          │
      │ ──────────────────▶│◀────────────────────│
      │                     │                      │
      │   Quản lý nhà hàng │    Xem nhà hàng     │
      │ ──────────────────▶│◀────────────────────│
      │                     │                      │
      │   Báo cáo doanh thu │    Thanh toán COD   │
      │ ◀──────────────────│────────────────────▶│
```

---

## 6. Tổng kết

| Module | Màn hình chính | Chức năng quan trọng |
|--------|---------------|----------------------|
| Xác thực | AdminLoginActivity | Bảo vệ app, phân quyền |
| Dashboard | DashboardFragment | Tổng quan số liệu |
| Quản lý đơn hàng | OrderListFragment, OrderDetailActivity | Xử lý đơn hàng |
| Quản lý nhà hàng | RestaurantListFragment, RestaurantFormActivity | CRUD nhà hàng |
| Quản lý danh mục | CategoryListFragment, CategoryFormActivity | Tổ chức món ăn |
| Quản lý món ăn | MenuListFragment, MenuItemFormActivity | Quản lý thực đơn |
| Quản lý người dùng | UserListActivity, UserDetailActivity | Quản lý khách hàng |
| Báo cáo | ReportFragment | Thống kê kinh doanh |
| Thông báo | NotificationFragment | Nhận thông báo |

---

*Document Version: 1.0*
*Created: 2026-04-04*
*Author: BA Team*