// WO-003 TASK-015 · seam S-15 (tasks.md § WO-003 › Seams) · architecture.md G-12 / §3: `:app` is the one
// place that builds the lock engine, the puzzle library and the Play screen's state holder, and hands them
// on (WO-002 design review F4: the engine and the library exist once per process).
package app.tangramplaytime.app

import app.tangramplaytime.content.PuzzleLibraries
import app.tangramplaytime.contracts.ILockEngine
import app.tangramplaytime.contracts.IPuzzleLibrary
import app.tangramplaytime.kernel.LockEngine
import app.tangramplaytime.play.PlayController

/**
 * The app's object graph, built once per process when [MainActivity] first reads it (a Kotlin `object`
 * is initialised once per class loader, thread-safely), and kept across activity recreations.
 *
 * The kits never call it: `AppLaunchKitTest` sees it only through the screen it shows.
 */
internal object AppGraph {
    /** The one lock rule (WO-001), stateless. */
    val engine: ILockEngine = LockEngine()

    /** The bundled puzzles (WO-002), read and checked once; throws on a build defect (G-15). */
    val library: IPuzzleLibrary = PuzzleLibraries.bundled(engine)

    /**
     * The Play screen's state holder over [library] and [engine]. Its events go nowhere in this WO:
     * sounds and the haptic are WO-007's (O-07), which passes a listener here.
     */
    val controller: PlayController = PlayController(library, engine)
}
