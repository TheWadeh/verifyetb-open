package com.verifyetb.app.data.repository

import com.verifyetb.app.data.local.dao.AccountBalanceDao
import com.verifyetb.app.data.local.dao.CategoryDao
import com.verifyetb.app.data.local.dao.RawSmsDao
import com.verifyetb.app.data.local.dao.TransactionDao
import com.verifyetb.app.data.local.entity.AccountBalanceEntity
import com.verifyetb.app.data.local.entity.CategoryEntity
import com.verifyetb.app.data.local.entity.RawSms
import com.verifyetb.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val rawSmsDao: RawSmsDao,
    private val accountBalanceDao: AccountBalanceDao,
) {
    fun observeAll(): Flow<List<TransactionEntity>> = transactionDao.observeAll()

    fun observeRecent(limit: Int = 12): Flow<List<TransactionEntity>> =
        transactionDao.observeRecent(limit)

    fun observeRecentForInstitution(institution: String, limit: Int = 20) =
        transactionDao.observeRecentForInstitution(institution, limit)

    fun observeById(id: Long): Flow<TransactionEntity?> = transactionDao.observeById(id)

    fun observeLatestBalance(): Flow<Double?> = transactionDao.observeLatestStatedBalance()

    fun observeSumByType(
        type: String,
        from: Long,
        to: Long,
        institution: String = "",
    ): Flow<Double> = transactionDao.observeSumByTypeInRange(type, from, to, institution)

    fun observeFeeSum(from: Long, to: Long, institution: String = ""): Flow<Double> =
        transactionDao.observeFeeSumInRange(from, to, institution)

    fun observeMonthSpend(from: Long, to: Long): Flow<Double> =
        observeSumByType(TransactionEntity.TYPE_DEBIT, from, to)

    fun observeMonthReceived(from: Long, to: Long): Flow<Double> =
        observeSumByType(TransactionEntity.TYPE_CREDIT, from, to)

    fun observeFiltered(
        query: String,
        type: String,
        institution: String,
        fromInclusive: Long,
        toExclusive: Long,
        feesOnly: Boolean = false,
    ): Flow<List<TransactionEntity>> = transactionDao.observeFiltered(
        query = query.trim(),
        type = type,
        institution = institution,
        fromInclusive = fromInclusive,
        toExclusive = toExclusive,
        feesOnly = if (feesOnly) 1 else 0,
    )

    fun observeInstitutions(): Flow<List<String>> = transactionDao.observeInstitutions()

    fun observeUnresolvedCount(): Flow<Int> = transactionDao.observeUnresolvedCount()

    fun observeFirstUnresolvedId(): Flow<Long?> = transactionDao.observeFirstUnresolvedId()

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    fun observeRawSms(): Flow<List<RawSms>> = rawSmsDao.observeAll()

    fun observeAccountBalances(): Flow<List<AccountBalanceEntity>> = accountBalanceDao.observeAll()

    /**
     * Live balance for an account keyed by catalog id, using display name on transactions.
     * Stated SMS balance rebases the account; later txs still apply.
     */
    fun observeLiveBalance(institutionId: String, displayName: String): Flow<Double> {
        val openingFlow = accountBalanceDao.observeAll().map { list ->
            list.find { it.institutionId == institutionId }?.openingBalance ?: 0.0
        }
        val isCash = institutionId == "cash" || displayName.equals("Cash", ignoreCase = true)
        return combine(openingFlow, transactionDao.observeAll()) { opening, all ->
            val txs = all.filter { it.institution.equals(displayName, ignoreCase = true) }
            com.verifyetb.app.util.BalanceMath.compute(opening, txs, isCash)
        }
    }

    fun observeAccountBalance(institutionId: String): Flow<Double> {
        val displayName = com.verifyetb.app.parser.EthiopianInstitutions.byId(institutionId)
            ?.displayName ?: institutionId
        return observeLiveBalance(institutionId, displayName)
    }

    suspend fun upsertOpeningBalance(institutionId: String, openingBalance: Double) {
        accountBalanceDao.upsert(
            AccountBalanceEntity(
                institutionId = institutionId,
                openingBalance = openingBalance,
            )
        )
    }

    suspend fun upsertOpeningBalances(map: Map<String, Double>) {
        accountBalanceDao.upsertAll(
            map.map { (id, bal) ->
                AccountBalanceEntity(institutionId = id, openingBalance = bal)
            }
        )
    }

    suspend fun addManual(
        type: String,
        amount: Double,
        institution: String,
        counterparty: String?,
        reason: String?,
        category: String?,
        date: Long,
        feeAmount: Double = 0.0,
    ): Long {
        return transactionDao.insert(
            TransactionEntity(
                type = type,
                amount = amount,
                feeAmount = feeAmount,
                institution = institution.ifBlank { "Cash" },
                counterparty = counterparty?.ifBlank { null },
                reason = reason?.ifBlank { null },
                category = category?.ifBlank { null },
                date = date,
                source = TransactionEntity.SOURCE_MANUAL,
                reference = "manual-${date}-${amount}-${type}-${System.nanoTime()}",
            )
        )
    }

    suspend fun updateNote(id: Long, reason: String?, category: String?) {
        val existing = transactionDao.getById(id) ?: return
        transactionDao.update(
            existing.copy(
                reason = reason?.ifBlank { null },
                category = category?.ifBlank { null },
            )
        )
    }

    suspend fun delete(id: Long) {
        val existing = transactionDao.getById(id) ?: return
        transactionDao.delete(existing)
    }

    suspend fun deleteSmsForReparsedWallets() {
        transactionDao.deleteSmsForReparsedWallets()
    }

    suspend fun insertParsed(tx: TransactionEntity): Long {
        dropBankDebitFiledOnWallet(tx)
        if (absorbMisfiledCbe(tx)) return -1L
        return transactionDao.insert(tx)
    }

    /** @return how many new rows were stored. Duplicates are ignored. */
    suspend fun insertParsedAll(txs: List<TransactionEntity>): Int {
        if (txs.isEmpty()) return 0
        val fresh = ArrayList<TransactionEntity>(txs.size)
        for (tx in txs) {
            dropBankDebitFiledOnWallet(tx)
            if (!absorbMisfiledCbe(tx)) fresh += tx
        }
        if (fresh.isEmpty()) return 0
        return transactionDao.insertAll(fresh).count { it != -1L }
    }

    /**
     * Older builds stored CBE Birr SMS on the CBE bank account.
     * Move that row instead of inserting a second one.
     * @return true when no new insert is needed
     */
    private suspend fun absorbMisfiledCbe(tx: TransactionEntity): Boolean {
        if (tx.institution != "CBE Birr") return false
        val reference = tx.reference ?: return false
        if (transactionDao.findCbeBirrId(tx.amount, reference) != null) return true
        val legacy = if (reference.startsWith("cbebirr-")) {
            "cbe-" + reference.removePrefix("cbebirr-")
        } else {
            reference
        }
        val misfiled = transactionDao.findMisfiledCbe(tx.amount, reference, legacy) ?: return false
        transactionDao.update(
            misfiled.copy(
                institution = "CBE Birr",
                reference = reference,
                feeAmount = if (tx.feeAmount > 0.0) tx.feeAmount else misfiled.feeAmount,
                statedBalance = tx.statedBalance ?: misfiled.statedBalance,
                counterparty = tx.counterparty ?: misfiled.counterparty,
                reason = misfiled.reason ?: tx.reason,
            ),
        )
        return true
    }

    /** A CBE account debit that only mentioned CBE Birr should not stay on the wallet. */
    private suspend fun dropBankDebitFiledOnWallet(tx: TransactionEntity) {
        if (tx.institution != "CBE") return
        val reference = tx.reference ?: return
        transactionDao.deleteByInstitutionAmountRef("CBE Birr", tx.amount, reference)
        if (reference.startsWith("cbe-")) {
            transactionDao.deleteByInstitutionAmountRef(
                "CBE Birr",
                tx.amount,
                "cbebirr-" + reference.removePrefix("cbe-"),
            )
        }
    }

    /**
     * When an SMS is reprocessed, refresh fee / stated balance on the existing row.
     * @return true if a transaction was found and updated
     */
    suspend fun refreshParsedFields(tx: TransactionEntity): Boolean {
        val existing = tx.rawSmsId?.let { transactionDao.getByRawSmsId(it) }
            ?: tx.reference?.let {
                transactionDao.getByDedupKey(it, tx.amount, tx.institution)
            }
            ?: return false
        val updated = existing.copy(
            feeAmount = if (tx.feeAmount > 0.0) tx.feeAmount else existing.feeAmount,
            statedBalance = tx.statedBalance ?: existing.statedBalance,
            counterparty = when {
                !tx.counterparty.isNullOrBlank() -> tx.counterparty
                existing.counterparty != null &&
                    com.verifyetb.app.parser.MoneyParse.isJunkLabel(existing.counterparty) -> null
                else -> existing.counterparty
            },
            reason = existing.reason ?: tx.reason,
        )
        if (updated != existing) {
            transactionDao.update(updated)
            return true
        }
        return false
    }

    suspend fun ensureDefaultCategories() {
        if (categoryDao.count() > 0) return
        listOf(
            "Food", "Transport", "Airtime", "Utilities",
            "Rent", "Transfer", "Shopping", "Salary", "Other",
        ).forEach { categoryDao.insert(CategoryEntity(name = it)) }
    }

    suspend fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) categoryDao.insert(CategoryEntity(name = trimmed))
    }

    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.delete(category)
}
