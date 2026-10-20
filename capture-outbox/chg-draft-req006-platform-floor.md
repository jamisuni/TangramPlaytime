---
status: proposed
targets: [REQ-006]
origin: review
sources: [src-draft-g1-owner-answers.md, src-draft-android-12-floor.md]
created: 2026-10-11
amended: 2026-10-19 (the floor raised to Android 12 by the owner)
raised_in: G1 review-01 F7
---
# CHG — The platform floor is a rule the owner signed: Android 12 and later

## Change
REQ-006 Rules, new bullets after **"Two layout classes (TYPE-007)."**:
- **"Supported Android versions: Android 12 (API 31) and later."** (owner, 2026-10-19: "Lets move to Android 12 as oldest supported." — `src-draft-android-12-floor.md`; it raises the G1 answer of 2026-10-11, "android 8 is more than enough, can go way higher if it simplifies development", under which the build had kept 8.0, `decisions.md` 2026-10-11 row 5. *Amended 2026-10-19: the bullet read "Android 8.0 (API 26) and later".* The build applies it from the work order after WO-010.)
- **"Smallest supported screen: 320 dp wide in the Phone class; every acceptance line that names a device size is also met at this width."** (derived from the REQ-013 arithmetic; the owner has not objected)
REQ-006 Out of scope: **"Phone landscape; platforms other than Android."** becomes **"Phone landscape; platforms other than Android; Android versions below the floor above."**

## Reason
Review-01 F7 (Should). No REQ names a minimum Android version or screen; the build chose minSdk 26 alone (principle 8, no silent scope). The outside-world topic "platform floor" was never asked before lock (lock gate v2, asked-before-lock table). The owner's answer is in `src-draft-g1-owner-answers.md`.

## Impact
REQ-006.A1 (gains a device class to run on: the floor emulator — API 31 from the work order after WO-010, API 26 until then — is a verification channel), REQ-013.A1, REQ-035.A1, REQ-036.A1 (a second width to check). Build side: `minSdk 31` (from `minSdk 26`) and the store's device filter become Contract-backed.
