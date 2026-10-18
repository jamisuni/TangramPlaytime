// WO-005 TASK-026 · S-25's `SheetLayer`, drawn by the aid (tasks.md § WO-005 › Seams S-25, S-26, S-10Δ2, S-22Δ2; the
// aid's tag table; the touch geometry "The sheet" and hit order 0) · REQ-046 statement (the DEV button asks for the
// passcode; after it, the offer to show the solution and to solve the puzzle), rule 1 ("Wrong passcode." reveals
// nothing), A1, A2, A3 · decisions.md 2026-10-17 "The DEV button, the overlay and the sheet, as placed" (below the top
// bar like the overview, the bar drawn and inert, the router standing aside, a tap on the scrim closes, Back not
// handled) and "The passcode prompt's open readings" ("Hide the solution"; OK on an empty field acts; the passcode
// never drawn as visible text — typed digits masked) · designs/WO-004-design.md §3 library fact 4: the sheet works with
// foundation `clickable`s while the root router stands aside · architecture.md G-08 / O-08, G-10 (every text from the
// aid's resources), G-17, S-12 · DI-1 `#devDlg`, `.sheet`, `.pin`, `.toggle`, `.big` (numbers no row fixes: proposed
// decisions row of TASK-026).
// WO-006 TASK-029 · S-33 (tasks.md § WO-006 › Seams) · decisions.md 2026-10-17 "Edge to edge, the insets and the
// keyboard, as drawn" closes the WO-005 carry ("The DEV aid, drawn in detail"): the scrim still covers `area`; the prompt
// and the menu sit inside `content` (`ScreenGeometry.sheetContent`, inside the insets and above the soft keyboard), so the
// number keyboard never covers the field, OK or Cancel. With no insets and no keyboard `content` = `area`: WO-005's look.
package app.tangramplaytime.play.devaid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tangramplaytime.play.PlayController
import app.tangramplaytime.play.PlayPalette
import app.tangramplaytime.play.R
import app.tangramplaytime.play.Slot
import app.tangramplaytime.play.SlotLayout
import app.tangramplaytime.play.drawControlShape
import app.tangramplaytime.play.slot
import app.tangramplaytime.play.within

/** Touch geometry "The sheet": inset 16 dp from the area below the top bar (DI-1 `.overlay` padding 16 px). */
private const val SHEET_INSET_DP = 16f

/** Touch geometry "The sheet": at most 360 dp wide (DI-1 `#devDlg .sheet` `width: min(360px, 100%)`). */
private const val SHEET_MAX_WIDTH_DP = 360f

/** Touch geometry "The sheet": 26 dp corners (DI-1 `.sheet`). */
private const val SHEET_CORNER_DP = 26f

/** DI-1 `.sheet`: padding 18 dp at the top and the sides and 16 dp at the bottom, 14 dp between the title and the body (as the overview draws it). */
private const val SHEET_PAD_DP = 18f
private const val SHEET_PAD_BOTTOM_DP = 16f
private const val SHEET_GAP_DP = 14f

/** DI-1 `#devLock`, `#devTools` (`.row`, a column): 8 dp between the body's lines, 12 dp between the prompt's two pills. */
private const val BODY_GAP_DP = 8f
private const val PILL_GAP_DP = 12f

/** DI-1 `.sheet h2`: the title 26 sp, weight 800 — drawn bold, as the overview's title. */
private val TITLE = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PlayPalette.silhouette)

/** DI-1 `.hint` (the hints and `#devMsg`, the "Wrong passcode." line): 13 sp, muted. */
private val HINT = TextStyle(fontSize = 13.sp, color = PlayPalette.muted)

/** DI-1 `.pin`: 150 dp wide, 10 dp padding, 14 dp corners, a 2 dp control edge on white; the digits 28 sp bold, 12 sp apart, centred. */
private const val FIELD_WIDTH_DP = 150f
private const val FIELD_PAD_DP = 10f
private const val FIELD_CORNER_DP = 14f
private val FIELD_TEXT = TextStyle(
    fontSize = 28.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 12.sp,
    textAlign = TextAlign.Center,
    color = PlayPalette.silhouette,
)

