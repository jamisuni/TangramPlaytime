// Acceptance kit (WO-007, TASK-058; kit stage 3) — the five sound files and their record, on the JVM. A file pin with no token
// (REQ-033's one line is `SoundKitTest`'s, on the device: designs/WO-007-design.md §2 "Tokens"); prose names a line without
// the dot (traceability-rules.md §2).
//
// - The sources:
//   - REQ-033 rule 1: "Each effect lasts at most 1 s." Rule 2: "No sound signals failure: a return to the tray uses a soft low
//     tone."
//   - S-52 (design §2): "`tools/make_sounds.py` (stdlib only, deterministic) writes `res/raw/sfx_{pick_up,turn,lock,home,solve}.wav`,
//     16-bit mono at 22 050 Hz. … `ASSET-LICENCES.md` gets a § Sounds (G-06, G-16)".
//   - decisions.md 2026-10-21 "The five sounds and the tick." (E7): the recipes ("pick-up: 520 → 700 Hz, 70 ms; turn: 760 Hz,
//     40 ms; lock: 1320 + 1760 Hz, 60 ms; return: 300 → 180 Hz, 160 ms, soft and low; solve: a rising four-note chime, 570
//     ms"); "Limits: each lasts at most 1 s"; "Licences: each sound is listed in `ASSET-LICENCES.md` § Sounds: made for this
//     project, GPL-3.0".
//   - G-16 (architecture.md): "every shipped sound, font and image is listed with its licence in `ASSET-LICENCES.md`".
//   - tasks.md § WO-007 seam note 3: "S-52's § Sounds is a new `## Sounds` section".
// - What the "soft low" pin asserts, and only that (the brief: it "cites REQ-033 rule 2 and E7's recipe, and asserts only what
//   they say"): low — the return's frequency, measured from its zero crossings, lies in E7's 300 → 180 Hz band (widened by
//   15 % for the estimate) and is the lowest of the five; soft — its loudest sample is no louder than the loudest sample of
//   any other sound. No exact duration, envelope or gain is asserted: E7 names none beyond "at most 1 s".
// - Data read at run time: the folder `app/src/main/res/raw/` and `ASSET-LICENCES.md`, both under the project root (the nearest
//   ancestor of the working directory that holds `settings.gradle.kts`, as `ReleasePromiseKitTest` finds it). The WAV reader
//   is written from the RIFF layout (a `fmt ` chunk and a `data` chunk, other chunks skipped), not from the tool.
// - Every check is computed and the test fails once with every failed check (as `ReleasePromiseKitTest` reports), so one run
//   shows every leg. JUnit 4; no `assume…`; helper names carry a `kit` / `Kit` / `KIT_` prefix and are members of this class.
package app.tangramplaytime.app

