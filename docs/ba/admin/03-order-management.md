# Module: Quản lý đơn hàng - Admin App

## 1. Mục tiêu

Module Order Management dùng cho `admin` và `staff` để:

- Theo dõi toàn bộ vòng đời đơn hàng.
- Xử lý vận hành nhà hàng.
- Theo dõi quá trình điều phối tài xế.
- Phát hiện sớm rủi ro "không có shipper" trước khi món ăn hoàn tất.

Tài liệu này chuẩn hóa nghiệp vụ theo hướng mới:

- Tìm/gán shipper bắt đầu sớm, song song với lúc nhà hàng chuẩn bị món.
- Không đợi đến khi món xong mới tìm tài xế.
- Tách rõ `order status` và `delivery status`.

## 2. Vấn đề của flow cũ

Flow cũ kiểu:

`co don -> nha hang lam xong -> moi tim shipper`

có nhiều rủi ro:

- Nhà hàng làm xong nhưng không có shipper online.
- Đơn bị trễ ngay tại quán.
- ETA giao hàng vô nghĩa vì chỉ sau khi món xong mới bắt đầu dispatch.
- Khách hàng có trải nghiệm xấu và bỏ đơn.

Vi vậy, flow nghiệp vụ mới phải chuyển sang:

`co don -> xac nhan don -> bat dau tim shipper -> shipper duoc gan/chap nhan -> nha hang tiep tuc chuan bi -> mon san sang -> shipper lay hang -> giao hang`

## 3. Vai trò và phạm vi

### 3.1 Admin

- Xem tất cả đơn.
- Theo dõi cả trạng thái order và trạng thái delivery.
- Can thiệp khi hệ thống không tìm được shipper.
- Có thể gán lại shipper nếu cần.
- Giám sát SLA vận hành.

### 3.2 Staff

- Xác nhận đơn.
- Chuyển đơn sang chuẩn bị.
- Cập nhật món đã sẵn sàng lấy.
- Theo dõi tài xế đã được gán hay chưa để phối hợp giao nhận.

### 3.3 Shipper

- Nhận cuốc/nhận đơn từ flow delivery.
- Đi tới nhà hàng.
- Lấy hàng khi món đã sẵn sàng.
- Giao cho khách và xác nhận hoàn tất.

## 4. Hai nhóm trạng thái cần tách biệt

## 4.1 Order status

`orders.status` phản ánh vòng đời nghiệp vụ của đơn:

| Trạng thái | Ý nghĩa |
|---|---|
| `pending` | Đơn mới tạo, chờ xác nhận |
| `confirmed` | Đơn đã được chấp nhận |
| `preparing` | Nhà hàng đang chuẩn bị món |
| `ready_for_pickup` | Món đã sẵn sàng để lấy |
| `delivering` | Hàng đã được shipper lấy và đang giao |
| `delivered` | Giao thành công |
| `cancelled` | Đơn bị hủy |

## 4.2 Delivery status

Cần có một luồng delivery tách riêng, để xác định tài xế đang ở giai đoạn nào.

Đề xuất `orders.delivery_status` hoặc một mô hình tương đương:

| Delivery status | Ý nghĩa |
|---|---|
| `unassigned` | Chưa bắt đầu tìm tài xế |
| `searching` | Hệ thống đang tìm tài xế |
| `assigned` | Đã có tài xế nhận/được gán |
| `arriving_pickup` | Tài xế đang đi tới nhà hàng |
| `waiting_pickup` | Tài xế đã đến, đang chờ món |
| `picked_up` | Tài xế đã lấy hàng |
| `completed` | Flow giao hàng đã xong |
| `failed` | Điều phối thất bại/can can thiệp |

Nếu chưa đổi DB ngay, tài liệu vẫn coi đây là nghiệp vụ cần tồn tại ở cấp sản phẩm.

## 5. Flow nghiệp vụ chuẩn mới

### 5.1 Luồng tổng thể

`pending -> confirmed -> preparing -> ready_for_pickup -> delivering -> delivered`

song song với:

`unassigned -> searching -> assigned -> arriving_pickup/waiting_pickup -> picked_up -> completed`

### 5.2 Ý nghĩa flow mới

