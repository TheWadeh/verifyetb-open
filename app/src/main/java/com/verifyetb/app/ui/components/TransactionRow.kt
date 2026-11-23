package com.verifyetb.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verifyetb.app.data.local.entity.TransactionEntity
import com.verifyetb.app.parser.MoneyParse
import com.verifyetb.app.ui.theme.AccentBlue
import com.verifyetb.app.ui.theme.AccentGreen
import com.verifyetb.app.ui.theme.AccentRust
import com.verifyetb.app.ui.theme.AmountStyle
import com.verifyetb.app.ui.theme.Divider
import com.verifyetb.app.ui.theme.TextMuted
import com.verifyetb.app.ui.theme.TextPrimary
import com.verifyetb.app.util.DateFormatters
import com.verifyetb.app.util.MoneyFormat

@Composable
fun TransactionRow(
    tx: TransactionEntity,
    animateIn: Boolean = false,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = true,
        enter = if (animateIn) {
            fadeIn() + slideInVertically(initialOffsetY = { -it / 2 })
        } else {
            fadeIn()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    val isAirtime = tx.counterparty.equals("Airtime", ignoreCase = true) ||
                        tx.reason.equals("Airtime", ignoreCase = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InstitutionLogoByName(
                            institutionName = tx.institution,
                            size = 32.dp,
                            modifier = Modifier.padding(end = 10.dp),
                        )
                        Text(
                            text = MoneyParse.listTitle(tx.counterparty, tx.reason, tx.institution),
                            color = TextPrimary,
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        )
                    }
                    val subtitle = buildString {
                        append(DateFormatters.dayTime(tx.date))
                        when {
                            isAirtime -> Unit // keep airtime rows short — date only
                            tx.needsNote -> append(" · needs note")
                            !tx.reason.isNullOrBlank() &&
                                !tx.reason.equals(tx.counterparty, ignoreCase = true) -> {
                                append(" · ${tx.reason}")
                            }
                        }
                    }
                    Text(
                        text = subtitle,
                        color = if (!isAirtime && tx.needsNote) AccentBlue else TextMuted,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (tx.feeAmount > 0) {
                        Text(
                            text = "Fees ${MoneyFormat.birr(tx.feeAmount)}",
                            color = AccentRust,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                Text(
                    text = MoneyFormat.signed(
                        tx.amount,
                        isDebit = tx.type == TransactionEntity.TYPE_DEBIT,
                    ),
                    style = AmountStyle,
                    color = if (tx.type == TransactionEntity.TYPE_DEBIT) AccentRust else AccentGreen,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 14.dp),
                thickness = 1.dp,
                color = Divider,
            )
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextMuted,
        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
fun EmptyHint(text: String) {
    Text(
        text = text,
        color = TextMuted,
        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 24.dp),
    )
}
