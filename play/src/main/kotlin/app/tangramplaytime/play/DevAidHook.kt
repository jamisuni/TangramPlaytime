// WO-005 TASK-024 · seam S-25 (tasks.md § WO-005 › Seams) · REQ-046 · architecture.md G-08 (the testing aid
// exists only in the debug build type) and O-08 (src/main holds no reference to the aid's code, text or
// behaviour), as read in decisions.md 2026-10-17 "The DEV aid reaches the screen through one door".
// WO-005 TASK-026 · S-25's four composable members (plan review F3: added together with their overrides in the
// debug source set). `PlayScreen` calls them only when it is given an aid (S-10Δ2); the router reads the button's
// slot and `sheetOpen` (S-22Δ2). They are declarations only: what they draw lives with the aid (O-08).
// WO-006 TASK-029 · S-33 (tasks.md § WO-006 › Seams) · REQ-035 rule 3; decisions.md 2026-10-17 "The DEV aid, drawn in
// detail" (the keyboard carry) and "Edge to edge, the insets and the keyboard, as drawn": `SheetLayer` also takes its
// content area (`ScreenGeometry.sheetContent`), so the prompt and the menu sit inside the insets and above the soft
// keyboard. The door still names no aid class and holds no aid text (O-08).
package app.tangramplaytime.play

import androidx.compose.runtime.Composable
import java.util.ServiceLoader

/**
 * REQ-046's one door from `src/main` to the testing aid that only a debug build carries (G-08, O-08).
 *
 * The aid itself (its passcode, its texts, its drawing and its state) lives in the debug source set and is
 * found at run time through [installed]. This class holds none of it and names none of the aid's classes,
 * so a release build, which registers no service for it, ships only this empty door (S-25, S-30).
 *
 * **Harmless when empty:** with [installed] null, the screen draws nothing for the aid, hits nothing for it
 * and never stands aside for it.
 *
 * The constructor and every member are `internal`: only `:play` and its debug source set see them.
 */
abstract class DevAidHook internal constructor() {

    /** The aid's prompt or menu is open, so the touch router stands aside (S-22Δ2, hit order 0). */
    internal abstract val sheetOpen: Boolean

    /** The DEV button was acted on at its lift (S-22Δ2: a tap within the slop, whatever its duration). */
    internal abstract fun onButton()

    /**
     * The DEV button's touch area in this frame, in `play-screen` px, or null when it is not shown (REQ-046 rule 5).
     * The screen calls it at each composition and publishes the slot with the other controls, so the router hits
     * exactly what is drawn (S-10Δ2, S-22Δ2).
     */
    @Composable
    internal abstract fun rememberButtonSlot(controller: PlayController, geometry: ScreenGeometry): Slot?

    /** The solution layer of REQ-046 A2, composed right after the silhouette (S-10Δ2); it takes no touch. */
    @Composable
    internal abstract fun SolutionLayer(controller: PlayController, geometry: ScreenGeometry)

    /** The DEV button at [slot], composed after the solved row and before the landing preview (S-10Δ2). */
    @Composable
    internal abstract fun ButtonLayer(slot: Slot)

    /**
     * The open sheet, composed last while [sheetOpen] holds (S-10Δ2): its scrim over [area] (`ScreenGeometry.overlay`,
     * the screen below the top bar to its edges), its prompt or menu inside [content] (`ScreenGeometry.sheetContent`:
     * inside the insets and above the soft keyboard; S-33). With no insets and no keyboard [content] is [area].
     */
    @Composable
    internal abstract fun SheetLayer(controller: PlayController, area: Slot, content: Slot)

    /** The door's one lookup (S-25). */
    companion object {
        /**
         * The debug build's aid, found once per process with [ServiceLoader] through the service registration
         * that only the debug source set ships (S-30); null in a release build, which ships none (REQ-046: a
         * release build contains no DEV button; G-08). A new process finds a new aid ("What survives").
         */
        val installed: DevAidHook? by lazy {
            ServiceLoader.load(DevAidHook::class.java, DevAidHook::class.java.classLoader).firstOrNull()
        }
    }
}
