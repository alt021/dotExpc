package com.dotexpc.app.data

import android.content.Context
import java.io.File
import java.util.Locale

/**
 * WebView 与临时目录的磁盘缓存。
 *
 * WebView 内核版本不同，HTTP 缓存目录名也不一样（旧版 `HTTP Cache`，新版 `Cache`），
 * 所以按目录名在整个 app_webview 树里识别，避免漏算导致清理阈值永远不触发。
 * 只删除缓存目录，Cookie、Local Storage、IndexedDB 一概不碰，清理后不需要重新登录。
 */
object CacheStore {

    private val CACHE_DIR_NAMES = setOf(
        "Cache",
        "HTTP Cache",
        "Code Cache",
        "GPUCache",
        "ShaderCache",
        "GrShaderCache",
        "DawnCache",
        "DawnGraphiteCache",
        "DawnWebGPUCache",
        "GraphiteDawnCache",
        "Application Cache",
        "CacheStorage",
        "ScriptCache",
    )

    fun size(context: Context): Long =
        (webViewCacheDirs(context) + context.cacheDir).sumOf { it.sizeRecursively() }

    fun clear(context: Context): Long {
        val before = size(context)
        webViewCacheDirs(context).forEach { it.deleteRecursively() }
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        return (before - size(context)).coerceAtLeast(0L)
    }

    fun format(bytes: Long): String = when {
        bytes >= 1L shl 30 -> String.format(Locale.US, "%.1f GB", bytes / 1073741824f)
        bytes >= 1L shl 20 -> String.format(Locale.US, "%.1f MB", bytes / 1048576f)
        bytes >= 1L shl 10 -> String.format(Locale.US, "%.0f KB", bytes / 1024f)
        else -> "$bytes B"
    }

    private fun webViewCacheDirs(context: Context): List<File> {
        val root = File(context.applicationInfo.dataDir, "app_webview")
        if (!root.isDirectory) return emptyList()
        val dirs = ArrayList<File>()
        fun collect(dir: File) {
            dir.listFiles()?.forEach { child ->
                if (!child.isDirectory) return@forEach
                // 命中缓存目录就不再向下找，避免把内部的同名子目录重复统计。
                if (child.name in CACHE_DIR_NAMES) dirs.add(child) else collect(child)
            }
        }
        collect(root)
        return dirs
    }

    private fun File.sizeRecursively(): Long =
        if (!exists()) 0L else walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
