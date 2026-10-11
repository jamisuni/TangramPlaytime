# Fresh-eyes review 01 — TangramPlaytime · collection v1.2 (all areas)

**Date:** 2026-10-11 · **Reviewer:** SWDev requirements-reviewer station, fresh context (no capture history, no evidence/, no chat, nothing from the first run) · **Inputs:** `requirements.md` (v1.2), `features.md`, `req_types.md` (v0.2), `reqs/REQ-001..051`, `views/digest.md`, `views/trace.md`, `views/lock-receipt.md`, `changes/CHG-001`, `changes/CHG-002`, `sources.md` header lines only; `validate.py` run (0 errors, 66 lock-gate-v2 warnings, each judged here, not copied). Intake path C: the collection is already locked, so every `chg` verdict is executed on the capture side from `capture-outbox/chg-draft-*.md`.

## Verdict

**4 Blockers / 21 Shoulds / 13 Notes.** Not ready to accept at G1 as it stands. Ready on the condition that the owner gives every row a verdict and the four Blockers are resolved or explicitly accepted: the puzzle-validity Contract lives in a script nobody signed (F1), six Draft/Inferred TYPEs carry the numbers every play REQ is tested against (F2), the distribution channel is an open question while REQ-048 and REQ-001 A2 can only be proven on Google Play (F3), and REQ-002 A1 contradicts REQ-018 (F4). No misleading abstract was found; the digest is an honest sign-off surface.

