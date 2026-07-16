-- v1.2 upgrade (app 0.2.0) for projects already on upgrade_v1_1.sql.
-- Replaces the boolean dark-theme setting with a tri-state theme.
-- Run once in the SQL Editor. Fresh projects should run schema.sql instead.

alter table settings add column if not exists theme text not null default 'system'
    check (theme in ('system', 'dark', 'light'));
update settings set theme = case when dark_theme then 'dark' else 'light' end;
alter table settings drop column if exists dark_theme;
