# release/ — the store kit for Google Play

Everything the Play Console asks for, prepared as files the owner pastes or uploads. The build performs none of the console steps: signing, the developer account and every console entry are the owner's. **Start with [`CHECKLIST.md`](CHECKLIST.md)**: it says what to do, in order, where, and from which file.

This folder is not a Gradle module; nothing in it is built into the app.

| File | What it is | Where it goes in the Play Console |
|---|---|---|
| `CHECKLIST.md` | The owner's release checklist, twelve steps in order | — (read it first) |
| `listing/en-US/title.txt` | The app name, "Tangram Playtime" (at most 30 characters) | Create app › App name; Main store listing › App name |
| `listing/en-US/short_description.txt` | The short description, with the promise "absolutely free — no ads, nothing to buy" (at most 80 characters) | Main store listing › Short description |
| `listing/en-US/full_description.txt` | The full description (at most 4000 characters) | Main store listing › Full description |
| `listing/fi-FI/` (the same three files) | The Finnish translation: the title stays "Tangram Playtime"; the texts are reference translations | Main store listing › Manage translations › Finnish (fi-FI) (as the console names it) |
| `console/app-content.md` | The answers for the App content pages: privacy policy, ads, app access, content rating, target audience, Teacher Approved, data safety, the other declarations; the category and the contact | App content (as the console names it); Store settings |
| `pictures/icon-512.png` | The store icon, 512 × 512, made from the launcher icon's source (DI-9) by `tools/make_icon.py` | Main store listing › App icon |
| `pictures/feature-graphic.png` | The feature graphic, 1024 × 500, 24-bit PNG without alpha | Main store listing › Feature graphic |
| `pictures/feature-graphic.svg` | The feature graphic's source. Re-make both files with `python tools/make_feature_graphic.py` | — (not uploaded) |
| `pictures/screenshots/` | Phone and tablet screenshots from the release build, added after the owner's marks | Main store listing › Phone screenshots; the tablet screenshots (as the console names them) |
| [`../PRIVACY.md`](../PRIVACY.md) | The privacy policy page, at the repository root | Its address goes in App content › Privacy policy |

**The listing files.** Each file holds one field. Its text is the field, ending with a single line break that is not part of it: copy the text, not the file's last line break.

**English and Finnish.** English is the Contract wording. The Finnish texts are reference translations for the owner to proof-read (`review-notes.md`).

**What the console shows is the owner's evidence.** No test can see the live listing. When it is live, the owner looks at it and signs the three rows in `owner-evidence.md` (the checklist's last step).
