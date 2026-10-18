// Acceptance kit (WO-006, TASK-027) — the promise lines on the release APK of `:app` (decisions.md 2026-10-17
// "The promise lines' oracles", part 1: "Release APK (`app/src/test`, the `ReleaseBuildKitTest` pattern): no
// `uses-permission` in the merged manifest carries REQ-010 A2's token; a closed allow-list of the dex's top-level
// packages … is pinned untokened for G-01 / REQ-008's 'no ad, billing or donation component', backed by a self-test
// that must fail").
//
// - Two tests. `theReleaseApkDeclaresNoPermission` carries the one acceptance token of this file, on the manifest
//   assertion (the TASK-027 row). `everyReleaseClassLiesUnderAnAllowedTopLevelPackage` is a pin (guardrail G-01;
//   REQ-008 rule 1; architecture.md §5 "Allowed dependencies"), no token: a package list is a proxy for "no ad,
//   billing or donation component", not that line's meaning (traceability-rules.md §2: "A token is a coverage claim").
//   REQ-008's own lines, and the device leg of REQ-010 A2, are TASK-030's (the play-through and the installed app).
// - The manifest is read as Android binary XML: the start-element chunks, each name and attribute resolved through
//   the file's string pool (UTF-8 or UTF-16). Liveness: the scan must read the package `app.tangramplaytime` on the
//   `manifest` element and at least one `activity` element, or the test fails — a scan that read nothing cannot pass.
// - The dex scan reads the class definitions (class_defs → type_ids → string_ids) of every `classes*.dex` at the zip
//   root, as `play/src/test/.../ReleaseBuildKitTest.kt` does (read, not called). Liveness: at least one class under
//   `app/tangramplaytime/`. Self-test, in the same test: the same check with `kotlinx/` removed from the list must
//   fail on the same APK ("a self-test that must fail"), so a check that cannot fail cannot pass; and a made-up
//   `androidx/ads/` class definition added to the APK's classes must land outside the list.
// - Recirculated 2026-10-18 (corrected check 13): decisions.md 2026-10-18 "S1 recorded and carried, S2 fixed now, N1
//   carried, N2 to the owner's play list, N3 on the G4 surface." amends the Q8 row's measured list — the bare
//   `androidx/` is replaced by the `androidx/` subpackages measured in that day's release APK, each by name; the
//   self-test also injects a made-up `androidx/ads/` class and expects it outside; `gradle/libs.versions.toml` and
//   `settings.gradle.kts` join the staleness inputs (reviews/WO-006-code-review.md S2). The other entries stay.
// - The release APK only: the debug APK carries debug-only classes (the DEV aid, the canary, the test manifest) that
//   are not shipped (G-08), and it is not read here.
// - The test builds nothing. It refuses an APK older than the newest file of the TASK-027 row's staleness sources and
//   fails on a missing APK with the command that builds it (platform-notes/android.md: "A harness that installs build
//   outputs refuses stale ones"; "A test that reads build outputs runs after them, in a separate Gradle run"). The
//   project root is the nearest ancestor of the working directory that holds `settings.gradle.kts`.
// - Every check is computed and printed (the XML's system-out) before the test fails with every failed check, so one
//   run shows every leg. No `assume…`; nothing is skipped.
// - When the allow-list pin fails on a new dependency, the answer is a D5 record and a `decisions.md` row before the
//   list grows (the Q8 row; platform-notes/android.md "re-judge the inventory and log a decision row before extending
//   it") — never an edit of this list alone.
// - Helper names carry a `kit` / `KIT_` prefix and are members of this class.
package app.tangramplaytime.app

import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.util.zip.ZipFile

class ReleasePromiseKitTest {

