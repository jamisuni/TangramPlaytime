// Acceptance kit (WO-005, TASK-023) — REQ-046 A4 on the build outputs of `:app`: the release APK holds none of
// the DEV aid's three markers (tasks.md § WO-005 › Seams S-30), and the debug APK holds all three — the positive
// control, in the same test, so a scan that cannot see a marker cannot pass. decisions.md 2026-10-17, the planner
// Q7 row ("… A4's oracle is the release APK"; its dotted title is written here without the dot, so the scanner
// counts no second token): "none of the aid's three markers … in the release APK; all three in the debug APK (its
// positive control); at least one product class read in each; a stale APK refused".
//
// - The reading of A4 (S-30, G-08, O-08): the aid is the DEV button, the passcode and the overlay, and it exists
//   only in `play/src/debug`; a release build is the `release` build type (tasks.md § WO-005 "Unverified by
//   design"). Its markers: (a) class descriptors under `Lapp/tangramplaytime/play/devaid/` in `classes*.dex`;
//   (b) resource names starting `dev_aid_` in `resources.arsc`; (c) the zip entry
//   `META-INF/services/app.tangramplaytime.play.DevAidHook`.
// - Two content markers, so an aid that leaks under other names cannot pass (reviews/WO-005-code-review.md S1;
//   G-08: "the release APK contains no DEV button, no passcode and no solution overlay"): (d) the REQ-046 rule 1
//   text "Wrong passcode." as a string of `resources.arsc`, read in UTF-8 and UTF-16LE as the names are; (e) the
//   passcode of the REQ-046 statement, "0417", as an entry of a dex string table (an exact entry, never a raw
//   byte search, which could hit by chance), where the aid's `const` puts it (S-26: "The passcode is a `const`
//   in the aid folder"). Each has its positive control in the debug APK.
// - The test builds nothing. The JVM channel line builds and repackages both APKs first (tasks.md § WO-005 seam
//   note 4); an APK older than the newest file of the sources the TASK-023 row names is refused, not judged
//   (platform-notes/android.md, WO-004 lesson: "A pass on an old APK is a pass for code nobody reviewed"). A
//   missing APK fails with the command that builds it. Nothing is skipped.
// - Every check is computed and printed (the XML's system-out), then the test fails with every failed check, so
//   one run shows both legs. Precedent: `.swdev/verifiers/v01_release_apk.py` (its positive control and its "the
//   scan read nothing" guard); it is not changed or called here.
// - Helper names carry a `kit` / `KIT_` prefix and are members of this class.
package app.tangramplaytime.play

import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.util.zip.ZipFile

class ReleaseBuildKitTest {

    private companion object {
        const val KIT_RELEASE_APK = "app/build/outputs/apk/release/app-release-unsigned.apk"
        const val KIT_DEBUG_APK = "app/build/outputs/apk/debug/app-debug.apk"

        /** The sources and build files an APK must not be older than (the TASK-023 row). */
        val KIT_STALENESS_INPUTS = listOf("play/src/main", "play/src/debug", "app/src/main", "play/build.gradle.kts", "app/build.gradle.kts")

        /** A product class of `:play` — the "the scan read nothing" guard (brief: at least one in each APK). */
        const val KIT_PRODUCT_PREFIX = "Lapp/tangramplaytime/play/"

        /** S-30 marker (a). */
        const val KIT_AID_CLASS_PREFIX = "Lapp/tangramplaytime/play/devaid/"

        /** S-30 marker (b). */
        const val KIT_AID_RES_PREFIX = "dev_aid_"

        /** S-30 marker (c). */
        const val KIT_SERVICE_ENTRY = "META-INF/services/app.tangramplaytime.play.DevAidHook"

        /** Content marker (d): REQ-046 rule 1, "A wrong passcode shows \"Wrong passcode.\"". */
        const val KIT_WRONG_PASSCODE_TEXT = "Wrong passcode."

        /** Content marker (e): the REQ-046 statement's passcode, "asks for the passcode 0417". */
        const val KIT_PASSCODE = "0417"

        val KIT_DEX_ENTRY = Regex("""classes\d*\.dex""")

        /** `RES_TABLE_TYPE` of the Android resource table's first chunk. */
        const val KIT_RES_TABLE_TYPE = 0x0002
    }