import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class SoundAssetsKitTest {

    private companion object {
        /** S-52's five files, by name: pick-up, turn, lock, return (home) and solve. */
        val KIT_FILES = listOf("sfx_pick_up.wav", "sfx_turn.wav", "sfx_lock.wav", "sfx_home.wav", "sfx_solve.wav")

        /** The return to the tray (REQ-033 rule 2): S-52's `sfx_home`. */
        const val KIT_RETURN = "sfx_home.wav"

        /** S-52: "16-bit mono at 22 050 Hz" (PCM, format tag 1). */
        const val KIT_RATE = 22_050
        const val KIT_BITS = 16
        const val KIT_CHANNELS = 1
        const val KIT_PCM = 1

        /** REQ-033 rule 1: "Each effect lasts at most 1 s." */
        const val KIT_MAX_SECONDS = 1.0

        /** E7: "return: 300 → 180 Hz", widened by 15 % on each side for a zero-crossing estimate of a sweep under an envelope. */
        const val KIT_RETURN_LOW_HZ = 180.0 * 0.85
        const val KIT_RETURN_HIGH_HZ = 300.0 * 1.15

        /** A sample counts as sounding (for the frequency estimate) above this share of the file's own peak. */
        const val KIT_SOUNDING_SHARE = 0.05

        /** The record's section for the sounds (seam note 3). */
        const val KIT_SOUNDS_HEADING = "## Sounds"
    }

    /** One WAV as read from its RIFF chunks. */
    private class KitWav(val name: String, val format: Int, val channels: Int, val rate: Int, val bits: Int, val samples: ShortArray) {
        val seconds: Double get() = samples.size.toDouble() / rate
        val peak: Int get() = samples.maxOfOrNull { abs(it.toInt()) } ?: 0

        /**
         * The frequency the zero crossings give over the sounding span (the samples from the first to the last above
         * [KIT_SOUNDING_SHARE] of the peak): crossings ÷ 2 ÷ the span's length. A sweep gives its mean frequency.
         */
        fun crossingHz(): Double {
            val threshold = peak * KIT_SOUNDING_SHARE
            val first = samples.indexOfFirst { abs(it.toInt()) > threshold }
            val last = samples.indexOfLast { abs(it.toInt()) > threshold }
            if (first < 0 || last <= first) return 0.0
            var crossings = 0
            var prev = 0
            for (i in first..last) {
                val s = samples[i].toInt()
                if (s == 0) continue
                if (prev != 0 && (s > 0) != (prev > 0)) crossings++
                prev = s
            }
            return crossings / 2.0 / ((last - first + 1).toDouble() / rate)
        }
    }

    /** The nearest ancestor of the working directory (itself included) that holds settings.gradle.kts. */
    private fun kitProjectRoot(): File {
        val start = File(System.getProperty("user.dir") ?: throw AssertionError("no user.dir: cannot find the project root")).absoluteFile
        return generateSequence(start) { it.parentFile }.firstOrNull { File(it, "settings.gradle.kts").isFile }
            ?: throw AssertionError("no ancestor of $start holds settings.gradle.kts; cannot find the project root")
    }

    /** Reads [file] as a RIFF/WAVE file: its `fmt ` chunk and its 16-bit `data` samples; anything else fails loudly. */
    private fun kitReadWav(file: File): KitWav {
        val bytes = file.readBytes()
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun tag(at: Int): String = String(bytes, at, 4, Charsets.US_ASCII)
        if (bytes.size < 12 || tag(0) != "RIFF" || tag(8) != "WAVE") throw AssertionError("${file.name}: not a RIFF/WAVE file")
        var at = 12
        var format = -1
        var channels = -1
        var rate = -1
        var bits = -1
        var samples: ShortArray? = null
        while (at + 8 <= bytes.size) {
            val id = tag(at)
            val size = b.getInt(at + 4)
            val body = at + 8
            if (size < 0 || body + size > bytes.size) throw AssertionError("${file.name}: chunk `$id` of $size bytes runs past the file's ${bytes.size}")
            when (id) {
                "fmt " -> {
                    format = b.getShort(body).toInt() and 0xFFFF
                    channels = b.getShort(body + 2).toInt() and 0xFFFF
                    rate = b.getInt(body + 4)
                    bits = b.getShort(body + 14).toInt() and 0xFFFF
                }
                "data" -> {
                    if (bits != 16) throw AssertionError("${file.name}: a `data` chunk before a 16-bit `fmt ` chunk (bits $bits)")
                    samples = ShortArray(size / 2) { b.getShort(body + 2 * it) }
                }
            }
            at = body + size + (size and 1)
        }
        return KitWav(file.name, format, channels, rate, bits, samples ?: throw AssertionError("${file.name}: no `data` chunk"))
    }

    /** The lines of `## Sounds` up to the next `## ` heading; null when the section is absent. */
    private fun kitSoundsSection(text: String): List<String>? {
        val lines = text.lines()
        val start = lines.indexOfFirst { it.trim() == KIT_SOUNDS_HEADING }
        if (start < 0) return null
        val body = lines.drop(start + 1)
        val end = body.indexOfFirst { it.trimStart().startsWith("## ") }
        return if (end < 0) body else body.take(end)
    }

    // Pin (no token): S-52 (the five names; "16-bit mono at 22 050 Hz"); REQ-033 rule 1 ("at most 1 s") and rule 2 ("a return to
    // the tray uses a soft low tone"); E7 (the return "300 → 180 Hz … soft and low"; "each sound is listed in `ASSET-LICENCES.md`
    // § Sounds: made for this project, GPL-3.0"); G-16; seam note 3 (a `## Sounds` section).
    // - The folder holds exactly S-52's five `sfx_*.wav` files.
    // - Each is PCM, 16-bit, mono, 22 050 Hz, holds a sound (a non-zero sample) and lasts at most 1 s.
    // - The return: its zero-crossing frequency lies in E7's band and is the lowest of the five; its peak is no louder than any
    //   other sound's peak.
    // - `ASSET-LICENCES.md` has a `## Sounds` section with exactly one table row naming each file, and each of those rows says
    //   "made for this project" and "GPL-3.0".
    @Test
    fun theFiveSoundsAreShortMonoWavsTheReturnIsTheSoftLowOneAndEachIsListed() {
        val checks = ArrayList<Pair<Boolean, String>>()
        fun check(ok: Boolean, what: String) {
            checks += ok to what
        }
        val root = kitProjectRoot()
        val raw = File(root, "app/src/main/res/raw")
        val found = raw.listFiles()?.map { it.name }?.filter { it.startsWith("sfx_") && it.endsWith(".wav") }?.sorted().orEmpty()
        check(found == KIT_FILES.sorted(), "the raw folder ${raw.path} holds exactly S-52's five sounds ${KIT_FILES.sorted()}; found $found")

        val wavs = KIT_FILES.mapNotNull { name ->
            val f = File(raw, name)
            if (!f.isFile) {
                check(false, "$name exists in ${raw.path} (S-52)")
                null
            } else {
                try {
                    kitReadWav(f)
                } catch (e: AssertionError) {
                    check(false, e.message ?: "$name: unreadable")
                    null
                }
            }
        }
        for (w in wavs) {
            check(w.format == KIT_PCM, "${w.name}: PCM (format tag ${w.format}, expected $KIT_PCM; S-52)")
            check(w.bits == KIT_BITS, "${w.name}: 16-bit (${w.bits}; S-52)")
            check(w.channels == KIT_CHANNELS, "${w.name}: mono (${w.channels} channels; S-52)")
            check(w.rate == KIT_RATE, "${w.name}: 22 050 Hz (${w.rate}; S-52)")
            check(w.peak > 0, "${w.name}: holds a sound, a non-zero sample (REQ-033 statement: it plays a short sound)")
            check(w.seconds > 0.0 && w.seconds <= KIT_MAX_SECONDS, "${w.name}: lasts at most 1 s (REQ-033 rule 1): ${"%.3f".format(w.seconds)} s")
            println("SoundAssetsKitTest: ${w.name} ${"%.3f".format(w.seconds)} s, peak ${w.peak}, zero-crossing ${"%.0f".format(w.crossingHz())} Hz")
        }

        val ret = wavs.firstOrNull { it.name == KIT_RETURN }
        val others = wavs.filter { it.name != KIT_RETURN }
        if (ret == null || others.size != KIT_FILES.size - 1) {
            check(false, "the return $KIT_RETURN and the four other sounds are readable, so the soft low pin can be judged")
        } else {
            val hz = ret.crossingHz()
            check(hz in KIT_RETURN_LOW_HZ..KIT_RETURN_HIGH_HZ, "$KIT_RETURN is low: ${"%.0f".format(hz)} Hz, in E7's 300 → 180 Hz band (± 15 %)")
            for (o in others) {
                check(hz < o.crossingHz(), "$KIT_RETURN is lower than ${o.name} (REQ-033 rule 2: the return is the low tone): ${"%.0f".format(hz)} Hz vs ${"%.0f".format(o.crossingHz())} Hz")
                check(ret.peak <= o.peak, "$KIT_RETURN is soft: its peak ${ret.peak} is no louder than ${o.name}'s ${o.peak} (REQ-033 rule 2; E7 \"soft\")")
            }
        }

        val record = File(root, "ASSET-LICENCES.md")
        if (!record.isFile) {
            check(false, "ASSET-LICENCES.md exists in the project root ${root.path} (G-16)")
        } else {
            val section = kitSoundsSection(record.readText(Charsets.UTF_8))
            if (section == null) {
                check(false, "ASSET-LICENCES.md has a `$KIT_SOUNDS_HEADING` section (seam note 3; S-52)")
            } else {
                val rows = section.filter { it.trimStart().startsWith("|") }
                for (name in KIT_FILES) {
                    val mine = rows.filter { it.contains(name) }
                    check(mine.size == 1, "§ Sounds lists $name in exactly one table row (G-16; E7): ${mine.size} rows")
                    val row = mine.firstOrNull() ?: continue
                    check(row.contains("made for this project"), "§ Sounds: $name's row says \"made for this project\" (E7): $row")
                    check(row.contains("GPL-3.0"), "§ Sounds: $name's row names its licence, GPL-3.0 (E7; G-16): $row")
                }
            }
        }

        val failed = checks.filter { !it.first }
        if (failed.isNotEmpty()) {
            throw AssertionError(failed.joinToString("\n", prefix = "SoundAssetsKitTest: ${failed.size} of ${checks.size} checks failed:\n") { it.second })
        }
    }
}
