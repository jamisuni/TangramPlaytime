// WO-003 TASK-015 · seam S-16 (tasks.md § WO-003 › Seams) · architecture.md G-08 / O-08: the debug build
// type's marker. The DEV aid (REQ-046, WO-005) will live beside it in `play/src/debug/`; nothing in
// `src/main` references either. The release verifier (`.swdev/verifiers/v01_release_apk.py`) reads
// [DebugCanary.MARKER] from this file, must find it in a debug APK (its positive control) and must not
// find it in the release APK.
package app.tangramplaytime.play

/** Present only in the `debug` build type; its string proves which build type an APK was built from. */
internal object DebugCanary {
    const val MARKER: String = "tangramplaytime-debug-canary-g08-only-in-debug"
}
