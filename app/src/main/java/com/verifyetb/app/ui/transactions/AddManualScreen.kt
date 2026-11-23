package com.verifyetb.app.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verifyetb.app.data.local.entity.TransactionEntity
import com.verifyetb.app.ui.theme.AccentGold
import com.verifyetb.app.ui.theme.OnGold
import com.verifyetb.app.ui.theme.BgPrimary
import com.verifyetb.app.ui.theme.BgSurface
import com.verifyetb.app.ui.theme.Divider
import com.verifyetb.app.ui.theme.TextMuted
import com.verifyetb.app.ui.theme.TextPrimary

@Composable
fun AddManualScreen(
    onBack: () -> Unit,
    viewModel: AddManualViewModel = hiltViewModel(),
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    var type by remember { mutableStateOf(TransactionEntity.TYPE_DEBIT) }
    var amount by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var accountExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(accounts) {
        if (institution.isBlank() && accounts.isNotEmpty()) {
            institution = (accounts.firstOrNull { it.id != "cash" } ?: accounts.first()).displayName
        }
    }

    LaunchedEffect(saved) {
        if (saved) onBack()
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
                text = "Add money",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp),
        ) {
            Field(amount, { amount = it; viewModel.clearError() }, "Amount (ETB)", KeyboardType.Decimal)

            Spacer(modifier = Modifier.height(16.dp))
            Text("What happened?", color = TextMuted, style = MaterialTheme.typography.labelMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                FilterChip(
                    selected = type == TransactionEntity.TYPE_DEBIT,
                    onClick = { type = TransactionEntity.TYPE_DEBIT },
                    label = { Text("Sent") },
                    colors = chipColors(),
                )
                FilterChip(
                    selected = type == TransactionEntity.TYPE_CREDIT,
                    onClick = {
                        type = TransactionEntity.TYPE_CREDIT
                        fee = ""
                    },
                    label = { Text("Received") },
                    colors = chipColors(),
                )
            }
            Text(
                text = if (type == TransactionEntity.TYPE_DEBIT) {
                    "Money left this account."
                } else {
                    "Money came into this account."
                },
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text("Account", color = TextMuted, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            SimpleDropdown(
                value = institution.ifBlank { "Choose account" },
                expanded = accountExpanded,
                onExpandedChange = { accountExpanded = it },
                options = accounts.map { it.displayName },
                onSelect = {
                    institution = it
                    accountExpanded = false
                    viewModel.clearError()
                },
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text("Category", color = TextMuted, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            SimpleDropdown(
                value = category.ifBlank { "None" },
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                options = listOf("None") + categories.map { it.name },
                onSelect = {
                    category = if (it == "None") "" else it
                    categoryExpanded = false
                },
            )

            Spacer(modifier = Modifier.height(16.dp))
            Field(reason, { reason = it }, "Note (optional)")

            if (type == TransactionEntity.TYPE_DEBIT) {
                Spacer(modifier = Modifier.height(12.dp))
                Field(fee, { fee = it; viewModel.clearError() }, "Fee (optional)", KeyboardType.Decimal)
            }

            if (error != null) {
                Text(error!!, color = AccentGold, modifier = Modifier.padding(top = 12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.save(
                        type = type,
                        amountText = amount,
                        institution = institution,
                        category = category.ifBlank { null },
                        reason = reason.ifBlank { null },
                        feeText = fee,
                    )
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = OnGold),
            ) {
                Text(if (saving) "Saving…" else if (type == TransactionEntity.TYPE_DEBIT) "Save sent" else "Save received")
            }
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun SimpleDropdown(
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandedChange(!expanded) },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = TextMuted)
            },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = Divider,
                disabledTextColor = TextPrimary,
                disabledTrailingIconColor = TextMuted,
                disabledContainerColor = BgSurface,
            ),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = TextMuted) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        colors = fieldColors(),
    )
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
    focusedLabelColor = AccentGold,
    unfocusedLabelColor = TextMuted,
)

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = BgSurface,
    labelColor = TextMuted,
    selectedContainerColor = AccentGold.copy(alpha = 0.22f),
    selectedLabelColor = AccentGold,
)
