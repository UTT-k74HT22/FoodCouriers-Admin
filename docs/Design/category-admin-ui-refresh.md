# Category Module UI Refresh (Admin App)

> Alignment: Material 3 guidelines + existing Admin design system (`#FF6B35` primary, compact flat surfaces). Focus on clarity, bilingual copy (EN/VI), and touch-friendly admin flows.

## 1. Visual Direction

- **Style**: Compact minimal, 8dp spacing grid, 6dp radii, subtle 8% shadow via `MaterialShapeDrawable`.
- **Palette tokens**:
  - `colorPrimary = #FF6B35`, `onPrimary = #FFFFFF`
  - `colorSecondary = #00C2A8` for secondary chip/filters.
  - `surface = #FFFFFF`, `surfaceVariant = #F4F6F8`, `onSurface = #1C1C1C`
  - Status badges: Active `#22C55E`, Inactive `#6B7280`, Draft `#F59E0B`
- **Typography** (Material roles):
  - Display/Screen title: `TitleLarge` (20sp, 600)
  - Section label: `TitleSmall` (14sp, 600)
  - Body copy: `BodyMedium` (14sp, 400)
  - Caption/helper: `LabelSmall` (12sp, 500)
- **Icons**: Use outlined 2px stroke (Material Symbols Outlined) for consistency; min size 24dp with hitSlop 12dp.

## 2. Category List Screen (`CategoryFragment`)

### Layout

1. **Top App Bar** (collapsing): Title "Danh mục" + subtitle "Category Manager"; right actions: search, overflow (sync, export).
2. **Filter Row**: SearchView (full width) + horizontal `ChipGroup` (All, Active, Hidden). Chips use secondary color border, filled on selection.
3. **RecyclerView** cards:
   - Container `materialCardView` w/ padding 16dp, spacing 12dp.
   - Leading avatar: 48dp circle image; fallback gradient background with initials.
   - Title row: Category name (bold) + badge (Active/Hidden) aligned end.
   - Subline: `description` truncated to 2 lines + metadata row (Sort order pill, Items count, Updated at icon).
   - Trailing actions: icon buttons (edit, more) anchored to card top-right.
4. **Floating Action Button**: Extended FAB bottom-right `+ Danh mục mới` (text + icon). Color = primary, elevation 3.
5. **State overlays**:
   - Loading skeleton: shimmer placeholders (avatar circle, 2 text bars) repeated 5 items.
   - Empty state view: illustration icon, text "Chưa có danh mục" + CTA button.
   - Error state: red icon, message, `Thử lại` button.

### Interaction & Motion

- Pull-to-refresh using `SwipeRefreshLayout` (indicator tinted primary).
- Pagination: show inline progress row after 3rd page load; animate fade-in/out 200ms.
- Card press feedback: opacity 0.92 + elevation increase from 1dp → 3dp (150ms).
- Search debounce 350ms before repository call; show inline loading bar below top app bar.
- Long-press on card opens bottom sheet quick actions (View items, Duplicate, Disable).

### Accessibility

- Ensure each card has `contentDescription` summarizing status ("Danh mục Món chính, 12 món, đang hoạt động").
- Focus order: App bar → Search → ChipGroup → List → FAB → Drawer.
- Contrast check: text vs background ≥ 4.5:1; badges use white text when background < #888888.

## 3. Category Form Dialog

### Layout Structure (`dialog_category_form.xml`)

- **Container**: Modal sheet anchored center; width 92% screen, max 480dp.
- **Header**: Title + optional subtitle (e.g., "Thêm danh mục mới" / "Edit category"); close icon 24dp top-right.
- **Form Body** (ScrollView):
  1. Image picker card (120×120) with dashed border (secondary), overlay "Tải ảnh" icon button. Show progress arc when uploading.
  2. `TextInputLayout` fields (outlined style) following order: Name, Description (multi-line), Sort order (number), Status switch, Display priority slider (0–5, discrete).
  3. Helper text bilingual: e.g., "Tên / Name" label, helper "Hiển thị cho khách / Visible to customers".
- **Footer**: Primary button spans full width (`Lưu / Save`), secondary `Huỷ` text button left-aligned.

### UX Rules

- Validate on blur; show inline message below field (`LabelSmall`, error color `#EF4444`).
- Disable Save until mandatory fields valid + upload done; once saving, button shows progress + text "Đang lưu…".
- Confirm dismissal if dirty (`MaterialAlertDialog` confirmation, bilingual copy).
- Provide realtime preview of how card will appear (mini card component below form, auto-updating with inputs).

## 4. Microcopy (Bilingual)

- Titles: `Danh mục` / `Categories`.
- Buttons: `Lưu / Save`, `Huỷ / Cancel`, `Thêm danh mục` / `Add category`.
- Empty state: "Chưa có danh mục nào" + subtitle "Nhấn + để tạo danh mục đầu tiên".
- Toasts: Use short bilingual combos ("Tạo thành công / Created successfully").

## 5. Component Tokens & Values

| Token | Value | Notes |
|-------|-------|-------|
| `spacing-xs` | 4dp | chip padding, icon gaps |
| `spacing-sm` | 8dp | between labels & helper |
| `spacing-md` | 12dp | card inner padding |
| `spacing-lg` | 16dp | section spacing |
| `radius-sm` | 6dp | inputs, chips |
| `radius-md` | 10dp | cards, modals |
| `elevation-rest` | 1dp | default cards |
| `elevation-hover` | 3dp | pressed state |
| `lineHeight-body` | 1.5 | readability |

## 6. Iconography & Illustration

- Use `category` icon for empty state; `image` icon for picker.
- Badges leverage icons: checkmark for active, eye-off for hidden, sparkles for featured.
- For fallback avatars, generate gradient backgrounds using primary + secondary at 60% blend; overlay first 2 letters uppercase.

## 7. Motion Specs

- Chip selection: state layer fade 120ms, color transition 180ms.
- Dialog entrance: scale 0.95 → 1 with alpha 0 → 1 over 200ms (Decelerate), exit 120ms.
- Skeleton shimmer duration 1.2s using linear gradient TranslateX.
- Bottom sheet quick actions slide-up 220ms and use 24dp corner radius.

## 8. QA Checklist (UI)

- [ ] All touch targets ≥48dp and maintain 8dp spacing.
- [ ] Screen supports Dynamic Type up to 130% without clipping chips or FAB.
- [ ] Tested TalkBack labels for cards, badges, FAB, dialog fields.
- [ ] Contrast passes for text/badges in both light & future dark theme.
- [ ] Loading/empty/error states verified with dark text on light surfaces.
- [ ] Offline mode: show inline banner "Mất kết nối" with retry.

## 9. Implementation Notes

- Use `MaterialComponents` theme overlays: `ThemeOverlay.Material3.Surface` for cards, `ThemeOverlay.Material3.MaterialAlertDialog` for form modal.
- RecyclerView item layout: `ConstraintLayout` for precise alignment; adopt `MotionScene` if future shared transitions needed.
- Add style entries in `themes.xml` / `styles.xml`: `Widget.Admin.CategoryChip`, `Widget.Admin.StateBadge`, `Widget.Admin.Dialog` referencing tokens above.
- Encapsulate color tokens under `colorCategory*` for easier dark-mode port.
