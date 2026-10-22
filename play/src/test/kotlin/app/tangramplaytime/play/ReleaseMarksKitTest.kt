// Acceptance kit (WO-009, TASK-064, stage 1) — REQ-039 A2 on the puzzle files the release APK of `:app` carries.
//
// - The reading (decisions.md 2026-10-22 R4, cited by its title without the dot: "REQ-039 A2 and REQ-042 A1..A3 are read
//   from the release APK; the REQ-042 legs stay red until the owner's marks, and run 2 of the JVM line runs with
//   `--continue` during WO-009."; tasks.md § WO-009 seam note 1, "Released means shipped": "an unmarked puzzle file is
//   absent from the release APK and the AAB, so REQ-039 A2 and REQ-042 are read from the release APK's files"). The code
//   home is `play/` (build-map.md #PuzzleArt: "A2 (release marks) is WO-009's and its test must live under `play/`").
// - What the APK holds (tasks.md § WO-009 S-55, "today's output layout"): Java resources at the zip root,
//   `app/tangramplaytime/content/puzzles/index.txt` (the sorted file names, one per line) and each file it names
//   (`BundledPuzzles.DIR`, read as a declaration). Every file under that folder is judged: each file `index.txt` names
//   (a named file missing from the zip fails) and any other file the folder holds (it ships too: "absent from the
//   release APK", seam note 1).
// - The mark is read with this file's own strict JSON reader (`:play`'s test classpath has no JSON library of
//   `:content`'s): the whole text must be one JSON value (RFC 8259; a duplicate key is refused), and the mark is the
//   root object's `provenance.reviewedByHuman` (contracts/Types.kt, `PuzzleDefinition.reviewedByHuman`: "the file's
//   `provenance.reviewedByHuman`; a released puzzle has `true` (REQ-039 rule and A2)"). Only JSON `true` is a mark: a
//   missing `provenance` or field reads as `false` (decisions.md 2026-10-11 "A missing `provenance` or
//   `reviewedByHuman` reads as `false`."), and a text that does not read as one JSON object is not a marked file.
// - Liveness: `index.txt` was read. It may name zero files: an empty release ships no unreviewed puzzle (REQ-042 A1, in
//   `content/`'s ReleaseSetKitTest, is what catches emptiness).
// - Discrimination, in this class: `theMarkCheckFlagsEveryCraftedTextThatIsNotMarkedTrue` runs the same check over
//   crafted texts — one `false`, one with no `provenance` (the brief's three, with one `true`), plus a `provenance`
//   without the field, a decoy `"reviewedByHuman": true` outside `provenance`, a truncated text, and the one-line
//   `provenance` layout of the two minis (S-54) — and flags exactly the ones that are not marked `true`. On today's tree
//   the release APK still carries every unmarked file (the in-situ fixture) until TASK-065's release set lands.
// - The test builds nothing. Run 1 of the JVM channel line builds and repackages both APKs; this test runs in run 2
//   (platform-notes/android.md: "A test that reads build outputs runs after them, in a separate Gradle run"). An APK
//   older than the newest file of the TASK-064 row's staleness sources is refused, not judged ("A harness that installs
//   build outputs refuses stale ones"); the top-level puzzle files are among them, so a mark made after a build refuses
//   the old APK. A missing APK fails with the build line. The project root is the nearest ancestor of the working
//   directory that holds `settings.gradle.kts`.
// - Every check is computed and printed (the XML's system-out), then the test fails with every failed check. No
//   `assume…`; nothing is skipped. Helper names carry a `kit` / `Kit` / `KIT_` prefix and are members of this class.
package app.tangramplaytime.play

import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.time.Instant
import java.util.zip.ZipFile

class ReleaseMarksKitTest {

