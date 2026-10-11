# WO-001 #Locking — Test & Verify report

## (a) Channel table
| Channel | Baseline | Hashes (recomputed now) | Counts (tests/fail/err) | Result |
|---|---|---|---|---|
| JVM `:kernel:test` | run-log r2 (12:57) reused, not re-run | test 75aa0d7b9c01, product 8b68aaadae29 = r2 | LockEngineKitTest 14/0/0, GeometryKitTest 20/0/0, KernelScaffoldTest 1/0/0, scaffolding.GeometryScaffoldingTest 10/0/0, scaffolding.LockEngineScaffoldingTest 7/0/0 | PASS (result XML read, counts match the log) |
| JVM `:contracts:test` | r2 reused | same | ContractsSanityTest 2/0/0 | PASS |
| Device (dev / floor image) | none named by the WO at G3 ("JVM unit tests only"); no waiver needed | n/a | n/a | not owed |

Counted coverage = the 14 tests of LockEngineKitTest. Scaffolding (17 tests, marked, no token) is not counted.

## (b) Per-ID verdict (JVM channel)
Token grep: 20 occurrences, all in LockEngineKitTest.kt; the set is exactly {REQ-019.A1-A4, REQ-020.A1, REQ-022.A1}. No REQ-020.A2 token anywhere.
- REQ-019.A1 PASS: req019A1_aDropNearAnOutlineCornerPositionLocksThereExactly, req019A1_aDropNearANeighbourCornerPositionLocksThereExactly, req019A1_aPositionAnchoredOnlyAtTheWarmup4PinchCornerLocksThere, req019A1_theLockDistanceWidensToThe30dpFloor (exact centre within 1e-9, turn and mirror kept).
- REQ-019.A2 PASS: req019A2_aLockNeverOverlapsOrSticksOut_traps (four hand-checked traps), req019A2_overASweep_everyLockIsInsideAndOverlapsNothing (3 boards, all pieces x turns x mirrors, at least 100 locks required; pinned oracle plus an independent sampled-point check).
- REQ-019.A3 PASS: req019A3_aPieceWithNoOutlineCornerOrNeighbourInReachNeverLocks (4 poses, incl. hidden-anchor traps on 360-degree vertices and the 180-degree edge point, and one with a neighbour out of reach).
- REQ-019.A4 PASS: req019A4_theSeventhPieceLocksIntoTheHoleOfItsShape (5 arrangements incl. turned and mirrored covers, every piece taking its turn as the seventh).
- REQ-020.A1 PASS (seam half): req020A1_everyDropEndsLockedOnTheBoardOrGoesHome (sweep: each result is a real lock — inside, no overlap, a corner on an anchor, turn and mirror kept — or None; off-board drops and an out-of-reach drop give None; both outcomes required at least 100 times).
- REQ-022.A1 PASS: req022A1_aDifferentCoverBuiltLockByLockCountsAsSolved (two non-stored covers built through seven findLock calls; isSolved false before the 7th lock, true after).
- REQ-020.A2: out of WO-001 (moved to WO-003); correctly untokened.

## (c) Substance audit
- No gaming token: every tokened test asserts its criterion's meaning; no empty bodies, no proxies.
- Edges and "must nevers" covered: never overlaps or sticks out (A2 traps and sweep); no hidden anchor (A3); turn and mirror never changed or searched (asserted in the A1/A4 helpers, plus untokened guardrail theTurnAndMirrorAreNeverSearched, G-04); a miss is None (untokened guardrail aMissIsTheValueNoneNeverAnException, G-15); any exact cover counts (REQ-022.A1).
- Scaffolding folder: marked, zero REQ tokens.
- Observations, not defects: (1) Traps (a) and (b) use "if Locked then check" followed by assertSame(None), so the conditional does not weaken the test. (2) Lock animation and soft sound (REQ-019/020 rules) are UI, outside this WO. (3) REQ-022 is exercised through the two covers only; sufficient for A1.
- Failures: none. Product defects: none. Test defects: none. Coverage gaps within WO-001 scope: none.

## (d) Verifier runner
`PYTHONUTF8=1 python C:/GitHub/AI/SWDev/framework/skills/verifiers/run_verifiers.py --project C:/GitHub/AI/TangramPlaytime --wo WO-001`
Output, exactly: `verifiers: none found under .swdev/verifiers (discovery mode) — nothing to run`
No budget line was printed. No verifiers or positive controls exist yet (expected; the WO says so).

## (e) Handoff
- Scope: WO-001 verification, JVM channels, six in-scope IDs
- Inputs read: WO-001 (scope, channels), run log r2, LockEngineKitTest.kt, scaffolding token grep, REQ-019/020/022, TYPE-004, result XML counts
- Result path: C:/GitHub/AI/TangramPlaytime/reviews/WO-001-test-verify.md
- Status: forward
- Traceability delta: none
- Notes: r2 reused (hashes match, no re-run). Counts: :kernel 14 kit + 20 geometry + 1 + 10 + 7 scaffolding; :contracts 2; all 0 failures. Verifier runner: discovery mode, nothing to run, no budget line.
