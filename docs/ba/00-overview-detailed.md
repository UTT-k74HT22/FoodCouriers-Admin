# Tổng quan hệ thống - Food Ordering System
## Mô tả chi tiết các Module

---

## PHẦN 1: ADMIN APP (App Quản trị)

### Module 1: Xác thực (Authentication)
**Mục đích:** Xử lý đăng nhập, đăng xuất và quản lý phiên làm việc cho Admin và Staff.

**Chức năng chi tiết:**
- **Login:** Cho phép Admin/Staff đăng nhập bằng email và mật khẩu
- **Session Management:** Lưu trữ JWT token một cách an toàn, tự động làm mới token khi hết hạn
- **Role Check:** Kiểm tra quyền của người dùng (Admin hay Staff) để hiển thị menu phù hợp
- **Logout:** Xóa token và đăng xuất khỏi hệ thống

**Đối tượng sử dụng:** Quản trị viên (Admin) và nhân viên (Staff) của nhà hàng

**Tác động nghiệp vụ:**
- Bảo mật hệ thống bằng cách chỉ cho phép người được ủy quyền truy cập
- Phân chia quyền hạn giữa Admin (toàn quyền) và Staff (chỉ được phép quản lý nhà hàng được giao)

---

### Module 2: Dashboard (Bảng điều khiển)
**Mục đích:** Hiển thị tổng quan các số liệu thống kê và đơn hàng mới cần xử lý.

**Chức năng chi tiết:**
- **Stats Cards:** Hiển thị 3 thẻ thông tin: Tổng số đơn hàng hôm nay, Doanh thu hôm nay, Số đơn đang xử lý
- **New Orders List:** Danh sách các đơn hàng mới có trạng thái "pending" cần xử lý ngay
- **Top Items:** Top 5 món ăn bán chạy nhất trong ngày
- **Realtime Updates:** Tự động cập nhật khi có đơn mới thông qua Supabase Realtime

**Đối tượng sử dụng:** Admin và Staff ngay khi đăng nhập vào hệ thống

**Tác động nghiệp vụ:**
- Giúp Admin/Staff nhanh chóng nắm bắt tình hình kinh doanh
- Phát hiện và xử lý đơn hàng mới kịp thời

---

### Module 3: Quản lý đơn hàng (Order Management)
**Mục đích:** Quản lý toàn bộ đơn hàng từ khi tạo đến khi hoàn thành hoặc hủy.

**Chức năng chi tiết:**
- **Order List:** Danh sách đơn hàng với các tab lọc theo trạng thái (Tất cả, Chờ xác nhận, Đã xác nhận, Đang nấu, Đang giao, Hoàn thành, Đã hủy)
- **Search:** Tìm kiếm đơn hàng theo mã đơn, tên khách hàng hoặc số điện thoại
- **Order Detail:** Xem chi tiết đơn hàng bao gồm thông tin khách hàng, danh sách món, địa chỉ giao hàng và tổng tiền
- **Update Status:** Cập nhật trạng thái đơn hàng với kiểm tra machine state (chỉ cho phép chuyển đổi hợp lệ)
- **Cancel Order:** Hủy đơn hàng kèm lý do
- **Status Timeline:** Hiển thị lịch sử thay đổi trạng thái đơn hàng

**Đối tượng sử dụng:** Admin và Staff quản lý đơn hàng

**Tác động nghiệp vụ:**
- Theo dõi và quản lý toàn bộ lifecycle của đơn hàng
- Đảm bảo trạng thái đơn hàng được cập nhật chính xác
- Ghi nhận log thay đổi để phục vụ việc kiểm tra sau này

---

### Module 4: Quản lý nhà hàng (Restaurant Management)
**Mục đích:** CRUD (Create, Read, Update, Delete) thông tin nhà hàng.

**Chức năng chi tiết:**
- **Restaurant List:** Danh sách tất cả nhà hàng với tính năng bật/tắt trạng thái hoạt động
- **Add Restaurant:** Thêm nhà hàng mới với các thông tin như tên, địa chỉ, số điện thoại, giờ mở cửa, phí giao hàng
- **Edit Restaurant:** Sửa thông tin nhà hàng
- **Upload Image:** Tải lên hình ảnh nhà hàng lên Supabase Storage
- **Toggle Status:** Bật/tắt trạng thái is_active và is_open

**Đối tượng sử dụng:** Admin (Staff chỉ thấy nhà hàng được giao)

**Tác động nghiệp vụ:**
- Quản lý thông tin các nhà hàng trong hệ thống
- Điều chỉnh trạng thái hoạt động của nhà hàng

---

### Module 5: Quản lý danh mục (Category Management)
**Mục đích:** Quản lý các danh mục món ăn (Ví dụ: Món chính, Món phụ, Đồ uống, Tráng miệng).

