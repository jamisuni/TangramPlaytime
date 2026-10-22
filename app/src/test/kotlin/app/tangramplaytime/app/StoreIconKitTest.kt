// Acceptance kit (WO-009, TASK-069, stage 3) — REQ-048 A4, the store leg, over files: the store icon, the launcher icon's
// resources and DI-9 (tasks.md § WO-009 S-57; decisions.md 2026-10-22 R7, "The launcher icon and the store icon are made from
// DI-9 by one generator.").
//
// - "Matches the store icon" (REQ-048 A4) is held through DI-9, the one source (S-57: "one source and one generator, so both
//   icons show the same seven pieces on the same background. The kit checks it at points inside each piece and on the
//   background: `LauncherIconKitTest` on the installed app, `StoreIconKitTest` on the files"). Stage 2's LauncherIconKitTest
//   holds the installed icon to DI-9 at the same piece and background points as this file; here:
//   (a) `release/pictures/icon-512.png` is 512 × 512, a 32-bit PNG (RGBA, 8 bits a channel, colour type 6) with every alpha
//       255 (S-57 as amended by decisions.md 2026-10-22 "Amends 2026-10-22 "The launcher icon and the store icon are made from
//       DI-9 by one generator." (R7): the 512 × 512 store icon is a 32-bit PNG (RGBA, colour type 6) with every alpha 255 —
//       opaque, the same pixels."), and at the DI-9 points, mapped through S-57's window ("x and y from 18 to 90 of the
//       108-unit viewBox, scaled by 512 / 72"), its pixels equal DI-9's colours (interior points only: "the edges
//       anti-aliased by a fixed supersampling");
//   (b) the launcher resources are DI-9 path for path and colour for colour: `ic_launcher_foreground.xml` a `<vector>` of
//       108 dp × 108 dp, viewport 108 × 108, holding exactly one `<path>` per SVG path, in order, `pathData` equal to the `d`
//       and `fillColor` to the `fill` ("copied verbatim"); `ic_launcher_background.xml` the colour `ic_launcher_background`
//       equal to DI-9's background; `ic_launcher.xml` an `<adaptive-icon>` of exactly `<background
//       android:drawable="@color/ic_launcher_background"/>` and `<foreground android:drawable="@drawable/ic_launcher_foreground"/>`,
//       no other layer (S-57; "No `<monochrome>` layer");
//   (c) no text (S-57 "No text: DI-9 has none, the tool refuses a `text`, `tspan` or `image` element, and a vector holds only
//       paths"): DI-9's SVG holds only `path` elements, each with a `fill` and a `d`; the vector holds only `path` elements;
//   (d) a pin, no token (S-58: `android:icon="@mipmap/ic_launcher"` on `<application>`; the TASK-069 row): the release APK's
//       merged manifest gives `application` an `icon` that is a resource reference into the app's own package (package id
//       0x7f), not the framework's (0x01).
// - DI-9 (`Spec/icon/option-A-foreground.svg`, `Spec/icon/option-A-background.txt`) is read at run time, so the paths compared
//   are the design input's own; the sample points are the author's, read from those paths and listed below with the path
//   each comes from (the same points as LauncherIconKitTest, and four window corners). Each piece point is the incentre of its
//   polygon (the centre of the square, the middle of the parallelogram), at least 4.08 units from every edge of its piece and
//   inside no other piece; each background point is at least 10.4 units from every piece and inside the 18–90 window. The
//   distances come from the author's own point-in-polygon script over the SVG's numbers (scratch, not in the tree).
// - The PNG is read by this file's own decoder (signature, chunks with their CRC-32, IHDR, the IDAT stream inflated with
//   `java.util.zip.Inflater`, the five scanline filters), so no image library decides what a pixel is. The crafted PNGs are
//   written by this file's own encoder.
// - Crafted fixtures, in this class, each failing its own check (the TASK-069 brief, as amended): a kit-made 32-bit icon that
//   passes (a positive control: DI-9's background with each piece's colour painted around its point, every alpha 255), and
//   the same with a stripe of another colour across a piece, with one alpha below 255, and written as a 24-bit RGB file; a
//   kit-made vector from DI-9's paths that passes, and the same with an eighth path; DI-9's SVG with a `text` element added.
// - The test builds nothing. The release APK comes from run 1 of the JVM channel line and is refused when stale
//   (`ReleasePromiseKitTest`'s pattern and staleness sources); a missing file fails with its path. A red pin is a finding
//   against TASK-067's file (the file, the line and the sentence); the kit never edits the data.
// - Every check is computed and printed (the XML's system-out), then the test fails with every failed check. No `assume…`;
//   nothing is skipped. Helper names carry a `kit` / `Kit` / `KIT_` prefix and are members of this class.
package app.tangramplaytime.app

import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs
import kotlin.math.floor

class StoreIconKitTest {

