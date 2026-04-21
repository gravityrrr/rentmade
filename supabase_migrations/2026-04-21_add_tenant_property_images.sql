-- Incremental migration: tenant/property image support
-- Run this file once in Supabase SQL editor.

begin;

-- Add new columns used by app payloads.
alter table if exists public.tenants
    add column if not exists avatar_url text;

alter table if exists public.properties
    add column if not exists image_url text;

-- Create public image buckets if missing.
insert into storage.buckets (id, name, public)
values ('tenant-images', 'tenant-images', true)
on conflict (id) do nothing;

insert into storage.buckets (id, name, public)
values ('property-images', 'property-images', true)
on conflict (id) do nothing;

-- Tenant images policies (owner folder only).
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

-- Property images policies (owner folder only).
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

commit;
