-- HUSH sync schema. Applied to Supabase project mrpagrxdyqooyfeybwqn (region ap-south-1) on 2026-11-25.
-- Every table is owned by one user and protected by Row Level Security.

create table public.library_items (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  media_id text not null,
  media_type text not null check (media_type in ('movie', 'series')),
  title text not null,
  poster_url text,
  saved boolean not null default true,
  updated_at timestamptz not null default now(),
  unique (user_id, media_type, media_id)
);

create table public.watch_history (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  media_id text not null,
  episode_id text not null default '',
  position_ms bigint not null default 0 check (position_ms >= 0),
  duration_ms bigint not null default 0 check (duration_ms >= 0),
  completed boolean not null default false,
  updated_at timestamptz not null default now(),
  unique (user_id, media_id, episode_id)
);

create table public.addons (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  url text not null,
  name text not null,
  enabled boolean not null default true,
  sort_order int not null default 0,
  updated_at timestamptz not null default now(),
  unique (user_id, url)
);

create table public.plugins (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  repo_url text not null,
  name text not null,
  enabled boolean not null default true,
  sort_order int not null default 0,
  updated_at timestamptz not null default now(),
  unique (user_id, repo_url)
);

alter table public.library_items enable row level security;
alter table public.watch_history enable row level security;
alter table public.addons enable row level security;
alter table public.plugins enable row level security;

create policy "own library rows" on public.library_items for all to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "own watch history rows" on public.watch_history for all to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "own addon rows" on public.addons for all to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "own plugin rows" on public.plugins for all to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);

revoke all on public.library_items, public.watch_history, public.addons, public.plugins from anon;
grant select, insert, update, delete on public.library_items, public.watch_history, public.addons, public.plugins to authenticated;

create index library_items_user_updated_idx on public.library_items (user_id, updated_at desc);
create index watch_history_user_updated_idx on public.watch_history (user_id, updated_at desc);