    private companion object {
        const val KIT_RELEASE_APK = "app/build/outputs/apk/release/app-release-unsigned.apk"

        /** Run 1 of the JVM channel line (workorders/WO-006-briefs.md § Verification channels), which builds it. */
        const val KIT_BUILD_LINE = ":app:assembleRelease :app:assembleDebug :app:packageRelease --rerun :app:packageDebug --rerun"

        /**
         * The TASK-027 row's staleness sources, plus the two the 2026-10-18 row adds ("the staleness inputs miss
         * `gradle/libs.versions.toml` and `settings.gradle.kts`"): the APK must not be older than the newest file
         * under any of them.
         */
        val KIT_STALENESS_INPUTS = listOf(
            "app/src/main", "play/src/main", "play/src/debug", "kernel/src/main", "content/src/main",
            "store/src/main", "contracts/src/main", "app/build.gradle.kts", "play/build.gradle.kts",
            "gradle/libs.versions.toml", "settings.gradle.kts",
        )

        /** The application id (architecture.md §5 "Application id: `app.tangramplaytime`"), the manifest's `package`. */
        const val KIT_PACKAGE = "app.tangramplaytime"

        /**
         * The manifest elements by which an app requests or declares a permission. REQ-010 A2: "The installed app
         * declares no permissions, not even network access"; G-01: "The merged manifest declares no
         * `<uses-permission>`". `uses-permission-sdk-23` (and its older spelling `uses-permission-sdk-m`) requests a
         * permission on API 23 and later; `permission`, `permission-group` and `permission-tree` declare one (the
         * Q8 row's device leg reads the same token as "no requested or declared permission").
         */
        val KIT_PERMISSION_ELEMENTS = listOf(
            "uses-permission", "uses-permission-sdk-23", "uses-permission-sdk-m",
            "permission", "permission-group", "permission-tree",
        )

        /**
         * The `androidx/` subpackages of the release dex, each by name — decisions.md 2026-10-18 (S2): "`androidx/`
         * is replaced by the `androidx/` subpackages measured in today's release APK (each listed by name; a new one
         * is a D5 record and a row before the list grows, as Q8 says)".
         *
         * Measured 2026-10-18 18:36 on `app-release-unsigned.apk` from run 1 of the JVM line (product tree
         * 83abecd0d723): every class definition of `classes.dex` and `classes2.dex` under `androidx/`, grouped by its
         * second segment (`androidx/<name>/`) — read by an independent dex reader and by this test's own INFO line
         * "androidx/ subpackages measured", which agree; no class lies directly in `androidx/`. Each one is placed in
         * an artifact of `:app`'s `releaseRuntimeClasspath` (AndroidX + Compose (BOM), architecture.md §5);
         * `androidx/inspection/` is the one class `androidx.inspection.compose.ui.ProguardDetection`, shipped inside
         * `androidx.compose.ui:ui-android`.
         */
        val KIT_ALLOWED_ANDROIDX = listOf(
            "androidx/activity/", "androidx/annotation/", "androidx/arch/", "androidx/autofill/",
            "androidx/collection/", "androidx/compose/", "androidx/concurrent/", "androidx/core/",
            "androidx/customview/", "androidx/emoji2/", "androidx/graphics/", "androidx/inspection/",
            "androidx/interpolator/", "androidx/lifecycle/", "androidx/navigationevent/", "androidx/profileinstaller/",
            "androidx/savedstate/", "androidx/startup/", "androidx/tracing/", "androidx/versionedparcelable/",
            "androidx/window/",
        )

        /**
         * The Q8 row's closed allow-list of the release dex's packages, "measured 2026-10-17", as class descriptor
         * prefixes (a descriptor `Lkotlin/Unit;` lies under `kotlin/`; `kotlinx/` is not under `kotlin/`), with its
         * bare `androidx/` replaced by [KIT_ALLOWED_ANDROIDX] (the 2026-10-18 row); the other entries stay, among them
         * `com/google/common/util/concurrent/` ("stays (one package)").
         */
        val KIT_ALLOWED_PACKAGES = listOf(
            "_COROUTINE/", "android/support/v4/", "app/tangramplaytime/",
            "com/google/common/util/concurrent/", "kotlin/", "kotlinx/", "org/intellij/lang/annotations/",
            "org/jetbrains/annotations/", "org/jspecify/annotations/",
        ) + KIT_ALLOWED_ANDROIDX

        /** The entry the self-test removes (the brief: "the same check with `kotlinx/` removed from the list must fail"). */
        const val KIT_SELF_TEST_REMOVED = "kotlinx/"

        /**
         * The made-up class definition the self-test injects (the 2026-10-18 row: "the self-test also injects a
         * made-up `androidx/ads/` class and expects it outside"). The class name is made up; its package is the real
         * family the code review names (`androidx.ads.identifier`, reviews/WO-006-code-review.md S2).
         */
        const val KIT_SELF_TEST_INJECTED = "Landroidx/ads/KitMadeUpAdClass;"

        /** The product's own classes: the "the scan read nothing" guard of the dex scan. */
        const val KIT_PRODUCT_PACKAGE = "app/tangramplaytime/"

        val KIT_DEX_ENTRY = Regex("""classes\d*\.dex""")

        /** Android binary XML chunk types (ResourceTypes.h): the file, the string pool, a start element. */
        const val KIT_RES_XML_TYPE = 0x0003
        const val KIT_RES_STRING_POOL_TYPE = 0x0001
        const val KIT_RES_XML_START_ELEMENT_TYPE = 0x0102

        /** `Res_value::TYPE_STRING`: the value's data is a string-pool index. */
        const val KIT_TYPE_STRING = 0x03

        /** `ResStringPool_header::UTF8_FLAG`. */
        const val KIT_UTF8_FLAG = 1 shl 8
    }

