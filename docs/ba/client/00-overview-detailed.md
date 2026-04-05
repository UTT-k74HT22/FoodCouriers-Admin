    # Tổng quan - App Client
## Hệ thống đặt đồ ăn

---

## 1. Thông tin dự án

| Item | Value |
|------|-------|
| Tên dự án | Food Ordering System |
| Tên app | FoodDelivery Client |
| Nền tảng | Android Native |
| Ngôn ngữ | Java + XML |
| Backend | Supabase |
| Version | 1.0.0 |
| Người dùng mục tiêu | Khách hàng muốn đặt đồ ăn online |

---

## 2. User Role

| Role | Description |
|------|-------------|
| **Customer** | Người dùng cuối, đặt đồ ăn, xem lịch sử đơn hàng và quản lý thông tin cá nhân |

---

## 3. Module tổng quan

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CLIENT APP MODULES                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    1. XÁC THỰC (AUTHENTICATION)                     │  │
│   │   • Login, Register, Forgot Password, Session Management           │  │
│   │   • Bảo vệ ứng dụng, cho phép truy cập có xác thực                 │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    2. TRANG CHỦ (HOME SCREEN)                       │  │
│   │   • Banner carousel, Categories, Featured & Near restaurants        │  │
│   │   • Điểm landing chính của ứng dụng                               │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                 3. CHI TIẾT NHÀ HÀNG (RESTAURANT DETAIL)           │  │
│   │   • Xem thông tin nhà hàng, menu theo danh mục                     │  │
│   │   • Thêm món vào giỏ hàng                                          │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │              4. GIỎ HÀNG & THANH TOÁN (CART & CHECKOUT)            │  │
│   │   • Quản lý giỏ hàng cục bộ (Room DB), Thanh toán COD              │  │
│   │   • Hoàn tất quá trình đặt hàng                                     │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │               5. LỊCH SỬ ĐƠN HÀNG (ORDER HISTORY)                   │  │
│   │   • Theo dõi trạng thái đơn, Hủy đơn, Đánh giá                      │  │
│   │   • Cập nhật realtime khi có thay đổi                             │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                 6. HỒ SƠ CÁ NHÂN (USER PROFILE)                    │  │
│   │   • Quản lý thông tin cá nhân, địa chỉ giao hàng                   │  │
│   │   • Quản lý tài khoản người dùng                                   │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Mô tả chi tiết từng Module

### Module 1: Xác thực (Authentication)
**Mục đích:** Xử lý đăng nhập, đăng ký và quản lý phiên làm việc cho khách hàng.

**Chức năng:**
- **Login:** Cho phép khách hàng đăng nhập bằng email và mật khẩu
- **Register:** Tạo tài khoản mới với thông tin email, số điện thoại, mật khẩu
- **Forgot Password:** Gửi email để đặt lại mật khẩu khi quên
- **Session Management:** Lưu trữ JWT token một cách an toàn trong EncryptedSharedPreferences, tự động làm mới khi hết hạn

**Màn hình:**
- LoginActivity
- RegisterActivity
- ResetPasswordActivity

**Tác động:** Bảo vệ ứng dụng, chỉ cho phép người dùng đã đăng ký truy cập.

---

### Module 2: Trang chủ (Home Screen)
**Mục đích:** Hiển thị nội dung chính của ứng dụng khi khách hàng mở app.

**Chức năng:**
- **Banner Carousel:** Hiển thị các banner khuyến mãi tự động chạy (ViewPager2)
- **Categories:** Danh sách danh mục món ăn cuộn ngang (Món chính, Món phụ, Đồ uống...)
- **Featured Restaurants:** Nhà hàng nổi bật được đánh dấu (is_featured=true)
- **Near Restaurants:** Nhà hàng gần vị trí hiện tại của khách hàng
- **Search Bar:** Chuyển đến màn hình tìm kiếm chi tiết
- **Pull to Refresh:** Cập nhật dữ liệu mới nhất

**Màn hình:**
- HomeFragment (trong MainActivity với BottomNavigation)

**Tác động:** Là điểm landing page giới thiệu các nhà hàng, danh mục và khuyến mãi.

---

### Module 3: Chi tiết nhà hàng (Restaurant Detail)
**Mục đích:** Xem thông tin chi tiết của một nhà hàng cụ thể và menu của nhà hàng đó.

**Chức năng:**
- **Restaurant Info:** Tên nhà hàng, hình ảnh, địa chỉ, đánh giá (rating), giờ mở cửa, phí giao hàng
- **Menu by Category:** Danh sách món ăn được tổ chức theo danh mục (sử dụng TabLayout)
- **Add to Cart:** Thêm món vào giỏ hàng với số lượng tùy chọn
- **Item Note:** Cho phép khách hàng thêm ghi chú cho từng món (vd: "không hành")
- **Search in Restaurant:** Tìm kiếm món ăn trong danh sách của nhà hàng
- **Featured Badge:** Hiển thị các món nổi bật

**Màn hình:**
- RestaurantDetailActivity

**Tác động:** Giúp khách hàng xem chi tiết thực đơn và dễ dàng thêm món vào giỏ.

---

### Module 4: Giỏ hàng & Thanh toán (Cart & Checkout)
**Mục đích:** Quản lý giỏ hàng và hoàn tất quá trình đặt hàng với phương thức COD.

