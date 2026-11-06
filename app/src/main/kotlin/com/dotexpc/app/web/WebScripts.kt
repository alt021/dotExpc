package com.dotexpc.app.web

import android.content.Context
import java.io.ByteArrayOutputStream

/** 读取 assets 中的投影脚本，只读一次。 */
class WebScripts(context: Context) {
    private val assets = context.applicationContext.assets

    val officialIdentity: String = read("official-identity.js")
    val loginReady: String = read("login-ready.js")
    val loginChrome: String = read("taobao-login-chrome.js")
    val mode: String = read("dexp-mode.js")

    val pddCompatibility: String =
        read("pdd-same-origin.js") + "\n" +
            read("pdd-section-refresh.js") + "\n" +
            read("pdd-layout.js")

    val taobaoShell: String = read("taobao.html")
        .replace("@@REFRESH_CONTROLLER@@", read("refresh-controller.js"))
        .replace("@@OFFICIAL_IDENTITY@@", officialIdentity)
        .replace("@@IDENTITY_URL@@", android.text.TextUtils.htmlEncode(UrlPolicy.TAOBAO_IDENTITY_ENTRY))
        .replace("@@PARCELS_URL@@", android.text.TextUtils.htmlEncode(UrlPolicy.TAOBAO_PARCELS_ENTRY))

    private fun read(name: String): String {
        return assets.open(name).use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                output.write(buffer, 0, count)
            }
            output.toString(Charsets.UTF_8.name())
        }
    }
}
