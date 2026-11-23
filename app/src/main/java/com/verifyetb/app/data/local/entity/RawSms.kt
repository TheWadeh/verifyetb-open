package com.verifyetb.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Staging table for inbound SMS.
 *
 * The BroadcastReceiver writes here and does nothing else.
 * Parsing / promotion to a transaction row happens in a worker.
 *
 * Dedup rule #1 (non-negotiable): unique on (sender, body, date).
 */
@Entity(
    tableName = "raw_sms",
    indices = [
        Index(
            value = ["sender", "body", "date"],
            unique = true,
            name = "index_raw_sms_sender_body_date"
        )
    ]
)
data class RawSms(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Address / shortcode the SMS arrived from (e.g. CBE, 127). */
    @ColumnInfo(name = "sender")
    val sender: String,

    /** Full SMS body as received. */
    @ColumnInfo(name = "body")
    val body: String,

    /** SMS timestamp from the PDU (epoch millis). */
    @ColumnInfo(name = "date")
    val date: Long,

    /** Wall-clock when we inserted into staging. */
    @ColumnInfo(name = "received_at")
    val receivedAt: Long = System.currentTimeMillis(),

    /**
     * Processing state for the worker pipeline.
     * PENDING → PROCESSED | FAILED | SKIPPED
     */
    @ColumnInfo(name = "status")
    val status: String = STATUS_PENDING,

    /** Optional parser / worker error message when status is FAILED. */
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_PROCESSED = "PROCESSED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_SKIPPED = "SKIPPED"
    }
}
