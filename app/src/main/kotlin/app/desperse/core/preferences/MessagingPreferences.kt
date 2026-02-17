package app.desperse.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.messagingDataStore: DataStore<Preferences> by preferencesDataStore(name = "messaging_preferences")

/**
 * Manages messaging preferences using DataStore
 * These control who can send direct messages to the user
 *
 * Options:
 * - dmEnabled: Master toggle for DMs
 * - allowBuyers: Allow people who purchased any of your editions
 * - allowCollectors: Allow people who collected 3+ of your collectibles
 * - collectorMinCount: Minimum number of collectibles (default 3)
 */
@Singleton
class MessagingPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dmEnabledKey = booleanPreferencesKey("dm_enabled")
    private val allowBuyersKey = booleanPreferencesKey("allow_buyers")
    private val allowCollectorsKey = booleanPreferencesKey("allow_collectors")
    private val collectorMinCountKey = intPreferencesKey("collector_min_count")
    private val allowTippersKey = booleanPreferencesKey("allow_tippers")
    private val tipMinAmountKey = intPreferencesKey("tip_min_amount")

    val dmEnabled: Flow<Boolean> = context.messagingDataStore.data.map { it[dmEnabledKey] ?: true }
    val allowBuyers: Flow<Boolean> = context.messagingDataStore.data.map { it[allowBuyersKey] ?: true }
    val allowCollectors: Flow<Boolean> = context.messagingDataStore.data.map { it[allowCollectorsKey] ?: true }
    val collectorMinCount: Flow<Int> = context.messagingDataStore.data.map { it[collectorMinCountKey] ?: 3 }
    val allowTippers: Flow<Boolean> = context.messagingDataStore.data.map { it[allowTippersKey] ?: true }
    val tipMinAmount: Flow<Int> = context.messagingDataStore.data.map { it[tipMinAmountKey] ?: 50 }

    suspend fun setDmEnabled(enabled: Boolean) {
        context.messagingDataStore.edit { it[dmEnabledKey] = enabled }
    }

    suspend fun setAllowBuyers(enabled: Boolean) {
        context.messagingDataStore.edit { it[allowBuyersKey] = enabled }
    }

    suspend fun setAllowCollectors(enabled: Boolean) {
        context.messagingDataStore.edit { it[allowCollectorsKey] = enabled }
    }

    suspend fun setCollectorMinCount(count: Int) {
        context.messagingDataStore.edit { it[collectorMinCountKey] = count }
    }

    suspend fun setAllowTippers(enabled: Boolean) {
        context.messagingDataStore.edit { it[allowTippersKey] = enabled }
    }

    suspend fun setTipMinAmount(amount: Int) {
        context.messagingDataStore.edit { it[tipMinAmountKey] = amount }
    }

    /**
     * Update all messaging preferences at once (from server sync)
     */
    suspend fun updateAll(
        dmEnabled: Boolean,
        allowBuyers: Boolean,
        allowCollectors: Boolean,
        collectorMinCount: Int,
        allowTippers: Boolean = true,
        tipMinAmount: Int = 50
    ) {
        context.messagingDataStore.edit {
            it[dmEnabledKey] = dmEnabled
            it[allowBuyersKey] = allowBuyers
            it[allowCollectorsKey] = allowCollectors
            it[collectorMinCountKey] = collectorMinCount
            it[allowTippersKey] = allowTippers
            it[tipMinAmountKey] = tipMinAmount
        }
    }
}
