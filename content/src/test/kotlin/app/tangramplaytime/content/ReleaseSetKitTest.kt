// Acceptance kit (WO-009, TASK-064, stage 1) — REQ-042 A1, A2 and A3 over the release set: the puzzle files the release
// APK of `:app` carries, loaded through `:content`'s own loader; and the untokened pins of the release set.
//
// - The reading (decisions.md 2026-10-22 R4, cited by its title without the dots: "REQ-039 A2 and REQ-042 A1..A3 are
//   read from the release APK; the REQ-042 legs stay red until the owner's marks, and run 2 of the JVM line runs with
//   `--continue` during WO-009."; tasks.md § WO-009 seam note 1, "Released means shipped"). The code home is `content/`
//   (build-map.md: #PuzzleVolume inherits #Content). WO-002's REQ-042 A2 and A3 tokens over the whole library
//   (PuzzleLibraryKitTest.kt) stay as they are; these legs re-cover the lines on what the release ships (decisions.md
//   2026-10-22 "Scope of WO-009 …": "REQ-042 A2..A3 are re-covered on the release bundle").
// - What the APK holds (tasks.md § WO-009 S-55, "today's output layout"): Java resources at the zip root, under
//   `BundledPuzzles.DIR`: `index.txt` (the sorted file names, one per line) and each file it names. The release set is
//   the files `index.txt` names (the order the app reads them in, `BundledPuzzles` KDoc), each read as UTF-8 into a
//   `PuzzleSource` and loaded with `PuzzleLoader.load(sources, kitEngine())` (the kit's door, designs/WO-002-design.md §2).
//   A named file missing from the zip fails. A load that is `Rejected` fails each leg with its violations (an empty
//   release set is `Rejected` with `EMPTY`: "there is at least one puzzle", the `load` KDoc).
// - Expected on the tree this stage lands on (tasks.md § WO-009 TASK-064 row): the release APK still carries all 106
//   files, none marked — REQ-042 A1 and the set-equality pin red, each naming its count; A2, A3 and the valid-load, mini
//   and warm-up pins green. From TASK-065 until the owner's marks, every release leg but the set-equality pin is the one
//   expected red (decisions.md R4; run 2 runs with `--continue`).
// - Discrimination: crafted release sets, made through the kit's doors (`kitEdited`, then the mark each fixture needs set in
//   memory by `kitWithMark`; `kitReviewed`), each loaded through the same loader and judged by the same six checks. The full
//   bar passes all six; each other set breaks exactly one item of the bar and fails exactly its own check
//   (task-decomposition.md, "a fixture must be able to distinguish the states the test claims to distinguish").
// - Corrected in the TASK-066 landing (decisions.md 2026-10-22 "ReleaseSetKitTest's crafted fixtures must not depend on the
//   shipped mark; back to the kit's author, a corrected check."): the fixtures took a shipped file's `"reviewedByHuman":
//   false` as their edit base, so they broke once the owner marked those files. Each fixture now sets the mark it needs,
//   whatever the shipped file holds, and reads it back.
// - The test builds nothing: run 1 of the JVM channel line builds the APK, this runs in run 2. A stale or missing APK is
//   refused with the build line (`ReleasePromiseKitTest`'s pattern). The staleness sources are the TASK-064 row's,
//   the top-level puzzle files among them, so a mark made after a build refuses the old APK.
// - Every check is computed and printed (the XML's system-out), then the test fails with every failed check. No
//   `assume…`; nothing is skipped. Helper names carry a `kit` / `Kit` / `KIT_` prefix and are members of this class;
//   `KitTexts.kt`'s helpers are reused, never changed.
package app.tangramplaytime.content

import app.tangramplaytime.contracts.PuzzleDefinition
import app.tangramplaytime.contracts.PuzzleKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import org.junit.Test
import java.io.File
import java.time.Instant
import java.util.zip.ZipFile

class ReleaseSetKitTest {

