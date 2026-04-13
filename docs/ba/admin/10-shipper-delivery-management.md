# Module: Quản lý giao hàng - Shipper Delivery Management

## 1. Mục tiêu

Module này phục vụ người dùng có role `shipper`.

Mục tiêu:

- Cho shipper được gán đơn sớm.
- Giảm thời gian chờ sau khi nhà hàng làm xong món.
- Hỗ trợ shipper đi tới nhà hàng ngay trong lúc món đang được chuẩn bị.
- Tách rõ "đã được gán đơn" và "đã lấy hàng".

## 2. Tư duy nghiệp vụ mới

Flow thực tế cần là:

`co don -> nha hang xac nhan -> tim/gian tai xe -> tai xe di toi nha hang -> nha hang hoan tat -> tai xe lay hang -> giao hang`

Ý nghĩa:

- Shipper không nhất thiết phải đợi tới lúc `ready_for_pickup` mới được biết về đơn.
- Hệ thống dispatch phải chạy song song với quá trình nhà hàng `preparing`.
- `delivering` không đồng nghĩa với "đã được assign", mà phải là "đã lấy hàng và đang giao".

## 3. Hai nhóm trạng thái cần nhìn từ phía shipper

## 3.1 Order status

`orders.status`:

| Trạng thái | Ý nghĩa với shipper |
|---|---|
| `confirmed` | Đơn đã được chấp nhận, có thể bắt đầu dispatch |
| `preparing` | Nhà hàng đang làm món |
| `ready_for_pickup` | Món đã sẵn sàng để lấy |
| `delivering` | Shipper đã lấy hàng và đang giao |
| `delivered` | Giao thành công |
| `cancelled` | Đơn bị hủy |

## 3.2 Delivery status

Cần có một trạng thái giao vận để shipper biết mình đang ở bước nào:

| Delivery status | Ý nghĩa |
|---|---|
| `searching` | Hệ thống đang tìm tài xế |
| `assigned` | Đơn đã được shipper nhận/được gán |
| `arriving_pickup` | Shipper đang đi tới quán |
| `waiting_pickup` | Shipper đã đến, chờ món |
| `picked_up` | Đã lấy hàng |
| `completed` | Đã giao xong |
| `failed` | Flow điều phối thất bại/cần xử lý lại |

## 4. Luồng nghiệp vụ chi tiết của shipper

### 4.1 Đơn được đưa vào dispatch

Khi đơn đã được xác nhận:

- Hệ thống bắt đầu tìm shipper.
- Shipper có thể nhìn thấy cuốc sớm.

Lúc này:

- `orders.status` có thể là `confirmed` hoặc `preparing`
- `delivery_status = searching`

### 4.2 Shipper được gán/nhận đơn

Khi shipper chấp nhận hoặc được hệ thống gán:

- `shipper_id` được set
- `delivery_status = assigned`

Lưu ý:

- Đơn lúc này vẫn có thể đang `preparing`
- Chưa được coi là `delivering`

### 4.3 Shipper đi tới nhà hàng

Khi shipper bắt đầu đi tới điểm lấy hàng:

- `delivery_status = arriving_pickup`

Nếu đến quán nhưng món chưa xong:

- `delivery_status = waiting_pickup`

### 4.4 Nhà hàng xác nhận sẵn sàng

Khi món xong:

- `orders.status = ready_for_pickup`

Nếu shipper đã ở quán thì thời gian chờ sẽ rất ngắn.

### 4.5 Shipper lấy hàng

Chỉ khi shipper lấy hàng xong mới cập nhật:

- `delivery_status = picked_up`
- `orders.status = delivering`

Đây là định nghĩa chuẩn của `delivering`.

### 4.6 Giao xong

Khi giao thành công:

- `orders.status = delivered`
- `delivery_status = completed`

## 5. Màn hình shipper đề xuất

Không nên chỉ có 3 tab kiểu cũ. Theo flow mới, shipper nên có các nhóm dữ liệu sau.

### 5.1 Tab Cuốc mới

Hiển thị:

- Các đơn đang `searching`
- hoặc đơn vừa `assigned` cho shipper nhưng chưa đến quán

Thông tin:

- Mã đơn
- Nhà hàng
- Địa chỉ giao
- Khoảng cách
- Tổng tiền
- Thời gian dự kiến sẵn sàng

Hành động:

- Nhận cuốc
- Từ chối cuốc

### 5.2 Tab Sắp lấy hàng

Hiển thị:

