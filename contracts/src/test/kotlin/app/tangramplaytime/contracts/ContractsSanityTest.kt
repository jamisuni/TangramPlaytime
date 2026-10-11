// scaffolding: compile + toolchain proof (P2); no REQ token
package app.tangramplaytime.contracts

import org.junit.Assert.assertEquals
import org.junit.Test

class ContractsSanityTest {
    @Test
    fun pieceIdDeclarationOrderIsTheTrayOrder() {
        assertEquals(
            listOf(PieceId.LT1, PieceId.LT2, PieceId.MT, PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
            PieceId.entries.toList(),
        )
    }

    @Test
    fun turnSevenNextWrapsToZero() {
        assertEquals(Turn(0), Turn(7).next())
    }
}
