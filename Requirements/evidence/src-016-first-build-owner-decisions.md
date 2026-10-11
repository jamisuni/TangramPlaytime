# SRC-016 — The owner's decisions during the first build (verbatim)

Captured 2026-10-08 from `C:/GitHub/AI/TangramNoAds/progress.md` (the
first build under SWDev, 2026-10-01 … 2026-10-05). The requirement
collection itself was not changed during that build (git: `Requirements/`
identical from commit 10ccac4 to 288d8ab). These are the owner's words
that bear on requirements or on forks the build had to settle; everything
else the owner wrote was "plz continue" and pauses. Each quote is exact;
the context lines are the capture session's.

## 2026-10-01 20:12 — technology (governance row 5)

> A. Kotlin + Jetpack Compose (native) is ok if can build it my computer. Anyway questions OK but need to jump Claude Code windows terminal soon for builds. And we do not build today as running out of tokens / time.. but get everything here ready with you

*Context:* the owner picked option A of a stack question. The first build
also ported the prototype's locking logic (ADR-001) on the owner's kickoff
("stack is DECIDED: Kotlin + Jetpack Compose native, port the prototype's
logic").

## 2026-10-02 — G1 acceptance of the fresh-eyes review (38 findings)

> (F1) yes plz lock requirements and continue with all reqs. (F2) OK good plan (F3) OK (F4) OK + rest OK .. YES

*Context:* F1 named `req_types.md` v0.2 as Contract (TYPE-001, 003–007
approved); F2 (REQ-012 vs REQ-016/018/013/020/025/026/034) and F3
(TYPE-007 vs REQ-035/036/006) were accepted *under the reviewer's proposed
ASSUMPTIONs*; "rest OK" accepted F4–F37 the same way. None of those
resolutions reached the collection as CHG deltas (first run, CA-3).

## 2026-10-05 — WO-008, how play time counts (an idea, not a requirement)

> "should today's and total play time include time spent browsing and in settings, or only time on a puzzle?" can be total time (if thats easier) up to you really, but maybe some unactivity checking makes sence so that if havent done anything for minute then stop counting time until next press etc.

## 2026-10-05 — a future idea (not today's requirement)

> plz add as potential future thing, not todays requirement... maybe 'easy' mode could be added with some 'safety robe' spinning around blocks so that when puzzle is finished that robe makes nice round around thru all blocks. Idea would be that it kind of tells if block is wrong place is robe breaks somewhere between two blocks

> thru block.. so block has robe passing thru it already when dropping it

## 2026-10-05 — WO-009, four release forks (the owner's answers)

> "Where should Tangram be published first?"="Google Play (Recommended)", "How many new puzzles should I draft for the first release (it needs at least 20, today there are 13)?"="12 new, 25 total", "Every puzzle must be marked 'reviewed by a person' before release, and only you may do that. How do you want to mark them?"="A mark command (Recommended)", "The app has no launcher icon yet and the store needs graphics. Who makes them?"="AI proposes, you pick"

## 2026-10-05 — the store title (changes locked REQ-048)

> "Google Play rejects 'free' in an app title, but the approved requirement fixes the title as 'Tangram, absolutely free'. What should we do?"="Change the title (Recommended)"

> "Which store title should Tangram have on Google Play? (The 'absolutely free, no ads' promise moves into the short description and stays in the app.)"="No seven, no classic.. as both of those we going to break someday to have harder level with non-standard pieces / etc. 'Tangram Playtime'"

*Context:* the first build recorded this as DA-154 (a row-14 owner
decision) and parked the re-lock as CA-12; it never became a CHG. The
same words also carry a second future idea: a harder level with
non-standard pieces (see #PieceSets).

## 2026-10-05 — the launcher icon

> "Which launcher icon should Tangram Playtime use? (Pictures: release/icon-options/option-A-512.png, -B-, -C-)"="A: coloured square"

*Context:* option A's source files are kept as a design input
(`Spec/icon/`, DI-9).
