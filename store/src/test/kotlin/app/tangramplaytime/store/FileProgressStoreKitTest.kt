// Acceptance kit (WO-004, TASK-016) — governance row 11's `:store` JVM test: FileProgressStore
// (designs/WO-004-design.md §2 S-17) over files this test writes and reads in its own temporary folder.
//
// - No token (tasks.md § WO-004 intro, "Lines covered twice": the row-11 cases are pins). Sources, cited per
//   test: the IProgressStore KDoc (contracts, baseline 2026-10-11); design §1 ADR-003 with "Unreadable
//   state" and "A failed write"; §2 S-17 ("Never throws: an Exception on the read side means a fresh install;
//   on the write side, a failed write"); §3 "Highest-risk part", first bullet; decisions.md 2026-10-15
//   "ADR-003, the persistence format of IProgressStore" and "Amends the 2026-10-15 ADR-003 row".
// - The on-disk JSON is written by hand as text in the ADR-003 shape: the test classpath has no
//   serialization (TASK-016 brief). The test reads no file it did not create.
// - S-18's one-instance pin runs on the device (tasks.md § WO-004 intro, seam note 2), not here.
// - Helper names carry a `kit` / `KIT_` prefix so they cannot collide with an `internal` declaration of
//   :store's main source set in the same package.
package app.tangramplaytime.store

import app.tangramplaytime.contracts.IProgressStore
import app.tangramplaytime.contracts.PieceId
import app.tangramplaytime.contracts.Placement
import app.tangramplaytime.contracts.PlayTimes
import app.tangramplaytime.contracts.Point
import app.tangramplaytime.contracts.PuzzleProgress
import app.tangramplaytime.contracts.PuzzleState
import app.tangramplaytime.contracts.Settings
import app.tangramplaytime.contracts.Turn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import kotlin.math.sqrt

class FileProgressStoreKitTest {

    @get:Rule
    val kitTmp = TemporaryFolder()

    // ── Values ───────────────────────────────────────────────────────────────────────────────────

    /** An In-progress board with a mirrored parallelogram, turns 7 and 0, and centres that are not short decimals. */
    private val kitA = PuzzleProgress(
        PuzzleState.IN_PROGRESS,
        listOf(
            Placement(PieceId.PG, Turn(7), true, Point(1.0 / 3.0, sqrt(2.0))),
            Placement(PieceId.LT2, Turn(0), false, Point(2.0 + sqrt(2.0) / 2.0, 0.1 + 0.2)),
            Placement(PieceId.ST1, Turn(3), false, Point(4.242640687119286, 0.7071067811865476)),
        ),
        125,
        null,
    )
    private val kitB = PuzzleProgress(PuzzleState.SOLVED, emptyList(), 61, 59)
    private val kitC = PuzzleProgress(PuzzleState.NEW, emptyList(), 0, 44)
    private val kitTimes = PlayTimes(301, LocalDate.of(2026, 10, 15), 7201)
    private val kitSettings = Settings(timerShown = true, soundOn = false)

    /**
     * A format-1 document in the ADR-003 shape (design §1), written by hand: every member, a nullable best
     * time both ways, the parallelogram mirrored.
     */
    private val kitDoc = """
        {
          "format": 1,
          "lastOpened": "kit-a",
          "puzzles": {
            "kit-a": {"state": "IN_PROGRESS", "placed": [{"piece": "PG", "turn": 3, "mirrored": true, "x": 1.5, "y": 2.0}, {"piece": "LT1", "turn": 7, "mirrored": false, "x": 0.3333333333333333, "y": 3.25}], "time": 12, "best": null},
            "kit-b": {"state": "SOLVED", "placed": [], "time": 40, "best": 38},
            "kit-c": {"state": "NEW", "placed": [], "time": 0, "best": 17}
          },
          "playTimes": {"today": 5, "date": "2026-10-15", "allTime": 99},
          "settings": {"timerShown": true, "soundOn": false}
        }
    """.trimIndent()

    /** What [kitDoc] holds, as contract values. */
    private val kitDocA = PuzzleProgress(
        PuzzleState.IN_PROGRESS,
        listOf(
            Placement(PieceId.PG, Turn(3), true, Point(1.5, 2.0)),
            Placement(PieceId.LT1, Turn(7), false, Point(1.0 / 3.0, 3.25)),
        ),
        12,
        null,
    )
    private val kitDocB = PuzzleProgress(PuzzleState.SOLVED, emptyList(), 40, 38)
    private val kitDocC = PuzzleProgress(PuzzleState.NEW, emptyList(), 0, 17)

