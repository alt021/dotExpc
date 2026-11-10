package com.dotexpc.app.data

import android.content.Context
import com.dotexpc.app.web.PlatformSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Activity 级会话容器：两个平台的 WebView 常驻，供三个页面共享。 */
class AppSessions(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
    private val cacheLock = Mutex()

    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val taobao = PlatformSession(context, Platform.TAOBAO)
    val pdd = PlatformSession(context, Platform.PDD)

    fun get(platform: Platform): PlatformSession = when (platform) {
        Platform.TAOBAO -> taobao
        Platform.PDD -> pdd
    }

    val all: List<PlatformSession> get() = listOf(taobao, pdd)

    /**
     * 打开应用：距上次打开超过一周，或距上次清理满一周，就清一次缓存。
     * 顺手记下本次打开时间。
     */
    fun onAppOpened() {
        val now = System.currentTimeMillis()
        val lastOpen = prefs.getLong(KEY_LAST_OPEN, 0L)
        val lastClean = prefs.getLong(KEY_LAST_CLEAN, 0L)
        prefs.edit().putLong(KEY_LAST_OPEN, now).apply()

        io.launch {
            cacheLock.withLock {
                val overdue = now - lastClean >= WEEK_MS ||
                    (lastOpen != 0L && now - lastOpen >= WEEK_MS)
                if (!overdue && CacheStore.size(appContext) <= CACHE_LIMIT_BYTES) return@withLock
                clearCaches()
            }
        }
    }

    /** 退到后台：距上次清理满一周或超过体积上限时再清一次。 */
    fun onAppHidden() {
        io.launch {
            cacheLock.withLock {
                val lastClean = prefs.getLong(KEY_LAST_CLEAN, 0L)
                val overdue = System.currentTimeMillis() - lastClean >= WEEK_MS
                if (!overdue && CacheStore.size(appContext) <= CACHE_LIMIT_BYTES) return@withLock
                clearCaches()
            }
        }
    }

    /** 退到后台时把登录 Cookie 落盘，进程随后被系统回收也不丢登录态。 */
    fun persistCookies() {
        io.launch { all.forEach { it.persistCookies() } }
    }

    /** 清掉 WebView 的资源缓存和临时文件；登录态不动。 */
    suspend fun clearCaches() {
        withContext(Dispatchers.Main) { all.forEach { it.clearWebViewCache() } }
        withContext(Dispatchers.IO) {
            CacheStore.clear(appContext)
            prefs.edit().putLong(KEY_LAST_CLEAN, System.currentTimeMillis()).apply()
        }
    }

    fun destroy() {
        io.cancel()
        all.forEach { it.destroy() }
    }

    private companion object {
        const val CACHE_PREFS = "cache_maintenance"
        const val KEY_LAST_OPEN = "last_open"
        const val KEY_LAST_CLEAN = "last_clean"
        const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
        const val CACHE_LIMIT_BYTES = 150L * 1024 * 1024
    }
}