    private companion object {
        const val KIT_SVG = "Spec/icon/option-A-foreground.svg"
        const val KIT_BACKGROUND_TXT = "Spec/icon/option-A-background.txt"
        const val KIT_STORE_ICON = "release/pictures/icon-512.png"
        const val KIT_FOREGROUND_XML = "app/src/main/res/drawable/ic_launcher_foreground.xml"
        const val KIT_BACKGROUND_XML = "app/src/main/res/values/ic_launcher_background.xml"
        const val KIT_ADAPTIVE_XML = "app/src/main/res/mipmap-anydpi/ic_launcher.xml"
        const val KIT_RELEASE_APK = "app/build/outputs/apk/release/app-release-unsigned.apk"
        const val KIT_BUILD_LINE = ":app:assembleRelease :app:assembleDebug :app:packageRelease --rerun :app:packageDebug --rerun"

        const val KIT_ANDROID_NS = "http://schemas.android.com/apk/res/android"

        /** The application id (architecture.md §5; S-58 "`applicationId` `app.tangramplaytime`, kept"), the manifest's `package`. */
        const val KIT_APP_PACKAGE = "app.tangramplaytime"

        /** S-57: the store icon's side, and the visible window of the 108-unit viewBox it shows. */
        const val KIT_STORE_SIDE = 512
        const val KIT_WINDOW_FROM = 18.0
        const val KIT_WINDOW_TO = 90.0

        /** DI-9's viewBox side (`viewBox="0 0 108 108"`). */
        const val KIT_UNITS = 108.0

        /** S-57's references inside `ic_launcher.xml`. */
        const val KIT_BACKGROUND_REF = "@color/ic_launcher_background"
        const val KIT_FOREGROUND_REF = "@drawable/ic_launcher_foreground"
        const val KIT_COLOUR_NAME = "ic_launcher_background"

        /** The release APK's staleness sources: `ReleasePromiseKitTest`'s list (the 2026-10-18 row's amendment included). */
        val KIT_STALENESS_INPUTS = listOf(
            "app/src/main", "play/src/main", "play/src/debug", "kernel/src/main", "content/src/main",
            "store/src/main", "contracts/src/main", "app/build.gradle.kts", "play/build.gradle.kts",
            "gradle/libs.versions.toml", "settings.gradle.kts",
        )

        /** PNG colour types (the PNG specification, IHDR): 2 is RGB, 6 is RGB with an alpha channel (RGBA). */
        const val KIT_PNG_RGB = 2
        const val KIT_PNG_RGBA = 6

        /** Android binary XML chunk types and value types (ResourceTypes.h), as `ReleasePromiseKitTest` reads them. */
        const val KIT_RES_XML_TYPE = 0x0003
        const val KIT_RES_STRING_POOL_TYPE = 0x0001
        const val KIT_RES_XML_START_ELEMENT_TYPE = 0x0102
        const val KIT_TYPE_REFERENCE = 0x01
        const val KIT_TYPE_STRING = 0x03
        const val KIT_UTF8_FLAG = 1 shl 8

        /** The app's own resources carry package id 0x7f; the framework's 0x01. */
        const val KIT_APP_PACKAGE_ID = 0x7F
    }

    /** One DI-9 sample point (units of the 108 viewBox): what it is, and its colour (0xRRGGBB), or null for the background. */
    private data class KitPoint(val what: String, val x: Double, val y: Double, val fill: String?)

    /**
     * The DI-9 sample points, each from `Spec/icon/option-A-foreground.svg` (the path with that `fill`) or, for the background,
     * `Spec/icon/option-A-background.txt`.
     */
    private val kitPoints: List<KitPoint> = listOf(
        // <path fill="#E8505B" d="M33.32 32.44 L74.68 32.44 L54 53.12 Z"/>: the incentre, 8.56 units in.
        KitPoint("inside path 1 (#E8505B)", 54.0, 41.0, "#E8505B"),
        // <path fill="#3D8BFD" d="M32.44 33.32 L53.12 54 L32.44 74.68 Z"/>: the incentre, 8.56 units in.
        KitPoint("inside path 2 (#3D8BFD)", 41.0, 54.0, "#3D8BFD"),
        // <path fill="#2DBE7E" d="M75.63 33.1 L75.63 52.9 L65.73 43 Z"/>: the incentre, 4.08 units in.
        KitPoint("inside path 3 (#2DBE7E)", 71.5, 43.0, "#2DBE7E"),
        // <path fill="#FFD23F" d="M65 43.88 L75.12 54 L65 64.12 L54.88 54 Z"/>: the centre, 7.16 units in.
        KitPoint("inside path 4 (#FFD23F)", 65.0, 54.0, "#FFD23F"),
        // <path fill="#F9A826" d="M75.56 54.88 L75.56 75.56 L54.88 75.56 Z"/>: the incentre, 6.05 units in.
        KitPoint("inside path 5 (#F9A826)", 69.5, 69.5, "#F9A826"),
        // <path fill="#FF7AB8" d="M33.15 75.61 L53.62 75.61 L63.84 65.39 L43.38 65.39 Z"/>: the middle, 5.11 units in.
        KitPoint("inside path 6 (#FF7AB8)", 48.5, 70.5, "#FF7AB8"),
        // <path fill="#9B5DE5" d="M44.1 64.63 L63.9 64.63 L54 54.73 Z"/>: the incentre, 4.08 units in.
        KitPoint("inside path 7 (#9B5DE5)", 54.0, 60.5, "#9B5DE5"),
        // option-A-background.txt: four points 32 units from (54, 54), at least 10.4 units from every piece …
        KitPoint("the background above the pieces", 54.0, 22.0, null),
        KitPoint("the background below the pieces", 54.0, 86.0, null),
        KitPoint("the background left of the pieces", 22.0, 54.0, null),
        KitPoint("the background right of the pieces", 86.0, 54.0, null),
        // … and the window's four corners, 2 units inside it, at least 12 units from every piece (Play masks the square itself).
        KitPoint("the background at the window's top-left", 20.0, 20.0, null),
        KitPoint("the background at the window's top-right", 88.0, 20.0, null),
        KitPoint("the background at the window's bottom-left", 20.0, 88.0, null),
        KitPoint("the background at the window's bottom-right", 88.0, 88.0, null),
    )