- `order status` trả lời câu hỏi: đơn đang ở bước vận hành nào.
- `delivery status` trả lời câu hỏi: tài xế đang ở bước điều phối nào.

Như vậy có thể xảy ra tình huống hợp lệ:

- Đơn đang `preparing`
- Nhưng delivery đã `assigned`

Đây là tình huống mong muốn, không phải lỗi.

## 6. Luồng nghiệp vụ chi tiết

### 6.1 Có đơn mới

Khi khách tạo đơn:

- `orders.status = pending`
- `delivery_status = unassigned`
- Chưa có `shipper_id`

### 6.2 Xác nhận đơn

Khi nhà hàng/admin chấp nhận:

- `orders.status = confirmed`
- Hệ thống có thể bắt đầu dispatch ngay hoặc chuyển sang `searching`

Khuyến nghị:

- Sau khi xác nhận đơn, hệ thống bắt đầu tìm shipper sớm.

### 6.3 Nhà hàng chuẩn bị món

Khi bắt đầu làm món:

- `orders.status = preparing`
- `delivery_status` có thể đang là `searching` hoặc `assigned`

Đây là điểm thay đổi quan trọng của nghiệp vụ mới:

- Không đợi món xong mới tìm shipper.
- Tìm shipper song song với `preparing`.

### 6.4 Tìm shipper

Hệ thống hoặc điều phối viên sẽ:

- Tìm shipper phù hợp
- Gửi cuốc/phân bố đơn
- Theo dõi timeout, retry, reassign nếu cần

Khi đang tìm:

- `delivery_status = searching`

### 6.5 Đã có shipper

Khi một shipper chấp nhận hoặc được gán thành công:

- `shipper_id = <shipper_user_id>`
- `delivery_status = assigned`

Đơn lúc này vẫn có thể đang `preparing`.

Nghĩa là:

- Tài xế đã có
- Nhà hàng vẫn đang nấu

Đây là trạng thái rất bình thường và cần hỗ trợ tốt trên UI.

### 6.6 Shipper đi tới nhà hàng

Sau khi đã assigned:

- `delivery_status = arriving_pickup`

Nếu shipper đến sớm và món chưa xong:

- `delivery_status = waiting_pickup`

### 6.7 Nhà hàng xác nhận món sẵn sàng

Khi món xong:

- `orders.status = ready_for_pickup`

Nếu shipper đến rồi, họ có thể lấy hàng gần như ngay lập tức.

Nếu shipper chưa tới, hệ thống vẫn biết đã có ai phụ trách đơn này.

### 6.8 Shipper lấy hàng

Khi shipper nhận hàng từ quán:

- `delivery_status = picked_up`
- `orders.status = delivering`

Đây là mốc quan trọng:

- `delivering` nên được hiểu là "đã lấy hàng và đang đi giao", không phải chỉ mới "đã được gán shipper".

### 6.9 Giao xong

Khi shipper giao thành công:

- `orders.status = delivered`
- `delivery_status = completed`

## 7. Xử lý trường hợp không có shipper

Đây là lý do chính phải đổi flow.

### 7.1 Hệ thống phát hiện sớm

Nếu sau khi xác nhận đơn mà không tìm được shipper:

- `delivery_status` phải vào `searching`
- Có timeout/retry rõ ràng
- Cảnh báo admin/staff sớm

Không được đợi tới lúc `ready_for_pickup` mới phát hiện "không có tài xế".

### 7.2 Các hành động nghiệp vụ khi tìm không ra

Có thể áp dụng 1 hoặc nhiều hướng:

- tiếp tục retry tìm shipper trong một cửa sổ thời gian
- admin/staff gán thủ công shipper
- thông báo ETA mới cho khách
- nếu qua ngưỡng cho phép thì hủy đơn theo policy

### 7.3 Mục tiêu vận hành

- Giảm tình trạng món xong nhưng nhặt cho shipper.
- Đưa cảnh báo lên sớm khi SLA điều phối đang xấu.

## 8. Gán shipper thủ công

Trong flow mới, admin có thể gán shipper thủ công mà không làm sai nghiệp vụ.

Sau khi gán:

- `shipper_id` được set
- `delivery_status = assigned`
- `orders.status` vẫn có thể là `confirmed` hoặc `preparing`

