package com.verifyetb.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.verifyetb.app.ui.theme.ThemeMode
import com.verifyetb.app.parser.EthiopianInstitutions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "verifyetb_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val onboardingDoneKey = booleanPreferencesKey("onboarding_done")
    private val accountsPickedKey = booleanPreferencesKey("accounts_picked")
    private val balancesSetKey = booleanPreferencesKey("balances_set")
    private val lastInboxSmsDateKey = longPreferencesKey("last_inbox_sms_date")
    private val trackingStartedAtKey = longPreferencesKey("tracking_started_at")
    private val selectedInstitutionsKey = stringSetPreferencesKey("selected_institutions")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val cashOpeningResetKey = booleanPreferencesKey("cash_opening_reset_v1")
    private val activatedKey = booleanPreferencesKey("activated")
    private val requestCodeKey = stringPreferencesKey("activation_request_code")
    private val fullHistoryImportKey = booleanPreferencesKey("full_history_import_v1")
    private val inboxParseV2Key = booleanPreferencesKey("inbox_parse_v2")
    private val inboxParseV3Key = booleanPreferencesKey("inbox_parse_v3")
    private val inboxParseV4Key = booleanPreferencesKey("inbox_parse_v4")
    private val inboxParseV5Key = booleanPreferencesKey("inbox_parse_v5")

    val activated: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[activatedKey] ?: false
    }

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[onboardingDoneKey] ?: false
    }

    val accountsPicked: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[accountsPickedKey] ?: false
    }

    val balancesSet: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[balancesSetKey] ?: false
    }

    /** Always includes Cash — users never need to pick it. */
    val selectedInstitutionIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        EthiopianInstitutions.withCashAlways(prefs[selectedInstitutionsKey] ?: emptySet())
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.fromPref(prefs[themeModeKey])
    }

    suspend fun setActivated(done: Boolean = true) {
        context.dataStore.edit { it[activatedKey] = done }
    }

    /**
     * Tempo code bound to this phone's ANDROID_ID (one code per device).
     * Always re-derived so reinstalls keep the same code.
     */
    suspend fun getOrCreateRequestCode(generator: () -> String): String {
        val derived = generator()
        val existing = context.dataStore.data.first()[requestCodeKey]
        if (existing != derived) {
            context.dataStore.edit { it[requestCodeKey] = derived }
        }
        return derived
    }

    /** Existing installs that finished onboarding before paywall are grandfathered. */
    suspend fun ensureActivationMigrated() {
        val prefs = context.dataStore.data.first()
        if (prefs[activatedKey] == true) return
        if (prefs[onboardingDoneKey] == true) {
            context.dataStore.edit { it[activatedKey] = true }
        }
    }

    suspend fun setOnboardingDone(done: Boolean = true) {
        context.dataStore.edit { it[onboardingDoneKey] = done }
    }

    suspend fun setAccountsPicked(done: Boolean = true) {
        context.dataStore.edit { it[accountsPickedKey] = done }
    }

    suspend fun setBalancesSet(done: Boolean = true) {
        context.dataStore.edit { it[balancesSetKey] = done }
    }

    suspend fun setSelectedInstitutionIds(ids: Set<String>) {
        context.dataStore.edit {
            it[selectedInstitutionsKey] = EthiopianInstitutions.withCashAlways(ids)
        }
    }

    /** Keep every bank or wallet we actually found in SMS, without clearing the rest. */
    suspend fun addSelectedInstitutionIds(ids: Set<String>) {
        if (ids.isEmpty()) return
        context.dataStore.edit {
            val current = it[selectedInstitutionsKey] ?: emptySet()
            it[selectedInstitutionsKey] = EthiopianInstitutions.withCashAlways(current + ids)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeModeKey] = mode.toPref() }
    }

    suspend fun getSelectedInstitutionIds(): Set<String> =
        EthiopianInstitutions.withCashAlways(
            context.dataStore.data.first()[selectedInstitutionsKey] ?: emptySet(),
        )

    suspend fun getLastInboxSmsDate(): Long =
        context.dataStore.data.first()[lastInboxSmsDateKey] ?: 0L

    suspend fun setLastInboxSmsDate(date: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[lastInboxSmsDateKey] ?: 0L
            if (date > current) prefs[lastInboxSmsDateKey] = date
        }
    }

    /** Inbox cursor. Pass 0 to read the inbox from the beginning. */
    suspend fun forceSetLastInboxSmsDate(date: Long) {
        context.dataStore.edit { it[lastInboxSmsDateKey] = date }
    }

    /**
     * Older builds stamped the inbox cursor at "now" and ignored history.
     * Once per install, rewind that cursor so the next sync reads every SMS.
     */
    suspend fun consumeFullHistoryReset(): Boolean {
        val prefs = context.dataStore.data.first()
        if (prefs[fullHistoryImportKey] == true) return false
        context.dataStore.edit {
            it[fullHistoryImportKey] = true
            it[lastInboxSmsDateKey] = 0L
            it[trackingStartedAtKey] = 0L
        }
        return true
    }

    /**
     * 0.6.15 could scan the inbox, skip every SMS, then remember that scan as finished.
     * One rewind so the next open parses history again.
     */
    suspend fun consumeInboxParseV2(): Boolean {
        val prefs = context.dataStore.data.first()
        if (prefs[inboxParseV2Key] == true) return false
        context.dataStore.edit {
            it[inboxParseV2Key] = true
            it[lastInboxSmsDateKey] = 0L
            it[trackingStartedAtKey] = 0L
        }
        return true
    }

    /** Re-read the inbox once so CBE Birr texts misfiled as CBE get parsed again. */
    suspend fun consumeInboxParseV3(): Boolean {
        val prefs = context.dataStore.data.first()
        if (prefs[inboxParseV3Key] == true) return false
        context.dataStore.edit {
            it[inboxParseV3Key] = true
            it[lastInboxSmsDateKey] = 0L
        }
        return true
    }

    /** Re-read CBEBirr (Afaan Oromo) and MPESA texts that the old keywords skipped. */
    suspend fun consumeInboxParseV5(): Boolean {
        val prefs = context.dataStore.data.first()
        if (prefs[inboxParseV5Key] == true) return false
        context.dataStore.edit {
            it[inboxParseV5Key] = true
            it[lastInboxSmsDateKey] = 0L
        }
        return true
    }

    /** Re-read SMS so bank transfers into CBE Birr stop counting as wallet spending. */
    suspend fun consumeInboxParseV4(): Boolean {
        val prefs = context.dataStore.data.first()
        if (prefs[inboxParseV4Key] == true) return false
        context.dataStore.edit {
            it[inboxParseV4Key] = true
            it[lastInboxSmsDateKey] = 0L
        }
        return true
    }

    /**
     * Fixed moment tracking began — used by the worker to ignore older SMS.
     * Must NOT advance when scanning the inbox (unlike lastInboxSmsDate).
     */
    suspend fun getTrackingStartedAt(): Long {
        val prefs = context.dataStore.data.first()
        val started = prefs[trackingStartedAtKey] ?: 0L
        if (started > 0L) return started
        // Upgrade path from builds that only stored the inbox cursor.
        val balances = prefs[balancesSetKey] == true
        val legacy = prefs[lastInboxSmsDateKey] ?: 0L
        if (balances && legacy > 0L) {
            context.dataStore.edit { it[trackingStartedAtKey] = legacy }
            return legacy
        }
        return 0L
    }

    suspend fun setTrackingStartedAt(date: Long) {
        context.dataStore.edit { it[trackingStartedAtKey] = date }
    }

    /** One-time clear of mistaken Cash opening balances from older builds. */
    suspend fun consumeCashOpeningReset(): Boolean {
        val prefs = context.dataStore.data.first()
        if (prefs[cashOpeningResetKey] == true) return false
        context.dataStore.edit { it[cashOpeningResetKey] = true }
        return true
    }
}
