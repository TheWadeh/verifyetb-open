package com.verifyetb.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verifyetb.app.data.local.entity.RawSms
import kotlinx.coroutines.flow.Flow

@Dao
interface RawSmsDao {

    /**
     * Insert a staged SMS. Duplicate (sender, body, date) is ignored —
     * that unique index is the primary dedup gate.
     *
     * @return row id of the insert, or -1 when conflict was ignored.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rawSms: RawSms): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<RawSms>): List<Long>

    @Query(
        """
        UPDATE raw_sms
        SET status = 'PENDING', error_message = NULL
        WHERE status = 'SKIPPED'
          AND error_message LIKE 'Before opening balances%'
        """
    )
    suspend fun reopenSkippedHistory(): Int

    @Query("SELECT * FROM raw_sms WHERE status = :status ORDER BY date ASC")
    suspend fun getByStatus(status: String): List<RawSms>

    @Query("SELECT * FROM raw_sms ORDER BY date DESC")
    fun observeAll(): Flow<List<RawSms>>

    @Query("SELECT COUNT(*) FROM raw_sms")
    fun observeCount(): Flow<Int>

    @Query(
        """
        UPDATE raw_sms
        SET status = :status, error_message = :errorMessage
        WHERE id = :id
        """
    )
    suspend fun updateStatus(id: Long, status: String, errorMessage: String? = null)

    @Query(
        """
        UPDATE raw_sms
        SET status = :toStatus, error_message = :errorMessage
        WHERE status = :fromStatus
        """
    )
    suspend fun updateStatusBulk(
        fromStatus: String,
        toStatus: String,
        errorMessage: String? = null,
    ): Int

    @Query("SELECT * FROM raw_sms WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RawSms?
}
