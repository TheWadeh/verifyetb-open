package com.verifyetb.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verifyetb.app.data.local.entity.AccountBalanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountBalanceDao {
    @Query("SELECT * FROM account_balances")
    fun observeAll(): Flow<List<AccountBalanceEntity>>

    @Query("SELECT * FROM account_balances WHERE institutionId = :id LIMIT 1")
    suspend fun get(id: String): AccountBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountBalanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(accounts: List<AccountBalanceEntity>)
}
