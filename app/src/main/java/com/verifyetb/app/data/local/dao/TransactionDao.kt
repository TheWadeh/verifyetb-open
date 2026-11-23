package com.verifyetb.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.verifyetb.app.data.local.entity.CategoryEntity
import com.verifyetb.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tx: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(txs: List<TransactionEntity>): List<Long>

    @Query(
        """
        SELECT * FROM transactions
        WHERE institution = 'CBE' AND amount = :amount
          AND (reference = :reference OR reference = :legacyReference)
        LIMIT 1
        """
    )
    suspend fun findMisfiledCbe(amount: Double, reference: String, legacyReference: String): TransactionEntity?

    @Query(
        """
        SELECT id FROM transactions
        WHERE institution = 'CBE Birr' AND amount = :amount AND reference = :reference
        LIMIT 1
        """
    )
    suspend fun findCbeBirrId(amount: Double, reference: String): Long?

    @Query(
        """
        DELETE FROM transactions
        WHERE institution = :institution AND amount = :amount AND reference = :reference
        """
    )
    suspend fun deleteByInstitutionAmountRef(institution: String, amount: Double, reference: String): Int

    /** Drop SMS rows for wallets whose templates were re-parsed. Manual entries stay. */
    @Query(
        """
        DELETE FROM transactions
        WHERE source = 'SMS' AND institution IN ('CBE Birr', 'M-Pesa', 'M-PESA', 'MPESA', 'CBEBirr')
        """
    )
    suspend fun deleteSmsForReparsedWallets()

    @Update
    suspend fun update(tx: TransactionEntity)

    @Delete
    suspend fun delete(tx: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE raw_sms_id = :rawSmsId LIMIT 1")
    suspend fun getByRawSmsId(rawSmsId: Long): TransactionEntity?

    @Query(
        """
        SELECT * FROM transactions
        WHERE reference = :reference AND amount = :amount AND institution = :institution
        LIMIT 1
        """
    )
    suspend fun getByDedupKey(
        reference: String,
        amount: Double,
        institution: String,
    ): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE institution = :institution
        ORDER BY date DESC, id DESC
        LIMIT :limit
        """
    )
    fun observeRecentForInstitution(institution: String, limit: Int): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT stated_balance FROM transactions
        WHERE stated_balance IS NOT NULL AND institution = :institution
        ORDER BY date DESC, id DESC
        LIMIT 1
        """
    )
    fun observeLatestStatedBalanceFor(institution: String): Flow<Double?>

    @Query(
        """
        SELECT stated_balance FROM transactions
        WHERE stated_balance IS NOT NULL
        ORDER BY date DESC, id DESC
        LIMIT 1
        """
    )
    fun observeLatestStatedBalance(): Flow<Double?>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = :type
          AND date >= :fromInclusive AND date < :toExclusive
          AND (:institution = '' OR institution = :institution)
        """
    )
    fun observeSumByTypeInRange(
        type: String,
        fromInclusive: Long,
        toExclusive: Long,
        institution: String = "",
    ): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(fee_amount), 0) FROM transactions
        WHERE date >= :fromInclusive AND date < :toExclusive
          AND (:institution = '' OR institution = :institution)
        """
    )
    fun observeFeeSumInRange(
        fromInclusive: Long,
        toExclusive: Long,
        institution: String = "",
    ): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE 0 END), 0)
             - COALESCE(SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE 0 END), 0)
             - COALESCE(SUM(fee_amount), 0)
        FROM transactions
        WHERE institution = :institution
        """
    )
    fun observeNetForInstitution(institution: String): Flow<Double>

    @Query(
        """
        SELECT * FROM transactions
        WHERE (:query = '' OR institution LIKE '%' || :query || '%'
            OR IFNULL(counterparty, '') LIKE '%' || :query || '%'
            OR IFNULL(reason, '') LIKE '%' || :query || '%'
            OR IFNULL(reference, '') LIKE '%' || :query || '%')
        AND (:type = '' OR type = :type)
        AND (:institution = '' OR institution = :institution)
        AND (:fromInclusive = 0 OR date >= :fromInclusive)
        AND (:toExclusive = 0 OR date < :toExclusive)
        AND (:feesOnly = 0 OR fee_amount > 0)
        ORDER BY date DESC, id DESC
        """
    )
    fun observeFiltered(
        query: String,
        type: String,
        institution: String,
        fromInclusive: Long,
        toExclusive: Long,
        feesOnly: Int = 0,
    ): Flow<List<TransactionEntity>>

    @Query("SELECT DISTINCT institution FROM transactions ORDER BY institution ASC")
    fun observeInstitutions(): Flow<List<String>>

    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE source = 'SMS' AND (reason IS NULL OR TRIM(reason) = '')
        """
    )
    fun observeUnresolvedCount(): Flow<Int>

    @Query(
        """
        SELECT id FROM transactions
        WHERE source = 'SMS' AND (reason IS NULL OR TRIM(reason) = '')
        ORDER BY date DESC, id DESC
        LIMIT 1
        """
    )
    fun observeFirstUnresolvedId(): Flow<Long?>
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}