    private companion object {
        const val KIT_RELEASE_APK = "app/build/outputs/apk/release/app-release-unsigned.apk"

        /** Run 1 of the JVM channel line (workorders/WO-009-briefs.md § Verification channels), which builds it. */
        const val KIT_BUILD_LINE = ":app:assembleRelease :app:assembleDebug :app:packageRelease --rerun :app:packageDebug --rerun"

        /** `BundledPuzzles.DIR` (`content/src/main/.../BundledPuzzles.kt`, a declaration), as a zip folder (S-55). */
        const val KIT_PUZZLE_DIR = "app/tangramplaytime/content/puzzles/"

        /** The sorted file names, one per line (S-55; `BundledPuzzles` KDoc "the generated `index.txt`"). */
        const val KIT_INDEX = "index.txt"

        /**
         * The TASK-064 row's staleness sources (its Inputs cell): `content/build.gradle.kts`, `app/build.gradle.kts`,
         * `settings.gradle.kts`, `gradle/libs.versions.toml`, the main source folders of `:content` and `:app` — and the
         * top-level puzzle files of `Tangrams/` ([kitPuzzleFiles]).
         */
        val KIT_STALENESS_INPUTS = listOf(
            "content/build.gradle.kts", "app/build.gradle.kts", "settings.gradle.kts", "gradle/libs.versions.toml",
            "content/src/main", "app/src/main",
        )

        /** How many file names a message lists before it says "…". */
        const val KIT_SHOW = 12

        // ── Crafted texts (hand-made; each names what it is) ─────────────────────────────────────

        /** The multi-line `provenance` layout of the shipped full puzzles, marked `false`: not marked. */
        val KIT_TEXT_FALSE = """
            {
              "format": "tangram-puzzle/1",
              "id": "shapes-kit-false",
              "title": {"en": "Kit puzzle", "fi": "Testipalapeli"},
              "category": "shapes",
              "difficulty": 1,
              "kind": "mini",
              "solution": [{"piece": "ST1", "polygon": [[0, 0], [2, 0], [1, 1]]}],
              "art": {"base": "#E9C46A", "shapes": []},
              "provenance": {
                "author": "human:kit",
                "reviewedByHuman": false
              }
            }
        """.trimIndent() + "\n"

        /** No `provenance` at all (the schema makes it optional): reads as `false` (the 2026-10-11 row). */
        val KIT_TEXT_NO_PROVENANCE = """
            {"format": "tangram-puzzle/1", "id": "shapes-kit-no-provenance", "title": {"en": "Kit puzzle", "fi": "Testipalapeli"}, "category": "shapes", "difficulty": 1, "kind": "mini", "solution": [{"piece": "ST1", "polygon": [[0, 0], [2, 0], [1, 1]]}], "art": {"base": "#E9C46A", "shapes": []}}
        """.trimIndent() + "\n"

        /** The same multi-line layout, marked `true`: marked. */
        val KIT_TEXT_TRUE = KIT_TEXT_FALSE.replace("shapes-kit-false", "shapes-kit-true").replace("\"reviewedByHuman\": false", "\"reviewedByHuman\": true")

        /** The two minis' one-line `provenance` layout (S-54: "the two minis' one-line one"), marked `true`: marked. */
        val KIT_TEXT_TRUE_ONE_LINE = """
            {
              "format": "tangram-puzzle/1",
              "id": "shapes-kit-true-one-line",
              "category": "shapes",
              "provenance": {"author": "ai:claude (mini test puzzle)", "reviewedByHuman": true}
            }
        """.trimIndent() + "\n"

        /** A `provenance` without `reviewedByHuman`: reads as `false` (the 2026-10-11 row: "or `reviewedByHuman`"). */
        val KIT_TEXT_NO_FIELD = KIT_TEXT_FALSE.replace("shapes-kit-false", "shapes-kit-no-field")
            .replace("\"author\": \"human:kit\",\n    \"reviewedByHuman\": false", "\"author\": \"human:kit\"")

        /**
         * A decoy: `"reviewedByHuman": true` as a key of `art`, before a `provenance` that says `false`. The mark is
         * `provenance.reviewedByHuman` (Types.kt KDoc), so this file is not marked; a reader that takes the first
         * `"reviewedByHuman"` of the text would call it marked.
         */
        val KIT_TEXT_DECOY = KIT_TEXT_FALSE.replace("shapes-kit-false", "shapes-kit-decoy")
            .replace("\"art\": {\"base\": \"#E9C46A\", \"shapes\": []}", "\"art\": {\"base\": \"#E9C46A\", \"shapes\": [], \"reviewedByHuman\": true}")

        /** The marked text cut short before its last brace: not one JSON value, so not a marked file. */
        val KIT_TEXT_TRUNCATED = KIT_TEXT_TRUE.replace("shapes-kit-true", "shapes-kit-truncated").trimEnd().removeSuffix("}")
    }