| Finding | Severity | Verdict | Resolution |
|---|---|---|---|
| F1 — REQ-038 (and REQ-041, 045, 047): the puzzle-validity Contract is "rules V1–V6, V8–V10, V12 in ../tools/validate_puzzles.py"; A2 is vacuous by its own text | Blocker | chg | Write the validity rules out in the REQ (or a TYPE); cite the script as a tool, not the rule; drop A2 — `chg-draft-req038-validity-rules-in-contract.md` |
| F2 — TYPE-001/003/004/005/006/007: dictionary Status Draft, five Inferred, under 47 locked REQs; R = 0.65, 60 s idle, 600 dp, colours, scoring are unsigned Contract | Blocker | chg | Owner confirms or changes each ASSUMPTION value and signs the dictionary (Status Locked, Approved by) — `chg-draft-lock-type-dictionary.md` |
| F3 — requirements.md §5 / REQ-048 / REQ-001 A2: OPEN QUESTION "which account publishes" while every REQ-048 rule and REQ-001 A2 presuppose Google Play | Blocker | chg | Owner names the channel; REQ-048 says "Google Play" (or its rules are rewritten for F-Droid/APK); the closed-test obligation becomes a rule or is declined — `chg-draft-req048-store-channel.md` |
| F4 — REQ-002 A1 vs REQ-018/TYPE-003: "completed by drag, turn and drop only" is false for any puzzle whose solution needs the mirrored parallelogram | Blocker | chg | Add "mirror (REQ-018)" to the Statement and A1 — `chg-draft-req002-a1-mirror.md` |
| F5 — REQ-042: "at least four of the categories in ../Spec/03-puzzle-format.md" delegates the theme set to a design input | Should | chg | Name the theme set in the REQ, or require four distinct declared themes without a file — `chg-draft-req042-themes-named.md` |
| F6 — REQ-046: written for the web prototype ("until the page … is restarted", hidden "in the design sheets (../Spec/ui/)"); "release build" undefined against a Play closed-test track | Should | chg | Strip prototype clauses; define release build as any build uploaded to a store track — `chg-draft-req046-android-build-terms.md` |
| F7 — REQ-006: no platform floor (minimum Android version, minimum screen) anywhere; the build chose API 26 alone | Should | chg | Owner confirms the floor the build uses, recorded as a rule — `chg-draft-req006-platform-floor.md` |
| F8 — REQ-006 A1: "fully playable" is the BIZ REQ's only acceptance and has no oracle | Should | chg | Replace with a concrete play-through on a named phone and tablet class — `chg-draft-req006-a1-playable-oracle.md` |
| F9 — REQ-005 A1/A2: "is available" names no place | Should | chg | Point at the display REQs (REQ-026, REQ-029) — `chg-draft-req005-available.md` |
| F10 — REQ-014 A1: "follows the finger without delay" is unmeasurable | Should | chg | Drop A1 or bound it to one displayed frame — `chg-draft-req014-a1-without-delay.md` |
| F11 — REQ-018 vs REQ-015: the flip badge sits inside the parallelogram's tray cell, whose whole area is the piece's touch area; a touch on the badge is both a tap-to-turn and a mirror | Should | chg | State that the badge's 60 dp area takes precedence inside the cell — `chg-draft-req018-badge-touch-precedence.md` |
| F12 — REQ-017 vs REQ-015: a second finger that lands on another piece during a drag is a twist finger by REQ-017 and a tap by REQ-015 | Should | chg | While a drag is in progress every further touch is a twist finger, never a tap or drag — `chg-draft-req017-second-finger-never-a-tap.md` |
| F13 — TYPE-006: no transition for Reset (REQ-034), for the dev-aid solve (REQ-046), for a load-check that empties the board (REQ-025); a missed first drop leaves In progress with an empty board | Should | chg | Add the three transitions and state the empty-board rule — `chg-draft-type006-missing-transitions.md` |
| F14 — REQ-025: "the state survives closing the app" says nothing about when state is saved (process death) or about saved state that cannot be read (corrupt / older format) | Should | chg | Save at every state change; unreadable state starts as a fresh install, never a failed launch — `chg-draft-req025-unreadable-state-and-save-timing.md` |
| F15 — REQ-014/REQ-020: a drag interrupted by anything but a release (call, app switch, tablet rotation, process death) has no defined outcome | Should | chg | An interrupted drag is a missed drop (REQ-020) — `chg-draft-req020-interrupted-drag.md` |
| F16 — REQ-048: rules for data safety, content rating, target audience and "teacher-approved programme where one exists" have no acceptance line; "where one exists" is vague | Should | chg | One owner-proven acceptance per console declaration; decide the programme opt-in — `chg-draft-req048-console-declarations-acceptance.md` |
| F17 — REQ-009 / REQ-049: the English texts are Contract, the Finnish wording "in the prototype" is a design input; A1 "in full" is unprovable in Finnish | Should | chg | Owner supplies and signs the Finnish wording inside the REQs — `chg-draft-req009-req049-finnish-wording.md` |
| F18 — REQ-033 A1: "no action makes a sound or a haptic tick" is the whole-app negative that cost the first run 28 decision rows (principle 15) | Should | chg | Make A1 a device check of the five named actions, accepted by the owner's evidence row — `chg-draft-req033-a1-device-oracle.md` |
| F19 — REQ-025 rule "stored per player profile … the storage layout should not prevent it": a how, for a feature declared out of scope | Should | chg | Keep "one profile", drop the storage-layout clause to design inputs — `chg-draft-req025-drop-profile-storage-rule.md` |
| F20 — REQ-040 A2, REQ-042 A3, REQ-047 A1: acceptance binds to puzzle ids/titles ("shapes-warmup", "shapes-square", the Cat puzzle "Kissa") that no REQ requires and that live in the design-input library | Should | chg | Reword to the property, not the id — `chg-draft-acceptance-content-ids.md` |
| F21 — `depends:` fields: 20 REQs name other REQs/TYPEs in Statement or Rules that are absent from `depends:` (REQ-014→019/020, REQ-024→025/050/TYPE-006, REQ-031→TYPE-005, REQ-032→the seven it lists, …) | Should | chg | Complete the fields from the prose — `chg-draft-depends-fields-complete.md` |
| F22 — REQ-032: "a short how-to-play text" has no content in either language; the build would write Contract text nobody signed | Should | chg | Question: does the owner supply/approve the text, or is it explicitly left to the build with a length bound? No draft until answered |
| F23 — TYPE-006 / REQ-050: Retry returns Solved → New, so a retried puzzle shows as unsolved in the grid and is the long-press target although its best time is kept | Should | reject | Question: is a retried puzzle "unsolved" for REQ-050 and the grid? No draft until answered |
| F24 — Legal, never asked: the GPL-3.0 licence appears only inside an open question; no REQ covers a licence notice/source offer in the app or listing, nor the licence of sounds, chime, confetti, fonts, pictures | Should | chg | Question for the owner (asked-before-lock topic "legal"); record the answer or `declined`. No draft |
| F25 — requirements.md: no "Asked before lock" table (store, legal, platform, data, money, size, outside systems); the v2 gate refuses the next re-lock without it | Should | chg | Capture session adds the table with the owner's answers (F3, F7, F24 feed it). No draft (index artifact) |
| F26 — 13 UI REQs (009, 013, 021, 031, 032, 035, 036, 037, 043, 049, 051 …) locked with no `firsthand` source: every dp and ms is Contract before anyone held a phone | Note | reject | Owner acknowledges the first device playtest is a planned CHG round, not a defect round |
| F27 — REQ-020/033 "soft (low) tone", REQ-035 "about 10 % padding", REQ-051 "about 600 ms" next to "shown for 600 ms" | Note | defer | Business does not care; leave, or align REQ-051's two durations |
| F28 — REQ-019: abstract says "nearest spot", rules choose by score within a 0.16 window; A4 omits the within-R / matching-turn precondition | Note | reject | Read A4 under the Statement's preconditions; optional abstract tweak |
| F29 — REQ-013: only cell width is defined ("Cell width = …"), cell height is implied by "fits in any turn"; A1 holds by arithmetic (smallest cell ≈ 65 dp on 360 × 780) | Note | reject | State height = width or leave to design |
| F30 — REQ-046 overlay "stays on while the player browses" vs REQ-026 A1 "never shows where the pieces were": conflict in test builds only | Note | reject | Overlay off on solved puzzles, or accept for test builds |
| F31 — REQ-023 "the pieces do not pop" under reduced motion: no rule says pieces pop | Note | defer | Either name the pop animation or drop the clause |
| F32 — REQ-039/040/041/042/045/047: puzzle-file field names (`kind`, `reviewedByHuman`, `title[lang]`) in Contract bind the content format, a design input | Note | reject | Acceptable as checkable; be aware a format change is a CHG |
| F33 — REQ-050 long press: wrap is unstated when the only unsolved puzzles lie before the current one | Note | reject | Read as wrapping like REQ-024 |
| F34 — Index/features still titled "TangramNoAds", tools v0.4 vs v0.5, §3 "Shared types: draft"; CHG-001 settled the Finnish store title "by default" as an ASSUMPTION | Note | defer | Cosmetic; the owner should see the Finnish-title default once |
| F35 — Whole-app negatives (REQ-001 A1, REQ-008 A1/A2, REQ-010 A2, REQ-046 A4) and REQ-007's acceptance (which only names its children) need a declared oracle on the build side | Note | reject | Build side: dependency scan + screen walk as owner-signed evidence rows |
| F36 — REQ-012/REQ-025 vs TYPE-006: a tray piece's turn is saved "per piece" while turning in the tray does not start the puzzle; whether a New puzzle's tray turns persist is unstated | Note | reject | Decide at build; harmless either way |
| F37 — REQ-046: passcode 0417 is public in a GPL repository (a speed bump, not a secret); REQ-045 A1 presumes the 3-piece mini sorts first | Note | reject | Accept as is |
| F38 — features.md #SoundToggle omits "turn"; REQ-012's title rule duplicates REQ-024's top-bar rule | Note | defer | Tidy at the next capture session |

