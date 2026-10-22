// Acceptance kit (WO-009, TASK-069, stage 3) — pins, no token: S-59's "mechanical self-check" of the store kit's drafts and
// S-60's size rule for the banner (tasks.md § WO-009 S-59, S-60; decisions.md 2026-10-22 R9 "The store kit lives in
// `release/`, the privacy page is `PRIVACY.md`, and the drafts carry a mechanical self-check.", R10 (the console answers),
// R11 (the store pictures)).
//
// - No token (seam note 4): REQ-001 A2 and REQ-048 A1 and A2 are about the live Play listing, which no test can see; they are
//   the owner's signed `owner-evidence.md` rows. These pins hold the drafts the owner pastes; their IDs are written here with
//   a space, never with the dot.
// - The list, item by item (S-59 "The mechanical self-check"), each with its source:
//   1. both `title.txt` read exactly `Tangram Playtime`, at most 30 characters, with no "free" in any case — REQ-048 rule 1
//      ("Store name: "Tangram Playtime" (SRC-016; Play's metadata policy bars "free" in a title — CHG-001)"), its ASSUMPTION
//      ("the store title is "Tangram Playtime" in both store languages"), Play's 30-character title limit;
//   2. the en short description holds the promise words and is at most 80 characters — REQ-048 rule 1 ("The listing's short
//      description carries the promise "absolutely free — no ads, nothing to buy""), Play's 80-character limit; the fi one
//      is at most 80 characters and not empty, its wording never asserted (REQ-048 ASSUMPTION; G-10: Finnish is reference);
//   3. each full description is at most 4000 characters and not empty — Play's 4000-character limit;
//   4. `PRIVACY.md` holds REQ-049 rule 1's text in full — REQ-048 rule 3 ("A privacy policy exists as a hosted text … both say
//      that the game collects nothing"), REQ-049 rule 1 (the text), REQ-048 ASSUMPTION ("a page in the project's repository");
//   5. `release/console/app-content.md` answers "No" to data collection and sharing, "No" to ads, and gives `PRIVACY.md`'s URL —
//      REQ-048 rule 4 ("The store's data-safety declaration says "no data collected""), REQ-001 rule 1 (no advertising),
//      S-59's answers ("Does your app collect or share any of the required user data types? — No"; "No, my app does not
//      contain ads"; `https://github.com/jamisuni/TangramPlaytime/blob/main/PRIVACY.md`), R9 (the URL);
//   6. `release/pictures/feature-graphic.png` is 1024 × 500, 8-bit RGB, no alpha — S-60 ("Play takes JPEG or 24-bit PNG";
//      1024 × 500), R11.
// - Readings (each the author's; the hand-back names them):
//   - A listing file's field is its UTF-8 text with one final LF removed when present (S-59: "one field per file, UTF-8, LF,
//     the file's text is the field, no trailing blank line"): the last line's terminator is not part of the field. A second
//     trailing LF, or a CR, stays in the field and so fails item 1's "exactly".
//   - Characters are counted as Unicode code points (the TASK-069 brief).
//   - "Holds the promise … in those words": the words of REQ-048 rule 1, with its em dash (U+2014) and comma, compared without
//     regard to letter case (a sentence may start with "Absolutely").
//   - "Holds REQ-049 rule 1's text in full": the text appears with every run of white space (a Markdown line break inside a
//     paragraph) read as one space; no other Markdown is undone.
//   - "Answers No": the text after S-59's data-safety question, on its line (or the next non-blank line), begins with the word
//     "No" once leading dashes, colons, quotes, `|`, `*`, `>` and backticks are passed over; the ads answer is S-59's "No, my
//     app does not contain ads", and the console's other answer, "Yes, my app contains ads", is absent.
// - Crafted fixtures, in this class, each failing its own check and only it, beside a kit-made text that passes (the TASK-069
//   brief): a title with "Free"; an 81-character short description; a short description without the promise words; a privacy
//   page missing one sentence; a data-safety answer "Yes"; a 1024 × 512 banner (written by this file's own PNG encoder).
// - A red pin is a finding against TASK-068's file (the file, the line and the sentence); the kit never edits the data. A
//   missing file fails with its path. Every check is computed and printed (the XML's system-out), then the test fails with
//   every failed check. No `assume…`; nothing is skipped. Helper names carry a `kit` / `Kit` / `KIT_` prefix and are members
//   of this class.
package app.tangramplaytime.app

