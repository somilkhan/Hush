-- Adds the display title to watch history so Continue watching works on other devices
-- without loading the catalogue. Additive only: no change to RLS or existing rows' meaning.
alter table public.watch_history add column if not exists title text not null default '';
