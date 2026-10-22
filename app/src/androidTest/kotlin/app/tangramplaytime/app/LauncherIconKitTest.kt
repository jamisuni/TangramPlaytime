// Acceptance kit (WO-009, TASK-066, stage 2) — REQ-048 A4, the installed app's leg: the launcher icon of the installed app is
// DI-9's icon (tasks.md § WO-009 S-57; decisions.md 2026-10-22 R7, "The launcher icon and the store icon are made from DI-9 by
// one generator.").
//
// - "Matches the store icon" (REQ-048 A4): S-57 makes both icons from one source by one generator ("one source and one
//   generator, so both icons show the same seven pieces on the same background. The kit checks it at points inside each
//   piece and on the background: `LauncherIconKitTest` on the installed app, `StoreIconKitTest` on the files"). This leg holds
//   the installed icon to DI-9 at the sample points below; stage 3's `StoreIconKitTest` holds the store PNG to the same DI-9
//   points, and "no text" is that file leg's too (the TASK-066 brief).
// - DI-9, the reference (design input, "used as is"): `Spec/icon/option-A-foreground.svg` (viewBox `0 0 108 108`, seven `path`
//   elements, each a filled polygon) and `Spec/icon/option-A-background.txt` (`#FBF7EE`). The sample points are the author's,
//   read from those paths (the brief: "a point well inside each of the seven pieces; background points inside the adaptive
//   icon's visible window and outside every piece"): each piece point is the incentre of its polygon (or the centre of the
//   square, the middle of the parallelogram), at least 4.08 units from every edge of its own piece and inside no other; each
//   background point lies 32 units from the centre (54, 54) — inside the 72-unit visible window (units 18 to 90; S-57) and
//   inside the inscribed circle of radius 36, so inside any launcher mask — and at least 10.4 units from every piece. The
//   distances were measured by the author's own point-in-polygon script over the SVG's numbers (scratch, not in the tree).
// - How it is drawn: the icon's two layers (`AdaptiveIconDrawable.getBackground()`, `getForeground()`), each over the whole
//   108-unit square of a 432 × 432 px ARGB_8888 software bitmap (4 px per unit), the background first. The full layers, not
//   the masked icon: the mask is the launcher's and the device's (a circle, a squircle), the layers are the app's. A DI-9 point
//   (x, y) is the pixel (⌊4x⌋, ⌊4y⌋). Every sample point lies inside the drawn square.
// - Tolerance: each channel within 8 of 255 of the DI-9 colour, alpha 255. Interior pixels of a flat fill carry no
//   anti-aliasing, but the foreground vector is rasterised into its own cache bitmap and drawn through bitmap filtering, and a
//   colour may pass a colour-space conversion on the way (the device's colour handling); 8 / 255 covers that rounding. It
//   cannot confuse two DI-9 colours: every pair of the eight (the seven fills and the background) differs by at least 42 / 255
//   in some channel (#FFD23F against #F9A826, the closest pair; the author's computation over the eight hex values).
// - The positive control, in the same test: the platform's default activity icon (`PackageManager.getDefaultActivityIcon()`,
//   what a resolve falls back to when an app names no icon) drawn the same way fails the same comparison. On today's tree the
//   app names no icon, so this test fails at run time until TASK-067 lands (the in-situ fixture; tasks.md § WO-009 "Red
//   windows"). It names no resource TASK-067 adds (seam note 3): the icon is reached through `PackageManager` only.
// - The launcher activity: `PackageManager.queryIntentActivities` of ACTION_MAIN / CATEGORY_LAUNCHER for the app's package
//   resolves exactly one activity, `MainActivity` (S-62: `app.tangramplaytime/app.tangramplaytime.app.MainActivity`); its icon
//   is loaded as a launcher loads a resolved activity's icon (`ResolveInfo.loadIcon`: the filter's icon, else the activity's,
//   else the application's, else the platform default) and its icon resource (`ResolveInfo.getIconResource`) is a resource of
//   the app's own package, never `android.R.drawable.sym_def_app_icon` (S-58: `android:icon="@mipmap/ic_launcher"` on
//   `<application>`).
// - This class launches nothing and changes nothing on the device or in the app's controller (tasks.md § WO-006 seam note 3).
//   Every check is computed and printed (logcat, tag `LauncherIconKitTest`) before the test fails with every failed check.
//   No `assume…`; nothing is skipped. Helper names carry a `kit` / `Kit` / `KIT_` prefix and are members of this class.
package app.tangramplaytime.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.floor

@RunWith(AndroidJUnit4::class)
class LauncherIconKitTest {