import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.Deflater

class StoreListingKitTest {

    private companion object {
        const val KIT_EN = "release/listing/en-US"
        const val KIT_FI = "release/listing/fi-FI"
        const val KIT_PRIVACY = "PRIVACY.md"
        const val KIT_CONSOLE = "release/console/app-content.md"
        const val KIT_BANNER = "release/pictures/feature-graphic.png"

        /** REQ-048 rule 1: "Store name: **"Tangram Playtime"**". */
        const val KIT_TITLE = "Tangram Playtime"

        /** Play's limits (the TASK-069 brief: "title 30, short description 80, full description 4000"). */
        const val KIT_TITLE_MAX = 30
        const val KIT_SHORT_MAX = 80
        const val KIT_FULL_MAX = 4000

        /** REQ-048 rule 1: the promise "absolutely free — no ads, nothing to buy", with its em dash (U+2014). */
        const val KIT_PROMISE = "absolutely free \u2014 no ads, nothing to buy"

        /** REQ-049 rule 1, "The text says, in full: …". */
        const val KIT_PRIVACY_TEXT = "Privacy: this game collects no data. It has no network access, no accounts and no analytics. " +
            "Your puzzle progress and play times are stored only on this device and are deleted when the game is uninstalled."

        /** S-59: the privacy policy URL (R9). */
        const val KIT_PRIVACY_URL = "https://github.com/jamisuni/TangramPlaytime/blob/main/PRIVACY.md"

        /** S-59: the data-safety question and the ads answers, in the console's words. */
        const val KIT_DATA_QUESTION = "Does your app collect or share any of the required user data types?"
        const val KIT_ADS_NO = "No, my app does not contain ads"
        const val KIT_ADS_YES = "Yes, my app contains ads"

        /** S-60: the banner's size; PNG colour type 2 is RGB without alpha. */
        const val KIT_BANNER_W = 1024
        const val KIT_BANNER_H = 500
        const val KIT_PNG_RGB = 2
    }

    // ── S-59's list over TASK-068's files ───────────────────────────────────────────────────────

    // Pin, no token — S-59 item 1: both `title.txt` read exactly `Tangram Playtime`, at most 30 characters, with no "free" in any
    // case (REQ-048 rule 1, its ASSUMPTION "in both store languages", CHG-001; Play's 30-character limit).
    @Test
    fun bothTitlesReadTangramPlaytimeWithinThirtyCharactersAndNoFree() {
        val checks = mutableListOf<Pair<Boolean, String>>()
        for (dir in listOf(KIT_EN, KIT_FI)) kitField("$dir/title.txt", checks)?.let { checks += kitTitleChecks("$dir/title.txt", it) }
        kitReport("bothTitlesReadTangramPlaytimeWithinThirtyCharactersAndNoFree", checks)
    }

    // Pin, no token — S-59 item 2: the en short description holds REQ-048 rule 1's promise words and is at most 80 characters;
    // the fi one is at most 80 characters and not empty (its wording is never asserted: G-10, REQ-048 ASSUMPTION).
    @Test
    fun theShortDescriptionsCarryThePromiseWithinEightyCharacters() {
        val checks = mutableListOf<Pair<Boolean, String>>()
        kitField("$KIT_EN/short_description.txt", checks)?.let { checks += kitShortEnChecks("$KIT_EN/short_description.txt", it) }
        kitField("$KIT_FI/short_description.txt", checks)?.let { checks += kitLimitChecks("$KIT_FI/short_description.txt", it, KIT_SHORT_MAX) }
        kitReport("theShortDescriptionsCarryThePromiseWithinEightyCharacters", checks)
    }

    // Pin, no token — S-59 item 3: each full description is at most 4000 characters and not empty (Play's limit).
    @Test
    fun theFullDescriptionsAreWithinFourThousandCharacters() {
        val checks = mutableListOf<Pair<Boolean, String>>()
        for (dir in listOf(KIT_EN, KIT_FI)) kitField("$dir/full_description.txt", checks)?.let { checks += kitLimitChecks("$dir/full_description.txt", it, KIT_FULL_MAX) }
        kitReport("theFullDescriptionsAreWithinFourThousandCharacters", checks)
    }