- Đơn đã được gán cho shipper
- `orders.status = preparing` hoặc `ready_for_pickup`
- `delivery_status` là `assigned`, `arriving_pickup`, `waiting_pickup`

Đây là tab rất quan trọng trong flow mới.

### 5.3 Tab Đang giao

Hiển thị:

- Đơn đã `picked_up`
- `orders.status = delivering`

Hành động:

- Gọi khách
- Xem chỉ đường
- Xác nhận giao xong

### 5.4 Tab Lịch sử

Hiển thị:

- Đơn `delivered`
- có `shipper_id = current_shipper`

## 6. Quy tắc nghiệp vụ khi nhận/gán shipper

### 6.1 Khi nào shipper được coi là "có đơn"

Ngay khi `shipper_id` được gán và `delivery_status = assigned`, shipper đã có trách nhiệm với đơn.

Nhưng:

- Chưa được coi là đã lấy hàng
- Chưa được coi là đang giao

### 6.2 Khi nào shipper được coi là "đang giao"

Chỉ khi:

- Món đã được lấy khỏi nhà hàng
- `orders.status = delivering`
- `delivery_status = picked_up`

### 6.3 Trường hợp shipper bỏ cuốc

Nếu shipper được gán nhưng không tiếp tục:

- `shipper_id` có thể bị clear hoặc gán shipper mới
- `delivery_status` quay lại `searching`

Nếu lúc đó nhà hàng đã xong món thì flow dispatch phải được ưu tiên cao.

## 7. Quy tắc về tài xế online/offline

`shippers.delivery_status` của profile shipper vẫn cần tồn tại để thể hiện khả năng nhận cuốc:

| Trạng thái shipper | Ý nghĩa |
|---|---|
| `available` | Có thể được dispatch |
| `busy` | Đang xử lý đơn khác |
| `offline` | Không tham gia nhận cuốc |

Tuy nhiên cần phân biệt:

- `shippers.delivery_status` là trạng thái tổng của tài xế
- `orders.delivery_status` là trạng thái delivery của từng đơn

## 8. Điều kiện hiển thị đơn cho shipper

Theo flow mới, shipper có thể thấy đơn sớm hơn.

### 8.1 Đơn có thể xuất hiện trước khi món xong

Nếu business cho phép dispatch sớm, shipper có thể thấy đơn khi:

- `orders.status = confirmed` hoặc `preparing`
- đơn đang được tìm/gán cho shipper

### 8.2 Đơn có ưu tiên cao khi món sắp xong hoặc đã xong

Khi:

- `orders.status = ready_for_pickup`

thì UI shipper phải ưu tiên cao hơn, vì đây là đơn cần lấy ngay.

## 9. Atomic dispatch và tránh chặt

Nếu vẫn cho shipper tự nhận cuốc, hệ thống phải có cơ chế atomic.

Nhưng khác với bản cũ:

- Atomic ở đây dùng cho việc `assign shipper`
- không phải đồng nghĩa cập nhật luôn `orders.status = delivering`

Kết quả thành công của RPC/transaction nhận cuốc nên là:

- gán `shipper_id`
- set `delivery_status = assigned`

Không nên đổi `orders.status` sang `delivering` tại bước này.

## 10. Edge cases

### 10.1 Đã có shipper nhưng món chưa xong

Tình huống bình thường:

- `orders.status = preparing`
- `delivery_status = assigned` hoặc `waiting_pickup`

### 10.2 Món xong nhưng shipper vẫn chưa có

Tình huống xấu:

- `orders.status = ready_for_pickup`
- `delivery_status = searching` hoặc `failed`

Hệ thống phải đẩy cảnh báo mạnh lên admin/điều phối.

### 10.3 Shipper đến sớm

Tình huống hợp lệ:

- `orders.status = preparing`
- `delivery_status = waiting_pickup`

### 10.4 Hủy đơn sau khi đã assign shipper

Cần có flow:

- thông báo cho shipper
- giải phong shipper
- ghi log vận hành

## 11. Kết luận nghiệp vụ

Flow shipper mới là:

- nhận cuốc sớm
- đi tới nhà hàng trong lúc món đang làm
- chờ nếu cần
- chỉ khi lấy hàng xong mới chuyển đơn sang `delivering`

Điều này giải quyết đúng vấn đề vận hành:

- không để tới lúc món xong mới phát hiện không có shipper
- giảm thời gian chờ của khách và nhà hàng
- nghiệp vụ rõ ràng hơn nhiều so với việc dồn tất cả vào `orders.status`