    // ── Helpers ──────────────────────────────────────────────────────────────────────────────────

    /** S-17: `FileProgressStore(file: File, io: CoroutineDispatcher = Dispatchers.IO)`. */
    private fun kitStore(file: File): IProgressStore = FileProgressStore(file)

    private fun <T> kitRun(block: suspend () -> T): T = runBlocking { withTimeout(20_000L) { block() } }

    private fun kitFile(name: String = "progress.json"): File = File(kitTmp.newFolder(), name)

    /**
     * Asserts [store] reads as a fresh install for every member — the IProgressStore KDoc: "every puzzle
     * PuzzleProgress.FRESH, PlayTimes.NONE, default Settings, no last-opened puzzle" — for the [ids] a test
     * wrote, and that no member throws ("It never fails the launch and never throws for it").
     */
    private fun kitAssertFresh(store: IProgressStore, ids: List<String>, what: String) = kitRun {
        for (id in ids) assertEquals("$what: $id", PuzzleProgress.FRESH, store.progress(id))
        assertTrue("$what: every saved entry FRESH", store.allProgress().values.all { it == PuzzleProgress.FRESH })
        assertEquals("$what: play times", PlayTimes.NONE, store.playTimes())
        assertEquals("$what: settings", Settings(), store.settings())
        assertNull("$what: last opened", store.lastOpenedPuzzleId())
    }

    /** True when [text] declares format 1 (ADR-003: `{"format":1, …}`). */
    private fun kitIsFormatOne(text: String): Boolean = Regex("\"format\"\\s*:\\s*1(?![0-9.eE])").containsMatchIn(text)

    // ── The format ────────────────────────────────────────────────────────────────────────────────

    // design §1 ADR-003 — the document `{"format":1,"lastOpened":"<id>"?,"puzzles":{"<id>":{"state":
    // "NEW|IN_PROGRESS|SOLVED","placed":[{"piece","turn","mirrored","x","y"}],"time","best"}},"playTimes":
    // {"today","date","allTime"},"settings":{"timerShown","soundOn"}}`, "One UTF-8 JSON file". A hand-written
    // document reads back as the values it holds, through every IProgressStore member; `lastOpened` may be
    // absent and `date` null (the shape's `?` and `|null`). Pin, no token.
    @Test
    fun aHandWrittenFormatOneDocumentReadsBackThroughEveryMember() {
        val file = kitFile()
        file.writeText(kitDoc, Charsets.UTF_8)
        val store = kitStore(file)
        kitRun {
            assertEquals("kit-a", kitDocA, store.progress("kit-a"))
            assertEquals("kit-b", kitDocB, store.progress("kit-b"))
            assertEquals("kit-c", kitDocC, store.progress("kit-c"))
            assertEquals("a puzzle without an entry is FRESH", PuzzleProgress.FRESH, store.progress("kit-none"))
            assertEquals("allProgress, keyed by id", mapOf("kit-a" to kitDocA, "kit-b" to kitDocB, "kit-c" to kitDocC), store.allProgress())
            assertEquals(PlayTimes(5, LocalDate.of(2026, 10, 15), 99), store.playTimes())
            assertEquals(Settings(timerShown = true, soundOn = false), store.settings())
            assertEquals("kit-a", store.lastOpenedPuzzleId())
        }

        val bare = kitFile()
        val noLastOpened = kitDoc.replace("\"lastOpened\": \"kit-a\",", "").replace("\"date\": \"2026-10-15\"", "\"date\": null")
        assertTrue("fixture: lastOpened removed", "lastOpened" !in noLastOpened)
        assertTrue("fixture: date null", "\"date\": null" in noLastOpened)
        bare.writeText(noLastOpened, Charsets.UTF_8)
        val other = kitStore(bare)
        kitRun {
            assertNull("an absent lastOpened reads null", other.lastOpenedPuzzleId())
            assertEquals("a null date reads null", PlayTimes(5, null, 99), other.playTimes())
            assertEquals("the rest reads as before", kitDocA, other.progress("kit-a"))
        }
    }

