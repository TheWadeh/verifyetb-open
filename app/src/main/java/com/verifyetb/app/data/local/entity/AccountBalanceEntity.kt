package com.verifyetb.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account_balances")
data class AccountBalanceEntity(
    @PrimaryKey
    val institutionId: String,
    val openingBalance: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis(),
)