    /** One file's mark as read: whether it is JSON `true`, and what was read. */
    private data class KitMark(val marked: Boolean, val reading: String)

    /** The release APK's puzzle folder: `index.txt`'s text (null when absent) and every other file under it, by name. */
    private data class KitFolder(val index: String?, val files: Map<String, ByteArray>)

    // REQ-039.A2 — "No released puzzle has reviewedByHuman: false." Released means shipped (seam note 1; R4): every puzzle
    // file the release APK (app-release-unsigned.apk) carries under app/tangramplaytime/content/puzzles/ — each file its
    // index.txt names, and any other file of that folder — holds `provenance.reviewedByHuman` JSON `true` (REQ-039 rule:
    // "A puzzle is released only after a person has solved it and approved its picture (reviewedByHuman: true)"; a missing
    // provenance or field reads as false, decisions.md 2026-10-11). A file index.txt names that the zip lacks fails.
    // Liveness: index.txt was read (zero names is allowed: an empty release ships no unreviewed puzzle). A stale or
    // missing APK fails, never passes. The check's discrimination is the crafted-text test below, in this class.
    @Test
    fun noPuzzleFileTheReleaseApkCarriesHasReviewedByHumanFalse() { // REQ-039.A2
        val checks = mutableListOf<Pair<Boolean, String>>()
        val apk = kitJudgeableApk(checks)
        if (apk != null) {
            checks += kitMarkChecks("the release APK", kitPuzzleFolder(apk))
        }
        kitReport("noPuzzleFileTheReleaseApkCarriesHasReviewedByHumanFalse", checks)
    }

