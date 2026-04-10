---
title: Order Sync Between Client And Admin Apps
tags:
  - business-analysis
  - orders
  - admin-app
  - client-app
  - supabase
aliases:
  - order-sync-requirements
---

# Bài toán

Mục tiêu là quản lý đơn hàng do app client tạo ra và hiển thị, xử lý chuẩn trên app admin theo cách:

- đồng bộ dữ liệu và trạng thái giữa 2 app
- không lệch logic giữa client và admin
- thao tác nhanh cho nhân sự vận hành
- chịu tải tốt ở giờ cao điểm như buổi trưa

# Hiện trạng đã xác nhận từ code

## Admin app

- Điều hướng đã có menu `Quản lý đơn hàng`, nhưng chưa gắn màn hình thật trong [MainActivity.java](C:/UTT/AppFood/FoodCouriers-Admin/app/src/main/java/com/utt/foodcouriers_admin/MainActivity.java) và [MainActivity.java](C:/UTT/AppFood/FoodCouriers-Admin/app/src/main/java/com/utt/foodcouriers_admin/ui/main/MainActivity.java).
- UI order mới dừng ở layout trong [activity_order_list.xml](C:/UTT/AppFood/FoodCouriers-Admin/app/src/main/res-layouts/order/layout/activity_order_list.xml), [activity_order_detail.xml](C:/UTT/AppFood/FoodCouriers-Admin/app/src/main/res-layouts/order/layout/activity_order_detail.xml), [item_order.xml](C:/UTT/AppFood/FoodCouriers-Admin/app/src/main/res-layouts/order/layout/item_order.xml).
- Java cho module order hiện là placeholder trong [a.java](C:/UTT/AppFood/FoodCouriers-Admin/app/src/main/java/com/utt/foodcouriers_admin/ui/order/a.java) và [a.java](C:/UTT/AppFood/FoodCouriers-Admin/app/src/main/java/com/utt/foodcouriers_admin/ui/order/adapter/a.java).
- App admin đang có 2 pattern data access:
  - pattern repository tương đối chuẩn qua [BaseSupabaseRepository.java](C:/UTT/AppFood/FoodCouriers-Admin/app/src/main/java/com/utt/foodcouriers_admin/data/repository/base/BaseSupabaseRepository.java) và [CategoryFragment.java](C:/UTT/AppFood/FoodCouriers-Admin/app/src/main/java/com/utt/foodcouriers_admin/ui/category/CategoryFragment.java)
  - pattern gọi client trực tiếp từ UI như [MenuItemFragment.java](C:/UTT/AppFood/FoodCouriers-Admin/app/src/main/java/com/utt/foodcouriers_admin/ui/menu/MenuItemFragment.java)
- Với order, nên đi theo pattern repository, không lặp lại hướng UI gọi trực tiếp client.

## Backend và database

- Schema order đã có trong [001_initial_schema.sql](C:/UTT/AppFood/FoodCouriers-Admin/docs/supabase/migrations/001_initial_schema.sql):
  - `orders`
  - `order_items`
  - `order_status_logs`
  - `notifications`
  - `payment_transactions`
- Luồng tạo đơn và cập nhật trạng thái đã có RPC trong [002_rpc_functions.sql](C:/UTT/AppFood/FoodCouriers-Admin/docs/supabase/migrations/002_rpc_functions.sql):
  - `rpc_create_order`
  - `rpc_update_order_status`
- RLS hiện tại trong [003_rls_policies.sql](C:/UTT/AppFood/FoodCouriers-Admin/docs/supabase/migrations/003_rls_policies.sql):
  - customer xem đơn của chính họ
  - admin xem và cập nhật toàn bộ đơn
  - chưa có policy chuẩn cho staff theo nhà hàng
- Migration [007_add_shipper_role.sql](C:/UTT/AppFood/FoodCouriers-Admin/docs/supabase/migrations/007_add_shipper_role.sql) đã thêm `shipper_id` và policy cho shipper.

# Constraint Gap

## Gap 1: Chưa có nguồn sự thật chung ở tầng nghiệp vụ giữa 2 app

