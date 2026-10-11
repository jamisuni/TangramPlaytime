// scaffolding (P2): proves :store compiles and tests against :contracts; no REQ token
package app.tangramplaytime.store

import app.tangramplaytime.contracts.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreScaffoldTest {
    @Test
    fun seesTheContractsModule() {
        assertEquals(3, PuzzleState.entries.size)
    }
}