**Verdicts** — recorded 2026-10-11 by the orchestrator on the owner's instruction (Jami, chat, verbatim in `capture-outbox/src-draft-g1-owner-answers.md`). F1–F4, F7, F17, F24 were answered one by one on the G1 surface (items 3, 2, 1, 4, 5, 6, 7 there); F5–F38 otherwise follow the Resolution column ("rest ok also"). Where the owner's answer changed the direction of a draft, the draft follows the owner, not this table: **F17** — the owner chose English as the Contract language, Finnish as a reference translation (drafts `chg-draft-req009-req049-finnish-wording.md`, rewritten, and `chg-draft-req047-english-contract.md`); **F7** — Android 8.0 confirmed; **F3** — Google Play; **F22** — the build writes the text, ≤ 60 words (`chg-draft-req032-how-to-play-text.md`); **F24** — both proposals accepted (`chg-draft-licence-notice-and-assets.md`); **F23** — `reject`: the locked TYPE-006 transition stands (a retried puzzle is unsolved for the grid). Notes marked `reject` are left as written; F27, F31, F34, F38 are `defer` to the next capture session as tidy-ups.

## Findings in detail

### F1 — UNTESTABLE · Blocker · REQ-038 (REQ-041, REQ-045, REQ-047)
- **Observation:** REQ-038 Rules: "Checked by the puzzle validator rules V1–V6, V8–V10 and V12 in ../tools/validate_puzzles.py; V11 prints the build order." A1: "Every puzzle file passes the validator." A2: "Each puzzle's printed build order places all of its pieces (it always does; the line is for authors)." REQ-041 ("validator rule V12 checks the measure"), REQ-045 ("validator rule V9 then accepts 1 to 6 distinct pieces") and REQ-047 ("validator rule V8 requires both en and fi") point at the same script. The script is a design input nobody signed; the collection never says what V1–V6, V8–V10 are.
- **Proposed resolution:** write the validity rules out (congruence with TYPE-001, each piece exactly once or 1–6 distinct for mini, no overlap within the TYPE-004 tolerance, connected through shared edges, outline equals the union's outline, both titles, warm-up measure, picture inside outline), cite the script as the tool that checks them, delete A2.
- **Why it matters to the build:** REQ-038 A1 is provable only by running an unsigned script; A2 is unprovable by construction ("it always does"). REQ-007 A1 inherits the hole.

### F2 — DEPENDENCY · Blocker · TYPE-001, 003, 004, 005, 006, 007
- **Observation:** `req_types.md` header: "Status: Draft · Approved by: —". TYPE-001/003/004/005/007 are "Confidence: Inferred". The lock receipt lists 45 "depends on TYPE-00n, which is not locked" lines. The numbers the build will be tested against — R = 0.65 units / 30 dp, the 0.04 corner bonus and 0.16 window, 1e-6 tolerance, 60 s idle, 600 dp boundary, the seven colours and resting turns, the TYPE-006 transitions — all sit in those TYPEs as ASSUMPTIONs.
- **Proposed resolution:** the owner reads each ASSUMPTION, confirms or changes it, and signs the dictionary (Status Locked, Approved by/on; Inferred → Stated). The playtest may move R later through a CHG.
- **Why it matters to the build:** REQ-019 A1–A4, REQ-021 A1–A2, REQ-016 A1, REQ-029 A1, REQ-030 A1, REQ-035 A1, REQ-036 A1, REQ-013 A1, REQ-043 A1 are all tested against values no human signed. The next re-lock fails the v2 gate on this alone.

### F3 — QUESTION · Blocker · requirements.md §5, REQ-048, REQ-001 A2
- **Observation:** Index §5: "OPEN QUESTION (distribution, SRC-013): which developer account publishes the game? … F-Droid … and a signed APK … have no such rule." REQ-048 Out of scope: "Which store or channel (Play, F-Droid, a signed file): an open question in the index." Yet every REQ-048 rule is Google Play (data-safety declaration, Families policies, content-rating questionnaire, teacher-approved programme) and REQ-001 A2 / REQ-048 A1 test the Play labels "Contains ads" / "In-app purchases". A question binds nobody; the lock binds everyone.
- **Proposed resolution:** the owner answers. If Google Play: REQ-048 names it, the account type decides whether the 12-tester/14-day closed test is a rule, and the index question closes. If F-Droid or a signed file: REQ-048's rules and REQ-001 A2 need rewriting instead.
- **Why it matters to the build:** REQ-048 A1, A2 and REQ-001 A2 have no oracle until the store is named; the #Release WO cannot close.

### F4 — CONFLICT · Blocker · REQ-002 A1 vs REQ-018 / TYPE-003
- **Observation:** REQ-002 Statement: "rebuild a shown silhouette … by dragging, turning and dropping them"; A1: "Every puzzle can be completed with pieces of TYPE-001 by drag, turn and drop only." REQ-018 locks a mirror operation and TYPE-003 says only the parallelogram's mirror image differs. Any puzzle whose solution needs the mirrored parallelogram cannot be completed by drag, turn and drop only.
- **Proposed resolution:** "by dragging, turning, mirroring (the parallelogram, REQ-018) and dropping" in the Statement; A1 likewise.
- **Why it matters to the build:** a literal A1 test fails on a valid puzzle; the Contract contradicts itself at BIZ level.

### F5 — UNTESTABLE · Should · REQ-042
- **Observation:** Rules: "Themes: at least four of the categories in ../Spec/03-puzzle-format.md." A2: "At least four categories are present." Spec/ is a design input; the set of categories is not Contract.
- **Proposed resolution:** list the theme names in the REQ, or require "at least four distinct themes declared by the puzzles" without naming a file.
- **Why it matters to the build:** A2 can be counted, but whether a declared theme is a legitimate one cannot be judged from the Contract.

### F6 — AMBIGUITY · Should · REQ-046
- **Observation:** "After the right passcode the aid stays unlocked until the page or app is restarted." "The DEV button is hidden on a solved puzzle and in the design sheets (`../Spec/ui/`)." "WHERE the build is a prototype or test build … WHERE the build is a release build." An Android build has no page and no design sheets. If the Play closed test (F3) uses the same artifact that is promoted to production, it is both a test build and a release build.
- **Proposed resolution:** strip the web clauses; define "release build" as any build uploaded to a store track and "test build" as a locally installed build.
- **Why it matters to the build:** A4 ("A release build contains no DEV button") needs the definition; the Spec/ui clause has no oracle at all.

### F7 — GAP · Should · REQ-006
- **Observation:** REQ-006 names "Android phones held upright and Android tablets" and nothing else: no minimum Android version, no minimum screen size, no density range. The build decided minSdk 26 without a REQ (principle 8). Outside-world topic "platform floor" was never asked.
- **Proposed resolution:** the owner confirms the floor the build uses (Android 8.0 / API 26 proposed) and the smallest supported width (320 dp proposed from the REQ-013 arithmetic), as rules.
- **Why it matters to the build:** REQ-006 A1 and every "on a … dp phone" line have no device class to run on; the store listing's device filter is a silent decision.

### F8 — UNTESTABLE · Should · REQ-006 A1
- **Observation:** A1: "The game is fully playable on a phone in portrait and on a tablet." "Fully playable" is a v2 vague term and the only acceptance of this BIZ REQ.
- **Proposed resolution:** "A play-through from the first mini puzzle to a solved full puzzle, using REQ-014..022 only, completes on a phone (360 × 780 dp class) in portrait and on a tablet (1280 × 800 dp class) in both orientations."
- **Why it matters to the build:** without it the oracle is an opinion.

### F9 — AMBIGUITY · Should · REQ-005
- **Observation:** A1 "its solve time is available"; A2 "The total active play time is available at any time." Available where, to whom?
- **Proposed resolution:** "is shown on the solved view (REQ-026)" and "is shown in the settings screen (REQ-029)".
- **Why it matters to the build:** two acceptance lines with no observable.

### F10 — UNTESTABLE · Should · REQ-014 A1
- **Observation:** A1: "A dragged piece follows the finger without delay." No system does; the rule above it already quantifies the 120 ms grow.
- **Proposed resolution:** drop A1, or "the piece is drawn at the finger's position of the same or the previous displayed frame".
- **Why it matters to the build:** a literal test cannot pass; a lenient one proves nothing.

### F11 — CONFLICT · Should · REQ-018 vs REQ-015
- **Observation:** REQ-015: "the touch area of a tray piece is its whole tray cell"; REQ-013: "A cell holds nothing but its piece, its size mark and, for the parallelogram, the flip badge"; REQ-018: the badge's touch area is 60 dp. A still touch on the badge satisfies both "tap on the piece" (turn, REQ-016) and "press the badge" (mirror).
- **Proposed resolution:** inside the parallelogram's cell the badge's area wins; the rest of the cell is the piece.
- **Why it matters to the build:** REQ-016 A1 and REQ-018 A1 cannot both be derived for the same touch.

### F12 — EDGE · Should · REQ-017 vs REQ-015
- **Observation:** REQ-017: "WHEN a second finger touches the screen and rotates around the first". REQ-015: "WHEN a touch on a piece ends having moved less than 12 dp … a tap." A second finger that lands on a tray piece and lifts is both the start of a twist and a tap that turns that other piece.
- **Proposed resolution:** while a drag is in progress every further touch is a twist finger and never a tap or a drag of another piece.
- **Why it matters to the build:** REQ-017 A2 ("A 20° twist does not turn the piece") can pass while a different piece turned.

### F13 — Lifecycle · Should · TYPE-006
- **Observation:** TYPE-006 lists New → In progress (drag starts on a tray piece), In progress → Solved (last piece locks), Restart, Retry. Missing: Reset all progress (REQ-034: "set every puzzle to New" from any state), the dev-aid solve (REQ-046: "marked solved" from New or In progress without a last lock), and the load check (REQ-025: pieces returned to the tray may empty the board of an In progress puzzle). Also, a first drag that goes home (REQ-020) leaves an In progress puzzle with an empty board, so time counts and Restart shows on an empty board.
- **Proposed resolution:** add the three transitions; state whether an In progress puzzle with no piece on the board stays In progress (proposed: yes, as written) so the build does not guess.
- **Why it matters to the build:** REQ-030 A3, REQ-034 A1, REQ-046 A3, REQ-025 A3 each touch an ungoverned transition.

### F14 — EDGE · Should · REQ-025
- **Observation:** "The state survives closing the app." REQ-024: "Moving away saves the current puzzle's state first." Nothing says state is saved on each lock, so a process death mid-puzzle may lose pieces placed since the last navigation (REQ-003 A2 "never loses its state"). Nothing says what happens when the saved state cannot be read (corrupt file, older format after an update): the "must never" side (a launch that fails) is absent.
- **Proposed resolution:** "state is saved at every lock, return, restart, retry and reset"; "IF saved state cannot be read, THEN the game starts as on a fresh install and never fails to start".
- **Why it matters to the build:** REQ-003 A2 and REQ-025 A1 are ambiguous under process death; the unreadable-state case has no acceptance.

### F15 — EDGE · Should · REQ-014 / REQ-020
- **Observation:** REQ-014 governs "WHEN the player releases it"; REQ-020 governs drops. A drag ended by an incoming call, an app switch, a tablet rotation (REQ-036 A2 "keeps every placed piece" — the dragged piece is not placed) or process death has no defined outcome, while REQ-012 A2 demands every piece be in the tray, on the board or dragged.
- **Proposed resolution:** REQ-020: "A drag ended by anything other than the player's release is a missed drop: the piece returns to its tray cell."
- **Why it matters to the build:** REQ-012 A2 and REQ-036 A2 need the answer; otherwise the build decides.

### F16 — GAP · Should · REQ-048
- **Observation:** Rules: "The store's data-safety declaration says 'no data collected'", "The content rating questionnaire is completed before release", "The target audience declared includes children under 13", "The game is offered to the store's teacher-approved programme where one exists." Acceptance has A1–A4 only (privacy link/labels, title, permissions, icon). "Where one exists" is vague for a programme Google runs by invitation/opt-in.
- **Proposed resolution:** one owner-proven acceptance line per console declaration; decide "opt in to Teacher Approved" or drop the rule.
- **Why it matters to the build:** four locked rules have no line to go GREEN on; the #Release WO's done-check is undefined.

### F17 — GAP · Should · REQ-009, REQ-049
- **Observation:** REQ-009 and REQ-049 give the English texts in full and require them "in full" (A1). REQ-047: "The free note (REQ-009) and the privacy text (REQ-049) exist in both languages." REQ-049 Assumptions: "the wording above, and its Finnish translation in the prototype." The prototype is a design input; the Finnish wording the owner's own audience reads is not Contract.
- **Proposed resolution:** the owner supplies/approves the Finnish wording of both texts inside the REQs.
- **Why it matters to the build:** REQ-009 A1 and REQ-049 A1 with the device in Finnish compare against nothing signed.

### F18 — UNTESTABLE · Should · REQ-033 A1
- **Observation:** A1: "With sound off, no action makes a sound or a haptic tick." A universal negative over the whole app; the framework's own harvest (principle 15, TangramNoAds) records that this exact shape became a JVM gate, scans, probes, 28 decision rows and a product change.
- **Proposed resolution:** "With sound off, picking up, turning, locking, returning a piece and solving a puzzle produce no audible sound and no vibration on the test device" — proven by the owner's device check (evidence row), not by a verifier larger than the feature.
- **Why it matters to the build:** hard-stop 6 again, unless the oracle is named now.

### F19 — Scope / how · Should · REQ-025
- **Observation:** Rule: "Progress is stored per player profile; the first version has exactly one profile … the storage layout should not prevent it." The index and REQ-003 put several profiles out of scope; "storage layout" is a how (principle 2) for a requirement that does not exist (principle 14).
- **Proposed resolution:** keep "the first version has exactly one profile", move the layout wish to design inputs.
- **Why it matters to the build:** a locked rule with no acceptance that invites speculative architecture.

### F20 — UNTESTABLE · Should · REQ-040 A2, REQ-042 A3, REQ-047 A1
- **Observation:** REQ-040 A2: "A full puzzle of rating 1 whose id sorts before 'shapes-warmup' …"; REQ-042 A3: "shapes-square is among them"; REQ-047 A1: "the Cat puzzle is titled 'Kissa'". No REQ requires a Cat puzzle or those ids; the library (`../Tangrams/`) is a design input the index itself calls "not requirements".
- **Proposed resolution:** REQ-040 A2 "a full puzzle of rating 1 comes after every warm-up whatever its id"; REQ-042 A3 "the classic square puzzle is among them"; REQ-047 A1 "every puzzle's title is its Finnish title".
- **Why it matters to the build:** three lines go RED or vacuous if the content is re-authored.

### F21 — DEPENDENCY · Should · `depends:` fields
- **Observation:** `depends:` mostly lists TYPEs; prose references are not reflected. REQ-014 → REQ-019/020; REQ-018 → REQ-016/037; REQ-021 → REQ-020; REQ-024 → REQ-025/050/TYPE-006; REQ-026 → REQ-030; REQ-030 → REQ-026/046; REQ-031 → TYPE-005/REQ-030; REQ-032 → REQ-009/029/030/031/033/034/049; REQ-033 → REQ-010; REQ-035 → REQ-025; REQ-037 → REQ-013/015/018/046; REQ-041 → REQ-045; REQ-042 → REQ-039; REQ-043 → REQ-014/047; REQ-045 → REQ-038/040; REQ-047 → REQ-009/039/043/049; REQ-048 → REQ-001/009/010/049; REQ-049 → REQ-009/047; REQ-050 → REQ-037/040; REQ-051 → REQ-045. No dangling ids, no cycles found.
- **Proposed resolution:** complete the fields.
- **Why it matters to the build:** WO ordering and the change-impact view (`views/trace.md`) read `depends:`, not prose.

### F22 — QUESTION · Should · REQ-032
- **Observation:** Rules: "a short how-to-play text" among the settings contents. No text, no language, no length.
- **Proposed resolution / question:** does the owner write or approve it (then it is Contract like REQ-009's note), or is it explicitly the build's with a bound ("at most N sentences, no hints")?
- **Why it matters to the build:** REQ-032 A1 passes with any text; the text is what a parent reads.

### F23 — QUESTION · Should · TYPE-006 / REQ-050 / REQ-026
- **Observation:** TYPE-006: "Retry returns Solved → New (the best time is kept)". REQ-050 shows New as a flat silhouette and jumps to "the next unsolved puzzle". A player who retries a solved puzzle and leaves it sees it as unsolved everywhere except the kept best time.
- **Question:** intended? If not, a retried puzzle needs a state (or a "solved before" flag) in TYPE-006.
- **Why it matters to the build:** REQ-050 A1 and A2 depend on the answer.

### F24 — QUESTION · Should · legal (outside world)
- **Observation:** "the game is GPL-3.0" appears only inside the index's open question. No REQ asks for a licence notice or source offer in the app or listing, nor states who owns and licenses the sounds, the chime, the confetti, any font, and the pictures (REQ-039 says who approves them, not who may distribute them). The Families rule "no unapproved third-party code" (REQ-048) makes third-party assets a store question too.
- **Question:** owner's answer or `declined` per topic.
- **Why it matters to the build:** an unasked legal obligation is a hard-stop domain hiding in silence.

### F25 — GAP · Should · requirements.md
- **Observation:** no "Asked before lock" table (store / channel, legal, platform, data, money, size, outside systems). Data and money are answered by REQ-010/REQ-001; channel (F3), platform (F7), legal (F24) and size are not.
- **Proposed resolution:** the capture session writes the table from the owner's answers to F3, F7, F24 and a size answer (or `declined`).
- **Why it matters to the build:** the v2 gate refuses the next re-lock without it; the build inherits the silence meanwhile.

### F26 — PROPOSAL · Note · UI REQs without firsthand use
- **Observation:** the lock receipt marks REQ-009, 013, 021, 031, 032, 035, 036, 037, 043, 049, 051 "UI numbers never used on a build". The index says "No phone or tablet playtest yet" and the lock happened "before a phone playtest".
- **Proposed resolution:** the owner acknowledges, once, that the first device playtest is a planned CHG round; no text change.

### F27 — AMBIGUITY · Note · REQ-020, REQ-033, REQ-035, REQ-051
- **Observation:** "soft sound" / "soft low tone", "about 10 % padding", "about 600 ms" (rule 1) next to "shown for 600 ms" (rule 3).
- **Proposed resolution:** calibrated as harmless; optionally make REQ-051 say 600 ms once.

### F28 — AMBIGUITY · Note · REQ-019
- **Observation:** abstract "locks at the nearest spot"; TYPE-004 chooses by score within a 0.16 window, not nearest. A4 "Six pieces locked in any arrangement … the seventh piece locks into it" reads as unconditional; it holds only within R with the matching turn and mirror.
- **Proposed resolution:** read A4 under the Statement; the abstract is a fair summary, not misleading.

### F29 — AMBIGUITY · Note · REQ-013
- **Observation:** "Cell width = the piece's turn diameter × scale + 12 dp padding"; height is implied by "fits in any turn". Checked: on 360 × 780 dp the height cap (16 % = 124.8 dp) bounds scale at 26.7; the square's cell is about 65 dp, so A1 (≥ 56 dp) holds.
- **Proposed resolution:** none needed; state height = width if the build asks.

### F30 — CONFLICT · Note · REQ-046 vs REQ-026 A1
- **Observation:** the solution overlay "stays on while the player browses puzzles until it is switched off"; REQ-026 A1 "A solved puzzle never shows where the pieces were." Test builds only.
- **Proposed resolution:** overlay hidden on solved puzzles, or accept.

### F31 — AMBIGUITY · Note · REQ-023
- **Observation:** "WHERE the device asks for reduced motion, the pieces do not pop" — no rule defines a pop.
- **Proposed resolution:** name it or drop it.

### F32 — Purity · Note · content format fields
- **Observation:** `kind`, `reviewedByHuman`, `title[lang]` are puzzle-file fields in locked Rules/Acceptance.
- **Proposed resolution:** acceptable because checkable; a format change is a CHG.

### F33 — EDGE · Note · REQ-050
- **Observation:** "the next unsolved puzzle after the current one" with wrap only stated for ‹ › (REQ-024).
- **Proposed resolution:** read as wrapping; A1 is unaffected.

### F34 — Housekeeping · Note · index, features, CHG-001
- **Observation:** titles "TangramNoAds"; "tools v0.4"; §3 "Shared types … draft" (true, F2). CHG-001: "Open point settled by default: the same title in both store languages" became a REQ-048 ASSUMPTION — a decision taken for the owner.
- **Proposed resolution:** the owner sees the Finnish-title default once; cosmetics at the next session.

### F35 — Oracle · Note · whole-app negatives
- **Observation:** REQ-001 A1, REQ-008 A1/A2, REQ-010 A2, REQ-046 A4 are "nowhere / no" lines; REQ-007 A1–A2 only name children REQs.
- **Proposed resolution:** build side: a dependency/permission scan plus an owner screen walk as evidence rows; do not build verifiers larger than the feature.

### F36 — EDGE · Note · REQ-012 / REQ-025 / TYPE-006
- **Observation:** REQ-025 saves "per piece: tray or board, position, turn, mirror"; TYPE-006 says a tray turn does not start the puzzle. Whether a New puzzle's tray turns persist across browsing is unstated.
- **Proposed resolution:** decide at build; either reading satisfies every acceptance line.

### F37 — Note · REQ-046, REQ-045
- **Observation:** the passcode is in a public GPL repository (a speed bump, not security); REQ-045 A1 ("opens on a mini puzzle whose tray holds three pieces") presumes the 3-piece mini sorts first among minis.
- **Proposed resolution:** accept.

### F38 — Consistency · Note · features.md, REQ-012
- **Observation:** #SoundToggle lists "picking, locking, returning and solving", REQ-033 adds "turned"; REQ-012's "puzzle's title is shown in the game's language" duplicates REQ-024's top-bar rule.
- **Proposed resolution:** tidy.

## Coverage of this review

- Checklist 1 Coverage: all 12 grill-me areas checked against the index §4 and the REQs; gaps found in platform floor (F7), legal (F24), how-to text (F22), the asked-before-lock record (F25). Content source and validity (area 13) is covered by REQ-038/039/042 but delegated (F1, F5).
- Checklist 2 Ambiguity: every Rules/Acceptance line scanned for the playbook and v2 terms (F8, F9, F10, F16, F27, F31).
- Checklist 3 Testability: all 100+ acceptance lines read for an honest oracle (F1, F5, F8, F9, F10, F18, F20, F35).
- Checklist 4 Consistency: REQ vs REQ and REQ vs TYPE (F4, F11, F12, F30, F38); abstracts checked one by one against the digest — none misleading.
- Checklist 5 Dependencies: `depends:` vs prose for all 47 locked REQs (F21); no dangling id, no cycle; TYPE status (F2).
- Checklist 6 Edge cases: kill/restore, update, unreadable state, interrupted gesture, rotation, language change, offline, all-solved, empty board (F13, F14, F15, F33, F36). Language change and offline are covered by REQ-047/REQ-010 as written.
- Checklist 7 Lifecycles: TYPE-006 transitions against REQ-025/030/034/046/050 (F13, F23).
- Checklist 8 Business events: none exist in this product; nothing to check.
- Checklist 9 Assumptions and open questions: index §5 and every ASSUMPTION line (F3, F26, F34).
- Checklist 10 Abstract honesty: all 47 locked abstracts vs their REQ (F28 only, not a Blocker).
- Checklist 11 Scope integrity: F19; withdrawn REQs and `idea` features confirmed as non-build units.
- Checklist 12 Purity: F6, F19, F32.
- Outside-world obligations: store (F3, F16), legal (F24), platform (F7), data and money (covered), size (unasked, F25).
- Not reviewed: `evidence/`, source bodies, `progress.md`, any chat, the first run's project (forbidden by the brief); the puzzle library and the prototype (design inputs, not Contract); the validator warning on 33 one-REQ leaves (a tree-shape opinion, not a Contract defect).

Every Blocker above must be resolved or explicitly accepted by the owner before the G1 yes; a `chg` verdict is executed on the capture side from the drafts in `capture-outbox/`, and no Blocker may be `defer`.