    private companion object {
        const val KIT_RELEASE_APK = "app/build/outputs/apk/release/app-release-unsigned.apk"

        /** Run 1 of the JVM channel line (workorders/WO-009-briefs.md § Verification channels), which builds it. */
        const val KIT_BUILD_LINE = ":app:assembleRelease :app:assembleDebug :app:packageRelease --rerun :app:packageDebug --rerun"

        /** S-55's output layout: the bundle's folder in the APK's Java resources, and its index. */
        const val KIT_PUZZLE_DIR = BundledPuzzles.DIR + "/"
        const val KIT_INDEX = "index.txt"

        /**
         * The TASK-064 row's staleness sources (its Inputs cell): `content/build.gradle.kts`, `app/build.gradle.kts`,
         * `settings.gradle.kts`, `gradle/libs.versions.toml`, the main source folders of `:content` and `:app` — and the
         * top-level puzzle files of `Tangrams/` ([kitFolderPuzzleFiles]).
         */
        val KIT_STALENESS_INPUTS = listOf(
            "content/build.gradle.kts", "app/build.gradle.kts", "settings.gradle.kts", "gradle/libs.versions.toml",
            "content/src/main", "app/src/main",
        )

        /** REQ-042 A1: "at least 20 puzzles with reviewedByHuman: true" (rule 1: "Count: at least 20"). */
        const val KIT_MIN_MARKED = 20

        /** REQ-042 A2: "At least four categories are present." (rule 2: "at least four of the categories"). */
        const val KIT_MIN_CATEGORIES = 4

        /** REQ-042 A3, as locked: "shapes-square is among them." */
        const val KIT_SQUARE = "shapes-square"

        /** REQ-045 statement: "a mini puzzle that uses exactly three pieces of TYPE-001". */
        const val KIT_MINI_PIECES = 3

        /** REQ-041 statement: "at least four warm-up puzzles of rating 1". */
        const val KIT_MIN_WARMUPS = 4

        /** How many names a message lists before it says "…". */
        const val KIT_SHOW = 12
    }

    /** The release set as read from the APK: the sources `index.txt` names, or null when it cannot be judged. */
    private data class KitRelease(val indexNames: List<String>, val otherFiles: List<String>, val sources: List<PuzzleSource>)

    /** One check of the bar. */
    private enum class KitItem { A1, A2, A3, LOADS, MINI, WARMUPS }

    // ── REQ-042 over the release APK ───────────────────────────────────────────────────────────

    // REQ-042.A1 — "The release contains at least 20 puzzles with reviewedByHuman: true." The release set (the files the
    // release APK carries, loaded through `PuzzleLoader.load`) lists at least 20 puzzles whose `reviewedByHuman` is true
    // (REQ-042 rule 1, "Count: at least 20"; `PuzzleDefinition.reviewedByHuman` KDoc: "the file's
    // `provenance.reviewedByHuman`"). Only the true ones count: the crafted set of 20 with one unmarked fails it.
    @Test
    fun theReleaseContainsAtLeast20PuzzlesWithReviewedByHumanTrue() { // REQ-042.A1
        kitReleaseLeg("theReleaseContainsAtLeast20PuzzlesWithReviewedByHumanTrue", KitItem.A1)
    }

    // REQ-042.A2 — "At least four categories are present." Among the release set's listed puzzles, at least four distinct
    // `category` values (REQ-042 rule 2: "Themes: at least four of the categories in ../Spec/03-puzzle-format.md"; as
    // locked, the theme-set draft is pending).
    @Test
    fun atLeastFourCategoriesArePresentInTheRelease() { // REQ-042.A2
        kitReleaseLeg("atLeastFourCategoriesArePresentInTheRelease", KitItem.A2)
    }

    // REQ-042.A3 — "shapes-square is among them." The release set lists the puzzle with the id `shapes-square` (as
    // locked; the content-ids draft is pending).
    @Test
    fun shapesSquareIsAmongTheReleasedPuzzles() { // REQ-042.A3
        kitReleaseLeg("shapesSquareIsAmongTheReleasedPuzzles", KitItem.A3)
    }

    // ── The release-set pins (no token) ────────────────────────────────────────────────────────

