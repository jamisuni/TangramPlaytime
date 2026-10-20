// Acceptance kit (WO-010, TASK-041) — kit stage 2 on the JVM: the asset record and the colour rule.
//
// - Two pins, no token (traceability-rules.md §2: a token is a coverage claim). Both serve REQ-007 A1
//   ("Every shipped puzzle meets REQ-038, REQ-039 and REQ-040"), whose token stays on PuzzleLibraryKitTest
//   (tasks.md § WO-010, the Q9 table: TASK-041 adds "the colour and record pins, untokened").
//   (a) Guardrail G-16 v1.2's record (architecture.md §2): "every shipped sound, font and image is listed
//       with its licence in `ASSET-LICENCES.md`", and each puzzle figure from a collection "is listed in
//       `ASSET-LICENCES.md` by puzzle id with its collection, source figure id, licence and credit; its
//       picture (`art`) is still made for this project". The file's shape is seam S-41 (tasks.md § WO-010 ›
//       Seams). It is kept complete against the Tangrams folder (decisions.md 2026-10-19
//       "`ASSET-LICENCES.md` lists every puzzle, kept complete by a JVM pin", R5: "every `Tangrams/*.json`
//       has exactly one row, every row names a file, and each figure has its licence"; the credit is the
//       `## Sources` row of the collection, keyed by the Figure cell).
//   (b) REQ-039 rule 1 ("The picture uses flat shapes, 3–8 colours and no text"), the colour count only,
//       over the imported pictures: every `## Puzzle figures` row whose Figure is a collection, counted as
//       seam S-42 counts ("the number of distinct `#RRGGBB` values (case-insensitive) among `art.base` and
//       every shape's `fill` and `stroke`"). The `this project` rows are outside it (decisions.md 2026-10-11
//       "What 'a well-formed picture' means": `shapes-mini-2` uses two colours, a note for the owner's
//       review; decisions.md 2026-10-19 reading R8, "REQ-039 A1's oracle over the imported pictures" — its
//       title written here without the dot, traceability-rules.md §2 — : "the 13 stay out of it"). The stub
//       picture of S-39 has one colour, so a picture not drawn fails, naming its id.
// - Data read at run time: `ASSET-LICENCES.md` in the project root (S-41), the Tangrams folder listing
//   (`kitTangramsPuzzleIds()`, KitTexts.kt) and each imported puzzle's picture through `:content`
//   (`PuzzleLibraries.bundled`, the door of PuzzleLibraryKitTest). The parser is written from S-41's text.
// - Fixtures discriminate (task-decomposition.md, "Assert only what is mandated"): the same parsing,
//   checking and counting functions run on crafted texts. Each crafted record breaks exactly one check and
//   must fail that check only, naming that id only; the crafted base record passes every check. The colour
//   count runs on crafted pictures loaded through the kit's own door (`kitLoadWithShipped`).
// - Cell values that S-41 does not ask to be in backticks (Figure, Licence, Picture, `—`) are read with or
//   without one pair of backticks: S-41's own backticks there quote the value. Puzzle id and Source figure
//   id are "in backticks" by S-41's words, so their backticks are checked.
// - Helper names carry a `kit` / `Kit` / `KIT_` prefix; KitTexts.kt helpers are reused, never changed.
//   JUnit 4; no `assume…`; no reflection; no dotted acceptance ID in this file.
package app.tangramplaytime.content

