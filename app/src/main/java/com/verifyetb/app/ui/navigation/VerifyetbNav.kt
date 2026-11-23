package com.verifyetb.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.verifyetb.app.data.SmsInboxImporter
import com.verifyetb.app.data.prefs.SettingsRepository
import com.verifyetb.app.data.repository.TransactionRepository
import com.verifyetb.app.ui.dashboard.AccountDetailScreen
import com.verifyetb.app.ui.dashboard.DashboardScreen
import com.verifyetb.app.ui.insights.InsightsScreen
import com.verifyetb.app.ui.onboarding.OnboardingScreen
import com.verifyetb.app.ui.onboarding.PickAccountsScreen
import com.verifyetb.app.ui.settings.SettingsScreen
import com.verifyetb.app.ui.theme.BgPrimary
import com.verifyetb.app.parser.EthiopianInstitutions
import com.verifyetb.app.ui.transactions.AddManualScreen
import com.verifyetb.app.ui.transactions.TransactionDetailScreen
import com.verifyetb.app.ui.transactions.TransactionListScreen
import com.verifyetb.app.worker.SmsProcessingWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VerifyetbNav(
    settingsRepository: SettingsRepository,
    transactionRepository: TransactionRepository,
    smsInboxImporter: SmsInboxImporter,
) {
    var startDestination by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val appContext = LocalContext.current.applicationContext

    LaunchedEffect(Unit) {
        startDestination = withContext(Dispatchers.IO) {
            val onboarding = settingsRepository.onboardingDone.first()
            val accounts = settingsRepository.accountsPicked.first()
            val balances = settingsRepository.balancesSet.first()
            if (accounts && !balances) {
                transactionRepository.upsertOpeningBalance(EthiopianInstitutions.cash.id, 0.0)
                settingsRepository.setBalancesSet(true)
            }
            when {
                !onboarding -> Routes.Onboarding
                !accounts -> Routes.PickAccounts
                else -> Routes.Dashboard
            }
        }
    }

    val start = startDestination
    if (start == null) {
        Box(modifier = Modifier.fillMaxSize().background(BgPrimary))
        return
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = start,
    ) {
        composable(Routes.Onboarding) {
            OnboardingScreen(
                onContinue = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            settingsRepository.setOnboardingDone(true)
                        }
                        navController.navigate(Routes.PickAccounts) {
                            popUpTo(Routes.Onboarding) { inclusive = true }
                        }
                    }
                },
                onSmsGranted = {
                    scope.launch(Dispatchers.IO) { smsInboxImporter.syncNew() }
                },
            )
        }
        composable(Routes.PickAccounts) {
            val selected by settingsRepository.selectedInstitutionIds
                .collectAsStateWithLifecycle(initialValue = emptySet())
            PickAccountsScreen(
                initialSelected = selected,
                onContinue = { ids ->
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            settingsRepository.setSelectedInstitutionIds(ids)
                            settingsRepository.setAccountsPicked(true)
                            transactionRepository.upsertOpeningBalance(EthiopianInstitutions.cash.id, 0.0)
                            settingsRepository.setBalancesSet(true)
                        }
                        navController.navigate(Routes.Dashboard) {
                            popUpTo(Routes.PickAccounts) { inclusive = true }
                        }
                        launch(Dispatchers.IO) {
                            smsInboxImporter.syncNew()
                        }
                    }
                },
            )
        }
        composable(Routes.Dashboard) {
            DashboardScreen(
                onOpenTransactions = { navController.navigate(Routes.transactions()) },
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onOpenDetail = { id -> navController.navigate(Routes.detail(id)) },
                onAddManual = { navController.navigate(Routes.AddManual) },
                onOpenAccount = { id -> navController.navigate(Routes.account(id)) },
                onOpenPeriod = { from, to, type ->
                    navController.navigate(Routes.transactions(from = from, to = to, type = type))
                },
                onOpenFees = { from, to ->
                    navController.navigate(
                        Routes.transactions(from = from, to = to, feesOnly = true),
                    )
                },
                onOpenInsights = { navController.navigate(Routes.Insights) },
            )
        }
        composable(Routes.Insights) {
            InsightsScreen(
                onBack = { navController.popBackStack() },
                onOpenDay = { from, to ->
                    navController.navigate(Routes.transactions(from = from, to = to))
                },
            )
        }
        composable(
            route = Routes.Transactions,
            arguments = listOf(
                navArgument("from") { type = NavType.LongType; defaultValue = 0L },
                navArgument("to") { type = NavType.LongType; defaultValue = 0L },
                navArgument("institution") { type = NavType.StringType; defaultValue = "" },
                navArgument("type") { type = NavType.StringType; defaultValue = "" },
                navArgument("feesOnly") { type = NavType.IntType; defaultValue = 0 },
            ),
        ) {
            TransactionListScreen(
                onBack = { navController.popBackStack() },
                onOpenDetail = { id -> navController.navigate(Routes.detail(id)) },
            )
        }
        composable(
            route = Routes.Detail,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) {
            TransactionDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.AddManual) {
            AddManualScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Settings) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.Account,
            arguments = listOf(navArgument("institutionId") { type = NavType.StringType }),
        ) {
            AccountDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenDetail = { id -> navController.navigate(Routes.detail(id)) },
                onOpenAll = { displayName ->
                    navController.navigate(Routes.transactions(institution = displayName))
                },
            )
        }
    }
}
