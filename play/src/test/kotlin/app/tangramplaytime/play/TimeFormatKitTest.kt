// Acceptance kit (WO-008, TASK-048) — one format for every time the game shows, on the JVM: S-48 (designs/WO-008-design.md §2
// S-11Δ3 / S-48) over `internal fun PlayTexts.timeOf(seconds: Int): String` and `bestTimeText(seconds: Int?, texts: PlayTexts)`,
// called directly.
//
// - A pin, no token (traceability-rules.md §2: a token is a coverage claim; the TASK-048 row: "no dotted ID" here). It serves
//   REQ-029 rule 2 ("Times are shown as m:ss below one hour, and as h and min from one hour up"), which decisions.md 2026-10-20
//   "One format for every time the game shows …" (D10, checkpoint candidate 2, on its default) applies to the timer and the
//   best time; REQ-031's timer and REQ-026's solved row show this text. REQ-029 A2's screen is WO-007's.
// - S-48: `timeOf` — "below 3600 s `m:ss` in hand-built ASCII; from 3600 s `hoursMinutes.format(Locale.ROOT, h, m)`";
//   `bestTimeText` — "it returns `noTime` or `timeOf`". S-11Δ3: `hoursMinutes` from `time_hours_minutes` = "%1$d h %2$d min"
//   (en and fi). D10: "Digits are ASCII under every device language (`Locale.ROOT`, as the 2026-10-19 counter row)"; "From one
//   hour the timer changes once a minute"; "WO-004's best time "65:00" becomes "1 h 5 min"".
// - Read here, where S-48 names h and m without defining them: h and min are the whole hours and the whole minutes of the
//   seconds (integer division, as m:ss takes whole minutes with the seconds left over), so the text changes once a minute
//   (D10). The boundary pairs on both sides of a minute and of an hour (the brief) pin that reading.
// - The expected texts are formed by the kit (never by the product's own formatter): m:ss from `Int.toString()` (which reads
//   no locale); h and min as the texts' own `hoursMinutes` with `%1$d` and `%2$d` replaced by ASCII numbers. The English
//   literals are also written out (G-10: English is the Contract wording). No Finnish word is asserted (G-10): the Finnish
//   texts' outputs are compared with their own resource's format only.
// - The locale fixture is CounterDigitsKitTest's: Arabic (Egypt) with Arabic-Indic digits, checked to give non-ASCII digits on
//   this JVM, set as the default in every category and restored in `finally` (one JVM runs every test class of the module).
// - Helper names carry a `kit` / `KIT_` prefix.
package app.tangramplaytime.play

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TimeFormatKitTest {

    private companion object {
        /** Arabic (Egypt) with Arabic-Indic digits (BCP 47 `-u-nu-arab`): neither Finnish nor English, and not ASCII digits. */
        val KIT_OTHER_DIGITS_LOCALE: Locale = Locale.forLanguageTag("ar-EG-u-nu-arab")

        /** S-11Δ3's resource text, the same in `values/` and `values-fi/` (designs/WO-008-design.md §2). */
        const val KIT_HOURS_MINUTES = "%1\$d h %2\$d min"

        /** The English texts as `:app` hands them in (`app/src/main/res/values/strings.xml`; S-11Δ fields, S-11Δ3). */
        val KIT_EN_TEXTS: PlayTexts = PlayTexts(
            sizeSmall = "S", sizeMedium = "M", sizeLarge = "L", titleLanguage = "en",
            restart = "Restart", retry = "Retry", next = "Next", bestTime = "Best time", noTime = "–",
            inProgress = "in progress", solved = "solved", counterFormat = "%1\$d / %2\$d",
            allPuzzles = "All puzzles", done = "Done", hoursMinutes = KIT_HOURS_MINUTES,
        )

        /** The Finnish texts (`values-fi/strings.xml`), data only: only format outputs are compared, never a word (G-10). */
        val KIT_FI_TEXTS: PlayTexts = PlayTexts(
            sizeSmall = "S", sizeMedium = "M", sizeLarge = "L", titleLanguage = "fi",
            restart = "Aloita alusta", retry = "Uudestaan", next = "Seuraava", bestTime = "Paras aika", noTime = "–",
            inProgress = "kesken", solved = "ratkaistu", counterFormat = "%1\$d / %2\$d",
            allPuzzles = "Kaikki tehtävät", done = "Valmis", hoursMinutes = KIT_HOURS_MINUTES,
        )

        /** Below one hour, the seconds and their m:ss: both sides of a minute (59/60/61, 599/600) and the last second (3599). */
        val KIT_BELOW_AN_HOUR: List<Pair<Int, String>> = listOf(
            0 to "0:00", 1 to "0:01", 9 to "0:09", 10 to "0:10", 59 to "0:59", 60 to "1:00", 61 to "1:01",
            75 to "1:15", 599 to "9:59", 600 to "10:00", 3599 to "59:59",
        )

        /**
         * From one hour, the seconds and the English h-and-min text: both sides of the hour (3599 above, 3600 here), of a minute
         * inside the hour (3659 / 3660), of the next hour (7199 / 7200), D10's "1 h 5 min" (3900 s, WO-004's "65:00"), and
         * two- and three-digit hours.
         */
        val KIT_FROM_AN_HOUR: List<Pair<Int, String>> = listOf(
            3600 to "1 h 0 min", 3659 to "1 h 0 min", 3660 to "1 h 1 min", 3900 to "1 h 5 min", 7199 to "1 h 59 min",
            7200 to "2 h 0 min", 36000 to "10 h 0 min", 359999 to "99 h 59 min", 360000 to "100 h 0 min",
        )

        /** Values whose texts use every digit 0–9 in both forms, so a digit mapped wrong anywhere shows. */
        val KIT_DIGIT_SWEEP: List<Int> = listOf(754, 3367, 489, 45240, 202020, 29340, 39540)

        /** m:ss as the kit forms it: whole minutes, then the seconds left over in two ASCII digits (S-48; REQ-029 rule 2). */
        fun kitMss(seconds: Int): String {
            check(seconds in 0 until 3600) { "kit: m:ss is the form below one hour only (S-48), not $seconds s" }
            return "${seconds / 60}:" + (seconds % 60).toString().padStart(2, '0')
        }

        /** h and min as the kit forms it: [texts]' own `hoursMinutes` with the whole hours and whole minutes in ASCII. */
        fun kitHoursMinutes(texts: PlayTexts, seconds: Int): String =
            texts.hoursMinutes.replace("%1\$d", (seconds / 3600).toString()).replace("%2\$d", ((seconds % 3600) / 60).toString())

        /** S-48's form of [seconds] for [texts], as the kit forms it. */
        fun kitTimeText(texts: PlayTexts, seconds: Int): String = if (seconds < 3600) kitMss(seconds) else kitHoursMinutes(texts, seconds)

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

        /** True when [text] holds a decimal digit outside ASCII `0`–`9`. */
        fun kitHasNonAsciiDigit(text: String): Boolean = text.any { Character.isDigit(it) && it !in '0'..'9' }

        /** [text] with its code points, so a failure shows which digits came out. */
        fun kitCodePoints(text: String): String = "\"$text\" [" + text.map { "U+%04X".format(Locale.ROOT, it.code) }.joinToString(" ") + "]"
    }

    // ── The form below and from one hour ─────────────────────────────────────────────────────────────────

    // Pin (no token): S-48 "below 3600 s `m:ss` in hand-built ASCII"; REQ-029 rule 2 "m:ss below one hour"; D10. Minutes as
    // they come, seconds in two digits; both sides of a minute; 3599 s is still m:ss.
    @Test
    fun belowOneHourATimeIsMinutesAndTwoDigitSeconds() {
        for ((seconds, expected) in KIT_BELOW_AN_HOUR) {
            assertEquals("fixture: the kit's m:ss of $seconds s", expected, kitMss(seconds))
            assertEquals("timeOf($seconds) (S-48: m:ss below 3600 s)", expected, KIT_EN_TEXTS.timeOf(seconds))
        }
    }

    // Pin (no token): S-48 "from 3600 s `hoursMinutes.format(Locale.ROOT, h, m)`"; S-11Δ3 `hoursMinutes` = "%1$d h %2$d min";
    // REQ-029 rule 2 "as h and min from one hour up"; D10 ("From one hour the timer changes once a minute"; "WO-004's best time
    // "65:00" becomes "1 h 5 min""). 3600 s is the first h-and-min text; the minute changes at 3660 s, not before; the hour
    // at 7200 s. The Finnish texts give their own format's output (no word asserted, G-10).
    @Test
    fun fromOneHourATimeIsHoursAndMinutesInTheTextsOwnFormat() {
        for ((seconds, expected) in KIT_FROM_AN_HOUR) {
            assertEquals("fixture: the kit's h and min of $seconds s in English", expected, kitHoursMinutes(KIT_EN_TEXTS, seconds))
            assertEquals("timeOf($seconds) in English (S-48: hoursMinutes from 3600 s)", expected, KIT_EN_TEXTS.timeOf(seconds))
            assertEquals("timeOf($seconds) with the Finnish texts: their own hoursMinutes", kitHoursMinutes(KIT_FI_TEXTS, seconds), KIT_FI_TEXTS.timeOf(seconds))
        }
        assertEquals("the hour's boundary: 3599 s is m:ss", "59:59", KIT_EN_TEXTS.timeOf(3599))
        assertEquals("the hour's boundary: 3600 s is h and min", "1 h 0 min", KIT_EN_TEXTS.timeOf(3600))
    }

    // ── The best time ──────────────────────────────────────────────────────────────────────────────────

    // Pin (no token): S-48 "`bestTimeText(seconds: Int?, texts: PlayTexts)` … returns `noTime` or `timeOf`"; the PlayTexts KDoc of
    // `noTime` ("what the best time reads while none is stored"); D10 (the best time on the solved view takes the one format).
    // No best time reads `noTime`; a best time of 0 s is a time ("0:00"), not none (decisions.md 2026-10-17 "Solve this puzzle
    // now is an ordinary solve": "a naive rule would show '0:00'" — the two are told apart); below and from an hour, the
    // best time reads as `timeOf` does.
    @Test
    fun theBestTimeReadsNoTimeWhileNoneIsStoredAndTheOneFormatOtherwise() {
        assertEquals("no best time: noTime", KIT_EN_TEXTS.noTime, bestTimeText(null, KIT_EN_TEXTS))
        assertEquals("a best time of 0 s is a time", "0:00", bestTimeText(0, KIT_EN_TEXTS))
        assertNotEquals("…and not noTime", KIT_EN_TEXTS.noTime, bestTimeText(0, KIT_EN_TEXTS))
        for (seconds in listOf(5, 75, 3599, 3600, 3900, 7200)) {
            assertEquals("bestTimeText($seconds) is S-48's form", kitTimeText(KIT_EN_TEXTS, seconds), bestTimeText(seconds, KIT_EN_TEXTS))
            assertEquals("bestTimeText($seconds) equals timeOf($seconds)", KIT_EN_TEXTS.timeOf(seconds), bestTimeText(seconds, KIT_EN_TEXTS))
        }
        assertEquals("D10: WO-004's \"65:00\" becomes \"1 h 5 min\"", "1 h 5 min", bestTimeText(3900, KIT_EN_TEXTS))
    }

    // ── ASCII digits under every device language ───────────────────────────────────────────────────────

    // Pin fixture (no token): D10 "Digits are ASCII under every device language" — the locale below must be one with other
    // digits on this JVM: `String.format` under it gives non-ASCII digits for the h-and-min format and for a "%d:%02d" m:ss, so
    // a `timeOf` that formats with the default locale fails the pin. The default is restored in every category.
    @Test
    fun theChosenDefaultLocaleFormatsNonAsciiDigitsOnThisJvm() {
        val before = listOf(Locale.getDefault(), Locale.getDefault(Locale.Category.DISPLAY), Locale.getDefault(Locale.Category.FORMAT))
        kitWithDefaultLocale(KIT_OTHER_DIGITS_LOCALE) {
            assertEquals("fixture: the default locale is switched", KIT_OTHER_DIGITS_LOCALE, Locale.getDefault(Locale.Category.FORMAT))
            val hm = KIT_HOURS_MINUTES.format(12, 34)
            assertTrue("fixture: String.format of the h-and-min text gives non-ASCII digits: ${kitCodePoints(hm)}", kitHasNonAsciiDigit(hm))
            val mss = "%d:%02d".format(12, 34)
            assertTrue("fixture: String.format of an m:ss gives non-ASCII digits: ${kitCodePoints(mss)}", kitHasNonAsciiDigit(mss))
            assertEquals("fixture: Locale.ROOT gives ASCII digits under the same default", "12 h 34 min", String.format(Locale.ROOT, KIT_HOURS_MINUTES, 12, 34))
        }
        assertEquals(
            "fixture: the default locale is restored in every category (the JVM is shared by the module's test classes)",
            before,
            listOf(Locale.getDefault(), Locale.getDefault(Locale.Category.DISPLAY), Locale.getDefault(Locale.Category.FORMAT)),
        )
    }

    // Pin (no token): D10 "Digits are ASCII under every device language (`Locale.ROOT`, as the 2026-10-19 counter row)"; S-48
    // ("hand-built ASCII"; "hoursMinutes.format(Locale.ROOT, h, m)"); REQ-047 statement ("in English otherwise": the English
    // time has ASCII digits). Under the Arabic-digit default, every time text — both forms, in both languages' texts, and the
    // best time — is the kit's ASCII text. The fixture check runs first, so the pin can never pass on a locale that gives ASCII
    // digits anyway.
    @Test
    fun timesKeepAsciiDigitsUnderADefaultLocaleWithOtherDigits() {
        kitWithDefaultLocale(KIT_OTHER_DIGITS_LOCALE) {
            val probe = KIT_HOURS_MINUTES.format(1, 5)
            assertTrue("fixture defect: $KIT_OTHER_DIGITS_LOCALE gives ASCII digits on this JVM: ${kitCodePoints(probe)}", kitHasNonAsciiDigit(probe))
            for ((language, texts) in listOf("en" to KIT_EN_TEXTS, "fi" to KIT_FI_TEXTS)) {
                for (seconds in KIT_DIGIT_SWEEP + KIT_BELOW_AN_HOUR.map { it.first } + KIT_FROM_AN_HOUR.map { it.first }) {
                    val shown = texts.timeOf(seconds)
                    assertFalse("$language timeOf($seconds) holds no non-ASCII digit: ${kitCodePoints(shown)}", kitHasNonAsciiDigit(shown))
                    assertEquals("$language timeOf($seconds) under $KIT_OTHER_DIGITS_LOCALE: ${kitCodePoints(shown)}", kitTimeText(texts, seconds), shown)
                }
                assertEquals("$language bestTimeText(45240) under $KIT_OTHER_DIGITS_LOCALE", kitTimeText(texts, 45240), bestTimeText(45240, texts))
            }
        }
    }
}
