package com.verifyetb.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.verifyetb.app.data.local.dao.RawSmsDao
import com.verifyetb.app.data.local.entity.RawSms
import com.verifyetb.app.worker.SmsProcessingWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Capture-only SMS receiver.
 * Writes into staging; SmsProcessingWorker owns parsing.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var rawSmsDao: RawSmsDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages.first().displayOriginatingAddress?.trim().orEmpty()
        if (sender.isEmpty()) {
            Log.w(TAG, "Ignoring SMS with empty sender")
            return
        }

        val body = messages.joinToString(separator = "") { it.displayMessageBody.orEmpty() }
        if (body.isBlank()) {
            Log.w(TAG, "Ignoring SMS with empty body from $sender")
            return
        }

        val date = messages.maxOf { it.timestampMillis }

        val pendingResult = goAsync()
        scope.launch {
            try {
                val rowId = rawSmsDao.insert(
                    RawSms(
                        sender = sender,
                        body = body,
                        date = date,
                    )
                )
                if (rowId == -1L) {
                    Log.d(TAG, "Duplicate SMS ignored (sender=$sender, date=$date)")
                } else {
                    Log.i(TAG, "Staged raw SMS id=$rowId from $sender")
                    SmsProcessingWorker.enqueue(context.applicationContext)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to stage SMS from $sender", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
