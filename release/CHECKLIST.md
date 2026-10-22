# The owner's release checklist — Tangram Playtime on Google Play

Twelve steps, in order. Each says where (the console page or the command) and which file to paste from. They are all the owner's: the build has performed none of them, and no key, password, account detail or personal e-mail address goes into this repository. Console pages are named as the console names them today; if a name differs, look for the page that asks the same question. Commands run from the repository root, in Git Bash.

## 1. Play and mark the puzzles

- **Where:** the debug build on your phone or tablet (`./gradlew :app:assembleDebug`, or Android Studio's Run), then the mark command.
- **What:** solve each puzzle you want in the first release and look at its picture. Then mark it: `PYTHONUTF8=1 python tools/mark_puzzles.py mark <id> …` (`unmark <id> …` takes a mark back).
- **Until:** `PYTHONUTF8=1 python tools/mark_puzzles.py status` says `release-ready: yes`: 20 or more marked puzzles, in four themes or more, with the classic square, a three-piece mini and the four warm-ups among them.
- **Then:** commit the marks (only the `Tangrams/*.json` files you marked change).

## 2. Make the repository public

- **Where:** github.com/jamisuni/TangramPlaytime › Settings › General › Danger Zone › Change repository visibility (as GitHub names it).
- **First:** look at what the repository holds. Its history carries your git name and e-mail address. Push `main`, so that `PRIVACY.md` is on GitHub.
- **Why:** the repository hosts the privacy page, and the settings screen's licence line names it as the GPL-3.0 source.
- **Check:** open https://github.com/jamisuni/TangramPlaytime/blob/main/PRIVACY.md in a private window, logged out. The privacy page must show.

## 3. Create the Google Play developer account

- **Where:** the Play Console sign-up (play.google.com/console).
- **What:** pay the one-time registration fee; choose a personal or an organisation account; complete the identity check.
- **Note down** (outside the repository): the account type and the date the account was created. Step 10 depends on both.

## 4. Create the app

- **Where:** Play Console › Home › Create app.
- **What:** app name `Tangram Playtime` (from `release/listing/en-US/title.txt`); default language English (United States) – en-US; app or game: Game; free or paid: Free. Accept the declarations the page lists.

## 5. Signing

- **Where:** Play Console › the app › App integrity (as the console names it) for Play App Signing; Android Studio for the upload key.
- **What:** use Play App Signing: Google keeps the key that signs the app for the store. Make an upload key with Android Studio (Build › Generate Signed App Bundle or APK › Create new, as Android Studio names it).
- **Keep** the upload key's key store file and its passwords outside the repository, back them up, and never commit them.

## 6. Build and sign the AAB

- **Where:** Android Studio › Build › Generate Signed App Bundle or APK › Android App Bundle (as Android Studio names it), with the release variant and the upload key from step 5.
- **The check:** the release bundle build refuses until step 1 is done (`:content:checkReleasePuzzles`, the same check `./gradlew :app:bundleRelease` runs), and its message names what is missing.
- **The file:** the wizard writes the signed AAB to `<the folder you choose>/release/app-release.aab`, under the destination folder you pick in the wizard.
- **Upload only the AAB written by a build that ended in BUILD SUCCESSFUL, right after it.** After a refused build, an older AAB from an earlier build can still be in that folder: the store build empties its own output folder in the repository before the check, but never the wizard's destination folder.

## 7. App content

- **Where:** Play Console › the app › Policy and programs › App content (as the console names it).
- **From:** `release/console/app-content.md`, section by section: the privacy policy URL, ads, app access, content rating, target audience and content (the Families policy; Teacher Approved where offered), data safety, and the other declarations.

## 8. Main store listing

- **Where:** Play Console › the app › Grow users › Store presence › Main store listing (as the console names it).
- **Texts:** paste the app name, the short description and the full description from `release/listing/en-US/` (`title.txt`, `short_description.txt`, `full_description.txt`). Each file's text is the field.
- **Finnish:** Manage translations › add your own translation › Finnish (fi-FI) (as the console names it), pasted from `release/listing/fi-FI/`.
- **Graphics, from `release/pictures/`:** the app icon `icon-512.png`, the feature graphic `feature-graphic.png`, and the phone and tablet screenshots from `screenshots/`.
- **Store settings** (as the console names it): category Games › Puzzle; a contact e-mail address of your choice (it shows on the listing, and the privacy page points to it). See `release/console/app-content.md` section 9.

## 9. Pricing and countries

- **Where:** the app's pricing page, and the countries or regions of the release track (as the console names them).
- **What:** the app stays free. A free app can never be changed to a paid one. Choose the countries or regions to offer it in.

## 10. Testing

- **If** the account is a personal account created after 13 November 2023: run a closed test with at least 12 testers opted in for 14 days before production (Play Console › Test and release › Testing › Closed testing, as the console names it; `capture-outbox/chg-draft-req048-store-channel.md`). Then take the console's step to production access, as it names it.
- **Otherwise:** an internal test is optional.

## 11. Upload and roll out

- **Where:** the release track (the closed test from step 10, or Production) › Create new release (as the console names it).
- **What:** upload the AAB from step 6, add the release notes, then review the release and roll it out. Google reviews it before it is live.

## 12. Look at the live listing and sign

- **Where:** the live Google Play page of "Tangram Playtime" (on a phone, or at play.google.com), then `owner-evidence.md`.
- **What:** look at the listing, then sign the three rows REQ-001 A2, REQ-048 A1 and REQ-048 A2: the date and your name.

Later uploads raise `versionCode` (in `app/build.gradle.kts`): Play accepts an upload only with a `versionCode` it has not seen before.
