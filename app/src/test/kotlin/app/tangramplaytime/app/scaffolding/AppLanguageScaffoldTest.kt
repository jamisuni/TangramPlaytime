// scaffolding — WO-006 TASK-032's own disposable check of S-36's `AppLanguage.of` (tasks.md § WO-006 › Seams): `"fi"` when
// the language of the device's first locale is Finnish, else `"en"`. Pure JVM (`java.util.Locale`); `wrap` needs a real
// Context and is proven on the device by the kit's `LanguageKitTest`. No token.
package app.tangramplaytime.app.scaffolding

import app.tangramplaytime.app.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AppLanguageScaffoldTest {

    private fun of(tag: String): String = AppLanguage.of(Locale.forLanguageTag(tag))

    @Test
    fun aFinnishFirstLocaleIsFinnishWhateverItsRegion() {
        assertEquals("fi", of("fi"))
        assertEquals("fi", of("fi-FI"))
        assertEquals("fi", of("fi-SE"))
    }

    @Test
    fun everyOtherFirstLocaleIsEnglish() {
        assertEquals("en", of("en-US"))
        assertEquals("en", of("en"))
        assertEquals("en", of("sv-SE"))
        assertEquals("en", of("sv-FI"))
        assertEquals("en", of("se-FI")) // Northern Sami: a language of Finland, not Finnish
        assertEquals("en", of("und"))
    }
}