    /** One path of DI-9 or of a vector: its fill and its path data, as written. */
    private data class KitPath(val fill: String, val d: String)

    /**
     * A decoded PNG: its IHDR fields, its chunk types in order, and (8 bits a channel, RGB or RGBA, not interlaced) its pixels as
     * 0xAARRGGBB, an RGB file's alpha read as 255.
     */
    private class KitPng(val width: Int, val height: Int, val bitDepth: Int, val colourType: Int, val interlace: Int, val chunks: List<String>, val argb: IntArray?)

    // ── REQ-048 A4 over the files ──────────────────────────────────────────────────────────────

    // REQ-048.A4 — "The installed app shows a launcher icon that matches the store icon; the icon contains no text." The store
    // icon's side (S-57; R7 as amended): `release/pictures/icon-512.png` is 512 × 512, a 32-bit PNG (RGBA, colour type 6) with
    // every alpha 255, and at every DI-9 point mapped through S-57's window its pixel is exactly DI-9's colour — the piece's
    // `fill`, or the background of option-A-background.txt. Exactly: an interior pixel's supersamples all fall inside one flat
    // fill (S-57: the colours are DI-9's, the edges alone anti-aliased). The installed icon is held to the same points by
    // LauncherIconKitTest.
    @Test
    fun theStoreIconIsDi9sSevenPiecesOnAnOpaque512PxSquare() { // REQ-048.A4
        val checks = mutableListOf<Pair<Boolean, String>>()
        val root = kitProjectRoot()
        val background = kitReadText(root, KIT_BACKGROUND_TXT, checks)?.trim()
        val png = kitReadBytes(root, KIT_STORE_ICON, checks)
        if (background != null && png != null) {
            checks += kitIconChecks(KIT_STORE_ICON, png, kitParseColour(background))
        }
        kitReport("theStoreIconIsDi9sSevenPiecesOnAnOpaque512PxSquare", checks)
    }

    // REQ-048.A4 — the launcher icon's resources are DI-9, path for path and colour for colour (S-57: "the `d` and the `fill`
    // copied verbatim"; "`<color name="ic_launcher_background">#FBF7EE</color>`"; "an `<adaptive-icon>` with exactly
    // `<background …/>` and `<foreground …/>`. No `<monochrome>` layer"), so the installed launcher icon and the store icon show
    // the same seven pieces on the same background (S-57 "The match").
    @Test
    fun theLauncherResourcesAreDi9PathForPathAndColourForColour() { // REQ-048.A4
        val checks = mutableListOf<Pair<Boolean, String>>()
        val root = kitProjectRoot()
        val svg = kitReadText(root, KIT_SVG, checks)
        val background = kitReadText(root, KIT_BACKGROUND_TXT, checks)?.trim()
        val vector = kitReadText(root, KIT_FOREGROUND_XML, checks)
        val colour = kitReadText(root, KIT_BACKGROUND_XML, checks)
        val adaptive = kitReadText(root, KIT_ADAPTIVE_XML, checks)
        if (svg != null && vector != null) {
            val di9 = kitSvgPaths(svg)
            checks += (di9.size == 7) to "DI-9 ($KIT_SVG) holds ${di9.size} path(s) (the seven pieces)"
            checks += kitVectorChecks(KIT_FOREGROUND_XML, vector, di9)
        }
        if (background != null && colour != null) {
            val values = kitParse(colour).documentElement
            val named = kitChildren(values).filter { it.tagName == "color" && it.getAttribute("name") == KIT_COLOUR_NAME }
            val value = named.singleOrNull()?.textContent?.trim()
            checks += (named.size == 1 && value != null && kitParseColour(value) == kitParseColour(background)) to
                "$KIT_BACKGROUND_XML: ${named.size} <color name=\"$KIT_COLOUR_NAME\">, value $value (DI-9's background $background)"
        }
        if (adaptive != null) {
            val icon = kitParse(adaptive).documentElement
            val layers = kitChildren(icon)
            checks += (icon.tagName == "adaptive-icon") to "$KIT_ADAPTIVE_XML: the root is <${icon.tagName}> (expected <adaptive-icon>)"
            checks += (layers.map { it.tagName }.sorted() == listOf("background", "foreground")) to
                "$KIT_ADAPTIVE_XML: its layers are ${layers.map { it.tagName }} (expected exactly a background and a foreground, no other layer)"
            val bg = layers.firstOrNull { it.tagName == "background" }?.getAttributeNS(KIT_ANDROID_NS, "drawable")
            val fg = layers.firstOrNull { it.tagName == "foreground" }?.getAttributeNS(KIT_ANDROID_NS, "drawable")
            checks += (bg == KIT_BACKGROUND_REF) to "$KIT_ADAPTIVE_XML: the background is \"$bg\" (expected \"$KIT_BACKGROUND_REF\")"
            checks += (fg == KIT_FOREGROUND_REF) to "$KIT_ADAPTIVE_XML: the foreground is \"$fg\" (expected \"$KIT_FOREGROUND_REF\")"
        }
        kitReport("theLauncherResourcesAreDi9PathForPathAndColourForColour", checks)
    }

