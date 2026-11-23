package com.verifyetb.app.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verifyetb.app.data.local.entity.TransactionEntity
import com.verifyetb.app.ui.components.EmptyHint
import com.verifyetb.app.ui.components.TransactionRow
import com.verifyetb.app.ui.theme.AccentBlue
import com.verifyetb.app.ui.theme.BgPrimary
import com.verifyetb.app.ui.theme.BgSurface
import com.verifyetb.app.ui.theme.Divider
import com.verifyetb.app.ui.theme.TextMuted
import com.verifyetb.app.ui.theme.TextPrimary
import com.verifyetb.app.util.DashboardPeriod
import com.verifyetb.app.util.DateFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    viewModel: TransactionListViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val type by viewModel.typeFilter.collectAsStateWithLifecycle()
    val institution by viewModel.institutionFilter.collectAsStateWithLifecycle()
    val institutions by viewModel.institutions.collectAsStateWithLifecycle()
    val items by viewModel.transactions.collectAsStateWithLifecycle()
    val from by viewModel.fromFilter.collectAsStateWithLifecycle()
    val to by viewModel.toFilter.collectAsStateWithLifecycle()
    val feesOnly by viewModel.feesOnly.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val customDayMillis by viewModel.customDayMillis.collectAsStateWithLifecycle()
    val periodActive = from > 0L || to > 0L || feesOnly
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = customDayMillis?.let(DateFormatters::localDayStartToUtcPicker)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = when {
                    feesOnly -> "Fees"
                    else -> "Transactions"
                },
                color = TextPrimary,
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            if (periodActive) {
                TextButton(onClick = viewModel::clearPeriodFilter) {
                    Text("Clear period", color = AccentBlue)
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            placeholder = { Text("Search institution, note, reference", color = TextMuted) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = Divider,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = AccentBlue,
                focusedContainerColor = BgSurface,
                unfocusedContainerColor = BgSurface,
            ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TypeChip("All", type.isEmpty() && !feesOnly) {
                viewModel.setTypeFilter("")
                viewModel.clearFeesOnly()
            }
            TypeChip("Spent", type == TransactionEntity.TYPE_DEBIT) {
                viewModel.setTypeFilter(TransactionEntity.TYPE_DEBIT)
            }
            TypeChip("Received", type == TransactionEntity.TYPE_CREDIT) {
                viewModel.setTypeFilter(TransactionEntity.TYPE_CREDIT)
            }
            institutions.forEach { name ->
                TypeChip(name, institution == name) {
                    viewModel.setInstitutionFilter(if (institution == name) "" else name)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TypeChip("Today", period == DashboardPeriod.TODAY) {
                viewModel.setPeriod(DashboardPeriod.TODAY)
            }
            TypeChip("Week", period == DashboardPeriod.WEEK) {
                viewModel.setPeriod(DashboardPeriod.WEEK)
            }
            TypeChip("Month", period == DashboardPeriod.MONTH) {
                viewModel.setPeriod(DashboardPeriod.MONTH)
            }
            TypeChip("Year", period == DashboardPeriod.YEAR) {
                viewModel.setPeriod(DashboardPeriod.YEAR)
            }
            val pickLabel = customDayMillis?.let { DateFormatters.dayShort(it) } ?: "Pick a Date"
            TypeChip(pickLabel, period == DashboardPeriod.CUSTOM) {
                showDatePicker = true
            }
        }

        if (items.isEmpty()) {
            EmptyHint("No matching transactions. Try clearing filters or add one manually from the dashboard.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items, key = { it.id }) { tx ->
                    TransactionRow(tx = tx, onClick = { onOpenDetail(tx.id) })
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = BgSurface,
            labelColor = TextMuted,
            selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
            selectedLabelColor = AccentBlue,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Divider,
            selectedBorderColor = AccentBlue,
        ),
    )
}
