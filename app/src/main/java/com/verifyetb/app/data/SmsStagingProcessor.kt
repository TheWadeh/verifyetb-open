package com.verifyetb.app.data

import android.util.Log
import com.verifyetb.app.data.local.dao.RawSmsDao
import com.verifyetb.app.data.local.entity.RawSms
import com.verifyetb.app.data.local.entity.TransactionEntity
import com.verifyetb.app.data.prefs.SettingsRepository
import com.verifyetb.app.data.repository.TransactionRepository
import com.verifyetb.app.parser.EthiopianInstitutions
import com.verifyetb.app.parser.InstitutionKind
import com.verifyetb.app.parser.SmsParserPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns staged bank/wallet SMS into transactions. Runs on the caller thread
 * pool so a fresh inbox shows up without waiting for WorkManager.
 */
@Singleton
class SmsStagingProcessor @Inject constructor(
    private val rawSmsDao: RawSmsDao,
    private val repository: TransactionRepository,
    private val pipeline: SmsParserPipeline,
    private val settingsRepository: SettingsRepository,
) {
    private val gate = Mutex()

    suspend fun <T> exclusive(block: suspend () -> T): T = gate.withLock { block() }

    suspend fun processPending(): Int = gate.withLock { processPendingBody() }

    internal suspend fun processPendingBody(): Int = withContext(Dispatchers.IO) {
        if (!settingsRepository.balancesSet.first()) return@withContext 0
        if (!settingsRepository.accountsPicked.first()) return@withContext 0
        val selected = settingsRepository.getSelectedInstitutionIds()
        val selectedBanksWallets = selected - EthiopianInstitutions.cash.id
        val pending = rawSmsDao.getByStatus(RawSms.STATUS_PENDING)
        var saved = 0
        for (sms in pending) {
            try {
                val parsed = pipeline.parse(sms.sender, sms.body, sms.date)
                if (parsed == null) {
                    rawSmsDao.updateStatus(sms.id, RawSms.STATUS_SKIPPED, "Unrecognized SMS format")
                    continue
                }
                val matched = EthiopianInstitutions.byDisplayName(parsed.institution)
                    ?: EthiopianInstitutions.match(sms.sender, sms.body)
                if (matched?.kind == InstitutionKind.CASH) {
                    rawSmsDao.updateStatus(sms.id, RawSms.STATUS_SKIPPED, "Cash is manual-only")
                    continue
                }
                if (matched != null && matched.id !in selectedBanksWallets) {
                    settingsRepository.addSelectedInstitutionIds(setOf(matched.id))
                }
                val candidate = TransactionEntity(
                    type = parsed.type,
                    amount = parsed.amount,
                    feeAmount = parsed.feeAmount,
                    statedBalance = parsed.statedBalance,
                    institution = matched?.displayName ?: parsed.institution,
                    counterparty = parsed.counterparty,
                    reference = parsed.reference,
                    reason = parsed.reason,
                    date = sms.date,
                    rawSmsId = sms.id,
                    source = TransactionEntity.SOURCE_SMS,
                )
                val rowId = repository.insertParsed(candidate)
                if (rowId == -1L) {
                    repository.refreshParsedFields(candidate)
                    rawSmsDao.updateStatus(sms.id, RawSms.STATUS_PROCESSED, "Updated existing transaction")
                } else {
                    rawSmsDao.updateStatus(sms.id, RawSms.STATUS_PROCESSED)
                    saved++
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed processing raw SMS ${sms.id}", t)
                rawSmsDao.updateStatus(sms.id, RawSms.STATUS_FAILED, t.message)
            }
        }
        saved
    }

    companion object {
        private const val TAG = "SmsStagingProcessor"
    }
}