    // REQ-048.A4 — "the icon contains no text." DI-9's SVG holds only `path` elements, each with a `fill` and a `d` (no `text`,
    // `tspan`, `image` or font element: S-57 "No text"), and the launcher vector holds only `path` elements ("a vector holds
    // only paths"); the store icon is drawn from the same paths (S-57).
    @Test
    fun theIconHoldsNoText() { // REQ-048.A4
        val checks = mutableListOf<Pair<Boolean, String>>()
        val root = kitProjectRoot()
        kitReadText(root, KIT_SVG, checks)?.let { checks += kitSvgOnlyPaths(KIT_SVG, it) }
        kitReadText(root, KIT_FOREGROUND_XML, checks)?.let { checks += kitVectorOnlyPaths(KIT_FOREGROUND_XML, it) }
        kitReport("theIconHoldsNoText", checks)
    }

    // Pin, no token — S-58: `android:icon="@mipmap/ic_launcher"` on `<application>`; the TASK-069 row: "the release manifest's
    // `application` icon a reference into the app's own package, not the platform's". The release APK's merged manifest (binary
    // XML, read as ReleasePromiseKitTest reads it) has one `application` element whose `icon` is a resource reference (value
    // type 0x01) with package id 0x7f. Liveness: the `manifest` element names the package `app.tangramplaytime`.
    @Test
    fun theReleaseManifestGivesTheApplicationAnIconOfItsOwnPackage() {
        val checks = mutableListOf<Pair<Boolean, String>>()
        val apk = kitJudgeableApk(checks)
        if (apk != null) {
            val manifest = ZipFile(apk).use { zip -> zip.getEntry("AndroidManifest.xml")?.let { e -> zip.getInputStream(e).use { it.readBytes() } } }
            if (manifest == null) {
                checks += false to "no AndroidManifest.xml in $KIT_RELEASE_APK"
            } else {
                val elements = kitManifestElements(manifest)
                val pkg = elements.firstOrNull { it.name == "manifest" }?.attributes?.get("package")?.raw
                checks += (pkg == KIT_APP_PACKAGE) to "liveness: the manifest's package is \"$pkg\" (expected \"$KIT_APP_PACKAGE\")"
                val apps = elements.filter { it.name == "application" }
                val icon = apps.singleOrNull()?.attributes?.get("icon")
                checks += (apps.size == 1 && icon != null && icon.type == KIT_TYPE_REFERENCE && (icon.data ushr 24) == KIT_APP_PACKAGE_ID) to
                    "the release manifest's application icon: ${apps.size} application element(s), icon " +
                    (icon?.let { "type 0x%02x data 0x%08x".format(it.type, it.data) } ?: "absent") + " (expected a reference 0x7fxxxxxx, the app's own)"
            }
        }
        kitReport("theReleaseManifestGivesTheApplicationAnIconOfItsOwnPackage", checks)
    }

    // ── Discrimination: crafted fixtures (no token) ─────────────────────────────────────────────

    // Pin, no token: the checks above can fail (the TASK-069 brief, as amended: "crafted fixtures, each failing its check: a
    // PNG with a stripe of another colour across a piece; a vector with an eighth path; an SVG with a `text` element"; "a
    // fixture with one alpha below 255, and one 24-bit RGB file, each fail"). Each is a kit-made text or picture that passes
    // the check, or the same with one defect that fails it, and only that check:
    // - a kit-made 512 × 512 32-bit icon (DI-9's background, each piece's colour painted 9 × 9 px around its mapped point, every
    //   alpha 255) passes (a); with an 8 px stripe of #FF00FF across the #E8505B piece's point, (a) fails at that point alone;
    //   with the alpha of one pixel (0, 0) at 254, (a) fails its alpha check alone; written as a 24-bit RGB file (colour type 2,
    //   the same pixels), (a) fails its format check alone;
    // - a kit-made vector of DI-9's seven paths (S-57's shape) passes (b) and (c); with an eighth path, (b) fails;
    // - DI-9's SVG with a `<text>` element added fails (c); DI-9's SVG itself passes it (theIconHoldsNoText).
    @Test
    fun everyCraftedIconFixtureFailsItsCheck() {
        val checks = mutableListOf<Pair<Boolean, String>>()
        val root = kitProjectRoot()
        val svg = kitReadText(root, KIT_SVG, checks)
        val backgroundText = kitReadText(root, KIT_BACKGROUND_TXT, checks)?.trim()
        if (svg != null && backgroundText != null) {
            val background = kitParseColour(backgroundText)
            val good = kitMadeIcon(background)
            val goodChecks = kitIconChecks("the kit-made icon", good, background)
            checks += goodChecks.all { it.first } to "the kit-made 32-bit icon passes (a): ${goodChecks.filter { !it.first }.map { it.second }}"
            val red = kitPoints.first()
            val striped = kitIconChecks("the striped icon", kitMadeIcon(background, stripeAt = red), background).filter { !it.first }
            checks += (striped.size == 1 && red.what in striped.single().second) to
                "the icon with a stripe across ${red.what} fails (a) there and nowhere else: ${striped.map { it.second }}"
            val translucent = kitIconChecks("the icon with one alpha of 254", kitMadeIcon(background, alpha254At = 0), background).filter { !it.first }
            checks += (translucent.size == 1 && "alpha" in translucent.single().second) to
                "the icon with one alpha below 255 fails (a)'s alpha check and nothing else: ${translucent.map { it.second }}"
            val rgb24 = kitIconChecks("the 24-bit RGB icon", kitMadeIcon(background, colourType = KIT_PNG_RGB), background).filter { !it.first }
            checks += (rgb24.size == 1 && "colour type $KIT_PNG_RGB" in rgb24.single().second) to
                "the icon written as 24-bit RGB fails (a)'s format check and nothing else: ${rgb24.map { it.second }}"

            val di9 = kitSvgPaths(svg)
            val vector = kitMadeVector(di9, extra = null)
            val vectorChecks = kitVectorChecks("the kit-made vector", vector, di9) + kitVectorOnlyPaths("the kit-made vector", vector)
            checks += vectorChecks.all { it.first } to "the kit-made vector of DI-9's paths passes (b) and (c): ${vectorChecks.filter { !it.first }.map { it.second }}"
            val eighth = kitMadeVector(di9, extra = KitPath("#000000", "M0 0 L1 0 L1 1 Z"))
            val eighthChecks = kitVectorChecks("the vector with an eighth path", eighth, di9)
            checks += eighthChecks.any { !it.first } to "the vector with an eighth path fails (b): ${eighthChecks.filter { !it.first }.map { it.second }}"

            val withText = svg.replace("</svg>", "  <text x=\"40\" y=\"100\" fill=\"#000000\">Tangram</text>\n</svg>")
            checks += (withText != svg) to "fixture: the SVG with a text element differs from DI-9's"
            val textChecks = kitSvgOnlyPaths("DI-9's SVG with a text element", withText)
            checks += textChecks.any { !it.first } to "DI-9's SVG with a text element fails (c): ${textChecks.filter { !it.first }.map { it.second }}"
        }
        kitReport("everyCraftedIconFixtureFailsItsCheck", checks)
    }

