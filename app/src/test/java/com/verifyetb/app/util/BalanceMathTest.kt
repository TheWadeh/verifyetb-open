package com.verifyetb.app.util

import com.verifyetb.app.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceMathTest {

    @Test
    fun openingPlusNetWhenNoStated() {
        val txs = listOf(
            tx(1, TransactionEntity.TYPE_CREDIT, 100.0, 0.0, null, 1),
            tx(2, TransactionEntity.TYPE_DEBIT, 30.0, 2.0, null, 2),
        )
        assertEquals(68.0, BalanceMath.compute(0.0, txs, isCash = false), 0.001)
    }

    @Test
    fun statedRebaseThenLaterTxsApply() {
        val txs = listOf(
            tx(1, TransactionEntity.TYPE_DEBIT, 50.0, 0.0, 500.0, 10),
            tx(2, TransactionEntity.TYPE_DEBIT, 20.0, 1.0, null, 20),
            tx(3, TransactionEntity.TYPE_CREDIT, 10.0, 0.0, null, 30),
        )
        // stated 500 at t=10; then -20 -1 +10 = 489
        assertEquals(489.0, BalanceMath.compute(1000.0, txs, isCash = false), 0.001)
    }

    @Test
    fun cashIgnoresStated() {
        val txs = listOf(
            tx(1, TransactionEntity.TYPE_DEBIT, 50.0, 0.0, 999.0, 10),
        )
        assertEquals(50.0, BalanceMath.compute(100.0, txs, isCash = true), 0.001)
    }

    private fun tx(
        id: Long,
        type: String,
        amount: Double,
        fee: Double,
        stated: Double?,
        date: Long,
    ) = TransactionEntity(
        id = id,
        type = type,
        amount = amount,
        feeAmount = fee,
        statedBalance = stated,
        institution = "CBE",
        date = date,
        source = TransactionEntity.SOURCE_SMS,
    )
}