**Chức năng:**
- **View Cart:** Hiển thị danh sách các món đã chọn với số lượng và giá
- **Update Quantity:** Tăng hoặc giảm số lượng của từng món
- **Remove Item:** Xóa món khỏi giỏ hàng
- **Apply Promo:** Nhập mã khuyến mãi để giảm giá (nếu hợp lệ)
- **Select Address:** Chọn địa chỉ giao hàng từ danh sách hoặc thêm địa chỉ mới
- **Order Note:** Ghi chú cho đơn hàng (vd: "gọi trước khi giao")
- **Place Order:** Xác nhận đặt hàng với phương thức thanh toán COD (Tiền mặt khi nhận hàng)
- **Order Success:** Màn hình thông báo đặt hàng thành công với mã đơn hàng

**Lưu trữ cục bộ:** Sử dụng Room Database để lưu giỏ hàng (giúp xem được giỏ hàng khi offline)

**Màn hình:**
- CartFragment
- CheckoutActivity
- OrderSuccessActivity

**Tác động:** Hoàn tất quy trình đặt hàng, lưu trữ tạm thời giỏ hàng trên thiết bị.

---

### Module 5: Lịch sử đơn hàng (Order History)
**Mục đích:** Theo dõi và quản lý các đơn hàng đã đặt.

**Chức năng:**
- **Order List:** Danh sách đơn hàng với các tab lọc:
  - Tất cả: Tất cả đơn hàng
  - Đang xử lý: Đơn có trạng thái pending, confirmed, preparing, delivering
  - Hoàn thành: Đơn có trạng thái delivered
  - Đã hủy: Đơn có trạng thái cancelled
- **Order Detail:** Xem chi tiết đơn hàng (món đã đặt, tổng tiền, địa chỉ giao hàng)
- **Status Timeline:** Hiển thị timeline trực quan các trạng thái đơn hàng (Chờ xác nhận → Đã xác nhận → Đang nấu → Đang giao → Hoàn thành)
- **Cancel Order:** Hủy đơn (chỉ khi trạng thái là pending hoặc confirmed)
- **Reorder:** Thêm lại tất cả món từ đơn cũ vào giỏ hàng
- **Submit Review:** Đánh giá nhà hàng sau khi nhận hàng (rating 1-5 sao + bình luận)
- **Realtime Updates:** Tự động cập nhật trạng thái đơn khi Admin thay đổi

**Màn hình:**
- OrderHistoryFragment
- OrderDetailActivity

**Tác động:** Giúp khách hàng theo dõi tình trạng đơn hàng và đánh giá sau khi nhận hàng.

---

### Module 6: Hồ sơ cá nhân (User Profile)
**Mục đích:** Quản lý thông tin cá nhân và địa chỉ giao hàng của khách hàng.

**Chức năng:**
- **View Profile:** Xem thông tin tài khoản (tên, email, số điện thoại, avatar)
- **Edit Profile:** Cập nhật tên và số điện thoại
- **Address List:** Danh sách tất cả địa chỉ đã lưu
- **Add Address:** Thêm địa chỉ mới (nhà, công ty, địa chỉ khác)
- **Edit Address:** Sửa thông tin địa chỉ
- **Delete Address:** Xóa địa chỉ
- **Set Default:** Đặt một địa chỉ làm mặc định (sẽ được chọn sẵn khi thanh toán)
- **Logout:** Đăng xuất khỏi ứng dụng

**Màn hình:**
- ProfileFragment
- EditProfileActivity
- AddressListActivity
- AddressFormActivity

**Tác động:** Giúp khách hàng quản lý thông tin cá nhân và địa chỉ tiện lợi cho các lần đặt hàng sau.

---

## 5. Luồng dữ liệu chính

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│   Client     │      │   Supabase   │      │    Admin     │
│    App       │ ───▶ │   Backend    │ ◀──▶ │    App       │
└──────────────┘      └──────────────┘      └──────────────┘
      │                     │                      │
      │     Đặt hàng       │    Nhận đơn         │
      │ ──────────────────▶│─────────────────────▶│
      │                     │                      │
      │    Cập nhật trạng  │    Cập nhật trạng   │
      │       thái         │        thái          │
      │ ◀──────────────────│◀────────────────────│
      │                     │                      │
      │   Thanh toán COD   │    Xác nhận đơn      │
      │ ──────────────────▶│─────────────────────▶│
```

---

## 6. Tổng kết

| Module | Màn hình chính | Chức năng quan trọng |
|--------|---------------|----------------------|
| Xác thực | Login, Register | Bảo vệ app |
| Trang chủ | HomeFragment | Hiển thị nhà hàng, danh mục |
| Chi tiết nhà hàng | RestaurantDetailActivity | Xem menu, thêm vào giỏ |
| Giỏ hàng & Thanh toán | CartFragment, Checkout | Hoàn tất đặt hàng |
| Lịch sử đơn hàng | OrderHistoryFragment | Theo dõi đơn |
| Hồ sơ cá nhân | ProfileFragment | Quản lý tài khoản, địa chỉ |

---

*Document Version: 1.0*
*Created: 2026-04-04*
*Author: BA Team*