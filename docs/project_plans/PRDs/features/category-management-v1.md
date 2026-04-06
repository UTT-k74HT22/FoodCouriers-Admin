---
title: "Category Management - PRD"
description: "Admin quản lý danh mục món ăn (categories)"
audience: [ai-agents, developers]
tags: [requirements, category, admin, mvp]
created: 2026-04-06
status: draft
---

# Category Management - PRD

## 1. Overview

Module cho phép Admin quản lý danh mục món ăn (categories) như: Món chính, Món phụ, Đồ uống, Tráng miệng. Mỗi category có thể chứa nhiều menu items.

## 2. Problem Statement

- Admin cần thêm/sửa/xóa categories để tổ chức menu
- Client app hiển thị categories để user duyệt món
- Categories cần có thứ tự hiển thị (sort_order)
- Cần hỗ trợ image cho category

## 3. User Stories

| ID | Actor | Story | Priority |
|----|-------|-------|----------|
| US-001 | Admin | Tôi muốn tạo category mới với tên, image, và thứ tự hiển thị | Must |
| US-002 | Admin | Tôi muốn xem danh sách tất cả categories có phân trang | Must |
| US-003 | Admin | Tôi muốn sửa thông tin category (tên, image, thứ tự) | Must |
| US-004 | Admin | Tôi muốn xóa category (chỉ xóa được nếu không có menu items) | Must |
| US-005 | Admin | Tôi muốn toggle trạng thái active/inactive của category | Should |
| US-006 | Client | Tôi muốn xem danh sách categories để duyệt món ăn | Must |
| US-007 | Client | Tôi chỉ thấy các categories đang active | Must |

## 4. Functional Requirements

### 4.1 Admin - Category List
- Hiển thị danh sách categories với: name, image, sort_order, item_count, is_active
- Phân trang (10 items/page)
- Sort theo sort_order tăng dần
- Search theo tên category

### 4.2 Admin - Create Category
- Fields:
  - `name` (required, max 100 chars)
  - `image_url` (optional, upload lên Supabase Storage)
  - `sort_order` (optional, default = 0)
- Validation:
  - Tên không được trùng lặp
  - Sort_order >= 0

### 4.3 Admin - Edit Category
- Fields: name, image_url, sort_order, is_active
- Validation: tên không trùng (trừ chính nó)

### 4.4 Admin - Delete Category
- Chỉ xóa được khi không có menu items thuộc category này
- Nếu có items → hiển thị thông báo và redirect sang option "Chuyển sang category khác"

### 4.5 Client - Get Categories
- Chỉ lấy categories where is_active = true
- Sort theo sort_order
- Bao gồm item_count mỗi category

## 5. Non-Functional Requirements

- API response time < 500ms
- Image upload size limit: 2MB
- Supported formats: JPG, PNG, WebP

## 6. Acceptance Criteria

| ID | Criteria | Test Method |
|----|----------|--------------|
| AC-001 | Admin tạo category thành công với đầy đủ fields | Create new category |
| AC-002 | Admin xem được danh sách có phân trang | Navigate pages |
| AC-003 | Admin sửa category thành công | Edit existing |
| AC-004 | Admin xóa category không có items | Delete with no items |
| AC-005 | Admin không xóa được category có items | Try delete with items |
| AC-006 | Client chỉ thấy categories active | Check API response |
| AC-007 | Category hiển thị đúng thứ tự sort_order | Check sorting |

## 7. Dependencies

- Supabase Storage cho image upload
- Supabase Database (categories table đã có trong schema)
- RLS policies cho categories table

## 8. Out of Scope

- Category nesting (sub-categories) - Phase 2
- Category analytics - Phase 2
- Bulk operations - Phase 2