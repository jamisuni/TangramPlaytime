---
status: proposed
targets: [REQ-033]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F18
---
# CHG — REQ-033 A1 is a device check of five named actions, not a whole-app negative

## Change
REQ-033 Acceptance A1: **"With sound off, no action makes a sound or a haptic tick."** becomes **"With sound off, picking up, turning, locking and returning a piece and solving a puzzle produce no audible sound and no vibration on the test device (owner's device check, recorded as an evidence row)."**
REQ-033 Acceptance, new **A2**: **"With sound on, each of the same five actions produces its sound, and a lock produces a haptic tick where the device supports it."**

## Reason
Review-01 F18 (Should). The framework's own harvest (principle 15, oracle-cost rule, hard-stop 6) records that this exact line shape became a JVM gate, scans, probes, 28 decision rows and a product change in the first run. Naming the five actions and the oracle now avoids a verifier larger than the feature.

## Impact
REQ-033.A1 (reworded), new REQ-033.A2. Build side: one device evidence row per state in the settings WO.