    private companion object {
        /** S-62: the launcher activity's class. */
        const val KIT_MAIN_ACTIVITY = "app.tangramplaytime.app.MainActivity"

        /** DI-9's viewBox side (`viewBox="0 0 108 108"`): the adaptive icon's layer square, in units. */
        const val KIT_UNITS = 108

        /** Pixels per DI-9 unit of the drawn square. */
        const val KIT_PX_PER_UNIT = 4

        /** The drawn square's side, px. */
        const val KIT_SIZE = KIT_UNITS * KIT_PX_PER_UNIT

        /** Per-channel tolerance, of 255 (see the header). */
        const val KIT_TOL = 8

        /** `Spec/icon/option-A-background.txt`: `#FBF7EE`. */
        const val KIT_BACKGROUND = 0xFBF7EE

        const val KIT_LOG = "LauncherIconKitTest"
    }

    /** One DI-9 sample point: where it lies (units of the 108 viewBox), what it is, and its colour (0xRRGGBB). */
    private data class KitSample(val what: String, val x: Double, val y: Double, val rgb: Int)

    /**
     * The DI-9 sample points, each read from `Spec/icon/option-A-foreground.svg` (the path, by its `fill`) or
     * `Spec/icon/option-A-background.txt`.
     */
    private val kitSamples: List<KitSample> = listOf(
        // <path fill="#E8505B" d="M33.32 32.44 L74.68 32.44 L54 53.12 Z"/>: the incentre (54, 41.0), 8.56 units in.
        KitSample("the #E8505B piece (path 1, M33.32 32.44 L74.68 32.44 L54 53.12)", 54.0, 41.0, 0xE8505B),
        // <path fill="#3D8BFD" d="M32.44 33.32 L53.12 54 L32.44 74.68 Z"/>: the incentre (41.0, 54), 8.56 units in.
        KitSample("the #3D8BFD piece (path 2, M32.44 33.32 L53.12 54 L32.44 74.68)", 41.0, 54.0, 0x3D8BFD),
        // <path fill="#2DBE7E" d="M75.63 33.1 L75.63 52.9 L65.73 43 Z"/>: the incentre (71.5, 43), 4.08 units in.
        KitSample("the #2DBE7E piece (path 3, M75.63 33.1 L75.63 52.9 L65.73 43)", 71.5, 43.0, 0x2DBE7E),
        // <path fill="#FFD23F" d="M65 43.88 L75.12 54 L65 64.12 L54.88 54 Z"/>: the square's centre (65, 54), 7.16 units in.
        KitSample("the #FFD23F piece (path 4, M65 43.88 L75.12 54 L65 64.12 L54.88 54)", 65.0, 54.0, 0xFFD23F),
        // <path fill="#F9A826" d="M75.56 54.88 L75.56 75.56 L54.88 75.56 Z"/>: the incentre (69.5, 69.5), 6.05 units in.
        KitSample("the #F9A826 piece (path 5, M75.56 54.88 L75.56 75.56 L54.88 75.56)", 69.5, 69.5, 0xF9A826),
        // <path fill="#FF7AB8" d="M33.15 75.61 L53.62 75.61 L63.84 65.39 L43.38 65.39 Z"/>: the middle (48.5, 70.5), 5.11 units in.
        KitSample("the #FF7AB8 piece (path 6, M33.15 75.61 L53.62 75.61 L63.84 65.39 L43.38 65.39)", 48.5, 70.5, 0xFF7AB8),
        // <path fill="#9B5DE5" d="M44.1 64.63 L63.9 64.63 L54 54.73 Z"/>: the incentre (54, 60.5), 4.08 units in.
        KitSample("the #9B5DE5 piece (path 7, M44.1 64.63 L63.9 64.63 L54 54.73)", 54.0, 60.5, 0x9B5DE5),
        // option-A-background.txt #FBF7EE, at four points 32 units from (54, 54), each at least 10.4 units from every piece.
        KitSample("the background above the pieces", 54.0, 22.0, KIT_BACKGROUND),
        KitSample("the background below the pieces", 54.0, 86.0, KIT_BACKGROUND),
        KitSample("the background left of the pieces", 22.0, 54.0, KIT_BACKGROUND),
        KitSample("the background right of the pieces", 86.0, 54.0, KIT_BACKGROUND),
    )