    /** What one APK holds, as read from its zip entries. */
    private data class KitApkScan(
        val dexCount: Int,
        /** Class definitions (class_defs) under [KIT_PRODUCT_PREFIX]. */
        val productClasses: List<String>,
        /** Class definitions under [KIT_AID_CLASS_PREFIX]. */
        val aidClassDefs: List<String>,
        /** Every class descriptor of the dex type table (type_ids: defined or referenced) under [KIT_AID_CLASS_PREFIX]. */
        val aidDescriptors: List<String>,
        /** The resource table parsed as one: its first chunk is RES_TABLE_TYPE, its size is the entry's, ≥ 1 package. */
        val resTableRead: Boolean,
        val resUtf8: Boolean,
        val resUtf16: Boolean,
        val serviceEntry: Boolean,
        /** Content marker (d) in resources.arsc, as UTF-8 and as UTF-16LE bytes. */
        val textUtf8: Boolean,
        val textUtf16: Boolean,
        /** The dex files whose string table holds an entry exactly equal to [KIT_PASSCODE] (content marker (e)). */
        val passcodeDex: List<String>,
    )

    // REQ-046.A4 — "A release build contains no DEV button." The release APK of `:app`
    // (app-release-unsigned.apk) holds none of S-30's three markers, in any dex file, in the resource table in
    // either string encoding (UTF-8 or UTF-16LE), or as a zip entry; the debug APK (app-debug.apk), built from the
    // same tree, holds all three — the positive control — and prints which encoding the resource scan sees. The two
    // content markers go the same way: no "Wrong passcode." (REQ-046 rule 1) in the release APK's resource table in
    // either encoding and no "0417" (REQ-046 statement) entry in any of its dex string tables; both in the debug
    // APK (the planner-Q7 row of decisions.md 2026-10-17, "… A4's oracle is the release APK": "none of the aid's
    // markers … in the release APK; all … in the debug APK (its positive control)"; code review S1). Each
    // APK yields at least one class under `Lapp/tangramplaytime/play/` and a resource table that parses, so a scan
    // that read nothing cannot pass. Both APKs are refused when stale (older than the newest file under
    // `play/src/main`, `play/src/debug` or `app/src/main`, or than `play/build.gradle.kts` or
    // `app/build.gradle.kts`); a missing one fails. The project root is the nearest ancestor of the working
    // directory that holds `settings.gradle.kts`.
    @Test
    fun aReleaseBuildContainsNoDevButton() { // REQ-046.A4
        val root = kitProjectRoot()
        val (newestFile, newestTime) = kitNewestInput(root)
        val checks = mutableListOf<Pair<Boolean, String>>()
        val debug = kitJudgeable(root, KIT_DEBUG_APK, newestFile, newestTime, checks)?.let { kitScan(it) }
        val release = kitJudgeable(root, KIT_RELEASE_APK, newestFile, newestTime, checks)?.let { kitScan(it) }

        if (debug != null) {
            checks += kitRead("debug", debug)
            checks += (debug.aidClassDefs.isNotEmpty() to
                "positive control (a): ${debug.aidClassDefs.size} class(es) under $KIT_AID_CLASS_PREFIX in the debug APK ${debug.aidClassDefs.take(3)}")
            val seen = listOfNotNull("UTF-8".takeIf { debug.resUtf8 }, "UTF-16LE".takeIf { debug.resUtf16 })
            checks += (seen.isNotEmpty() to
                "positive control (b): resource names starting $KIT_AID_RES_PREFIX in the debug APK's resources.arsc — seen as ${if (seen.isEmpty()) "neither UTF-8 nor UTF-16LE" else seen.joinToString(" and ")}")
            checks += (debug.serviceEntry to "positive control (c): the zip entry $KIT_SERVICE_ENTRY in the debug APK: ${if (debug.serviceEntry) "present" else "absent"}")
            val textSeen = listOfNotNull("UTF-8".takeIf { debug.textUtf8 }, "UTF-16LE".takeIf { debug.textUtf16 })
            checks += (textSeen.isNotEmpty() to
                "positive control (d): the text \"$KIT_WRONG_PASSCODE_TEXT\" in the debug APK's resources.arsc — seen as ${if (textSeen.isEmpty()) "neither UTF-8 nor UTF-16LE" else textSeen.joinToString(" and ")}")
            checks += (debug.passcodeDex.isNotEmpty() to
                "positive control (e): a dex string-table entry \"$KIT_PASSCODE\" in the debug APK: ${if (debug.passcodeDex.isEmpty()) "in none of ${debug.dexCount} dex file(s)" else "in ${debug.passcodeDex}"}")
        }
        if (release != null) {
            checks += kitRead("release", release)
            checks += (release.aidDescriptors.isEmpty() to
                "release (a): ${release.aidDescriptors.size} class descriptor(s) under $KIT_AID_CLASS_PREFIX in ${release.dexCount} dex file(s) ${release.aidDescriptors.take(5)}")
            checks += ((!release.resUtf8 && !release.resUtf16) to
                "release (b): resource names starting $KIT_AID_RES_PREFIX in resources.arsc — UTF-8 ${if (release.resUtf8) "FOUND" else "none"}, UTF-16LE ${if (release.resUtf16) "FOUND" else "none"}")
            checks += (!release.serviceEntry to "release (c): the zip entry $KIT_SERVICE_ENTRY: ${if (release.serviceEntry) "FOUND" else "absent"}")
            checks += ((!release.textUtf8 && !release.textUtf16) to
                "release (d): the text \"$KIT_WRONG_PASSCODE_TEXT\" in resources.arsc — UTF-8 ${if (release.textUtf8) "FOUND" else "none"}, UTF-16LE ${if (release.textUtf16) "FOUND" else "none"}")
            checks += (release.passcodeDex.isEmpty() to
                "release (e): a dex string-table entry \"$KIT_PASSCODE\": ${if (release.passcodeDex.isEmpty()) "in none of ${release.dexCount} dex file(s)" else "FOUND in ${release.passcodeDex}"}")
        }

        for ((ok, text) in checks) println("ReleaseBuildKitTest ${if (ok) "PASS" else "FAIL"} $text")
        val failed = checks.filter { !it.first }
        if (failed.isNotEmpty()) {
            throw AssertionError(failed.joinToString("\n", prefix = "${failed.size} of ${checks.size} checks failed:\n") { it.second })
        }
    }

