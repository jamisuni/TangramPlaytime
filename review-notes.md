# Review notes — TangramPlaytime

**Status:** Placeholder — started at the first WO close; append-only,
AI-written; **non-blocking**.

> Things to look at when you play the build — never decisions. Strings and
> translations to proof-read, wording, icon and colour choices already made
> under an `ai` row, screens worth a second look. Nothing here waits for an
> answer and nothing here is a checkpoint item (those are the at-most-five
> product-visible decisions in the WO, each with a default —
> `gates-and-autonomy.md` §8). Correct an entry by editing the artifact or
> filing a capture-side `DEF`/`CHG`; the AI never asks about these in chat.
> *(v0.18: 52 translated strings sat in checkpoints for nine WOs and were
> never reviewed — a list is read when the owner has the build in hand.)*

| Date | WO | What to look at | Where (screen / file) | Decision row |
|---|---|---|---|---|
| 2026-10-11 | WO-003 | **The corner pulse in a mini puzzle** fires on every release that sends a piece home — a drop in the middle, outside the board, *and* a deliberate drop back on the tray (REQ-051 cites all of REQ-020). Play the two minis and see whether the tray-drop pulse feels like a nag; the narrower reading (only a drop that found no place) is a one-line change plus a chg-draft. Also look at: the board's colours (checkpoint item 1, DI-2 palette), the S / M / L marks' size and tint, the badge next to a placed parallelogram, the solve pop (size-neutral) and the picture fade. Strings this WO adds (en Contract / fi reference) are listed by the implementers at close in this file. **Pieces are flat fills without the 2 dp white edge the layout spec drew** — say so if you want a hairline back (decisions.md, TASK-012 row). | Play screen, any mini puzzle | decisions.md 2026-10-11 (WO-003 plan rows) |
| 2026-10-12 | WO-003 | **Strings this WO adds** (en = Contract, fi = reference): `size_mark_small` S / S, `size_mark_medium` M / M, `size_mark_large` L / L (REQ-043, REQ-047: the same letters in both languages). The puzzle title in the top bar is the file's `en` title unless the device's primary language is Finnish. No other text on the Play screen this WO. | `app/src/main/res/values/strings.xml`, `values-fi/strings.xml` | decisions.md 2026-10-12 (TASK-015 row) |
| 2026-10-12 | WO-003 | **The flip badge's touch area** (code review F1). In the tray the badge now sits a little lower inside the parallelogram's cell (its centre 21 dp in from the cell's right edge and 21 dp down from its top), and a touch on the parallelogram itself turns or drags it even where the badge's 60 dp area reaches over it; the white disc mirrors it. On the board the same rule holds: a tap on a piece you can see acts on that piece, the disc mirrors the parallelogram. Try turning and mirroring the parallelogram in a full puzzle on a phone; if the disc should move elsewhere, it is one line. | Play screen, the parallelogram's tray cell and the badge next to a placed parallelogram | decisions.md 2026-10-12 (fix batch, reviewer note on F1) |
| 2026-10-11 | WO-002 | The thirteen shipped puzzles as the library lists them: 2 mini (Pyramid, Window), 4 warm-ups, then House, Arrow (rating 2), Cat, Chocolate bar, Sailboat (3), Mountain, Gift box (4). Their pictures on the preview sheets; their Finnish titles are reference translations (Mini: Pyramidi, Mini: Ikkuna, Lämmittely 1–4, Talo, Nuoli, Kissa, Suklaalevy (suorakulmio), Purjevene, Vuori, Lahjapaketti) — nothing to answer; `reviewedByHuman` stays `false` until you solve one and like its picture (owner rule 7) | `Tangrams/previews/*.png`, `Tangrams/*.json` `title.fi` | decisions.md 2026-10-11 (WO-002 scope, checkpoint item 1) |