    // Pin, no token — S-59 item 4: `PRIVACY.md` holds REQ-049 rule 1's text in full (REQ-048 rule 3: the hosted text says the
    // game collects nothing; REQ-048 ASSUMPTION: "a page in the project's repository").
    @Test
    fun thePrivacyPageHoldsTheInGamePrivacyTextInFull() {
        val checks = mutableListOf<Pair<Boolean, String>>()
        kitText(KIT_PRIVACY, checks)?.let { checks += kitPrivacyChecks(KIT_PRIVACY, it) }
        kitReport("thePrivacyPageHoldsTheInGamePrivacyTextInFull", checks)
    }

    // Pin, no token — S-59 item 5: the console answers say "No" to data collection and sharing (REQ-048 rule 4; REQ-010 makes it
    // true), "No" to ads (REQ-001 rule 1), and give `PRIVACY.md`'s URL (R9).
    @Test
    fun theConsoleAnswersSayNoDataNoAdsAndGiveThePrivacyUrl() {
        val checks = mutableListOf<Pair<Boolean, String>>()
        kitText(KIT_CONSOLE, checks)?.let { checks += kitConsoleChecks(KIT_CONSOLE, it) }
        kitReport("theConsoleAnswersSayNoDataNoAdsAndGiveThePrivacyUrl", checks)
    }

    // Pin, no token — S-59 item 6 and S-60: `feature-graphic.png` is 1024 × 500, 8-bit RGB with no alpha ("Play takes JPEG or
    // 24-bit PNG").
    @Test
    fun theFeatureGraphicIs1024By500RgbWithoutAlpha() {
        val checks = mutableListOf<Pair<Boolean, String>>()
        val f = File(kitProjectRoot(), KIT_BANNER)
        if (!f.isFile) checks += false to "$KIT_BANNER does not exist" else checks += kitBannerChecks(KIT_BANNER, f.readBytes())
        kitReport("theFeatureGraphicIs1024By500RgbWithoutAlpha", checks)
    }

    // ── Discrimination: crafted drafts (no token) ───────────────────────────────────────────────

    // Pin, no token: each check above can fail (the TASK-069 brief: "Crafted fixtures fail: a title with "Free"; an
    // 81-character short description; a short description without the promise words; a privacy page missing one sentence; a
    // data-safety answer "Yes"; a 1024 × 512 banner"). Each crafted draft fails its own check, and the kit-made draft it
    // departs from passes that check, so the defect alone is what fails.
    @Test
    fun everyCraftedDraftFailsItsCheck() {
        val checks = mutableListOf<Pair<Boolean, String>>()
        fun pair(what: String, good: List<Pair<Boolean, String>>, bad: List<Pair<Boolean, String>>) {
            checks += good.all { it.first } to "$what — the kit-made draft passes: ${good.filter { !it.first }.map { it.second }}"
            checks += bad.any { !it.first } to "$what — the crafted draft fails: ${bad.filter { !it.first }.map { it.second }}"
        }

        pair("a title with \"Free\"", kitTitleChecks("title", kitFieldOf("Tangram Playtime\n")), kitTitleChecks("title", kitFieldOf("Tangram Playtime Free\n")))

        val goodShort = "Tangram for ages 8 and up, $KIT_PROMISE."
        val long = "$goodShort Seven classic pieces and many shapes to build.".take(81)
        checks += (long.codePointCount(0, long.length) == 81) to "fixture: the long short description has ${long.codePointCount(0, long.length)} code points"
        pair("an 81-character short description", kitShortEnChecks("short", kitFieldOf(goodShort)), kitShortEnChecks("short", kitFieldOf(long)))
        pair("a short description without the promise words", kitShortEnChecks("short", kitFieldOf(goodShort)), kitShortEnChecks("short", kitFieldOf("A calm tangram puzzle game for ages 8 and up.")))

        val page = "# Tangram Playtime \u2014 privacy policy\n\n" + KIT_PRIVACY_TEXT.replace(" It has no network", "\nIt has no network") + "\n"
        val missing = page.replace("It has no network access, no accounts and no analytics. ", "")
        checks += (missing != page) to "fixture: the privacy page without its second sentence differs from the full one"
        pair("a privacy page missing one sentence", kitPrivacyChecks("page", page), kitPrivacyChecks("page", missing))

        val console = "## Privacy policy\n$KIT_PRIVACY_URL\n\n## Ads\n$KIT_ADS_NO\n\n## Data safety\n$KIT_DATA_QUESTION \u2014 No\n"
        val yes = console.replace("$KIT_DATA_QUESTION \u2014 No", "$KIT_DATA_QUESTION \u2014 Yes")
        checks += (yes != console) to "fixture: the data-safety answer \"Yes\" differs from \"No\""
        pair("a data-safety answer \"Yes\"", kitConsoleChecks("console", console), kitConsoleChecks("console", yes))

        pair(
            "a 1024 × 512 banner",
            kitBannerChecks("banner", kitWritePng(KIT_BANNER_W, KIT_BANNER_H)),
            kitBannerChecks("banner", kitWritePng(KIT_BANNER_W, 512)),
        )
        kitReport("everyCraftedDraftFailsItsCheck", checks)
    }