    // IProgressStore KDoc — "A save is complete when the call returns: a later read — in this process or
    // after a restart — returns what was saved"; `allProgress`: "Every saved puzzle's progress, keyed by
    // puzzle id"; each save member "replacing the saved ones". Design §3 "Highest-risk part": "A new store
    // instance over the same file reads every member back." ADR-003: the file is the one S-17 is given, a
    // format-1 UTF-8 JSON document. Doubles round-trip exactly (design §3, library fact 2). Pin, no token.
    @Test
    fun aSaveIsCompleteWhenTheCallReturnsAndANewInstanceReadsEveryMemberBack() {
        val file = kitFile()
        val first = kitStore(file)
        kitRun {
            first.saveProgress("kit-a", kitA)
            first.saveProgress("kit-b", kitB)
            first.saveProgress("kit-c", kitC)
            first.savePlayTimes(kitTimes)
            first.saveSettings(kitSettings)
            first.saveLastOpenedPuzzleId("kit-b")
        }
        assertTrue("the save wrote the file S-17 was given", file.isFile)
        assertTrue("a format-1 document", kitIsFormatOne(file.readText(Charsets.UTF_8)))
        for ((what, store) in listOf("the same instance" to first, "a new instance over the same file" to kitStore(file))) {
            kitRun {
                assertEquals("$what: kit-a", kitA, store.progress("kit-a"))
                assertEquals("$what: kit-b", kitB, store.progress("kit-b"))
                assertEquals("$what: kit-c", kitC, store.progress("kit-c"))
                assertEquals("$what: allProgress", mapOf("kit-a" to kitA, "kit-b" to kitB, "kit-c" to kitC), store.allProgress())
                assertEquals("$what: play times", kitTimes, store.playTimes())
                assertEquals("$what: settings", kitSettings, store.settings())
                assertEquals("$what: last opened", "kit-b", store.lastOpenedPuzzleId())
            }
        }

        val second = kitStore(file)
        val noDate = PlayTimes(0, null, 5)
        kitRun {
            second.savePlayTimes(noDate)
            second.saveSettings(Settings())
            second.saveLastOpenedPuzzleId("kit-c")
        }
        val third = kitStore(file)
        kitRun {
            assertEquals("play times replaced, a null date kept", noDate, third.playTimes())
            assertEquals("settings replaced", Settings(), third.settings())
            assertEquals("last opened replaced", "kit-c", third.lastOpenedPuzzleId())
            assertEquals("progress untouched by the other members", kitA, third.progress("kit-a"))
        }
    }

    // IProgressStore KDoc — `saveProgress`: "replacing what was saved for it and nothing else"; "Must never:
    // … lose one puzzle's progress when another is saved (REQ-003)"; design §3 "Highest-risk part": "Saving B
    // keeps A." The quantifier's adversarial path: two dozen puzzles saved at once from concurrent coroutines
    // on one instance ("Reading. Once, into memory, behind a Mutex", ADR-003), then read by a new instance.
    // Pin, no token.
    @Test
    fun savingOnePuzzleKeepsEveryOtherPuzzle() {
        val file = kitFile()
        val store = kitStore(file)
        val a2 = PuzzleProgress(PuzzleState.IN_PROGRESS, kitA.placed.take(1), 130, 3)
        kitRun {
            store.saveProgress("kit-a", kitA)
            store.saveLastOpenedPuzzleId("kit-a")
            store.saveSettings(kitSettings)
            store.savePlayTimes(kitTimes)
            store.saveProgress("kit-b", kitB)
        }
        kitRun {
            val fresh = kitStore(file)
            assertEquals("saving B keeps A", kitA, fresh.progress("kit-a"))
            assertEquals(kitB, fresh.progress("kit-b"))
        }
        kitRun { store.saveProgress("kit-a", a2) }
        kitRun {
            val fresh = kitStore(file)
            assertEquals("A replaced", a2, fresh.progress("kit-a"))
            assertEquals("replacing A keeps B", kitB, fresh.progress("kit-b"))
            assertEquals("and the last opened id", "kit-a", fresh.lastOpenedPuzzleId())
            assertEquals("and the settings", kitSettings, fresh.settings())
            assertEquals("and the play times", kitTimes, fresh.playTimes())
        }

        val many = (1..24).associate { n -> "kit-many-$n" to PuzzleProgress(PuzzleState.SOLVED, emptyList(), n, n) }
        kitRun {
            coroutineScope {
                many.map { (id, p) -> async(Dispatchers.Default) { store.saveProgress(id, p) } }.awaitAll()
            }
        }
        kitRun {
            val fresh = kitStore(file)
            for ((id, p) in many) assertEquals("$id after concurrent saves", p, fresh.progress(id))
            assertEquals("the earlier ones too", a2, fresh.progress("kit-a"))
            assertEquals(kitB, fresh.progress("kit-b"))
        }
    }

