package com.dotexpc.app.web

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream

/**
 * 拼多多官方 Next.js 脚本的下载与本地补丁。
 *
 * 页面把业务请求发到 api.pinduoduo.com，普通浏览环境没有官方 App 的鉴权桥，
 * 只有改写回同源 /proxy/api 才能拿到登录态和取件数据。前缀脚本由调用方决定：
 * 身份码会话要连同投影壳注入，导入页只能给同源改写（投影壳会隐藏官方页面）。
 */
class PddBundles(private val preamble: String) {

    private val cache = ConcurrentHashMap<String, ByteArray>()

    fun response(address: String, appBundle: Boolean, part: String?): WebResourceResponse? {
        val bytes = fetch(address, appBundle, part) ?: return null
        val headers = HashMap<String, String>()
        headers["Cache-Control"] = "no-store"
        headers["Access-Control-Allow-Origin"] = "https://m.pinduoduo.net"
        return WebResourceResponse(
            "application/javascript",
            "UTF-8",
            200,
            "OK",
            headers,
            ByteArrayInputStream(bytes),
        )
    }

    /** 补丁后的脚本；地址不在补丁范围或上游结构变化时返回 null（fail closed）。 */
    private fun fetch(address: String, appBundle: Boolean, part: String?): ByteArray? {
        cache[address]?.let { return it }
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(address).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 12_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept-Encoding", "gzip")
            if (connection.responseCode != 200) return null

            val type = connection.contentType ?: return null
            if (!type.contains("javascript") && !type.contains("ecmascript")) return null

            var input: java.io.InputStream = connection.inputStream
            if ("gzip".equals(connection.contentEncoding, ignoreCase = true)) {
                input = GZIPInputStream(input)
            }
            val raw = input.use {
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = it.read(buffer)
                    if (count == -1) break
                    if (output.size() + count > 6 * 1024 * 1024) return null
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }

            val result = if (part != null) {
                val patched = PddBundleAdapter.patch(part, String(raw, Charsets.UTF_8)) ?: return null
                patched.toByteArray(Charsets.UTF_8)
            } else if (appBundle) {
                preamble.toByteArray(Charsets.UTF_8) + byteArrayOf('\n'.code.toByte()) + raw
            } else {
                raw
            }

            if (cache.size >= 3) cache.clear()
            cache[address] = result
            result
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
