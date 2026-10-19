// Acceptance kit (WO-010, TASK-033) — the counter's digits on the JVM: seam S-11Δ2 (tasks.md § WO-010 ›
// Seams) over `internal fun PlayTexts.counterOf(number: Int, count: Int): String`, called directly.
//
// - A pin, no token (traceability-rules.md §2: a token is a coverage claim). It serves the REQ-047
//   statement ("in English otherwise": on a device whose language is neither Finnish nor English the game
//   is English, and the English counter has ASCII digits) and the number in the top bar of REQ-024 A2.
//   REQ-047 A2's letter (a Swedish device) does not reach digits, and its token stays WO-006's
//   (decisions.md 2026-10-19 "The counter keeps ASCII digits under every device language", R9; the carry:
//   decisions.md 2026-10-18 "S1 recorded and carried, S2 fixed now, N1 carried …", N1).
// - S-11Δ2: `counterOf` "formats with `java.util.Locale.ROOT`, so the digits are ASCII whatever the JVM's
//   default locale. The signature, `counterFormat` and every string are unchanged."
// - The fixture discriminates: the JVM default locale is switched to one whose digits are not ASCII on
//   this JVM, and the fixture check shows that `String.format` under it really gives non-ASCII digits, so
//   a `counterOf` that formats with the default locale fails the pin. The default locale is restored in
//   `finally` (every category), because one JVM runs every test class of the module.
// - The two `PlayTexts` are the `:app` resources as data (`app/src/main/res/values/strings.xml` and
//   `values-fi/strings.xml`): the counter's `counter_format` is `%1$d / %2$d` in both. No other text is
//   asserted (the `fi` texts are reference translations, G-10). Helper names carry a `kit` / `KIT_` prefix.
package app.tangramplaytime.play

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CounterDigitsKitTest {

    private companion object {
        /**
         * Arabic (Egypt) with the Arabic-Indic numbering system (BCP 47 `-u-nu-arab`): a device language that
         * is neither Finnish nor English (so the game is English, REQ-047 statement) and whose digits are not
         * ASCII. The fixture check proves the digits on this JVM.
         */
        val KIT_OTHER_DIGITS_LOCALE: Locale = Locale.forLanguageTag("ar-EG-u-nu-arab")

        /** The English texts, from `app/src/main/res/values/strings.xml` (S-11Δ fields; S-11Δ2: unchanged). */
        val KIT_EN_TEXTS: PlayTexts = PlayTexts(
            sizeSmall = "S", sizeMedium = "M", sizeLarge = "L", titleLanguage = "en",
            restart = "Restart", retry = "Retry", next = "Next", bestTime = "Best time", noTime = "–",
            inProgress = "in progress", solved = "solved", counterFormat = "%1\$d / %2\$d",
            allPuzzles = "All puzzles", done = "Done",
        )

        /** The Finnish texts, from `app/src/main/res/values-fi/strings.xml` (data only; only the counter is asserted). */
        val KIT_FI_TEXTS: PlayTexts = PlayTexts(
            sizeSmall = "S", sizeMedium = "M", sizeLarge = "L", titleLanguage = "fi",
            restart = "Aloita alusta", retry = "Uudestaan", next = "Seuraava", bestTime = "Paras aika", noTime = "–",
            inProgress = "kesken", solved = "ratkaistu", counterFormat = "%1\$d / %2\$d",
            allPuzzles = "Kaikki tehtävät", done = "Valmis",
        )

        /**
         * Puzzle number and total: a one-digit, a two-digit and a three-digit total (WO-010's grid reaches
         * three-digit counts: `workorders/WO-010.md` § Scope notes, the N1 carry). Together they use every
         * digit 0–9, so a digit mapped wrong anywhere shows.
         */
        val KIT_COUNTS: List<Pair<Int, Int>> = listOf(3 to 7, 24 to 58, 106 to 139)

        /** Runs [block] with the JVM default locale (every category) set to [locale], and restores all three after. */
        fun <T> kitWithDefaultLocale(locale: Locale, block: () -> T): T {
            val saved = Locale.getDefault()
            val savedDisplay = Locale.getDefault(Locale.Category.DISPLAY)
            val savedFormat = Locale.getDefault(Locale.Category.FORMAT)
            try {
                Locale.setDefault(locale)
                return block()
            } finally {
                Locale.setDefault(saved)
                Locale.setDefault(Locale.Category.DISPLAY, savedDisplay)
                Locale.setDefault(Locale.Category.FORMAT, savedFormat)
            }
        }

        /**
         * The format's text with ASCII digits: [format]'s `%1$d` and `%2$d` (S-11Δ `counterFormat`: "the 1-based
         * number (`%1$d`) and the total (`%2$d`)") replaced by [number] and [count] written in ASCII decimal
         * (`Int.toString()` reads no locale).
         */
        fun kitAsciiCounter(format: String, number: Int, count: Int): String =
            format.replace("%1\$d", number.toString()).replace("%2\$d", count.toString())

        /** True when [text] holds a decimal digit outside ASCII `0`–`9`. */
        fun kitHasNonAsciiDigit(text: String): Boolean = text.any { Character.isDigit(it) && it !in '0'..'9' }

        /** [text] with its code points, so a failure shows which digits came out. */
        fun kitCodePoints(text: String): String = "\"$text\" [" + text.map { "U+%04X".format(Locale.ROOT, it.code) }.joinToString(" ") + "]"
    }

    // ── Fixture check (no token): the chosen default locale gives non-ASCII digits on this JVM ────────────

    @Test // pin fixture: S-11Δ2 "whatever the JVM's default locale" — the locale below must be one with other digits
    fun theChosenDefaultLocaleFormatsNonAsciiDigitsOnThisJvm() {
        val before = listOf(Locale.getDefault(), Locale.getDefault(Locale.Category.DISPLAY), Locale.getDefault(Locale.Category.FORMAT))
        kitWithDefaultLocale(KIT_OTHER_DIGITS_LOCALE) {
            assertEquals("fixture: the default locale is switched", KIT_OTHER_DIGITS_LOCALE, Locale.getDefault(Locale.Category.FORMAT))
            for ((n, total) in KIT_COUNTS) {
                // The mechanism of the carried defect (N1): the counter's format through `String.format` with the
                // default locale, as `counterFormat.format(number, count)` does.
                val withDefault = KIT_EN_TEXTS.counterFormat.format(n, total)
                assertTrue("fixture: String.format under $KIT_OTHER_DIGITS_LOCALE gives non-ASCII digits: ${kitCodePoints(withDefault)}", kitHasNonAsciiDigit(withDefault))
                assertNotEquals("fixture: so it differs from the ASCII text", kitAsciiCounter(KIT_EN_TEXTS.counterFormat, n, total), withDefault)
                // And the ASCII text is what Locale.ROOT gives under the same default (S-11Δ2's form), read here
                // only to show the expected value is reachable.
                assertEquals(
                    "fixture: Locale.ROOT gives ASCII digits under the same default",
                    kitAsciiCounter(KIT_EN_TEXTS.counterFormat, n, total),
                    String.format(Locale.ROOT, KIT_EN_TEXTS.counterFormat, n, total),
                )
            }
        }
        assertEquals(
            "fixture: the default locale is restored in every category (the JVM is shared by the module's test classes)",
            before,
            listOf(Locale.getDefault(), Locale.getDefault(Locale.Category.DISPLAY), Locale.getDefault(Locale.Category.FORMAT)),
        )
    }

    // ── The pin (no token) ────────────────────────────────────────────────────────────────────────────

    @Test // pin: S-11Δ2; REQ-047 statement "in English otherwise"; REQ-024 A2 (the number in the top bar); R9; no token
    fun theCounterKeepsAsciiDigitsUnderADefaultLocaleWithOtherDigits() {
        kitWithDefaultLocale(KIT_OTHER_DIGITS_LOCALE) {
            // Fixture check first, so the pin can never pass on a locale that gives ASCII digits anyway.
            val probe = KIT_EN_TEXTS.counterFormat.format(KIT_COUNTS.first().first, KIT_COUNTS.first().second)
            assertTrue("fixture defect: $KIT_OTHER_DIGITS_LOCALE gives ASCII digits on this JVM: ${kitCodePoints(probe)}", kitHasNonAsciiDigit(probe))

            for ((language, texts) in listOf("en" to KIT_EN_TEXTS, "fi" to KIT_FI_TEXTS)) {
                for ((n, total) in KIT_COUNTS) {
                    val shown = texts.counterOf(n, total)
                    val expected = kitAsciiCounter(texts.counterFormat, n, total)
                    assertEquals(
                        "$language counterOf($n, $total) under the default locale $KIT_OTHER_DIGITS_LOCALE: " +
                            "the format's text with ASCII digits (S-11Δ2), got ${kitCodePoints(shown)}",
                        expected,
                        shown,
                    )
                }
            }
        }
    }
}