import app.tangramplaytime.contracts.ArtLayer
import app.tangramplaytime.kernel.LockEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AssetLicencesKitTest {

    /** The checks of pin (a), one per S-41 sentence, so that a fixture can show which check fired. */
    private enum class KitCheck {
        /** The sections and the two tables' columns S-41 names. */
        SHAPE,

        /** S-41: "Puzzle id is in backticks." */
        ID_CELL,

        // S-41: "one row per `Tangrams/*.json`" — a file without a row. (Line comments: Kotlin block comments nest.)
        MISSING_ROW,

        /** R5: "every row names a file" — a row without a file. */
        ROW_WITHOUT_FILE,

        // S-41: "one row per `Tangrams/*.json`"; R5: "exactly one row" — a puzzle listed twice.
        DUPLICATE_ROW,

        /** S-41: "Figure is `this project`, `convex-13` or `android-tangram-131`." */
        FIGURE,

        /** S-41: the Licence of each Figure. */
        LICENCE,

        /** S-41: Source figure id and Source name — the collection's, or `—` for this project. */
        SOURCE_CELLS,

        /** S-41: "Picture is `made for this project` on every row." */
        PICTURE,

        /** S-41: "Rows are sorted by puzzle id." */
        ORDER,

        /** S-41 `## Sources`: one row per figure source; a collection's licence, where from and credit. */
        SOURCES,
    }

    /** One finding: the check, the puzzle id (or source, or heading) it names, and what is wrong. */
    private data class KitProblem(val check: KitCheck, val subject: String, val detail: String) {
        override fun toString(): String = "$check $subject: $detail"
    }

    /** A markdown table: its header cells and body rows, each cell trimmed. */
    private data class KitTable(val header: List<String>, val rows: List<List<String>>)

    /** One `## Puzzle figures` row. [idCell] and [sourceIdCell] as written; the other values unquoted. */
    private data class KitFigureRow(
        val id: String,
        val idCell: String,
        val figure: String,
        val sourceIdCell: String,
        val sourceName: String,
        val licence: String,
        val picture: String,
    )

    /** The record as read: its puzzle-figure rows and every problem found. */
    private data class KitRecordRead(val rows: List<KitFigureRow>, val problems: List<KitProblem>)

    private companion object {
        // ── S-41's words ──────────────────────────────────────────────────────────────────────────

        /** S-41: "`ASSET-LICENCES.md`, new, project root". */
        const val KIT_RECORD_FILE = "ASSET-LICENCES.md"

        /** S-41: "`## Sources`: a table with the columns Source · Where from · Licence · Credit". */
        const val KIT_SOURCES_HEADING = "## Sources"
        val KIT_SOURCES_COLUMNS = listOf("Source", "Where from", "Licence", "Credit")

        /** S-41: "`## Puzzle figures`: a table with the columns Puzzle id · Figure · Source figure id · Source name · Licence · Picture". */
        const val KIT_FIGURES_HEADING = "## Puzzle figures"
        val KIT_FIGURES_COLUMNS = listOf("Puzzle id", "Figure", "Source figure id", "Source name", "Licence", "Picture")

        /** S-41: "`## Pictures, fonts, sounds`: every puzzle picture is made for this project; system font only (ADR-005); no sound yet". */
        const val KIT_ASSETS_HEADING = "## Pictures, fonts, sounds"

        /** S-41: "Figure is `this project`, `convex-13` or `android-tangram-131`." */
        const val KIT_THIS_PROJECT = "this project"
        const val KIT_CONVEX = "convex-13"
        const val KIT_ANDROID = "android-tangram-131"
        val KIT_COLLECTIONS: Set<String> = setOf(KIT_CONVEX, KIT_ANDROID)

        /**
         * S-41: "Licence is `GPL-3.0` for this project (the app's `LICENSE`) and for `android-tangram-131`,
         * and `CC0-1.0` for `convex-13`." The same pairs are S-41's `## Sources` licences of the collections.
         */
        val KIT_LICENCE_OF: Map<String, String> =
            mapOf(KIT_THIS_PROJECT to "GPL-3.0", KIT_CONVEX to "CC0-1.0", KIT_ANDROID to "GPL-3.0")

        /** S-41: "Picture is `made for this project` on every row." */
        const val KIT_PICTURE = "made for this project"

        /** S-41: Source figure id and Source name "are `—` for this project" (U+2014, the em dash). */
        const val KIT_NONE = "—"

        /** REQ-039 rule 1: "3–8 colours". */
        val KIT_COLOURS: IntRange = 3..8

        /** S-39: "`art` from TASK-036: the stub `{"base": "#D9DCE8", "shapes": []}`. It has one colour". */
        const val KIT_STUB_BASE = "#D9DCE8"

        // ── Reading the record ────────────────────────────────────────────────────────────────────

        /** The project's `ASSET-LICENCES.md`: the Tangrams folder's parent is the project root. Fails loudly. */
        fun kitRecordFileText(): String {
            val file = File(kitTangramsDir().parentFile, KIT_RECORD_FILE)
            if (!file.isFile) throw AssertionError("no $KIT_RECORD_FILE at ${file.absolutePath} (S-41: project root)")
            return file.readText(Charsets.UTF_8)
        }

        /** [cell] without one surrounding pair of backticks, if it has one. */
        fun kitUnquote(cell: String): String =
            if (cell.length >= 2 && cell.startsWith("`") && cell.endsWith("`")) cell.substring(1, cell.length - 1).trim() else cell

        /** Whether [cell] is a non-empty value in one pair of backticks. */
        fun kitBackticked(cell: String): Boolean = Regex("`[^`]+`").matches(cell)

        /** Whether [cell] holds no value: blank, or `—`. */
        fun kitEmptyCell(cell: String): Boolean = kitUnquote(cell).isBlank() || kitUnquote(cell) == KIT_NONE

        /** The lines under the line [heading], up to the next `## ` heading; null when the heading is absent. */
        fun kitSection(lines: List<String>, heading: String): List<String>? {
            val start = lines.indexOfFirst { it.trim() == heading }
            if (start < 0) return null
            val body = lines.drop(start + 1)
            val end = body.indexOfFirst { it.trimStart().startsWith("## ") }
            return if (end < 0) body else body.take(end)
        }

        /** The cells of one table line: the outer pipes dropped, split on `|`, each trimmed. */
        fun kitCells(line: String): List<String> =
            line.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }

        /** The first table of a section: its first run of `|` lines (header, delimiter row, body). Null when none. */
        fun kitTable(section: List<String>): KitTable? {
            val first = section.indexOfFirst { it.trimStart().startsWith("|") }
            if (first < 0) return null
            val run = section.drop(first).takeWhile { it.trimStart().startsWith("|") }
            if (run.size < 2 || !kitCells(run[1]).all { Regex(":?-+:?").matches(it) }) return null
            return KitTable(kitCells(run[0]), run.drop(2).map { kitCells(it) })
        }

        /**
         * Reads a record [text] as S-41 shapes it and checks it against [folderIds] (the puzzle files of
         * the Tangrams folder, as stems). Returns the puzzle-figure rows and every problem, each tagged
         * with the check that found it.
         */
        fun kitReadRecord(text: String, folderIds: List<String>): KitRecordRead {
            val problems = mutableListOf<KitProblem>()
            val lines = text.lines()

            // ── S-41's shape: the three sections, and the two tables with S-41's columns ──
            if (kitSection(lines, KIT_ASSETS_HEADING) == null) {
                problems += KitProblem(KitCheck.SHAPE, KIT_ASSETS_HEADING, "section missing (S-41)")
            }
            val sources = kitSection(lines, KIT_SOURCES_HEADING)?.let { kitTable(it) }
            val figures = kitSection(lines, KIT_FIGURES_HEADING)?.let { kitTable(it) }
            for ((heading, table, columns) in listOf(
                Triple(KIT_SOURCES_HEADING, sources, KIT_SOURCES_COLUMNS),
                Triple(KIT_FIGURES_HEADING, figures, KIT_FIGURES_COLUMNS),
            )) {
                if (table == null) {
                    problems += KitProblem(KitCheck.SHAPE, heading, "section or its table missing (S-41)")
                } else if (table.header != columns) {
                    problems += KitProblem(KitCheck.SHAPE, heading, "columns ${table.header}; S-41 names $columns")
                }
            }
            if (sources == null || figures == null || problems.any { it.subject != KIT_ASSETS_HEADING }) {
                return KitRecordRead(emptyList(), problems) // the rows cannot be read against S-41's columns
            }

            // ── `## Sources`: "one row per figure source" (S-41); a collection's licence (S-41's pairs),
            //    where from, and credit ("The credit text comes from each collection's `NOTICE.md`"; R5:
            //    the credit is the collection's `## Sources` row, keyed by the Figure cell) ──
            for (cells in sources.rows) {
                if (cells.size != KIT_SOURCES_COLUMNS.size) {
                    problems += KitProblem(KitCheck.SHAPE, kitUnquote(cells.first()), "${cells.size} cells in $KIT_SOURCES_HEADING; S-41 names ${KIT_SOURCES_COLUMNS.size}")
                }
            }
            for (source in listOf(KIT_THIS_PROJECT, KIT_CONVEX, KIT_ANDROID)) {
                val hits = sources.rows.filter { it.size == KIT_SOURCES_COLUMNS.size && kitUnquote(it[0]) == source }
                if (hits.size != 1) {
                    problems += KitProblem(KitCheck.SOURCES, source, "${hits.size} rows in $KIT_SOURCES_HEADING; S-41: one row per figure source")
                    continue
                }
                if (source !in KIT_COLLECTIONS) continue
                val (_, whereFrom, licence, credit) = hits.single()
                if (kitUnquote(licence) != KIT_LICENCE_OF.getValue(source)) {
                    problems += KitProblem(KitCheck.SOURCES, source, "licence '$licence'; S-41: ${KIT_LICENCE_OF.getValue(source)}")
                }
                if (kitEmptyCell(whereFrom)) problems += KitProblem(KitCheck.SOURCES, source, "no Where from (S-41)")
                if (kitEmptyCell(credit)) problems += KitProblem(KitCheck.SOURCES, source, "no Credit (S-41; G-16: \"licence and credit\")")
            }

            // ── `## Puzzle figures`: the rows ──
            val rows = mutableListOf<KitFigureRow>()
            for (cells in figures.rows) {
                if (cells.size != KIT_FIGURES_COLUMNS.size) {
                    problems += KitProblem(KitCheck.SHAPE, kitUnquote(cells.first()), "${cells.size} cells in $KIT_FIGURES_HEADING; S-41 names ${KIT_FIGURES_COLUMNS.size}")
                    continue
                }
                val id = kitUnquote(cells[0])
                // S-41: "Puzzle id is in backticks."
                if (!kitBackticked(cells[0])) problems += KitProblem(KitCheck.ID_CELL, id, "Puzzle id '${cells[0]}' is not in backticks (S-41)")
                rows += KitFigureRow(
                    id = id,
                    idCell = cells[0],
                    figure = kitUnquote(cells[1]),
                    sourceIdCell = cells[2],
                    sourceName = kitUnquote(cells[3]),
                    licence = kitUnquote(cells[4]),
                    picture = kitUnquote(cells[5]),
                )
            }
            val rowIds = rows.map { it.id }

            // S-41: "one row per `Tangrams/*.json`"; R5: "every `Tangrams/*.json` has exactly one row,
            // every row names a file". The brief: "A file missing from the table, or a table row without a
            // file, fails with the id."
            for (id in folderIds) {
                if (id !in rowIds) problems += KitProblem(KitCheck.MISSING_ROW, id, "Tangrams/$id.json has no row")
            }
            for (id in rowIds.distinct()) {
                if (id !in folderIds) problems += KitProblem(KitCheck.ROW_WITHOUT_FILE, id, "no Tangrams/$id.json for this row")
            }
            for ((id, count) in rowIds.groupingBy { it }.eachCount()) {
                if (count > 1) problems += KitProblem(KitCheck.DUPLICATE_ROW, id, "$count rows; S-41: one row per file")
            }

            for (row in rows) {
                // S-41: "Figure is `this project`, `convex-13` or `android-tangram-131`."
                val licence = KIT_LICENCE_OF[row.figure]
                if (licence == null) {
                    problems += KitProblem(KitCheck.FIGURE, row.id, "Figure '${row.figure}'; S-41: ${KIT_LICENCE_OF.keys}")
                } else {
                    // S-41: "Licence is `GPL-3.0` for this project ... and for `android-tangram-131`, and
                    // `CC0-1.0` for `convex-13`." G-16: each figure is listed "with its ... licence".
                    if (row.licence != licence) {
                        problems += KitProblem(KitCheck.LICENCE, row.id, "Licence '${row.licence}' for ${row.figure}; S-41: $licence")
                    }
                    if (row.figure == KIT_THIS_PROJECT) {
                        // S-41: Source figure id and Source name "are `—` for this project".
                        if (kitUnquote(row.sourceIdCell) != KIT_NONE || row.sourceName != KIT_NONE) {
                            problems += KitProblem(KitCheck.SOURCE_CELLS, row.id, "this project row with source '${row.sourceIdCell}' / '${row.sourceName}'; S-41: an em dash (U+2014) in both")
                        }
                    } else {
                        // S-41: "Source figure id (the collection's `id`, in backticks) and Source name (its
                        // `name`)"; G-16: "by puzzle id with its collection, source figure id, licence and credit".
                        if (!kitBackticked(row.sourceIdCell) || kitEmptyCell(row.sourceIdCell)) {
                            problems += KitProblem(KitCheck.SOURCE_CELLS, row.id, "Source figure id '${row.sourceIdCell}'; S-41: the collection's id, in backticks")
                        }
                        if (kitEmptyCell(row.sourceName)) {
                            problems += KitProblem(KitCheck.SOURCE_CELLS, row.id, "no Source name; S-41: the collection's name")
                        }
                    }
                }
                // S-41: "Picture is `made for this project` on every row." G-16: "its picture (`art`) is still
                // made for this project".
                if (row.picture != KIT_PICTURE) {
                    problems += KitProblem(KitCheck.PICTURE, row.id, "Picture '${row.picture}'; S-41: $KIT_PICTURE")
                }
            }

            // S-41: "Rows are sorted by puzzle id." (A repeated id is DUPLICATE_ROW's, not an order break.)
            for ((before, after) in rowIds.zipWithNext()) {
                if (before > after) problems += KitProblem(KitCheck.ORDER, after, "listed after $before; S-41: sorted by puzzle id")
            }
            return KitRecordRead(rows, problems)
        }

        // ── Counting colours (S-42) ───────────────────────────────────────────────────────────────

        /** S-42: the distinct `#RRGGBB` values, case-insensitive, among `art.base` and every shape's `fill` and `stroke`. */
        fun kitColourCount(art: ArtLayer): Int =
            (listOf(art.base) + art.shapes.flatMap { listOfNotNull(it.style.fill, it.style.stroke) })
                .map { it.uppercase() }
                .toSet()
                .size

        /**
         * What breaks REQ-039 rule 1's colour count for the puzzle [id] whose listed picture is [art] (null:
         * no listed puzzle), as text; null when it holds. A picture that is not well formed is reported first:
         * S-42 counts `#RRGGBB` values (KitTexts.kt `kitArtProblems`, decisions.md 2026-10-11 "What 'a
         * well-formed picture' means").
         */
        fun kitColourProblem(id: String, art: ArtLayer?): String? {
            if (art == null) return "$id: no listed puzzle with this id"
            val malformed = kitArtProblems(art)
            if (malformed.isNotEmpty()) return "$id: picture not well formed: $malformed"
            val count = kitColourCount(art)
            if (count in KIT_COLOURS) return null
            val stub = art.shapes.isEmpty() && art.base.equals(KIT_STUB_BASE, ignoreCase = true)
            val note = if (stub) " (S-39's stub: the picture is not drawn)" else ""
            return "$id: $count distinct colour(s); REQ-039 rule 1: 3 to 8$note"
        }

        // ── Crafted records (hand-made; S-41's shape with three puzzles, one per figure source) ──

        /** One `## Puzzle figures` line from its six cells as written. */
        fun kitFigureLine(id: String, figure: String, sourceId: String, sourceName: String, licence: String, picture: String = KIT_PICTURE): String =
            "| $id | $figure | $sourceId | $sourceName | $licence | $picture |"

        /** One `## Sources` line from its four cells as written. */
        fun kitSourceLine(source: String, whereFrom: String, licence: String, credit: String): String =
            "| $source | $whereFrom | $licence | $credit |"

        /** The crafted Tangrams folder: one puzzle per figure source; ids carry a `kit` segment (S-39). */
        val KIT_FOLDER: List<String> = listOf("animals-kit-own", "shapes-kit-convex", "things-kit-android")

        val KIT_OWN_ROW = kitFigureLine("`animals-kit-own`", KIT_THIS_PROJECT, KIT_NONE, KIT_NONE, "GPL-3.0")
        val KIT_CONVEX_ROW = kitFigureLine("`shapes-kit-convex`", KIT_CONVEX, "`convex-01`", "Convex 1", "CC0-1.0")
        val KIT_ANDROID_ROW = kitFigureLine("`things-kit-android`", KIT_ANDROID, "`objects-bowl`", "Bowl", "GPL-3.0")
        val KIT_FIGURE_ROWS: List<String> = listOf(KIT_OWN_ROW, KIT_CONVEX_ROW, KIT_ANDROID_ROW)

        val KIT_OWN_SOURCE = kitSourceLine(KIT_THIS_PROJECT, "`Tangrams/*.json`, drawn for this project", "GPL-3.0", "this repository, `LICENSE`")
        val KIT_CONVEX_SOURCE = kitSourceLine(KIT_CONVEX, "Wikimedia Commons, File:Convex_tangram_shapes.svg", "CC0-1.0", "László Németh; CC0 1.0")
        val KIT_ANDROID_SOURCE = kitSourceLine(KIT_ANDROID, "github.com/Typraeurion/Android-Tangram", "GPL-3.0", "Trevin Beattie; GPL-3.0")
        val KIT_SOURCE_ROWS: List<String> = listOf(KIT_OWN_SOURCE, KIT_CONVEX_SOURCE, KIT_ANDROID_SOURCE)

        /** A hand-made record in S-41's shape, from its table lines; a section can be left out. */
        fun kitCraftedRecord(
            sourceRows: List<String> = KIT_SOURCE_ROWS,
            figureRows: List<String> = KIT_FIGURE_ROWS,
            figureColumns: List<String> = KIT_FIGURES_COLUMNS,
            withFigures: Boolean = true,
            withAssets: Boolean = true,
        ): String = buildList<String> {
            add("# Asset licences — kit")
            add("")
            add("Hand-made for AssetLicencesKitTest (S-41's shape).")
            add("")
            add(KIT_SOURCES_HEADING)
            add("")
            add(KIT_SOURCES_COLUMNS.joinToString(" | ", "| ", " |"))
            add(KIT_SOURCES_COLUMNS.joinToString("|", "|", "|") { "---" })
            addAll(sourceRows)
            if (withFigures) {
                add("")
                add(KIT_FIGURES_HEADING)
                add("")
                add("One row per `Tangrams/*.json`, sorted by puzzle id.")
                add("")
                add(figureColumns.joinToString(" | ", "| ", " |"))
                add(figureColumns.joinToString("|", "|", "|") { "---" })
                addAll(figureRows)
            }
            if (withAssets) {
                add("")
                add(KIT_ASSETS_HEADING)
                add("")
                add("- **Pictures:** made for this project. **Fonts:** the system font only. **Sounds:** none yet.")
            }
        }.joinToString("\n", postfix = "\n")

        /** [rows] with the line [old] (exactly once) replaced by [new]. */
        fun kitReplaced(rows: List<String>, old: String, new: String): List<String> {
            if (rows.count { it == old } != 1) throw AssertionError("kit row base changed: '$old' must occur exactly once")
            return rows.map { if (it == old) new else it }
        }

        /** Asserts that [problems] come from [check] only and name [subject] only. */
        fun kitAssertOnly(problems: List<KitProblem>, check: KitCheck, subject: String) {
            assertTrue("expected $check for $subject; got no problem", problems.isNotEmpty())
            assertEquals("only $check may fire; got $problems", setOf(check), problems.map { it.check }.toSet())
            assertEquals("only $subject may be named; got $problems", setOf(subject), problems.map { it.subject }.toSet())
        }

        /** The problems of a crafted record over the crafted folder. */
        fun kitProblemsOf(text: String, folder: List<String> = KIT_FOLDER): List<KitProblem> =
            kitReadRecord(text, folder).problems

        // ── Crafted pictures (hand-made one-piece minis; KitTexts.kt `kitHandMade`) ──

        /** The small triangle (0,0)(2,0)(1,1) at turn 0 (KitTexts.kt `kitOnePieceMini`): a valid one-piece mini. */
        const val KIT_ONE_PIECE = "{'piece': 'ST1', 'polygon': [[0, 0], [2, 0], [1, 1]]}"

        /** Art shapes: one small filled circle per colour in [fills] (single-quoted JSON, KitTexts.kt `kitQ`). */
        fun kitCircles(vararg fills: String): String =
            fills.joinToString(", ") { "{'type': 'circle', 'c': [1, 0.4], 'r': 0.1, 'fill': '$it'}" }
    }

    // ── Pin (a): G-16 v1.2's record, S-41 ──────────────────────────────────────────────────────────

    @Test // guardrail G-16
    fun theAssetRecordListsEveryPuzzleFileOnceWithItsFigureLicenceAndPicture() {
        // R5: "every `Tangrams/*.json` has exactly one row, every row names a file, and each figure has its
        // licence"; S-41 the shape, the pairs and the Picture cell; G-16 "licence and credit" via `## Sources`.
        val folder = kitTangramsPuzzleIds()
        assertTrue("the Tangrams folder lists puzzle files", folder.isNotEmpty())

        val read = kitReadRecord(kitRecordFileText(), folder)

        assertTrue(
            "$KIT_RECORD_FILE breaks S-41 (id, check, cell):\n" + read.problems.joinToString("\n"),
            read.problems.isEmpty(),
        )
        // Liveness: S-41 "The 13 shipped puzzles are included", and seam note 1 "Puzzles are added, never
        // changed", so the record has at least 13 rows; and it has one row per puzzle file.
        assertTrue("liveness: at least 13 rows; read ${read.rows.size}", read.rows.size >= 13)
        assertEquals("one row per puzzle file", folder.size, read.rows.size)
    }

    // ── Pin (b): REQ-039 rule 1's colour count over the imported pictures, S-42 ────────────────────

    @Test // REQ-039 rule 1 (no token)
    fun everyImportedPictureUsesThreeToEightColours() {
        // REQ-039 rule 1: "The picture uses flat shapes, 3–8 colours and no text." S-42: the distinct
        // `#RRGGBB` values, case-insensitive, among `art.base` and every shape's `fill` and `stroke`.
        // The imports are the rows whose Figure is a collection (S-41; tasks.md TASK-041 row: "the
        // imported files ... The 13 shipped files are not in it").
        val read = kitReadRecord(kitRecordFileText(), kitTangramsPuzzleIds())
        val shape = read.problems.filter { it.check == KitCheck.SHAPE }
        assertTrue("the record's rows cannot be read (S-41):\n" + shape.joinToString("\n"), shape.isEmpty())
        val imported = read.rows.filter { it.figure in KIT_COLLECTIONS }.map { it.id }.distinct()
        assertTrue("liveness: the colour pin judges at least one imported picture", imported.isNotEmpty())

        val library = PuzzleLibraries.bundled(LockEngine())
        val problems = imported.mapNotNull { id -> kitColourProblem(id, library.byId(id)?.art) }

        assertTrue(
            "${problems.size} of ${imported.size} imported pictures break REQ-039 rule 1's colour count:\n" +
                problems.joinToString("\n"),
            problems.isEmpty(),
        )
    }

    // ── Fixture checks for pin (a): each crafted record fails its own check, and only that one ─────

    @Test
    fun theCraftedRecordInS41sShapePassesEveryCheck() {
        // The control: without it, a check that fires on everything would pass every case below.
        val read = kitReadRecord(kitCraftedRecord(), KIT_FOLDER)
        assertEquals("no problem in the crafted base record", emptyList<KitProblem>(), read.problems)
        assertEquals(KIT_FOLDER, read.rows.map { it.id })
        assertEquals(listOf(KIT_THIS_PROJECT, KIT_CONVEX, KIT_ANDROID), read.rows.map { it.figure })
    }

    @Test
    fun aRecordMissingARowFailsNamingTheFile() {
        // S-41 "one row per `Tangrams/*.json`": Tangrams/things-kit-android.json has no row.
        val text = kitCraftedRecord(figureRows = KIT_FIGURE_ROWS - KIT_ANDROID_ROW)
        kitAssertOnly(kitProblemsOf(text), KitCheck.MISSING_ROW, "things-kit-android")
    }

    @Test
    fun aRecordWithARowForNoFileFailsNamingTheRow() {
        // R5 "every row names a file": a fourth row, sorted last, whose file is not in the folder.
        val extra = kitFigureLine("`vehicles-kit-ghost`", KIT_ANDROID, "`objects-ghost`", "Ghost", "GPL-3.0")
        val text = kitCraftedRecord(figureRows = KIT_FIGURE_ROWS + extra)
        kitAssertOnly(kitProblemsOf(text), KitCheck.ROW_WITHOUT_FILE, "vehicles-kit-ghost")
    }

    @Test
    fun aRecordListingAPuzzleTwiceFails() {
        // R5 "exactly one row": the convex row twice, adjacent, so the order still holds.
        val text = kitCraftedRecord(figureRows = listOf(KIT_OWN_ROW, KIT_CONVEX_ROW, KIT_CONVEX_ROW, KIT_ANDROID_ROW))
        kitAssertOnly(kitProblemsOf(text), KitCheck.DUPLICATE_ROW, "shapes-kit-convex")
    }

    @Test
    fun aWrongFigureAndLicencePairFailsForEachFigure() {
        // S-41: GPL-3.0 for this project and android-tangram-131, CC0-1.0 for convex-13. One wrong pair each.
        val cases = listOf(
            Triple(KIT_OWN_ROW, kitFigureLine("`animals-kit-own`", KIT_THIS_PROJECT, KIT_NONE, KIT_NONE, "CC0-1.0"), "animals-kit-own"),
            Triple(KIT_CONVEX_ROW, kitFigureLine("`shapes-kit-convex`", KIT_CONVEX, "`convex-01`", "Convex 1", "GPL-3.0"), "shapes-kit-convex"),
            Triple(KIT_ANDROID_ROW, kitFigureLine("`things-kit-android`", KIT_ANDROID, "`objects-bowl`", "Bowl", "CC0-1.0"), "things-kit-android"),
        )
        for ((old, new, id) in cases) {
            val text = kitCraftedRecord(figureRows = kitReplaced(KIT_FIGURE_ROWS, old, new))
            kitAssertOnly(kitProblemsOf(text), KitCheck.LICENCE, id)
        }
    }

    @Test
    fun anUnknownFigureFails() {
        // S-41: "Figure is `this project`, `convex-13` or `android-tangram-131`."
        val new = kitFigureLine("`things-kit-android`", "some-other-collection", "`objects-bowl`", "Bowl", "GPL-3.0")
        val text = kitCraftedRecord(figureRows = kitReplaced(KIT_FIGURE_ROWS, KIT_ANDROID_ROW, new))
        kitAssertOnly(kitProblemsOf(text), KitCheck.FIGURE, "things-kit-android")
    }

    @Test
    fun sourceCellsThatDoNotFitTheFigureFail() {
        // S-41: Source figure id (the collection's id, in backticks) and Source name (its name) on a
        // collection row; `—` in both for this project.
        val cases = listOf(
            // this project with a source figure id
            Triple(KIT_OWN_ROW, kitFigureLine("`animals-kit-own`", KIT_THIS_PROJECT, "`objects-cat`", KIT_NONE, "GPL-3.0"), "animals-kit-own"),
            // this project with a source name
            Triple(KIT_OWN_ROW, kitFigureLine("`animals-kit-own`", KIT_THIS_PROJECT, KIT_NONE, "Cat", "GPL-3.0"), "animals-kit-own"),
            // a collection row with `—` as its source figure id
            Triple(KIT_CONVEX_ROW, kitFigureLine("`shapes-kit-convex`", KIT_CONVEX, KIT_NONE, "Convex 1", "CC0-1.0"), "shapes-kit-convex"),
            // a collection row whose source figure id is not in backticks
            Triple(KIT_ANDROID_ROW, kitFigureLine("`things-kit-android`", KIT_ANDROID, "objects-bowl", "Bowl", "GPL-3.0"), "things-kit-android"),
            // a collection row without a source name
            Triple(KIT_ANDROID_ROW, kitFigureLine("`things-kit-android`", KIT_ANDROID, "`objects-bowl`", "", "GPL-3.0"), "things-kit-android"),
        )
        for ((old, new, id) in cases) {
            val text = kitCraftedRecord(figureRows = kitReplaced(KIT_FIGURE_ROWS, old, new))
            kitAssertOnly(kitProblemsOf(text), KitCheck.SOURCE_CELLS, id)
        }
    }

    @Test
    fun aPictureCellOtherThanMadeForThisProjectFails() {
        // S-41: "Picture is `made for this project` on every row." (G-16: the picture is still made for this project.)
        val new = kitFigureLine("`things-kit-android`", KIT_ANDROID, "`objects-bowl`", "Bowl", "GPL-3.0", picture = "from the collection")
        val text = kitCraftedRecord(figureRows = kitReplaced(KIT_FIGURE_ROWS, KIT_ANDROID_ROW, new))
        kitAssertOnly(kitProblemsOf(text), KitCheck.PICTURE, "things-kit-android")
    }

    @Test
    fun rowsOutOfIdOrderFail() {
        // S-41: "Rows are sorted by puzzle id." things-kit-android listed before shapes-kit-convex.
        val text = kitCraftedRecord(figureRows = listOf(KIT_OWN_ROW, KIT_ANDROID_ROW, KIT_CONVEX_ROW))
        kitAssertOnly(kitProblemsOf(text), KitCheck.ORDER, "shapes-kit-convex")
    }

    @Test
    fun aPuzzleIdOutsideBackticksFails() {
        // S-41: "Puzzle id is in backticks." The id is still read, so no row goes missing.
        val new = kitFigureLine("shapes-kit-convex", KIT_CONVEX, "`convex-01`", "Convex 1", "CC0-1.0")
        val text = kitCraftedRecord(figureRows = kitReplaced(KIT_FIGURE_ROWS, KIT_CONVEX_ROW, new))
        kitAssertOnly(kitProblemsOf(text), KitCheck.ID_CELL, "shapes-kit-convex")
    }

    @Test
    fun aSourcesTableWithoutAFigureSourceOrACollectionsLicenceOrCreditFails() {
        // S-41 `## Sources`: "one row per figure source" (this project, convex-13, android-tangram-131); a
        // collection's licence as S-41 pairs it; its Where from; its credit (G-16 "licence and credit"; R5).
        val cases = listOf(
            (KIT_SOURCE_ROWS - KIT_CONVEX_SOURCE) to KIT_CONVEX,
            (KIT_SOURCE_ROWS - KIT_OWN_SOURCE) to KIT_THIS_PROJECT,
            (KIT_SOURCE_ROWS + KIT_ANDROID_SOURCE) to KIT_ANDROID,
            kitReplaced(KIT_SOURCE_ROWS, KIT_ANDROID_SOURCE, kitSourceLine(KIT_ANDROID, "github.com/Typraeurion/Android-Tangram", "CC0-1.0", "Trevin Beattie; GPL-3.0")) to KIT_ANDROID,
            kitReplaced(KIT_SOURCE_ROWS, KIT_CONVEX_SOURCE, kitSourceLine(KIT_CONVEX, "Wikimedia Commons, File:Convex_tangram_shapes.svg", "GPL-3.0", "László Németh; CC0 1.0")) to KIT_CONVEX,
            kitReplaced(KIT_SOURCE_ROWS, KIT_CONVEX_SOURCE, kitSourceLine(KIT_CONVEX, "Wikimedia Commons, File:Convex_tangram_shapes.svg", "CC0-1.0", KIT_NONE)) to KIT_CONVEX,
            kitReplaced(KIT_SOURCE_ROWS, KIT_ANDROID_SOURCE, kitSourceLine(KIT_ANDROID, "", "GPL-3.0", "Trevin Beattie; GPL-3.0")) to KIT_ANDROID,
        )
        for ((sourceRows, source) in cases) {
            kitAssertOnly(kitProblemsOf(kitCraftedRecord(sourceRows = sourceRows)), KitCheck.SOURCES, source)
        }
    }

    @Test
    fun aRecordWithoutAnS41SectionOrWithOtherColumnsFails() {
        // S-41's three sections and the two tables' columns.
        kitAssertOnly(kitProblemsOf(kitCraftedRecord(withAssets = false)), KitCheck.SHAPE, KIT_ASSETS_HEADING)
        kitAssertOnly(kitProblemsOf(kitCraftedRecord(withFigures = false)), KitCheck.SHAPE, KIT_FIGURES_HEADING)
        // The Picture column left out (header and every row): the rows cannot be read as S-41's.
        val fiveColumns = KIT_FIGURES_COLUMNS.dropLast(1)
        val fiveCellRows = KIT_FIGURE_ROWS.map { it.substringBeforeLast("| $KIT_PICTURE |") + "|" }
        kitAssertOnly(
            kitProblemsOf(kitCraftedRecord(figureRows = fiveCellRows, figureColumns = fiveColumns)),
            KitCheck.SHAPE,
            KIT_FIGURES_HEADING,
        )
    }

    @Test
    fun theProjectRecordWithOneRowRemovedOrOneRowAddedFailsNamingThatId() {
        // The parser over the real file's layout: removing one of its collection rows, or adding a row for a
        // file that does not exist, adds exactly that one problem to whatever the real record has.
        val folder = kitTangramsPuzzleIds()
        val text = kitRecordFileText()
        val lines = text.lines()
        val before = kitReadRecord(text, folder).problems.toSet()

        // The body rows of the `## Puzzle figures` table: its `|` lines after the header and delimiter row.
        val heading = lines.indexOfFirst { it.trim() == KIT_FIGURES_HEADING }
        assertTrue("liveness: the project record has $KIT_FIGURES_HEADING", heading >= 0)
        val tableStart = (heading + 1 until lines.size).first { lines[it].trimStart().startsWith("|") }
        val rowLines = (tableStart + 2 until lines.size)
            .takeWhile { lines[it].trimStart().startsWith("|") }
            .filter { kitCells(lines[it]).size == KIT_FIGURES_COLUMNS.size }
        val removed = rowLines.firstOrNull { kitUnquote(kitCells(lines[it])[1]) in KIT_COLLECTIONS }
        assertNotNull("liveness: the project record has a collection row", removed)
        val removedId = kitUnquote(kitCells(lines[removed!!])[0])
        val withoutRow = lines.filterIndexed { i, _ -> i != removed }.joinToString("\n")
        val added1 = kitReadRecord(withoutRow, folder).problems - before
        kitAssertOnly(added1, KitCheck.MISSING_ROW, removedId)

        val last = rowLines.last()
        val extraId = kitUnquote(kitCells(lines[last])[0]) + "-kit-extra" // sorts right after the last row
        val extraLine = kitFigureLine("`$extraId`", KIT_ANDROID, "`objects-kit-extra`", "Kit extra", "GPL-3.0")
        val withExtra = (lines.take(last + 1) + extraLine + lines.drop(last + 1)).joinToString("\n")
        val added2 = kitReadRecord(withExtra, folder).problems - before
        kitAssertOnly(added2, KitCheck.ROW_WITHOUT_FILE, extraId)
    }

    // ── Fixture check for pin (b): the colour count on crafted pictures ────────────────────────────

    @Test
    fun theColourCountFailsTheStubTwoAndNineColoursAndPassesThreeAndEight() {
        // Crafted one-piece minis (valid: KitTexts.kt `kitOnePieceMini`'s piece) loaded beside the shipped
        // texts through the kit's door; their pictures are read back from the library, as the pin reads them.
        // The colour count is not a load rule (decisions.md 2026-10-11 "What 'a well-formed picture' means"),
        // so every one of them loads.
        val stub = kitHandMade("shapes-kit-colours-stub", "mini", KIT_ONE_PIECE, art = "{'base': '$KIT_STUB_BASE', 'shapes': []}")
        val two = kitHandMade("shapes-kit-colours-two", "mini", KIT_ONE_PIECE, art = "{'base': '#112233', 'shapes': [${kitCircles("#445566")}]}")
        // Three: the base, one fill, and a colour used only as a stroke. A count that leaves out the base or
        // the strokes gives 2 and fails it.
        val three = kitHandMade(
            "shapes-kit-colours-three", "mini", KIT_ONE_PIECE,
            art = "{'base': '#112233', 'shapes': [{'type': 'circle', 'c': [1, 0.4], 'r': 0.1, 'fill': '#445566', 'stroke': '#778899', 'width': 0.05}]}",
        )
        // Eight: the base and seven fills, the last written twice in different case. A case-sensitive count
        // gives 9 and fails it (S-42: "case-insensitive").
        val eight = kitHandMade(
            "shapes-kit-colours-eight", "mini", KIT_ONE_PIECE,
            art = "{'base': '#101010', 'shapes': [${kitCircles("#202020", "#303030", "#404040", "#505050", "#606060", "#707070", "#A0B0C0", "#a0b0c0")}]}",
        )
        val nine = kitHandMade(
            "shapes-kit-colours-nine", "mini", KIT_ONE_PIECE,
            art = "{'base': '#101010', 'shapes': [${kitCircles("#202020", "#303030", "#404040", "#505050", "#606060", "#707070", "#808080", "#A0B0C0")}]}",
        )
        val result = kitLoadWithShipped(stub, two, three, eight, nine)

        val expected = listOf(stub to 1, two to 2, three to 3, eight to 8, nine to 9)
        for ((source, count) in expected) {
            val id = kitStem(source)
            val art = kitAssertListed(result, source).art
            assertEquals("$id: distinct colours (S-42)", count, kitColourCount(art))
            val problem = kitColourProblem(id, art)
            if (count in KIT_COLOURS) {
                assertNull("$id: $count colours keep REQ-039 rule 1", problem)
            } else {
                assertNotNull("$id: $count colours break REQ-039 rule 1", problem)
                assertTrue("$id: the problem names the id: $problem", problem!!.startsWith("$id:"))
            }
        }
        assertTrue(
            "the stub is reported as a picture not drawn",
            kitColourProblem(kitStem(stub), kitAssertListed(result, stub).art)!!.contains("stub"),
        )
        // An id with no listed puzzle is reported, not skipped.
        assertNotNull(kitColourProblem("things-kit-not-listed", null))
    }
}