    // Pin, no token — tasks.md § WO-009 S-55: "the release bundle: those of them [every top-level `Tangrams/*.json` but
    // the schema] whose `provenance.reviewedByHuman` is JSON `true`. A missing `provenance` or field reads `false`"
    // (decisions.md 2026-10-11 "A missing `provenance` or `reviewedByHuman` reads as `false`."). The file names the
    // release APK's index.txt names, and the files its puzzle folder holds, are each exactly the marked puzzle files of the
    // Tangrams folder (seam note 1: "an unmarked puzzle file is absent from the release APK").
    @Test
    fun theReleaseFilesAreExactlyTheMarkedPuzzleFilesOfTheTangramsFolder() {
        val checks = mutableListOf<Pair<Boolean, String>>()
        val release = kitReadRelease(checks)
        val folder = kitFolderPuzzleFiles(kitTangramsDir())
        checks += folder.isNotEmpty() to "the Tangrams folder lists ${folder.size} top-level puzzle file(s) (liveness)"
        val marks = folder.associate { it.name to kitFolderMark(it) }
        val marked = marks.filterValues { it }.keys.sorted()
        println("ReleaseSetKitTest INFO the Tangrams folder marks ${marked.size} of ${folder.size} puzzle file(s)${kitList(marked)}")
        if (release != null) {
            val carried = release.indexNames.sorted()
            checks += (carried == marked) to
                "$KIT_INDEX names ${carried.size} file(s); the folder marks ${marked.size} of ${folder.size}; " +
                "named but not marked: ${(carried - marked.toSet()).size}${kitList(carried - marked.toSet())}; " +
                "marked but not named: ${(marked - carried.toSet()).size}${kitList(marked - carried.toSet())}"
            val held = (release.indexNames + release.otherFiles).distinct().sorted()
            checks += (held == marked) to
                "the APK's puzzle folder holds ${held.size} puzzle file(s); held but not marked: ${(held - marked.toSet()).size}" +
                "${kitList(held - marked.toSet())}; marked but not held: ${(marked - held.toSet()).size}${kitList(marked - held.toSet())}"
        }
        kitReport("theReleaseFilesAreExactlyTheMarkedPuzzleFilesOfTheTangramsFolder", checks)
    }

    // Pin, no token — REQ-007 statement ("The game SHALL ship a library of puzzles …"; A1, "Every shipped puzzle meets
    // REQ-038, REQ-039 and REQ-040", is WO-002's over the whole library) and IPuzzleLibrary KDoc ("Every listed puzzle is
    // valid"; "the list is never empty in a shipped build"): the release set loads with no violation.
    @Test
    fun theReleaseSetLoadsWithNoViolation() {
        kitReleaseLeg("theReleaseSetLoadsWithNoViolation", KitItem.LOADS)
    }

    // Pin, no token — REQ-045 statement ("The game SHALL start its puzzle list with at least one mini puzzle that uses
    // exactly three pieces of TYPE-001") and rule 3 ("Mini puzzles have rating 1 and sort before the warm-ups"); the
    // IPuzzleLibrary `puzzles` KDoc: "the first entry on a fresh install is a mini puzzle of rating 1 whose tray holds
    // three pieces". The release list's first puzzle is a mini whose piece set has exactly three pieces, rating 1.
    @Test
    fun theReleaseListOpensWithAThreePieceMiniOfRatingOne() {
        kitReleaseLeg("theReleaseListOpensWithAThreePieceMiniOfRatingOne", KitItem.MINI)
    }

    // Pin, no token — REQ-041 statement: "The game SHALL start its puzzle list, after the mini puzzles of REQ-045, with at
    // least four warm-up puzzles of rating 1". The four puzzles right after the release list's opening run of minis are
    // warm-ups of rating 1.
    @Test
    fun theReleaseListThenHasAtLeastFourWarmUpsOfRatingOne() {
        kitReleaseLeg("theReleaseListThenHasAtLeastFourWarmUpsOfRatingOne", KitItem.WARMUPS)
    }

    // ── Discrimination: crafted release sets (no token) ────────────────────────────────────────

    // The full bar passes every check: 20 marked puzzles over four categories (shapes, animals, things, letters), with
    // shapes-square, the three-piece mini shapes-mini-1 and the four warm-ups of rating 1. The control every crafted set
    // below departs from by exactly one item.
    @Test
    fun aCraftedReleaseMeetingTheWholeBarPassesEveryCheck() {
        kitFixture("aCraftedReleaseMeetingTheWholeBarPassesEveryCheck", kitBar(), failing = null)
    }

    // REQ-042 A1's count: 19 marked (the bar without letters-e; four categories kept) fails A1 alone.
    @Test
    fun aCraftedReleaseOf19MarkedPuzzlesFailsTheCount() {
        kitFixture("aCraftedReleaseOf19MarkedPuzzlesFailsTheCount", kitBar() - kitMarked("letters-e"), failing = KitItem.A1)
    }