    // ── Checks (shared by the files and the fixtures) ───────────────────────────────────────────

    /** Item 1: the field is exactly `Tangram Playtime`, at most 30 code points, without "free" in any case. */
    private fun kitTitleChecks(what: String, field: String): List<Pair<Boolean, String>> = listOf(
        (field == KIT_TITLE) to "$what reads \"${kitShow(field)}\" (expected exactly \"$KIT_TITLE\")",
        (kitLength(field) <= KIT_TITLE_MAX) to "$what: ${kitLength(field)} characters (at most $KIT_TITLE_MAX)",
        ("free" !in field.lowercase(Locale.ROOT)) to "$what: ${if ("free" in field.lowercase(Locale.ROOT)) "HOLDS" else "no"} \"free\" in any case (CHG-001)",
    )

    /** Item 2, en: at most 80 code points and the promise words. */
    private fun kitShortEnChecks(what: String, field: String): List<Pair<Boolean, String>> =
        kitLimitChecks(what, field, KIT_SHORT_MAX) + listOf(
            (KIT_PROMISE in field.lowercase(Locale.ROOT)) to "$what holds REQ-048 rule 1's promise \"$KIT_PROMISE\": ${KIT_PROMISE in field.lowercase(Locale.ROOT)}",
        )

    /** Items 2 (fi) and 3: not empty, at most [max] code points. */
    private fun kitLimitChecks(what: String, field: String, max: Int): List<Pair<Boolean, String>> = listOf(
        field.isNotBlank() to "$what is ${if (field.isBlank()) "EMPTY" else "not empty"}",
        (kitLength(field) <= max) to "$what: ${kitLength(field)} characters (at most $max)",
    )

    /** Item 4: REQ-049 rule 1's text in full, white space read as single spaces. */
    private fun kitPrivacyChecks(what: String, text: String): List<Pair<Boolean, String>> {
        val flat = kitFlat(text)
        val sentences = KIT_PRIVACY_TEXT.split(Regex("(?<=\\.) ")).filter { it.isNotBlank() }
        val absent = sentences.filter { kitFlat(it) !in flat }
        return listOf(
            (kitFlat(KIT_PRIVACY_TEXT) in flat) to "$what holds REQ-049 rule 1's text in full${if (absent.isEmpty()) "" else "; sentences not found: $absent"}",
        )
    }

    /** Item 5: data safety "No", ads "No" (and not the console's "Yes" answer), the privacy URL. */
    private fun kitConsoleChecks(what: String, text: String): List<Pair<Boolean, String>> {
        val answer = kitAnswerAfter(text, KIT_DATA_QUESTION)
        return listOf(
            (answer != null && Regex("^No\\b").containsMatchIn(answer)) to "$what answers S-59's data-safety question with \"${answer ?: "(the question is absent)"}\" (expected \"No\")",
            (KIT_ADS_NO in text) to "$what holds the ads answer \"$KIT_ADS_NO\": ${KIT_ADS_NO in text}",
            (KIT_ADS_YES !in text) to "$what does not hold \"$KIT_ADS_YES\": ${KIT_ADS_YES !in text}",
            (KIT_PRIVACY_URL in text) to "$what gives the privacy policy URL $KIT_PRIVACY_URL: ${KIT_PRIVACY_URL in text}",
        )
    }

