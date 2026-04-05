# Roadmap - Food Ordering App MVP

## Phase Overview

| Phase | Name | Weeks | Focus |
|-------|------|-------|-------|
| 1 | Foundation | 1-3 | Setup + Auth + Catalog |
| 2 | Order Flow | 4-6 | Cart + Checkout + Order Mgmt |
| 3 | Engagement | 7-9 | Reviews + Promotions + Notifications |
| 4 | Polish | 10-12 | QA + UAT + Launch Prep |

## Phase 1: Foundation (Weeks 1-3)

### Week 1: Discovery & Scope
- [ ] Finalize MVP scope
- [ ] Define user roles (customer, staff, admin)
- [ ] Define order lifecycle workflow
- [ ] Create assumption log
- [ ] Risk assessment v1

### Week 2: Architecture & Database
- [ ] Solution architecture design
- [ ] Database schema (ERD)
- [ ] RLS policy design
- [ ] Supabase integration contracts
- [ ] Security design

### Week 3: Project Setup
- [ ] Initialize Android projects
- [ ] Setup CI/CD
- [ ] Setup Supabase project
- [ ] Auth skeleton (Client + Admin)
- [ ] Design system base

**Phase 1 Deliverables:**
- Project structure for both apps
- Supabase schema with RLS
- Basic auth flow working
- Architecture document

## Phase 2: Order Flow (Weeks 4-6)

### Week 4: Admin Order Management
- [ ] Order List Screen
- [ ] Order Detail Screen
- [ ] Update Order Status
- [ ] Cancel Order

### Week 5: Supabase RPCs
- [ ] Create Order RPC (rpc_create_order)
- [ ] Apply Promotion RPC (rpc_apply_promotion)
- [ ] Order Status Update RPC

### Week 6: Notifications + Reports
- [ ] In-App Notifications
- [ ] Basic Dashboard Reports
- [ ] Restaurant listing
- [ ] Restaurant detail
- [ ] Menu browsing
- [ ] Search/filter

### Week 6: Cart + Checkout
- [ ] Cart management (Room DB)
- [ ] Add/remove items
- [ ] Checkout flow
- [ ] COD order submission
- [ ] Order success screen
- [ ] rpc_create_order in Supabase

**Phase 2 Deliverables:**
- Client can browse restaurants and menus
- Client can add to cart and checkout
- Orders created in database
- Admin can see orders

## Phase 3: Engagement (Weeks 7-9)

### Week 7: Order Management
- [ ] My Orders list (Client)
- [ ] Order detail screen
- [ ] Order status tracking
- [ ] Order list (Admin)
- [ ] Update order status
- [ ] Status change notifications

### Week 8: Admin Catalog
- [ ] Restaurant CRUD
- [ ] Category CRUD
- [ ] Menu item CRUD
- [ ] Image upload to Storage

### Week 9: Promotions + Reviews
- [ ] Promotion CRUD (Admin)
- [ ] Apply promotion (Client)
- [ ] Banner management
- [ ] Review after delivery
- [ ] Basic reports

**Phase 3 Deliverables:**
- Full order lifecycle
- Admin can manage all catalog items
- Promotions work
- Basic reviews

## Phase 4: Polish (Weeks 10-12)

### Week 10: QA Cycle 1
- [ ] Test case execution
- [ ] Bug fixing
- [ ] Security hardening
- [ ] Performance testing

### Week 11: UAT
- [ ] UAT with seed data
- [ ] Demo preparation
- [ ] Bug fixes
- [ ] Regression testing

### Week 12: Launch Prep
- [ ] Release build
- [ ] APK signing
- [ ] Push notification setup
- [ ] Documentation
- [ ] Runbook creation

**Phase 4 Deliverables:**
- Production-ready APKs
- UAT sign-off
- Launch checklist complete

## Progress Tracking

Check progress in `.planning/progress/[phase-name]/`

## Dependencies

- Phase 1 must complete before Phase 2
- Phase 2 core (cart+checkout) needed for Phase 3
- Phase 3 builds on Phase 2 order management

## Notes

- Parallel development possible: Client browse + Admin order management
- Weekly sync meetings recommended
- Daily standups 15 min
