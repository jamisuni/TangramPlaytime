---
status: proposed
targets: [REQ-006]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F7
---
# CHG — The platform floor is a rule the owner signed: Android 8.0 and later

## Change
REQ-006 Rules, new bullets after **"Two layout classes (TYPE-007)."**:
- **"Supported Android versions: Android 8.0 (API 26) and later."** (owner, 2026-10-11: "android 8 is more than enough, can go way higher if it simplifies development" — the build keeps 8.0, `decisions.md` 2026-10-11 row 5; raising the floor later is a value change to this bullet)
- **"Smallest supported screen: 320 dp wide in the Phone class; every acceptance line that names a device size is also met at this width."** (derived from the REQ-013 arithmetic; the owner has not objected)
REQ-006 Out of scope: **"Phone landscape; platforms other than Android."** becomes **"Phone landscape; platforms other than Android; Android versions below the floor above."**

## Reason
Review-01 F7 (Should). No REQ names a minimum Android version or screen; the build chose minSdk 26 alone (principle 8, no silent scope). The outside-world topic "platform floor" was never asked before lock (lock gate v2, asked-before-lock table). The owner's answer is in `src-draft-g1-owner-answers.md`.

## Impact
REQ-006.A1 (gains a device class to run on: the API 26 floor emulator is a verification channel), REQ-013.A1, REQ-035.A1, REQ-036.A1 (a second width to check). Build side: `minSdk 26` and the store's device filter become Contract-backed.