    // REQ-048.A4 — "The installed app shows a launcher icon that matches the store icon; the icon contains no text." The
    // installed app's leg (S-57; R7): the one launcher activity is MainActivity; its icon, loaded as a launcher loads it, is an
    // adaptive icon whose resource is the app's own and not the platform default; its two layers, drawn over the 108-unit
    // square, show DI-9's seven colours inside DI-9's seven pieces and DI-9's background colour around them, within 8 / 255.
    // The store side and "no text" are StoreIconKitTest's (stage 3), against the same DI-9 points. Positive control, in this
    // test: the platform default icon drawn the same way fails the comparison.
    @Test
    fun theInstalledLauncherIconIsDi9sIconOfSevenPiecesOnItsBackground() { // REQ-048.A4
        val checks = mutableListOf<Pair<Boolean, String>>()
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val pm = target.packageManager
        val pkg = target.packageName
        checks += (pkg == KIT_APP_PACKAGE) to "the app under test is $pkg (expected $KIT_APP_PACKAGE)"

        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(pkg)
        @Suppress("DEPRECATION")
        val resolved = pm.queryIntentActivities(launcher, 0)
        val names = resolved.map { it.activityInfo.name }
        checks += (names == listOf(KIT_MAIN_ACTIVITY)) to "the launcher activities of $pkg: $names (expected exactly [$KIT_MAIN_ACTIVITY])"

        val ri = resolved.firstOrNull { it.activityInfo.name == KIT_MAIN_ACTIVITY }
        if (ri != null) {
            val res = ri.iconResource
            val resName = if (res == 0) "none" else runCatching { target.resources.getResourceName(res) }.getOrElse { "unnamed (${it.javaClass.simpleName})" }
            checks += (res != 0 && res != android.R.drawable.sym_def_app_icon) to
                "the icon resource of $KIT_MAIN_ACTIVITY: 0x%08x (%s) — neither none nor the platform's sym_def_app_icon".format(res, resName)
            val resPackage = if (res == 0) null else runCatching { pm.getResourcesForApplication(pkg).getResourcePackageName(res) }.getOrNull()
            checks += (resPackage == pkg) to "the icon resource belongs to the package $resPackage (expected the app's own, $pkg)"

            val icon = ri.loadIcon(pm)
            checks += (icon is AdaptiveIconDrawable) to "the loaded icon is ${icon.javaClass.name} (expected an AdaptiveIconDrawable, S-57)"
            if (icon is AdaptiveIconDrawable) {
                checks += (icon.background != null) to "its background layer: ${icon.background?.javaClass?.name}"
                checks += (icon.foreground != null) to "its foreground layer: ${icon.foreground?.javaClass?.name}"
            }
            val mismatches = kitCompare("the installed icon", kitRender(icon))
            checks += mismatches.isEmpty() to
                "the installed icon at the ${kitSamples.size} DI-9 points: ${mismatches.size} outside the tolerance${if (mismatches.isEmpty()) "" else " — " + mismatches.joinToString("; ")}"
        }

        val default = pm.defaultActivityIcon
        val control = kitCompare("the platform default icon", kitRender(default))
        checks += control.isNotEmpty() to
            "positive control: the platform default icon (${default.javaClass.name}) drawn the same way fails the comparison at ${control.size} of ${kitSamples.size} points" +
            if (control.isEmpty()) " — the comparison CANNOT FAIL" else ""

        for ((ok, text) in checks) Log.i(KIT_LOG, "${if (ok) "PASS" else "FAIL"} $text")
        val failed = checks.filter { !it.first }
        if (failed.isNotEmpty()) {
            throw AssertionError(failed.joinToString("\n", prefix = "${failed.size} of ${checks.size} checks failed:\n") { it.second })
        }
    }

    // ── Kit helpers ──────────────────────────────────────────────────────────────────────────────

    /**
     * [d] drawn on a fresh [KIT_SIZE] square software bitmap: an adaptive icon's background layer and then its foreground
     * layer, each over the whole square (the 108-unit layer square); any other drawable over the whole square.
     */
    private fun kitRender(d: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(KIT_SIZE, KIT_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val icon = d.mutate()
        if (icon is AdaptiveIconDrawable) {
            for (layer in listOfNotNull(icon.background, icon.foreground)) {
                layer.setBounds(0, 0, KIT_SIZE, KIT_SIZE)
                layer.draw(canvas)
            }
        } else {
            icon.setBounds(0, 0, KIT_SIZE, KIT_SIZE)
            icon.draw(canvas)
        }
        return bitmap
    }

    /** The DI-9 points of [bitmap] whose pixel is not the point's colour within [KIT_TOL] per channel with alpha 255, described. */
    private fun kitCompare(what: String, bitmap: Bitmap): List<String> {
        val out = ArrayList<String>()
        for (s in kitSamples) {
            val px = floor(s.x * KIT_PX_PER_UNIT).toInt()
            val py = floor(s.y * KIT_PX_PER_UNIT).toInt()
            val argb = bitmap.getPixel(px, py)
            val a = (argb ushr 24) and 0xFF
            val dr = abs(((argb shr 16) and 0xFF) - ((s.rgb shr 16) and 0xFF))
            val dg = abs(((argb shr 8) and 0xFF) - ((s.rgb shr 8) and 0xFF))
            val db = abs((argb and 0xFF) - (s.rgb and 0xFF))
            val ok = a == 0xFF && dr <= KIT_TOL && dg <= KIT_TOL && db <= KIT_TOL
            Log.i(KIT_LOG, "INFO $what at (${s.x}, ${s.y}) = px ($px, $py): #%08X, expected #%06X — %s (%s)".format(argb, s.rgb, if (ok) "match" else "MISMATCH", s.what))
            if (!ok) out += "(${s.x}, ${s.y}) ${s.what}: #%08X, expected #FF%06X".format(argb, s.rgb)
        }
        return out
    }
}
