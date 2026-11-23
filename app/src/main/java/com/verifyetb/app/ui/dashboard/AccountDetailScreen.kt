package com.verifyetb.app.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verifyetb.app.ui.components.EmptyHint
import com.verifyetb.app.ui.components.InstitutionLogo
import com.verifyetb.app.ui.components.SectionLabel
import com.verifyetb.app.ui.components.TransactionRow
import com.verifyetb.app.ui.theme.AccentGold
import com.verifyetb.app.ui.theme.AccentGreen
import com.verifyetb.app.ui.theme.AccentRust
import com.verifyetb.app.ui.theme.BgPrimary
import com.verifyetb.app.ui.theme.BgSurface
import com.verifyetb.app.ui.theme.TextMuted
import com.verifyetb.app.ui.theme.TextPrimary
import com.verifyetb.app.util.MoneyFormat

@Composable
fun AccountDetailScreen(
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onOpenAll: (displayName: String) -> Unit,
    viewModel: AccountDetailViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

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
            InstitutionLogo(
                institutionId = ui.institutionId,
                displayName = ui.displayName,
                size = 36.dp,
                modifier = Modifier.padding(end = 10.dp),
            )
            Text(
                text = ui.displayName.ifBlank { "Account" },
                color = TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Balance", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                Text(
                    text = MoneyFormat.birr(ui.balance),
                    color = AccentGold,
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(modifier = Modifier.height(20.dp))
                SectionLabel("This month")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgSurface, RoundedCornerShape(10.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Sent", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        Text(MoneyFormat.birr(ui.sent), color = AccentRust, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Fees ${MoneyFormat.birr(ui.fees)}",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Received", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        Text(MoneyFormat.birr(ui.received), color = AccentGreen, style = MaterialTheme.typography.titleLarge)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel("Recent")
                    TextButton(onClick = { onOpenAll(ui.displayName) }) {
                        Text("See all", color = AccentGold)
                    }
                }
            }
            if (ui.recent.isEmpty()) {
                item { EmptyHint("No transactions for this account yet.") }
            } else {
                items(ui.recent, key = { it.id }) { tx ->
                    TransactionRow(tx = tx, onClick = { onOpenDetail(tx.id) })
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