    // ── Kit helpers ──────────────────────────────────────────────────────────────────────────────

    /** The nearest ancestor of the working directory (itself included) that holds settings.gradle.kts. */
    private fun kitProjectRoot(): File {
        val start = File(System.getProperty("user.dir") ?: throw AssertionError("no user.dir: cannot find the project root")).absoluteFile
        return generateSequence(start) { it.parentFile }.firstOrNull { File(it, "settings.gradle.kts").isFile }
            ?: throw AssertionError("no ancestor of $start holds settings.gradle.kts; cannot find the project root")
    }

    /** The newest file (by modification time) of [KIT_STALENESS_INPUTS]; a missing input fails, since staleness cannot be judged. */
    private fun kitNewestInput(root: File): Pair<File, Long> {
        var newest: File? = null
        for (path in KIT_STALENESS_INPUTS) {
            val f = File(root, path)
            if (!f.exists()) throw AssertionError("cannot judge staleness: $path does not exist under $root")
            for (file in f.walkTopDown().filter { it.isFile }) {
                if (newest == null || file.lastModified() > newest.lastModified()) newest = file
            }
        }
        val found = newest ?: throw AssertionError("cannot judge staleness: no file under $KIT_STALENESS_INPUTS")
        return found to found.lastModified()
    }

    /** [path] under [root] when it exists and is not older than the newest input; otherwise null, with a failed check saying why. */
    private fun kitJudgeable(root: File, path: String, newestFile: File, newestTime: Long, checks: MutableList<Pair<Boolean, String>>): File? {
        val apk = File(root, path)
        if (!apk.isFile) {
            checks += (false to "$path not found — build both APKs with the JVM channel line (:app:assembleRelease :app:assembleDebug :app:packageRelease --rerun :app:packageDebug --rerun)")
            return null
        }
        val apkTime = apk.lastModified()
        val newest = "${newestFile.relativeTo(root).invariantSeparatorsPath} (${Instant.ofEpochMilli(newestTime)})"
        if (apkTime < newestTime) {
            checks += (false to "$path (${Instant.ofEpochMilli(apkTime)}) is STALE: older than $newest — refused, not judged; rebuild with the JVM channel line")
            return null
        }
        checks += (true to "$path (${Instant.ofEpochMilli(apkTime)}) is not older than the newest input, $newest")
        return apk
    }

    /** The "the scan read nothing" guards of one APK. */
    private fun kitRead(which: String, scan: KitApkScan): List<Pair<Boolean, String>> = listOf(
        (scan.productClasses.isNotEmpty() to
            "$which: the scan read ${scan.productClasses.size} class(es) under $KIT_PRODUCT_PREFIX in ${scan.dexCount} dex file(s)"),
        (scan.resTableRead to "$which: resources.arsc ${if (scan.resTableRead) "parses as a resource table" else "was NOT read as a resource table"}"),
    )

