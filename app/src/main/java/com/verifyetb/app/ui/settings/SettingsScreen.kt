package com.verifyetb.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verifyetb.app.BuildConfig
import com.verifyetb.app.parser.EthiopianInstitutions
import com.verifyetb.app.parser.InstitutionKind
import com.verifyetb.app.ui.components.InstitutionLogo
import com.verifyetb.app.ui.components.InstitutionMultiSelect
import com.verifyetb.app.ui.theme.AccentGold
import com.verifyetb.app.ui.theme.AccentGreen
import com.verifyetb.app.ui.theme.AccentRust
import com.verifyetb.app.ui.theme.BgPrimary
import com.verifyetb.app.ui.theme.BgSurface
import com.verifyetb.app.ui.theme.Divider
import com.verifyetb.app.ui.theme.OnGold
import com.verifyetb.app.ui.theme.TextMuted
import com.verifyetb.app.ui.theme.TextPrimary
import com.verifyetb.app.ui.theme.ThemeMode

private enum class SettingsSection {
    None,
    Sms,
    Appearance,
    Accounts,
    Balances,
    Categories,
    About,
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val stagedCount by viewModel.stagedCount.collectAsStateWithLifecycle()
    val status by viewModel.statusMessage.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedInstitutionIds.collectAsStateWithLifecycle()
    val balances by viewModel.accountBalances.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    var newCategory by remember { mutableStateOf("") }
    var openSection by remember { mutableStateOf(SettingsSection.None) }
    var smsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        smsGranted = result.values.all { it }
        if (smsGranted) viewModel.scanInbox()
    }
    val balanceDrafts = remember(selectedIds) {
        selectedIds.associateWith { id ->
            val v = balances.find { it.institutionId == id }?.openingBalance ?: 0.0
            mutableStateOf(if (v == 0.0) "" else v.toString())
        }
    }
    var balanceStatus by remember { mutableStateOf<String?>(null) }

    val bankCount = EthiopianInstitutions.banks.count { it.id in selectedIds }
    val walletCount = EthiopianInstitutions.wallets.count { it.id in selectedIds }
    val themeLabel = when (themeMode) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }

    fun toggle(section: SettingsSection) {
        openSection = if (openSection == section) SettingsSection.None else section
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
                text = "Settings",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        Text(
            text = "Tap a row to open it",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = 24.dp),
        ) {
            SettingsMenuCard {
                SettingsMenuRow(
                    title = "SMS",
                    subtitle = if (smsGranted) "On · $stagedCount staged" else "Permission needed",
                    expanded = openSection == SettingsSection.Sms,
                    onClick = { toggle(SettingsSection.Sms) },
                )
                AnimatedVisibility(visible = openSection == SettingsSection.Sms) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(
                            text = if (smsGranted) {
                                "Reads the bank and wallet SMS already on this phone, then new ones as they arrive."
                            } else {
                                "Enable SMS so Verifyetb can read your bank messages."
                            },
                            color = if (smsGranted) AccentGreen else TextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (!smsGranted) {
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.RECEIVE_SMS,
                                            Manifest.permission.READ_SMS,
                                        ),
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = OnGold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Enable SMS") }
                        } else {
                            Button(
                                onClick = { viewModel.scanInbox() },
                                enabled = !busy,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = OnGold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Check for new SMS") }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.reprocessPending() },
                                enabled = !busy,
                                colors = ButtonDefaults.buttonColors(containerColor = BgPrimary, contentColor = TextPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Reprocess recent SMS") }
                        }
                        if (status != null) {
                            Text(
                                text = status!!,
                                color = AccentGold,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                MenuDivider()
                SettingsMenuRow(
                    title = "Appearance",
                    subtitle = themeLabel,
                    expanded = openSection == SettingsSection.Appearance,
                    onClick = { toggle(SettingsSection.Appearance) },
                )
                AnimatedVisibility(visible = openSection == SettingsSection.Appearance) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        ThemeModeChip("System", themeMode == ThemeMode.SYSTEM) {
                            viewModel.setThemeMode(ThemeMode.SYSTEM)
                        }
                        ThemeModeChip("Light", themeMode == ThemeMode.LIGHT) {
                            viewModel.setThemeMode(ThemeMode.LIGHT)
                        }
                        ThemeModeChip("Dark", themeMode == ThemeMode.DARK) {
                            viewModel.setThemeMode(ThemeMode.DARK)
                        }
                    }
                }

                MenuDivider()
                SettingsMenuRow(
                    title = "Banks & wallets",
                    subtitle = "$bankCount banks · $walletCount wallets",
                    expanded = openSection == SettingsSection.Accounts,
                    onClick = { toggle(SettingsSection.Accounts) },
                )
                AnimatedVisibility(visible = openSection == SettingsSection.Accounts) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            text = "Only SMS from selected accounts is kept.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                        )
                        InstitutionMultiSelect(
                            selectedIds = selectedIds,
                            onToggle = viewModel::toggleInstitution,
                            onSetSelection = viewModel::setInstitutions,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                MenuDivider()
                SettingsMenuRow(
                    title = "Cash on hand",
                    subtitle = "Optional",
                    expanded = openSection == SettingsSection.Balances,
                    onClick = { toggle(SettingsSection.Balances) },
                )
                AnimatedVisibility(visible = openSection == SettingsSection.Balances) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(
                            text = "Optional for Cash — set only if you track cash on hand.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        val balanceAccounts = EthiopianInstitutions.selectable
                            .filter { it.kind == InstitutionKind.CASH }
                        if (balanceAccounts.isEmpty()) {
                            Text("Open Banks & wallets first.", color = TextMuted)
                        } else {
                            balanceAccounts.forEach { inst ->
                                val draft = balanceDrafts[inst.id] ?: return@forEach
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                ) {
                                    InstitutionLogo(
                                        institutionId = inst.id,
                                        displayName = inst.displayName,
                                        size = 28.dp,
                                    )
                                    Text(
                                        text = inst.displayName,
                                        color = TextMuted,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(start = 10.dp),
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedTextField(
                                        value = draft.value,
                                        onValueChange = { draft.value = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("0.00", color = TextMuted) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = fieldColors(),
                                    )
                                    TextButton(
                                        onClick = {
                                            val amount = draft.value.replace(",", "").toDoubleOrNull() ?: 0.0
                                            viewModel.saveOpeningBalance(inst.id, amount)
                                            balanceStatus = "Saved ${inst.displayName}"
                                        },
                                    ) {
                                        Text("Save", color = AccentGold)
                                    }
                                }
                            }
                        }
                        if (balanceStatus != null) {
                            Text(
                                text = balanceStatus!!,
                                color = AccentGold,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                MenuDivider()
                SettingsMenuRow(
                    title = "Categories",
                    subtitle = "${categories.size} categories",
                    expanded = openSection == SettingsSection.Categories,
                    onClick = { toggle(SettingsSection.Categories) },
                )
                AnimatedVisibility(visible = openSection == SettingsSection.Categories) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        categories.forEach { cat ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(cat.name, color = TextPrimary)
                                TextButton(onClick = { viewModel.removeCategory(cat) }) {
                                    Text("Remove", color = AccentRust)
                                }
                            }
                            HorizontalDivider(color = Divider)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newCategory,
                            onValueChange = { newCategory = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("New category", color = TextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = fieldColors(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.addCategory(newCategory)
                                newCategory = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = OnGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Add category") }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                MenuDivider()
                SettingsMenuRow(
                    title = "About",
                    subtitle = "Verifyetb ${BuildConfig.VERSION_NAME}",
                    expanded = openSection == SettingsSection.About,
                    onClick = { toggle(SettingsSection.About) },
                )
                AnimatedVisibility(visible = openSection == SettingsSection.About) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text(
                            text = "Free and fully offline. Bank and wallet balances come from your SMS.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsMenuCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSurface, RoundedCornerShape(16.dp)),
    ) {
        content()
    }
}

@Composable
private fun SettingsMenuRow(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = TextMuted,
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(color = Divider.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 14.dp))
}

@Composable
private fun ThemeModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Text(
        text = label,
        color = if (selected) OnGold else TextMuted,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .background(if (selected) AccentGold else BgPrimary, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentGold,
    unfocusedBorderColor = Divider,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = AccentGold,
    focusedContainerColor = BgPrimary,
    unfocusedContainerColor = BgPrimary,
)
