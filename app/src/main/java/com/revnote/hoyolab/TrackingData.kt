package com.revnote.hoyolab

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.serialization.Serializable
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Serializable
data class FPData (
    val fpSeedTime: String = "",
    val fpSeedId: String = "",
    val deviceFp: String = ""
)

@Serializable
data class YsNoteExpedition (
    val avatar_side_icon: String,
    val remained_time: String,
    val status: String
)
@Serializable
data class YsNoteTransformerRecoveryTime (
    val Day: Int,
    val Hour: Int,
    val Minute: Int,
    val Second: Int,
    val reached: Boolean,
)
@Serializable
data class YsNoteTransformer (
    val latest_job_id: String,
    val noticed: Boolean,
    val obtained: Boolean,
    val recovery_time: YsNoteTransformerRecoveryTime,
    val wiki: String
)
@Serializable
data class YsNoteData (
    val calendar_url: String,
    val current_expedition_num: Int,
    val current_home_coin: Int,
    val current_resin: Int,
    val expeditions: List<YsNoteExpedition>,
    val finished_task_num: Int,
    val home_coin_recovery_time: String,
    val is_extra_task_reward_received: Boolean,
    val max_expedition_num: Int,
    val max_home_coin: Int,
    val max_resin: Int,
    val remain_resin_discount_num: Int,
    val resin_discount_num_limit: Int,
    val resin_recovery_time: String,
    val total_task_num: Int,
    val transformer: YsNoteTransformer
)

@Serializable
data class YsTrackingInfo (
    val index: Int,
    val uid: String,
    val cookie: String,
    val gameId: Int,
    val lastRefreshTime: Long,
    val data: YsNoteData
)

@Serializable
data class SrNoteExpedition (
    val avatars: List<String>,
    val name: String,
    val remaining_time: Int,
    val status: String
)
@Serializable
data class SrNoteData (
    val accepted_epedition_num: Int,
    val current_rogue_score: Int,
    val current_stamina: Int,
    val current_train_score: Int,
    val expeditions: List<SrNoteExpedition>,
    val max_rogue_score: Int,
    val max_stamina: Int,
    val max_train_score: Int,
    val stamina_recover_time: Int,
    val total_expedition_num: Int,
    val weekly_cocoon_cnt: Int,
    val weekly_cocoon_limit: Int
)

@Serializable
data class SrTrackingInfo (
    val index: Int,
    val uid: String,
    val bbsUid: String,
    val cookie: String,
    val gameId: Int,
    val lastRefreshTime: Long,
    val data: SrNoteData
)

//@Serializable
//data class YsApi (
//    val retcode: Int,
//    val message: String,
//    val data: YsNoteData
//)
//@Serializable
//data class SrApi (
//    val retcode: Int,
//    val message: String,
//    val data: SrNoteData
//)

class TrackingData(private val context: Context) {

    //保存数据
    suspend fun saveData(key: Preferences.Key<String>, data: String) {
        context.dataStore.edit { preferences ->
            preferences[key] = data
        }
    }
    suspend fun saveData(key: String, data: String) {
        saveData(stringPreferencesKey(key), data);
    }

    //获取数据
    fun getData(key: Preferences.Key<String>): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: ""
        }
    }
    fun getData(key: String): Flow<String> {
        return getData(stringPreferencesKey(key))
    }
    suspend fun getDataSync(key: Preferences.Key<String>): String = suspendCoroutine { continuation ->
        runBlocking {
            val data = context.dataStore.data.first()
            continuation.resume(data[key] ?: "")
        }
    }
    suspend fun getDataSync(key: String): String {
        return getDataSync(stringPreferencesKey(key))
    }

    //删除数据
    suspend fun removeData(key: Preferences.Key<String>) {
        context.dataStore.edit {
            it.remove(key)
        }
    }
    suspend fun removeData(key: String) {
        removeData(stringPreferencesKey(key))
    }

    fun getAllKeys(): Flow<Set<Preferences.Key<*>>> {
        return context.dataStore.data.map { it.asMap().keys }
    }

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "data")
    }
}