    /** One start element of the binary manifest: its name and its attributes by name. */
    private data class KitElement(val name: String, val attributes: Map<String, String>)

    // REQ-010.A2 — "The installed app declares no permissions, not even network access." The release APK's merged
    // manifest (app-release-unsigned.apk, AndroidManifest.xml in binary XML) holds no `uses-permission` and no
    // `uses-permission-sdk-23` element — the Q8 row: "no `uses-permission` in the merged manifest carries [this line's]
    // token" — and, for "declares", no `permission`, `permission-group` or `permission-tree` element (the same
    // token's device leg reads "no requested or declared permission"). Liveness: the scan reads the package
    // `app.tangramplaytime` and at least one `activity` element. A stale or missing APK fails, never passes.
    @Test
    fun theReleaseApkDeclaresNoPermission() { // REQ-010.A2
        val checks = mutableListOf<Pair<Boolean, String>>()
        val apk = kitJudgeableApk(checks)
        if (apk != null) {
            val manifest = ZipFile(apk).use { zip ->
                zip.getEntry("AndroidManifest.xml")?.let { e -> zip.getInputStream(e).use { it.readBytes() } }
            }
            if (manifest == null) {
                checks += (false to "release: no AndroidManifest.xml entry in $KIT_RELEASE_APK")
            } else {
                val elements = kitManifestElements(manifest)
                val roots = elements.filter { it.name == "manifest" }
                val pkg = roots.firstOrNull()?.attributes?.get("package")
                checks += ((roots.size == 1 && pkg == KIT_PACKAGE) to
                    "liveness: ${elements.size} start element(s) read; ${roots.size} `manifest` element(s), package \"$pkg\" (expected \"$KIT_PACKAGE\")")
                val activities = elements.filter { it.name == "activity" }.map { it.attributes["name"] ?: "(no name)" }
                checks += (activities.isNotEmpty() to "liveness: ${activities.size} `activity` element(s) $activities")
                for (forbidden in KIT_PERMISSION_ELEMENTS) {
                    val found = elements.filter { it.name == forbidden }.map { it.attributes["name"] ?: "(no name)" }
                    checks += (found.isEmpty() to
                        "release manifest: ${found.size} `$forbidden` element(s)${if (found.isEmpty()) "" else " FOUND $found"}")
                }
                println("ReleasePromiseKitTest INFO manifest elements: ${elements.groupingBy { it.name }.eachCount()}")
            }
        }
        kitReport("theReleaseApkDeclaresNoPermission", checks)
    }

