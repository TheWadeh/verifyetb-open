package com.verifyetb.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.verifyetb.app.data.local.dao.AccountBalanceDao
import com.verifyetb.app.data.local.dao.CategoryDao
import com.verifyetb.app.data.local.dao.RawSmsDao
import com.verifyetb.app.data.local.dao.TransactionDao
import com.verifyetb.app.data.local.entity.AccountBalanceEntity
import com.verifyetb.app.data.local.entity.CategoryEntity
import com.verifyetb.app.data.local.entity.RawSms
import com.verifyetb.app.data.local.entity.TransactionEntity

@Database(
    entities = [
        RawSms::class,
        TransactionEntity::class,
        CategoryEntity::class,
        AccountBalanceEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rawSmsDao(): RawSmsDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountBalanceDao(): AccountBalanceDao
}
