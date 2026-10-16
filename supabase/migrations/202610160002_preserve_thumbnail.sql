-- Older clients do not know about thumbnails. Their normal edits must retain
-- metadata already collected by newer clients for the same owner's same URL.
create or replace function public.preserve_library_thumbnail()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if new.user_id = old.user_id and new.url = old.url
     and coalesce(new.payload->>'thumbnail', '') = ''
     and coalesce(old.payload->>'thumbnail', '') like 'https://%' then
    new.payload = jsonb_set(new.payload, '{thumbnail}', old.payload->'thumbnail', true);
  end if;
  return new;
end;
$$;

create trigger preserve_library_thumbnail
before update on public.library_entries
for each row execute function public.preserve_library_thumbnail();
