create extension if not exists pgcrypto;

create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  new.updated_at = timezone('utc', now());
  return new;
end;
$$;

create table if not exists public.account (
  id uuid primary key default gen_random_uuid(),
  auth_user_id uuid not null unique references auth.users(id) on delete cascade,
  email text not null unique,
  username text,
  role text not null default 'operator' check (role in ('super_admin', 'manager', 'operator')),
  status text not null default 'active' check (status in ('pending', 'active', 'locked')),
  last_login_at timestamptz,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists public."user" (
  id uuid primary key default gen_random_uuid(),
  account_id uuid not null unique references public.account(id) on delete cascade,
  full_name text,
  avatar_url text,
  phone text,
  job_title text,
  department text,
  note text,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create index if not exists idx_account_auth_user_id on public.account(auth_user_id);
create index if not exists idx_account_role on public.account(role);
create index if not exists idx_user_account_id on public."user"(account_id);

drop trigger if exists trg_account_set_updated_at on public.account;
create trigger trg_account_set_updated_at
before update on public.account
for each row
execute function public.set_updated_at();

drop trigger if exists trg_user_set_updated_at on public."user";
create trigger trg_user_set_updated_at
before update on public."user"
for each row
execute function public.set_updated_at();

alter table public.account enable row level security;
alter table public."user" enable row level security;

drop policy if exists "account_select_own" on public.account;
create policy "account_select_own" on public.account
for select to authenticated
using (auth.uid() = auth_user_id);

drop policy if exists "account_update_own" on public.account;
create policy "account_update_own" on public.account
for update to authenticated
using (auth.uid() = auth_user_id)
with check (auth.uid() = auth_user_id);

drop policy if exists "user_select_own" on public."user";
create policy "user_select_own" on public."user"
for select to authenticated
using (
  exists (
    select 1
    from public.account
    where account.id = "user".account_id
      and account.auth_user_id = auth.uid()
  )
);

drop policy if exists "user_update_own" on public."user";
create policy "user_update_own" on public."user"
for update to authenticated
using (
  exists (
    select 1
    from public.account
    where account.id = "user".account_id
      and account.auth_user_id = auth.uid()
  )
)
with check (
  exists (
    select 1
    from public.account
    where account.id = "user".account_id
      and account.auth_user_id = auth.uid()
  )
);

create or replace function public.handle_admin_auth_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_account_id uuid;
  v_full_name text;
begin
  v_full_name := coalesce(
    new.raw_user_meta_data ->> 'full_name',
    new.raw_user_meta_data ->> 'display_name',
    split_part(new.email, '@', 1)
  );

  insert into public.account (auth_user_id, email, username, role, status)
  values (
    new.id,
    new.email,
    coalesce(new.raw_user_meta_data ->> 'username', split_part(new.email, '@', 1)),
    coalesce(new.raw_user_meta_data ->> 'role', 'operator'),
    coalesce(new.raw_user_meta_data ->> 'status', 'active')
  )
  on conflict (auth_user_id)
  do update set
    email = excluded.email,
    username = coalesce(excluded.username, public.account.username),
    role = excluded.role,
    status = excluded.status,
    updated_at = timezone('utc', now())
  returning id into v_account_id;

  insert into public."user" (account_id, full_name, phone, job_title, department, avatar_url, note)
  values (
    v_account_id,
    v_full_name,
    new.raw_user_meta_data ->> 'phone',
    new.raw_user_meta_data ->> 'job_title',
    new.raw_user_meta_data ->> 'department',
    new.raw_user_meta_data ->> 'avatar_url',
    new.raw_user_meta_data ->> 'note'
  )
  on conflict (account_id)
  do update set
    full_name = excluded.full_name,
    phone = coalesce(excluded.phone, public."user".phone),
    job_title = coalesce(excluded.job_title, public."user".job_title),
    department = coalesce(excluded.department, public."user".department),
    avatar_url = coalesce(excluded.avatar_url, public."user".avatar_url),
    note = coalesce(excluded.note, public."user".note),
    updated_at = timezone('utc', now());

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row
execute function public.handle_admin_auth_user();

create or replace view public.admin_account_profiles as
select
  a.id as account_id,
  a.auth_user_id,
  a.email,
  a.username,
  a.role,
  a.status,
  a.last_login_at,
  u.id as user_id,
  u.full_name,
  u.avatar_url,
  u.phone,
  u.job_title,
  u.department,
  u.note,
  a.created_at,
  a.updated_at
from public.account a
left join public."user" u on u.account_id = a.id;

alter view public.admin_account_profiles set (security_invoker = true);
grant select on public.admin_account_profiles to authenticated;
