package com.dotexpc.app.web

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent

/**
 * 打开外部链接：默认浏览器支持应用内浏览（Custom Tabs）时用它打开，
 * 否则直接跳到浏览器打开对应链接。
 */
fun openExternalLink(context: Context, url: String) {
    val uri = Uri.parse(url)
    val browser = runCatching { CustomTabsClient.getPackageName(context, null) }.getOrNull()
    try {
        if (browser != null) {
            val customTabs = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabs.intent.setPackage(browser)
            customTabs.launchUrl(context, uri)
        } else {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "未找到可打开链接的浏览器", Toast.LENGTH_SHORT).show()
    }
}
