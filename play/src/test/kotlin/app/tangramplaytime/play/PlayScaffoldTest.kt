// scaffolding (P2): proves :play compiles and tests against :contracts; no REQ token
package app.tangramplaytime.play

import app.tangramplaytime.contracts.Turn
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayScaffoldTest {
    @Test
    fun seesTheContractsModule() {
        assertEquals(Turn(7), Turn(0).previous())
    }
}
