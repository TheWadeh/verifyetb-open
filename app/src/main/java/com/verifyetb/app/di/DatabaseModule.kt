package com.verifyetb.app.di

import android.content.Context
import androidx.room.Room
import com.verifyetb.app.data.local.AppDatabase
import com.verifyetb.app.data.local.dao.AccountBalanceDao
import com.verifyetb.app.data.local.dao.CategoryDao
import com.verifyetb.app.data.local.dao.RawSmsDao
import com.verifyetb.app.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "verifyetb.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideRawSmsDao(db: AppDatabase): RawSmsDao = db.rawSmsDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideAccountBalanceDao(db: AppDatabase): AccountBalanceDao = db.accountBalanceDao()
}