    /** Reads [apk]: every `classes*.dex` at the zip root, `resources.arsc`, and the service entry. */
    private fun kitScan(apk: File): KitApkScan = ZipFile(apk).use { zip ->
        val dexEntries = zip.entries().asSequence().filter { KIT_DEX_ENTRY.matches(it.name) }.toList()
        val classDefs = ArrayList<String>()
        val descriptors = ArrayList<String>()
        val passcodeDex = ArrayList<String>()
        for (entry in dexEntries) {
            val dex = kitDexClasses(zip.getInputStream(entry).use { it.readBytes() }, "${apk.name}!${entry.name}")
            descriptors += dex.types
            classDefs += dex.defs
            if (dex.holdsPasscode) passcodeDex += entry.name
        }
        val arsc = zip.getEntry("resources.arsc")?.let { e -> zip.getInputStream(e).use { it.readBytes() } }
        KitApkScan(
            dexCount = dexEntries.size,
            productClasses = classDefs.filter { it.startsWith(KIT_PRODUCT_PREFIX) },
            aidClassDefs = classDefs.filter { it.startsWith(KIT_AID_CLASS_PREFIX) },
            aidDescriptors = descriptors.filter { it.startsWith(KIT_AID_CLASS_PREFIX) },
            resTableRead = arsc != null && kitIsResTable(arsc),
            resUtf8 = arsc != null && kitContains(arsc, KIT_AID_RES_PREFIX.toByteArray(Charsets.UTF_8)),
            resUtf16 = arsc != null && kitContains(arsc, KIT_AID_RES_PREFIX.toByteArray(Charsets.UTF_16LE)),
            serviceEntry = zip.getEntry(KIT_SERVICE_ENTRY) != null,
            textUtf8 = arsc != null && kitContains(arsc, KIT_WRONG_PASSCODE_TEXT.toByteArray(Charsets.UTF_8)),
            textUtf16 = arsc != null && kitContains(arsc, KIT_WRONG_PASSCODE_TEXT.toByteArray(Charsets.UTF_16LE)),
            passcodeDex = passcodeDex,
        )
    }

    /** One dex file: its type table, its class definitions, and whether its string table holds the entry [KIT_PASSCODE]. */
    private data class KitDex(val types: List<String>, val defs: List<String>, val holdsPasscode: Boolean)

    /**
     * The class descriptors of one dex file: every entry of its type table (type_ids → string_ids → string_data)
     * and the subset that class_defs defines; and whether one entry of its string table is exactly
     * [KIT_PASSCODE] (UTF-16 length 4 and those four bytes, then the terminating zero). Header offsets of the dex
     * format: string_ids 56/60, type_ids 64/68, class_defs 96/100; a class_def is 32 bytes and starts with its
     * type index; string data is a ULEB128 UTF-16 length, then MUTF-8 bytes up to a zero byte (class descriptors
     * and the passcode are ASCII). A file that is not a dex fails loudly.
     */
    private fun kitDexClasses(dex: ByteArray, name: String): KitDex {
        if (dex.size < 112 || String(dex, 0, 4, Charsets.ISO_8859_1) != "dex\n") throw AssertionError("$name is not a dex file")
        val b = ByteBuffer.wrap(dex).order(ByteOrder.LITTLE_ENDIAN)
        val stringIdsSize = b.getInt(56)
        val stringIdsOff = b.getInt(60)
        val typeIdsSize = b.getInt(64)
        val typeIdsOff = b.getInt(68)
        val classDefsSize = b.getInt(96)
        val classDefsOff = b.getInt(100)
        fun string(index: Int): String {
            var p = b.getInt(stringIdsOff + 4 * index)
            while (dex[p].toInt() and 0x80 != 0) p++
            p++
            var end = p
            while (dex[end] != 0.toByte()) end++
            return String(dex, p, end - p, Charsets.UTF_8)
        }
        val types = List(typeIdsSize) { string(b.getInt(typeIdsOff + 4 * it)) }
        val defs = List(classDefsSize) { types[b.getInt(classDefsOff + 32 * it)] }
        val passcode = KIT_PASSCODE.toByteArray(Charsets.US_ASCII)
        val holdsPasscode = (0 until stringIdsSize).any { index ->
            val p = b.getInt(stringIdsOff + 4 * index)
            // A UTF-16 length of 4 is the one-byte ULEB128 value 4; then the four bytes and the zero terminator.
            dex[p].toInt() == passcode.size && p + passcode.size + 1 < dex.size &&
                (passcode.indices).all { dex[p + 1 + it] == passcode[it] } && dex[p + 1 + passcode.size] == 0.toByte()
        }
        return KitDex(types, defs, holdsPasscode)
    }

    /** True when [arsc] starts with a RES_TABLE_TYPE chunk whose size is the whole entry and which declares at least one package. */
    private fun kitIsResTable(arsc: ByteArray): Boolean {
        if (arsc.size < 12) return false
        val b = ByteBuffer.wrap(arsc).order(ByteOrder.LITTLE_ENDIAN)
        return (b.getShort(0).toInt() and 0xFFFF) == KIT_RES_TABLE_TYPE && b.getInt(4) == arsc.size && b.getInt(8) >= 1
    }

    /** True when [needle] occurs in [hay] as a byte sequence. */
    private fun kitContains(hay: ByteArray, needle: ByteArray): Boolean {
        if (needle.isEmpty() || hay.size < needle.size) return needle.isEmpty()
        val first = needle[0]
        outer@ for (i in 0..hay.size - needle.size) {
            if (hay[i] != first) continue
            for (j in 1 until needle.size) if (hay[i + j] != needle[j]) continue@outer
            return true
        }
        return false
    }
}
