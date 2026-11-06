package com.dotexpc.app.web

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/** 官方内容与登录域名白名单。只允许 HTTPS 主框架停留在官方站点。 */
object UrlPolicy {
    const val PDD_ENTRY = "https://m.pinduoduo.net/mdkd/package?tab=ID_CODE"
    const val PDD_IDENTITY_APP_LINK =
        "pinduoduo://com.xunmeng.pinduoduo/mdkd/package?tab=ID_CODE"
    /** 官方 App 的「取快递」页（待取包裹 tab）。 */
    const val PDD_PACKAGE_APP_LINK =
        "pinduoduo://com.xunmeng.pinduoduo/mdkd/package?tab=PACKAGE"
    const val PDD_PACKAGE_WEB = "https://m.pinduoduo.net/mdkd/package?tab=PACKAGE"

    const val TAOBAO_IDENTITY_ENTRY =
        "https://pages-fast.m.taobao.com/wow/z/uniapp/1011717/last-mile-fe/end-collect-platform/identity-code"
    const val TAOBAO_PARCELS_ENTRY =
        "https://pages-fast.m.taobao.com/wow/z/uniapp/1100333/last-mile-fe/m-end-school-tab/home"
    /** 运单号查取件码的独立页面，打开即是一个可输入的搜索框。 */
    const val TAOBAO_SEARCH_ENTRY =
        "https://pages-fast.m.taobao.com/wow/z/uniapp/1011717/last-mile-fe/end-collect-platform/school-search?x-ssr=true"

    private val TAOBAO_HOSTS =
        arrayOf("taobao.com", "tmall.com", "alipay.com", "cainiao.com", "alibaba.com")

    /** 登录可能跳转到 yangkeduo.com 等官方域名，主框架导航需要放行。 */
    private val PDD_HOSTS = arrayOf("pinduoduo.net", "pinduoduo.com", "yangkeduo.com")

    fun isTaobaoAllowed(raw: String?): Boolean {
        val uri = httpsUri(raw) ?: return false
        val host = uri.host.lowercase(Locale.ROOT)
        return TAOBAO_HOSTS.any { host == it || host.endsWith(".$it") }
    }

    fun isTaobaoIdentityPage(raw: String?): Boolean {
        val uri = httpsUri(raw) ?: return false
        val expected = httpsUri(TAOBAO_IDENTITY_ENTRY) ?: return false
        return uri.host.equals(expected.host, ignoreCase = true) && uri.path == expected.path
    }

    fun isPddAllowed(raw: String?): Boolean {
        val uri = httpsUri(raw) ?: return false
        val host = uri.host.lowercase(Locale.ROOT)
        return PDD_HOSTS.any { host == it || host.endsWith(".$it") }
    }

    fun isPddLoginPage(raw: String?): Boolean {
        val uri = httpsUri(raw) ?: return false
        if (!isPddAllowed(raw)) return false
        val path = uri.path?.lowercase(Locale.ROOT) ?: return false
        return path.contains("login") || path.contains("passport")
    }

    fun isPddBundle(raw: String?): Boolean {
        val uri = httpsUri(raw) ?: return false
        return "pfile.pddpic.com".equals(uri.host, ignoreCase = true) &&
            uri.path?.matches(Regex("/mdkd/mdkd/_next/static/chunks/pages/_app-[0-9a-f]+\\.js")) == true
    }

    fun pddRefreshPart(raw: String?): String? {
        val uri = httpsUri(raw) ?: return null
        if (!"pfile.pddpic.com".equals(uri.host, ignoreCase = true)) return null
        val path = uri.path ?: return null
        return when {
            path.matches(Regex("/mdkd/mdkd/_next/static/chunks/1235-[0-9a-f]+\\.js")) -> "identity"
            path.matches(Regex("/mdkd/mdkd/_next/static/chunks/8905-[0-9a-f]+\\.js")) -> "parcels"
            else -> null
        }
    }

    fun isPddLoginReturn(raw: String?): Boolean {
        val uri = httpsUri(raw) ?: return false
        return isPddAllowed(raw) && uri.path == "/index.html"
    }

    /** 登录页上的「打开拼多多APP」只在用户手势、主框架、目标完全匹配时交给官方 App。 */
    fun pddOpensIdentityApp(current: String?, target: String, gesture: Boolean): Boolean {
        return gesture && isPddLoginPage(current) && target == PDD_IDENTITY_APP_LINK
    }

    private fun httpsUri(raw: String?): URI? {
        if (raw.isNullOrEmpty() || raw.length > 8192) return null
        for (char in raw) {
            if (char.isISOControl() || char.isWhitespace() || char == '\\') return null
        }
        return try {
            val uri = URI(raw)
            if (!"https".equals(uri.scheme, ignoreCase = true)) return null
            if (uri.rawUserInfo != null) return null
            if (uri.port != -1 && uri.port != 443) return null
            if (uri.host == null) null else uri
        } catch (_: URISyntaxException) {
            null
        }
    }
}
