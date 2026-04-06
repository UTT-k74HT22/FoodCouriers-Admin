---
title: "Category Management - Implementation Plan"
description: "Kế hoạch triển khai module quản lý danh mục (Category)"
audience: [ai-agents, developers]
tags: [implementation, category, admin]
created: 2026-04-06
status: draft
---

# Category Management - Implementation Plan

## Executive Summary

Triển khai module quản lý danh mục món ăn (Category) cho hệ thống Food Ordering. Bao gồm Admin CRUD và Client API read-only.

## Phase 1: Database & Supabase

### Tasks

| # | Task | Module | Type | Estimate |
|---|------|--------|------|----------|
| 1.1 | Kiểm tra categories table trong schema | Supabase | verify | 0.5d |
| 1.2 | Tạo RLS policies cho categories | Supabase | feature | 0.5d |
| 1.3 | Tạo storage bucket cho category images | Supabase | feature | 0.5d |
| 1.4 | Tạo seed data categories (nếu chưa có) | Supabase | feature | 0.5d |

### Subtasks

- [ ] 1.1.1 Verify categories table exists với fields: id, name, image_url, sort_order, is_active, created_at
- [ ] 1.2.1 RLS: Admin full access (select, insert, update, delete)
- [ ] 1.2.2 RLS: Client chỉ select where is_active = true
- [ ] 1.3.1 Tạo bucket "category-images" với public read
- [ ] 1.4.1 Insert categories mẫu: Món chính, Món phụ, Đồ uống, Tráng miệng

---

## Phase 2: Admin App - API Service

### Tasks

| # | Task | Module | Type | Estimate |
|---|------|--------|------|----------|
| 2.1 | Tạo CategoryApiService class | Admin | feature | 1d |
| 2.2 | Implement getCategories() with pagination | Admin | feature | 0.5d |
| 2.3 | Implement createCategory() | Admin | feature | 0.5d |
| 2.4 | Implement updateCategory() | Admin | feature | 0.5d |
| 2.5 | Implement deleteCategory() | Admin | feature | 0.5d |
| 2.6 | Implement uploadImage() | Admin | feature | 0.5d |

### Subtasks

- [ ] 2.1.1 Tạo CategoryApiService.java trong package api
- [ ] 2.1.2 Tạo CategoryResponse model class
- [ ] 2.2.1 API: GET /rest/v1/categories?select=*&order=sort_order.asc&limit=10&offset=0
- [ ] 2.3.1 API: POST /rest/v1/categories
- [ ] 2.4.1 API: PATCH /rest/v1/categories?id=eq.{id}
- [ ] 2.5.1 API: DELETE /rest/v1/categories?id=eq.{id}
- [ ] 2.6.1 API: POST /storage/v1/object/category-images/{filename}

---

## Phase 3: Admin App - UI Screens

### Tasks

| # | Task | Module | Type | Estimate |
|---|------|--------|------|----------|
| 3.1 | Tạo CategoryListFragment | Admin | feature | 1d |
| 3.2 | Tạo CategoryAdapter với RecyclerView | Admin | feature | 1d |
| 3.3 | Tạo CategoryFormDialog cho create/edit | Admin | feature | 1d |
| 3.4 | Thêm Category menu vào Navigation | Admin | feature | 0.5d |
| 3.5 | Implement search/filter categories | Admin | feature | 0.5d |
| 3.6 | Implement pagination load more | Admin | feature | 0.5d |

### Subtasks

- [ ] 3.1.1 Tạo layout category_list.xml với RecyclerView, SearchView, FAB
- [ ] 3.1.2 Load categories on fragment created
- [ ] 3.2.1 CategoryAdapter với ViewHolder: image, name, sort_order, item_count, active toggle
- [ ] 3.2.2 SwipeRefreshLayout cho pull-to-refresh
- [ ] 3.3.1 CategoryFormDialog: AlertDialog với form fields
- [ ] 3.3.2 Image picker sử dụng ActivityResultContracts
- [ ] 3.3.3 Validation: tên không trống, không trùng
- [ ] 3.4.1 Thêm menu item "Danh mục" vào navigation drawer
- [ ] 3.5.1 SearchView filter theo tên (client-side hoặc API)
- [ ] 3.6.1 Endless scroll hoặc "Load more" button

---

## Phase 4: Client App - API & UI

### Tasks

| # | Task | Module | Type | Estimate |
|---|------|--------|------|----------|
| 4.1 | Tạo CategoryApiService trong Client | Client | feature | 0.5d |
| 4.2 | Tạo CategoryRepository | Client | feature | 0.5d |
| 4.3 | Tích hợp vào Home/Menu screen | Client | feature | 1d |

### Subtasks

- [ ] 4.1.1 Client API: GET /rest/v1/categories?select=*&is_active=eq.true&order=sort_order.asc
- [ ] 4.2.1 CategoryRepository với Single source of truth
- [ ] 4.3.1 Hiển thị categories trong HomeScreen (horizontal scroll hoặc grid)
- [ ] 4.3.2 Khi chọn category → hiển thị menu items thuộc category đó

---

## Dependencies

- Phase 1 phải hoàn thành trước Phase 2, 3
- Phase 2 hoàn thành trước Phase 3
- Phase 4 có thể chạy song song với Phase 3

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Image upload fail | Medium | Show error, allow retry |
| Delete category with items | Medium | Show warning, suggest move items |
| RLS config incorrect | High | Test thoroughly before demo |