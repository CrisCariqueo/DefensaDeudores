# Defensa de Deudores — Scope v1

Money-lending tracker app. Track who owes you, how much, from what source.

## Stack
- Android native, Kotlin + Jetpack Compose
- `minSdk 29` (Android 10 — covers Redmi Note 9 Pro's full official OS range), `targetSdk` latest-stable at build time
- Backend: Supabase (Postgres + realtime sync), offline-first (local writes queue, sync on reconnect)
- Auth: email / Google sign-in
  - Google OAuth: package name + SHA-1/256 fingerprint (debug *and* release keystores) registered in Google Cloud Console, wired into Supabase's Google provider. One-time setup at build/signing time.
- Charts: Compose Canvas / Vico for circular graphs
- Fonts: aim for something close to Claude's UI font; open/licensable alternative if the original isn't free. Pin exact family at scaffold time.
- Locales v1: base locale (es-CL) + en-US
- Android-only v1, iOS considered later

## Entities
- **Track** — persisted entity, own name; a page/view onto its slice of the registries "database." Main screen lists tracks. Created via bottom-right FAB on Main. Renamed from its own config view (topbar-right icon on the Track screen). Must always have ≥1 debtor shortcut (`track_people`). Delete = soft delete only (`deleted_at`); registries keep pointing at it, history intact.
- **Person** — id, name (CRUD screen). These are the debtors. No uniqueness constraint on name — duplicates allowed. Delete: hard-delete if the person has zero registries, otherwise soft delete (`deleted_at`).
- **Source** — id, name (tag/category, e.g. bank name) (CRUD screen). No uniqueness constraint on name. Delete: same tier as Person — hard delete if unreferenced, soft delete otherwise.
- **Registry** — id, amount (CLP, integer), person_id, source_id, date, note (short text, ~100-140 char cap), type (normal | return | superseded), checked (bool — fully settled, applies to normal/return: normal reg fully returned, or retReg fully consumed), supersedes_id (nullable, points at the reg this one replaces)

## Screens
0. **Starting screen** — shown to new accounts only. Must create ≥1 Person and ≥1 Source before leaving it. Not shown again once satisfied.
1. **Main** — entry points to each Track screen, People, Sources, Config. Bottom-right FAB creates a new Track.
2. **Track** (can span multiple people)
   - Two circular graphs: by debtor, by source. Both computed unchecked-only; debtor and source graphs are independent, no cross-influence.
   - Quick-create: buttons per source → **modal** for amount, tied to selected debtor
   - Table 1: recent registries, default 50, configurable (20/30/40/50/60/70)
   - Table 2: full history, toggle visibility, default off, paginated, default 100, configurable (50/75/100/125/150)
   - Both tables sorted newest-first
   - Toolbar: filter icons, search, return-search trigger
   - Empty states: anywhere a selector or quick-create space is used with nothing to select (no people/sources yet), show a warning message + button to the relevant create screen.
   - **Search**: plain text query, matches on amount and note.
   - **Filters**: AND-combinable — person(s), source(s), type, date range (from/to, entered via month-view popups), checked state.
   - **Return-search modal** (pull direction, manual, triggered by search icon): enter amount → lists existing unchecked normal regs with amount ≤ entered amount.
     - Path A: select one or more of those regs summing exactly to entered amount → confirm → they get checked directly. No retReg created, one-shot settlement.
     - Path B: no exact combo (or user choice) → create a **retReg** for that amount instead. Persists, unaccounted, live to absorb future debt.
   - **retReg match suggestion** (suggest-and-confirm — never silent): system checks debtor's unchecked retRegs, picks closest-fit candidate. Fires in two moments: (a) retroactively, right when a retReg is created — scans existing unchecked debt immediately, not just going forward; (b) on every subsequent new normal reg creation for that debtor.
     1. Smallest unchecked retReg with amount ≥ new reg's amount, if one exists → suggest: "debt will register as checked." On confirm: new reg checked (fully covered); retReg's amount reduced by new reg's amount — stays unchecked if remainder > 0, else checked.
     2. Else, largest unchecked retReg with amount < new reg's amount, if one exists → suggest: "debt will register as {remaining amount}." On confirm: new reg's amount reduced by retReg's amount, stays unchecked/open; retReg fully consumed → checked.
     3. Else (no unchecked retRegs for debtor) → no suggestion, new reg created as-is.
     User can dismiss any suggestion; new reg then created unmatched.
   - Return regs always render in normal tables (never hidden), negative in totals, bg color set in Config (not fixed).
   - **Edit flow**: click a reg's amount → edit **popup**. If reg was created today (`created_at`) → edits in place. If older:
     - Original left untouched apart from `type` flipping to `superseded` (strikethrough in table, excluded from totals/graphs); new reg inserted with corrected amount + link back to the one it replaced.
     - If the original was a checked normal reg matched to a retReg: undo the match first (credit the amount back to that retReg, uncheck both), then try to re-apply the new corrected amount against that same retReg. Only re-applies if it still fully fits (new amount ≤ restored retReg balance) — if it would push the retReg negative, skip the auto-rematch; the new reg is created unmatched and left for the normal match-suggestion flow to find a different retReg on its own.
     - Popup gives feedback when the supersede path triggers ("this will log a correction, not edit directly").
3. **People** — CRUD
4. **Sources** — CRUD
5. **Config** — font, language (es-CL / en-US), dark theme toggle, return-reg bg color, table page-size defaults (recent + historical)

## Rules
- Registry amounts always CLP, integer, no cap beyond guarding the int4 type's own overflow limit
- Dark theme required
- i18n-ready (language switch in Config)
- Soft delete is the default everywhere there are dependencies; hard delete only when an entity has zero referencing registries
- Offline-first: app usable with no connection, writes queue locally and sync on reconnect. Sync conflicts open a resolution screen — choose remote or local wholesale, or resolve manually field-by-field.

## Out of scope for v1 (later)
- iOS
- Multi-currency
- Notifications/reminders
