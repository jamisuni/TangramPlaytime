// Acceptance kit (WO-006, TASK-030): the soft keyboard under the DEV passcode prompt in the real debug app — the WO-006
// carry (tasks.md § WO-006 S-31 `keyboardDp`, S-32 "The keyboard moves nothing else", S-33 the aid's `content` =
// `sheetContent`; decisions.md 2026-10-17 "Edge to edge, the insets and the keyboard, as drawn" and "The DEV aid, drawn in
// detail"). No token lives in this file: the carry is a pin.
// - Its own class (decisions.md 2026-10-17 "The device kit's open readings — amends the Q5 row's probe", ruling 2): the
//   pin needs the passcode prompt, so the process-wide aid (S-25: one per process) must still be locked, and
//   `DevAidAppKitTest` unlocks it for the rest of its process (REQ-046 rule 2). The app-set channel line runs this class
//   alone, then the rest of the set with `notClass` naming it. The pin fails loudly when the aid is already unlocked; it
//   never skips and relies on no class order.
// - Where it runs: every image, on the 390 × 844 dp phone (a display override read back from the launched activity, seam
//   note 4). `show_ime_with_hard_keyboard` is set to 1 before the prompt opens, so the field's focus on open shows the
//   emulator's soft keyboard beside its hardware one (ruling 7), and is restored as read (each image's own default).
// - It leaves the controller, the DEV sheet and the device as it found them (seam note 3; `KitApp.restoreRule()`): the sheet
//   first, then `show_ime_with_hard_keyboard` and the display through the shell alone, bounded, then the activity is closed
//   (decisions.md 2026-10-18 "Worker wins three contests", part 3).
package app.tangramplaytime.app

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.play.PlayTags
import app.tangramplaytime.play.devaid.DevAidTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class DevPromptKeyboardKitTest {

    @get:Rule(order = 0)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val kit = KitApp(composeRule)

    @get:Rule(order = 1)
    val kitRestore: TestRule = kit.restoreRule()

    @Test // pin: S-32 ("The keyboard moves nothing else: only `sheetContent` reads `keyboardDp`; the bar, the board, the tray and every control keep their slots"); S-33 (the aid's `content` = `sheetContent`, "so the number keyboard never covers the field, OK or Cancel"); decisions.md 2026-10-17 "Edge to edge, the insets and the keyboard, as drawn" and "The DEV aid, drawn in detail" (the WO-006 carry); no token
    fun theSoftKeyboardLeavesTheDevPromptUncoveredAndMovesNothingElse() {
        // Oracle, on the 390 × 844 dp phone of the debug app: DEV opens the prompt, whose field takes focus (the aid's touch
        // geometry); once the keyboard's inset exceeds 0 (failing loudly after the timeout) and has settled, `dev-aid-passcode`,
        // `dev-aid-ok` and `dev-aid-cancel` end at or above the keyboard's top, while `board`'s BoardMappingKey and every tray
        // cell's box equal their values before the prompt opened.
        // "Before" is read once the bars have settled after the display change (decisions.md 2026-10-18 "Worker wins three
        // contests", part 2: the bars move seconds later, dev 63 → 72 px, tablet 64 → 48 → 72 px, and a "before" taken
        // earlier blamed the keyboard for their move). Fixture: the system bars and the cutout read the same with the keyboard
        // open as before it, so the keyboard is the only thing that changed between the two reads.
        kit.start("the keyboard carry")
        kit.usePhone(KIT_PHONE_390, "the 390 × 844 dp phone")
        assertTrue("precondition: the open puzzle ${kit.shownId()} is not Solved, so the DEV button shows (REQ-046 rule 5)", kit.shownState() != PuzzleState.SOLVED)
        assertTrue("the debug app shows `dev-button`", kit.exists(DevAidTags.DEV_BUTTON))
        val bars = kit.settledInsets("before the prompt opens")
        assertTrue("precondition: no keyboard before the prompt opens ($bars)", bars.ime.bottom == 0)
        val mapping = kit.mapping()
        val cells = kit.onController { puzzle.pieces }.associateWith { kit.bounds(PlayTags.trayCell(it)) }
        kit.assertInsetsStill(bars, "before the prompt opens, after the reads")
        kit.showImeWithHardKeyboard("the soft keyboard beside the hardware one")
        kit.closeDevSheetAfterTheTest()
        kit.pressCentre(DevAidTags.DEV_BUTTON)
        if (kit.exists(DevAidTags.MENU)) {
            throw AssertionError(
                "precondition: DEV opened the menu, so the process-wide aid was unlocked earlier in this process (DevAidAppKitTest; " +
                    "REQ-046 rule 2) and the passcode prompt is unreachable. The channel runs DevPromptKeyboardKitTest alone, before " +
                    "the rest of the app set (decisions.md 2026-10-17 \"The device kit's open readings\", ruling 2).",
            )
        }
        kit.awaitExists(DevAidTags.PROMPT, "DEV opens the prompt")
        kit.awaitExists(DevAidTags.PASSCODE, "the prompt's field")
        kit.await("the soft keyboard's inset to exceed 0 after the prompt took focus", KIT_APP_DEVICE_WAIT_MS) { kit.insets("the keyboard").ime.bottom > 0 }
        val ins = kit.settledInsets("the keyboard open")
        assertTrue("the keyboard is still open once its inset has settled ($ins)", ins.ime.bottom > 0)
        assertTrue(
            "fixture: the window, the system bars and the cutout read the same with the keyboard open as before it, so only the keyboard changed (before $bars; now $ins)",
            ins.windowWidthPx == bars.windowWidthPx && ins.windowHeightPx == bars.windowHeightPx &&
                ins.systemBars == bars.systemBars && ins.displayCutout == bars.displayCutout,
        )
        val keyboardTop = ins.windowHeightPx - ins.ime.bottom
        for (tag in listOf(DevAidTags.PASSCODE, DevAidTags.OK, DevAidTags.CANCEL)) {
            val b = kit.windowBounds(tag)
            assertTrue("`$tag` $b ends at or above the keyboard's top $keyboardTop ($ins)", b.bottom <= keyboardTop + 0.5f)
        }
        assertEquals("`board`'s BoardMappingKey while the keyboard is open (the keyboard moves nothing else)", mapping, kit.mapping())
        for ((piece, before) in cells) assertNear("$piece's tray cell while the keyboard is open", before, kit.bounds(PlayTags.trayCell(piece)), 0.5f)
        kit.assertInsetsStill(ins, "the keyboard open, after the reads")
        kit.closeDevSheet()
    }

    private fun assertNear(what: String, expected: Rect, actual: Rect, tolPx: Float) {
        val ok = abs(expected.left - actual.left) <= tolPx && abs(expected.top - actual.top) <= tolPx &&
            abs(expected.right - actual.right) <= tolPx && abs(expected.bottom - actual.bottom) <= tolPx
        assertTrue("$what: expected $expected, was $actual (tolerance $tolPx px)", ok)
    }
}
