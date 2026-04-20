-- Run this SQL in your Supabase SQL editor.
-- This sets up profile records, admin controls, and secure admin-only monitoring access.

begin;

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    email text unique not null,
    full_name text,
    role text not null default 'landlord' check (role in ('landlord', 'admin')),
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    last_sign_in_at timestamptz
);

create or replace function public.handle_profile_timestamps()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists trg_profiles_updated_at on public.profiles;
create trigger trg_profiles_updated_at
before update on public.profiles
for each row
execute function public.handle_profile_timestamps();

create or replace function public.handle_new_user_profile()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    insert into public.profiles (id, email, full_name, role, last_sign_in_at)
    values (
        new.id,
        new.email,
        coalesce(new.raw_user_meta_data ->> 'full_name', split_part(new.email, '@', 1)),
        case
            when lower(new.email) = 'rushil.reddycode@gmail.com' then 'admin'
            else 'landlord'
        end,
        new.last_sign_in_at
    )
    on conflict (id) do update
    set
        email = excluded.email,
        full_name = coalesce(excluded.full_name, public.profiles.full_name),
        role = case
            when lower(excluded.email) = 'rushil.reddycode@gmail.com' then 'admin'
            else public.profiles.role
        end,
        last_sign_in_at = excluded.last_sign_in_at,
        updated_at = now();

    return new;
end;
$$;

drop trigger if exists on_auth_user_created_profile on auth.users;
create trigger on_auth_user_created_profile
after insert on auth.users
for each row
execute function public.handle_new_user_profile();

create or replace function public.sync_profile_signin()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    update public.profiles
    set
        email = new.email,
        last_sign_in_at = new.last_sign_in_at,
        updated_at = now()
    where id = new.id;

    return new;
end;
$$;

drop trigger if exists on_auth_user_updated_profile on auth.users;
create trigger on_auth_user_updated_profile
after update of email, last_sign_in_at on auth.users
for each row
execute function public.sync_profile_signin();

-- Backfill existing users
insert into public.profiles (id, email, full_name, role, last_sign_in_at)
select
    u.id,
    u.email,
    coalesce(u.raw_user_meta_data ->> 'full_name', split_part(u.email, '@', 1)),
    case
        when lower(u.email) = 'rushil.reddycode@gmail.com' then 'admin'
        else 'landlord'
    end,
    u.last_sign_in_at
from auth.users u
on conflict (id) do update
set
    email = excluded.email,
    full_name = coalesce(excluded.full_name, public.profiles.full_name),
    role = case
        when lower(excluded.email) = 'rushil.reddycode@gmail.com' then 'admin'
        else public.profiles.role
    end,
    last_sign_in_at = excluded.last_sign_in_at,
    updated_at = now();

alter table public.profiles enable row level security;

-- Users can view their own profile.
drop policy if exists profiles_select_own on public.profiles;
create policy profiles_select_own
on public.profiles
for select
using (auth.uid() = id);

-- Admin can view all profiles.
drop policy if exists profiles_select_admin on public.profiles;
create policy profiles_select_admin
on public.profiles
for select
using (
    exists (
        select 1
        from public.profiles p
        where p.id = auth.uid()
          and p.role = 'admin'
          and p.is_active = true
    )
);

-- Users can update their own safe preferences.
drop policy if exists profiles_update_own on public.profiles;
create policy profiles_update_own
on public.profiles
for update
using (auth.uid() = id)
with check (auth.uid() = id);

-- Admin can update all profiles (role, active state, metadata).
drop policy if exists profiles_update_admin on public.profiles;
create policy profiles_update_admin
on public.profiles
for update
using (
    exists (
        select 1
        from public.profiles p
        where p.id = auth.uid()
          and p.role = 'admin'
          and p.is_active = true
    )
)
with check (
    exists (
        select 1
        from public.profiles p
        where p.id = auth.uid()
          and p.role = 'admin'
          and p.is_active = true
    )
);

-- Keep insert/delete locked to backend logic.
drop policy if exists profiles_no_insert on public.profiles;
create policy profiles_no_insert
on public.profiles
for insert
with check (false);

drop policy if exists profiles_no_delete on public.profiles;
create policy profiles_no_delete
on public.profiles
for delete
using (false);

commit;
