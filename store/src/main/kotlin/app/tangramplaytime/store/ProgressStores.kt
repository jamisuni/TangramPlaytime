// S-18 (designs/WO-004-design.md §2): the device door to the one IProgressStore (G-13), and the only
// android.* use in :store.
package app.tangramplaytime.store

import android.content.Context
import app.tangramplaytime.contracts.IProgressStore
import java.io.File

/**
 * Where `:app` gets its store (G-13: the only road to persisted state; O-03).
 *
 * The file is `progress.json` in `Context.noBackupFilesDir`, so it never leaves the device (REQ-010 rule;
 * `allowBackup` is false as well).
 */
object ProgressStores {

    private const val FILE_NAME = "progress.json"

    /** One store per file path, built on first use. Guarded by itself. */
    private val stores = HashMap<String, IProgressStore>()

    /**
     * The store of this app's `progress.json`. **Every call returns the same instance** (S-18; design
     * review F3): a second instance over the file would hold its own document, and its next save would
     * drop the first one's saves (the KDoc's must-never: lose one puzzle's progress when another is saved).
     */
    fun onDevice(context: Context): IProgressStore {
        val file = File(context.noBackupFilesDir, FILE_NAME)
        synchronized(stores) {
            return stores.getOrPut(file.absolutePath) { FileProgressStore(file) }
        }
    }
}
