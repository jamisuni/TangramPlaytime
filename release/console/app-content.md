# Tangram Playtime — the Play Console answers

The answers the owner enters in the Play Console (`release/CHECKLIST.md`, steps 7 and 8). Nothing here has been entered by the build. Each answer names its source. The console words its own pages and questions; where this file is not sure of a name it says "as the console names it". An outcome the console decides (the rating) is written as expected, never as promised.

Sources: the requirements in `Requirements/reqs/` (REQ-001, REQ-002, REQ-010, REQ-048, REQ-049) and `decisions.md` 2026-10-22, "The console answers: no data, no ads, rated for all ages, audience from 6 with the Families policy, Teacher Approved where offered." (R10).

## 1. Privacy policy

Privacy policy URL:

https://github.com/jamisuni/TangramPlaytime/blob/main/PRIVACY.md

It works once the repository is public and `PRIVACY.md` is on `main` (checklist step 2). Source: REQ-048 rule 3 and its ASSUMPTION "a page in the project's repository"; `decisions.md` 2026-10-22 (R9).

## 2. Ads

> Does your app contain ads?

No, my app does not contain ads

Source: REQ-001 (no advertising of any kind); REQ-048 rule 6.

## 3. App access

All functionality is available without special access (choose the option that says so, as the console names it). There is no login, no account and nothing to unlock.

Source: REQ-010 (no accounts, no network); REQ-001 rule 3 (nothing locked).

## 4. Content rating

Fill in the rating questionnaire (as the console names it):

- E-mail address for the rating: one of the owner's choice (it is not written in this repository).
- Category: a game (as the questionnaire names that category).
- Violence, fear, sexuality, language, controlled substances, gambling, crude humour: No to every question.
- Users interact or exchange content with each other: No.
- Shares the user's location with other users: No.
- Digital purchases: No.
- Unrestricted internet access (a web browser or search engine): No.

Expected outcome, not promised (the questionnaire decides): the all-ages rating in each rating system, for example PEGI 3 and ESRB Everyone.

Source: REQ-048 rule 5 (completed before release); REQ-001 (no purchases); REQ-010 (no network, no data); the game is a puzzle game with no other players (REQ-002).

## 5. Target audience and content

Target age groups: 6–8, 9–12, 13–15, 16–17, 18 and over. Not "5 and under".

- Why from 6: REQ-002's players are aged 8 and older. Play has no group that starts at 8; its group "6–8" is the one that holds 8-year-olds.
- Because the groups include children under 13, the Families policy applies (REQ-048 rule 6): the game has no ads, no third-party advertising or analytics code, no location permission and collects no data (REQ-001, REQ-010).

Source: REQ-048 rule 6; REQ-002 statement; `decisions.md` 2026-10-22 (R10); checkpoint item 4 of WO-009.

## 6. Teacher Approved

Opt in where the console offers it (as the console names the programme's opt-in). If the console offers no opt-in, there is nothing to enter.

Source: REQ-048 rule 7.

## 7. Data safety

> Does your app collect or share any of the required user data types?

No

With this answer the console skips the data types. The store listing's Data safety section then shows "No data collected" and "No data shared".

Source: REQ-048 rule 4; REQ-010 makes it true (no network, no permissions, everything stays on the device); REQ-049.

## 8. The other declarations

- News app: No.
- Health features: none.
- Financial features: none.
- Government app: No.
- Advertising ID, where the console asks: No. The game declares no permissions at all (REQ-010 rule 2), and it has no advertising.
- Any other declaration the console lists: the game has none of the features it asks about. If one is unclear, the answer follows from REQ-010: no network, no permissions, no data.

## 9. Store settings

- App category: Games › Puzzle.
- Contact e-mail: an address of the owner's choice. It is shown on the listing and is the contact `PRIVACY.md` names. It is not written in this repository.

Source: `tasks.md` § WO-009, S-59; `decisions.md` 2026-10-22 (R9).