    // Pin, no token: the discrimination of the REQ-039 A2 check above (workorders/WO-009-briefs.md TASK-064 "Discrimination
    // in the same class: your check over crafted texts (one with `false`, one with no `provenance`, one with `true`) flags
    // exactly the first two"; the TASK-064 row: "Crafted fixtures (a file with `false`; a file with no `provenance`) must
    // fail"). The same reading and the same folder check, over crafted folders:
    // - per text: exactly the texts that are not `provenance.reviewedByHuman` JSON `true` are flagged — `false`; no
    //   `provenance`; no field (the 2026-10-11 row); the decoy outside `provenance` (Types.kt KDoc: "the file's
    //   `provenance.reviewedByHuman`"); the truncated text — and the two marked layouts (S-54: the multi-line `provenance`
    //   and the minis' one-line one) are not;
    // - the folder check fails over the folder of all seven, naming the five, and passes over the folder of the two
    //   marked ones;
    // - a file index.txt names but the zip lacks fails ("a named file missing from the zip fails", the brief); a file the
    //   folder holds but index.txt does not name is still judged (seam note 1: released means shipped); a folder with no
    //   index.txt fails its liveness; an index.txt naming no file, over an empty folder, passes (the brief: "it may name
    //   zero files: an empty release ships no unreviewed puzzle").
    @Test
    fun theMarkCheckFlagsEveryCraftedTextThatIsNotMarkedTrue() {
        val checks = mutableListOf<Pair<Boolean, String>>()
        val texts = linkedMapOf(
            "shapes-kit-false.json" to KIT_TEXT_FALSE,
            "shapes-kit-no-provenance.json" to KIT_TEXT_NO_PROVENANCE,
            "shapes-kit-no-field.json" to KIT_TEXT_NO_FIELD,
            "shapes-kit-decoy.json" to KIT_TEXT_DECOY,
            "shapes-kit-truncated.json" to KIT_TEXT_TRUNCATED,
            "shapes-kit-true.json" to KIT_TEXT_TRUE,
            "shapes-kit-true-one-line.json" to KIT_TEXT_TRUE_ONE_LINE,
        )
        val marked = setOf("shapes-kit-true.json", "shapes-kit-true-one-line.json")
        val notMarked = texts.keys - marked

        // Fixture checks: each crafted edit landed (a no-op replace would make two texts the same).
        checks += (texts.values.toSet().size == texts.size) to "fixture: the ${texts.size} crafted texts are pairwise different"
        checks += ("\"author\": \"human:kit\"\n" in KIT_TEXT_NO_FIELD && "reviewedByHuman" !in KIT_TEXT_NO_FIELD) to
            "fixture: the no-field text has a provenance without reviewedByHuman"
        checks += (KIT_TEXT_DECOY.indexOf("\"reviewedByHuman\": true") in 0 until KIT_TEXT_DECOY.indexOf("\"provenance\"")) to
            "fixture: the decoy's first \"reviewedByHuman\" is a true one, before its provenance"

        for ((name, text) in texts) {
            val mark = kitReadMark(text.toByteArray(Charsets.UTF_8))
            val want = name in marked
            checks += (mark.marked == want) to
                "per text: $name read as ${if (mark.marked) "marked" else "NOT marked"} (${mark.reading}); expected ${if (want) "marked" else "not marked"}"
        }

        val all = texts.mapValues { it.value.toByteArray(Charsets.UTF_8) }
        val allChecks = kitMarkChecks("the crafted folder of seven", KitFolder(kitIndexOf(all.keys), all))
        val flagged = allChecks.filter { !it.first }
        checks += (flagged.size == 1 && notMarked.all { it in flagged.single().second } && marked.none { it in flagged.single().second }) to
            "the folder check over all seven fails once, naming exactly the ${notMarked.size} not marked: ${flagged.map { it.second }}"

        val good = all.filterKeys { it in marked }
        val goodChecks = kitMarkChecks("the crafted folder of the two marked", KitFolder(kitIndexOf(good.keys), good))
        checks += goodChecks.all { it.first } to "the folder check over the two marked passes: ${goodChecks.filter { !it.first }.map { it.second }}"

        val missing = kitMarkChecks("a folder whose index.txt names a file it lacks", KitFolder(kitIndexOf(good.keys + "shapes-kit-gone.json"), good))
        checks += missing.any { !it.first && "shapes-kit-gone.json" in it.second } to
            "a named file missing from the zip fails: ${missing.filter { !it.first }.map { it.second }}"

        val unnamed = kitMarkChecks(
            "a folder holding an unmarked file index.txt does not name",
            KitFolder(kitIndexOf(good.keys), good + ("shapes-kit-false.json" to all.getValue("shapes-kit-false.json"))),
        )
        checks += unnamed.any { !it.first && "shapes-kit-false.json" in it.second } to
            "an unmarked file the folder holds but index.txt does not name is judged and fails: ${unnamed.filter { !it.first }.map { it.second }}"

        val noIndex = kitMarkChecks("a folder with no index.txt", KitFolder(null, good))
        checks += noIndex.any { !it.first && KIT_INDEX in it.second } to
            "a folder with no index.txt fails its liveness: ${noIndex.filter { !it.first }.map { it.second }}"

        val empty = kitMarkChecks("an empty release", KitFolder("", emptyMap()))
        checks += empty.all { it.first } to "an index.txt naming no file over an empty folder passes: ${empty.map { it.second }}"

        kitReport("theMarkCheckFlagsEveryCraftedTextThatIsNotMarkedTrue", checks)
    }

    // ── Kit helpers ──────────────────────────────────────────────────────────────────────────────

    /**
     * The REQ-039 A2 checks over one puzzle folder: liveness (index.txt read), every named file present, and every file
     * of the folder (named or not) marked JSON `true`. One failed check names every file that is not marked.
     */
    private fun kitMarkChecks(what: String, folder: KitFolder): List<Pair<Boolean, String>> {
        val checks = mutableListOf<Pair<Boolean, String>>()
        val index = folder.index
        if (index == null) {
            checks += false to "$what: no $KIT_INDEX under $KIT_PUZZLE_DIR — the folder was not read (liveness)"
            return checks
        }
        val names = index.lines().map { it.trim() }.filter { it.isNotEmpty() }
        checks += true to "$what: $KIT_INDEX read, naming ${names.size} file(s); the folder holds ${folder.files.size} file(s) besides $KIT_INDEX"
        val absent = names.filter { it !in folder.files }
        checks += absent.isEmpty() to
            "$what: ${absent.size} file(s) named in $KIT_INDEX but missing from the zip${kitList(absent)}"
        val unnamed = folder.files.keys.filter { it !in names }.sorted()
        if (unnamed.isNotEmpty()) println("ReleaseMarksKitTest INFO $what: ${unnamed.size} file(s) under $KIT_PUZZLE_DIR not named in $KIT_INDEX, judged too${kitList(unnamed)}")
        val judged = folder.files.keys.sorted()
        val readings = judged.associateWith { kitReadMark(folder.files.getValue(it)) }
        val notMarked = readings.filterValues { !it.marked }
        val byReading = notMarked.entries.groupBy({ it.value.reading }, { it.key })
        for ((reading, files) in byReading) println("ReleaseMarksKitTest INFO $what: ${files.size} file(s) read as \"$reading\"${kitList(files)}")
        checks += notMarked.isEmpty() to
            "$what: ${notMarked.size} of ${judged.size} puzzle file(s) not marked reviewedByHuman: true${kitList(notMarked.keys.toList())}"
        return checks
    }

