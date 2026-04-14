# AGENTS.md

## Scope and source of truth
- This guide is for the Android admin app in `app/` (`com.utt.foodcouriers_admin`).
- Treat implemented code in `app/src/main/**` as source of truth over planning docs.
- `app/README.md` contains broader/aspirational architecture; verify against current code before implementing.

## Big picture architecture (current implementation)
- Single Android module (`:app`) using Java + XML + Material components.
- Entry flow: `LoginActivity` -> `MainActivity` with Navigation Drawer (`app/src/main/res/menu/menu_navigation_drawer.xml`).
- Implemented feature slices are partial: auth, dashboard skeleton, category list, menu item list.
- Data flow is mostly UI -> repository/client -> Supabase REST/Auth:
  - Auth: `ui/auth/LoginActivity.java` -> `data/repository/AuthRepository.java` -> `data/remote/SupabaseClient.java`
  - Category CRUD base: `ui/menu/CategoryFragment.java` -> `data/repository/CategoryRepository.java` -> `data/repository/base/BaseSupabaseRepository.java`
  - Menu list currently calls `SupabaseClient` directly in `ui/menu/MenuItemFragment.java` (existing inconsistency)

## Supabase integration boundaries
- Supabase config comes from `BuildConfig` fields populated from `local.properties` in `app/build.gradle.kts`.
- Required keys: `SUPABASE_URL`, `SUPABASE_ANON_KEY` (see `data/remote/SupabaseConfig.java`).
- Network stack is OkHttp + Gson; callbacks are marshaled back to main thread via `Handler`.
- Session tokens live in memory (`SupabaseClient`) and are persisted in `SharedPreferences` via `utils/SessionManager.java`.
- DB/RLS context: `docs/supabase/migrations/003_rls_policies.sql`, `docs/supabase/migrations/005_grant_api_roles.sql`.

## Developer workflow (verified from project files)
- Build debug APK:
```powershell
.\gradlew.bat :app:assembleDebug
```
- Run unit tests:
```powershell
.\gradlew.bat :app:testDebugUnitTest
```
- Run instrumentation tests (device/emulator required):
```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```
- If auth fails unexpectedly, first verify `local.properties` values and `SupabaseConfig.isConfigured()` paths.

## Project-specific patterns to follow
- Repositories return `BaseResponse<T>` via `RepositoryCallback<T>` (`data/common/*`).
- Reuse `BaseSupabaseRepository` for REST table CRUD patterns (`fetchList`, `fetchSingle`, `createItem`, `updateItem`, `deleteItem`).
- Request DTOs use `*UpsertRequest` classes with `@SerializedName` for snake_case columns.
- UI text is mixed EN/VI; preserve existing screen language and avoid broad copy rewrites in feature PRs.
- Many modules are placeholders (`ui/**/a.java`, TODO blocks); prefer extending existing real screens over adding new placeholder classes.

## Guardrails for AI agents
- Do not commit secrets; `local.properties` is gitignored in `.gitignore`.
- Avoid changing package/application IDs or Supabase endpoint composition without migration context.
- When adding a new admin data screen, mirror existing structure: Fragment + Adapter + Repository method + request/model mapping.
- For menu-related code, favor moving toward repository-based access (Category pattern) rather than adding more direct `SupabaseClient` usage from UI.
- Before implementing behavior described only in docs, confirm corresponding classes/files actually exist in `app/src/main`.