    // Pin, no token — guardrail G-01 ("no dependency on an ad, billing, analytics, crash-reporting or network
    // library"), REQ-008 rule 1 ("No ad, billing or donation component is part of the game"), architecture.md §5
    // ("Allowed dependencies: AndroidX + Compose (BOM), `kotlinx.serialization` …, `kotlinx.coroutines` … Anything
    // else is a D5 record in the WO"), decisions.md 2026-10-17 "The promise lines' oracles" (the closed allow-list;
    // "an allow-list is chosen over the orchestrator's denylist lean because a denylist is never complete"), as
    // amended by decisions.md 2026-10-18 "S1 recorded and carried, S2 fixed now, …" (the `androidx/` subpackages by
    // name). Every class definition of the release `classes*.dex` lies under one of the allowed packages; at least one
    // class under `app/tangramplaytime/` is read; and the self-test: with `kotlinx/` removed from the list, the same
    // check reports at least one class outside it, on the same APK — so the check can fail; and with a made-up
    // `androidx/ads/` class definition added to the APK's classes, the same check reports that class outside the list
    // — so a bare `androidx/` (which admits it) cannot pass. The method keeps its name (run-log and XML continuity).
    @Test
    fun everyReleaseClassLiesUnderAnAllowedTopLevelPackage() {
        val checks = mutableListOf<Pair<Boolean, String>>()
        val apk = kitJudgeableApk(checks)
        if (apk != null) {
            val (dexCount, classes) = kitReleaseClassDefs(apk)
            checks += ((dexCount > 0) to "release: $dexCount classes*.dex file(s), ${classes.size} class definition(s)")
            val product = classes.count { it.removePrefix("L").startsWith(KIT_PRODUCT_PACKAGE) }
            checks += ((product > 0) to "liveness: $product class definition(s) under $KIT_PRODUCT_PACKAGE")

            val outside = kitOutside(classes, KIT_ALLOWED_PACKAGES)
            checks += (outside.isEmpty() to
                "allow-list (${KIT_ALLOWED_PACKAGES.size} packages, ${KIT_ALLOWED_ANDROIDX.size} of them under androidx/): " +
                "${outside.size} class definition(s) outside it" +
                if (outside.isEmpty()) "" else ", first ${outside.take(20)}; top-level of those: ${kitTopLevels(outside)}" +
                    "; androidx/ subpackages of those: ${kitAndroidxSubpackages(outside)}")

            val reduced = KIT_ALLOWED_PACKAGES - KIT_SELF_TEST_REMOVED
            val selfTest = kitOutside(classes, reduced)
            checks += (selfTest.isNotEmpty() to
                "self-test: with $KIT_SELF_TEST_REMOVED removed, the same check finds ${selfTest.size} class definition(s) outside the list" +
                if (selfTest.isEmpty()) " — it CANNOT FAIL on this APK" else " (it fails, as it must), first ${selfTest.take(3)}")

            val injected = kitOutside(classes + KIT_SELF_TEST_INJECTED, KIT_ALLOWED_PACKAGES)
            checks += ((KIT_SELF_TEST_INJECTED in injected) to
                "self-test: with the made-up $KIT_SELF_TEST_INJECTED added to the APK's ${classes.size} class definitions, " +
                "the same check finds ${injected.size} outside the list" +
                if (KIT_SELF_TEST_INJECTED in injected) ", the injected one among them (it lands outside, as it must)"
                else " — the injected androidx/ads/ class is INSIDE the list: an androidx/ ad family would pass")

            val perPackage = KIT_ALLOWED_PACKAGES.associateWith { p -> classes.count { it.removePrefix("L").startsWith(p) } }
            println("ReleasePromiseKitTest INFO class definitions per allowed package: $perPackage")
            println("ReleasePromiseKitTest INFO measured top-level packages (first segment): ${kitTopLevels(classes)}")
            println("ReleasePromiseKitTest INFO androidx/ subpackages measured (second segment): ${kitAndroidxSubpackages(classes)}")
            val unused = perPackage.filterValues { it == 0 }.keys
            if (unused.isNotEmpty()) println("ReleasePromiseKitTest INFO allowed packages with no class in this APK: $unused")
        }
        kitReport("everyReleaseClassLiesUnderAnAllowedTopLevelPackage", checks)
    }

    // ── Kit helpers ──────────────────────────────────────────────────────────────────────────────