    // IProgressStore `resetAll` KDoc — "afterwards every puzzle reads PuzzleProgress.FRESH … and the play
    // times read PlayTimes.NONE. Settings are kept … and the last opened puzzle id is unchanged"; read on the
    // same instance and on a new one (a save is complete when the call returns). Pin, no token.
    @Test
    fun resetAllErasesProgressAndPlayTimesAndKeepsSettingsAndTheLastOpenedId() {
        val file = kitFile()
        val store = kitStore(file)
        kitRun {
            store.saveProgress("kit-a", kitA)
            store.saveProgress("kit-b", kitB)
            store.savePlayTimes(kitTimes)
            store.saveSettings(kitSettings)
            store.saveLastOpenedPuzzleId("kit-b")
            store.resetAll()
        }
        for ((what, s) in listOf("the same instance" to store, "a new instance" to kitStore(file))) {
            kitRun {
                assertEquals("$what: kit-a", PuzzleProgress.FRESH, s.progress("kit-a"))
                assertEquals("$what: kit-b", PuzzleProgress.FRESH, s.progress("kit-b"))
                assertTrue("$what: every entry FRESH", s.allProgress().values.all { it == PuzzleProgress.FRESH })
                assertEquals("$what: play times", PlayTimes.NONE, s.playTimes())
                assertEquals("$what: settings kept", kitSettings, s.settings())
                assertEquals("$what: last opened kept", "kit-b", s.lastOpenedPuzzleId())
            }
        }
    }

    // ── Unreadable state ──────────────────────────────────────────────────────────────────────────

    // IProgressStore KDoc — "Missing or unreadable stored state behaves as a fresh install … It never fails
    // the launch and never throws for it"; ADR-003 "Unreadable state": "the file is missing". Pin, no token.
    @Test
    fun aMissingFileReadsAsAFreshInstall() {
        val file = kitFile()
        assertTrue("fixture: no file", !file.exists())
        kitAssertFresh(kitStore(file), listOf("kit-a"), "a missing file")
    }

    // ADR-003 "Unreadable state" — "`format` ≠ 1 (absent or 0 is older, above 1 is unknown)" reads as a fresh
    // install, "and the next save overwrites the file"; governance row 11 (workorders/WO-004.md § Constraints):
    // "an unknown or older format version … read as a fresh install". Each variant is the valid [kitDoc] with
    // only `format` changed, so the whole store, valid entries included, reads fresh. Pin, no token.
    @Test
    fun anOlderOrUnknownFormatReadsAsAFreshInstallAndTheNextSaveOverwritesIt() {
        val variants = listOf(
            "format 0 (older)" to kitDoc.replace("\"format\": 1,", "\"format\": 0,"),
            "no format (older)" to kitDoc.replace("\"format\": 1,", ""),
            "format 2 (unknown)" to kitDoc.replace("\"format\": 1,", "\"format\": 2,"),
        )
        for ((what, text) in variants) {
            assertNotEquals("fixture: $what changes the document", kitDoc, text)
            kitAssertUnreadable(what, text.toByteArray(Charsets.UTF_8))
        }
    }

