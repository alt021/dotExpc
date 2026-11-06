package com.dotexpc.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 每日一言：句子与出处随日期一起缓存，同一天只向官方接口请求一次，避免触发限流。
 * 接口只返回句子文本和出处，不涉及账号或设备标识。
 */
class HitokotoStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    init {
        // 句子长度上限从 34 字收紧到 13 字后，旧缓存可能超长：清掉日期，让下次进入主页重新获取。
        if (prefs.getInt(KEY_CACHE_VERSION, 0) < CACHE_VERSION) {
            prefs.edit()
                .putInt(KEY_CACHE_VERSION, CACHE_VERSION)
                .remove(KEY_DATE)
                .apply()
        }
    }

    var sentence by mutableStateOf(prefs.getString(KEY_SENTENCE, "").orEmpty())
        private set

    var author by mutableStateOf(prefs.getString(KEY_AUTHOR, "").orEmpty())
        private set

    private val mutex = Mutex()

    /** 今天还没请求过才发请求；请求失败也记下日期，今天不再重试。 */
    suspend fun refreshIfNeeded() {
        mutex.withLock {
            if (today() == prefs.getString(KEY_DATE, null)) return
            fetchAndSave()
        }
    }

    /** 手动刷新：忽略当天的缓存，直接再请求一次；已有请求在跑时等它结束再来一次。 */
    suspend fun refreshNow() {
        mutex.withLock { fetchAndSave() }
    }

    private suspend fun fetchAndSave() {
        val result = withContext(Dispatchers.IO) { fetch() }
        if (result != null) {
            sentence = result.first
            author = result.second
        }
        prefs.edit()
            .putString(KEY_DATE, today())
            .putString(KEY_SENTENCE, sentence)
            .putString(KEY_AUTHOR, author)
            .apply()
    }

    /** 返回「句子 to 出处」；任一环节失败都返回 null，保留上一次的内容。 */
    private fun fetch(): Pair<String, String>? {
        val connection = try {
            URL(API).openConnection() as HttpURLConnection
        } catch (_: Exception) {
            return null
        }
        return try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("User-Agent", "dotExpc/0.9.0")
            if (connection.responseCode != 200) return null
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(body)
            val text = json.optString("hitokoto").trim()
            if (text.isEmpty()) return null
            text to json.optString("from").trim()
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private companion object {
        const val API = "https://v1.hitokoto.cn/?encode=json&charset=utf-8&min_length=6&max_length=13"
        const val PREFS = "hitokoto"
        const val KEY_DATE = "date"
        const val KEY_SENTENCE = "sentence"
        const val KEY_AUTHOR = "author"
        const val KEY_CACHE_VERSION = "cache_version"
        const val CACHE_VERSION = 1
    }
}