    // ── Checks (shared by the files and the fixtures) ───────────────────────────────────────────

    /**
     * (a): [png] is 512 × 512, a 32-bit PNG (8 bits a channel, colour type 6) with every alpha 255; at each DI-9 point through
     * S-57's window, exactly DI-9's colour.
     */
    private fun kitIconChecks(what: String, png: ByteArray, background: Int): List<Pair<Boolean, String>> {
        val checks = mutableListOf<Pair<Boolean, String>>()
        val p = try {
            kitReadPng(png)
        } catch (e: IllegalArgumentException) {
            checks += false to "$what is not a PNG the kit can read: ${e.message}"
            return checks
        }
        checks += (p.width == KIT_STORE_SIDE && p.height == KIT_STORE_SIDE) to "$what is ${p.width} × ${p.height} (expected $KIT_STORE_SIDE × $KIT_STORE_SIDE, S-57)"
        checks += (p.bitDepth == 8 && p.colourType == KIT_PNG_RGBA) to
            "$what: bit depth ${p.bitDepth}, colour type ${p.colourType} (expected a 32-bit PNG: 8 bits a channel, colour type $KIT_PNG_RGBA, R7 as amended)"
        val argb = p.argb
        if (argb == null || p.width != KIT_STORE_SIDE || p.height != KIT_STORE_SIDE) {
            checks += false to "$what: no pixels read (the kit reads 8 bits a channel, RGB or RGBA, not interlaced; interlace ${p.interlace})"
            return checks
        }
        val below = argb.indices.filter { (argb[it] ushr 24) != 0xFF }
        checks += below.isEmpty() to
            "$what: ${below.size} pixel(s) with an alpha below 255 (expected every alpha 255: opaque, R7 as amended)" +
            (below.firstOrNull()?.let { " — first at (${it % p.width}, ${it / p.width}), alpha ${argb[it] ushr 24}" } ?: "")
        val scale = KIT_STORE_SIDE / (KIT_WINDOW_TO - KIT_WINDOW_FROM)
        for (pt in kitPoints) {
            val px = floor((pt.x - KIT_WINDOW_FROM) * scale).toInt()
            val py = floor((pt.y - KIT_WINDOW_FROM) * scale).toInt()
            val want = pt.fill?.let { kitParseColour(it) } ?: background
            val got = argb[py * p.width + px] and 0xFFFFFF
            checks += (got == want) to "$what at ${pt.what} (${pt.x}, ${pt.y}) = px ($px, $py): #%06X (expected #%06X)".format(got, want)
        }
        return checks
    }

    /** (b): [xml] is a 108 dp × 108 dp vector, viewport 108 × 108, whose `path` elements are [di9], in order, verbatim. */
    private fun kitVectorChecks(what: String, xml: String, di9: List<KitPath>): List<Pair<Boolean, String>> {
        val checks = mutableListOf<Pair<Boolean, String>>()
        val vector = kitParse(xml).documentElement
        checks += (vector.tagName == "vector") to "$what: the root is <${vector.tagName}> (expected <vector>)"
        val w = vector.getAttributeNS(KIT_ANDROID_NS, "width")
        val h = vector.getAttributeNS(KIT_ANDROID_NS, "height")
        val vw = vector.getAttributeNS(KIT_ANDROID_NS, "viewportWidth")
        val vh = vector.getAttributeNS(KIT_ANDROID_NS, "viewportHeight")
        checks += (kitDp(w) == KIT_UNITS && kitDp(h) == KIT_UNITS) to "$what: width $w, height $h (expected 108dp × 108dp)"
        checks += (vw.toDoubleOrNull() == KIT_UNITS && vh.toDoubleOrNull() == KIT_UNITS) to "$what: viewport $vw × $vh (expected 108 × 108)"
        val paths = kitChildren(vector).filter { it.tagName == "path" }
            .map { KitPath(it.getAttributeNS(KIT_ANDROID_NS, "fillColor"), it.getAttributeNS(KIT_ANDROID_NS, "pathData")) }
        checks += (paths.size == di9.size) to "$what holds ${paths.size} path(s) (DI-9: ${di9.size})"
        for (i in di9.indices) {
            val got = paths.getOrNull(i)
            checks += (got == di9[i]) to "$what path ${i + 1}: fillColor ${got?.fill}, pathData \"${got?.d}\" (DI-9: fill ${di9[i].fill}, d \"${di9[i].d}\")"
        }
        if (paths.size > di9.size) checks += false to "$what: ${paths.size - di9.size} path(s) DI-9 does not have: ${paths.drop(di9.size)}"
        return checks
    }

