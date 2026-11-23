package com.verifyetb.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["date"]),
        Index(value = ["institution"]),
        Index(value = ["type"]),
        Index(
            value = ["reference", "amount", "institution"],
            unique = true,
            name = "index_tx_heuristic_dedup",
        ),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** DEBIT or CREDIT */
    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "amount")
    val amount: Double,

    /** Transaction fee when money is sent (from SMS or manual). */
    @ColumnInfo(name = "fee_amount")
    val feeAmount: Double = 0.0,

    /** Balance stated in the SMS — used to rebase that account when present. */
    @ColumnInfo(name = "stated_balance")
    val statedBalance: Double? = null,

    @ColumnInfo(name = "institution")
    val institution: String,

    @ColumnInfo(name = "counterparty")
    val counterparty: String? = null,

    @ColumnInfo(name = "reference")
    val reference: String? = null,

    /** Reason captured from SMS or typed by the user. */
    @ColumnInfo(name = "reason")
    val reason: String? = null,

    @ColumnInfo(name = "category")
    val category: String? = null,

    @ColumnInfo(name = "date")
    val date: Long,

    @ColumnInfo(name = "raw_sms_id")
    val rawSmsId: Long? = null,

    @ColumnInfo(name = "source")
    val source: String = SOURCE_SMS,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val TYPE_DEBIT = "DEBIT"
        const val TYPE_CREDIT = "CREDIT"
        const val SOURCE_SMS = "SMS"
        const val SOURCE_MANUAL = "MANUAL"
    }

    val needsNote: Boolean get() = reason.isNullOrBlank()
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
)
