# 📄 Module: Quản lý Giao hàng (Shipper Delivery Management)

## 1. Tổng quan (Overview)
Module này được thiết kế dành riêng cho người dùng có vai trò **Shipper**. Mục tiêu là tách biệt quy trình giao hàng khỏi quy trình quản lý đơn hàng chung của Admin, giúp Shipper tập trung tối đa vào việc nhận và xử lý đơn, đồng thời đảm bảo tính chính xác và realtime.

## 2. Quy trình Nghiệp vụ (Business Workflow)

Quy trình giao hàng được chuẩn hóa qua các bước sau:

1.  **Chế biến xong:** Đơn hàng sau khi được nhà hàng chuẩn bị xong sẽ được chuyển sang trạng thái `ready_for_pickup`.
2.  **Đưa vào Pool:** Hệ thống tự động đẩy đơn hàng này vào "Bể đơn" (Available Orders) của các Shipper đang có trạng thái `available` (Online và đang rảnh).
3.  **Nhận đơn:** 
    *   Shipper chủ động chọn đơn từ danh sách và nhấn "Nhận đơn".
    *   Hệ thống sử dụng cơ chế **Atomic Update** (Hàm RPC) để kiểm tra tranh chấp. Nếu đơn đã có người nhận trước đó 1 giây, hệ thống sẽ báo lỗi cho người đến sau.
4.  **Thực hiện giao hàng:**
    *   Khi nhận đơn thành công, trạng thái đơn chuyển sang `delivering`, trạng thái Shipper chuyển sang `busy`.
    *   Shipper di chuyển đến nhà hàng lấy hàng và sau đó đến nhà khách.
5.  **Hoàn thành:**
    *   Shipper nhấn "Xác nhận đã giao".
    *   Trạng thái đơn chuyển sang `delivered`, trạng thái Shipper quay lại `available` để nhận đơn tiếp theo.

## 3. Cấu trúc Trạng thái (Status Mapping)

### A. Trạng thái Đơn hàng (`orders.status`)
| Trạng thái | Ý nghĩa | Đối tượng tác động |
| :--- | :--- | :--- |
| `preparing` | Nhà hàng đang nấu | Staff/Admin |
| `ready_for_pickup` | Đã nấu xong, chờ Shipper | Staff/Admin |
| `delivering` | Shipper đã lấy hàng và đang đi | Shipper |
| `delivered` | Giao hàng thành công | Shipper |
| `cancelled` | Đơn bị hủy (có lý do) | Admin/Staff |

### B. Trạng thái hoạt động của Shipper (`shippers.delivery_status`)
| Trạng thái | Ý nghĩa |
| :--- | :--- |
| `available` | Đang trực tuyến (Online) và rảnh, sẵn sàng nhận đơn. |
| `busy` | Đang bận xử lý một đơn hàng khác. |
| `offline` | Đang ngoại tuyến (Tắt ứng dụng hoặc nghỉ). |

## 4. Thiết kế Giao diện (UI/UX)

Màn hình **Quản lý giao hàng** của Shipper sẽ bao gồm 3 Tab:

### Tab 1: Đơn mới (Available)
*   Hiển thị danh sách các đơn đang ở trạng thái `ready_for_pickup`.
*   Thông tin hiển thị: Mã đơn, tổng tiền, địa chỉ nhà hàng, địa chỉ khách hàng.
*   Hành động: Nút **"Nhận đơn"**.

### Tab 2: Đang giao (Ongoing)
*   Hiển thị thông tin chi tiết của đơn hàng mà Shipper đó đã nhận thành công.
*   Thông tin hiển thị: Danh sách món ăn, ghi chú của khách, số điện thoại khách (có nút gọi nhanh).
*   Hành động: Nút **"Xác nhận đã lấy hàng"**, **"Hoàn thành giao hàng"**.

### Tab 3: Lịch sử (History)
*   Danh sách các đơn đã hoàn thành trong ngày của Shipper.
*   Thống kê nhanh: Tổng số đơn, tổng doanh thu nhận được.

## 5. Giải pháp Kỹ thuật (Technical Implementation)

### A. Xử lý tranh chấp (Race Condition)
Sử dụng hàm SQL Function (`accept_order`) trên Supabase:
*   Hàm này thực hiện một **Transaction**.
*   Kiểm tra `IF shipper_id IS NULL` trước khi `UPDATE`.
*   Đảm bảo tính duy nhất: Một đơn hàng chỉ có duy nhất 1 shipper tại một thời điểm.

### B. Realtime Updates
*   Sử dụng **Supabase Realtime** để lắng nghe thay đổi của bảng `orders`.
*   Khi Admin đổi trạng thái đơn sang `ready_for_pickup`, danh sách của Shipper sẽ tự hiện đơn mới mà không cần vuốt để tải lại.

### C. Quản lý Hồ sơ
*   Shipper được phép tự cập nhật: Họ tên, Số điện thoại, Biển số xe, Loại xe và Ảnh đại diện.
*   Các thông tin nhạy cảm khác (Lương, Hợp đồng) do Admin quản lý.

---
*Tài liệu được biên soạn vào ngày 10/04/2026 bởi Gemini CLI Assistant.*