    /** (c), DI-9's side: every element under the `svg` root is a `path` with a `fill` and a `d`. */
    private fun kitSvgOnlyPaths(what: String, svg: String): List<Pair<Boolean, String>> {
        val root = kitParse(svg).documentElement
        val all = kitDescendants(root)
        val others = all.filter { it.localName != "path" }.map { it.localName ?: it.tagName }
        val bare = all.filter { it.localName == "path" && (!it.hasAttribute("fill") || !it.hasAttribute("d")) }.size
        return listOf(
            (root.localName == "svg") to "$what: the root is <${root.localName}>",
            others.isEmpty() to "$what: elements other than path under the root: $others (no text, tspan, image or font: S-57 \"No text\")",
            (bare == 0) to "$what: $bare path(s) without a fill and a d",
            (all.isNotEmpty()) to "$what: ${all.size} element(s) read under the root (liveness)",
        )
    }

    /** (c), the vector's side: every element under the `vector` root is a `path` ("a vector holds only paths"). */
    private fun kitVectorOnlyPaths(what: String, xml: String): List<Pair<Boolean, String>> {
        val root = kitParse(xml).documentElement
        val others = kitDescendants(root).filter { it.tagName != "path" }.map { it.tagName }
        return listOf(others.isEmpty() to "$what: elements other than path under <${root.tagName}>: $others")
    }

    // ── DI-9 and XML ─────────────────────────────────────────────────────────────────────────────

    /** DI-9's paths in order: each `path` element's `fill` and `d`, as written. */
    private fun kitSvgPaths(svg: String): List<KitPath> =
        kitDescendants(kitParse(svg).documentElement).filter { it.localName == "path" }.map { KitPath(it.getAttribute("fill"), it.getAttribute("d")) }

    /** A namespace-aware parse with no DTD and no external entity. */
    private fun kitParse(xml: String): Document {
        val f = DocumentBuilderFactory.newInstance()
        f.isNamespaceAware = true
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        f.isExpandEntityReferences = false
        return f.newDocumentBuilder().parse(xml.byteInputStream(Charsets.UTF_8))
    }

    /** The element children of [e]. */
    private fun kitChildren(e: Element): List<Element> =
        (0 until e.childNodes.length).map { e.childNodes.item(it) }.filter { it.nodeType == Node.ELEMENT_NODE }.map { it as Element }

    /** Every element below [e], depth first. */
    private fun kitDescendants(e: Element): List<Element> = kitChildren(e).flatMap { listOf(it) + kitDescendants(it) }

    /** "108dp" → 108.0; anything else → null. */
    private fun kitDp(v: String): Double? = if (v.endsWith("dp")) v.removeSuffix("dp").toDoubleOrNull() else null

    /** "#RRGGBB" (either case) → 0xRRGGBB; anything else fails loudly. */
    private fun kitParseColour(hex: String): Int {
        val t = hex.trim()
        if (!Regex("^#[0-9A-Fa-f]{6}$").matches(t)) throw AssertionError("kit: \"$t\" is not a #RRGGBB colour")
        return t.substring(1).toInt(16)
    }

    // ── Fixture makers ───────────────────────────────────────────────────────────────────────────

    /**
     * A kit-made 512 × 512 PNG of [colourType] (6 by default: 32-bit RGBA): [background] everywhere, each piece's `fill`
     * painted 9 × 9 px around its mapped point, every alpha 255; with [stripeAt], an 8 px horizontal stripe of #FF00FF across
     * the whole width through that point's row; with [alpha254At], that pixel index's alpha 254.
     */
    private fun kitMadeIcon(background: Int, stripeAt: KitPoint? = null, alpha254At: Int? = null, colourType: Int = KIT_PNG_RGBA): ByteArray {
        val side = KIT_STORE_SIDE
        val opaque = 0xFF shl 24
        val argb = IntArray(side * side) { opaque or background }
        val scale = side / (KIT_WINDOW_TO - KIT_WINDOW_FROM)
        for (pt in kitPoints) {
            val fill = pt.fill ?: continue
            val cx = floor((pt.x - KIT_WINDOW_FROM) * scale).toInt()
            val cy = floor((pt.y - KIT_WINDOW_FROM) * scale).toInt()
            for (y in cy - 4..cy + 4) for (x in cx - 4..cx + 4) argb[y * side + x] = opaque or kitParseColour(fill)
        }
        if (stripeAt != null) {
            val cy = floor((stripeAt.y - KIT_WINDOW_FROM) * scale).toInt()
            for (y in cy - 4 until cy + 4) for (x in 0 until side) argb[y * side + x] = opaque or 0xFF00FF
        }
        if (alpha254At != null) argb[alpha254At] = (254 shl 24) or (argb[alpha254At] and 0xFFFFFF)
        return kitWritePng(side, side, argb, colourType)
    }

