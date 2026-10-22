// WO-007 TASK-059 · seam S-49 with its tags and strings (designs/WO-007-design.md §2; tasks.md § WO-007 "Files"): the settings
// screen, a Compose layer in the activity's own window over the Play screen · REQ-032 (statement, rules 1–3, A1, A2),
// REQ-009, REQ-049, REQ-029 (A2; rule 2 through `timeOf`), REQ-030 rule 3, REQ-034 (rule 1, A1, A2), REQ-031, REQ-033,
// REQ-037, REQ-047; G-09, G-10, O-06 · decisions.md 2026-10-21 "The gear and the settings screen, as drawn." (E2), "The
// settings screen sits in the activity's window …" (E3), "The two switches write through `PlayController` …" (E4), "Play
// time and best times on the settings screen." (E5), "Reset all progress, in order, and where the player is afterwards."
// (E6), "The closed world of texts admits the settings screen's times …" (E10), "Six readings of kit stage 3's oracles."
// (one scrolling body, every item composed) · design §3 "The layer must take every touch", library facts 1 and 5.
package app.tangramplaytime.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tangramplaytime.play.PlayController
import app.tangramplaytime.play.PlayTexts
import app.tangramplaytime.play.timeOf
import app.tangramplaytime.play.titleOf

/**
 * S-49's tag row (designs/WO-007-design.md §2 "S-49 tags"). Controls: the scrim, Done, the two whole-row switches, Reset and
 * the inline confirmation's Erase and Cancel. Every other tag is an inert merged node. Each text entry inside a node is one
 * resource, one title or one time, never concatenated (the closed world of texts, E10).
 */
internal object SettingsTags {
    /** The scrim: covers the whole window, the top bar included; its `clickable` calls `closeSettings()`. */
    const val SCRIM = "settings"

    /** Done, in the fixed header. */
    const val DONE = "settings-done"

    /** The two whole-row switches (`toggleable`, Role.Switch, at least 56 dp high). */
    const val TIMER = "settings-timer"
    const val SOUND = "settings-sound"

    /** Reset all progress, and the inline confirmation's two answers. */
    const val RESET = "settings-reset"
    const val ERASE = "reset-erase"
    const val CANCEL = "reset-cancel"

    /** The header title: an inert merged node (the kits' window-touch point on the open screen, review F2). */
    const val TITLE = "settings-title"

    /** The sheet: the hit target that takes every touch on it. */
    const val SHEET = "settings-sheet"

    /** [label, value] rows of today's and the all-time play time. */
    const val PLAY_TIME_TODAY = "play-time-today"
    const val PLAY_TIME_ALL = "play-time-all"

    /** [heading], plus "–" while no best time is stored. */
    const val BEST_TIMES = "best-times"

    /** The prefix of each [title, time] best-time row; see [bestTime]. */
    const val BEST_TIME_PREFIX = "best-time-"

    /** The reset question, shown in place of Reset until it is answered. */
    const val RESET_QUESTION = "reset-question"

    /** [heading, text] of the how-to-play text. */
    const val HOW_TO_PLAY = "how-to-play"

    /** The free note (REQ-009), in its green box. */
    const val FREE_NOTE = "free-note"

    /** The privacy text (REQ-049), below the free note. */
    const val PRIVACY_TEXT = "privacy-text"

    /** The licence line, below the privacy text (plain text, never a link). */
    const val LICENCE_LINE = "licence-line"

    /** The best-time row of the puzzle [id]. */
    fun bestTime(id: String): String = BEST_TIME_PREFIX + id
}