    /** Prints every check, then fails with every failed one. */
    private fun kitReport(test: String, checks: List<Pair<Boolean, String>>) {
        for ((ok, text) in checks) println("ReleasePromiseKitTest ${if (ok) "PASS" else "FAIL"} $test: $text")
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
     * The release APK when it exists and is not older than the newest file of [KIT_STALENESS_INPUTS]; otherwise
     * null, with a failed check saying why. A missing staleness source fails too: staleness cannot be judged.
     */
    private fun kitJudgeableApk(checks: MutableList<Pair<Boolean, String>>): File? {
        val root = kitProjectRoot()
        var newest: File? = null
        for (path in KIT_STALENESS_INPUTS) {
            val f = File(root, path)
            if (!f.exists()) {
                checks += (false to "cannot judge staleness: $path does not exist under $root")
                return null
            }
            for (file in f.walkTopDown().filter { it.isFile }) {
                if (newest == null || file.lastModified() > newest.lastModified()) newest = file
            }
        }
        val newestFile = newest ?: run {
            checks += (false to "cannot judge staleness: no file under $KIT_STALENESS_INPUTS")
            return null
        }
        val apk = File(root, KIT_RELEASE_APK)
        if (!apk.isFile) {
            checks += (false to "$KIT_RELEASE_APK not found — build it with run 1 of the JVM channel line ($KIT_BUILD_LINE)")
            return null
        }
        val newestText = "${newestFile.relativeTo(root).invariantSeparatorsPath} (${Instant.ofEpochMilli(newestFile.lastModified())})"
        if (apk.lastModified() < newestFile.lastModified()) {
            checks += (false to "$KIT_RELEASE_APK (${Instant.ofEpochMilli(apk.lastModified())}) is STALE: older than $newestText — refused, not judged; rebuild with $KIT_BUILD_LINE")
            return null
        }
        checks += (true to "$KIT_RELEASE_APK (${Instant.ofEpochMilli(apk.lastModified())}) is not older than the newest staleness source, $newestText")
        return apk
    }

    /** The class definitions of [classes] (descriptors) under none of [allowed]. */
    private fun kitOutside(classes: List<String>, allowed: List<String>): List<String> =
        classes.filter { d -> val body = d.removePrefix("L"); allowed.none { body.startsWith(it) } }

    /** The first package segment of each descriptor, with counts ("(default package)" for a class with no package). */
    private fun kitTopLevels(classes: List<String>): Map<String, Int> =
        classes.groupingBy { d ->
            val body = d.removePrefix("L").removeSuffix(";")
            if ('/' in body) body.substringBefore('/') + "/" else "(default package)"
        }.eachCount().toSortedMap()

    /**
     * The descriptors of [classes] under `androidx/`, grouped by their second segment (`androidx/<name>/`), with
     * counts; a class directly in `androidx/` is counted as "androidx/ (no subpackage)". The measurement the
     * [KIT_ALLOWED_ANDROIDX] list is read from.
     */
    private fun kitAndroidxSubpackages(classes: List<String>): Map<String, Int> =
        classes.map { it.removePrefix("L").removeSuffix(";") }.filter { it.startsWith("androidx/") }.groupingBy { body ->
            val rest = body.removePrefix("androidx/")
            if ('/' in rest) "androidx/" + rest.substringBefore('/') + "/" else "androidx/ (no subpackage)"
        }.eachCount().toSortedMap()

    /**
     * The start elements of an Android binary XML file: a RES_XML_TYPE chunk whose size is the whole file, holding a
     * string pool and the tree's chunks. Each start element (ResXMLTree_node, then ResXMLTree_attrExt: ns, name,
     * attributeStart, attributeSize, attributeCount …) gives its name and its attributes (ResXMLTree_attribute: ns,
     * name, rawValue, then Res_value: size, res0, dataType, data); a value is its raw string, or the pool string a
     * TYPE_STRING value points at, or the typed data as hex. A file that does not parse fails loudly.
     */
    private fun kitManifestElements(bytes: ByteArray): List<KitElement> {
        if (bytes.size < 8) throw AssertionError("AndroidManifest.xml is ${bytes.size} bytes: not binary XML")
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun u16(at: Int) = b.getShort(at).toInt() and 0xFFFF
        val fileType = u16(0)
        val fileHeader = u16(2)
        if (fileType != KIT_RES_XML_TYPE || b.getInt(4) != bytes.size) {
            throw AssertionError("AndroidManifest.xml is not binary XML: chunk type 0x%04x, size %d of %d bytes".format(fileType, b.getInt(4), bytes.size))
        }
        var pool: List<String>? = null
        val out = ArrayList<KitElement>()
        var off = fileHeader
        while (off + 8 <= bytes.size) {
            val type = u16(off)
            val header = u16(off + 2)
            val size = b.getInt(off + 4)
            if (size < 8 || off + size > bytes.size) throw AssertionError("AndroidManifest.xml: a chunk at byte $off has size $size")
            when (type) {
                KIT_RES_STRING_POOL_TYPE -> pool = kitStringPool(b, bytes, off, header)
                KIT_RES_XML_START_ELEMENT_TYPE -> {
                    val strings = pool ?: throw AssertionError("AndroidManifest.xml: a start element before the string pool")
                    fun str(index: Int): String = strings.getOrNull(index)
                        ?: throw AssertionError("AndroidManifest.xml: string index $index outside the pool of ${strings.size}")
                    val ext = off + header
                    val name = str(b.getInt(ext + 4))
                    val attrStart = u16(ext + 8)
                    val attrSize = u16(ext + 10)
                    val attrCount = u16(ext + 12)
                    val attributes = LinkedHashMap<String, String>()
                    for (i in 0 until attrCount) {
                        val a = ext + attrStart + i * attrSize
                        val attrName = str(b.getInt(a + 4))
                        val raw = b.getInt(a + 8)
                        val dataType = bytes[a + 15].toInt() and 0xFF
                        val data = b.getInt(a + 16)
                        attributes[attrName] = when {
                            raw != -1 -> str(raw)
                            dataType == KIT_TYPE_STRING -> str(data)
                            else -> "0x%08x (type 0x%02x)".format(data, dataType)
                        }
                    }
                    out += KitElement(name, attributes)
                }
            }
            off += size
        }
        if (pool == null) throw AssertionError("AndroidManifest.xml: no string pool")
        return out
    }

    /**
     * A ResStringPool chunk at [off] (header size [header]): stringCount at +8, flags at +16, stringsStart at +20,
     * then the u32 string offsets. UTF-8 strings: the UTF-16 length (1 or 2 bytes), the byte length (1 or 2 bytes),
     * the bytes. UTF-16 strings: the length in u16 units (1 or 2 units), then the units.
     */
    private fun kitStringPool(b: ByteBuffer, bytes: ByteArray, off: Int, header: Int): List<String> {
        val count = b.getInt(off + 8)
        val utf8 = b.getInt(off + 16) and KIT_UTF8_FLAG != 0
        val stringsStart = b.getInt(off + 20)
        return List(count) { i ->
            var p = off + stringsStart + b.getInt(off + header + 4 * i)
            if (utf8) {
                p += if (bytes[p].toInt() and 0x80 != 0) 2 else 1
                var n = bytes[p].toInt() and 0xFF
                if (n and 0x80 != 0) {
                    n = ((n and 0x7F) shl 8) or (bytes[p + 1].toInt() and 0xFF)
                    p += 2
                } else {
                    p += 1
                }
                String(bytes, p, n, Charsets.UTF_8)
            } else {
                var n = b.getShort(p).toInt() and 0xFFFF
                if (n and 0x8000 != 0) {
                    n = ((n and 0x7FFF) shl 16) or (b.getShort(p + 2).toInt() and 0xFFFF)
                    p += 4
                } else {
                    p += 2
                }
                String(bytes, p, 2 * n, Charsets.UTF_16LE)
            }
        }
    }

    /**
     * The class definitions of every `classes*.dex` at the zip root of [apk], as descriptors (`Lkotlin/Unit;`), and
     * the number of dex files. Dex header offsets: string_ids 56/60, type_ids 64/68, class_defs 96/100; a class_def is
     * 32 bytes and starts with its type index; string data is a ULEB128 UTF-16 length, then MUTF-8 bytes up to a
     * zero byte. A file that is not a dex fails loudly.
     */
    private fun kitReleaseClassDefs(apk: File): Pair<Int, List<String>> = ZipFile(apk).use { zip ->
        val entries = zip.entries().asSequence().filter { KIT_DEX_ENTRY.matches(it.name) }.toList()
        val defs = ArrayList<String>()
        for (entry in entries) {
            val dex = zip.getInputStream(entry).use { it.readBytes() }
            val name = "${apk.name}!${entry.name}"
            if (dex.size < 112 || String(dex, 0, 4, Charsets.ISO_8859_1) != "dex\n") throw AssertionError("$name is not a dex file")
            val b = ByteBuffer.wrap(dex).order(ByteOrder.LITTLE_ENDIAN)
            val stringIdsOff = b.getInt(60)
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
            for (k in 0 until classDefsSize) {
                val typeIndex = b.getInt(classDefsOff + 32 * k)
                defs += string(b.getInt(typeIdsOff + 4 * typeIndex))
            }
        }
        entries.size to defs
    }
}