Không nên:

- Vừa gán shipper xong đã chuyển `orders.status = delivering`

vì lúc đó shipper có thể chưa đến quán, chưa lấy hàng.

## 9. Rule chuyển trạng thái đề xuất

### 9.1 Order status

| Từ trạng thái | Có thể chuyển sang | Ghi chú |
|---|---|---|
| `pending` | `confirmed`, `cancelled` | Đơn mới |
| `confirmed` | `preparing`, `cancelled` | Đã chấp nhận |
| `preparing` | `ready_for_pickup`, `cancelled` | Nhà hàng đang làm |
| `ready_for_pickup` | `delivering`, `cancelled` | `delivering` khi shipper đã lấy hàng |
| `delivering` | `delivered` | Đang giao |
| `delivered` | Không chuyển tiếp | Trạng thái cuối |
| `cancelled` | Không chuyển tiếp | Trạng thái cuối |

### 9.2 Delivery status

| Từ trạng thái | Có thể chuyển sang |
|---|---|
| `unassigned` | `searching`, `assigned`, `failed` |
| `searching` | `assigned`, `failed` |
| `assigned` | `arriving_pickup`, `waiting_pickup`, `failed` |
| `arriving_pickup` | `waiting_pickup`, `picked_up`, `failed` |
| `waiting_pickup` | `picked_up`, `failed` |
| `picked_up` | `completed` |
| `completed` | Không chuyển tiếp |
| `failed` | `searching`, `assigned`, `cancelled` theo policy |

## 10. Dữ liệu cần hiển thị trên admin

### 10.1 Danh sách đơn

- Mã đơn
- Khách hàng
- Nhà hàng
- Tổng tiền
- `orders.status`
- `delivery_status`
- Tên shipper nếu đã có
- ETA/chuẩn bị nếu có
- Cảnh báo nếu đang `searching` quá lâu

### 10.2 Chi tiết đơn

- Toàn bộ thông tin đơn
- Trạng thái nhà hàng
- Trạng thái điều phối shipper
- Shipper hiện tại
- Mốc thời gian:
  - tạo đơn
  - xác nhận đơn
  - bắt đầu tìm shipper
  - shipper được gán
  - món sẵn sàng
  - lấy hàng
  - giao xong

## 11. Realtime

Admin cần thấy ngay các thay đổi sau:

- Đơn vừa được tạo
- Đơn được xác nhận
- Bắt đầu tìm shipper
- Shipper đã được gán
- Món đã sẵn sàng
- Shipper đã lấy hàng
- Giao xong
- Hủy đơn

Realtime tối thiểu nên bắm:

- `orders`
- sau này có thể mở rộng thêm bảng/log delivery nếu được thêm vào DB

## 12. Edge cases

### 12.1 Đã có shipper nhưng nhà hàng làm chậm

Tình huống hợp lệ:

- `orders.status = preparing`
- `delivery_status = waiting_pickup`

UI không được coi đây là lỗi.

### 12.2 Nhà hàng xong nhưng vẫn chưa có shipper

Tình huống xấu nhưng phải được xử lý:

- `orders.status = ready_for_pickup`
- `delivery_status = searching` hoặc `failed`

Hệ thống phải cảnh báo rõ.

### 12.3 Đã gán shipper nhưng shipper hủy/bỏ cuốc

Cần support:

- clear `shipper_id` hoặc gán lại shipper
- đưa `delivery_status` về `searching`

### 12.4 Hủy đơn khi đã có shipper

Cần có policy rõ:

- nếu chưa lấy hàng thì có thể hủy và giải phong shipper
- nếu đã `picked_up` thì phải có flow exception riêng

## 13. Kết luận nghiệp vụ

Flow nghiệp vụ mới của module order là:

- Tìm shipper sớm, song song với lúc nhà hàng chuẩn bị món.
- `shipper_id` có thể xuất hiện trước khi đơn `delivering`.
- `delivering` chỉ bắt đầu sau khi shipper đã lấy hàng.
- Muốn mô tả nghiệp vụ đúng, phải tách `order status` và `delivery status`.

Tài liệu này là nguồn chuẩn nghiệp vụ cho admin order trong phase tiếp theo.