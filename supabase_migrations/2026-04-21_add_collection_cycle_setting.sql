-- Incremental migration: add rent collection cycle setting
-- Run this file once in Supabase SQL editor.

begin;

alter table if exists public.landlord_settings
    add column if not exists collection_cycle text not null default 'advance'
    check (collection_cycle in ('advance', 'arrears'));

commit;