/**
 * The settings screen (S-49), composed by `MainActivity` after `PlayScreen`, in the same window, while
 * [PlayController.settingsOpen] holds. It is never a `Dialog` or a `Popup`: those are other windows, whose touches
 * `MainActivity.dispatchTouchEvent` never sees (play time must count here, E3), and whose clickables would get another
 * window's click sound (reviews/WO-007-design-review.md, F1 re-check, the watch point).
 *
 * - **The layer** (design §3 "The layer must take every touch"; library fact 1): the scrim and the sheet are siblings,
 *   never nested. The scrim covers the whole window and closes the screen on a tap; the sheet above it is a hit target
 *   that consumes nothing, so a touch on the sheet reaches neither the scrim nor the Play screen below, while the sheet's
 *   own controls still see every change.
 * - **The sheet** (E2): inside the safe drawing area with 16 dp margins, at most 560 dp wide and centred, as tall as its
 *   content up to the safe height. A fixed header holds the title and Done; the body below it scrolls.
 * - **The body** is one scrolling column with every item composed (decisions.md 2026-10-21 "Six readings of kit stage
 *   3's oracles."), in REQ-032 rule 1's order: the timer and sound switches, the play times, the best times, Reset, how
 *   to play, the free note, the privacy text, the licence line.
 * - **Reset** (REQ-034 rule 1): Reset becomes the question with Erase and Cancel, held in `remember` state, so a
 *   recreation reads it as cancelled (E3). Only Erase calls [PlayController.resetAll]; the screen stays open (E6).
 * - **Times** (E5, E10): the play times and each best time are the controller's values through [timeOf], read live; a
 *   best-time title is the puzzle's title in the game's language through [titleOf].
 * - **Texts** (G-10, O-06): `:app` resources read through `stringResource`, which resolves under S-36's wrapped context
 *   (library fact 5); Done and "–" are [PlayTexts]' own.
 *
 * Nothing here is a difficulty control (G-09; REQ-032.A2).
 */
@Composable
internal fun SettingsScreen(controller: PlayController, texts: PlayTexts) {
    var asking by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        // The scrim: every point of the window; a tap on it closes the screen (E2 "Closing: … a tap on the scrim").
        Box(
            Modifier
                .fillMaxSize()
                .testTag(SettingsTags.SCRIM)
                .background(SCRIM)
                .clickable(interactionSource = null, indication = null) { controller.closeSettings() },
        )
        // The sheet's frame: the safe drawing area less 16 dp. It holds no hit target of its own, so the scrim takes every
        // touch outside the sheet.
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(SHEET_MARGIN_DP.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .widthIn(max = SHEET_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .testTag(SettingsTags.SHEET)
                    .semantics(mergeDescendants = true) {}
                    // A hit target that consumes nothing: a touch on the sheet stops here instead of at the scrim beneath
                    // it, and the switches', Reset's, Erase's, Cancel's and Done's `clickable`s still see it unconsumed.
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                    .background(PAPER, SHEET_SHAPE)
                    .border(1.dp, EDGE, SHEET_SHAPE)
                    .padding(SHEET_PAD_DP.dp),
                verticalArrangement = Arrangement.spacedBy(GAP_DP.dp),
            ) {
                Header(controller, texts)
                Body(controller, texts, asking, onAsking = { asking = it }, Modifier.weight(1f, fill = false))
            }
        }
    }
}

/** The fixed header (E2): the title, an inert merged node, and Done. */
@Composable
private fun Header(controller: PlayController, texts: PlayTexts) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.weight(1f).testTag(SettingsTags.TITLE).semantics(mergeDescendants = true) {}) {
            BasicText(stringResource(R.string.settings_title), style = TITLE_STYLE, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Pill(texts.done, SettingsTags.DONE, PillLook.PRIMARY) { controller.closeSettings() }
    }
}

