package com.dotexpc.app.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dotexpc.app.data.Platform
import org.json.JSONObject

/**
 * 一个平台的官方页面会话。
 *
 * 每个平台只保留一个 WebView：官方页面（以及淘宝的合并页）常驻，
 * 三个标签页共享它。切换页面只是把同一个 View 挂到不同卡片容器上，
 * 不会重新登录或重新加载。
 *
 * 所有内容仍由官方页面生成：应用不请求业务接口、不读取 Cookie、
 * 不保存任何码值。原生层只接收「是否已绑定 / 待取件数 / 身份码是否出现」。
 */
class PlatformSession(
    private val context: Context,
    val platform: Platform,
) {
    var state by mutableStateOf(WebState())
        private set

    private val main = Handler(Looper.getMainLooper())
    private val scripts = WebScripts(context)
    private val pddBundles by lazy { PddBundles(scripts.pddCompatibility) }

    private var web: WebView? = null
    private val containers = ArrayList<ViewGroup>()
    private var merged = false
    private var sawPddLogin = false
    private var returnedAfterLogin = false
    private var pddBundleAdjusted = false
    private var monitorRunning = false

    private val mergeTick = object : Runnable {
        override fun run() {
            val view = web ?: return
            if (platform == Platform.TAOBAO && !merged && view.parent != null) {
                tryMergeTaobao(view)
            }
            main.postDelayed(this, 2000)
        }
    }

    fun attach(container: ViewGroup) {
        val view = ensureWebView()
        // 最近一次挂载的容器排在最前。子页压在标签页上时由子页接管 WebView，
        // 子页销毁后要把同一个 View 还给它下面仍然存在的容器。
        containers.remove(container)
        containers.add(0, container)
        moveTo(view, container)
        view.onResume()
        view.resumeTimers()
        applyMode(view)
        startMonitor()
    }

    fun detach(container: ViewGroup) {
        val view = web ?: return
        containers.remove(container)
        if (view.parent === container) container.removeView(view)
        if (view.parent == null) {
            containers.firstOrNull { it.isAttachedToWindow }?.let { next -> moveTo(view, next) }
        }
    }

    private fun moveTo(view: WebView, container: ViewGroup) {
        if (view.parent === container) return
        (view.parent as? ViewGroup)?.removeView(view)
        container.addView(
            view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    /** 供导入页复用同一个 WebView：登录态、页面和脚本上下文都在这个实例里。 */
    fun evaluateJavascript(script: String) {
        web?.evaluateJavascript(script) { }
    }

    fun evaluateJavascript(script: String, onResult: (String?) -> Unit) {
        val view = web
        if (view == null) {
            onResult(null)
            return
        }
        view.evaluateJavascript(script) { result -> onResult(result) }
    }

    fun refresh() {        val view = web ?: return
        view.evaluateJavascript(
            "(function(){try{" +
                "if(window.DexpMode&&DexpMode.refresh())return true;" +
                "}catch(e){}return false;})()",
        ) { handled ->
            if (handled != "true") view.reload()
        }
    }

    /** 只清资源缓存，Cookie、Local Storage 和登录态保持不变。 */
    fun clearWebViewCache() {
        web?.clearCache(true)
    }

    fun destroy() {
        main.removeCallbacks(mergeTick)
        web?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            view.destroy()
        }
        web = null
        containers.clear()
    }

    /** 解除绑定：清掉该平台在本机的登录态，再重新打开官方页面。 */
    fun unbind() {
        val view = web ?: return
        merged = false
        sawPddLogin = false
        returnedAfterLogin = false
        state = WebState(phase = "loading")

        view.evaluateJavascript(
            "(function(){try{localStorage.clear();sessionStorage.clear();}catch(e){}})()",
            null,
        )
        expireCookies(if (platform == Platform.TAOBAO) TAOBAO_COOKIE_HOSTS else PDD_COOKIE_HOSTS)
        view.postDelayed({
            if (web === view) {
                view.loadUrl(
                    if (platform == Platform.TAOBAO) UrlPolicy.TAOBAO_IDENTITY_ENTRY else UrlPolicy.PDD_ENTRY,
                )
            }
        }, 250)
    }

    /**
     * CookieManager 不支持按域名删除，只能逐个读出来再用过期时间覆盖。
     * 同时写一份带 Domain 的过期项，覆盖官方站点挂在父域上的登录 Cookie。
     */
    private fun expireCookies(hosts: List<String>) {
        val manager = CookieManager.getInstance()
        hosts.forEach hostLoop@{ host ->
            val url = "https://$host/"
            val header = manager.getCookie(url) ?: return@hostLoop
            val domain = host.split('.').takeLast(2).joinToString(".")
            header.split(';').forEach cookieLoop@{ pair ->
                val name = pair.substringBefore('=').trim()
                if (name.isEmpty()) return@cookieLoop
                manager.setCookie(url, "$name=; Max-Age=0; Path=/")
                manager.setCookie(url, "$name=; Max-Age=0; Path=/; Domain=.$domain")
            }
        }
        manager.flush()
    }

    // ------------------------------------------------------------------ WebView

    @SuppressLint("SetJavaScriptEnabled")
    private fun ensureWebView(): WebView {
        web?.let { return it }

        val view = WebView(context)
        view.setBackgroundColor(Color.TRANSPARENT)
        view.isVerticalScrollBarEnabled = false

        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            // device-width 取的是整屏宽度，而卡片给 WebView 留了左右各 16dp。
            // 按 meta 视口排版时页面按整屏宽度排、控件只有卡片宽度，
            // 内容会整体右移 16dp 并把右侧裁掉；关掉宽视口后布局宽度等于控件宽度。
            useWideViewPort = false
            loadWithOverviewMode = false
            mediaPlaybackRequiresUserGesture = true
        }

        CookieManager.getInstance().setAcceptCookie(true)
        if (platform == Platform.PDD) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(view, false)
        }

        view.addJavascriptInterface(Bridge(), "DexpBridge")
        view.webViewClient = if (platform == Platform.TAOBAO) taobaoClient() else pddClient()
        view.webChromeClient = chromeClient()
        web = view

        view.loadUrl(
            if (platform == Platform.TAOBAO) UrlPolicy.TAOBAO_IDENTITY_ENTRY else UrlPolicy.PDD_ENTRY,
        )
        return view
    }

    private fun chromeClient() = object : WebChromeClient() {
        override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
            val text = message.message() ?: return false
            if (text == "__HANDYCODE_PDD_AUTH__:signed-in" && platform == Platform.PDD) {
                state = state.copy(bound = true)
                return true
            }
            return false
        }

        override fun onPermissionRequest(request: android.webkit.PermissionRequest) {
            request.deny()
        }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun report(json: String) {
            if (json.length > 4096) return
            val next = runCatching {
                val data = JSONObject(json)
                if (data.optString("platform") != platform.id) return
                WebState(
                    bound = data.optBoolean("bound", false),
                    identityReady = data.optBoolean("identity", false),
                    phase = data.optString("phase", "loading"),
                )
            }.getOrNull() ?: return
            main.post { state = next }
        }
    }

    private fun applyMode(view: WebView) {
        view.evaluateJavascript(
            "window.DEXP_PLATFORM='${platform.id}';" + scripts.mode,
            null,
        )
    }

    private fun startMonitor() {
        if (monitorRunning) return
        monitorRunning = true
        main.post(mergeTick)
    }

    // ------------------------------------------------------------------- 淘宝

    private fun taobaoClient() = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean {
            if (!request.isForMainFrame) return false
            return !UrlPolicy.isTaobaoAllowed(request.url.toString())
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            merged = false
            state = WebState(phase = "loading")
        }

        override fun onPageFinished(view: WebView, url: String?) {
            if (!UrlPolicy.isTaobaoAllowed(url)) return
            CookieManager.getInstance().flush()
            view.evaluateJavascript(
                scripts.loginChrome + "\n" + scripts.officialIdentity,
                null,
            )
            applyMode(view)
            tryMergeTaobao(view)
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            if (request.isForMainFrame) state = WebState(phase = "error")
        }

        override fun onReceivedSslError(
            view: WebView,
            handler: SslErrorHandler,
            error: SslError,
        ) {
            handler.cancel()
            state = WebState(phase = "error")
        }
    }

    private fun tryMergeTaobao(view: WebView) {
        if (merged || !UrlPolicy.isTaobaoIdentityPage(view.url)) return
        view.evaluateJavascript(scripts.officialIdentity + "\n" + scripts.loginReady) { ready ->
            if (ready == "true") {
                mergeTaobao(view)
            } else if (!merged) {
                // 身份码暂未生成时，有界地触发官方刷新控件（登录页内自动跳过）。
                view.evaluateJavascript(
                    "window.HandyCodeOfficialIdentity&&HandyCodeOfficialIdentity.recover(document,Date.now())",
                    null,
                )
            }
        }
    }

    private fun mergeTaobao(view: WebView) {
        val markup = JSONObject.quote(scripts.taobaoShell)
        val script = scripts.officialIdentity + "\n" +
            "(function(){if(document.getElementById('handycode-merged'))return true;" +
            "if(!(${scripts.loginReady}))return false;" +
            "document.open();document.write($markup);document.close();return true;})()"
        view.evaluateJavascript(script) { result ->
            if (result == "true") {
                merged = true
                state = state.copy(bound = true)
                applyMode(view)
            }
        }
    }

    // ---------------------------------------------------------------- 拼多多

    private fun pddClient() = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean {
            if (!request.isForMainFrame) return false
            val url = request.url.toString()
            if (UrlPolicy.pddOpensIdentityApp(view.url, url, request.hasGesture())) {
                runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, request.url)) }
                return true
            }
            return !UrlPolicy.isPddAllowed(url)
        }

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ): WebResourceResponse? {
            if (request.method != "GET") return null
            val address = request.url.toString()
            val bundle = UrlPolicy.isPddBundle(address)
            val part = UrlPolicy.pddRefreshPart(address)
            if (!bundle && part == null) return null

            val response = pddBundles.response(address, bundle, part) ?: return null
            if (bundle) pddBundleAdjusted = true
            return response
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            if (UrlPolicy.isPddLoginPage(url)) {
                sawPddLogin = true
                state = WebState(phase = "login")
            } else {
                state = WebState(phase = "loading")
            }
            if (!UrlPolicy.isPddAllowed(url)) view.stopLoading()
        }

        override fun onPageFinished(view: WebView, url: String?) {
            if (!UrlPolicy.isPddAllowed(url)) return
            CookieManager.getInstance().flush()

            // 官方验证流程偶尔会丢掉返回地址；只在确实登录过后返回一次，避免重定向循环。
            if (UrlPolicy.isPddLoginReturn(url) && sawPddLogin && !returnedAfterLogin) {
                returnedAfterLogin = true
                view.post { if (web === view) view.loadUrl(UrlPolicy.PDD_ENTRY) }
                return
            }

            applyMode(view)
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            if (request.isForMainFrame) state = WebState(phase = "error")
        }

        override fun onReceivedSslError(
            view: WebView,
            handler: SslErrorHandler,
            error: SslError,
        ) {
            handler.cancel()
            state = WebState(phase = "error")
        }

        override fun onRenderProcessGone(
            view: WebView,
            detail: android.webkit.RenderProcessGoneDetail,
        ): Boolean {
            if (web === view) {
                (view.parent as? ViewGroup)?.removeView(view)
                view.destroy()
                web = null
                state = WebState(phase = "error")
            }
            return true
        }
    }
}

// 解除绑定时清理登录态的站点；父域 Cookie 通过 Domain 过期项一并覆盖。
private val TAOBAO_COOKIE_HOSTS = listOf(
    "taobao.com",
    "www.taobao.com",
    "m.taobao.com",
    "login.taobao.com",
    "h5api.m.taobao.com",
    "pages-fast.m.taobao.com",
    "tmall.com",
    "www.tmall.com",
    "m.tmall.com",
    "alipay.com",
    "www.alipay.com",
    "cainiao.com",
    "www.cainiao.com",
    "alibaba.com",
    "passport.alibaba.com",
)

private val PDD_COOKIE_HOSTS = listOf(
    "pinduoduo.net",
    "m.pinduoduo.net",
    "pinduoduo.com",
    "www.pinduoduo.com",
    "m.pinduoduo.com",
    "yangkeduo.com",
    "www.yangkeduo.com",
    "mobile.yangkeduo.com",
)
