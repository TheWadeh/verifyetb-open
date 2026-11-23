package com.verifyetb.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verifyetb.app.parser.EthiopianInstitutions
import com.verifyetb.app.ui.components.InstitutionMultiSelect
import com.verifyetb.app.ui.theme.AccentBlue
import com.verifyetb.app.ui.theme.BgPrimary
import com.verifyetb.app.ui.theme.OnBlue
import com.verifyetb.app.ui.theme.TextMuted
import com.verifyetb.app.ui.theme.TextPrimary

@Composable
fun PickAccountsScreen(
    initialSelected: Set<String> = emptySet(),
    onContinue: (Set<String>) -> Unit,
) {
    var selected by remember {
        mutableStateOf(EthiopianInstitutions.withCashAlways(initialSelected))
    }
    LaunchedEffect(initialSelected) {
        if (initialSelected.isNotEmpty()) {
            selected = EthiopianInstitutions.withCashAlways(initialSelected)
        }
    }
    val bankWalletCount = selected.count { it != EthiopianInstitutions.cash.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text("Your accounts", color = AccentBlue, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose banks and wallets",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Only new SMS from these (after you set balances) will be tracked.",
            color = TextMuted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            InstitutionMultiSelect(
                selectedIds = selected,
                onToggle = { id ->
                    if (id == EthiopianInstitutions.cash.id) return@InstitutionMultiSelect
                    selected = EthiopianInstitutions.withCashAlways(
                        if (id in selected) selected - id else selected + id,
                    )
                },
                onSetSelection = { ids ->
                    selected = EthiopianInstitutions.withCashAlways(ids)
                },
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        if (bankWalletCount == 0) {
            Text(
                text = "Select at least one bank or wallet to continue.",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Button(
            onClick = { onContinue(EthiopianInstitutions.withCashAlways(selected)) },
            enabled = bankWalletCount > 0,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = OnBlue),
        ) {
            Text("Continue")
        }
    }
}