/** The scrolling body, in REQ-032 rule 1's order, every item composed at once. */
@Composable
private fun Body(controller: PlayController, texts: PlayTexts, asking: Boolean, onAsking: (Boolean) -> Unit, modifier: Modifier) {
    val settings = controller.settings
    val on = stringResource(R.string.setting_on)
    val off = stringResource(R.string.setting_off)
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(GAP_DP.dp),
    ) {
        // REQ-031, REQ-033: each switch writes through the controller and takes effect at once (E4; N1).
        SwitchRow(SettingsTags.TIMER, stringResource(R.string.setting_timer), settings.timerShown, on, off) { controller.setTimerShown(it) }
        SwitchRow(SettingsTags.SOUND, stringResource(R.string.setting_sound), settings.soundOn, on, off) { controller.setSoundOn(it) }

        // REQ-029 A2 and E5: today's and the all-time play time, live.
        val times = controller.playTimes
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ROW_GAP_DP.dp)) {
            BasicText(stringResource(R.string.play_time_title), style = SECTION_STYLE)
            ValueRow(SettingsTags.PLAY_TIME_TODAY, stringResource(R.string.play_time_today), texts.timeOf(times.todaySeconds))
            ValueRow(SettingsTags.PLAY_TIME_ALL, stringResource(R.string.play_time_all), texts.timeOf(times.allTimeSeconds))
        }

        // REQ-030 rule 3 and E5: one row per puzzle with a stored best, in ‹ › order; "–" under the heading while none is.
        val bests = controller.bestTimes
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ROW_GAP_DP.dp)) {
            Row(
                Modifier.fillMaxWidth().testTag(SettingsTags.BEST_TIMES).semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BasicText(stringResource(R.string.best_times_title), Modifier.weight(1f), style = SECTION_STYLE)
                if (bests.isEmpty()) BasicText(texts.noTime, style = VALUE_STYLE)
            }
            for (best in bests) {
                key(best.puzzle.id) {
                    ValueRow(SettingsTags.bestTime(best.puzzle.id), texts.titleOf(best.puzzle), texts.timeOf(best.seconds))
                }
            }
        }

        ResetItem(controller, asking, onAsking)

        // REQ-032 rule 1: "a short how-to-play text" (the design's draft, a proof-reading item in review-notes.md).
        Column(
            Modifier.fillMaxWidth().testTag(SettingsTags.HOW_TO_PLAY).semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(ROW_GAP_DP.dp),
        ) {
            BasicText(stringResource(R.string.how_to_title), style = SECTION_STYLE)
            BasicText(stringResource(R.string.how_to_text), style = HINT_STYLE)
        }

        // REQ-009: the free note in full, in a green box (its ASSUMPTION; E2).
        NoteBox(SettingsTags.FREE_NOTE, stringResource(R.string.free_note), FREE_BACKGROUND, FREE_STYLE)
        // REQ-049: the privacy text in full, as plain text, below the free note.
        NoteBox(SettingsTags.PRIVACY_TEXT, stringResource(R.string.privacy_text), PRIVACY_BACKGROUND, PRIVACY_STYLE)
        // The licence line (WO-007 scope; S-49 strings): plain text, never a link (REQ-010: no network use).
        Box(Modifier.fillMaxWidth().testTag(SettingsTags.LICENCE_LINE).semantics(mergeDescendants = true) {}) {
            BasicText(stringResource(R.string.licence_line), style = LICENCE_STYLE)
        }
    }
}

/**
 * Reset all progress (REQ-034 rule 1; E2 "Reset is inline"): Reset, or — once pressed — the question with Cancel and Erase in
 * its place, brought into the body's view so both answers can be pressed. Erase calls [PlayController.resetAll] and answers
 * the question; Cancel only answers it (REQ-034.A2).
 *
 * A press repeated where Reset was never answers with Erase (REQ-034 rule 1, "a second, explicit confirmation";
 * reviews/WO-007-code-review.md N4): Reset stays inside the left [RESET_MAX_FRACTION] of the body's width, and Erase inside the
 * right [ERASE_MAX_FRACTION], at the row's end, so their columns never meet. However far the sheet re-centres or the body
 * scrolls as the question grows (both move the content only up and down), Reset's old place can only hold the question, the
 * gap of the answers' row, or Cancel at the row's start.
 */
