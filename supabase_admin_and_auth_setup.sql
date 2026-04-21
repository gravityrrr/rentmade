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

-- ===== Property Units + Rich Tenant Fields =====
create table if not exists public.units (
    id uuid primary key default gen_random_uuid(),
    property_id uuid not null references public.properties(id) on delete cascade,
    door_number text not null,
    floor_label text,
    bedroom_count int not null default 1,
    fan_count int not null default 0,
    geyser_count int not null default 0,
    notes text,
    tenant_id uuid references public.tenants(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (property_id, door_number)
);

drop trigger if exists trg_units_updated_at on public.units;
create trigger trg_units_updated_at
before update on public.units
for each row
execute function public.handle_profile_timestamps();

alter table if exists public.tenants
    add column if not exists unit_id uuid references public.units(id) on delete set null,
    add column if not exists avatar_url text,
    add column if not exists water_bill numeric not null default 0,
    add column if not exists electricity_bill numeric not null default 0,
    add column if not exists trash_bill numeric not null default 0,
    add column if not exists aadhar_number text,
    add column if not exists aadhar_url text,
    add column if not exists aadhar_path text,
    add column if not exists lease_agreement_path text,
    add column if not exists lease_agreement_url text;

alter table if exists public.properties
    add column if not exists image_url text;

-- Grace period + automation settings per landlord
create table if not exists public.landlord_settings (
    landlord_id uuid primary key references auth.users(id) on delete cascade,
    grace_days int not null default 3 check (grace_days >= 0 and grace_days <= 30),
    auto_overdue_enabled boolean not null default true,
    reminder_window_days int not null default 3 check (reminder_window_days between 0 and 14),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

drop trigger if exists trg_landlord_settings_updated_at on public.landlord_settings;
create trigger trg_landlord_settings_updated_at
before update on public.landlord_settings
for each row
execute function public.handle_profile_timestamps();

alter table public.landlord_settings enable row level security;

drop policy if exists landlord_settings_select_own on public.landlord_settings;
create policy landlord_settings_select_own
on public.landlord_settings
for select
using (auth.uid() = landlord_id);

drop policy if exists landlord_settings_insert_own on public.landlord_settings;
create policy landlord_settings_insert_own
on public.landlord_settings
for insert
with check (auth.uid() = landlord_id);

drop policy if exists landlord_settings_update_own on public.landlord_settings;
create policy landlord_settings_update_own
on public.landlord_settings
for update
using (auth.uid() = landlord_id)
with check (auth.uid() = landlord_id);

-- Unit occupancy timeline
create table if not exists public.unit_occupancy_history (
    id uuid primary key default gen_random_uuid(),
    unit_id uuid not null references public.units(id) on delete cascade,
    tenant_id uuid not null references public.tenants(id) on delete cascade,
    move_in_date date not null default current_date,
    move_out_date date,
    occupancy_status text not null default 'active' check (occupancy_status in ('active', 'moved_out')),
    notes text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

drop trigger if exists trg_unit_occupancy_history_updated_at on public.unit_occupancy_history;
create trigger trg_unit_occupancy_history_updated_at
before update on public.unit_occupancy_history
for each row
execute function public.handle_profile_timestamps();

create index if not exists idx_occupancy_unit on public.unit_occupancy_history(unit_id, move_in_date desc);
create index if not exists idx_occupancy_tenant on public.unit_occupancy_history(tenant_id, move_in_date desc);

alter table public.unit_occupancy_history enable row level security;

drop policy if exists occupancy_select_owner on public.unit_occupancy_history;
create policy occupancy_select_owner
on public.unit_occupancy_history
for select
using (
    exists (
        select 1
        from public.units u
        join public.properties p on p.id = u.property_id
        where u.id = unit_occupancy_history.unit_id
          and p.landlord_id = auth.uid()
    )
);

drop policy if exists occupancy_insert_owner on public.unit_occupancy_history;
create policy occupancy_insert_owner
on public.unit_occupancy_history
for insert
with check (
    exists (
        select 1
        from public.units u
        join public.properties p on p.id = u.property_id
        where u.id = unit_occupancy_history.unit_id
          and p.landlord_id = auth.uid()
    )
);

drop policy if exists occupancy_update_owner on public.unit_occupancy_history;
create policy occupancy_update_owner
on public.unit_occupancy_history
for update
using (
    exists (
        select 1
        from public.units u
        join public.properties p on p.id = u.property_id
        where u.id = unit_occupancy_history.unit_id
          and p.landlord_id = auth.uid()
    )
)
with check (
    exists (
        select 1
        from public.units u
        join public.properties p on p.id = u.property_id
        where u.id = unit_occupancy_history.unit_id
          and p.landlord_id = auth.uid()
    )
);

-- Auto-manage occupancy timeline when tenant unit changes
create or replace function public.sync_unit_occupancy_history()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    if tg_op = 'INSERT' and new.unit_id is not null then
        insert into public.unit_occupancy_history (unit_id, tenant_id, move_in_date, occupancy_status)
        values (new.unit_id, new.id, current_date, 'active')
        on conflict do nothing;
    elsif tg_op = 'UPDATE' then
        if old.unit_id is distinct from new.unit_id then
            update public.unit_occupancy_history
            set move_out_date = current_date,
                occupancy_status = 'moved_out',
                updated_at = now()
            where tenant_id = new.id
              and occupancy_status = 'active';

            if new.unit_id is not null then
                insert into public.unit_occupancy_history (unit_id, tenant_id, move_in_date, occupancy_status)
                values (new.unit_id, new.id, current_date, 'active');
            end if;
        end if;
    end if;
    return new;
end;
$$;

drop trigger if exists trg_tenant_occupancy_sync on public.tenants;
create trigger trg_tenant_occupancy_sync
after insert or update of unit_id on public.tenants
for each row
execute function public.sync_unit_occupancy_history();

-- Bill history ledger (monthly split)
create table if not exists public.tenant_bill_ledger (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references public.tenants(id) on delete cascade,
    property_id uuid not null references public.properties(id) on delete cascade,
    unit_id uuid references public.units(id) on delete set null,
    period_month date not null,
    due_date date not null,
    rent_amount numeric not null default 0,
    water_amount numeric not null default 0,
    electricity_amount numeric not null default 0,
    trash_amount numeric not null default 0,
    total_amount numeric not null default 0,
    status text not null default 'pending' check (status in ('pending', 'overdue', 'paid')),
    paid_on date,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (tenant_id, period_month)
);

drop trigger if exists trg_tenant_bill_ledger_updated_at on public.tenant_bill_ledger;
create trigger trg_tenant_bill_ledger_updated_at
before update on public.tenant_bill_ledger
for each row
execute function public.handle_profile_timestamps();

alter table public.tenant_bill_ledger enable row level security;

drop policy if exists ledger_select_owner on public.tenant_bill_ledger;
create policy ledger_select_owner
on public.tenant_bill_ledger
for select
using (
    exists (
        select 1 from public.properties p
        where p.id = tenant_bill_ledger.property_id
          and p.landlord_id = auth.uid()
    )
);

drop policy if exists ledger_insert_owner on public.tenant_bill_ledger;
create policy ledger_insert_owner
on public.tenant_bill_ledger
for insert
with check (
    exists (
        select 1 from public.properties p
        where p.id = tenant_bill_ledger.property_id
          and p.landlord_id = auth.uid()
    )
);

drop policy if exists ledger_update_owner on public.tenant_bill_ledger;
create policy ledger_update_owner
on public.tenant_bill_ledger
for update
using (
    exists (
        select 1 from public.properties p
        where p.id = tenant_bill_ledger.property_id
          and p.landlord_id = auth.uid()
    )
)
with check (
    exists (
        select 1 from public.properties p
        where p.id = tenant_bill_ledger.property_id
          and p.landlord_id = auth.uid()
    )
);

-- Extend payments table with bill split fields if it already exists
alter table if exists public.payments
    add column if not exists property_id uuid references public.properties(id) on delete set null,
    add column if not exists unit_id uuid references public.units(id) on delete set null,
    add column if not exists rent_amount numeric not null default 0,
    add column if not exists water_amount numeric not null default 0,
    add column if not exists electricity_amount numeric not null default 0,
    add column if not exists trash_amount numeric not null default 0,
    add column if not exists notes text;

-- Overdue sync function using grace_days and supports YYYY-MM-DD or day-of-month due_date formats
create or replace function public.sync_overdue_statuses(p_landlord_id uuid, p_today date default current_date)
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
    v_updated int := 0;
begin
    with cfg as (
        select coalesce((
            select ls.grace_days
            from public.landlord_settings ls
            where ls.landlord_id = p_landlord_id
            limit 1
        ), 3) as grace_days
    ),
    to_mark as (
        select t.id
        from public.tenants t
        join public.properties p on p.id = t.property_id
        cross join cfg
        where p.landlord_id = p_landlord_id
          and t.payment_status <> 'paid'
          and (
            (
                t.due_date ~ '^\\d{4}-\\d{2}-\\d{2}$'
                and (t.due_date::date + cfg.grace_days) < p_today
            )
            or
            (
                t.due_date ~ '^\\d{1,2}$'
                and ((date_trunc('month', p_today)::date + (least(greatest(t.due_date::int, 1), 28) - 1)) + cfg.grace_days) < p_today
            )
          )
    )
    update public.tenants t
    set payment_status = 'overdue'
    from to_mark
    where t.id = to_mark.id;

    get diagnostics v_updated = row_count;
    return v_updated;
end;
$$;

-- Aggregate sync across all landlords who have auto-overdue enabled (or no settings row yet).
create or replace function public.sync_overdue_statuses_all(p_today date default current_date)
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
    v_total int := 0;
    v_landlord_id uuid;
begin
    for v_landlord_id in
        select distinct p.landlord_id
        from public.properties p
        left join public.landlord_settings ls on ls.landlord_id = p.landlord_id
        where coalesce(ls.auto_overdue_enabled, true) = true
    loop
        v_total := v_total + public.sync_overdue_statuses(v_landlord_id, p_today);
    end loop;
    return v_total;
end;
$$;

-- True backend automation (runs daily at 01:10 UTC) even if the app is closed.
create extension if not exists pg_cron with schema extensions;

select cron.unschedule(jobid)
from cron.job
where jobname = 'sync-overdue-daily';

select cron.schedule(
    'sync-overdue-daily',
    '10 1 * * *',
    'select public.sync_overdue_statuses_all(current_date);'
);

-- Secure document vault (private storage bucket + signed URL usage in app)
insert into storage.buckets (id, name, public)
values ('tenant-documents', 'tenant-documents', false)
on conflict (id) do nothing;

insert into storage.buckets (id, name, public)
values ('tenant-images', 'tenant-images', true)
on conflict (id) do nothing;

insert into storage.buckets (id, name, public)
values ('property-images', 'property-images', true)
on conflict (id) do nothing;

drop policy if exists tenant_docs_select_own on storage.objects;
create policy tenant_docs_select_own
on storage.objects
for select
to authenticated
using (
    bucket_id = 'tenant-documents'
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists tenant_docs_insert_own on storage.objects;
create policy tenant_docs_insert_own
on storage.objects
for insert
to authenticated
with check (
    bucket_id = 'tenant-documents'
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists tenant_docs_update_own on storage.objects;
create policy tenant_docs_update_own
on storage.objects
for update
to authenticated
using (
    bucket_id = 'tenant-documents'
    and (storage.foldername(name))[1] = auth.uid()::text
)
with check (
    bucket_id = 'tenant-documents'
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists tenant_docs_delete_own on storage.objects;
create policy tenant_docs_delete_own
on storage.objects
for delete
to authenticated
using (
    bucket_id = 'tenant-documents'
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists tenant_images_insert_own on storage.objects;
create policy tenant_images_insert_own
on storage.objects
for insert
to authenticated
with check (
    bucket_id = 'tenant-images'
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists tenant_images_update_own on storage.objects;
create policy tenant_images_update_own
on storage.objects
for update
to authenticated
using (
    bucket_id = 'tenant-images'
    and (storage.foldername(name))[1] = auth.uid()::text
)
with check (
    bucket_id = 'tenant-images'
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists tenant_images_delete_own on storage.objects;
create policy tenant_images_delete_own
on storage.objects
for delete
to authenticated
using (
    bucket_id = 'tenant-images'
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists property_images_insert_own on storage.objects;
create policy property_images_insert_own
on storage.objects
for insert
to authenticated
with check (
    bucket_id = 'property-images'
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists property_images_update_own on storage.objects;
create policy property_images_update_own
on storage.objects
for update
to authenticated
using (
    bucket_id = 'property-images'
    and (storage.foldername(name))[1] = auth.uid()::text
)
with check (
    bucket_id = 'property-images'
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists property_images_delete_own on storage.objects;
create policy property_images_delete_own
on storage.objects
for delete
to authenticated
using (
    bucket_id = 'property-images'
    and (storage.foldername(name))[1] = auth.uid()::text
);

create index if not exists idx_units_property on public.units(property_id);
create index if not exists idx_units_tenant on public.units(tenant_id);
create index if not exists idx_tenants_unit on public.tenants(unit_id);

alter table public.units enable row level security;

drop policy if exists units_select_owner on public.units;
create policy units_select_owner
on public.units
for select
using (
    exists (
        select 1
        from public.properties pr
        where pr.id = units.property_id
          and pr.landlord_id = auth.uid()
    )
);

drop policy if exists units_insert_owner on public.units;
create policy units_insert_owner
on public.units
for insert
with check (
    exists (
        select 1
        from public.properties pr
        where pr.id = units.property_id
          and pr.landlord_id = auth.uid()
    )
);

drop policy if exists units_update_owner on public.units;
create policy units_update_owner
on public.units
for update
using (
    exists (
        select 1
        from public.properties pr
        where pr.id = units.property_id
          and pr.landlord_id = auth.uid()
    )
)
with check (
    exists (
        select 1
        from public.properties pr
        where pr.id = units.property_id
          and pr.landlord_id = auth.uid()
    )
);

drop policy if exists units_delete_owner on public.units;
create policy units_delete_owner
on public.units
for delete
using (
    exists (
        select 1
        from public.properties pr
        where pr.id = units.property_id
          and pr.landlord_id = auth.uid()
    )
);

alter table public.tenants enable row level security;

drop policy if exists tenants_select_owner on public.tenants;
create policy tenants_select_owner
on public.tenants
for select
using (
    exists (
        select 1
        from public.properties pr
        where pr.id = tenants.property_id
          and pr.landlord_id = auth.uid()
    )
);

drop policy if exists tenants_insert_owner on public.tenants;
create policy tenants_insert_owner
on public.tenants
for insert
with check (
    exists (
        select 1
        from public.properties pr
        where pr.id = tenants.property_id
          and pr.landlord_id = auth.uid()
    )
);

drop policy if exists tenants_update_owner on public.tenants;
create policy tenants_update_owner
on public.tenants
for update
using (
    exists (
        select 1
        from public.properties pr
        where pr.id = tenants.property_id
          and pr.landlord_id = auth.uid()
    )
)
with check (
    exists (
        select 1
        from public.properties pr
        where pr.id = tenants.property_id
          and pr.landlord_id = auth.uid()
    )
);

drop policy if exists tenants_delete_owner on public.tenants;
create policy tenants_delete_owner
on public.tenants
for delete
using (
    exists (
        select 1
        from public.properties pr
        where pr.id = tenants.property_id
          and pr.landlord_id = auth.uid()
    )
);

commit;
