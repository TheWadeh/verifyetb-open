package com.verifyetb.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verifyetb.app.data.local.entity.TransactionEntity
import com.verifyetb.app.ui.components.EmptyHint
import com.verifyetb.app.ui.components.InstitutionLogo
import com.verifyetb.app.ui.components.SectionLabel
import com.verifyetb.app.ui.components.TransactionRow
import com.verifyetb.app.ui.theme.AccentGold
import com.verifyetb.app.ui.theme.AccentGreen
import com.verifyetb.app.ui.theme.AccentRust
import com.verifyetb.app.ui.theme.BgElevated
import com.verifyetb.app.ui.theme.BgPrimary
import com.verifyetb.app.ui.theme.BgSurface
import com.verifyetb.app.ui.theme.Divider
import com.verifyetb.app.ui.theme.OnGold
import com.verifyetb.app.ui.theme.TextMuted
import com.verifyetb.app.ui.theme.TextPrimary
import com.verifyetb.app.util.DashboardPeriod
import com.verifyetb.app.util.DateFormatters
import com.verifyetb.app.util.MoneyFormat
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenTransactions: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onAddManual: () -> Unit,
    onOpenAccount: (String) -> Unit,
    onOpenPeriod: (from: Long, to: Long, type: String) -> Unit,
    onOpenFees: (from: Long, to: Long) -> Unit,
    onOpenInsights: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val newestId by viewModel.newestAnimatedId.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val refreshMessage by viewModel.refreshMessage.collectAsStateWithLifecycle()
    val pullState = rememberPullToRefreshState()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(refreshMessage) {
        if (refreshMessage != null) {
            delay(2200)
            viewModel.clearRefreshMessage()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = ui.customDayMillis?.let(DateFormatters::localDayStartToUtcPicker)
                ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(viewModel::setCustomDate)
                        showDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Verifyetb", color = AccentGold, style = MaterialTheme.typography.titleMedium)
                    Text("Pull down to sync new SMS", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onOpenInsights) {
                    Icon(Icons.Outlined.BarChart, contentDescription = "Insights", tint = TextMuted)
                }
                IconButton(onClick = onOpenTransactions) {
                    Icon(Icons.AutoMirrored.Outlined.List, contentDescription = "All transactions", tint = TextMuted)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = TextMuted)
                }
            }

            if (refreshMessage != null) {
                Text(
                    text = refreshMessage!!,
                    color = AccentGold,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
                )
            }

            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = viewModel::refreshFromSms,
                state = pullState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        BalanceHero(
                            total = ui.totalBalance,
                            accountCount = ui.accountCount,
                            unresolved = ui.unresolvedCount,
                            onUnresolved = {
                                ui.firstUnresolvedId?.let(onOpenDetail)
                            },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionLabel("Balance by type")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MiniBalance("Banks", ui.banksBalance, Modifier.weight(1f))
                            MiniBalance("Wallets", ui.walletsBalance, Modifier.weight(1f))
                            if (ui.cashBalance != 0.0 || ui.accounts.any { it.id == "cash" }) {
                            MiniBalance("Cash", ui.cashBalance, Modifier.weight(1f))
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))
                        SectionLabel("Period")
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(
                                DashboardPeriod.TODAY to "Today",
                                DashboardPeriod.WEEK to "Week",
                                DashboardPeriod.MONTH to "Month",
                                DashboardPeriod.YEAR to "Year",
                            ).forEach { (period, label) ->
                                PeriodChip(label, ui.period == period) { viewModel.setPeriod(period) }
                            }
                            val pickLabel = ui.customDayMillis?.let { DateFormatters.dayShort(it) }
                                ?: "Pick a Date"
                            PeriodChip(
                                label = pickLabel,
                                selected = ui.period == DashboardPeriod.CUSTOM,
                                onClick = { showDatePicker = true },
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            StatTile(
                                label = "Sent",
                                value = MoneyFormat.birr(ui.sent),
                                color = AccentRust,
                                modifier = Modifier.weight(1f),
                                subtitle = "Fees ${MoneyFormat.birr(ui.fees)}",
                                onSubtitleClick = { onOpenFees(ui.periodFrom, ui.periodTo) },
                            ) {
                                onOpenPeriod(ui.periodFrom, ui.periodTo, TransactionEntity.TYPE_DEBIT)
                            }
                            StatTile(
                                label = "Received",
                                value = MoneyFormat.birr(ui.received),
                                color = AccentGreen,
                                modifier = Modifier.weight(1f),
                            ) {
                                onOpenPeriod(ui.periodFrom, ui.periodTo, TransactionEntity.TYPE_CREDIT)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        SectionLabel("Period details")
                        DetailPanel {
                            DetailRow("Transactions", "${ui.transactionCount}")
                            DetailRow("Sent count", "${ui.debitCount}")
                            DetailRow("Received count", "${ui.creditCount}")
                            DetailRow("Avg sent", MoneyFormat.birr(ui.avgSent))
                            DetailRow("Avg received", MoneyFormat.birr(ui.avgReceived))
                            DetailRow("Largest fee", MoneyFormat.birr(ui.largestFee))
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        InsightsEntry(onClick = onOpenInsights)

                        Spacer(modifier = Modifier.height(22.dp))
                        SectionLabel("Accounts")
                    }

                    if (ui.accounts.isEmpty()) {
                        item { EmptyHint("Pick banks and wallets in Settings.") }
                    } else {
                        itemsIndexed(ui.accounts, key = { _, a -> a.id }) { _, account ->
                            AccountCard(account = account, onClick = { onOpenAccount(account.id) })
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(22.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionLabel("Recent")
                            TextButton(onClick = onOpenTransactions) {
                                Text("See all", color = AccentGold)
                            }
                        }
                    }

                    if (ui.recent.isEmpty()) {
                        item { EmptyHint("No transactions yet — pull down to read your SMS, or tap +.") }
                    } else {
                        itemsIndexed(ui.recent, key = { _, tx -> tx.id }) { _, tx ->
                            TransactionRow(
                                tx = tx,
                                animateIn = tx.id == newestId,
                                onClick = { onOpenDetail(tx.id) },
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(96.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddManual,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp),
            containerColor = AccentGold,
            contentColor = OnGold,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 8.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add manually")
        }
    }
}

@Composable
private fun BalanceHero(
    total: Double,
    accountCount: Int,
    unresolved: Int,
    onUnresolved: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(BgSurface, BgElevated, AccentGold.copy(alpha = 0.16f)),
                ),
                shape = RoundedCornerShape(20.dp),
            )
            .border(1.dp, AccentGold.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Text("All accounts", color = TextMuted, style = MaterialTheme.typography.labelMedium)
        Text(
            text = MoneyFormat.birr(total),
            color = TextPrimary,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = "$accountCount accounts · opening + SMS − fees",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (unresolved > 0) {
            Text(
                text = if (unresolved == 1) "1 transaction needs a note — tap" else "$unresolved need notes — tap",
                color = AccentGold,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 10.dp).clickable(onClick = onUnresolved),
            )
        }
    }
}

@Composable
private fun MiniBalance(label: String, value: Double, modifier: androidx.compose.ui.Modifier) {
    Column(
        modifier = modifier
            .background(BgSurface, RoundedCornerShape(14.dp))
            .border(1.dp, Divider.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text(label, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        Text(
            MoneyFormat.birr(value),
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun DetailPanel(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSurface, RoundedCornerShape(16.dp))
            .border(1.dp, Divider.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        content()
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AccountCard(account: AccountRowUi, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(BgSurface, RoundedCornerShape(16.dp))
            .border(1.dp, Divider.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                InstitutionLogo(
                    institutionId = account.id,
                    displayName = account.displayName,
                    size = 42.dp,
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(account.displayName, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${account.kindLabel} · ${account.txCount} tx this period",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(MoneyFormat.birr(account.balance), color = AccentGold, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Sent ${MoneyFormat.birr(account.sent)}", color = AccentRust, style = MaterialTheme.typography.bodySmall)
                Text("Fees ${MoneyFormat.birr(account.fees)}", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text("In ${MoneyFormat.birr(account.received)}", color = AccentGreen, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun InsightsEntry(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSurface, RoundedCornerShape(20.dp))
            .border(1.dp, AccentGold.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.BarChart, contentDescription = null, tint = AccentGold)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text("Charts and heatmap", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Text(
                "Net flow by day, where money went, last 12 weeks",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
    }
}

@Composable
private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Text(
        text = label,
        color = if (selected) OnGold else TextMuted,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .background(if (selected) AccentGold else BgSurface, shape)
            .border(1.dp, if (selected) AccentGold else Divider, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: androidx.compose.ui.Modifier = Modifier,
    subtitle: String? = null,
    onSubtitleClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(BgSurface, RoundedCornerShape(16.dp))
            .border(1.dp, Divider.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column(modifier = Modifier.clickable(onClick = onClick)) {
            Text(label, color = TextMuted, style = MaterialTheme.typography.bodySmall)
            Text(value, color = color, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 6.dp))
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .then(
                        if (onSubtitleClick != null) Modifier.clickable(onClick = onSubtitleClick)
                        else Modifier,
                    ),
            )
        }
    }
}