@Composable
private fun ResetItem(controller: PlayController, asking: Boolean, onAsking: (Boolean) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val resetMax = maxWidth * RESET_MAX_FRACTION
        val eraseMax = maxWidth * ERASE_MAX_FRACTION
        if (!asking) {
            Box(Modifier.widthIn(max = resetMax)) {
                Pill(stringResource(R.string.reset_all), SettingsTags.RESET, PillLook.DANGER) { onAsking(true) }
            }
        } else {
            val requester = remember { BringIntoViewRequester() }
            LaunchedEffect(requester) {
                // One frame first, so the question's place is laid out before the body scrolls to it.
                withFrameNanos { }
                requester.bringIntoView()
            }
            Column(
                Modifier.fillMaxWidth().bringIntoViewRequester(requester),
                verticalArrangement = Arrangement.spacedBy(ROW_GAP_DP.dp),
            ) {
                Box(Modifier.fillMaxWidth().testTag(SettingsTags.RESET_QUESTION).semantics(mergeDescendants = true) {}) {
                    BasicText(stringResource(R.string.reset_question), style = QUESTION_STYLE)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Pill(stringResource(R.string.reset_cancel), SettingsTags.CANCEL, PillLook.PLAIN) { onAsking(false) }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.widthIn(max = eraseMax)) {
                        Pill(stringResource(R.string.reset_erase), SettingsTags.ERASE, PillLook.DANGER) {
                            controller.resetAll()
                            onAsking(false)
                        }
                    }
                }
            }
        }
    }
}

/**
 * A whole-row switch (S-49: `toggleable`, Role.Switch, at least 56 dp high): the label, then "On" or "Off" and a drawn
 * track. A tap anywhere on the row flips it; nothing inside it is a control of its own.
 */
@Composable
private fun SwitchRow(tag: String, label: String, value: Boolean, on: String, off: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .testTag(tag)
            .heightIn(min = SWITCH_ROW_MIN_DP.dp)
            .background(CARD, ROW_SHAPE)
            .border(2.dp, EDGE, ROW_SHAPE)
            .toggleable(value = value, interactionSource = null, indication = null, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicText(label, Modifier.weight(1f), style = LABEL_STYLE)
        BasicText(if (value) on else off, style = VALUE_STYLE)
        Box(
            Modifier
                .size(TRACK_WIDTH_DP.dp, TRACK_HEIGHT_DP.dp)
                .drawBehind {
                    val h = size.height
                    drawRoundRect(if (value) GO else EDGE, cornerRadius = CornerRadius(h / 2f))
                    val r = h / 2f - KNOB_INSET_DP.dp.toPx()
                    val cx = if (value) size.width - h / 2f else h / 2f
                    drawCircle(CARD, radius = r, center = Offset(cx, h / 2f))
                },
        )
    }
}

/** An inert merged [label, value] row (S-49: the play times and the best times), with a hairline below it. */
@Composable
private fun ValueRow(tag: String, label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .testTag(tag)
            .semantics(mergeDescendants = true) {}
            .drawBehind {
                val line = 1.dp.toPx()
                drawRect(EDGE, topLeft = Offset(0f, size.height - line), size = Size(size.width, line))
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicText(label, Modifier.weight(1f), style = LABEL_STYLE)
        BasicText(value, style = VALUE_STYLE)
    }
}

/** An inert merged box holding one text in full (the free note, the privacy text). */
@Composable
private fun NoteBox(tag: String, text: String, background: Color, style: TextStyle) {
    Box(
        Modifier
            .fillMaxWidth()
            .testTag(tag)
            .semantics(mergeDescendants = true) {}
            .background(background, NOTE_SHAPE)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        BasicText(text, style = style)
    }
}

/** How a [Pill] is drawn. */
private enum class PillLook { PRIMARY, PLAIN, DANGER }

/**
 * A pill button whose node bounds are its touch area (S-13Δ): at least 48 dp in both directions (REQ-037), drawn without a
 * clip, so every point of its box takes the touch. A label wider than the room it is given wraps, centred, and the pill grows.
 */
