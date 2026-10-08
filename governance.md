# Governance profile — TangramPlaytime

**Status:** in force  ·  **Preset:** ai-mastered  ·  **Version:** 0.1  ·  **Last updated:** YYYY-MM-DD
**Set by:** drafted by the scaffold 2026-10-08 from the v0.20 preset — **the owner flips Status to `in force`** (F4); v1 ran ai-mastered, in force since 2026-10-01  ·  **Owner checkpoint:** before the dependent WO · ≤ 5 items · defaults after 3 days
**Decisions sheet:** `decisions.md`

> Who owns each control point: `human` = stop and ask · `ai` = decide, act,
> log to `decisions.md` · `ai+inform` = as `ai`, surfaced at the next
> checkpoint. Floors F1–F4 are never `ai`. A `proposed` profile is NOT in
> force — the project runs human-governed until the owner sets it. Fill in
> from `C:\GitHub\AI\SWDev\framework\templates\governance.md`; rules:
> `C:\GitHub\AI\SWDev\framework\methodology\gates-and-autonomy.md` §8.

---

## 1. Control points

| # | Control point | Owner | Floor | Notes / conditions |
|---|---|---|---|---|
| 1 | G1 — requirements lock | human | F1 | never AI |
| 2 | Build from unsigned `ai-approved` items (prototype lane) | no | — | `Target: both`, pending |
| 3 | G1 fresh-eyes requirements review | yes | — | |
| 4 | G2 — architecture + guardrails sign-off | ai+inform | — | |
| 5 | Technology / stack choice | ai+inform | — | within §2 |
| 6 | Governed interface tiers at G2 | ai | — | defaults to `notify` |
| 7 | G3 — work-order plan approval | ai | — | |
| 8 | WO sequencing / priorities | ai | — | by Depends-on graph + value |
| 9 | G4 — merge review | ai (spot-check 1 in 5) | — | trust dial §6 inside this |
| 10 | Hard-stop 1 — security / PII / money | human | F2 | never AI |
| 11 | Hard-stop 2 — schema / migration / breaking API | ai+inform | — | device-local data only; migration test required |
| 12 | Hard-stop 3 — ambiguous requirement | ai | — | ASSUMPTION line + capture-side DEF/CHG proposal; two locked items in conflict → same-day CHG; product-visible → checkpoint item |
| 13 | Hard-stop 4 — guardrail / directive conflict | ai+inform | — | project guardrails only; directives stay human |
| 14 | Hard-stop 5 — change to a locked REQ / TYPE / locked-tier I* | human | F1 | AI may file a CHG proposal |
| 15 | New external dependency | ai | — | D5 justification |
| 16 | Waivers | ai+inform | — | |
| 17 | Owner checkpoint | before the dependent WO | — | ≤ 5 product-visible items, Q-shaped, defaults after 3 days; lists → `review-notes.md` |
| 18 | Version-control snapshot | human | F3 | never AI |
| 19 | Editing this profile | human | F4 | never AI |
| 20 | Hard-stop 6 — test/verifier needs a product change | ai (CHG + unverified) | — | never by changing the product |

## 2. Constraints the AI must stay inside when it owns a row

- none yet

## 3. Change log

| Version | Date | Change | Reason (stakes / evidence) | By |
|---|---|---|---|---|
| 0.1 | 2026-10-08 | ai-mastered preset, proposed (v0.20 rows incl. 12/17/20) | the v1 profile, re-based on v0.20 | scaffold (owner to set in force) |
