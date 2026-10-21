// WO-008 TASK-049 · seam S-46 (designs/WO-008-design.md §2; tasks.md § WO-008 "Files"): the app's one `Clock` · G-14
// ("product code never calls `System.currentTimeMillis()` / `SystemClock` outside `:app`'s one `Clock` implementation"),
// ADR-006 (`:app` provides the real clock; tests set it) · TYPE-005, REQ-029 (rule 1: today's time restarts at local
// midnight) · decisions.md 2026-10-20 "TYPE-005 as code." (`AppClock.nowMillis()` = `SystemClock.elapsedRealtime()`).
package app.tangramplaytime.app

import android.os.SystemClock
import app.tangramplaytime.contracts.Clock
import java.time.LocalDate

/**
 * The real [Clock] of the process (S-46; G-14): [nowMillis] is `SystemClock.elapsedRealtime()`, which only moves forward
 * and keeps counting while the device sleeps (design §3 library fact 3); [today] is `LocalDate.now()`, read in the default
 * time zone at each call, so a zone change reaches it (library fact 3).
 *
 * `AppGraph` builds the kernel's `ActiveTime` over this object (S-15Δ3); every reading goes through it, on the main thread.
 */
internal object AppClock : Clock {
    /**
     * The kits' door (S-46; ADR-006 "tests set it"): while non-null, every reading is this clock's instead of the system's.
     * Product code never sets it. Volatile because the instrumentation thread may write it while the main thread reads.
     */
    @Volatile
    internal var override: Clock? = null

    override fun nowMillis(): Long = override?.nowMillis() ?: SystemClock.elapsedRealtime()

    override fun today(): LocalDate = override?.today() ?: LocalDate.now()
}
