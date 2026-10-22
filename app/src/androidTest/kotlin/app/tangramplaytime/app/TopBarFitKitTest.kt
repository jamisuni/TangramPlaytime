// Acceptance kit (WO-007, TASK-055; kit stage 2) — F5's pin: the top bar's second line fits the counter that the gear
// narrows (designs/WO-007-design.md §2 S-22Δ3, §3 "The second line (F5)"; reviews/WO-007-design-review.md F5 and
// Re-derived row 14). No token: REQ-024's rule is consumed here ("The top bar shows the puzzle's name, its difficulty dots,
// its number out of the total, and its state (TYPE-006)"), on REQ-006 A1's 360 × 780 dp phone; prose names a line without
// the dot (traceability-rules.md §2).
//
// - The rows: decisions.md 2026-10-21 "Amends … the top bar's second line shrinks to fit the narrower counter." (E2a): "with
//   the gear, the counter is 188 dp on the 360 dp phone, so its content gets 180 dp. At font scale 1 the worst case, "100 /
//   106 · in progress", needs about 198 dp"; "the dots, the separators, "n / N" and the state word scale by one factor …
//   floored at 0.75"; "If even 0.75 does not fit: the state word ellipsizes. The number never does"; "A new pin: at 360 ×
//   780 dp, on a three-digit In-progress puzzle, in `en` and in `fi`, the number and the state word are drawn whole — no
//   visual overflow — inside `counter`". Design §3: "the number's and the word's `TextLayoutResult` have no visual overflow
//   and their nodes lie inside `counter`". decisions.md 2026-10-21 "F5's second-line pin is an app-kit class,
//   `TopBarFitKitTest`, …" (PL2): the texts come from `:app`'s `en` and `fi` resources, "so it needs no device locale and
//   runs on the floor".
// - "Drawn whole" is read from each text's layout: one line, not ellipsized, every character of the text visible on that line,
//   and no visual overflow — the drawn line's extent inside the text's own box. An ellipsized word is not whole (E2a lets only
//   the state word ellipsize, and only below the 0.75 floor, which this case does not reach). `hasVisualOverflow` is not the
//   reading: in this row it is true for a text that fits (see `kitDrawnWhole`; a corrected check, TASK-055 fix).
// - The host: `PlayScreen` (S-10: it "fills `modifier`'s size") composed in a kit-sized box of 360 × 780 dp anchored at the
//   window's top-left, so the size is the same on every image, the tablet included (the box may run past a shorter window's
//   bottom; nothing below the bar is read). Font scale 1 is set for the host (E2a, F5: "at font scale 1"), whatever the
//   device's setting. The layout is the app's phone value (`PlayLayouts.phone`, S-5; its insets are `PlayInsets.NONE`, as a
//   portrait phone's side insets are). Every size is read from the nodes, the density from the compose rule.
// - The texts: a `PlayTexts` built from `:app`'s string resources resolved for the leg's language only
//   (`createConfigurationContext`, as `KitApp.contextFor` builds it; the TASK-030 brief), never a copied Finnish word (G-10:
//   Finnish is a reference, never asserted). The fixture checks that the two legs resolve different state words, so the
//   `fi` leg is not the `en` one again.
// - The puzzle: the bundle's 100th puzzle in ‹ › order (a three-digit number; the bundle holds 106), made In progress by a
//   drag from its tray that goes home (TYPE-006: "New → In progress when a piece first leaves the tray"; S-2 `startDrag`,
//   `goHome`), so the bar shows the state word (decisions.md 2026-10-15 "The top bar, Restart and the solved row, as drawn":
//   "a state word for In progress and Solved").
// - Fixtures discriminate:
//   - the harness reads a kit-owned text laid out too narrow, clipped and then ellipsized, in the same composition, and must
//     find neither drawn whole (the predicate can fail);
//   - in `en`, the line's natural width at 13 sp — the five rating dots and two separators of decisions.md 2026-10-15 "The
//     top bar's drawing numbers" (dots 8 dp, 3 dp apart; separators 3 dp dots with 5 dp either side) plus the number and the
//     word as the kit's text measurer lays them out — exceeds the counter's content (its width less the 4 dp it keeps from
//     each column, the same row), so a line that did not shrink would not be whole (review F5: "the bar shows "in progre"").
//     The `fi` words are shorter (E2a), so that leg is the pin without the discrimination.
// - No `:app` seam is called and nothing of the device changes: the class uses its own controller (no `AppGraph`), no store,
//   no display override and no locale (seam note 5 of `tasks.md` § WO-007: every app-kit class leaves the controller, the
//   settings and the device as it found them). Nothing sleeps. Helper names carry a `kit` / `Kit` / `KIT_` prefix.
// - Corrected WO-007 (a corrected check, found at TASK-056's device run on both images): `state-word` is a merged, inert text
//   node (S-14Δ "texts are merged, inert nodes"; seam note 4 of `tasks.md` § WO-004), so in the unmerged tree its tag sits on
//   the merging node and its text and text layout on the text node inside it. Each layout is now read from the one text node
//   at or under the node that shows the text (`kitTextNodeUnder`), and both the tagged node and that text node must lie
//   inside `counter`. The measured widths and the drawn font sizes are logged (tag `TopBarFitKitTest`) on both legs.
package app.tangramplaytime.app

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.play.PlayController
import app.tangramplaytime.play.PlayScreen
import app.tangramplaytime.play.PlayTags
import app.tangramplaytime.play.PlayTexts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class TopBarFitKitTest {

    @get:Rule
    val rule = createComposeRule()

    private companion object {
        /** REQ-006 A1's phone in the play-through, 360 × 780 dp (decisions.md 2026-10-17 Q5 and Q9 rows; `KitApp.KIT_PHONE_360`). */
        const val KIT_PHONE_WIDTH_DP = 360f
        const val KIT_PHONE_HEIGHT_DP = 780f

        /** A three-digit number in ‹ › order (E2a: "a three-digit In-progress puzzle"; the design's worst case "100 / 106"). */
        const val KIT_NUMBER = 100

        /** The counter: 360 − 2 × 60 − 52 dp (E2a: "the counter is 188 dp on the 360 dp phone"); it keeps 4 dp from each column (C6). */
        const val KIT_COUNTER_DP = 188f
        const val KIT_COUNTER_PADDING_DP = 4f

        /** The second line's natural size, decisions.md 2026-10-15 "The top bar's drawing numbers": 13 sp; five dots 8 dp, 3 dp apart; separators 3 dp dots with 5 dp either side. */
        const val KIT_LINE_SP = 13f
        const val KIT_DOTS_DP = 5 * 8f + 4 * 3f
        const val KIT_SEPARATOR_DP = 5f + 3f + 5f

        /** The harness's own texts, laid out too narrow for their word (one clipped, one ellipsized). */
        const val KIT_CLIPPED_TAG = "kit-fit-clipped"
        const val KIT_ELLIPSIZED_TAG = "kit-fit-ellipsized"
        const val KIT_NARROW_DP = 8f

        /** A node edge read from a layout may sit 1 px off its parent's. */
        const val KIT_EDGE_TOL_PX = 1f

        /** A text line's extent against its box: the box is whole px, the line a float. */
        const val KIT_LINE_TOL_PX = 0.5f
    }

    // ── Legs ─────────────────────────────────────────────────────────────────────────────────────────

    // Pin (no token): E2a ("at 360 × 780 dp, on a three-digit In-progress puzzle, in `en` …, the number and the state word are
    // drawn whole — no visual overflow — inside `counter`"); design §3 "The second line (F5)"; REQ-024 rule (consumed). The en
    // leg also shows that the case needs the shrink (see the header).
    @Test
    fun inEnglishTheNumberAndTheStateWordAreDrawnWholeInsideTheCounter() = kitAssertTheLineFits("en", needsTheShrink = true)

    // Pin (no token): E2a, the same pin "in `fi`"; PL2 (the `fi` texts are `:app`'s resources, so no device locale is needed
    // and the leg runs on the floor); G-10 (no Finnish word is written here).
    @Test
    fun inFinnishTheNumberAndTheStateWordAreDrawnWholeInsideTheCounter() = kitAssertTheLineFits("fi", needsTheShrink = false)

    // ── The oracle ───────────────────────────────────────────────────────────────────────────────────

    private fun kitAssertTheLineFits(language: String, needsTheShrink: Boolean) {
        val texts = kitTextsFor(language)
        val other = kitTextsFor(if (language == "fi") "en" else "fi")
        assertNotEquals("fixture: the $language leg resolves its own state word, not the other language's", other.inProgress, texts.inProgress)

        val library = kitAppLibrary
        if (library.puzzles.size < KIT_NUMBER) throw AssertionError("fixture defect: the bundle lists ${library.puzzles.size} puzzles, no number $KIT_NUMBER")
        val p = library.puzzles[KIT_NUMBER - 1]
        val controller = rule.runOnUiThread { PlayController(library, kitAppEngine) }
        rule.runOnUiThread { check(controller.open(p.id)) { "kit: open(${p.id}) refused" } }

        var measurer: TextMeasurer? = null
        rule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 1f)) {
                measurer = rememberTextMeasurer()
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().wrapContentSize(Alignment.TopStart, unbounded = true)) {
                        Box(Modifier.size(KIT_PHONE_WIDTH_DP.dp, KIT_PHONE_HEIGHT_DP.dp)) {
                            PlayScreen(controller, PlayLayouts.phone, texts, true, Modifier.fillMaxSize())
                        }
                    }
                    Column(Modifier.align(Alignment.TopEnd)) {
                        BasicText(texts.inProgress, Modifier.width(KIT_NARROW_DP.dp).testTag(KIT_CLIPPED_TAG), softWrap = false, maxLines = 1)
                        BasicText(
                            texts.inProgress,
                            Modifier.width(KIT_NARROW_DP.dp).testTag(KIT_ELLIPSIZED_TAG),
                            softWrap = false,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        val density = rule.density.density
        fun dp(v: Float): Float = v * density

        // The host is the 360 × 780 dp phone, read from the node.
        val screen = kitUnclipped(kitNode(PlayTags.SCREEN))
        assertTrue("fixture: `play-screen` is ${screen.width} × ${screen.height} px, not 360 × 780 dp at $density", abs(screen.width - dp(KIT_PHONE_WIDTH_DP)) <= 1.5f && abs(screen.height - dp(KIT_PHONE_HEIGHT_DP)) <= 1.5f)

        // The puzzle In progress: a drag from the tray that goes home (TYPE-006; S-2).
        rule.runOnUiThread {
            val piece = controller.puzzle.pieces.first()
            check(controller.startDrag(piece, Point(2.0, 12.0))) { "kit: startDrag($piece) from the tray refused" }
            controller.goHome()
        }
        rule.waitForIdle()
        assertEquals("fixture: ${p.id} open", p.id, rule.runOnUiThread { controller.puzzle.id })
        assertEquals("fixture: ${p.id} In progress", PuzzleState.IN_PROGRESS, rule.runOnUiThread { controller.state })

        // The harness can see a text that is not whole.
        val clipped = kitLayoutOf(kitNode(KIT_CLIPPED_TAG), KIT_CLIPPED_TAG)
        val ellipsized = kitLayoutOf(kitNode(KIT_ELLIPSIZED_TAG), KIT_ELLIPSIZED_TAG)
        Log.i("TopBarFitKitTest", "$language: harness clipped ${kitDescribe(clipped, texts.inProgress)}")
        Log.i("TopBarFitKitTest", "$language: harness ellipsized ${kitDescribe(ellipsized, texts.inProgress)}")
        assertFalse("harness: a word clipped in an 8 dp box is not drawn whole", kitDrawnWhole(clipped, texts.inProgress))
        assertFalse("harness: a word ellipsized in an 8 dp box is not drawn whole", kitDrawnWhole(ellipsized, texts.inProgress))

        // The counter is the narrowed one.
        val counter = kitUnclipped(kitNode(PlayTags.COUNTER))
        assertTrue("fixture: `counter` is ${counter.width} px wide, not $KIT_COUNTER_DP dp (E2a) at $density", abs(counter.width - dp(KIT_COUNTER_DP)) <= 2f)

        val number = String.format(Locale.ROOT, texts.counterFormat, KIT_NUMBER, library.puzzles.size)
        val numberNodes = rule.onAllNodes(hasText(number) and hasAnyAncestor(hasTestTag(PlayTags.COUNTER)), useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals("the number \"$number\" is one text node inside `counter`", 1, numberNodes.size)
        // S-14Δ "texts are merged, inert nodes": `state-word` tags the merged node, and in the unmerged tree its text and its
        // text layout sit on the text node inside it (corrected WO-007, TASK-055 at TASK-056's device run: the tagged node
        // itself has no layout). Each layout is read from the one text node at or under the node that shows the text.
        val wordNode = kitNode(PlayTags.STATE_WORD)
        val numberText = kitTextNodeUnder(numberNodes.single(), number, "the number")
        val wordText = kitTextNodeUnder(wordNode, texts.inProgress, PlayTags.STATE_WORD)
        val numberLayout = kitLayoutOf(numberText, "the number")
        val wordLayout = kitLayoutOf(wordText, PlayTags.STATE_WORD)

        val m = measurer ?: throw AssertionError("kit: no text measurer from the composition")
        val natural = rule.runOnUiThread {
            val style = TextStyle(fontSize = KIT_LINE_SP.sp)
            m.measure(number, style, softWrap = false).size.width + m.measure(texts.inProgress, style, softWrap = false).size.width
        } + dp(KIT_DOTS_DP + 2 * KIT_SEPARATOR_DP)
        val content = counter.width - dp(2 * KIT_COUNTER_PADDING_DP)
        Log.i(
            "TopBarFitKitTest",
            "$language: natural $natural px at 13 sp, counter content $content px (ratio ${content / natural}); drawn sizes: number " +
                "${numberLayout.layoutInput.style.fontSize} (factor ${numberLayout.layoutInput.style.fontSize.value / KIT_LINE_SP}), word " +
                "${wordLayout.layoutInput.style.fontSize} (factor ${wordLayout.layoutInput.style.fontSize.value / KIT_LINE_SP}); density $density",
        )
        if (needsTheShrink) {
            assertTrue(
                "fixture: at 13 sp the line needs $natural px, more than the counter's $content px content, so an unshrunk line would not fit (E2a; review F5)",
                natural > content,
            )
        }

        Log.i("TopBarFitKitTest", "$language: number ${kitDescribe(numberLayout, number)}; box ${kitUnclipped(numberText)}")
        Log.i("TopBarFitKitTest", "$language: word ${kitDescribe(wordLayout, texts.inProgress)}; box ${kitUnclipped(wordText)}; tagged box ${kitUnclipped(wordNode)}")
        Log.i("TopBarFitKitTest", "$language: number ink ${kitInkRows(rule.onNode(hasText(number) and hasAnyAncestor(hasTestTag(PlayTags.COUNTER)), useUnmergedTree = true).captureToImage())}")
        Log.i("TopBarFitKitTest", "$language: word ink ${kitInkRows(rule.onNode(hasTestTag(PlayTags.STATE_WORD), useUnmergedTree = true).captureToImage())}")
        assertTrue("$language: the number \"$number\" is drawn whole (E2a: the number never ellipsizes): ${kitDescribe(numberLayout, number)}", kitDrawnWhole(numberLayout, number))
        assertTrue("$language: the state word \"${texts.inProgress}\" is drawn whole (E2a): ${kitDescribe(wordLayout, texts.inProgress)}", kitDrawnWhole(wordLayout, texts.inProgress))
        val numberBox = kitUnclipped(numberText)
        val wordBox = kitUnclipped(wordNode)
        val wordTextBox = kitUnclipped(wordText)
        assertTrue("$language: the number's node $numberBox lies inside `counter` $counter", kitWithin(numberBox, counter))
        assertTrue("$language: the state word's node $wordBox lies inside `counter` $counter", kitWithin(wordBox, counter))
        assertTrue("$language: the state word's text node $wordTextBox lies inside `counter` $counter", kitWithin(wordTextBox, counter))
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────────────────────────

    /** The target context resolved for [language] only, as `KitApp.contextFor` builds it (`createConfigurationContext`). */
    private fun kitContextFor(language: String): Context {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val conf = Configuration(target.resources.configuration)
        conf.setLocales(LocaleList(Locale.forLanguageTag(language)))
        return target.createConfigurationContext(conf)
    }

    /**
     * `PlayTexts` from `:app`'s string resources for [language] (G-10; O-06: every user-visible string is an `:app`
     * resource), one resource per field; the title language is the leg's (REQ-047: `fi` or `en`).
     */
    private fun kitTextsFor(language: String): PlayTexts {
        val c = kitContextFor(language)
        return PlayTexts(
            sizeSmall = c.getString(R.string.size_mark_small),
            sizeMedium = c.getString(R.string.size_mark_medium),
            sizeLarge = c.getString(R.string.size_mark_large),
            titleLanguage = language,
            restart = c.getString(R.string.restart),
            retry = c.getString(R.string.retry),
            next = c.getString(R.string.next),
            bestTime = c.getString(R.string.best_time),
            noTime = c.getString(R.string.no_time),
            inProgress = c.getString(R.string.state_in_progress),
            solved = c.getString(R.string.state_solved),
            counterFormat = c.getString(R.string.counter_format),
            allPuzzles = c.getString(R.string.all_puzzles),
            done = c.getString(R.string.done),
            hoursMinutes = c.getString(R.string.time_hours_minutes),
        )
    }

    /** The one node tagged [tag] (unmerged tree); none or two fail loudly. */
    private fun kitNode(tag: String): SemanticsNode {
        val nodes = rule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes()
        if (nodes.size != 1) throw AssertionError("kit: ${nodes.size} nodes tagged `$tag`, expected one")
        return nodes.single()
    }

    /** A node's own box in root px, whatever a parent clips (its position and size). */
    private fun kitUnclipped(n: SemanticsNode): Rect {
        val o = n.positionInRoot
        return Rect(o.x, o.y, o.x + n.size.width, o.y + n.size.height)
    }

    private fun kitWithin(inner: Rect, outer: Rect): Boolean =
        inner.left >= outer.left - KIT_EDGE_TOL_PX && inner.top >= outer.top - KIT_EDGE_TOL_PX &&
            inner.right <= outer.right + KIT_EDGE_TOL_PX && inner.bottom <= outer.bottom + KIT_EDGE_TOL_PX

    /**
     * The one node at or under [n] (unmerged tree) that shows [text] and carries a text layout: [n] itself for a text node,
     * its text child for a merged, inert text node (S-14Δ). None or two fail loudly.
     */
    private fun kitTextNodeUnder(n: SemanticsNode, text: String, what: String): SemanticsNode {
        val found = ArrayList<SemanticsNode>()
        fun walk(x: SemanticsNode) {
            val shown = x.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
            if (shown == text && x.config.getOrNull(SemanticsActions.GetTextLayoutResult) != null) found += x
            x.children.forEach { walk(it) }
        }
        walk(n)
        if (found.size != 1) throw AssertionError("kit: $what: ${found.size} text nodes at or under it show \"$text\" with a text layout, expected one")
        return found.single()
    }

    /** The node's text layout, through its `GetTextLayoutResult` action (a text node has one); none fails loudly. */
    private fun kitLayoutOf(n: SemanticsNode, what: String): TextLayoutResult {
        val action = n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action
            ?: throw AssertionError("kit: $what has no text layout to read (no GetTextLayoutResult action)")
        val out = ArrayList<TextLayoutResult>()
        rule.runOnUiThread { action(out) }
        return out.firstOrNull() ?: throw AssertionError("kit: $what returned no text layout")
    }

    /**
     * "Drawn whole": the layout is of [text], one line, not ellipsized, every character visible, and no visual overflow read as
     * the drawn line's extent (left, right, top, bottom) lying inside the text's own box ([TextLayoutResult.size]). Not
     * `hasVisualOverflow`: in this row it compares the box with the paragraph's layout width (the row's maximum), not with
     * the line drawn, and is true for a text whose box holds the whole line (measured on the floor, 2026-10-21: `fi` at
     * factor 1, "100 / 106" in a 144 px box laid out in a 299 px paragraph, its ink nowhere cut).
     */
    private fun kitDrawnWhole(r: TextLayoutResult, text: String): Boolean =
        r.layoutInput.text.text == text && r.lineCount == 1 && !r.isLineEllipsized(0) &&
            r.getLineEnd(0, visibleEnd = true) == text.length &&
            r.getLineLeft(0) >= -KIT_LINE_TOL_PX && r.getLineRight(0) <= r.size.width + KIT_LINE_TOL_PX &&
            r.getLineTop(0) >= -KIT_LINE_TOL_PX && r.getLineBottom(0) <= r.size.height + KIT_LINE_TOL_PX

    /** Diagnostic: a capture's size and its ink per row (pixels away from its most frequent colour), first and last rows. */
    private fun kitInkRows(img: ImageBitmap): String {
        val px = img.toPixelMap()
        val counts = HashMap<Int, Int>()
        for (y in 0 until px.height) for (x in 0 until px.width) counts.merge(px[x, y].toArgb(), 1, Int::plus)
        val bg = Color(counts.maxByOrNull { it.value }!!.key)
        fun inkIn(y: Int): Int = (0 until px.width).count { x ->
            val c = px[x, y]
            abs(c.red - bg.red) > 0.2f || abs(c.green - bg.green) > 0.2f || abs(c.blue - bg.blue) > 0.2f
        }
        fun inkAt(x: Int, y: Int): Boolean {
            val c = px[x, y]
            return abs(c.red - bg.red) > 0.2f || abs(c.green - bg.green) > 0.2f || abs(c.blue - bg.blue) > 0.2f
        }
        val rows = (0 until px.height).map { inkIn(it) }
        val cols = (0 until px.width).map { x -> (0 until px.height).count { y -> inkAt(x, y) } }
        return "${px.width} × ${px.height} px; ink rows ${rows.indexOfFirst { it > 0 }}..${rows.indexOfLast { it > 0 }}, " +
            "columns ${cols.indexOfFirst { it > 0 }}..${cols.indexOfLast { it > 0 }}; edge rows ${rows.first()}/${rows.last()}, edge columns ${cols.first()}/${cols.last()}"
    }

    private fun kitDescribe(r: TextLayoutResult, text: String): String =
        "layout of \"${r.layoutInput.text.text}\" (expected \"$text\"): ${r.size.width} × ${r.size.height} px, lines ${r.lineCount}, " +
            "visual overflow ${r.hasVisualOverflow} (width ${r.didOverflowWidth}, height ${r.didOverflowHeight}), paragraph " +
            "${r.multiParagraph.width} × ${r.multiParagraph.height} px, line left ${r.getLineLeft(0)} right ${r.getLineRight(0)} top ${r.getLineTop(0)} " +
            "baseline ${r.getLineBaseline(0)} bottom ${r.getLineBottom(0)}, " +
            "ellipsized ${r.isLineEllipsized(0)}, visible end ${r.getLineEnd(0, visibleEnd = true)} of ${text.length}, " +
            "font ${r.layoutInput.style.fontSize}, line height ${r.layoutInput.style.lineHeight}, constraints ${r.layoutInput.constraints}"
}