**Chức năng chi tiết:**
- **Category List:** Danh sách danh mục theo thứ tự sắp xếp
- **Add Category:** Thêm danh mục mới
- **Edit Category:** Sửa tên, hình ảnh, thứ tự hiển thị của danh mục
- **Delete Category:** Xóa danh mục (nếu chưa có món nào thuộc danh mục này)

**Đối tượng sử dụng:** Admin

**Tác động nghiệp vụ:**
- Tổ chức và phân loại món ăn theo danh mục giúp người dùng dễ tìm kiếm

---

### Module 6: Quản lý món ăn (Menu Item Management)
**Mục đích:** Quản lý các món ăn trong thực đơn của từng nhà hàng.

**Chức năng chi tiết:**
- **Menu List:** Danh sách món ăn theo nhà hàng với bộ lọc theo danh mục
- **Add Menu Item:** Thêm món mới với tên, mô tả, giá, hình ảnh
- **Edit Menu Item:** Sửa thông tin món ăn
- **Toggle Available:** Bật/tắt trạng thái "còn hàng" / "hết hàng"
- **Toggle Featured:** Đánh dấu món nổi bật

**Đối tượng sử dụng:** Admin và Staff (Staff chỉ thấy menu của nhà hàng được giao)

**Tác động nghiệp vụ:**
- Quản lý thực đơn của các nhà hàng
- Cập nhật trạng thái món ăn theo thời gian thực

---

### Module 7: Quản lý người dùng (User Management)
**Mục đích:** Quản lý tài khoản khách hàng trong hệ thống.

**Chức năng chi tiết:**
- **User List:** Danh sách tất cả khách hàng
- **Search:** Tìm kiếm theo tên, email hoặc số điện thoại
- **User Detail:** Xem thông tin chi tiết và lịch sử đơn hàng của khách
- **Deactivate/Activate:** Vô hiệu hóa hoặc kích hoạt tài khoản khách hàng

**Đối tượng sử dụng:** Admin

**Tác động nghiệp vụ:**
- Quản lý tài khoản khách hàng
- Xử lý các vấn đề liên quan đến tài khoản

---

### Module 8: Báo cáo (Reports)
**Mục đích:** Thống kê và báo cáo doanh thu, đơn hàng theo thời gian.

**Chức năng chi tiết:**
- **Date Range Picker:** Chọn khoảng thời gian (Hôm nay, Tuần, Tháng, Tùy chọn)
- **Revenue Stats:** Tổng doanh thu, số đơn hàng, trung bình/đơn
- **Order Stats:** Số đơn hoàn thành, số đơn hủy, tỉ lệ hủy
- **Top Items:** Top 5 món bán chạy nhất
- **Top Restaurants:** Top 5 nhà hàng có doanh thu cao nhất
- **Chart:** Biểu đồ doanh thu theo ngày (Phase 2)

**Đối tượng sử dụng:** Admin

**Tác động nghiệp vụ:**
- Cung cấp số liệu để phân tích và đưa ra quyết định kinh doanh

---

### Module 9: Thông báo (Notifications)
**Mục đích:** Quản lý các thông báo trong app dành cho Admin/Staff.

**Chức năng chi tiết:**
- **Notification List:** Danh sách tất cả thông báo
- **Filter:** Lọc theo loại (đơn hàng, hệ thống)
- **Mark as Read:** Đánh dấu đã đọc hoặc đánh dấu tất cả đã đọc
- **Badge Count:** Hiển thị số thông báo chưa đọc trên menu

**Đối tượng sử dụng:** Admin và Staff

**Tác động nghiệp vụ:**
- Thông báo kịp thời về các sự kiện quan trọng (đơn hàng mới, cập nhật trạng thái...)

---

## PHẦN 2: CLIENT APP (App Khách hàng)

### Module 1: Xác thực (Authentication)
**Mục đích:** Xử lý đăng nhập, đăng ký và quên mật khẩu cho khách hàng.

**Chức năng chi tiết:**
- **Login:** Đăng nhập bằng email và mật khẩu
- **Register:** Đăng ký tài khoản mới với thông tin email, số điện thoại, mật khẩu
- **Forgot Password:** Gửi email đặt lại mật khẩu
- **Session Management:** Lưu trữ JWT token an toàn, tự động làm mới

**Đối tượng sử dụng:** Khách hàng muốn đặt đồ ăn

**Tác động nghiệp vụ:**
- Cho phép khách hàng truy cập vào hệ thống đặt hàng

---

### Module 2: Trang chủ (Home Screen)
**Mục đích:** Hiển thị nội dung chính của ứng dụng bao gồm banners, danh mục và nhà hàng.

**Chức năng chi tiết:**
- **Banner Carousel:** Hiển thị các banner quảng cáo (ViewPager2 tự động chạy)
- **Categories:** Danh sách danh mục món ăn cuộn ngang
- **Featured Restaurants:** Nhà hàng nổi bật (được đánh dấu là featured)
- **Near Restaurants:** Nhà hàng gần vị trí hiện tại của khách hàng
- **Search Bar:** Chuyển đến màn hình tìm kiếm
- **Pull to Refresh:** Cập nhật dữ liệu mới

