---
status: proposed
targets: [REQ-025]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F14
---
# CHG — State is saved at every change, and unreadable saved state never stops the game from starting

## Change
REQ-025 Rules, **"The state survives closing the app."** becomes **"The state is saved at every lock, return to tray, restart, retry and reset, so it survives closing the app and the system ending the app at any moment."**
REQ-025 Rules, new bullet: **"IF the saved state cannot be read (damaged, or written by a version this build does not understand), THEN the game starts as on a fresh install (every puzzle New, all times zero, settings default) and never fails to start."**
REQ-025 Acceptance, new **A4**: **"With the saved state replaced by unreadable content, the game starts and shows the first puzzle as New."**

## Reason
Review-01 F14 (Should). REQ-003 A2 ("never loses its state") is ambiguous under process death because only navigation saves (REQ-024); the "must never" side of a corrupt or older save file is absent. Both edges are routine on Android.

## Impact
REQ-003.A2, REQ-025.A1, new REQ-025.A4. Build side: persistence timing and a read-failure path in the browsing WO.
