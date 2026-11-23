package com.verifyetb.app.util

import com.verifyetb.app.data.local.entity.TransactionEntity

/**
 * Live account balance.
 *
 * Opening + credits − debits − fees, unless a bank/wallet SMS stated a
 * balance — then that snapshot is the base and only later transactions apply.
 * Cash never uses SMS stated balances.
 */
object BalanceMath {
    fun compute(
        opening: Double,
        txs: List<TransactionEntity>,
        isCash: Boolean,
    ): Double {
        if (isCash) {
            return opening + netOf(txs)
        }
        val anchor = txs
            .filter { plausible(it.statedBalance) }
            .maxWithOrNull(compareBy({ it.date }, { it.id }))
            ?: return opening + netOf(txs)
        val stated = anchor.statedBalance ?: return opening + netOf(txs)
        val after = txs.filter { tx ->
            tx.date > anchor.date || (tx.date == anchor.date && tx.id > anchor.id)
        }
        return stated + netOf(after)
    }

    private fun netOf(txs: List<TransactionEntity>): Double {
        val usable = txs.filter { plausible(it.amount) }
        val credits = usable.filter { it.type == TransactionEntity.TYPE_CREDIT }.sumOf { it.amount }
        val debits = usable.filter { it.type == TransactionEntity.TYPE_DEBIT }.sumOf { it.amount }
        val fees = usable.sumOf { fee -> if (fee.feeAmount in 0.0..99_999.0) fee.feeAmount else 0.0 }
        return credits - debits - fees
    }

    /** Phone numbers parsed as amounts are 10+ digits and must not move the balance. */
    private fun plausible(amount: Double?): Boolean {
        if (amount == null || amount.isNaN()) return false
        return amount in 0.0..99_999_999.0
    }
}
