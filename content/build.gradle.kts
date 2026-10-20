// :content — puzzle assets, the tangram-puzzle/1 parser, validity check and ordering
// (IPuzzleLibrary implementation; architecture.md §3, ADR-004: parsed with kotlinx.serialization).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.tangramplaytime.content"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
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
 */
abstract class BundlePuzzles : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val puzzleFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun bundle() {
        val root = outputDir.get().asFile
        root.deleteRecursively() // a puzzle removed from Tangrams/ must not linger in the bundle
        val dir = File(root, "app/tangramplaytime/content/puzzles") // = BundledPuzzles.DIR
        dir.mkdirs()
        val files = puzzleFiles.files.sortedBy { it.name }
        files.forEach { it.copyTo(File(dir, it.name), overwrite = true) }
        File(dir, "index.txt").writeText(files.joinToString("") { it.name + "\n" }, Charsets.UTF_8)
    }
}

// Inputs: the top-level *.json of Tangrams/ only — never the schema, README.md or previews/ (G-05).
val bundlePuzzles = tasks.register<BundlePuzzles>("bundlePuzzles") {
    puzzleFiles.from(
        fileTree("../Tangrams") {
            include("*.json")
            exclude("*.schema.json")
        },
    )
    outputDir.set(layout.buildDirectory.dir("generated/puzzles"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.resources?.addGeneratedSourceDirectory(bundlePuzzles, BundlePuzzles::outputDir)
    }
}

// AssetLicencesKitTest (R5's pin) reads ../ASSET-LICENCES.md at run time: an edit of the record alone must re-run the tests.
tasks.withType<Test>().configureEach {
    inputs.file(file("../ASSET-LICENCES.md"))
}
