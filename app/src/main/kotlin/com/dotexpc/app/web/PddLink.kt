package com.dotexpc.app.web

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** 在官方拼多多 App 里打开取件码页；装不了再退回官方网页。 */
fun openPddIdentity(context: Context) {
    openPdd(context, UrlPolicy.PDD_IDENTITY_APP_LINK, UrlPolicy.PDD_ENTRY)
}

/** 在官方拼多多 App 里打开「取快递」页；装不了再退回官方网页。 */
fun openPddPackage(context: Context) {
    openPdd(context, UrlPolicy.PDD_PACKAGE_APP_LINK, UrlPolicy.PDD_PACKAGE_WEB)
}

private fun openPdd(context: Context, appLink: String, webFallback: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(appLink)))
        return
    } catch (_: ActivityNotFoundException) {
        // 没有处理该 scheme 的应用，走下面的网页兜底。
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webFallback)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "未找到可打开拼多多的应用", Toast.LENGTH_SHORT).show()
    }
}
