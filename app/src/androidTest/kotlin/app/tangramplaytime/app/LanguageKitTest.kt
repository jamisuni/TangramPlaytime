// Acceptance kit (WO-006, TASK-030): the en / fi texts in the real app (REQ-047; tasks.md § WO-006 S-36 and seam note 5;
// decisions.md 2026-10-17 "One language rule for strings and titles" (Q7, as amended), "REQ-047 A1 carries `implemented
// (unverified)`"; capture-outbox/chg-draft-req047-english-contract.md; G-10).
// - G-10: no test asserts a Finnish word. Every comparison is with a resource as Android resolves it for a language
//   (`createConfigurationContext` on the target context) and with the puzzle file's own `title.fi` / `title.en`, read from the
//   bundle through `:content`. The comparisons discriminate: the test shows, on the resources themselves, that texts it
//   compares differ between `fi` and `en`, and that at least one of them is on screen.
// - Where it runs (seam note 5; the per-image table; decisions.md 2026-10-17 "The floor routes, as measured — amends the Q7
//   and Q8 rows" and "The device kit's open readings"):
//   - the `cmd locale` legs carry `@SdkSuppress(minSdkVersion = 33)`: they run on the two API 37 images, where the kit sets the
//     device locale (`cmd locale set-device-locale`) and the app's locale list (`set-app-locales`) itself, and they are
//     filtered out on the floor `Phone_API_26` (absent from its XML, not skipped);
//   - the own-locale leg runs on every image in every run. In the ordinary runs (`en-US`) it is the "in English otherwise"
//     check. On the floor the channel line sets the device's list by the measured root route before Gradle and names it in
//     `kitDeviceLocales`, three runs: `fi-FI` (the floor's REQ-047 A1 evidence), `sv-SE` and `sv-SE,fi-FI` (its A2
//     evidence, the second the adversarial list). Without the argument it requires an English device (the other app classes
//     assume English: `DevAidAppKitTest.kitAssertEnglish`).
// - The screens that exist (decisions.md 2026-10-17 "Scope of WO-006 …"): the Play screen New and In progress, the solved view,
//   the grid overview, and the DEV pill and prompt (the debug app; their strings come from `play/src/debug/res`, S-29).
// - The app's controller is one per process and shared by every class of the run: each test leaves it, and the device and
//   app locales, as it found them (seam note 3; `KitApp.restoreRule()`: the locales are restored through the shell alone,
//   bounded and waiting on nothing of the app, before the activity is closed, decisions.md 2026-10-18 "Worker wins three
//   contests", part 3).
package app.tangramplaytime.app

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.play.PlayTags
import app.tangramplaytime.play.devaid.DevAidTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguageKitTest {

    @get:Rule(order = 0)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val kit = KitApp(composeRule)

    @get:Rule(order = 1)
    val kitRestore: TestRule = kit.restoreRule()

    @SdkSuppress(minSdkVersion = 33)
    @Test // REQ-047.A1
    fun withTheDeviceInFinnishEveryTextShowsItsFinnishResourceAndTheTitleItsFinnishTitle() {
        // REQ-047 A1 (the mechanism; the line stays `implemented (unverified)` on the pending English-Contract draft, decisions.md
        // 2026-10-17): "With the device in Finnish, the top bar … and the buttons show Finnish; the Cat puzzle is titled …".
        // Statement: every text, including puzzle titles, in Finnish WHEN the device language is Finnish. Oracle (Q7 row; S-36),
        // with the device locale `fi-FI` set through `cmd locale` (seam note 5): on every screen that exists, each text shown is
        // one of the texts resolved for `fi` (the `:app` and `dev_aid_` resources, the counter, the titles' `title.fi`), the
        // title is the open puzzle's `title.fi` from the bundle, and at least one compared text's `fi` and `en` resources differ.
        kit.start("REQ-047 A1, the device in Finnish")
        kit.setDeviceLocales("fi-FI", "the device in Finnish")
        assertScreensFollow("fi", "the device locale fi-FI")
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test // REQ-047.A2
    fun withTheDeviceInSwedishEveryTextIsInEnglish() {
        // REQ-047 A2: "With the device in Swedish, everything is in English." Rule: "any other device language gets `en`". Oracle
        // (Q7 row; S-36), with the device locale `sv-SE` set through `cmd locale`: on every screen that exists, each text shown is
        // its `values/` resource (or the counter, or a title's `title.en`), the title is the open puzzle's `title.en`, and at least
        // one compared text's `en` and `fi` resources differ.
        kit.start("REQ-047 A2, the device in Swedish")
        kit.setDeviceLocales("sv-SE", "the device in Swedish")
        assertScreensFollow("en", "the device locale sv-SE")
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test // REQ-047.A2
    fun withTheLocaleListSwedishThenFinnishEverythingIsStillInEnglish() {
        // REQ-047 A2's adversarial path (the TASK-030 brief; the 2026-10-12 TASK-015 row's flagged mix): with the list
        // [sv-SE, fi-FI], Android alone resolves strings to Finnish (the first available) while the title follows the first
        // locale (English). Q7 row: "the rest of the locale list is never consulted, so a device set [sv, fi] shows English
        // everywhere"; S-36: `AppLanguage.of(first)`. Oracle: with the app's locale list `[sv-SE, fi-FI]` set through
        // `cmd locale set-app-locales` (the route seam note 5 names; ruling 6 of the open-readings row: the first locale S-36
        // reads is the same), every screen is in English as in the Swedish leg. On the floor this leg is filtered; its case runs
        // there as the own-locale leg with the device list `sv-SE,fi-FI` set by the root route.
        kit.start("REQ-047 A2, the list [sv-SE, fi-FI]")
        kit.setAppLocales("sv-SE,fi-FI", "the app's locale list [sv-SE, fi-FI]")
        assertScreensFollow("en", "the locale list [sv-SE, fi-FI]")
    }

    @Test // REQ-047.A1 REQ-047.A2
    fun theTextsFollowTheDevicesOwnFirstLocale() {
        // REQ-047 statement: "in Finnish WHEN the device language is Finnish, and in English otherwise". The leg sets nothing: it
        // reads the device's own locale list (`Resources.getSystem()`; the activity's own configuration is the app's rule, S-36).
        // - With `kitDeviceLocales` (one locale or a comma-separated list, set on the floor by the measured root route) it first
        //   asserts that the device's list starts with the locales named, in that order, so a route that did not take fails
        //   loudly instead of passing as English. Under `fi-FI` this is the floor's A1 evidence; under `sv-SE` and the
        //   adversarial `sv-SE,fi-FI` its A2 evidence.
        // - Without it the device must be English (the ordinary runs; the other app classes assume English).
        // Then S-36's rule for that list (Q7 row): a Finnish first locale → the `fi` resources and `title.fi`; anything else → the
        // `values/` resources and `title.en`, compared on every screen as in the `cmd locale` legs.
        kit.start("REQ-047, the device's own locale list")
        val device = kit.deviceLocales()
        val named = kit.channelDeviceLocales()
        if (named != null) {
            assertTrue(
                "the device's locale list ${device.toLanguageTags()} starts, in order, with the locales the channel line set by its root route ($named, `kitDeviceLocales`)",
                kit.localesStartWith(device, named),
            )
        } else {
            assertEquals(
                "without `kitDeviceLocales` this run is an ordinary one, on an English device (seam note 5); the list is ${device.toLanguageTags()}",
                "en", device[0].language,
            )
        }
        if (kit.sdk >= 33) {
            assertEquals("precondition: no app locale list is set for $KIT_APP_PACKAGE (S-37 resets it), so the app sees the device's list", "", kit.appLocaleTags())
        }
        val language = if (device[0].language == "fi") "fi" else "en"
        assertScreensFollow(language, "the device's own list ${device.toLanguageTags()}")
    }

    // ── The screens ─────────────────────────────────────────────────────────────────────────────────────

    /**
     * Every screen that exists, each checked against [language]'s texts: the Play screen New (a full puzzle, the DEV pill on
     * it), In progress (one piece placed through S-2), the solved view (solved through S-2), the grid overview, and the DEV
     * sheet (the prompt, or the menu where this process's aid is already unlocked: either holds only `dev_aid_` texts). The fixture puzzle is the first New full puzzle whose
     * `title.fi` and `title.en` differ, where one exists (so the title comparison discriminates too).
     */
    private fun assertScreensFollow(language: String, leg: String) {
        val world = kit.languageWorld(language)
        val discriminating = kit.languageOnly(language)
        assertTrue("fixture: some `:app` or `dev_aid_` resource reads differently in `fi` and `en`, so the comparison can tell them apart", discriminating.isNotEmpty())
        val p = runCatching {
            kit.firstNew(PuzzleKind.FULL, "$leg: a full puzzle with distinct titles") { it.pieces.size == 7 && it.titles["fi"] != null && it.titles["fi"] != it.titles["en"] }
        }.getOrNull() ?: kit.firstNew(PuzzleKind.FULL, "$leg: a full puzzle") { it.pieces.size == 7 }
        val seen = ArrayList<String>()

        fun screen(name: String, titled: Boolean) {
            val where = "$leg, $name"
            val texts = kit.shownTexts()
            val stray = texts.filter { it !in world }.distinct()
            assertTrue("$where: texts shown that are not resolved for `$language` (S-36): $stray", stray.isEmpty())
            if (titled) assertEquals("$where: the title is ${p.id}'s title for `$language` (S-11, Q7 row)", kitAppTitle(p, language), kit.text(PlayTags.TITLE))
            seen += texts.filter { it in discriminating }
        }

        kit.openByController(p.id, "$leg: ${p.id} New")
        screen("the Play screen, New", titled = true)

        val order = kitAppLockOrder(p) ?: throw AssertionError("kit: no lock order for ${p.id}")
        kit.placeByController(order.first(), "$leg: one piece placed")
        kit.awaitExists(KitAppTag.RESTART, "$leg: Restart while In progress")
        screen("the Play screen, In progress", titled = true)

        kit.solveByController("$leg: solved")
        kit.awaitSolvedRow("$leg: the solved view")
        screen("the solved view", titled = true)
        kit.onController { retry() }
        kit.idle()

        kit.onController { openOverview() }
        kit.awaitExists(KitAppTag.OVERVIEW, "$leg: the grid")
        // The top bar stays drawn and inert under the grid and the DEV sheet (S-22 hit order 0); its title is compared on the
        // three Play-screen states, and any text it shows here is still held to the closed world.
        screen("the grid overview", titled = false)
        kit.onController { closeOverview() }
        kit.awaitGone(KitAppTag.OVERVIEW, "$leg: the grid closed")

        assertTrue("$leg: the debug app shows `dev-button` on the unsolved ${p.id}", kit.exists(DevAidTags.DEV_BUTTON))
        kit.closeDevSheetAfterTheTest()
        kit.pressCentre(DevAidTags.DEV_BUTTON)
        kit.awaitExists(DevAidTags.SHEET, "$leg: DEV opens its sheet")
        screen(if (kit.exists(DevAidTags.MENU)) "the DEV menu" else "the DEV prompt", titled = false)
        kit.closeDevSheet()

        assertTrue(
            "$leg: at least one text on screen is a resource whose `fi` and `en` readings differ, so these comparisons discriminate (shown: ${seen.distinct()})",
            seen.isNotEmpty(),
        )
    }
}
