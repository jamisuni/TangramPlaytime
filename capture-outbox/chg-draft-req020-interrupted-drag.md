---
status: proposed
targets: [REQ-020]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F15
---
# CHG — A drag that ends without a release is a missed drop

## Change
REQ-020 Rules, new bullet: **"A drag ended by anything other than the player's release — an incoming call, a switch to another app, the tablet turning (REQ-036), the system ending the app — is a missed drop: the piece returns to its tray cell, with no sound and no animation if the screen is not visible."**
REQ-020 Acceptance, new **A3**: **"A drag interrupted by the app going to the background leaves the piece in its tray cell when the app returns."**

## Reason
Review-01 F15 (Should). REQ-014 governs a release and REQ-020 a drop; REQ-012 A2 demands every piece be in the tray, on the board or dragged, and REQ-036 A2 keeps "every placed piece" — the piece in the hand at interruption time has no defined home.

## Impact
REQ-012.A2, REQ-036.A2, new REQ-020.A3. Build side: lifecycle handling of the active drag in the play WO.