    /** One file's mark: the root object's `provenance.reviewedByHuman`, JSON `true` or not, and what was read. */
    private fun kitReadMark(bytes: ByteArray): KitMark {
        val text = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: CharacterCodingException) {
            return KitMark(false, "not UTF-8 text")
        }
        val root = try {
            KitJson(text).document()
        } catch (e: IllegalArgumentException) {
            return KitMark(false, "not one JSON value: ${e.message}")
        }
        if (root !is Map<*, *>) return KitMark(false, "not a JSON object")
        if (!root.containsKey("provenance")) return KitMark(false, "no provenance (reads as false)")
        val provenance = root["provenance"] as? Map<*, *> ?: return KitMark(false, "provenance is not an object")
        if (!provenance.containsKey("reviewedByHuman")) return KitMark(false, "provenance without reviewedByHuman (reads as false)")
        return when (val value = provenance["reviewedByHuman"]) {
            true -> KitMark(true, "reviewedByHuman: true")
            false -> KitMark(false, "reviewedByHuman: false")
            else -> KitMark(false, "reviewedByHuman is not a boolean: $value")
        }
    }

    /** The `index.txt` text a folder of [names] would carry: the sorted names, one per line. */
    private fun kitIndexOf(names: Collection<String>): String = names.sorted().joinToString("\n", postfix = "\n")

    /** The release APK's puzzle folder: `index.txt` and every other non-folder entry directly or deeper under it. */
    private fun kitPuzzleFolder(apk: File): KitFolder = ZipFile(apk).use { zip ->
        var index: String? = null
        val files = LinkedHashMap<String, ByteArray>()
        for (entry in zip.entries().asSequence().filter { !it.isDirectory && it.name.startsWith(KIT_PUZZLE_DIR) }) {
            val name = entry.name.removePrefix(KIT_PUZZLE_DIR)
            val bytes = zip.getInputStream(entry).use { it.readBytes() }
            if (name == KIT_INDEX) index = String(bytes, Charsets.UTF_8) else files[name] = bytes
        }
        KitFolder(index, files)
    }

    /** ": a, b, …" (the first [KIT_SHOW] names, then how many more) for a non-empty list; "" for an empty one. */
    private fun kitList(names: List<String>): String =
        if (names.isEmpty()) "" else ": " + names.take(KIT_SHOW).joinToString(", ") + if (names.size > KIT_SHOW) ", … (${names.size - KIT_SHOW} more)" else ""

    /** Prints every check, then fails with every failed one. */
    private fun kitReport(test: String, checks: List<Pair<Boolean, String>>) {
        for ((ok, text) in checks) println("ReleaseMarksKitTest ${if (ok) "PASS" else "FAIL"} $test: $text")
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

    /** The top-level puzzle files of `Tangrams/`: `*.json` but `*.schema.json` (S-54, S-55). */
    private fun kitPuzzleFiles(root: File): List<File> =
        File(root, "Tangrams").listFiles()?.filter { it.isFile && it.name.endsWith(".json") && !it.name.endsWith(".schema.json") }.orEmpty()

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
        val puzzles = kitPuzzleFiles(root)
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

    /** A JSON number, kept as its text (only the mark's type matters here). */
    private data class KitJsonNumber(val text: String) {
        override fun toString() = text
    }

    /** JSON `null`, kept apart from a missing key. */
    private object KitJsonNull {
        override fun toString() = "null"
    }

    /**
     * A strict JSON reader (RFC 8259): the text is exactly one value with only whitespace around it. Objects are
     * [LinkedHashMap]s (a duplicate key is refused), arrays [List]s, strings [String]s with every escape decoded, numbers
     * [KitJsonNumber]s, `true` / `false` [Boolean]s and `null` [KitJsonNull]. Anything else throws
     * [IllegalArgumentException] naming the character position.
     */
    private class KitJson(private val s: String) {
        private var i = 0

        fun document(): Any {
            val value = value()
            space()
            if (i != s.length) fail("text after the value")
            return value
        }

        private fun fail(what: String): Nothing = throw IllegalArgumentException("$what at character $i")

        private fun space() {
            while (i < s.length && (s[i] == ' ' || s[i] == '\t' || s[i] == '\n' || s[i] == '\r')) i++
        }

        private fun value(): Any {
            space()
            if (i >= s.length) fail("the end of the text where a value belongs")
            return when (val c = s[i]) {
                '{' -> obj()
                '[' -> array()
                '"' -> string()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", KitJsonNull)
                else -> if (c == '-' || c in '0'..'9') number() else fail("an unexpected '$c'")
            }
        }

        private fun literal(word: String, value: Any): Any {
            if (!s.startsWith(word, i)) fail("not the literal $word")
            i += word.length
            return value
        }

        private fun obj(): Map<String, Any> {
            i++
            val members = LinkedHashMap<String, Any>()
            space()
            if (i < s.length && s[i] == '}') {
                i++
                return members
            }
            while (true) {
                space()
                if (i >= s.length || s[i] != '"') fail("no key string")
                val key = string()
                space()
                if (i >= s.length || s[i] != ':') fail("no ':' after the key \"$key\"")
                i++
                val value = value()
                if (members.put(key, value) != null) fail("the duplicate key \"$key\"")
                space()
                if (i >= s.length) fail("the end of the text inside an object")
                when (s[i]) {
                    ',' -> i++
                    '}' -> {
                        i++
                        return members
                    }
                    else -> fail("neither ',' nor '}' after a member")
                }
            }
        }

        private fun array(): List<Any> {
            i++
            val items = ArrayList<Any>()
            space()
            if (i < s.length && s[i] == ']') {
                i++
                return items
            }
            while (true) {
                items += value()
                space()
                if (i >= s.length) fail("the end of the text inside an array")
                when (s[i]) {
                    ',' -> i++
                    ']' -> {
                        i++
                        return items
                    }
                    else -> fail("neither ',' nor ']' after an item")
                }
            }
        }

        private fun string(): String {
            i++
            val out = StringBuilder()
            while (true) {
                if (i >= s.length) fail("an unterminated string")
                val c = s[i++]
                when {
                    c == '"' -> return out.toString()
                    c == '\\' -> {
                        if (i >= s.length) fail("an unterminated escape")
                        when (val e = s[i++]) {
                            '"' -> out.append('"')
                            '\\' -> out.append('\\')
                            '/' -> out.append('/')
                            'b' -> out.append('\b')
                            'f' -> out.append('\u000C')
                            'n' -> out.append('\n')
                            'r' -> out.append('\r')
                            't' -> out.append('\t')
                            'u' -> {
                                val hex = if (i + 4 <= s.length) s.substring(i, i + 4) else fail("a short \\u escape")
                                if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) fail("a bad \\u escape")
                                out.append(hex.toInt(16).toChar())
                                i += 4
                            }
                            else -> fail("the bad escape \\$e")
                        }
                    }
                    c < ' ' -> fail("a control character inside a string")
                    else -> out.append(c)
                }
            }
        }

        private fun number(): KitJsonNumber {
            val start = i
            if (s[i] == '-') i++
            when {
                i < s.length && s[i] == '0' -> i++
                i < s.length && s[i] in '1'..'9' -> while (i < s.length && s[i] in '0'..'9') i++
                else -> fail("no digit in a number")
            }
            if (i < s.length && s[i] == '.') {
                i++
                if (i >= s.length || s[i] !in '0'..'9') fail("no digit after a decimal point")
                while (i < s.length && s[i] in '0'..'9') i++
            }
            if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
                i++
                if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
                if (i >= s.length || s[i] !in '0'..'9') fail("no digit in an exponent")
                while (i < s.length && s[i] in '0'..'9') i++
            }
            return KitJsonNumber(s.substring(start, i))
        }
    }
}
