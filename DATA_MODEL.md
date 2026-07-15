# Data Model v1 (Supabase / Postgres)

All tables scoped by `user_id` (Supabase auth.uid()), RLS: `user_id = auth.uid()` on every table.

## tracks
| col | type | notes |
|---|---|---|
| id | uuid pk | |
| user_id | uuid fk auth.users | |
| name | text | |
| created_at | timestamptz | default now() |
| updated_at | timestamptz | default now(), bumped on every write; used for offline sync conflict detection |
| deleted_at | timestamptz, nullable | soft delete only (never hard-deleted); registries keep pointing at it |

Must always have ≥1 row in `track_people` (enforced app-side, not a DB constraint — a track with 0 shortcuts is invalid but the DB allows the transient state during edits).

## track_people (shortcuts, not a membership constraint)
People surfaced as quick-pick debtor shortcuts in a track's quick-create area. Does NOT restrict which debtors can have registries in the track — see `registries.person_id`.

| col | type | notes |
|---|---|---|
| track_id | uuid fk tracks.id | |
| person_id | uuid fk people.id | |
| PK | (track_id, person_id) | |

## track_sources (optional relation, v1.1)
Sources offered in a track's quick-create area. An EMPTY set means every source is offered; once ≥1 row exists, only the related ones show. Does NOT restrict which sources registries may use. Managed from the per-track settings screen; a source with registries in the track can't be unrelated.

| col | type | notes |
|---|---|---|
| track_id | uuid fk tracks.id | |
| source_id | uuid fk sources.id | |
| PK | (track_id, source_id) | |

## people
| col | type | notes |
|---|---|---|
| id | uuid pk | |
| user_id | uuid fk auth.users | |
| name | text | debtor's name, no uniqueness constraint |
| color | text, nullable | swatch key from the app's fixed 12-color palette (v1.1); null = auto by id hash |
| created_at | timestamptz | default now() |
| updated_at | timestamptz | default now(), bumped on every write |
| deleted_at | timestamptz, nullable | soft delete if referenced by ≥1 registry; hard-delete the row instead if zero references |

## sources
| col | type | notes |
|---|---|---|
| id | uuid pk | |
| user_id | uuid fk auth.users | |
| name | text | global tag, e.g. bank name, no uniqueness constraint |
| color | text, nullable | swatch key from the app's fixed 12-color palette (v1.1); null = auto by id hash |
| created_at | timestamptz | default now() |
| updated_at | timestamptz | default now(), bumped on every write |
| deleted_at | timestamptz, nullable | soft delete if referenced by ≥1 registry; hard-delete the row instead if zero references |

## registries
| col | type | notes |
|---|---|---|
| id | uuid pk | |
| user_id | uuid fk auth.users | |
| track_id | uuid fk tracks.id | |
| person_id | uuid fk people.id | any person owned by user — NOT restricted to track_people shortcuts |
| source_id | uuid fk sources.id, nullable | null for return regs (a return isn't tied to a source); normal regs must have one — `CHECK (type <> 0 OR source_id IS NOT NULL)` |
| amount | integer | CLP, no decimals, stored positive; sign/direction comes from `type` at display/sum time. For retRegs, `amount` is the *remaining* value — it shrinks as it absorbs debt and reaches 0 when fully consumed. `CHECK (amount >= 0)` + returns-only exemption for 0 — no business cap, int4's own range (up to 2,147,483,647) is the only ceiling |
| type | smallint | `0 = normal, 1 = return, 2 = superseded` |
| checked | bool | default false; normal reg = fully returned, retReg = fully consumed |
| note | text | ~140 char cap, app-enforced |
| date | date | user-set transaction date |
| created_at | timestamptz | default now(); also drives the edit-in-place-vs-supersede check |
| updated_at | timestamptz | default now(), bumped on every write; sync conflict detection |
| supersedes_id | uuid fk registries.id, nullable | set on the new row when it replaces an older one; old row's `type` flips to `2` |
| matched_retreg_id | uuid fk registries.id, nullable | normal reg only — the retReg that checked it (case 1 of match suggestion). Needed to undo the match on supersede. |
| deleted_at | timestamptz, nullable | soft delete (registries always have dependents by nature — person/source history — never hard-deleted) |

Indexes:
- `(track_id, date desc)` — table default sort
- `(person_id, type, checked)` — retReg match-suggestion lookup (find unchecked retRegs for debtor, closest-fit above/below new amount). Same index serves both the retroactive lookup (fires immediately on retReg creation) and the forward lookup (fires on every new normal reg creation) — same query shape either direction.
- `(track_id, type)` — totals/graphs exclude `type = 2` cheaply
- `(matched_retreg_id)` — supersede undo lookup

### Edit flow
Click an amount → edit popup. Check `created_at`, not `date` (avoids backdating edge case):
- `created_at` is today → UPDATE in place, no new row.
- `created_at` is older → INSERT new row with corrected amount + `supersedes_id` pointing at the old row; old row's `type` set to `2`. Popup surfaces feedback that this will log a correction rather than edit directly.
- Rows with `type = 2` render strikethrough, excluded from totals/graphs via the `type` filter.
- If the old row was a checked normal reg with `matched_retreg_id` set: credit its amount back to that retReg (retReg's amount += old row's amount, uncheck retReg if it had gone to `checked`), uncheck the old row too. Then try to re-apply the *new* corrected amount against that same retReg (retReg's amount -= new amount). Only proceed if `retReg.amount >= new amount` — otherwise skip the rematch, leave the new row's `matched_retreg_id` null, and let the normal match-suggestion flow (fires on new-reg creation) find it a different retReg on its own.

## settings (1 row per user)
| col | type | notes |
|---|---|---|
| user_id | uuid pk fk auth.users | |
| font | text | |
| language | text | locale code, `es-CL` or `en-US` for v1 |
| dark_theme | bool | default true |
| return_bg_color | text | hex, configurable bg for retRegs in tables |
| recent_table_size | smallint | default 50, one of 20/30/40/50/60/70 |
| historical_table_size | smallint | default 100, one of 50/75/100/125/150 |
| onboarded | bool | default false; flips true once the account has created ≥1 person and ≥1 source, gating the Starting screen |
| updated_at | timestamptz | default now(), bumped on every write; sync conflict detection (same as other tables) |

## Offline & sync
- Local writes queue while offline (Room/SQLDelight local DB mirrors the Supabase schema), sync on reconnect.
- Conflict = local `updated_at` and remote `updated_at` diverged since last sync on the same row. Surfaces a resolution screen: pick remote wholesale, pick local wholesale, or resolve manually field-by-field.
- Auth session persistence kept as offline-compatible as Supabase's client allows (cached session, no forced re-auth just for being offline).

## Notes
- `tracks` = persisted entity, one page/view per track onto its slice of the registries "database." Confirmed.
- `people` = the debtors.
