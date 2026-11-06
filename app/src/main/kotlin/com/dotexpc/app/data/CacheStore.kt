package com.dotexpc.app.data

import android.content.Context
import java.io.File
import java.util.Locale

/**
 * WebView 与临时目录的磁盘缓存。
 *
 * 只统计和删除 HTTP/代码/GPU 缓存以及 Service Worker 的资源缓存，
 * Cookie、Local Storage、IndexedDB 与数据库一概不碰，清理后不需要重新登录。
 */
object CacheStore {

    private val WEBVIEW_CACHE_PATHS = listOf(
        "Default/HTTP Cache",
        "Default/Code Cache",
        "Default/GPUCache",
        "Default/ShaderCache",
        "Default/Application Cache",
        "Default/Service Worker/CacheStorage",
        "Default/Service Worker/ScriptCache",
    )

    fun size(context: Context): Long =
        (webViewCacheDirs(context) + context.cacheDir).sumOf { it.sizeRecursively() }

    fun clear(context: Context): Long {
        val before = size(context)
        webViewCacheDirs(context).forEach { it.deleteRecursively() }
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        return before - size(context)
    }

    fun format(bytes: Long): String = when {
        bytes >= 1L shl 30 -> String.format(Locale.US, "%.1f GB", bytes / 1073741824f)
        bytes >= 1L shl 20 -> String.format(Locale.US, "%.1f MB", bytes / 1048576f)
        bytes >= 1L shl 10 -> String.format(Locale.US, "%.0f KB", bytes / 1024f)
        else -> "$bytes B"
    }

    private fun webViewCacheDirs(context: Context): List<File> {
        val root = File(context.applicationInfo.dataDir, "app_webview")
        return WEBVIEW_CACHE_PATHS.map { File(root, it) }
    }

    private fun File.sizeRecursively(): Long =
        if (!exists()) 0L else walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