    // ADR-003 "Unreadable state" — "any Exception occurs while reading, decoding, validating or mapping.
    // Validation covers an unknown key or enum, turn outside 0..7, a non-finite number, a negative time and
    // an invalid date"; decisions.md 2026-10-15 "Amends the 2026-10-15 ADR-003 row": "adds valid-JSON corrupt
    // cases (bad date, unknown enum, out-of-range int)"; design §3: "corrupt content: garbage bytes, a bad
    // date, an unknown enum, an out-of-range int". Each valid-JSON case is [kitDoc] with one field changed,
    // so the whole store — the valid entries too — reads fresh, and the next save overwrites the file.
    // Pin, no token.
    @Test
    fun corruptContentReadsAsAFreshInstallAndTheNextSaveOverwritesIt() {
        fun kitOne(what: String, from: String, to: String): Pair<String, ByteArray> {
            val text = kitDoc.replaceFirst(from, to)
            assertNotEquals("fixture: $what changes the document", kitDoc, text)
            return what to text.toByteArray(Charsets.UTF_8)
        }
        val variants = listOf(
            "garbage bytes" to byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00, 0x7B, 0x22, 0x9C.toByte(), 0x01, 0x7D),
            "an empty file" to ByteArray(0),
            "truncated JSON" to kitDoc.substring(0, kitDoc.length / 2).toByteArray(Charsets.UTF_8),
            "not an object" to "[]".toByteArray(Charsets.UTF_8),
            kitOne("an invalid date", "\"2026-10-15\"", "\"2026-02-30\""),
            kitOne("a date that is not a date", "\"2026-10-15\"", "\"yesterday\""),
            kitOne("an unknown state", "\"SOLVED\"", "\"DONE\""),
            kitOne("an unknown piece", "\"LT1\"", "\"LT3\""),
            kitOne("turn 8", "\"turn\": 7", "\"turn\": 8"),
            kitOne("turn -1", "\"turn\": 3", "\"turn\": -1"),
            kitOne("an unknown top-level key", "\"format\": 1,", "\"format\": 1, \"profile\": \"kit\","),
            kitOne("an unknown key in a placement", "\"x\": 1.5,", "\"x\": 1.5, \"z\": 0.0,"),
            kitOne("an infinite number", "\"x\": 1.5", "\"x\": 1e999"),
            kitOne("NaN", "\"y\": 2.0", "\"y\": NaN"),
            kitOne("a negative puzzle time", "\"time\": 12", "\"time\": -1"),
            kitOne("a negative best time", "\"best\": 38", "\"best\": -38"),
            kitOne("a negative today time", "\"today\": 5", "\"today\": -5"),
            kitOne("a negative all-time time", "\"allTime\": 99", "\"allTime\": -99"),
            kitOne("a boolean where a time is", "\"time\": 40", "\"time\": true"),
        )
        for ((what, bytes) in variants) kitAssertUnreadable(what, bytes)
    }

    /**
     * Writes [bytes] as the file, then: a store over it reads as a fresh install for every member without
     * throwing; a save then overwrites the file, so a new instance reads exactly that save and nothing of
     * the unreadable document ("the next save overwrites the file", ADR-003).
     */
    private fun kitAssertUnreadable(what: String, bytes: ByteArray) {
        val file = kitFile()
        file.writeBytes(bytes)
        val store = kitStore(file)
        kitAssertFresh(store, listOf("kit-a", "kit-b", "kit-c"), what)
        kitRun { store.saveProgress("kit-z", kitB) }
        assertTrue("$what: the save wrote a format-1 document", kitIsFormatOne(file.readText(Charsets.UTF_8)))
        val after = kitStore(file)
        kitRun {
            assertEquals("$what: the save is read back", kitB, after.progress("kit-z"))
            assertEquals("$what: the save is the only entry", mapOf("kit-z" to kitB), after.allProgress())
            for (id in listOf("kit-a", "kit-b", "kit-c")) assertEquals("$what, after the overwrite: $id", PuzzleProgress.FRESH, after.progress(id))
            assertEquals("$what, after the overwrite: play times", PlayTimes.NONE, after.playTimes())
            assertEquals("$what, after the overwrite: settings", Settings(), after.settings())
            assertNull("$what, after the overwrite: last opened", after.lastOpenedPuzzleId())
        }
    }

    // S-17 — "Never throws: an Exception on the read side means a fresh install; on the write side, a failed
    // write". The read side's boundary: `file` names a folder (with a file in it), which this JVM cannot read
    // as a file (shown test-side). Every member reads fresh and a save does not throw. Pin, no token.
    @Test
    fun aFileThatCannotBeReadReadsAsAFreshInstallAndASaveDoesNotThrow() {
        val file = kitFile()
        assertTrue("fixture: a folder where the file should be", file.mkdir())
        File(file, "kit-inside").writeText("kit")
        assertThrows("fixture: this JVM cannot read it as a file", IOException::class.java) { file.readBytes() }
        val store = kitStore(file)
        kitAssertFresh(store, listOf("kit-a"), "a folder at the file's path")
        kitRun { store.saveProgress("kit-a", kitA) }
    }

    // ── A failed write ────────────────────────────────────────────────────────────────────────────

    // ADR-003 "A failed write is any Exception while encoding or writing. It keeps the old file and the
    // in-memory document; the next save retries, and the call never throws (G-15)"; decisions.md 2026-10-15
    // ADR-003 row: "Failed write: keeps the previous file, is retried by the next save, never thrown";
    // IProgressStore KDoc: a later read "in this process … returns what was saved". The failure is provoked
    // through S-17's `file`: the old file is made read-only. Test-side, on this JVM (Windows), an atomic
    // replace of a read-only file and an overwrite of it both fail (shown on a sibling probe). Pin, no token.
    @Test
    fun aFailedWriteKeepsTheOldFileNeverThrowsAndTheNextSaveRetries() {
        val file = kitFile()
        kitRun {
            val earlier = kitStore(file)
            earlier.saveProgress("kit-a", kitA)
            earlier.saveSettings(kitSettings)
            earlier.saveLastOpenedPuzzleId("kit-a")
        }
        val store = kitStore(file)
        kitRun { assertEquals("fixture: the old file reads", kitA, store.progress("kit-a")) }

        val probeTarget = File(file.parentFile, "kit-probe-target.json").apply { writeText("kit") }
        val probeTmp = File(file.parentFile, "kit-probe.tmp").apply { writeText("kit-new") }
        val oldBytes = file.readBytes()
        try {
            assertTrue("fixture: the probe target is read-only", probeTarget.setWritable(false))
            assertThrows("fixture: an atomic replace of a read-only file fails on this JVM", IOException::class.java) {
                Files.move(probeTmp.toPath(), probeTarget.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            }
            assertThrows("fixture: an overwrite of a read-only file fails on this JVM", IOException::class.java) {
                FileOutputStream(probeTarget).close()
            }
            assertTrue("fixture: the store's file is read-only", file.setWritable(false))

            kitRun { store.saveProgress("kit-b", kitB) } // never throws
            assertArrayEquals("the old file is kept", oldBytes, file.readBytes())
            kitRun {
                assertEquals("the same instance reads what was saved", kitB, store.progress("kit-b"))
                val reader = kitStore(file)
                assertEquals("a new instance reads the old file", kitA, reader.progress("kit-a"))
                assertEquals("a new instance does not see the failed save", PuzzleProgress.FRESH, reader.progress("kit-b"))
            }
        } finally {
            file.setWritable(true)
            probeTarget.setWritable(true)
        }

        kitRun { store.saveProgress("kit-c", kitC) }
        kitRun {
            val after = kitStore(file)
            assertEquals("the next save retried the failed one", kitB, after.progress("kit-b"))
            assertEquals("and wrote its own", kitC, after.progress("kit-c"))
            assertEquals("and kept the old content", kitA, after.progress("kit-a"))
            assertEquals(kitSettings, after.settings())
            assertEquals("kit-a", after.lastOpenedPuzzleId())
        }
    }

    // The same ADR-003 rule, with the failure provoked through S-17's `file` another way: its folder is a
    // regular file, so nothing can be written under it (shown test-side). There is no old file, so the store
    // reads fresh; a save does not throw and the same instance reads it; once the folder exists, the next
    // save retries and a new instance reads both saves. Pin, no token.
    @Test
    fun aWriteThatCannotReachItsFolderNeverThrowsAndTheNextSaveRetries() {
        val blocker = kitTmp.newFile("kit-blocker")
        val file = File(blocker, "progress.json")
        assertThrows("fixture: nothing can be written under a regular file", IOException::class.java) {
            Files.write(File(blocker, "kit-probe").toPath(), byteArrayOf(1))
        }
        val store = kitStore(file)
        kitAssertFresh(store, listOf("kit-a"), "a file under a regular file")
        kitRun { store.saveProgress("kit-a", kitA) } // never throws
        kitRun { assertEquals("the same instance reads what was saved", kitA, store.progress("kit-a")) }

        assertTrue("fixture: the blocker removed", blocker.delete())
        assertTrue("fixture: the folder made", blocker.mkdir())
        kitRun { store.saveProgress("kit-b", kitB) }
        kitRun {
            val after = kitStore(file)
            assertEquals("the next save retried the failed one", kitA, after.progress("kit-a"))
            assertEquals("and wrote its own", kitB, after.progress("kit-b"))
        }
    }
}