    // REQ-042 A1 counts only the true ones: 20 files, one of them unmarked (letters-e set `false`), fails A1 alone — a
    // count of files would pass.
    @Test
    fun aCraftedReleaseOf20FilesWithOneUnmarkedFailsTheCount() {
        val set = kitBar() - kitMarked("letters-e") + kitUnmarked("letters-e")
        kitFixture("aCraftedReleaseOf20FilesWithOneUnmarkedFailsTheCount", set, failing = KitItem.A1)
    }

    // REQ-042 A1 reads "at least 20 … with reviewedByHuman: true": the bar plus one unmarked file (letters-f set `false`) is
    // 20 marked among 21 and passes every check of this class (REQ-039 A2, over the APK, is the line an unmarked
    // released file breaks: ReleaseMarksKitTest).
    @Test
    fun aCraftedReleaseOf20MarkedPlusOneUnmarkedMeetsTheCount() {
        kitFixture("aCraftedReleaseOf20MarkedPlusOneUnmarkedMeetsTheCount", kitBar() + kitUnmarked("letters-f"), failing = null)
    }

    // REQ-042 A2: 20 marked over three categories (the bar's four letters swapped for two animals and two things) fails A2
    // alone.
    @Test
    fun aCraftedReleaseOverThreeCategoriesFailsTheThemes() {
        val letters = listOf("letters-a", "letters-c", "letters-d", "letters-e").map { kitMarked(it) }
        val others = listOf("animals-chicken", "animals-crab", "things-key", "things-shoe").map { kitMarked(it) }
        kitFixture("aCraftedReleaseOverThreeCategoriesFailsTheThemes", kitBar() - letters.toSet() + others, failing = KitItem.A2)
    }

    // REQ-042 A3: 20 marked without shapes-square (swapped for shapes-rectangle) fails A3 alone.
    @Test
    fun aCraftedReleaseWithoutShapesSquareFailsTheSquare() {
        val set = kitBar() - kitMarked(KIT_SQUARE) + kitMarked("shapes-rectangle")
        kitFixture("aCraftedReleaseWithoutShapesSquareFailsTheSquare", set, failing = KitItem.A3)
    }

    // REQ-045 statement: no three-piece mini — shapes-mini-1 swapped for the kit's marked one-piece mini (`kitReviewed`),
    // so the list still opens on a mini of rating 1, of one piece — fails the mini pin alone.
    @Test
    fun aCraftedReleaseWhoseMiniHasOnePieceFailsTheMiniPin() {
        val set = kitBar() - kitMarked("shapes-mini-1") + kitReviewed()
        kitFixture("aCraftedReleaseWhoseMiniHasOnePieceFailsTheMiniPin", set, failing = KitItem.MINI)
    }

    // REQ-045 statement: no mini at all — shapes-mini-1 swapped for a marked full puzzle (letters-f), so the list opens on
    // a warm-up — fails the mini pin alone.
    @Test
    fun aCraftedReleaseWithNoMiniFailsTheMiniPin() {
        val set = kitBar() - kitMarked("shapes-mini-1") + kitMarked("letters-f")
        kitFixture("aCraftedReleaseWithNoMiniFailsTheMiniPin", set, failing = KitItem.MINI)
    }

    // REQ-041 statement: three warm-ups — shapes-warmup-4 swapped for a marked full puzzle (letters-f) — fails the
    // warm-up pin alone.
    @Test
    fun aCraftedReleaseWithThreeWarmUpsFailsTheWarmUpPin() {
        val set = kitBar() - kitMarked("shapes-warmup-4") + kitMarked("letters-f")
        kitFixture("aCraftedReleaseWithThreeWarmUpsFailsTheWarmUpPin", set, failing = KitItem.WARMUPS)
    }

    // REQ-041 statement, "of rating 1": four warm-ups, one of them rated 2 (shapes-warmup-4 edited `difficulty` 1 → 2;
    // a warm-up may have any rating 1..5, so it loads), fails the warm-up pin alone — a count of warm-ups would pass.
    @Test
    fun aCraftedReleaseWithAWarmUpOfRatingTwoFailsTheWarmUpPin() {
        val rated2 = kitWithMark(kitEdited("shapes-warmup-4.json", "shapes-warmup-4", "'difficulty': 1," to "'difficulty': 2,"), true)
        val set = kitBar() - kitMarked("shapes-warmup-4") + rated2
        kitFixture("aCraftedReleaseWithAWarmUpOfRatingTwoFailsTheWarmUpPin", set, failing = KitItem.WARMUPS)
    }

