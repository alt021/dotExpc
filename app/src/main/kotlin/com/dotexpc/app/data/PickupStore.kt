package com.dotexpc.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** 标记为已取的件保留 3 天，到期自动删除。 */
const val PICKED_RETENTION_MS = 3L * 24 * 60 * 60 * 1000

/** 一条手动导入的取件码；码、驿站、备注都按完整字符串保存，不做拆分。 */
data class PickupCode(
    val code: String,
    val platform: Platform,
    val note: String = "",
    val picked: Boolean = false,
    val pickedAt: Long = 0L,
) {
    /** 到期自动删除的时间点；未取件的件为 0。 */
    val autoDeleteAt: Long
        get() = if (picked && pickedAt > 0L) pickedAt + PICKED_RETENTION_MS else 0L
}

/** 手动导入的取件码，只保存在本机。 */
class PickupStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var codes by mutableStateOf(load())
        private set

    init {
        // 旧数据里的已取件没有时间戳，从升级那一刻起算；顺手清掉已经过期的。
        val now = System.currentTimeMillis()
        val normalized = codes.map {
            if (it.picked && it.pickedAt <= 0L) it.copy(pickedAt = now) else it
        }
        val kept = normalized.filterNot { it.picked && now - it.pickedAt >= PICKED_RETENTION_MS }
        if (kept != codes) {
            codes = kept
            save()
        }
    }

    /** 主页统计用：还没取的件数。 */
    val pendingCount: Int get() = codes.count { !it.picked }

    /** 成功返回 null，失败返回给用户看的原因。 */
    fun add(rawCode: String, platform: Platform, rawNote: String): String? {
        val code = rawCode.trim()
        val note = rawNote.trim()
        return validate(code, note, self = null) ?: run {
            codes = codes + PickupCode(code = code, platform = platform, note = note)
            save()
            null
        }
    }

    /** 批量导入：跳过长码、重复项和超出上限的条目，返回实际新增数量。 */
    fun addAll(newCodes: List<String>, platform: Platform): Int {
        val additions = mutableListOf<PickupCode>()
        for (raw in newCodes) {
            val code = raw.trim()
            if (code.isEmpty() || code.length > MAX_CODE_LENGTH) continue
            if (codes.size + additions.size >= MAX_COUNT) break
            if (additions.any { it.code.equals(code, ignoreCase = true) }) continue
            if (codes.any { it.code.equals(code, ignoreCase = true) }) continue
            additions += PickupCode(code = code, platform = platform)
        }
        if (additions.isEmpty()) return 0
        codes = codes + additions
        save()
        return additions.size
    }

    /** 编辑后保留原来的已取/未取状态。 */
    fun update(target: PickupCode, rawCode: String, platform: Platform, rawNote: String): String? {
        val code = rawCode.trim()
        val note = rawNote.trim()
        return validate(code, note, self = target) ?: run {
            codes = codes.map {
                if (it == target) it.copy(code = code, platform = platform, note = note) else it
            }
            save()
            null
        }
    }

    fun togglePicked(target: PickupCode, now: Long = System.currentTimeMillis()) {
        codes = codes.map {
            if (it != target) {
                it
            } else if (it.picked) {
                it.copy(picked = false, pickedAt = 0L)
            } else {
                it.copy(picked = true, pickedAt = now)
            }
        }
        save()
    }

    fun remove(target: PickupCode) {
        codes = codes.filterNot { it == target }
        save()
    }

    /** 清掉标记已取超过 [PICKED_RETENTION_MS] 的件。 */
    fun pruneExpired(now: Long = System.currentTimeMillis()) {
        val kept = codes.filterNot {
            it.picked && it.pickedAt > 0L && now - it.pickedAt >= PICKED_RETENTION_MS
        }
        if (kept.size != codes.size) {
            codes = kept
            save()
        }
    }

    private fun validate(code: String, note: String, self: PickupCode?): String? = when {
        code.isEmpty() -> "请输入取件码"
        code.length > MAX_CODE_LENGTH -> "取件码过长"
        note.length > MAX_NOTE_LENGTH -> "备注最多 $MAX_NOTE_LENGTH 个字"
        codes.any { it != self && it.code.equals(code, ignoreCase = true) } -> "这个取件码已经导入过了"
        self == null && codes.size >= MAX_COUNT -> "最多保存 $MAX_COUNT 个取件码"
        else -> null
    }

    private fun load(): List<PickupCode> = runCatching {
        val array = JSONArray(prefs.getString(KEY, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                if (size >= MAX_COUNT) break
                when (val item = array.opt(index)) {
                    is JSONObject -> {
                        val code = item.optString("code").trim()
                        if (code.isEmpty()) continue
                        add(
                            PickupCode(
                                code = code,
                                platform = Platform.fromId(item.optString("platform")) ?: Platform.TAOBAO,
                                note = item.optString("note").trim().take(MAX_NOTE_LENGTH),
                                picked = item.optBoolean("picked", false),
                                pickedAt = item.optLong("picked_at", 0L),
                            ),
                        )
                    }

                    // 旧版本只存码值字符串，加载时按淘宝、未取迁移过来。
                    is String -> {
                        val code = item.trim()
                        if (code.isNotEmpty()) add(PickupCode(code = code, platform = Platform.TAOBAO))
                    }

                    else -> Unit
                }
            }
        }
    }.getOrDefault(emptyList())

    private fun save() {
        val array = JSONArray()
        codes.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("code", item.code)
                    put("platform", item.platform.id)
                    put("note", item.note)
                    put("picked", item.picked)
                    put("picked_at", item.pickedAt)
                },
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private companion object {
        const val PREFS = "pickup_codes"
        const val KEY = "codes"
        const val MAX_COUNT = 50
        const val MAX_CODE_LENGTH = 24
        const val MAX_NOTE_LENGTH = 10
    }
}
