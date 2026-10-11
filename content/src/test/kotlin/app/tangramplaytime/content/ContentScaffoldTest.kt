// scaffolding (P2): proves :content compiles and tests against :contracts; no REQ token
package app.tangramplaytime.content

import app.tangramplaytime.contracts.PuzzleKind
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentScaffoldTest {
    @Test
    fun seesTheContractsModule() {
        assertEquals(3, PuzzleKind.entries.size)
    }
}
