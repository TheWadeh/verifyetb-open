package com.verifyetb.app.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verifyetb.app.data.local.entity.TransactionEntity
import com.verifyetb.app.ui.theme.AccentGold
import com.verifyetb.app.ui.theme.OnGold
import com.verifyetb.app.ui.theme.AccentGreen
import com.verifyetb.app.ui.theme.AccentRust
import com.verifyetb.app.ui.theme.AmountStyle
import com.verifyetb.app.ui.theme.BgPrimary
import com.verifyetb.app.ui.theme.BgSurface
import com.verifyetb.app.ui.theme.Divider
import com.verifyetb.app.ui.theme.TextMuted
import com.verifyetb.app.ui.theme.TextPrimary
import com.verifyetb.app.util.DateFormatters
import com.verifyetb.app.util.MoneyFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailScreen(
    onBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val tx by viewModel.transaction.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tx?.id) {
        note = tx?.reason.orEmpty()
        category = tx?.category
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = "Transaction",
                color = TextPrimary,
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            )
        }

        val current = tx
        if (current == null) {
            Text("Transaction not found.", color = TextMuted, modifier = Modifier.padding(24.dp))
        } else {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp),
        ) {
            Text(
                text = MoneyFormat.signed(
                    current.amount,
                    isDebit = current.type == TransactionEntity.TYPE_DEBIT,
                ),
                style = AmountStyle.copy(fontSize = 32.sp),
                color = if (current.type == TransactionEntity.TYPE_DEBIT) AccentRust else AccentGreen,
            )
            Spacer(modifier = Modifier.height(20.dp))
            DetailLine("Institution", current.institution)
            DetailLine("When", DateFormatters.dayTime(current.date))
            val isAirtime = current.counterparty.equals("Airtime", ignoreCase = true) ||
                current.reason.equals("Airtime", ignoreCase = true)
            DetailLine(
                "Type",
                when {
                    isAirtime && current.type == TransactionEntity.TYPE_DEBIT -> "Airtime"
                    current.type == TransactionEntity.TYPE_DEBIT -> "Sent"
                    else -> "Received"
                },
            )
            if (!isAirtime && !current.counterparty.isNullOrBlank()) {
                DetailLine(
                    label = if (current.type == TransactionEntity.TYPE_DEBIT) "Sent to" else "Received from",
                    value = current.counterparty,
                )
            }
            DetailLine("Source", if (current.source == TransactionEntity.SOURCE_SMS) "SMS" else "Manual")
            if (current.feeAmount > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(AccentRust.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                        .padding(14.dp),
                ) {
                    Text(
                        "Fees (service + VAT)",
                        color = TextMuted,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        MoneyFormat.birr(current.feeAmount),
                        color = AccentRust,
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "Total of service charge, VAT, and other bank/wallet fees from the SMS.",
                        color = TextMuted,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            if (!current.reference.isNullOrBlank()) DetailLine("Reference", current.reference)
            if (current.statedBalance != null) DetailLine("Stated balance", MoneyFormat.birr(current.statedBalance))

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = if (current.needsNote) "Add a note" else "Note",
                color = TextPrimary,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            )
            if (current.needsNote) {
                Text(
                    text = "This SMS didn't include a reason — a short note helps later.",
                    color = TextMuted,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Lunch, taxi, airtime", color = TextMuted) },
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors(),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Category", color = TextMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = category == cat.name,
                        onClick = { category = if (category == cat.name) null else cat.name },
                        label = { Text(cat.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = BgSurface,
                            labelColor = TextMuted,
                            selectedContainerColor = AccentGold.copy(alpha = 0.22f),
                            selectedLabelColor = AccentGold,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = {
                    viewModel.saveNote(note, category)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = OnGold),
            ) {
                Text("Save note")
            }
            TextButton(
                onClick = {
                    viewModel.delete()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Delete transaction", color = AccentRust)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, color = TextMuted, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        Text(value, color = TextPrimary, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentGold,
    unfocusedBorderColor = Divider,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = AccentGold,
    focusedContainerColor = BgSurface,
    unfocusedContainerColor = BgSurface,
)
