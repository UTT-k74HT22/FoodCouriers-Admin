# Phase 2: Order Flow Plan

**Phase:** 2
**Duration:** Weeks 4-6
**Focus:** Cart + Checkout + Order Management

## Overview

Phase 2 implements the core order flow: cart management, COD checkout, and order status management for Admin.

## Tasks

### Week 4: Admin Order Management

#### Task 4.1: Order List Screen
- **Type:** feature
- **Module:** Admin
- **Estimate:** 2d
- **Description:** Create order list with filtering by status
- **Files:** OrderListActivity, OrderListFragment, OrderAdapter
- **Action:** 
  1. Create OrderListActivity
  2. Create order list layout with RecyclerView
  3. Add filter tabs (All, Pending, Confirmed, Preparing, Delivering, Delivered, Cancelled)
  4. Integrate Supabase query with status filter
  5. Add pull-to-refresh
- **Verify:** Admin can see all orders filtered by status
- **Done:** Order list displays correctly

#### Task 4.2: Order Detail Screen
- **Type:** feature
- **Module:** Admin
- **Estimate:** 1d
- **Description:** View order details with items and status history
- **Files:** OrderDetailActivity, OrderDetailFragment
- **Action:** 
  1. Create OrderDetailActivity
  2. Display order info (customer, address, items, total)
  3. Display order status timeline
  4. Add action buttons based on current status
- **Verify:** Admin can view complete order details
- **Done:** Order detail shows all info

#### Task 4.3: Update Order Status
- **Type:** feature
- **Module:** Admin
- **Estimate:** 2d
- **Description:** Implement status update with validation
- **Files:** UpdateStatusDialog, rpc_update_order_status
- **Action:** 
  1. Create status update dialog
  2. Implement rpc_update_order_status in Supabase
  3. Validate status transitions (cannot go backward)
  4. Add status change reason input
  5. Trigger notification on status change
- **Verify:** Status updates correctly with log
- **Done:** Admin can update order status

#### Task 4.4: Cancel Order
- **Type:** feature
- **Module:** Admin
- **Estimate:** 1d
- **Description:** Allow admin to cancel orders
- **Files:** CancelOrderDialog
- **Action:** 
  1. Create cancel dialog with reason input
  2. Implement cancel logic with validation
  3. Update order status to cancelled
  4. Add cancellation reason to order
- **Verify:** Admin can cancel orders with reason
- **Done:** Cancel order works

### Week 5: Cart + Checkout (Supabase RPC)

#### Task 5.1: Create Order RPC
- **Type:** feature
- **Module:** Supabase
- **Estimate:** 2d
- **Description:** Implement atomic order creation
- **Files:** rpc_create_order function
- **Action:** 
  1. Validate cart items (available, restaurant active)
  2. Validate restaurant is open
  3. Calculate subtotal, delivery fee, discount
  4. Insert order with status 'pending'
  5. Insert order_items (snapshot name/price)
  6. Insert order_status_log
  7. Create notification
  8. Return order result
- **Verify:** Order created atomically with all data
- **Done:** rpc_create_order works

#### Task 5.2: Apply Promotion RPC
- **Type:** feature
- **Module:** Supabase
- **Estimate:** 1d
- **Description:** Validate and apply promotion code
- **Files:** rpc_apply_promotion function
- **Action:** 
  1. Check promotion active
  2. Check date range
  3. Check usage limit
  4. Check minimum order
  5. Calculate discount
  6. Return discount amount and validity
- **Verify:** Promotion applied correctly
- **Done:** rpc_apply_promotion works

#### Task 5.3: Order Status Update RPC
- **Type:** feature
- **Module:** Supabase
- **Estimate:** 1d
- **Description:** Secure status update with validation
- **Files:** rpc_update_order_status function
- **Action:** 
  1. Validate status transition (pending→confirmed→preparing→delivering→delivered)
  2. Check user permissions
  3. Update order status
  4. Insert order_status_log
  5. Create notification for customer
- **Verify:** Status updates correctly with audit
- **Done:** Status update RPC works

### Week 6: Order Notifications + Reports

#### Task 6.1: In-App Notifications
- **Type:** feature
- **Module:** Admin
- **Estimate:** 1d
- **Description:** Admin notification inbox for order updates
- **Files:** NotificationFragment, NotificationAdapter
- **Action:** 
  1. Create notification list
  2. Mark as read functionality
  3. Filter by type
- **Verify:** Notifications display correctly
- **Done:** Admin sees order notifications

#### Task 6.2: Basic Reports
- **Type:** feature
- **Module:** Admin
- **Estimate:** 2d
- **Description:** Basic dashboard with daily stats
- **Files:** DashboardFragment, ReportRepository
- **Action:** 
  1. Create daily stats query (orders today, revenue today)
  2. Create top restaurants query
  3. Create top items query
  4. Create SQL views for reports
- **Verify:** Dashboard shows basic stats
- **Done:** Reports functional

## Dependencies

- Task 5.1 depends on Task 2.1 (Database Schema from Phase 1)
- Task 5.2 depends on Task 2.1
- Task 4.3 depends on Task 5.3
- Task 4.1 → Task 4.2 → Task 4.3 → Task 4.4

## Checkpoints

### Week 4 Checkpoint
- [ ] Admin can view order list
- [ ] Admin can view order details
- [ ] Admin can update status

### Week 5 Checkpoint
- [ ] Create order RPC works
- [ ] Apply promotion RPC works
- [ ] Status update RPC works

### Week 6 Checkpoint
- [ ] Notifications display
- [ ] Basic reports work

## Notes

- Week 4: Admin order management (Client will implement similar flow in Weeks 4-5)
- Week 5: Backend RPCs (shared, coordinate with Client)
- Week 6: Notifications and reports

- This phase shares Supabase RPCs with Client Phase 2
- Sync with Client team on RPC contract
