# Sơ đồ luồng - Module Quản lý đơn hàng (Mermaid)

## 1. Load Order List Flow

```mermaid
sequenceDiagram
    participant User as Admin/Staff
    participant UI as OrderListFragment
    participant VM as OrderViewModel
    participant Repo as OrderRepository
    participant Supabase as Supabase

    User->>UI: onViewCreated()
    UI->>VM: loadOrders(status)
    
    VM->>Repo: getOrdersByStatus(status)
    Repo->>Supabase: GET /rest/v1/orders?status=eq.{status}&select=*,user(*),restaurant(*)
    Supabase-->>Repo: List<Order>
    Repo-->>VM: LiveData<List<Order>>
    VM-->>UI: Update adapter
    UI->>User: Display orders
```

## 2. Order List Tab Filtering

```mermaid
sequenceDiagram
    participant User as Admin
    participant UI as TabLayout
    participant VM as OrderViewModel

    User->>UI: Tap tab "Chờ xác nhận"
    UI->>VM: onTabSelected("pending")
    VM->>VM: loadOrders("pending")
    VM->>UI: Update list with pending orders
    
    User->>UI: Tap tab "Đã xác nhận"
    UI->>VM: onTabSelected("confirmed")
    VM->>VM: loadOrders("confirmed")
    VM->>UI: Update list with confirmed orders
    
    User->>UI: Tap tab "Tất cả"
    UI->>VM: onTabSelected(null)
    VM->>VM: loadOrders(null)
    VM->>UI: Update list with all orders
```

## 3. Order Detail & Status Update Flow

```mermaid
sequenceDiagram
    participant User as Admin
    participant UI as OrderDetailActivity
    participant VM as OrderViewModel
    participant Repo as OrderRepository
    participant RPC as RPC Function
    participant DB as Supabase DB

    User->>UI: Tap order item
    UI->>VM: loadOrderDetail(orderId)
    
    par Load Order Info
        VM->>Repo: getOrderHeader(orderId)
        Repo->>DB: SELECT orders WHERE id=orderId
    and Load Order Items
        VM->>Repo: getOrderItems(orderId)
        Repo->>DB: SELECT order_items WHERE order_id=orderId
    and Load Status Logs
        VM->>Repo: getStatusLogs(orderId)
        Repo->>DB: SELECT order_status_logs WHERE order_id=orderId
    end
    
    VM-->>UI: Display all info
    
    User->>UI: Tap "Xác nhận"
    UI->>VM: updateStatus(orderId, "confirmed")
    
    VM->>VM: Validate state machine
    
    alt Invalid Transition
        VM-->>UI: Show error "Không thể chuyển sang trạng thái này"
    else Valid Transition
        VM->>Repo: updateOrderStatus(orderId, "confirmed", userId)
        Repo->>RPC: rpc_update_order_status()
        
        RPC->>DB: UPDATE orders SET status='confirmed'
        RPC->>DB: INSERT order_status_logs
        RPC->>DB: INSERT notifications
        
        RPC-->>Repo: Success
        Repo-->>VM: Success
        
        par Refresh List
            VM->>UI: refreshOrderList()
        and Show Success
            VM-->>UI: Show snackbar "Đã xác nhận đơn hàng"
        end
    end
```

## 4. Cancel Order Flow

```mermaid
sequenceDiagram
    participant User as Admin
    participant UI as OrderDetailActivity
    participant Dialog as CancelDialog
    participant VM as OrderViewModel
    participant RPC as RPC Function

    User->>UI: Tap "Hủy đơn"
    UI->>Dialog: Show dialog
    
    Dialog->>User: Hiển thị lý do hủy
    
    User->>Dialog: Chọn lý do + Xác nhận
    Dialog->>VM: cancelOrder(orderId, reason)
    
    VM->>RPC: rpc_update_order_status(orderId, "cancelled", userId, reason)
    
    RPC-->>VM: Success
    
    par Update Order
        VM->>UI: Update status to "cancelled"
    and Log Action
        VM->>UI: Insert status log
    and Notify Customer
        VM->>UI: Create notification
    end
    
    VM-->>UI: Show snackbar "Đã hủy đơn hàng"
    UI->>User: Hiển thị kết quả
```

## 5. Order Status State Machine

```mermaid
stateDiagram-v2
    [*] --> pending: Client đặt hàng
    
    pending --> confirmed: Admin xác nhận
    pending --> cancelled: Admin/Hệ thống hủy
    
    confirmed --> preparing: Bắt đầu nấu
    confirmed --> cancelled: Hủy (trước khi nấu)
    
    preparing --> delivering: Bắt đầu giao
    preparing --> cancelled: Hủy (khi đang chuẩn bị)
    
    delivering --> delivered: Giao thành công
    delivering --> cancelled: Giao thất bại
    
    delivered --> [*]: Hoàn tất
    
    cancelled --> [*]: Final state
    
    note right of pending: Chờ xác nhận
    note right of confirmed: Đã xác nhận
    note right of preparing: Đang nấu/chuẩn bị
    note right of delivering: Đang giao
    note right of delivered: Hoàn thành
    note right of cancelled: Đã hủy
```