    /** A kit-made vector in S-57's shape: 108 dp, viewport 108, one path per [paths] entry (plus [extra]). */
    private fun kitMadeVector(paths: List<KitPath>, extra: KitPath?): String {
        val all = paths + listOfNotNull(extra)
        return buildString {
            append("<vector xmlns:android=\"$KIT_ANDROID_NS\" android:width=\"108dp\" android:height=\"108dp\" android:viewportWidth=\"108\" android:viewportHeight=\"108\">\n")
            for (p in all) append("    <path android:fillColor=\"${p.fill}\" android:pathData=\"${p.d}\" />\n")
            append("</vector>\n")
        }
    }

    // ── PNG ──────────────────────────────────────────────────────────────────────────────────────

    /**
     * Decodes a PNG: chunks with their CRC-32 checked, IHDR, and — 8 bits a channel, RGB (2) or RGBA (6), not interlaced — the
     * pixels through the five scanline filters, as 0xAARRGGBB (an RGB file's alpha 255).
     */
    private fun kitReadPng(bytes: ByteArray): KitPng {
        val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        require(bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(signature)) { "no PNG signature" }
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        var off = 8
        val types = ArrayList<String>()
        var ihdr: ByteArray? = null
        val idat = ByteArrayOutputStream()
        while (off + 12 <= bytes.size) {
            val len = b.getInt(off)
            require(len >= 0 && off + 12 + len <= bytes.size) { "a chunk at byte $off runs past the end" }
            val type = String(bytes, off + 4, 4, Charsets.US_ASCII)
            val crc = CRC32().apply { update(bytes, off + 4, 4 + len) }.value.toInt()
            require(crc == b.getInt(off + 8 + len)) { "the $type chunk at byte $off fails its CRC-32" }
            val data = bytes.copyOfRange(off + 8, off + 8 + len)
            types += type
            when (type) {
                "IHDR" -> ihdr = data
                "IDAT" -> idat.write(data)
            }
            off += 12 + len
            if (type == "IEND") break
        }
        val h = ihdr ?: throw IllegalArgumentException("no IHDR chunk")
        require(types.firstOrNull() == "IHDR" && types.lastOrNull() == "IEND") { "chunks $types do not run from IHDR to IEND" }
        val hb = ByteBuffer.wrap(h).order(ByteOrder.BIG_ENDIAN)
        val width = hb.getInt(0)
        val height = hb.getInt(4)
        val depth = h[8].toInt() and 0xFF
        val colourType = h[9].toInt() and 0xFF
        val interlace = h[12].toInt() and 0xFF
        if (depth != 8 || (colourType != KIT_PNG_RGB && colourType != KIT_PNG_RGBA) || interlace != 0 || width <= 0 || height <= 0) {
            return KitPng(width, height, depth, colourType, interlace, types, null)
        }
        val bpp = if (colourType == KIT_PNG_RGBA) 4 else 3
        val stride = width * bpp
        val raw = ByteArray(height * (stride + 1))
        val inflater = Inflater()
        inflater.setInput(idat.toByteArray())
        var n = 0
        while (n < raw.size && !inflater.finished()) {
            val got = inflater.inflate(raw, n, raw.size - n)
            if (got == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
            n += got
        }
        inflater.end()
        require(n == raw.size) { "the image data inflates to $n bytes, not ${raw.size}" }
        val out = IntArray(width * height)
        val prev = ByteArray(stride)
        val cur = ByteArray(stride)
        for (y in 0 until height) {
            val filter = raw[y * (stride + 1)].toInt() and 0xFF
            System.arraycopy(raw, y * (stride + 1) + 1, cur, 0, stride)
            for (i in 0 until stride) {
                val a = if (i >= bpp) cur[i - bpp].toInt() and 0xFF else 0
                val up = prev[i].toInt() and 0xFF
                val c = if (i >= bpp) prev[i - bpp].toInt() and 0xFF else 0
                val x = cur[i].toInt() and 0xFF
                val v = when (filter) {
                    0 -> x
                    1 -> x + a
                    2 -> x + up
                    3 -> x + (a + up) / 2
                    4 -> {
                        val pp = a + up - c
                        val pa = abs(pp - a)
                        val pb = abs(pp - up)
                        val pc = abs(pp - c)
                        x + if (pa <= pb && pa <= pc) a else if (pb <= pc) up else c
                    }
                    else -> throw IllegalArgumentException("row $y has the unknown filter $filter")
                }
                cur[i] = v.toByte()
            }
            for (px in 0 until width) {
                val o = px * bpp
                val alpha = if (bpp == 4) cur[o + 3].toInt() and 0xFF else 0xFF
                out[y * width + px] = (alpha shl 24) or ((cur[o].toInt() and 0xFF) shl 16) or ((cur[o + 1].toInt() and 0xFF) shl 8) or (cur[o + 2].toInt() and 0xFF)
            }
            System.arraycopy(cur, 0, prev, 0, stride)
        }
        return KitPng(width, height, depth, colourType, interlace, types, out)
    }

    /**
     * Encodes [argb] (0xAARRGGBB, row by row) as a PNG of 8 bits a channel, [colourType] 6 (RGBA) or 2 (RGB, the alpha dropped),
     * every row with filter 0.
     */
    private fun kitWritePng(width: Int, height: Int, argb: IntArray, colourType: Int): ByteArray {
        require(colourType == KIT_PNG_RGBA || colourType == KIT_PNG_RGB) { "kit: the encoder writes colour type 2 or 6" }
        val raw = ByteArrayOutputStream()
        for (y in 0 until height) {
            raw.write(0)
            for (x in 0 until width) {
                val c = argb[y * width + x]
                raw.write((c shr 16) and 0xFF)
                raw.write((c shr 8) and 0xFF)
                raw.write(c and 0xFF)
                if (colourType == KIT_PNG_RGBA) raw.write((c ushr 24) and 0xFF)
            }
        }
        val deflater = Deflater()
        deflater.setInput(raw.toByteArray())
        deflater.finish()
        val z = ByteArrayOutputStream()
        val buf = ByteArray(65536)
        while (!deflater.finished()) z.write(buf, 0, deflater.deflate(buf))
        deflater.end()
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        fun chunk(type: String, data: ByteArray) {
            val head = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putInt(data.size).put(type.toByteArray(Charsets.US_ASCII)).array()
            out.write(head)
            out.write(data)
            val crc = CRC32().apply { update(head, 4, 4); update(data) }.value.toInt()
            out.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(crc).array())
        }
        chunk("IHDR", ByteBuffer.allocate(13).order(ByteOrder.BIG_ENDIAN).putInt(width).putInt(height).put(8).put(colourType.toByte()).put(0).put(0).put(0).array())
        chunk("IDAT", z.toByteArray())
        chunk("IEND", ByteArray(0))
        return out.toByteArray()
    }