/** Touch geometry "Its pills are at least 48 dp high": DI-1 `.big` (OK, Done, Show, Solve) 52 dp, 22 dp each side, 20 sp bold; `.toggle` (Cancel) 48 dp, 14 dp each side, 14 sp. */
private enum class Pill(val heightDp: Float, val padDp: Float, val filled: Boolean, val label: TextStyle) {
    /** `.big.next`: green, the label white. */
    GO(52f, 22f, true, TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PlayPalette.control)),

    /** `.big.retry`: white with the control edge, the label in ink. */
    PLAIN(52f, 22f, false, TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PlayPalette.silhouette)),

    /** `.toggle`: white with the control edge, the label in ink. */
    TOGGLE(48f, 14f, false, TextStyle(fontSize = 14.sp, color = PlayPalette.silhouette)),
}

/** A label too wide for its pill shrinks down to 12 sp before it ellipsizes, as the solved row's pills do. */
private const val LABEL_MIN_SP = 12f

/** REQ-037's 48 dp, kept for the pills' width too (the #DevTools controls are exempt; the touch geometry asks 48 dp in height). */
private const val MIN_TOUCH_DP = 48f

/**
 * The aid's sheet over [area] (the tag table's `dev-aid-sheet`, "the screen below the top bar"): the scrim, which closes
 * the sheet at a tap (S-26 `close()`), and, centred in [content] and inset 16 dp, the prompt or the menu (S-33: [content]
 * is `ScreenGeometry.sheetContent`, inside the insets and above the soft keyboard). Every touch here goes to foundation
 * `clickable`s and the field; the root router stands aside while the sheet is open (S-22Δ2 hit order 0).
 */
@Composable
internal fun DevSheetLayer(aid: DevAid, controller: PlayController, area: Slot, content: Slot) {
    val open = aid.sheet
    if (open == DevSheet.NONE) return
    Box(Modifier.slot(area).testTag(DevAidTags.SHEET)) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawRect(PlayPalette.scrim) }
                .clickable(interactionSource = null, indication = null) { aid.close() },
        )
        // S-33: the card's box is `content`, in the sheet node's own px; it takes no touch, so the scrim still gets every
        // touch the card does not cover.
        SlotLayout(Modifier.fillMaxSize()) {
            Box(Modifier.slot(content.within(area)).padding(SHEET_INSET_DP.dp), contentAlignment = Alignment.Center) {
                when (open) {
                    DevSheet.PROMPT -> DevPrompt(aid)
                    DevSheet.MENU -> DevMenu(aid, controller)
                    DevSheet.NONE -> Unit
                }
            }
        }
    }
}

/**
 * The prompt (the tag table's `dev-aid-prompt`): the title, the hint, the masked numeric field, which takes focus when
 * the prompt opens, the "Wrong passcode." line while S-26 `wrongPasscode` holds, Cancel and OK. OK submits what the
 * field holds, the empty entry included; a refused entry empties the field (touch geometry "The sheet"). What was typed
 * lives in this composition only, so a recreation may lose it ("What survives").
 */
