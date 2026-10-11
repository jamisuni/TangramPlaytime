---
status: proposed
targets: [REQ-046]
origin: review
sources: []
created: 2026-10-11
raised_in: G1 review-01 F6
---
# CHG — REQ-046 speaks Android: no "page", no design sheets, and "release build" is defined

## Change
REQ-046 Rules, second bullet: **"After the right passcode the aid stays unlocked until the page or app is restarted."** becomes **"After the right passcode the aid stays unlocked until the app is restarted."**
REQ-046 Rules, fifth bullet: **"The DEV button is hidden on a solved puzzle and in the design sheets (`../Spec/ui/`)."** becomes **"The DEV button is hidden on a solved puzzle."**
REQ-046 Rules, new bullet: **"A release build is any build uploaded to a store track (including a closed or internal test track); a test build is a build installed directly on a device. The passcode is a speed bump for testers, not a secret."**

## Reason
Review-01 F6 (Should). The clauses were written for the web prototype; an Android build has no page and no design sheets, so the rule has no oracle. If the Play closed test (review F3) uses the artifact later promoted to production, "test build" and "release build" must not overlap, or A4 is ambiguous.

## Impact
REQ-046.A4 (gains a definition), REQ-046 rules 2 and 5. Build side: the build-variant split in the #DevTools WO.