    // ── Files, the release APK and its manifest ──────────────────────────────────────────────────

    /** Prints every check, then fails with every failed one. */
    private fun kitReport(test: String, checks: List<Pair<Boolean, String>>) {
        for ((ok, text) in checks) println("StoreIconKitTest ${if (ok) "PASS" else "FAIL"} $test: $text")
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

    /** The bytes of [path] under [root], or null with a failed check naming the missing file. */
    private fun kitReadBytes(root: File, path: String, checks: MutableList<Pair<Boolean, String>>): ByteArray? {
        val f = File(root, path)
        if (!f.isFile) {
            checks += false to "$path does not exist under $root"
            return null
        }
        return f.readBytes()
    }

    /** The UTF-8 text of [path] under [root], or null with a failed check naming the missing file. */
    private fun kitReadText(root: File, path: String, checks: MutableList<Pair<Boolean, String>>): String? =
        kitReadBytes(root, path, checks)?.let { String(it, Charsets.UTF_8) }

    /** The release APK when it exists and is not older than the newest staleness source; otherwise null, with a failed check. */
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

    /** One attribute of a binary-XML start element: its raw string (or the pool string of a string value), its value type and data. */
    private data class KitAttr(val raw: String?, val type: Int, val data: Int)

    /** One start element of the binary manifest: its name and its attributes by name. */
    private data class KitElement(val name: String, val attributes: Map<String, KitAttr>)

    /**
     * The start elements of an Android binary XML file, read as `ReleasePromiseKitTest.kitManifestElements` reads them (read,
     * not called): a RES_XML_TYPE chunk whose size is the whole file, a string pool, and start-element chunks; each attribute
     * is ns, name, rawValue, then Res_value (size, res0, dataType, data). A file that does not parse fails loudly.
     */
    private fun kitManifestElements(bytes: ByteArray): List<KitElement> {
        if (bytes.size < 8) throw AssertionError("AndroidManifest.xml is ${bytes.size} bytes: not binary XML")
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun u16(at: Int) = b.getShort(at).toInt() and 0xFFFF
        if (u16(0) != KIT_RES_XML_TYPE || b.getInt(4) != bytes.size) throw AssertionError("AndroidManifest.xml is not binary XML")
        var pool: List<String>? = null
        val out = ArrayList<KitElement>()
        var off = u16(2)
        while (off + 8 <= bytes.size) {
            val type = u16(off)
            val header = u16(off + 2)
            val size = b.getInt(off + 4)
            if (size < 8 || off + size > bytes.size) throw AssertionError("AndroidManifest.xml: a chunk at byte $off has size $size")
            when (type) {
                KIT_RES_STRING_POOL_TYPE -> pool = kitStringPool(b, bytes, off, header)
                KIT_RES_XML_START_ELEMENT_TYPE -> {
                    val strings = pool ?: throw AssertionError("AndroidManifest.xml: a start element before the string pool")
                    fun str(index: Int): String = strings.getOrNull(index) ?: throw AssertionError("AndroidManifest.xml: string index $index outside the pool")
                    val ext = off + header
                    val name = str(b.getInt(ext + 4))
                    val attrStart = u16(ext + 8)
                    val attrSize = u16(ext + 10)
                    val attrCount = u16(ext + 12)
                    val attributes = LinkedHashMap<String, KitAttr>()
                    for (i in 0 until attrCount) {
                        val a = ext + attrStart + i * attrSize
                        val rawIndex = b.getInt(a + 8)
                        val dataType = bytes[a + 15].toInt() and 0xFF
                        val data = b.getInt(a + 16)
                        val raw = when {
                            rawIndex != -1 -> str(rawIndex)
                            dataType == KIT_TYPE_STRING -> str(data)
                            else -> null
                        }
                        attributes[str(b.getInt(a + 4))] = KitAttr(raw, dataType, data)
                    }
                    out += KitElement(name, attributes)
                }
            }
            off += size
        }
        if (pool == null) throw AssertionError("AndroidManifest.xml: no string pool")
        return out
    }

    /** A ResStringPool chunk at [off] (header size [header]), UTF-8 or UTF-16, as `ReleasePromiseKitTest.kitStringPool` reads it. */
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
}
