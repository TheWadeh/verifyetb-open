package com.verifyetb.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.verifyetb.app.data.SmsStagingProcessor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SmsProcessingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val processor: SmsStagingProcessor,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        processor.processPending()
        return Result.success()
    }

    companion object {
        private const val UNIQUE = "sms-processing"

        fun enqueue(context: Context) {
            val req = OneTimeWorkRequestBuilder<SmsProcessingWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                req,
            )
        }
    }
}