@Composable
private fun DevPrompt(aid: DevAid) {
    var entry by remember { mutableStateOf(TextFieldValue("")) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(focus) { focus.requestFocus() }
    SheetCard(DevAidTags.PROMPT) {
        SheetText(stringResource(R.string.dev_aid_title), TITLE, oneLine = true)
        Column(verticalArrangement = Arrangement.spacedBy(BODY_GAP_DP.dp)) {
            SheetText(stringResource(R.string.dev_aid_passcode_hint), HINT)
            BasicTextField(
                value = entry,
                onValueChange = { entry = it },
                modifier = Modifier.testTag(DevAidTags.PASSCODE).focusRequester(focus).width(FIELD_WIDTH_DP.dp),
                textStyle = FIELD_TEXT,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                cursorBrush = SolidColor(PlayPalette.silhouette),
                decorationBox = { field ->
                    Box(
                        Modifier
                            .drawBehind { drawControlShape(Offset.Zero, size, FIELD_CORNER_DP * density) }
                            .padding(FIELD_PAD_DP.dp),
                        contentAlignment = Alignment.Center,
                    ) { field() }
                },
            )
            if (aid.wrongPasscode) {
                Box(Modifier.testTag(DevAidTags.MESSAGE).semantics(mergeDescendants = true) {}) {
                    BasicText(stringResource(R.string.dev_aid_wrong_passcode), style = HINT)
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PILL_GAP_DP.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SheetPill(DevAidTags.CANCEL, stringResource(R.string.dev_aid_cancel), Pill.TOGGLE) { aid.close() }
                SheetPill(DevAidTags.OK, stringResource(R.string.dev_aid_ok), Pill.GO) {
                    if (!aid.submit(entry.text)) entry = TextFieldValue("")
                }
            }
        }
    }
}

/**
 * The menu (the tag table's `dev-aid-menu`): the title, the unlocked hint, "Show the solution" — "Hide the solution"
 * while the overlay is on — and "Solve this puzzle now", the no-best-time hint and Done. Show, Hide and Solve close the
 * sheet before they act (S-26); Done closes it.
 */
@Composable
private fun DevMenu(aid: DevAid, controller: PlayController) {
    SheetCard(DevAidTags.MENU) {
        SheetText(stringResource(R.string.dev_aid_title), TITLE, oneLine = true)
        Column(verticalArrangement = Arrangement.spacedBy(BODY_GAP_DP.dp)) {
            SheetText(stringResource(R.string.dev_aid_unlocked_hint), HINT)
            val show = stringResource(if (aid.solutionShown) R.string.dev_aid_hide else R.string.dev_aid_show)
            SheetPill(DevAidTags.SHOW, show, Pill.PLAIN, Modifier.fillMaxWidth()) { aid.toggleSolution() }
            SheetPill(DevAidTags.SOLVE, stringResource(R.string.dev_aid_solve), Pill.PLAIN, Modifier.fillMaxWidth()) { aid.solveNow(controller) }
            SheetText(stringResource(R.string.dev_aid_no_best_time), HINT)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                SheetPill(DevAidTags.DONE, stringResource(R.string.dev_aid_done), Pill.GO) { aid.close() }
            }
        }
    }
}

/**
 * The sheet itself, tagged [tag] (the prompt's or the menu's content): at most 360 dp wide, the paper colour, 26 dp
 * corners. It is a hit target that consumes nothing, so a touch on it stops here instead of at the scrim beneath it,
 * and the pills' `clickable`s and the field still see every change unconsumed (as the overview's sheet).
 */
@Composable
private fun SheetCard(tag: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .testTag(tag)
            .widthIn(max = SHEET_MAX_WIDTH_DP.dp)
            .fillMaxWidth()
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .drawBehind { drawRoundRect(PlayPalette.paper, cornerRadius = CornerRadius(SHEET_CORNER_DP * density)) }
            .padding(start = SHEET_PAD_DP.dp, top = SHEET_PAD_DP.dp, end = SHEET_PAD_DP.dp, bottom = SHEET_PAD_BOTTOM_DP.dp),
        verticalArrangement = Arrangement.spacedBy(SHEET_GAP_DP.dp),
        content = content,
    )
}

/** A line of the sheet's text: a merged, inert node; the title on one line, ellipsized. */
@Composable
private fun SheetText(text: String, style: TextStyle, oneLine: Boolean = false) {
    Box(Modifier.semantics(mergeDescendants = true) {}) {
        if (oneLine) {
            BasicText(text, style = style, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        } else {
            BasicText(text, style = style)
        }
    }
}

/**
 * One pill of the sheet (the tag table: "the pill, which is its touch area"; OK and Cancel "merged containers with a
 * click action"): a `clickable` box of [kind]'s height, green or white with the control edge, holding [label].
 */
@Composable
private fun SheetPill(tag: String, label: String, kind: Pill, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .testTag(tag)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .defaultMinSize(minWidth = MIN_TOUCH_DP.dp)
            .height(kind.heightDp.dp)
            .drawBehind {
                val corner = size.height / 2f
                if (kind.filled) drawRoundRect(PlayPalette.go, cornerRadius = CornerRadius(corner)) else drawControlShape(Offset.Zero, size, corner)
            }
            .padding(horizontal = kind.padDp.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = kind.label,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = LABEL_MIN_SP.sp, maxFontSize = kind.label.fontSize),
        )
    }
}
