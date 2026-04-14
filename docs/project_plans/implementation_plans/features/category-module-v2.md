---
title: "Category Module Completion - Implementation Plan"
description: "Chi tiết các bước để hoàn thiện module Category trên Android Admin app"
audience: [ai-agents, developers]
tags: [implementation, category, admin]
created: 2026-04-06
status: draft
---

# Category Module Completion - Implementation Plan

## Executive Summary

Hoàn thiện toàn bộ luồng Category trong Admin app: đồng bộ schema Supabase, repository chuẩn hóa, API services, màn hình UI (list + form) với trải nghiệm đẹp, đảm bảo kiểm thử và tài liệu hóa.

## Phase 1: Supabase & Contract Validation

### Tasks

| # | Task | Module | Type | Estimate |
|---|------|--------|------|----------|
| 1.1 | Audit bảng `categories` và seed hiện tại | Supabase | verify | 0.5d |
| 1.2 | Chuẩn hóa RLS + policies cho Admin/Client | Supabase | feature | 0.5d |
| 1.3 | Định nghĩa contract JSON → DTO (Docs) | Docs | feature | 0.5d |
| 1.4 | Tạo checklist test API (Postman/curl) | Supabase | verify | 0.25d |

### Subtasks

- [ ] 1.1.1 So khớp schema với `docs/technical_design/category-module.md`
- [ ] 1.1.2 Bổ sung cột `updated_at`, `description`, `image_path` nếu thiếu
- [ ] 1.2.1 Viết policy: role `service_role` full CRUD, `anon` select `is_active=true`
- [ ] 1.2.2 Bật storage bucket `category-images` với signed URLs
- [ ] 1.3.1 Update `docs/project_plans/PRDs/features/category-management-v1.md` với contract mới
- [ ] 1.4.1 Lưu sample payload (list/create/update) vào `docs/api_contracts/categories.md`

---

## Phase 2: Data Layer & Repositories

### Tasks

| # | Task | Module | Type | Estimate |
|---|------|--------|------|----------|
| 2.1 | Chuẩn hóa `CategoryRepository` extends `BaseSupabaseRepository` | Admin | refactor | 0.75d |
| 2.2 | Tạo DTO/Request models (list + upsert) | Admin | feature | 0.5d |
| 2.3 | Implement `CategoryRemoteDataSource` (optional layer) | Admin | feature | 0.5d |
| 2.4 | Viết unit test cho mapping & error handling | Admin | test | 0.5d |

### Subtasks

- [ ] 2.1.1 Refactor `data/repository/CategoryRepository.java` sử dụng `fetchList`, `createItem`, `updateItem`, `deleteItem`
- [ ] 2.1.2 Đảm bảo callback trả `BaseResponse<List<Category>>`
- [ ] 2.2.1 Thêm `data/model/category/CategoryDto.java` & `CategoryUpsertRequest.java`
- [ ] 2.2.2 Map snake_case qua `@SerializedName`
- [ ] 2.3.1 (Nếu cần) thêm lớp datasource để gom API logic khỏi repository
- [ ] 2.4.1 Unit test tại `app/src/test/.../CategoryRepositoryTest.java`

---

## Phase 3: UI/UX - List & Interactions

### Tasks

| # | Task | Module | Type | Estimate |
|---|------|--------|------|----------|
| 3.1 | Hoàn thiện `CategoryFragment` logic load/paginate | Admin | feature | 0.75d |
| 3.2 | Tạo `CategoryAdapter` (RecyclerView + DiffUtil) | Admin | feature | 0.75d |
| 3.3 | Thiết kế layout `fragment_category.xml` (search, filter, FAB) | Admin | design | 0.5d |
| 3.4 | State handling (loading/empty/error) | Admin | feature | 0.5d |
| 3.5 | Integrate image preview & status badges | Admin | design | 0.5d |

### Subtasks

- [ ] 3.1.1 Di chuyển logic khỏi `MenuItemFragment` direct API → repository calls
- [ ] 3.1.2 Sử dụng `SwipeRefreshLayout` + `RecyclerView.OnScrollListener` để load thêm
- [ ] 3.2.1 ViewHolder gồm: hình tròn, tên, mô tả, badge trạng thái, menu overflow
- [ ] 3.3.1 Layout dùng Material 3 components + custom chip filter
- [ ] 3.4.1 Hiển thị view placeholder trong `include/state_view_category.xml`
- [ ] 3.5.1 Dùng `MaterialShapeDrawable` cho card + `Coil/Glide` cho ảnh

---

## Phase 4: UI/UX - Create/Edit Form

### Tasks

| # | Task | Module | Type | Estimate |
|---|------|--------|------|----------|
| 4.1 | Thiết kế `dialog_category_form.xml` | Admin | design | 0.5d |
| 4.2 | Tạo `CategoryFormDialogFragment` với validation | Admin | feature | 0.75d |
| 4.3 | Image picker + upload pipeline | Admin | feature | 0.5d |
| 4.4 | Status toggle & sort order controls | Admin | feature | 0.25d |

### Subtasks

- [ ] 4.1.1 Form fields: name, description, sort_order, active switch, image preview
- [ ] 4.2.1 Validate: name unique (check API), sort_order >=0
- [ ] 4.2.2 Dùng `TextInputLayout` + helper text song ngữ EN/VI
- [ ] 4.3.1 Tích hợp `ActivityResultContracts.GetContent`
- [ ] 4.3.2 Upload ảnh → lấy URL → bind vào request
- [ ] 4.4.1 Support drag-to-reorder? (Optional backlog)

---

## Phase 5: Quality, Docs & Release

### Tasks

| # | Task | Module | Type | Estimate |
|---|------|--------|------|----------|
| 5.1 | Viết UI test (Espresso) cho list & form | Admin | test | 1d |
| 5.2 | Manual QA checklist (VN/EN) | QA | verify | 0.5d |
| 5.3 | Cập nhật screenshots & changelog | Docs | chore | 0.25d |
| 5.4 | Review accessibility (talkback, contrast) | Admin | verify | 0.5d |

### Subtasks

- [ ] 5.1.1 Test create/update/delete happy path
- [ ] 5.1.2 Test validation errors hiển thị đúng copy
- [ ] 5.2.1 Checklist trong `docs/qa/category-module.md`
- [ ] 5.3.1 Screenshot mới cho README + store nội bộ
- [ ] 5.4.1 Đảm bảo hit target ≥ 48dp & contrast ≥ 4.5

---

## Dependencies

- Phase 1 hoàn tất trước khi làm Phase 2 trở đi
- Phase 2 kết thúc (repository ready) trước Phase 3/4 để tránh duplicate logic
- Phase 3 & 4 có thể chạy song song nhưng cần thống nhất component/share ViewModel
- Phase 5 yêu cầu build ổn định từ Phase 4

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Migration Supabase ảnh hưởng dữ liệu đang có | High | Tạo backup + script rollback trước khi deploy |
| Validation name trùng gây nghẽn UI | Medium | Debounce gọi API check, hiển thị lỗi nhẹ |
| Upload ảnh dung lượng lớn | Medium | Resize client-side trước khi upload |
| UI phức tạp, thiếu guideline | Medium | Tận dụng Material 3 tokens + skill ui-ux review |

## Deliverables

- Source code cập nhật trong `app/src/main/java/com/utt/foodcouriers_admin/*`
- Layout XML mới và component state assets
- Docs: API contract, QA checklist, changelog entry
- Demo video/quay màn hình thao tác CRUD Category