@Composable
private fun Pill(text: String, tag: String, look: PillLook, onClick: () -> Unit) {
    val fill = when (look) {
        PillLook.PRIMARY -> GO
        PillLook.PLAIN, PillLook.DANGER -> CARD
    }
    val edge = when (look) {
        PillLook.PRIMARY -> GO
        PillLook.PLAIN, PillLook.DANGER -> EDGE
    }
    val ink = when (look) {
        PillLook.PRIMARY -> CARD
        PillLook.DANGER -> WARN
        PillLook.PLAIN -> INK
    }
    Box(
        Modifier
            .testTag(tag)
            .heightIn(min = CONTROL_MIN_DP.dp)
            .widthIn(min = PILL_MIN_WIDTH_DP.dp)
            .background(fill, PILL_SHAPE)
            .border(2.dp, edge, PILL_SHAPE)
            .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = PILL_STYLE.copy(color = ink))
    }
}

// ── The look (DI-1 `.overlay`, `.sheet`, `.toggle`, `.free`, `.privacy`; a behaviour reference only: E2 "as drawn") ──

/** E2: "One sheet sits inside the safe area with 16 dp margins, at most 560 dp wide and centred." */
private const val SHEET_MARGIN_DP = 16
private const val SHEET_MAX_WIDTH_DP = 560
private const val SHEET_PAD_DP = 18
private const val GAP_DP = 14
private const val ROW_GAP_DP = 4

/** REQ-037: every player control at least 48 dp in both directions; S-49: the switch rows at least 56 dp high. */
private const val CONTROL_MIN_DP = 48
private const val SWITCH_ROW_MIN_DP = 56
private const val PILL_MIN_WIDTH_DP = 88

/**
 * REQ-034 rule 1 (code review N4): Reset fits in the left 60 % of the body's width and Erase in the right 36 %, so the two
 * never share a column and a press repeated where Reset was cannot land on Erase.
 */
private const val RESET_MAX_FRACTION = 0.60f
private const val ERASE_MAX_FRACTION = 0.36f

private const val TRACK_WIDTH_DP = 44
private const val TRACK_HEIGHT_DP = 26
private const val KNOB_INSET_DP = 3

private val SHEET_SHAPE = RoundedCornerShape(26.dp)
private val ROW_SHAPE = RoundedCornerShape(16.dp)
private val NOTE_SHAPE = RoundedCornerShape(16.dp)
private val PILL_SHAPE = RoundedCornerShape(50)

private val SCRIM = Color(0x522B2D42)
private val PAPER = Color(0xFFFBF7EE)
private val CARD = Color(0xFFFFFFFF)
private val EDGE = Color(0xFFE0D6C2)
private val INK = Color(0xFF2B2D42)
private val MUTED = Color(0xFF7E8299)
private val GO = Color(0xFF2DBE7E)
private val WARN = Color(0xFFE8505B)
private val FREE_BACKGROUND = Color(0xFFEAF7F0)
private val FREE_INK = Color(0xFF1F6B47)
private val PRIVACY_BACKGROUND = Color(0xFFF1EDE3)

private val TITLE_STYLE = TextStyle(color = INK, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
private val SECTION_STYLE = TextStyle(color = MUTED, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
private val LABEL_STYLE = TextStyle(color = INK, fontSize = 16.sp)
private val VALUE_STYLE = TextStyle(color = INK, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
private val HINT_STYLE = TextStyle(color = MUTED, fontSize = 14.sp)
private val QUESTION_STYLE = TextStyle(color = INK, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
private val FREE_STYLE = TextStyle(color = FREE_INK, fontSize = 15.sp)
private val PRIVACY_STYLE = TextStyle(color = INK, fontSize = 13.sp)
private val LICENCE_STYLE = TextStyle(color = MUTED, fontSize = 12.sp)
private val PILL_STYLE = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