    // ── Kit helpers ──────────────────────────────────────────────────────────────────────────────

    /** The release APK's set, loaded, judged by one [item] of the bar. */
    private fun kitReleaseLeg(test: String, item: KitItem) {
        val checks = mutableListOf<Pair<Boolean, String>>()
        val release = kitReadRelease(checks)
        if (release != null) {
            val result = PuzzleLoader.load(release.sources, kitEngine())
            checks += kitJudge(item, "the release set (${release.sources.size} file(s))", result)
        }
        kitReport(test, checks)
    }

    /**
     * A crafted set through the same loader and the same six checks: it must load and be a set of distinct files, and
     * every check must pass but [failing], which must fail (null: every check passes).
     */
    private fun kitFixture(test: String, set: List<PuzzleSource>, failing: KitItem?) {
        val checks = mutableListOf<Pair<Boolean, String>>()
        val names = set.map { it.fileName }
        checks += (names.size == names.toSet().size) to "fixture: ${set.size} file(s), each named once"
        val marked = set.count { kitMark(it.text) }
        println("ReleaseSetKitTest INFO $test: ${set.size} file(s), $marked marked: ${names.sorted()}")
        val result = PuzzleLoader.load(set, kitEngine())
        for (item in KitItem.entries) {
            val (ok, text) = kitJudge(item, "the crafted set", result)
            val want = item != failing
            checks += (ok == want) to "${if (want) "passes" else "FAILS, as it must"} — ${if (ok) "pass" else "fail"}: $text"
        }
        kitReport(test, checks)
    }

    /** One item of the bar over one load; a `Rejected` load fails every item, naming its violations. */
    private fun kitJudge(item: KitItem, what: String, result: LoadResult): Pair<Boolean, String> {
        if (result is LoadResult.Rejected) {
            return false to "$what does not load (${item.name}): ${result.violations.size} violation(s) ${result.violations.take(KIT_SHOW)}"
        }
        val listed = (result as LoadResult.Loaded).library.puzzles
        return when (item) {
            KitItem.A1 -> {
                val marked = listed.count { it.reviewedByHuman }
                (marked >= KIT_MIN_MARKED) to
                    "REQ-042 A1: $what lists $marked of ${listed.size} puzzle(s) with reviewedByHuman true (at least $KIT_MIN_MARKED)"
            }
            KitItem.A2 -> {
                val categories = listed.map { it.category }.toSortedSet()
                (categories.size >= KIT_MIN_CATEGORIES) to
                    "REQ-042 A2: $what has ${categories.size} categories $categories (at least $KIT_MIN_CATEGORIES)"
            }
            KitItem.A3 -> {
                val among = listed.any { it.id == KIT_SQUARE }
                among to "REQ-042 A3: $KIT_SQUARE ${if (among) "is" else "is NOT"} among the ${listed.size} puzzle(s) $what lists"
            }
            KitItem.LOADS -> true to "REQ-007 statement: $what loads with no violation and lists ${listed.size} puzzle(s)"
            KitItem.MINI -> {
                val first = listed.firstOrNull()
                val ok = first != null && first.kind == PuzzleKind.MINI && first.pieces.size == KIT_MINI_PIECES && first.rating == 1
                ok to "REQ-045 statement and rule 3: the list of $what opens with ${kitDescribe(first)} " +
                    "(a mini of exactly $KIT_MINI_PIECES pieces, rating 1)"
            }
            KitItem.WARMUPS -> {
                val minis = listed.takeWhile { it.kind == PuzzleKind.MINI }.size
                val next = listed.drop(minis).take(KIT_MIN_WARMUPS)
                val ok = next.size == KIT_MIN_WARMUPS && next.all { it.kind == PuzzleKind.WARMUP && it.rating == 1 }
                ok to "REQ-041 statement: after $minis mini(s), the list of $what goes on with ${next.map { kitDescribe(it) }} " +
                    "(at least $KIT_MIN_WARMUPS warm-ups of rating 1)"
            }
        }
    }

    /** "id (kind, n pieces, rating r)", or "nothing". */
    private fun kitDescribe(p: PuzzleDefinition?): String =
        if (p == null) "nothing" else "${p.id} (${p.kind}, ${p.pieces.size} piece(s), rating ${p.rating})"

