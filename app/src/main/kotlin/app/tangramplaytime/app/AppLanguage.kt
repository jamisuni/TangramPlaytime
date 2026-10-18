// WO-006 TASK-032 · seam S-36, one language rule (tasks.md § WO-006 › Seams) · REQ-047 statement, rules 1–2, A1, A2;
// REQ-012 rule "The puzzle's title is shown in the game's language" through S-11's `titleLanguage` · architecture.md G-10,
// O-06 · decisions.md 2026-10-17 "One language rule for strings and titles" (Q7), which replaces the 2026-10-12 TASK-015
// row's split (titles by the first locale, strings by the whole locale list: the flagged mix).
package app.tangramplaytime.app

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * The game's one language rule (S-36; REQ-047): the device's **first** locale decides, and the rest of its locale list is
 * never consulted. `MainActivity.attachBaseContext` wraps its base context with [wrap], so every string the activity, `:play`
 * and the DEV aid resolve through it comes from `values-fi/` for `fi` and from `values/` for anything else, and the
 * activity's `PlayTexts.titleLanguage` is [of] the same first locale, so a title is `title.fi` or `title.en` (S-11).
 */
internal object AppLanguage {

    /** Finnish, the one language shipped besides English (REQ-047 rule: "Languages shipped: `fi` and `en`"). */
    private const val FINNISH = "fi"

    /** English: the Contract wording and every other device language's (REQ-047 rule: "any other device language gets `en`"). */
    private const val ENGLISH = "en"

    /** `"fi"` when the language of [first] (the device's first locale) is Finnish, else `"en"` (REQ-047 statement and rule). */
    fun of(first: Locale): String = if (first.language == FINNISH) FINNISH else ENGLISH

    /**
     * [base] with its locale list replaced by the one locale [of] its first locale names (`createConfigurationContext`), so a
     * list such as `[sv-SE, fi-FI]` resolves no string in Finnish (REQ-047 A2; Q7 row).
     *
     * The override is a delta that sets the locales alone: `Configuration()` starts with every field undefined, and the
     * framework lays the override over the activity's current configuration whenever that changes, so the screen size, the
     * density, the orientation and the night mode keep following the device. A full copy of [base]'s configuration would
     * pin those fields at their values of this moment.
     */
    fun wrap(base: Context): Context {
        val language = of(base.resources.configuration.locales[0])
        val locales = Configuration().apply { setLocales(LocaleList(Locale.forLanguageTag(language))) }
        return base.createConfigurationContext(locales)
    }
}
