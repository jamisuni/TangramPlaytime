---
status: proposed
targets: [REQ-025]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F19
---
# CHG — The one-profile rule stays; the storage-layout wish leaves the Contract

## Change
REQ-025 Rules: **"Progress is stored per player profile; the first version has exactly one profile. *(Agent proposal, confirmed by the owner's lock SRC-014; first inferred from SRC-011: several profiles on one tablet are a release-two candidate, and the storage layout should not prevent it.)*"** becomes **"The first version keeps progress for exactly one player."** The release-two note moves to the project's design inputs.

## Reason
Review-01 F19 (Should). "Storage layout" is a how (principle 2) and the layout wish serves a requirement that does not exist (principle 14; several profiles are out of scope in REQ-003 and the index). A locked rule with no acceptance invites speculative architecture.

## Impact
No acceptance line changes. Build side: removes an implicit instruction to design for multi-profile storage.
