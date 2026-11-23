package com.verifyetb.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.verifyetb.app.data.local.dao.RawSmsDao
import com.verifyetb.app.data.local.entity.RawSms
import com.verifyetb.app.data.local.entity.TransactionEntity
import com.verifyetb.app.data.prefs.SettingsRepository
import com.verifyetb.app.data.repository.TransactionRepository
import com.verifyetb.app.parser.EthiopianInstitutions
import com.verifyetb.app.parser.SmsParserPipeline
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On open, reads the inbox once and writes transactions in batches.
 * Later opens only read SMS newer than the last one already parsed.
 */
@Singleton
class SmsInboxImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rawSmsDao: RawSmsDao,
    private val settingsRepository: SettingsRepository,
    private val repository: TransactionRepository,
    private val pipeline: SmsParserPipeline,
    private val processor: SmsStagingProcessor,
) {
    /**
     * Import financial SMS with date > [sinceExclusive].
     * Pass 0 to read the inbox from the beginning.
     */
    suspend fun importSince(sinceExclusive: Long): Int = withContext(Dispatchers.IO) {
        if (!canReadSms()) {
            Log.i(TAG, "READ_SMS not granted")
            return@withContext 0
        }
        var inserted = 0
        var newestSeen = sinceExclusive
        val batch = ArrayList<RawSms>(BATCH)
        try {
            val selection = if (sinceExclusive > 0L) "${Telephony.Sms.DATE} > ?" else null
            val args = if (sinceExclusive > 0L) arrayOf(sinceExclusive.toString()) else null
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                selection,
                args,
                "${Telephony.Sms.DATE} ASC",
            )?.use { cursor ->
                val addrIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                while (cursor.moveToNext()) {
                    val sender = cursor.getString(addrIdx)?.trim().orEmpty()
                    val body = cursor.getString(bodyIdx).orEmpty()
                    val date = cursor.getLong(dateIdx)
                    if (date > newestSeen) newestSeen = date
                    if (sender.isEmpty() || body.isBlank() || !looksFinancial(body)) continue
                    batch += RawSms(sender = sender, body = body, date = date)
                    if (batch.size >= BATCH) {
                        inserted += flush(batch)
                        batch.clear()
                    }
                }
            } ?: return@withContext 0
            if (batch.isNotEmpty()) inserted += flush(batch)
            if (newestSeen > sinceExclusive) {
                settingsRepository.setLastInboxSmsDate(newestSeen)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Inbox import failed", t)
        }
        inserted
    }

    /**
     * Parse the inbox into transactions immediately.
     * Does not wait for opening balances or a background worker.
     */
    suspend fun syncNew(): Int = processor.exclusive {
        if (!canReadSms()) return@exclusive 0
        settingsRepository.consumeInboxParseV2()
        settingsRepository.consumeInboxParseV3()
        settingsRepository.consumeInboxParseV4()
        if (settingsRepository.consumeInboxParseV5()) {
            repository.deleteSmsForReparsedWallets()
        }
        val since = settingsRepository.getLastInboxSmsDate()
        parseInbox(sinceExclusive = since)
    }

    private suspend fun parseInbox(sinceExclusive: Long): Int {
        var saved = 0
        var newestSeen = sinceExclusive
        val batch = ArrayList<TransactionEntity>(BATCH)
        val foundInstitutions = HashSet<String>()
        val selection = if (sinceExclusive > 0L) "${Telephony.Sms.DATE} > ?" else null
        val args = if (sinceExclusive > 0L) arrayOf(sinceExclusive.toString()) else null
        try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                selection,
                args,
                "${Telephony.Sms.DATE} DESC",
            )?.use { cursor ->
                val addrIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                while (cursor.moveToNext()) {
                    val date = cursor.getLong(dateIdx)
                    if (date > newestSeen) newestSeen = date
                    val body = cursor.getString(bodyIdx).orEmpty()
                    if (body.length < 12 || !containsDigit(body)) continue
                    val sender = cursor.getString(addrIdx)?.trim().orEmpty()
                    if (sender.isEmpty()) continue
                    val parsed = pipeline.parse(sender, body, date) ?: continue
                    val matched = EthiopianInstitutions.byDisplayName(parsed.institution)
                    matched?.id?.let { foundInstitutions += it }
                    batch += TransactionEntity(
                        type = parsed.type,
                        amount = parsed.amount,
                        feeAmount = parsed.feeAmount,
                        statedBalance = parsed.statedBalance,
                        institution = matched?.displayName ?: parsed.institution,
                        counterparty = parsed.counterparty,
                        reference = parsed.reference,
                        reason = parsed.reason,
                        date = date,
                        source = TransactionEntity.SOURCE_SMS,
                    )
                    if (batch.size >= BATCH) {
                        saved += flushParsed(batch, foundInstitutions)
                        batch.clear()
                        foundInstitutions.clear()
                    }
                }
            } ?: return 0
            if (batch.isNotEmpty()) saved += flushParsed(batch, foundInstitutions)
            if (newestSeen > sinceExclusive) {
                settingsRepository.setLastInboxSmsDate(newestSeen)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Inbox parse failed", t)
        }
        return saved
    }

    private suspend fun flushParsed(batch: List<TransactionEntity>, institutionIds: Set<String>): Int {
        settingsRepository.addSelectedInstitutionIds(institutionIds)
        return repository.insertParsedAll(batch)
    }

    private fun containsDigit(body: String): Boolean {
        for (ch in body) {
            if (ch.isDigit()) return true
        }
        return false
    }

    private suspend fun flush(batch: List<RawSms>): Int {
        if (batch.isEmpty()) return 0
        return rawSmsDao.insertAll(batch).count { it != -1L }
    }

    private fun canReadSms(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    private fun looksFinancial(body: String): Boolean {
        val lower = body.lowercase()
        return FINANCIAL_HINTS.any { lower.contains(it) }
    }

    companion object {
        private const val TAG = "SmsInboxImporter"
        private const val BATCH = 300
        private val FINANCIAL_HINTS = listOf(
            "etb", "birr", "telebirr", "cbe", "debited", "credited",
            "debit", "credit", "balance", "transferred", "withdrawn", "received",
            "br.", "txn", "transaction", "account", "acct",
            "m-pesa", "mpesa", "amole", "kacha", "ebirr", "hellocash",
            "awash", "dashen", "abyssinia", "coopay", "mbirr", "fee",
            "commission", "service charge", "paid", "sent",
        )
    }
}