Client và admin hiện đều đang được mô tả dùng chung RPC `rpc_update_order_status`, đây là hướng đúng. Tuy nhiên chưa có contract nghiệp vụ được đóng gói đủ rõ:

- ai được chuyển trạng thái nào
- ở trạng thái nào thì cho hủy
- ai được gán shipper
- khi nào gửi notification
- khi nào cho phép client nhìn thấy trạng thái trung gian

Nếu không khóa các rule này ở backend, 2 app sẽ dần lệch logic.

## Gap 2: RLS chưa phản ánh đúng mô hình vận hành nhà hàng

Hiện `orders_select_all` và `orders_update_all` mới mở cho `admin`. Điều này chưa phù hợp nếu app admin được dùng bởi:

- admin hệ thống
- manager/operator của từng nhà hàng
- shipper

Kết quả là staff nhà hàng có thể đăng nhập app admin nhưng chưa chắc đọc và thao tác đơn đúng phạm vi.

## Gap 3: Chưa có danh sách đơn tối ưu cho giờ cao điểm

Layout list hiện có tìm kiếm và tab trạng thái, nhưng chưa có:

- phân trang hoặc incremental loading
- realtime merge strategy
- ưu tiên đơn mới/chờ xử lý
- snapshot summary để nhân viên không phải mở detail mới hiểu đơn
- cơ chế chống thao tác đè nhau khi nhiều người cùng xử lý

## Gap 4: Status machine hiện chưa đủ rõ theo vai trò

`rpc_update_order_status` đang validate theo trạng thái hiện tại, nhưng chưa ràng theo actor:

- customer
- admin
- restaurant staff
- shipper

Về mặt đồng bộ, rule đúng phải là:

- client không được patch trực tiếp `orders.status`
- mọi chuyển trạng thái phải đi qua RPC hoặc edge function
- backend quyết định transition hợp lệ theo `current_status + actor_role + actor_scope`

## Gap 5: Chưa có read model riêng cho màn hình admin

Admin list cần xem rất nhanh:

- mã đơn
- khách hàng
- nhà hàng
- tổng tiền
- thời gian vào đơn
- trạng thái
- thanh toán
- số món
- ghi chú ngắn

Nếu mỗi item phải join nhiều bảng nặng hoặc phải mở detail mới đủ dữ liệu thì sẽ chậm ở khung giờ trưa.

# Kiến trúc đề xuất

## 1. Single source of truth

Nguồn sự thật duy nhất phải là Supabase Postgres + RPC:

- client app chỉ tạo đơn qua `rpc_create_order`
- admin app chỉ đổi trạng thái qua `rpc_update_order_status` hoặc RPC mới mở rộng
- mọi thay đổi trạng thái đều ghi `order_status_logs`
- mọi app chỉ đọc dữ liệu đã được backend xác nhận

Không cho 2 app tự cập nhật `orders.status` bằng REST patch trực tiếp.

## 2. Tách rõ write model và read model

### Write model

- `orders`
- `order_items`
- `order_status_logs`
- `notifications`
- `payment_transactions`

### Read model cho admin

Đề xuất thêm view hoặc API read-focused, ví dụ `admin_order_list_view`, gồm:

- `order_id`
- `order_code`
- `created_at`
- `status`
- `payment_status`
- `restaurant_id`
- `restaurant_name`
- `customer_name`
- `customer_phone`
- `item_count`
- `total`
- `shipper_id`
- `shipper_name`
- `is_priority`
- `last_status_changed_at`

Read model này giúp:

- list tải nhanh
- filter và sort đơn giản
- giảm join lặp lại ở app

## 3. Role-based state machine

Trạng thái nghiệp vụ nên chuẩn hóa như sau:

- `pending`
- `confirmed`
- `preparing`
- `ready_for_delivery` hoặc giữ `delivering` nếu chưa muốn mở rộng
- `delivering`
- `delivered`
- `cancelled`

Nếu giữ tối thiểu theo schema hiện tại thì dùng:

- `pending`
- `confirmed`
- `preparing`
- `delivering`
- `delivered`
- `cancelled`

Nhưng cần ràng vai trò:

- customer: chỉ được hủy khi `pending`
- restaurant staff: `pending -> confirmed`, `confirmed -> preparing`, `preparing -> delivering`, có thể `cancelled` theo policy
- shipper: chỉ `delivering -> delivered`
- admin hệ thống: có thể can thiệp, nhưng vẫn đi qua audit log

Khuyến nghị: sửa RPC để validate cả actor role/scope, không chỉ validate theo trạng thái.

## 4. Scope theo nhà hàng

Nếu một staff thuộc nhà hàng A thì:

- chỉ nhìn thấy order của nhà hàng A
- chỉ thao tác order của nhà hàng A
- không thấy order nhà hàng khác

Điểm này phải khóa ở RLS trước khi hoàn thiện UI.

# Luồng đồng bộ chuẩn giữa 2 app

## Tạo đơn từ client

1. Client gọi `rpc_create_order`.
2. RPC snapshot giá món, tên món, khuyến mãi, phí giao hàng vào `orders` và `order_items`.
3. Backend ghi `order_status_logs` với `pending`.
4. Backend tạo notification cho khách.
5. Admin app nhận đơn mới từ list polling ngắn hạn hoặc realtime feed.

## Xử lý đơn từ admin

1. Nhân viên mở queue `pending`.
2. Chọn đơn và bấm action phù hợp.
3. Admin app gọi RPC đổi trạng thái.
4. RPC:
   - kiểm tra quyền
   - kiểm tra transition
   - cập nhật `orders`
   - ghi `order_status_logs`
   - tạo notification cho client
5. Client app cập nhật timeline từ cùng nguồn dữ liệu.

## Đồng bộ hiển thị

Client và admin đều đọc cùng:

- `orders.status`
- `order_status_logs`
- `notifications`

Không app nào tự tính timeline riêng.

# Thiết kế admin UX để thao tác nhanh

## Màn hình list phải là màn hình vận hành, không chỉ là danh sách

Tab/queue đề xuất:

- `Mới vào`
- `Chờ xác nhận`
- `Đang chuẩn bị`
- `Đang giao`
- `Hoàn thành`
- `Đã hủy`

Nếu chưa muốn thêm trạng thái mới thì dùng mapping UI từ trạng thái hiện có.

## Mỗi item đơn cần hiển thị ngay tại list

- mã đơn
- giờ vào đơn
- tên khách
- số điện thoại rút gọn hoặc full
- nhà hàng
- số món
- tổng tiền
- trạng thái thanh toán
- ghi chú nổi bật
- badge SLA, ví dụ `3 phút chưa xác nhận`

## Tối ưu thao tác

- có action nhanh ngay trên item:
  - `Xác nhận`
  - `Bắt đầu chuẩn bị`
  - `Bàn giao shipper`
- giữ detail screen cho thao tác sâu hơn
- search ưu tiên:
  - mã đơn
  - tên khách
  - số điện thoại
- filter thêm:
  - nhà hàng
  - phương thức thanh toán
  - trạng thái thanh toán

## Chống thao tác nhầm

- disable action không hợp lệ ngay trên UI
- khi RPC trả lỗi transition thì refresh đơn ngay
- hiển thị ai vừa đổi trạng thái gần nhất trong detail/timeline

# Thiết kế chịu tải giờ cao điểm

## Backend

Ưu tiên 1:

- tất cả cập nhật trạng thái qua RPC
- bổ sung index nếu cần cho query vận hành:
  - `(restaurant_id, status, created_at desc)`
  - `(status, created_at desc)`
  - `(shipper_id, status, created_at desc)`

Ưu tiên 2:

- thêm read model riêng cho admin queue
- tránh query `select=*` ở list
- chỉ lấy field cần thiết cho list

Ưu tiên 3:

- dùng optimistic concurrency:
  - update có điều kiện theo `id + current_status`
  - nếu trạng thái đã đổi bởi người khác thì RPC fail với message rõ ràng

## Admin app

- danh sách mặc định chỉ tải queue hoạt động, không tải tất cả lịch sử
- dùng phân trang hoặc load thêm theo `created_at`
- debounce search
- merge realtime event vào đầu list thay vì reload toàn màn hình
- tách list summary và detail fetch riêng
- dùng repository thống nhất cho order để giữ luồng dữ liệu rõ ràng

## Realtime strategy

Nếu realtime Supabase ổn định:

- subscribe theo scope phù hợp:
  - admin toàn hệ thống
  - staff theo `restaurant_id`
  - shipper theo `shipper_id`

Nếu realtime chưa ổn định ở Android Java hiện tại:

- dùng hybrid:
  - polling ngắn 10-15 giây cho queue hoạt động
  - manual refresh
  - realtime chỉ để đẩy badge hoặc tín hiệu có đơn mới

Khuyến nghị thực tế cho giai đoạn đầu: hybrid, vì dễ kiểm soát hơn trong giờ cao điểm.

# Đề xuất thay đổi backend để chuẩn giữa 2 app

## Bắt buộc

1. Mở rộng RLS cho restaurant staff theo `restaurant_staff`.
2. Nâng `rpc_update_order_status` để kiểm tra:
   - actor role
   - actor restaurant scope
   - current status
3. Chuẩn hóa response lỗi nghiệp vụ để 2 app hiển thị thống nhất.
4. Thêm API/view read model cho admin list.

## Nên làm sớm

1. Thêm RPC `rpc_assign_shipper`.
2. Thêm `last_status_changed_at`.
3. Thêm cờ ưu tiên hoặc SLA derived field cho queue vận hành.
4. Ghi `admin_logs` khi admin/staff tác động order.

# Đề xuất cấu trúc module order cho app admin

Theo pattern đang đúng của project:

- `data/model/Order.java`
- `data/model/OrderItem.java`
- `data/model/OrderStatusLog.java`
- `data/request/OrderStatusUpdateRequest.java` nếu cần
- `data/repository/OrderRepository.java`
- `ui/order/OrderFragment.java`
- `ui/order/OrderDetailActivity.java` hoặc `OrderDetailFragment.java`
- `ui/order/adapter/OrderAdapter.java`
- `ui/order/adapter/OrderItemAdapter.java`

Repository nên cung cấp:

- `getOrders(status, query, restaurantId, limit, offset)`
- `getOrderDetail(orderId)`
- `getOrderStatusLogs(orderId)`
- `updateOrderStatus(orderId, currentStatus, newStatus, note)`
- `assignShipper(orderId, shipperId)`

# Lộ trình triển khai

## Phase 1: Chuẩn hóa backend contract

- khóa RLS đúng vai trò
- nâng RPC đổi trạng thái
- chuẩn hóa query list/detail
- thống nhất enum trạng thái dùng chung cho 2 app

## Phase 2: Dựng admin order MVP

- list theo tab trạng thái
- search
- detail
- action đổi trạng thái
- refresh thủ công + polling ngắn hạn

## Phase 3: Tối ưu vận hành

- action nhanh trên list
- realtime badge/feed
- assign shipper
- bộ lọc nâng cao
- audit đầy đủ

## Phase 4: Tối ưu tải cao

- read model chuyên cho queue
- phân trang tốt hơn
- conflict handling tốt hơn
- dashboard SLA giờ cao điểm

# Quyết định khuyến nghị

1. Dùng database + RPC làm nguồn sự thật duy nhất cho order lifecycle.
2. Không cho client/admin cập nhật trạng thái trực tiếp bằng REST patch.
3. Hoàn thiện RLS cho `staff theo nhà hàng` trước khi dựng UI order.
4. Triển khai admin order theo repository pattern như Category, không theo pattern UI gọi client trực tiếp như Menu hiện tại.
5. Giai đoạn đầu dùng hybrid sync: polling ngắn + manual refresh + realtime tín hiệu đơn mới.
6. Khi tải tăng, thêm read model riêng cho admin queue thay vì bắt app join dữ liệu nặng ở client side.

# Kết luận

Hướng đúng không phải là chỉ "làm màn hình order" cho app admin. Trục chính phải là:

- chuẩn hóa state machine ở backend
- chuẩn hóa quyền theo vai trò và theo nhà hàng
- chuẩn hóa read model để admin thao tác nhanh
- sau đó mới dựng module order trong admin app theo repository pattern

Nếu làm theo thứ tự này thì 2 app sẽ đồng bộ chuẩn, giảm lệch logic, và đủ nền để chịu tải tốt hơn ở các khung giờ nhiều đơn.
