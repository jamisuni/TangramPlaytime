// S-17 (designs/WO-004-design.md §2): the IProgressStore of ADR-003 over one UTF-8 JSON file
// (decisions.md 2026-10-15, the ADR-003 row and its amendment; governance row 11).
package app.tangramplaytime.store

import app.tangramplaytime.contracts.IProgressStore
import app.tangramplaytime.contracts.PlayTimes
import app.tangramplaytime.contracts.PuzzleProgress
import app.tangramplaytime.contracts.Settings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Everything that survives closing the app (REQ-025 rule, REQ-003), kept in [file] as the format-1
 * document of ADR-003.
 *
 * - **Reading.** The file is read once, on the first call, into memory behind a [Mutex]; every member
 *   then answers from memory.
 * - **Saving.** Every save changes the in-memory document and rewrites the whole file: a temporary file
 *   beside it, `fd.sync()`, then `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)` over it. The call returns
 *   after the rename. The file is never patched.
 * - **Never throws** (S-17; G-15; the KDoc's "never fails the launch and never throws for it"):
 *   - any `Exception` while reading, decoding, validating or mapping makes the whole store read as a
 *     fresh install, and the next save overwrites the file;
 *   - any `Exception` while encoding or writing is a failed write: the old file is kept, the in-memory
 *     document keeps the save, and the next save retries by writing the whole document again.
 *
 * Build one instance per file (S-18 [ProgressStores.onDevice]): two instances over one file each hold
 * their own document, and the later save would drop the other's.
 *
 * @param file the document's file; its temporary file is `<file>.tmp` in the same folder, so the move
 *   stays one rename.
 * @param io where the file is read and written.
 */
class FileProgressStore(
    private val file: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : IProgressStore {

    private val mutex = Mutex()

    /** The in-memory document; null until the first call reads the file. Guarded by [mutex]. */
    private var document: ProgressDocument? = null

    override suspend fun progress(puzzleId: String): PuzzleProgress =
        read { it.puzzles[puzzleId] ?: PuzzleProgress.FRESH }

    override suspend fun saveProgress(puzzleId: String, progress: PuzzleProgress) =
        save { it.copy(puzzles = it.puzzles + (puzzleId to progress)) }

    override suspend fun allProgress(): Map<String, PuzzleProgress> = read { it.puzzles }

    override suspend fun playTimes(): PlayTimes = read { it.playTimes }

    override suspend fun savePlayTimes(times: PlayTimes) = save { it.copy(playTimes = times) }

    override suspend fun settings(): Settings = read { it.settings }

    override suspend fun saveSettings(settings: Settings) = save { it.copy(settings = settings) }

    override suspend fun lastOpenedPuzzleId(): String? = read { it.lastOpened }

    override suspend fun saveLastOpenedPuzzleId(id: String) = save { it.copy(lastOpened = id) }

    /** Every puzzle FRESH (no entry left) and the play times NONE; the settings and the last-opened id are kept. */
    override suspend fun resetAll() = save { it.copy(puzzles = emptyMap(), playTimes = PlayTimes.NONE) }

    private suspend fun <T> read(get: (ProgressDocument) -> T): T =
        withContext(io) { mutex.withLock { get(loaded()) } }

    private suspend fun save(change: (ProgressDocument) -> ProgressDocument) {
        withContext(io) {
            mutex.withLock {
                val next = change(loaded())
                document = next
                write(next)
            }
        }
    }

    /** The in-memory document, reading the file on the first call. Call only while holding [mutex]. */
    private fun loaded(): ProgressDocument = document ?: readOrFresh().also { document = it }

    /** ADR-003 "Unreadable state": a missing file, a read error or any decode, validation or mapping failure. */
    private fun readOrFresh(): ProgressDocument =
        try {
            ProgressDocument.decode(strictUtf8(file.readBytes()))
        } catch (e: Exception) {
            ProgressDocument.FRESH
        }

    /** ADR-003 "Saving" and "A failed write". Call only while holding [mutex]. */
    private fun write(next: ProgressDocument) {
        val tmp = File(file.path + TMP_SUFFIX)
        try {
            val bytes = next.encode().toByteArray(Charsets.UTF_8)
            FileOutputStream(tmp).use { out ->
                out.write(bytes)
                out.fd.sync()
            }
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            try {
                tmp.delete()
            } catch (ignored: Exception) {
                // The leftover temporary file is truncated and rewritten by the next save.
            }
        }
    }

    private companion object {
        const val TMP_SUFFIX = ".tmp"

        /** UTF-8 that reports malformed bytes instead of replacing them (ADR-003: one UTF-8 file, decoded strictly). */
        fun strictUtf8(bytes: ByteArray): String = Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString()
    }
}
