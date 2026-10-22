// Acceptance kit (WO-009, TASK-066, stage 2) — REQ-002 A2 over every player screen of `:play`, the code home (build-map.md
// #Solving: "REQ-002 A2 → WO-009"): the Play screen New, the Play screen In progress, the solved view and the grid overview.
//
// - The oracle (tasks.md § WO-009 S-63; decisions.md 2026-10-22 R5, "REQ-002 A2's oracle: every player screen offers only the
//   controls its REQs name.", its ID quoted here without the dot): "A mode is offered by a control, so the oracle is: on every
//   player screen, every control the screen offers is one its REQs name, and every visible text is in the closed world."
//   REQ-002 rule 2: "The target audience is 8 years and older; there are no modes for younger children."
// - The host: `KitScreen` — `PlayScreen` with the real engine and the bundled library (all 106 puzzles), at the kit's phone
//   layout (the S-5 literal) with the S-11 text literal `kitTexts`, and no DEV aid (KitScreen's default: the aid is REQ-046's,
//   a debug-only tool outside the player's screens, and its absence from the release is ReleaseBuildKitTest's REQ-046 A4).
// - Three checks on each screen, every problem naming its node:
//   (1) Controls with a semantics action. Every node with a click action, a long-click action or a toggle state is one of the
//       screen's controls in tasks.md § WO-006 "The player controls per screen" (the Play screen New and In progress, the
//       solved view, the grid overview), with the gear of S-53 on the three screens that show the top bar's columns; by its
//       tag, or — untagged — by its nearest tagged ancestor when it spans that ancestor's bounds within 1 px (decisions.md
//       2026-10-17 "Two readings of the device kit's closed-world checks." (2)). `dev-*` nodes are left out (§ WO-006: "Exempt:
//       every `dev-*` node"); none is composed here.
//   (2) Router controls. The router's controls carry no click action (S-53: "no `clickable`, no semantics click action";
//       § WO-007 seam note 2), so (1) cannot see them. `HitTest.kt`'s `Control` set, read as a declaration, is exactly the
//       nine controls the tag tables name, each with its tag (S-13Δ and S-22: ‹, ›, the counter, Restart, the solved row's
//       Retry, best time and Next; S-53: the gear; § WO-005's aid table: `dev-button`); and every one of them whose node the
//       screen shows is one of that screen's controls — § WO-006's list, plus the solved row's best time, which S-22 draws
//       inert ("Retry, best time (inert), Next"), plus, on the grid, the screen under it, whose bar "stays drawn and inert"
//       (S-22 "Overview").
//   (3) Visible texts. Every `Text` entry of the unmerged tree is one the screen's REQs give it: the S-11 / S-11Δ / S-11Δ3
//       texts the screen is given (`kitTexts`: the size marks, Restart, Retry, Next, Best time, "–", the state words, "All
//       puzzles", Done), the bundle's titles in the title language (S-11: `titles[titleLanguage]`), the counter's "n / N" for
//       this library (S-11Δ `counterFormat`; REQ-024 rule), and the times (decisions.md 2026-10-20 "One format for every time
//       the game shows: m:ss below one hour, "h h m min" from one hour (REQ-029's rule)."; S-11Δ3 `hoursMinutes`). Content
//       descriptions are not visible text; they are printed, not judged.
// - Planted controls, in this class (S-63: "The planted control each leg must catch, shown failing in the same class"): a host
//   composing `PlayScreen` with one extra clickable text "Toddler mode" beside the board, and one with an untagged clickable
//   icon box. On the Play screen New, the checks report the planted node and nothing else. The planted nodes are found through
//   a kit semantics key (`KitPlanted`), which is no test tag: they stay untagged.
// - Time (platform-notes/android.md § Compose test kit, WO-010): each screen's tree is read in one walk — the grid's 106 cells
//   in that walk, never one read per cell. Time the class on the floor before relying on it elsewhere (the TASK-066 brief).
// - Every check is computed and printed (logcat, `System.out`) before the test fails with every failed check. No `assume…`;
//   nothing is skipped. Helper names carry a `kit` / `Kit` / `KIT_` prefix and are members of this class; KitScreen.kt's
//   helpers are reused, never changed.
package app.tangramplaytime.play

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.PuzzleKind
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.play.devaid.DevAidTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class NoToddlerModeKitTest {

    @get:Rule
    val rule = createComposeRule()

    private companion object {
        /** A kit semantics key that marks a planted node so the test can find it; it is no test tag. */
        val KitPlanted = SemanticsPropertyKey<String>("KitPlanted")

        /** The planted text control (S-63: "one extra clickable text "Toddler mode" beside the board"). */
        const val KIT_TODDLER = "Toddler mode"

        /** The prefix of every #DevTools node's tag (§ WO-005's aid table; § WO-006 "Exempt: every `dev-*` node"). */
        const val KIT_DEV_PREFIX = "dev-"

        /**
         * `Control` (HitTest.kt, a declaration) by name → the tag its node carries: S-13Δ (`nav-prev`, `nav-next`, `counter`,
         * `restart`, `retry`, `best-time`, `solved-next`; "A control's bounds are its touch area"), S-53 (`PlayTags.GEAR =
         * "gear"`), § WO-005's aid table (`dev-button`).
         */
        val KIT_CONTROL_TAGS: Map<String, String> = mapOf(
            "PREV" to KitTag.NAV_PREV,
            "NEXT" to KitTag.NAV_NEXT,
            "COUNTER" to KitTag.COUNTER,
            "GEAR" to PlayTags.GEAR,
            "RESTART" to KitTag.RESTART,
            "RETRY" to KitTag.RETRY,
            "BEST_TIME" to KitTag.BEST_TIME,
            "SOLVED_NEXT" to KitTag.SOLVED_NEXT,
            "DEV_BUTTON" to DevAidTags.DEV_BUTTON,
        )
    }

    /** A player screen (tasks.md § WO-006 "The player controls per screen"). */
    private enum class KitWhere { NEW, IN_PROGRESS, SOLVED, GRID }

    /** One problem a check found, with the node it names. */
    private data class KitProblem(val node: SemanticsNode, val what: String)

    /** One screen read in one walk: every node of the unmerged tree, and the problems and liveness of the three checks. */
    private class KitJudged(val controls: List<KitProblem>, val router: List<KitProblem>, val texts: List<KitProblem>, val liveness: List<Pair<Boolean, String>>) {
        val problems: List<KitProblem> get() = controls + router + texts
    }

    // REQ-002.A2 — "No screen offers a toddler or picture-matching mode." Over the Play screen New (a full puzzle, all seven
    // pieces in the tray, the parallelogram among them), the Play screen In progress (two pieces placed through S-2), the solved
    // view (the puzzle solved through S-2, 3 s on: the picture fully shown and the solved row in place) and the grid overview
    // (opened by a tap on the counter, over the solved view): on each, every control the screen offers — by a semantics action
    // or as a router control — is one the tag tables and § WO-006's list name for that screen, and every visible text is in the
    // closed world (checks (1)–(3) of the header). Liveness on each screen: the walk read the screen's own nodes (the top bar's
    // four columns; on the grid at least one node with a click action, the grid's cells being foundation clickables, § WO-006
    // "Probe"), and the shown puzzle's title and "n / N" among its texts on the three Play screens. The checks' power to fail is
    // shown in this class by the two planted hosts below. Also here: the `Control` set is exactly the nine named controls.
    @Test
    fun noPlayerScreenOffersAControlOrATextItsReqsDoNotName() { // REQ-002.A2
        val checks = mutableListOf<Pair<Boolean, String>>()
        val names = Control.entries.map { it.name }
        checks += (names.toSet() == KIT_CONTROL_TAGS.keys && names.size == KIT_CONTROL_TAGS.size) to
            "the router's Control set (HitTest.kt) is ${names.sorted()}; the tag tables name ${KIT_CONTROL_TAGS.keys.sorted()}" +
            (names.filter { it !in KIT_CONTROL_TAGS }.let { if (it.isEmpty()) "" else " — Control entries no tag table names: $it" })

        val kit = KitScreen(rule)
        kit.show()
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        checks += (kit.shownState() == PuzzleState.NEW && PieceId.PG in p.pieces) to
            "fixture: ${p.id} open, ${kit.shownState()}, pieces ${p.pieces} (New, the parallelogram among them)"
        checks += kitReport("the Play screen New", kitJudge(kit, KitWhere.NEW))

        var placed = 0
        for (piece in kitBottomFirst(p)) {
            if (placed == 2) break
            if (kit.placeByApi(piece)) placed++
        }
        kit.frames(2)
        kit.advance(300)
        checks += (placed == 2 && kit.shownState() == PuzzleState.IN_PROGRESS) to
            "fixture: $placed pieces placed through S-2; ${p.id} ${kit.shownState()} (In progress)"
        checks += kitReport("the Play screen In progress", kitJudge(kit, KitWhere.IN_PROGRESS))

        kitSolveTheRest(kit)
        kit.advance(3000)
        kit.frames(2)
        checks += (kit.shownState() == PuzzleState.SOLVED && kit.exists(KitTag.SOLVED_ROW)) to
            "fixture: ${p.id} ${kit.shownState()}, solved row ${if (kit.exists(KitTag.SOLVED_ROW)) "shown" else "NOT shown"} (the solved view)"
        checks += kitReport("the solved view", kitJudge(kit, KitWhere.SOLVED))

        kit.press(KitTag.COUNTER)
        kit.advance(300)
        checks += (kit.exists(KitTag.OVERVIEW) && kit.onMain { kit.controller.overviewOpen }) to
            "fixture: a tap on the counter opened the grid overview (`overview` ${if (kit.exists(KitTag.OVERVIEW)) "shown" else "NOT shown"})"
        checks += kitReport("the grid overview", kitJudge(kit, KitWhere.GRID))
        kit.onMain { kit.controller.closeOverview() }
        kit.frames(2)

        kitFail("noPlayerScreenOffersAControlOrATextItsReqsDoNotName", checks)
    }

    // Pin, no token: the planted text control (S-63: "a kit host that composes `PlayScreen` with one extra clickable text
    // "Toddler mode" beside the board … The checks report the planted node"). On the Play screen New of the same full puzzle,
    // check (1) reports exactly the planted node (an untagged clickable with no tagged ancestor spanning it) and check (3)
    // exactly its text; nothing else is reported, so the plant alone is what the checks see.
    @Test
    fun aPlantedClickableToddlerModeTextIsReportedByTheControlAndTheTextChecks() {
        val checks = mutableListOf<Pair<Boolean, String>>()
        val kit = KitScreen(rule)
        kitShowWith(kit) {
            BasicText(
                KIT_TODDLER,
                Modifier.align(Alignment.CenterEnd).clickable { }.semantics { this[KitPlanted] = "text" },
            )
        }
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        val planted = kitPlanted()
        checks += (planted != null) to "fixture: the planted text node is composed${planted?.let { " at ${it.boundsInRoot}" } ?: ""}"
        val judged = kitJudge(kit, KitWhere.NEW)
        kitReport("the Play screen New with a planted \"$KIT_TODDLER\" text", judged)
        checks += judged.liveness
        if (planted != null) {
            checks += (judged.controls.isNotEmpty() && judged.controls.all { kitSameBox(it.node.boundsInRoot, planted.boundsInRoot) }) to
                "check (1) reports the planted node and nothing else: ${judged.controls.map { it.what }}"
            checks += (judged.texts.map { it.what }.let { t -> t.size == 1 && KIT_TODDLER in t.single() }) to
                "check (3) reports the planted text and nothing else: ${judged.texts.map { it.what }}"
            checks += judged.router.isEmpty() to "check (2) reports nothing: ${judged.router.map { it.what }}"
        }
        kitFail("aPlantedClickableToddlerModeTextIsReportedByTheControlAndTheTextChecks", checks)
    }

    // Pin, no token: the planted icon control (S-63: "… and a second with an untagged clickable icon box. The checks report the
    // planted node"). A 48 dp box with a drawn dot, clickable, untagged and without a text: on the Play screen New of the same
    // full puzzle, check (1) reports exactly the planted node; checks (2) and (3) report nothing — a text scan alone would miss
    // it (R5's rejected alternative: "misses an icon-only control").
    @Test
    fun aPlantedUntaggedClickableIconBoxIsReportedByTheControlCheck() {
        val checks = mutableListOf<Pair<Boolean, String>>()
        val kit = KitScreen(rule)
        kitShowWith(kit) {
            Box(
                Modifier.align(Alignment.CenterStart).size(48.dp).background(Color(0xFF7A5CFF))
                    .drawBehind { drawCircle(Color.White, radius = size.minDimension / 4f) }
                    .clickable { }.semantics { this[KitPlanted] = "icon" },
            )
        }
        val p = kitFirst(PuzzleKind.FULL)
        kit.open(p.id)
        val planted = kitPlanted()
        checks += (planted != null) to "fixture: the planted icon box is composed${planted?.let { " at ${it.boundsInRoot}" } ?: ""}"
        val judged = kitJudge(kit, KitWhere.NEW)
        kitReport("the Play screen New with a planted icon box", judged)
        checks += judged.liveness
        if (planted != null) {
            checks += (judged.controls.isNotEmpty() && judged.controls.all { kitSameBox(it.node.boundsInRoot, planted.boundsInRoot) }) to
                "check (1) reports the planted node and nothing else: ${judged.controls.map { it.what }}"
            checks += judged.texts.isEmpty() to "check (3) reports nothing (the box has no text): ${judged.texts.map { it.what }}"
            checks += judged.router.isEmpty() to "check (2) reports nothing: ${judged.router.map { it.what }}"
        }
        kitFail("aPlantedUntaggedClickableIconBoxIsReportedByTheControlCheck", checks)
    }

    // ── Kit helpers ──────────────────────────────────────────────────────────────────────────────

    /**
     * Composes `PlayScreen` exactly as `KitScreen.show()` does — the same controller, the S-5 layout literal, the S-11 texts,
     * no reduced motion, the whole window, no DEV aid — with [planted] beside it in one `Box`; then the same first layout,
     * paused clock and two frames as `show()`.
     */
    private fun kitShowWith(kit: KitScreen, planted: @Composable BoxScope.() -> Unit) {
        rule.mainClock.autoAdvance = true
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                PlayScreen(kit.controller, kitPhoneLayout, kitTexts, false, Modifier.fillMaxSize(), devAid = null)
                planted()
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        kit.frames(2)
        check(kit.exists(PlayTags.SCREEN)) { "kit: no `play-screen` node in the planted host" }
    }

    /**
     * Places every piece of the open puzzle that is not on the board yet at its solution pose through S-2 (`KitScreen.placeByApi`,
     * as `solveByApi` does for a board with nothing placed), as the engine allows, until the puzzle is solved.
     */
    private fun kitSolveTheRest(kit: KitScreen) {
        val p = kit.puzzle
        var remaining = p.pieces.toList() - kit.placed().map { it.piece }.toSet()
        while (remaining.isNotEmpty()) {
            val now = remaining.filter { kit.placeByApi(it) }
            check(now.isNotEmpty()) { "kit: none of $remaining locks at its solution pose in ${p.id}" }
            remaining = remaining - now.toSet()
            kit.frames()
        }
    }

    /** The planted node (the one node carrying [KitPlanted]), or null. */
    private fun kitPlanted(): SemanticsNode? = kitAllNodes().singleOrNull { KitPlanted in it.config }

    /** Every node of the unmerged tree, in one walk from the root. */
    private fun kitAllNodes(): List<SemanticsNode> {
        val out = ArrayList<SemanticsNode>()
        fun walk(n: SemanticsNode) {
            out += n
            n.children.forEach { walk(it) }
        }
        walk(rule.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        return out
    }

    /** The controls § WO-006's list (and S-53's gear) name for [where], for the puzzle [kit] shows. */
    private fun kitControlsOf(where: KitWhere, kit: KitScreen): Set<String> {
        val p = kit.puzzle
        val bar = setOf(KitTag.NAV_PREV, KitTag.NAV_NEXT, KitTag.COUNTER, PlayTags.GEAR)
        val cells = p.pieces.map { PlayTags.trayCell(it) }.toSet()
        val pg = PieceId.PG in p.pieces
        return when (where) {
            // "every `tray-cell-<ID>`; `flip-badge-tray` when the puzzle has the parallelogram"
            KitWhere.NEW -> bar + cells + (if (pg) setOf(PlayTags.FLIP_BADGE_TRAY) else emptySet())
            // "the same, plus `restart`, and `flip-badge-board` while the parallelogram is placed"
            KitWhere.IN_PROGRESS -> bar + cells + KitTag.RESTART + (if (pg) setOf(PlayTags.FLIP_BADGE_TRAY, PlayTags.FLIP_BADGE_BOARD) else emptySet())
            // "`nav-prev`, `nav-next`, `counter`, `retry`, `solved-next`. `best-time` is inert, not a control."
            KitWhere.SOLVED -> bar + KitTag.RETRY + KitTag.SOLVED_NEXT
            // "every `overview-cell-<id>`, `overview-done`, and the scrim (the `overview` node outside the sheet)"
            KitWhere.GRID -> kitLibrary.puzzles.map { KitTag.overviewCell(it.id) }.toSet() + KitTag.OVERVIEW_DONE + KitTag.OVERVIEW
        }
    }

    /**
     * The router controls [where] may draw: its controls, plus the solved row's best time on the solved view (S-22, drawn
     * inert); on the grid, also the solved view's under it (the walk opens the grid over the solved view; S-22: the bar "stays
     * drawn and inert").
     */
    private fun kitRouterAllowed(where: KitWhere, kit: KitScreen): Set<String> = when (where) {
        KitWhere.SOLVED -> kitControlsOf(KitWhere.SOLVED, kit) + KitTag.BEST_TIME
        KitWhere.GRID -> kitControlsOf(KitWhere.GRID, kit) + kitControlsOf(KitWhere.SOLVED, kit) + KitTag.BEST_TIME
        else -> kitControlsOf(where, kit)
    }

    /** The visible texts the screens' REQs give them (header, check (3)). */
    private fun kitWorld(): Set<String> {
        val n = kitLibrary.puzzles.size
        val out = LinkedHashSet<String>()
        with(kitTexts) { out += listOf(sizeSmall, sizeMedium, sizeLarge, restart, retry, next, bestTime, noTime, inProgress, solved, allPuzzles, done) }
        for (i in 1..n) out += String.format(Locale.ROOT, kitTexts.counterFormat, i, n)
        for (q in kitLibrary.puzzles) out += q.titles[kitTexts.titleLanguage] ?: q.titles.getValue("en")
        return out
    }

    /** m:ss below one hour; "h h m min" from one hour, from S-11Δ3's `hoursMinutes` with its two numbers (the 2026-10-20 row). */
    private fun kitIsTime(text: String): Boolean {
        if (Regex("^[0-9]+:[0-5][0-9]$").matches(text)) return true
        val parts = kitTexts.hoursMinutes.split(Regex("%[0-9]+\\\$d"))
        return parts.size == 3 && Regex("^" + parts.joinToString("[0-9]+") { Regex.escape(it) } + "$").matches(text)
    }

    /** The three checks over the screen [kit] shows now, as [where]; one walk of the tree. */
    private fun kitJudge(kit: KitScreen, where: KitWhere): KitJudged {
        val nodes = kitAllNodes()
        val tagOf = { n: SemanticsNode -> n.config.getOrNull(SemanticsProperties.TestTag) }
        val allowed = kitControlsOf(where, kit)
        val world = kitWorld()

        // (1) controls with a semantics action
        val controls = ArrayList<KitProblem>()
        var actionable = 0
        for (n in nodes) {
            val acts = SemanticsActions.OnClick in n.config || SemanticsActions.OnLongClick in n.config || SemanticsProperties.ToggleableState in n.config
            if (!acts) continue
            actionable++
            val tag = tagOf(n)
            if (tag != null) {
                if (!tag.startsWith(KIT_DEV_PREFIX) && tag !in allowed) controls += KitProblem(n, "`$tag` at ${n.boundsInRoot} has an action and is no control of ${where.name}")
            } else {
                var a = n.parent
                while (a != null && tagOf(a) == null) a = a.parent
                val owner = a?.let { tagOf(it) }
                val spans = a != null && kitSameBox(a.boundsInRoot, n.boundsInRoot)
                val ok = owner != null && spans && (owner in allowed || owner.startsWith(KIT_DEV_PREFIX))
                if (!ok) controls += KitProblem(n, "an untagged node with an action at ${n.boundsInRoot}${kitTextsOf(n)}, nearest tagged ancestor `$owner`${if (a != null) " at ${a.boundsInRoot}" else ""}")
            }
        }

        // (2) router controls the screen draws
        val routerAllowed = kitRouterAllowed(where, kit)
        val router = ArrayList<KitProblem>()
        val controlTags = KIT_CONTROL_TAGS.values.toSet()
        for (n in nodes) {
            val tag = tagOf(n) ?: continue
            if (tag in controlTags && !tag.startsWith(KIT_DEV_PREFIX) && tag !in routerAllowed) router += KitProblem(n, "the router control `$tag` is drawn at ${n.boundsInRoot}, no control of ${where.name}")
        }

        // (3) visible texts
        val texts = ArrayList<KitProblem>()
        val read = ArrayList<String>()
        for (n in nodes) {
            for (t in n.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }) {
                read += t
                if (t !in world && !kitIsTime(t)) texts += KitProblem(n, "the text \"$t\" at ${n.boundsInRoot}${tagOf(n)?.let { " (`$it`)" } ?: ""}")
            }
            n.config.getOrNull(SemanticsProperties.ContentDescription)?.let { println("NoToddlerModeKitTest INFO ${where.name}: a content description $it at ${n.boundsInRoot}") }
        }

        // liveness
        val tags = nodes.mapNotNull { tagOf(it) }.toSet()
        val liveness = ArrayList<Pair<Boolean, String>>()
        liveness += (nodes.size > 1) to "${where.name}: the walk read ${nodes.size} node(s), $actionable with an action, ${read.size} text(s)"
        val bar = listOf(KitTag.NAV_PREV, KitTag.NAV_NEXT, KitTag.COUNTER, PlayTags.GEAR)
        if (where == KitWhere.GRID) {
            liveness += (actionable > 0) to "${where.name}: the walk saw $actionable node(s) with an action (the grid's cells and Done are foundation clickables, § WO-006 \"Probe\")"
        } else {
            liveness += bar.all { it in tags } to "${where.name}: the top bar's four columns read: ${bar.filter { it in tags }}"
            val id = kit.shownId()
            val title = kitLibrary.byId(id)?.titles?.get(kitTexts.titleLanguage)
            liveness += (title != null && title in read && kitCounterText(id) in read) to
                "${where.name}: the shown puzzle's title \"$title\" and \"${kitCounterText(id)}\" among the texts read"
        }
        return KitJudged(controls, router, texts, liveness)
    }

    /** The texts of [n] and its descendants, for a problem's message. */
    private fun kitTextsOf(n: SemanticsNode): String {
        val out = ArrayList<String>()
        fun walk(m: SemanticsNode) {
            m.config.getOrNull(SemanticsProperties.Text)?.forEach { out += it.text }
            m.children.forEach { walk(it) }
        }
        walk(n)
        return if (out.isEmpty()) "" else " with the text $out"
    }

    /** Prints a screen's liveness and problems; returns them as checks (each problem one failed check). */
    private fun kitReport(screen: String, judged: KitJudged): List<Pair<Boolean, String>> {
        val out = ArrayList<Pair<Boolean, String>>()
        out += judged.liveness
        out += judged.controls.isEmpty() to "$screen, check (1): ${judged.controls.size} node(s) with an action that are no control of the screen"
        out += judged.router.isEmpty() to "$screen, check (2): ${judged.router.size} router control(s) drawn that are no control of the screen"
        out += judged.texts.isEmpty() to "$screen, check (3): ${judged.texts.size} text(s) outside the closed world"
        for (problem in judged.problems) out += false to "$screen: ${problem.what}"
        for ((ok, text) in out) println("NoToddlerModeKitTest ${if (ok) "PASS" else "FAIL"} $text")
        return out
    }

    /** Prints every check, then fails with every failed one. */
    private fun kitFail(test: String, checks: List<Pair<Boolean, String>>) {
        for ((ok, text) in checks) println("NoToddlerModeKitTest ${if (ok) "PASS" else "FAIL"} $test: $text")
        val failed = checks.filter { !it.first }
        if (failed.isNotEmpty()) {
            throw AssertionError(failed.joinToString("\n", prefix = "$test: ${failed.size} of ${checks.size} checks failed:\n") { it.second })
        }
    }

    /** Two boxes within 1 px at every edge (decisions.md 2026-10-17 "Two readings …" (2): "±1 px"). */
    private fun kitSameBox(a: Rect, b: Rect): Boolean =
        abs(a.left - b.left) <= 1f && abs(a.top - b.top) <= 1f && abs(a.right - b.right) <= 1f && abs(a.bottom - b.bottom) <= 1f
}
