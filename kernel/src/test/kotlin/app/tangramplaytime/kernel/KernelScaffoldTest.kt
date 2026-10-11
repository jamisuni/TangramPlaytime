// scaffolding (P2): proves :kernel compiles and tests against :contracts; no REQ token
package app.tangramplaytime.kernel

import app.tangramplaytime.contracts.PieceId
import org.junit.Assert.assertEquals
import org.junit.Test

class KernelScaffoldTest {
    @Test
    fun seesTheContractsModule() {
        assertEquals(7, PieceId.entries.size)
    }
}