    /** The full bar: 20 shipped files, each marked `true` in memory ([kitMarked]), whatever the shipped file holds. */
    private fun kitBar(): List<PuzzleSource> = listOf(
        "shapes-mini-1", "shapes-warmup-1", "shapes-warmup-2", "shapes-warmup-3", "shapes-warmup-4", KIT_SQUARE,
        "animals-bat", "animals-bear", "animals-bird", "animals-camel", "animals-cat",
        "things-arrow", "things-bowl", "things-chair", "things-heart", "things-house",
        "letters-a", "letters-c", "letters-d", "letters-e",
    ).map { kitMarked(it) }

    /** A shipped file (its id kept, through `kitEdited`) with its mark set `true` in memory, whatever the shipped file holds. */
    private fun kitMarked(id: String): PuzzleSource = kitWithMark(kitEdited("$id.json", id), true)

    /** A shipped file (its id kept, through `kitEdited`) with its mark set `false` in memory, whatever the shipped file holds. */
    private fun kitUnmarked(id: String): PuzzleSource = kitWithMark(kitEdited("$id.json", id), false)

    /**
     * [source] with its one `"reviewedByHuman"` boolean set to [marked], its colon and spacing kept — whether the text held
     * `true` or `false` (decisions.md 2026-10-22 "ReleaseSetKitTest's crafted fixtures must not depend on the shipped mark;
     * back to the kit's author, a corrected check.": the owner's marks are the WO's planned state, so a fixture sets the mark
     * it needs). A text without exactly one such boolean fails loudly; the result is read back through [kitMark] (S-55's
     * reading), so a fixture never holds a mark other than the one it names.
     */
    private fun kitWithMark(source: PuzzleSource, marked: Boolean): PuzzleSource {
        val hits = Regex("\"reviewedByHuman\"\\s*:\\s*(true|false)").findAll(source.text).toList()
        if (hits.size != 1) throw AssertionError("kit: ${source.fileName} holds ${hits.size} \"reviewedByHuman\" booleans; setting its mark needs exactly one")
        val value = hits.single().groups[1] ?: throw AssertionError("kit: ${source.fileName}: no boolean after \"reviewedByHuman\"")
        val text = source.text.replaceRange(value.range, marked.toString())
        if (kitMark(text) != marked) throw AssertionError("kit: ${source.fileName} reads ${kitMark(text)} after its mark was set to $marked")
        return PuzzleSource(source.fileName, text)
    }

    /** A text's `provenance.reviewedByHuman`, JSON `true` or not (S-55; a missing one reads false, the 2026-10-11 row). */
    private fun kitMark(text: String): Boolean {
        val root = Json.parseToJsonElement(text) as? JsonObject ?: return false
        val value = (root["provenance"] as? JsonObject)?.get("reviewedByHuman") as? JsonPrimitive ?: return false
        return !value.isString && value.booleanOrNull == true
    }

    /** A Tangrams folder file's mark, read as UTF-8. */
    private fun kitFolderMark(file: File): Boolean = kitMark(file.readText(Charsets.UTF_8))

    /** The top-level puzzle files of the Tangrams folder: `*.json` but `*.schema.json` (S-54, S-55). */
    private fun kitFolderPuzzleFiles(dir: File): List<File> =
        dir.listFiles()?.filter { it.isFile && it.name.endsWith(".json") && !it.name.endsWith(".schema.json") }?.sortedBy { it.name }.orEmpty()

