create table public.library_entries (
  user_id uuid not null references auth.users(id) on delete cascade,
  url text not null check (length(url) <= 8192 and url ~ '^https?://'),
  payload jsonb,
  deleted boolean not null default false,
  updated_at timestamptz not null default now(),
  primary key (user_id, url),
  constraint valid_library_payload check (
    (deleted and payload is null) or
    (not deleted and jsonb_typeof(payload) = 'object' and payload->>'url' = url
      and jsonb_typeof(payload->'title') = 'string' and octet_length(payload::text) <= 100000)
  )
);
alter table public.library_entries enable row level security;
revoke all on public.library_entries from anon;
grant select, insert, update on public.library_entries to authenticated;
create policy own_library_read on public.library_entries for select to authenticated using ((select auth.uid()) = user_id);
create policy own_library_insert on public.library_entries for insert to authenticated with check ((select auth.uid()) = user_id);
create policy own_library_update on public.library_entries for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create function public.touch_library_entry() returns trigger language plpgsql set search_path = '' as $$
begin new.updated_at = now(); return new; end;
$$;
create trigger library_updated before update on public.library_entries for each row execute function public.touch_library_entry();

-- Quick capture preserves existing notes/read status, but re-saving a deleted link restores it.
create function public.capture_link(item jsonb) returns void language sql security invoker set search_path = '' as $$
  insert into public.library_entries(user_id,url,payload,deleted)
  values ((select auth.uid()),item->>'url',item,false)
  on conflict (user_id,url) do update set payload=excluded.payload,deleted=false
  where library_entries.deleted=true;
$$;
revoke all on function public.capture_link(jsonb) from public, anon;
grant execute on function public.capture_link(jsonb) to authenticated;
