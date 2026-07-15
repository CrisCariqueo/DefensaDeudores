-- Defensa de Deudores -- initial schema
-- Mirrors DATA_MODEL.md. Run once against a fresh Supabase project
-- (SQL Editor, or `supabase db push` if you set up migrations locally).

create extension if not exists "pgcrypto";

-- ============================================================ tracks
create table tracks (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    name text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz
);

alter table tracks enable row level security;
create policy "tracks_owner" on tracks for all
    using (user_id = auth.uid()) with check (user_id = auth.uid());

-- ============================================================ people
create table people (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    name text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz
);

alter table people enable row level security;
create policy "people_owner" on people for all
    using (user_id = auth.uid()) with check (user_id = auth.uid());

-- ============================================================ sources
create table sources (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    name text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz
);

alter table sources enable row level security;
create policy "sources_owner" on sources for all
    using (user_id = auth.uid()) with check (user_id = auth.uid());

-- ============================================================ track_people (shortcuts, not membership)
create table track_people (
    track_id uuid not null references tracks(id) on delete cascade,
    person_id uuid not null references people(id) on delete cascade,
    primary key (track_id, person_id)
);

alter table track_people enable row level security;
create policy "track_people_owner" on track_people for all
    using (exists (select 1 from tracks t where t.id = track_id and t.user_id = auth.uid()))
    with check (exists (select 1 from tracks t where t.id = track_id and t.user_id = auth.uid()));

-- ============================================================ registries
-- type: 0 = normal, 1 = return, 2 = superseded
-- source_id is null for return regs (a return isn't tied to a source);
-- normal regs must have one (enforced by source_required below).
create table registries (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    track_id uuid not null references tracks(id) on delete cascade,
    person_id uuid not null references people(id),
    source_id uuid references sources(id),
    -- retRegs are reduced as they absorb debt and may reach 0 (fully consumed);
    -- everything else stays strictly positive.
    amount integer not null check (amount >= 0),
    type smallint not null default 0 check (type in (0, 1, 2)),
    checked boolean not null default false,
    note text,
    date date not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    supersedes_id uuid references registries(id),
    matched_retreg_id uuid references registries(id),
    deleted_at timestamptz,
    constraint note_length check (char_length(note) <= 140),
    constraint source_required check (type <> 0 or source_id is not null),
    constraint amount_positive_unless_consumed_return check (amount > 0 or type = 1)
);

alter table registries enable row level security;
create policy "registries_owner" on registries for all
    using (user_id = auth.uid()) with check (user_id = auth.uid());

create index registries_track_date_idx on registries (track_id, date desc);
create index registries_person_type_checked_idx on registries (person_id, type, checked);
create index registries_track_type_idx on registries (track_id, type);
create index registries_matched_retreg_idx on registries (matched_retreg_id);

-- ============================================================ settings (1 row per user)
create table settings (
    user_id uuid primary key references auth.users(id) on delete cascade,
    font text not null default 'default',
    language text not null default 'es-CL' check (language in ('es-CL', 'en-US')),
    dark_theme boolean not null default true,
    return_bg_color text not null default '#FFF3CD',
    recent_table_size smallint not null default 50 check (recent_table_size in (20, 30, 40, 50, 60, 70)),
    historical_table_size smallint not null default 100 check (historical_table_size in (50, 75, 100, 125, 150)),
    onboarded boolean not null default false,
    updated_at timestamptz not null default now()
);

alter table settings enable row level security;
create policy "settings_owner" on settings for all
    using (user_id = auth.uid()) with check (user_id = auth.uid());

-- Auto-create a settings row when a new auth user signs up.
create function public.handle_new_user()
returns trigger as $$
begin
    insert into public.settings (user_id) values (new.id);
    return new;
end;
$$ language plpgsql security definer;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute procedure public.handle_new_user();

-- ============================================================ updated_at auto-bump
create function public.bump_updated_at()
returns trigger as $$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

create trigger tracks_bump_updated_at before update on tracks
    for each row execute procedure public.bump_updated_at();
create trigger people_bump_updated_at before update on people
    for each row execute procedure public.bump_updated_at();
create trigger sources_bump_updated_at before update on sources
    for each row execute procedure public.bump_updated_at();
create trigger registries_bump_updated_at before update on registries
    for each row execute procedure public.bump_updated_at();
create trigger settings_bump_updated_at before update on settings
    for each row execute procedure public.bump_updated_at();

-- ============================================================ grants
-- Projects created after Supabase's secure-by-default change no longer
-- auto-grant table privileges, so grant explicitly. RLS still scopes every
-- row to its owner; anon gets nothing because the app only syncs signed in.
grant usage on schema public to authenticated;
grant select, insert, update, delete on all tables in schema public to authenticated;
alter default privileges in schema public
    grant select, insert, update, delete on tables to authenticated;
