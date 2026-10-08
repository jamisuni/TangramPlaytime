# capture-outbox/ — drafts for the capture side (never write into the collection)

Placeholder — keep this file. Drop here, one concern per file:
`chg-draft-<slug>.md` (a change to a locked item, SwReqCollector's
`templates/change.md` shape without an id, `origin: build`) and
`src-draft-<slug>.md` (a verbatim owner statement as draft evidence).
Shapes and rules: `C:/GitHub/AI/SWDev/framework/templates/capture-outbox/`.
A capture session ingests the folder, assigns the ids over there and
empties it; until then `trace-check` reports **lock behind build** at every
WO close and the dependent line stays `implemented (unverified)`.