**Đối tượng sử dụng:** Khách hàng mở app

**Tác động nghiệp vụ:**
- Trang landing page giới thiệu các nhà hàng và khuyến mãi

---

### Module 3: Chi tiết nhà hàng (Restaurant Detail)
**Mục đích:** Xem thông tin chi tiết của nhà hàng và menu.

**Chức năng chi tiết:**
- **Restaurant Info:** Tên, hình ảnh, địa chỉ, đánh giá, giờ mở cửa
- **Menu by Category:** Danh sách món ăn được tổ chức theo danh mục (TabLayout)
- **Add to Cart:** Thêm món vào giỏ hàng với số lượng
- **Item Note:** Ghi chú cho từng món (nếu cần)
- **Search in Restaurant:** Tìm kiếm món trong nhà hàng

**Đối tượng sử dụng:** Khách hàng muốn xem menu của một nhà hàng cụ thể

**Tác động nghiệp vụ:**
- Giúp khách hàng xem chi tiết thực đơn và thêm món vào giỏ

---

### Module 4: Giỏ hàng & Thanh toán (Cart & Checkout)
**Mục đích:** Quản lý giỏ hàng và thanh toán đơn hàng (COD).

**Chức năng chi tiết:**
- **View Cart:** Xem danh sách món trong giỏ hàng
- **Update Quantity:** Tăng/giảm số lượng món
- **Remove Item:** Xóa món khỏi giỏ
- **Apply Promo:** Nhập mã khuyến mãi để giảm giá
- **Select Address:** Chọn địa chỉ giao hàng hoặc thêm địa chỉ mới
- **Order Note:** Ghi chú cho đơn hàng
- **Place Order:** Đặt hàng với phương thức thanh toán COD (Tiền mặt)
- **Order Success:** Màn hình thông báo đặt hàng thành công

**Đối tượng sử dụng:** Khách hàng hoàn tất quá trình chọn món

**Tác động nghiệp vụ:**
- Hoàn tất quy trình đặt hàng
- Lưu trữ giỏ hàng cục bộ (Room Database)

---

### Module 5: Lịch sử đơn hàng (Order History)
**Mục đích:** Xem và theo dõi các đơn hàng đã đặt.

**Chức năng chi tiết:**
- **Order List:** Danh sách đơn hàng với các tab lọc (Tất cả, Đang xử lý, Hoàn thành, Đã hủy)
- **Order Detail:** Xem chi tiết đơn hàng
- **Status Timeline:** Theo dõi trạng thái đơn hàng bằng hình ảnh trực quan
- **Cancel Order:** Hủy đơn (nếu chưa được xác nhận)
- **Reorder:** Đặt lại đơn cũ
- **Submit Review:** Đánh giá sau khi nhận hàng (chỉ sau khi hoàn thành)
- **Realtime Updates:** Cập nhật trạng thái tự động

**Đối tượng sử dụng:** Khách hàng theo dõi đơn hàng của mình

**Tác động nghiệp vụ:**
- Giúp khách hàng theo dõi tình trạng đơn hàng
- Cho phép đánh giá sau khi nhận hàng

---

### Module 6: Hồ sơ cá nhân (User Profile)
**Mục đựng:** Quản lý thông tin cá nhân và địa chỉ giao hàng.

**Chức năng chi tiết:**
- **View Profile:** Xem thông tin tài khoản (tên, email, số điện thoại, avatar)
- **Edit Profile:** Cập nhật thông tin cá nhân
- **Address List:** Danh sách địa chỉ giao hàng
- **Add/Edit/Delete Address:** Thêm, sửa, xóa địa chỉ
- **Set Default:** Đặt địa chỉ mặc định
- **Logout:** Đăng xuất khỏi hệ thống

**Đối tượng sử dụng:** Khách hàng quản lý thông tin tài khoản

**Tác động nghiệp vụ:**
- Giúp khách hàng quản lý thông tin và địa chỉ giao hàng tiện lợi

---

## TỔNG KẾT

| App | Số Module | Mô tả |
|-----|-----------|-------|
| **Admin** | 9 | Quản lý toàn bộ hoạt động kinh doanh: từ đơn hàng, nhà hàng, món ăn, khách hàng đến báo cáo |
| **Client** | 6 | Ứng dụng cho khách hàng: từ đăng nhập, xem nhà hàng, đặt hàng đến theo dõi đơn |

**Mối quan hệ giữa Client và Admin:**
- Client gửi yêu cầu đặt hàng → Admin nhận và xử lý đơn
- Admin cập nhật trạng thái đơn → Client nhận thông báo realtime
- Cả hai app sử dụng chung một Supabase database để lưu trữ dữ liệu

---

*Document Version: 1.0*
*Created: 2026-04-04*
*Author: BA Team*