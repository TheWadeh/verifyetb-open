package com.verifyetb.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.verifyetb.app.data.SmsInboxImporter
import com.verifyetb.app.data.prefs.SettingsRepository
import com.verifyetb.app.data.repository.TransactionRepository
import com.verifyetb.app.ui.navigation.VerifyetbNav
import com.verifyetb.app.ui.theme.BgPrimary
import com.verifyetb.app.ui.theme.VerifyetbTheme
import com.verifyetb.app.ui.theme.ThemeMode
import com.verifyetb.app.worker.SmsProcessingWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var smsInboxImporter: SmsInboxImporter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SmsProcessingWorker.enqueue(applicationContext)
        lifecycleScope.launch {
            transactionRepository.ensureDefaultCategories()
            if (settingsRepository.consumeCashOpeningReset()) {
                // Older builds could seed Cash with a non-zero opening by mistake.
                transactionRepository.upsertOpeningBalance(
                    com.verifyetb.app.parser.EthiopianInstitutions.cash.id,
                    0.0,
                )
            }
            syncInbox()
        }
        setContent {
            val themeMode by settingsRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            VerifyetbTheme(themeMode = themeMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgPrimary),
                ) {
                    VerifyetbNav(
                        settingsRepository = settingsRepository,
                        transactionRepository = transactionRepository,
                        smsInboxImporter = smsInboxImporter,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { syncInbox() }
        SmsProcessingWorker.enqueue(applicationContext)
    }

    private suspend fun syncInbox() {
        val read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (!read) return
        smsInboxImporter.syncNew()
    }
}