    /**
     * The release set of the release APK: liveness (`index.txt` read), every named file present, each read as UTF-8 into
     * a [PuzzleSource] in `index.txt`'s order. Null, with a failed check, when the APK cannot be judged.
     */
    private fun kitReadRelease(checks: MutableList<Pair<Boolean, String>>): KitRelease? {
        val apk = kitJudgeableApk(checks) ?: return null
        return ZipFile(apk).use { zip ->
            val index = zip.getEntry(KIT_PUZZLE_DIR + KIT_INDEX)?.let { e -> zip.getInputStream(e).use { String(it.readBytes(), Charsets.UTF_8) } }
            if (index == null) {
                checks += false to "no $KIT_PUZZLE_DIR$KIT_INDEX in $KIT_RELEASE_APK — the release set was not read (liveness)"
                return@use null
            }
            val names = index.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val others = zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith(KIT_PUZZLE_DIR) && it.name != KIT_PUZZLE_DIR + KIT_INDEX }
                .map { it.name.removePrefix(KIT_PUZZLE_DIR) }.filter { it !in names }.sorted().toList()
            checks += true to "$KIT_INDEX read, naming ${names.size} file(s); ${others.size} other file(s) under $KIT_PUZZLE_DIR${kitList(others)}"
            val absent = names.filter { zip.getEntry(KIT_PUZZLE_DIR + it) == null }
            checks += absent.isEmpty() to "${absent.size} file(s) named in $KIT_INDEX but missing from the zip${kitList(absent)}"
            val sources = names.filter { it !in absent }.map { name ->
                PuzzleSource(name, zip.getInputStream(zip.getEntry(KIT_PUZZLE_DIR + name)).use { String(it.readBytes(), Charsets.UTF_8) })
            }
            if (absent.isEmpty()) KitRelease(names, others, sources) else null
        }
    }

    /** ": a, b, …" (the first [KIT_SHOW] names, then how many more) for a non-empty list; "" for an empty one. */
    private fun kitList(names: List<String>): String =
        if (names.isEmpty()) "" else ": " + names.take(KIT_SHOW).joinToString(", ") + if (names.size > KIT_SHOW) ", … (${names.size - KIT_SHOW} more)" else ""

    /** Prints every check, then fails with every failed one. */
    private fun kitReport(test: String, checks: List<Pair<Boolean, String>>) {
        for ((ok, text) in checks) println("ReleaseSetKitTest ${if (ok) "PASS" else "FAIL"} $test: $text")
        val failed = checks.filter { !it.first }
        if (failed.isNotEmpty()) {
            throw AssertionError(failed.joinToString("\n", prefix = "$test: ${failed.size} of ${checks.size} checks failed:\n") { it.second })
        }
    }

    /** The nearest ancestor of the working directory (itself included) that holds settings.gradle.kts. */
    private fun kitProjectRoot(): File {
        val start = File(System.getProperty("user.dir") ?: throw AssertionError("no user.dir: cannot find the project root")).absoluteFile
        return generateSequence(start) { it.parentFile }.firstOrNull { File(it, "settings.gradle.kts").isFile }
            ?: throw AssertionError("no ancestor of $start holds settings.gradle.kts; cannot find the project root")
    }

    /**
     * The release APK when it exists and is not older than the newest file of [KIT_STALENESS_INPUTS] and the top-level
     * puzzle files; otherwise null, with a failed check saying why. A missing staleness source fails: staleness cannot
     * be judged.
     */
    private fun kitJudgeableApk(checks: MutableList<Pair<Boolean, String>>): File? {
        val root = kitProjectRoot()
        var newest: File? = null
        for (path in KIT_STALENESS_INPUTS) {
            val f = File(root, path)
            if (!f.exists()) {
                checks += false to "cannot judge staleness: $path does not exist under $root"
                return null
            }
            for (file in f.walkTopDown().filter { it.isFile }) {
                if (newest == null || file.lastModified() > newest.lastModified()) newest = file
            }
        }
        val puzzles = kitFolderPuzzleFiles(File(root, "Tangrams"))
        if (puzzles.isEmpty()) {
            checks += false to "cannot judge staleness: no top-level puzzle file in ${File(root, "Tangrams")}"
            return null
        }
        for (file in puzzles) if (newest == null || file.lastModified() > newest.lastModified()) newest = file
        val newestFile = newest ?: run {
            checks += false to "cannot judge staleness: no file under $KIT_STALENESS_INPUTS"
            return null
        }
        val apk = File(root, KIT_RELEASE_APK)
        if (!apk.isFile) {
            checks += false to "$KIT_RELEASE_APK not found — build it with run 1 of the JVM channel line ($KIT_BUILD_LINE)"
            return null
        }
        val newestText = "${newestFile.relativeTo(root).invariantSeparatorsPath} (${Instant.ofEpochMilli(newestFile.lastModified())})"
        if (apk.lastModified() < newestFile.lastModified()) {
            checks += false to "$KIT_RELEASE_APK (${Instant.ofEpochMilli(apk.lastModified())}) is STALE: older than $newestText — refused, not judged; rebuild with $KIT_BUILD_LINE"
            return null
        }
        checks += true to "$KIT_RELEASE_APK (${Instant.ofEpochMilli(apk.lastModified())}) is not older than the newest staleness source, $newestText"
        return apk
    }
}
