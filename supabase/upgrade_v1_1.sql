-- v1.1 upgrade for projects that already ran the original schema.sql.
-- Adds entity colors and the optional track<->source relation.
-- Run once in the SQL Editor. Fresh projects should run schema.sql instead.

alter table people add column if not exists color text;
alter table sources add column if not exists color text;

create table if not exists track_sources (
    track_id uuid not null references tracks(id) on delete cascade,
    source_id uuid not null references sources(id) on delete cascade,
    primary key (track_id, source_id)
);

alter table track_sources enable row level security;
create policy "track_sources_owner" on track_sources for all
    using (exists (select 1 from tracks t where t.id = track_id and t.user_id = auth.uid()))
    with check (exists (select 1 from tracks t where t.id = track_id and t.user_id = auth.uid()));

-- Table privileges come from the default privileges set up by the grants
-- block in schema.sql; if you never ran that block, run it first.
