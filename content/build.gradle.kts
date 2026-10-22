// :content — puzzle assets, the tangram-puzzle/1 parser, validity check and ordering
// (IPuzzleLibrary implementation; architecture.md §3, ADR-004: parsed with kotlinx.serialization).
import java.nio.ByteBuffer

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.tangramplaytime.content"
    compileSdk = 37

    defaultConfig {
        minSdk = 31
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":contracts"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    // The real lock engine (WO-001) for the load-time check in tests; main code depends on
    // :contracts only and gets the engine injected (G-12; designs/WO-002-design.md §2).
    testImplementation(project(":kernel"))
}

/**
 * Ships the puzzle files as Java (classpath) resources (decisions.md 2026-10-11, "Java resources";
 * designs/WO-002-design.md §2 `bundlePuzzles`). Copies every input file, plus an `index.txt` of the
 * sorted file names, under `app/tangramplaytime/content/puzzles` — the same folder as
 * `BundledPuzzles.DIR`, the one reader. The AAR `classes.jar`, the APK root and the unit-test
 * classpath all carry it.
 *
 * Two registrations (tasks.md § WO-009 S-55; decisions.md 2026-10-22 "The release ships only the puzzles the owner
 * marked; the debug build keeps every puzzle."): the debug bundle copies every input file, as before; the release
 * bundle ([markedOnly]) only the files whose `provenance.reviewedByHuman` is JSON `true` — a missing `provenance` or
 * field reads `false` (decisions.md 2026-10-11). The inputs are hashed by content, so a mark rebuilds the bundle.
 */
abstract class BundlePuzzles : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val puzzleFiles: ConfigurableFileCollection

    /** True for the release bundle: only the marked files ship (S-55). */
    @get:Input
    abstract val markedOnly: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun bundle() {
        val root = outputDir.get().asFile
        root.deleteRecursively() // a puzzle removed from Tangrams/ must not linger in the bundle
        val dir = File(root, "app/tangramplaytime/content/puzzles") // = BundledPuzzles.DIR
        dir.mkdirs()
        val all = puzzleFiles.files.sortedBy { it.name }
        val files = if (!markedOnly.get()) all else all.filter { file ->
            val puzzle = readPuzzle(file)
            // As `python tools/mark_puzzles.py status` counts it: a file that is not a UTF-8 JSON object is unmarked.
            if (puzzle == null) logger.warn("warning: ${file.name} is not a UTF-8 JSON object; left out of the release bundle")
            isMarked(puzzle)
        }
        files.forEach { it.copyTo(File(dir, it.name), overwrite = true) }
        File(dir, "index.txt").writeText(files.joinToString("") { it.name + "\n" }, Charsets.UTF_8)
    }

    companion object {
        /** The file as a JSON object, read with Gradle's own JSON parser (no new dependency), or null if it is not one. */
        fun readPuzzle(file: File): Map<*, *>? = try {
            val text = Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(file.readBytes())).toString()
            groovy.json.JsonSlurper().parseText(text) as? Map<*, *>
        } catch (e: Exception) {
            null
        }

        /** `provenance.reviewedByHuman` is JSON `true`; anything else, a missing field included, reads `false`. */
        fun isMarked(puzzle: Map<*, *>?): Boolean = (puzzle?.get("provenance") as? Map<*, *>)?.get("reviewedByHuman") == true
    }
}

// Inputs: the top-level *.json of Tangrams/ only — never the schema, README.md or previews/ (G-05).
fun puzzleFolderFiles() = fileTree("../Tangrams") {
    include("*.json")
    exclude("*.schema.json")
}

// The debug bundle: every puzzle file, as before WO-009 (every build type but `release`; every kit reads it).
val bundlePuzzles = tasks.register<BundlePuzzles>("bundlePuzzles") {
    puzzleFiles.from(puzzleFolderFiles())
    markedOnly.set(false)
    outputDir.set(layout.buildDirectory.dir("generated/puzzles"))
}

// The release bundle: the marked files only (S-55). Before the owner's marks it is empty, so the release APK fails at
// start with "no puzzle file to list" (decisions.md 2026-10-11 "A bundled puzzle that fails a rule fails fast.").
val bundleReleasePuzzles = tasks.register<BundlePuzzles>("bundleReleasePuzzles") {
    puzzleFiles.from(puzzleFolderFiles())
    markedOnly.set(true)
    outputDir.set(layout.buildDirectory.dir("generated/releasePuzzles"))
}

// The `release` build type takes the release bundle; every other build type, and so every unit-test and device-test
// classpath, the debug bundle (S-55).
androidComponents {
    onVariants { variant ->
        val bundle = if (variant.buildType == "release") bundleReleasePuzzles else bundlePuzzles
        variant.sources.resources?.addGeneratedSourceDirectory(bundle, BundlePuzzles::outputDir)
    }
}