    /** Item 6: a PNG of 1024 × 500, 8-bit RGB (colour type 2), with no tRNS chunk. */
    private fun kitBannerChecks(what: String, bytes: ByteArray): List<Pair<Boolean, String>> {
        val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        if (bytes.size < 33 || !bytes.copyOfRange(0, 8).contentEquals(signature) || String(bytes, 12, 4, Charsets.US_ASCII) != "IHDR") {
            return listOf(false to "$what is not a PNG with an IHDR chunk first")
        }
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val w = b.getInt(16)
        val h = b.getInt(20)
        val depth = bytes[24].toInt() and 0xFF
        val colourType = bytes[25].toInt() and 0xFF
        val chunks = ArrayList<String>()
        var off = 8
        while (off + 12 <= bytes.size) {
            val len = b.getInt(off)
            if (len < 0 || off + 12 + len > bytes.size) break
            chunks += String(bytes, off + 4, 4, Charsets.US_ASCII)
            off += 12 + len
        }
        return listOf(
            (w == KIT_BANNER_W && h == KIT_BANNER_H) to "$what is $w × $h (expected $KIT_BANNER_W × $KIT_BANNER_H, S-60)",
            (depth == 8 && colourType == KIT_PNG_RGB && "tRNS" !in chunks) to
                "$what: bit depth $depth, colour type $colourType, chunks ${chunks.distinct()} (expected 8-bit RGB, colour type 2, no tRNS: 24-bit, no alpha)",
        )
    }

    // ── Reading ──────────────────────────────────────────────────────────────────────────────────

    /** The field of a listing file: its strict UTF-8 text, one final LF removed (the header's reading). */
    private fun kitField(path: String, checks: MutableList<Pair<Boolean, String>>): String? = kitText(path, checks)?.let { kitFieldOf(it) }

    private fun kitFieldOf(text: String): String = text.removeSuffix("\n")

    /** The strict UTF-8 text of [path] under the project root, or null with a failed check (missing, or not UTF-8). */
    private fun kitText(path: String, checks: MutableList<Pair<Boolean, String>>): String? {
        val f = File(kitProjectRoot(), path)
        if (!f.isFile) {
            checks += false to "$path does not exist"
            return null
        }
        return try {
            Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(f.readBytes())).toString()
        } catch (e: CharacterCodingException) {
            checks += false to "$path is not UTF-8 text"
            null
        }
    }

    /** Unicode code points (the TASK-069 brief: "Count characters as Unicode code points"). */
    private fun kitLength(s: String): Int = s.codePointCount(0, s.length)

    /** Every run of white space as one space, trimmed. */
    private fun kitFlat(s: String): String = s.replace(Regex("\\s+"), " ").trim()

    /** A field shown with its line breaks visible. */
    private fun kitShow(s: String): String = s.replace("\r", "\\r").replace("\n", "\\n")

    /** The answer after [question]: the rest of its line, or the next non-blank line, past leading dashes, colons, quotes and marks. */
    private fun kitAnswerAfter(text: String, question: String): String? {
        val at = text.indexOf(question)
        if (at < 0) return null
        val lines = text.substring(at + question.length).split('\n')
        val strip = Regex("^[\\s\u2014\u2013\\-:*|\"\u201C\u201D>`]+")
        return lines.map { it.replace(strip, "").trim() }.firstOrNull { it.isNotEmpty() }
    }

    /** Prints every check, then fails with every failed one. */
    private fun kitReport(test: String, checks: List<Pair<Boolean, String>>) {
        for ((ok, text) in checks) println("StoreListingKitTest ${if (ok) "PASS" else "FAIL"} $test: $text")
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

    /** A kit-made 8-bit RGB PNG of [width] × [height], DI-9's background colour everywhere (filter 0 rows, one IDAT). */
    private fun kitWritePng(width: Int, height: Int): ByteArray {
        val row = ByteArray(1 + width * 3).also { r -> for (x in 0 until width) { r[1 + 3 * x] = 0xFB.toByte(); r[2 + 3 * x] = 0xF7.toByte(); r[3 + 3 * x] = 0xEE.toByte() } }
        val raw = ByteArrayOutputStream()
        repeat(height) { raw.write(row) }
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
        chunk("IHDR", ByteBuffer.allocate(13).order(ByteOrder.BIG_ENDIAN).putInt(width).putInt(height).put(8).put(KIT_PNG_RGB.toByte()).put(0).put(0).put(0).array())
        chunk("IDAT", z.toByteArray())
        chunk("IEND", ByteArray(0))
        return out.toByteArray()
    }
}
