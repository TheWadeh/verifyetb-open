package com.verifyetb.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verifyetb.app.parser.EthiopianInstitution
import com.verifyetb.app.parser.EthiopianInstitutions
import com.verifyetb.app.ui.theme.AccentGold
import com.verifyetb.app.ui.theme.BgElevated
import com.verifyetb.app.ui.theme.BgSurface
import com.verifyetb.app.ui.theme.Divider
import com.verifyetb.app.ui.theme.OnGold
import com.verifyetb.app.ui.theme.TextMuted
import com.verifyetb.app.ui.theme.TextPrimary

@Composable
fun InstitutionSectionTitle(title: String, count: Int) {
    Text(
        text = "$title ($count)",
        color = TextPrimary,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
    )
}

@Composable
fun InstitutionNameList(institutions: List<EthiopianInstitution>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        institutions.forEachIndexed { index, inst ->
            Text(
                text = inst.displayName,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            if (index < institutions.lastIndex) {
                HorizontalDivider(color = Divider)
            }
        }
    }
}

/** Bank + wallet checklist only. Cash is kept in prefs automatically — not shown here. */
@Composable
fun InstitutionMultiSelect(
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onSetSelection: ((Set<String>) -> Unit)? = null,
) {
    val pickSelected = selectedIds - EthiopianInstitutions.cash.id
    Column(modifier = Modifier.fillMaxWidth()) {
        PickerGroup(
            title = "Banks",
            items = EthiopianInstitutions.banks,
            selectedIds = pickSelected,
            onToggle = onToggle,
            onSelectAll = onSetSelection?.let { set ->
                {
                    set(pickSelected + EthiopianInstitutions.banks.map { it.id })
                }
            },
            onClear = onSetSelection?.let { set ->
                {
                    set(pickSelected - EthiopianInstitutions.banks.map { it.id }.toSet())
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))
        PickerGroup(
            title = "Mobile wallets",
            items = EthiopianInstitutions.wallets,
            selectedIds = pickSelected,
            onToggle = onToggle,
            onSelectAll = onSetSelection?.let { set ->
                {
                    set(pickSelected + EthiopianInstitutions.wallets.map { it.id })
                }
            },
            onClear = onSetSelection?.let { set ->
                {
                    set(pickSelected - EthiopianInstitutions.wallets.map { it.id }.toSet())
                }
            },
        )

        val bankCount = EthiopianInstitutions.banks.count { it.id in pickSelected }
        val walletCount = EthiopianInstitutions.wallets.count { it.id in pickSelected }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "$bankCount banks · $walletCount wallets selected",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PickerGroup(
    title: String,
    items: List<EthiopianInstitution>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onSelectAll: (() -> Unit)?,
    onClear: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSurface, RoundedCornerShape(16.dp))
            .border(1.dp, Divider.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$title (${items.size})",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (onSelectAll != null) {
                TextButton(onClick = onSelectAll) { Text("All", color = AccentGold) }
            }
            if (onClear != null) {
                TextButton(onClick = onClear) { Text("Clear", color = TextMuted) }
            }
        }
        items.forEachIndexed { index, inst ->
            InstitutionCheckRow(
                institution = inst,
                selected = inst.id in selectedIds,
                onClick = { onToggle(inst.id) },
            )
            if (index < items.lastIndex) {
                HorizontalDivider(
                    color = Divider.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun InstitutionCheckRow(
    institution: EthiopianInstitution,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InstitutionLogo(
            institutionId = institution.id,
            displayName = institution.displayName,
            size = 36.dp,
        )
        Text(
            text = institution.displayName,
            color = if (selected) TextPrimary else TextMuted,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        BoxCheck(selected = selected)
    }
}

@Composable
private fun BoxCheck(selected: Boolean) {
    val shape = RoundedCornerShape(7.dp)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(22.dp)
            .background(if (selected) AccentGold else BgElevated, shape)
            .border(1.dp, if (selected) AccentGold else Divider, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = OnGold,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