/**
 * The release-readiness gate (tasks.md § WO-009 S-56; decisions.md 2026-10-22 "The release-readiness bar, and the store
 * build refuses below it."). Reads the release bundle and fails unless all five items of the bar hold, naming every
 * unmet item with its count; prints the same lines when it passes. The words are those of
 * `python tools/mark_puzzles.py status` (S-54). Only `:app`'s store artifact (the AAB) waits on it.
 */
abstract class CheckReleasePuzzles : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val releaseBundle: DirectoryProperty

    @TaskAction
    fun check() {
        val dir = File(releaseBundle.get().asFile, "app/tangramplaytime/content/puzzles") // = BundledPuzzles.DIR
        val names = File(dir, "index.txt").readLines(Charsets.UTF_8).filter { it.isNotBlank() }
        val marked = names.map { it.removeSuffix(".json") to BundlePuzzles.readPuzzle(File(dir, it)) }
            .filter { BundlePuzzles.isMarked(it.second) }
            .map { it.first to it.second!! }

        val categories = marked.mapNotNull { it.second["category"] as? String }.distinct()
            .sortedWith(compareBy({ CATEGORIES.indexOf(it).let { i -> if (i < 0) CATEGORIES.size else i } }, { it }))
        val minis = marked.filter { (_, p) -> kindOf(p) == "mini" && (p["solution"] as? List<*>)?.size == MINI_PIECES && isEasiest(p) }
            .map { it.first }.sorted()
        val warmups = marked.count { (_, p) -> kindOf(p) == "warmup" && isEasiest(p) }
        val square = marked.any { it.first == SQUARE }
        val items = listOf(
            Triple("marked puzzles", "${marked.size} of $PUZZLES", marked.size >= PUZZLES),
            Triple(
                "categories with a marked puzzle",
                "${categories.size} of $CATEGORY_COUNT (${if (categories.isEmpty()) "none" else categories.joinToString(", ")})",
                categories.size >= CATEGORY_COUNT,
            ),
            Triple(SQUARE, if (square) "marked" else "not marked", square),
            Triple(
                "a marked mini with exactly three solution entries and difficulty 1",
                if (minis.isEmpty()) "none" else minis.joinToString(", "),
                minis.isNotEmpty(),
            ),
            Triple("marked warmup puzzles of difficulty 1", "$warmups of $WARMUPS", warmups >= WARMUPS),
        )
        val unmet = items.filter { !it.third }.map { "${it.first}: ${it.second}" }
        val lines = listOf("release bar (the release needs all five):") +
            items.mapIndexed { n, (label, value, _) -> "  ${n + 1}. $label: $value" } +
            (if (unmet.isEmpty()) "release-ready: yes" else "release-ready: no — " + unmet.joinToString("; "))
        if (unmet.isNotEmpty()) {
            throw GradleException(
                (listOf("The store build stops here: the release set does not meet the release bar.") + lines +
                    "See what is marked and what the bar still needs: python tools/mark_puzzles.py status").joinToString("\n"),
            )
        }
        lines.forEach { logger.lifecycle(it) }
    }

    private fun kindOf(p: Map<*, *>): Any? = if (p.containsKey("kind")) p["kind"] else "full"

    /** `difficulty` 1 — "rating 1" in REQ-045 and REQ-041 (Spec/03-puzzle-format.md §2). */
    private fun isEasiest(p: Map<*, *>): Boolean = (p["difficulty"] as? Number)?.toDouble() == 1.0

    private companion object {
        /** Spec/03-puzzle-format.md §2, the `category` row, in its order (as `mark_puzzles.py` names them). */
        val CATEGORIES = listOf("shapes", "animals", "people", "things", "vehicles", "nature", "letters", "numbers")
        const val PUZZLES = 20 // REQ-042 A1 and rule 1
        const val CATEGORY_COUNT = 4 // REQ-042 A2 and rule 2
        const val SQUARE = "shapes-square" // REQ-042 A3, as locked
        const val MINI_PIECES = 3 // REQ-045 statement; rule 3: rating 1
        const val WARMUPS = 4 // REQ-041 statement: at least four warm-ups of rating 1
    }
}

tasks.register<CheckReleasePuzzles>("checkReleasePuzzles") {
    description = "Fails unless the release set meets the release bar (S-56); :app:bundleRelease depends on it."
    releaseBundle.set(bundleReleasePuzzles.flatMap { it.outputDir })
    // Code review S1: in the store build the gate runs after `:app` has deleted an earlier AAB, so a refusal leaves none.
    mustRunAfter(":app:deleteReleaseBundle")
}

// AssetLicencesKitTest (R5's pin) reads ../ASSET-LICENCES.md at run time: an edit of the record alone must re-run the tests.
tasks.withType<Test>().configureEach {
    inputs.file(file("../ASSET-LICENCES.md"))
}
