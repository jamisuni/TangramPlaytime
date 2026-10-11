---
status: proposed
targets: [REQ-038, REQ-041, REQ-045, REQ-047]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F1
---
# CHG — The puzzle-validity rules are written in the Contract; the validator script is a tool, not the rule

## Change
REQ-038 Rules, third bullet: **"Checked by the puzzle validator rules V1–V6, V8–V10 and V12 in ../tools/validate_puzzles.py; V11 prints the build order."** becomes a written list:
- "Every piece of a puzzle's solution is congruent to one piece of TYPE-001; a full puzzle uses each of the seven exactly once, a mini puzzle (REQ-045) uses 1 to 6 distinct pieces."
- "No two solution pieces overlap, and every piece lies inside the silhouette, within the TYPE-004 geometry tolerance."
- "The solution pieces are connected through shared edges (a single shared corner point does not connect)."
- "The silhouette outline equals the outline of the union of the solution pieces."
- "Each puzzle carries an English and a Finnish title (REQ-047), a difficulty rating 1–5 and a kind (REQ-040), and a picture drawn inside its outline (REQ-039)."
- "The project's puzzle validator is the tool that checks these rules; it is evidence of a check, never the rule."
(The capture session transcribes any further V-rule the script enforces that is missing above; the reviewer could not read the script as Contract.)

REQ-038 Acceptance A1: **"Every puzzle file passes the validator."** becomes **"Every shipped puzzle satisfies every rule above."** A2 (**"Each puzzle's printed build order places all of its pieces (it always does; the line is for authors)."**) is deleted; its sentence moves to Rationale.

REQ-041 Rules: **"validator rule V12 checks the measure"** becomes **"the measure is checked for every warm-up"**. REQ-045 Rules: **"validator rule V9 then accepts 1 to 6 distinct pieces instead of all seven"** becomes **"a mini puzzle uses 1 to 6 distinct pieces of TYPE-001 (REQ-038)"**. REQ-047 Rules: **"validator rule V8 requires both `en` and `fi`"** becomes **"every puzzle carries both an `en` and a `fi` title (REQ-038)"**.

## Reason
Review-01 F1 (Blocker). A locked rule that delegates to a file puts the Contract in code nobody signed (lock gate v2). The build must prove REQ-038 A1 and REQ-007 A1 with an honest oracle; today the only oracle is running an unsigned script, and A2 is unprovable by its own admission.

## Impact
REQ-038.A1 (reworded), REQ-038.A2 (deleted), REQ-007.A1 (inherits), REQ-041 rule 2, REQ-045 rule 1, REQ-047 rule 2. Build side: the content WO's validity test is written against the listed rules; the script may implement them. Carries `implemented (unverified)` on REQ-038.A1 until decided.
