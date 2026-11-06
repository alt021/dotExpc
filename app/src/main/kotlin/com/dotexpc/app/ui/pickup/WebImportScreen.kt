package com.dotexpc.app.ui.pickup

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import com.dotexpc.app.data.PickupStore
import com.dotexpc.app.data.Platform
import com.dotexpc.app.ui.component.AppIcons
import com.dotexpc.app.ui.component.SessionWeb
import com.dotexpc.app.web.PlatformSession
import com.dotexpc.app.web.UrlPolicy
import org.json.JSONTokener
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 取件码形如 12-3-4567 或 1-2-3456，全部是数字。
 * 页面上同一个码可能夹空格、换行，或用全角数字、各种连字符（‐‑‒–—−）拼出来。
 */
private val PICKUP_CODE = Regex(
    """(?<!\d)(\d{1,2})\s*[-‐‑‒–—−﹣－]\s*(\d)\s*[-‐‑‒–—−﹣－]\s*(\d{4})(?!\d)""",
)

/** 全角数字换半角，不可见字符换空格，保证同一个码在不同排版下匹配出同一个值。 */
private fun normalizeText(text: String): String = buildString(text.length) {
    text.forEach { ch ->
        append(
            when {
                ch in '０'..'９' -> '0' + (ch - '０')
                ch == '\u00A0' || ch == '\u200B' || ch == '\uFEFF' -> ' '
                else -> ch
            },
        )
    }
}

/**
 * 取当前页面里能看到的文字；同源内嵌页面能读到的也一并带上，读不到的静默跳过。
 * 再附上页面源码，兜住只写在属性里的码；重复部分由调用方统一去重。
 */
private const val SOURCE_SCRIPT = """(function(){
    var parts=[];
    function text(d){
        try{
            var t=(d.body&&d.body.innerText)||(d.body&&d.body.textContent)||'';
            if(t)parts.push(t);
        }catch(e){}
    }
    text(document);
    for(var i=0;i<window.frames.length;i++){
        try{ var d=window.frames[i].document; if(d) text(d); }catch(e){}
    }
    parts.push(document.documentElement?document.documentElement.outerHTML:'');
    for(var i=0;i<window.frames.length;i++){
        try{
            var d=window.frames[i].document;
            if(d&&d.documentElement)parts.push(d.documentElement.outerHTML);
        }catch(e){}
    }
    return parts.join('\n');
})()"""

/**
 * 隐藏底部的「同学都在买」推荐流：标题是一张固定图片，列表容器带 data-role 标记，
 * 两者都与类名哈希无关。用样式表而不是删节点，SPA 重新渲染后依然生效。
 */
private const val HIDE_FEED_SCRIPT = """(function(){
    if (document.getElementById('dotexpc-hide-feed')) return;
    var style = document.createElement('style');
    style.id = 'dotexpc-hide-feed';
    style.textContent = 'img[class*="titleimg--"],img[src*="O1CN0147N91n1fkf9byrqj6"],[data-role^="mama-feeds"],[data-role="mama-card-feeds"]{display:none!important}';
    (document.head || document.documentElement).appendChild(style);
})()"""

/**
 * 拼多多导入要露出官方原始页面：先切到「取快递」，收起投影和自己的返回条，
 * 让官方页面占满导入页。离开时由 EXIT_SCRIPT 还原。
 */
private const val ENTER_SCRIPT = """(function(){try{
    if(!window.HandyPddLayout||!window.HandyPddLayout.source('取快递'))return false;
    var bar=document.getElementById('hc-pdd-back');if(bar)bar.hidden=true;
    var src=document.querySelector('[data-hc-pdd-source]');
    if(src){src.style.setProperty('top','0','important');src.style.setProperty('height','100%','important');}
    return true;}catch(e){return false;}})()"""

private const val EXIT_SCRIPT = """(function(){try{
    var src=document.querySelector('[data-hc-pdd-source]');
    if(src){src.style.removeProperty('top');src.style.removeProperty('height');}
    if(window.HandyPddLayout)window.HandyPddLayout.cards('身份码');
    return true;}catch(e){return true;}})()"""

/**
 * 平台网页导入页：打开官方页面供用户自己登录浏览，点右上角「导入」时只读取当前页面
 * 源码做本地正则匹配，不请求任何接口。
 *
 * 拼多多直接复用身份码页的 WebView：登录态和已登入的页面都在那个实例里，另起一个
 * WebView 即使共享 Cookie，官方页面仍会判定未登录（导入页曾因此白屏）。
 */
@Composable
fun WebImportScreen(
    pickups: PickupStore,
    platform: Platform,
    session: PlatformSession?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var importing by remember { mutableStateOf(false) }
    var importedCount by remember { mutableStateOf(0) }
    var showImported by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Toast.makeText(context, "找到取件码后点击右上角导入", Toast.LENGTH_SHORT).show()
    }

    val shared = if (platform == Platform.PDD) session else null

    val entryUrl = UrlPolicy.TAOBAO_SEARCH_ENTRY
    val title = when (platform) {
        Platform.TAOBAO -> "菜鸟导入"
        Platform.PDD -> "多多导入"
    }

    fun evaluate(script: String, onResult: (String?) -> Unit) {
        val target = shared
        if (target != null) {
            target.evaluateJavascript(script, onResult)
        } else {
            webView?.evaluateJavascript(script) { result -> onResult(result) }
        }
    }

    // 进入时把官方页面切到「取快递」，离开时恢复身份码投影。页面可能还没加载完，重试到就绪。
    DisposableEffect(shared) {
        val target = shared
        if (target == null) {
            onDispose { }
        } else {
            val main = Handler(Looper.getMainLooper())
            var tries = 0
            val enter = object : Runnable {
                override fun run() {
                    target.evaluateJavascript(ENTER_SCRIPT) { result ->
                        if (result != "true" && tries++ < 75) main.postDelayed(this, 200)
                    }
                }
            }
            main.post(enter)
            onDispose {
                main.removeCallbacks(enter)
                target.evaluateJavascript(EXIT_SCRIPT)
            }
        }
    }

    fun goBack() {
        val view = webView
        if (shared == null && view?.canGoBack() == true) view.goBack() else onClose()
    }

    BackHandler { goBack() }

    fun importFromPage() {
        if (importing) return
        if (shared == null && webView == null) return
        importing = true
        evaluate(SOURCE_SCRIPT) { result ->
            importing = false
            val source = runCatching { JSONTokener(result).nextValue() as String }.getOrNull()
            val codes = source?.let { raw ->
                PICKUP_CODE.findAll(normalizeText(raw))
                    .map { match ->
                        val (head, middle, tail) = match.destructured
                        "$head-$middle-$tail"
                    }
                    // 同一个码常会以多处排版出现，归一化后只留一份，保持页面里的先后顺序。
                    .distinct()
                    .toList()
            }.orEmpty()
            if (codes.isEmpty()) {
                Toast.makeText(context, "当前页面没有找到取件码", Toast.LENGTH_SHORT).show()
                return@evaluate
            }
            // 菜鸟导入统一归到「淘宝」，多多导入统一归到「拼多多」：platform 就是目标分类。
            val added = pickups.addAll(codes, platform)
            if (added > 0) {
                importedCount = added
                showImported = true
            } else {
                Toast.makeText(context, "这些取件码已经导入过了", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        TopAppBar(
            title = title,
            navigationIcon = {
                // 站内跳转（如搜索、身份码）是同窗口打开的，先退历史再退出页面。
                IconButton(onClick = { goBack() }) {
                    Icon(
                        imageVector = AppIcons.Back,
                        contentDescription = "返回",
                    )
                }
            },
            actions = {
                TextButton(text = "导入", onClick = { importFromPage() })
            },
        )
        if (shared != null) {
            SessionWeb(
                session = shared,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .navigationBarsPadding(),
            )
        } else {
            AndroidView(
                factory = { container ->
                    createImportWebView(container).also { view ->
                        webView = view
                        // 页面首屏会按窗口高度算绝对定位，尺寸为 0 时加载会让推荐流压住取件列表，
                        // 所以等真正拿到尺寸后再请求。
                        var requested = false
                        view.doOnLayout {
                            if (!requested && view.width > 0 && view.height > 0) {
                                requested = true
                                view.loadUrl(entryUrl)
                            }
                        }
                    }
                },
                onRelease = { view ->
                    if (webView === view) webView = null
                    (view.parent as? ViewGroup)?.removeView(view)
                    view.destroy()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .navigationBarsPadding(),
            )
        }
    }

    // 导入成功后停在页面上提示结果，确认（或点空白收起）再退回取件码页。
    OverlayDialog(
        show = showImported,
        title = "导入完成",
        summary = "已导入 $importedCount 个取件码至${platform.title}",
        onDismissRequest = { onClose() },
    ) {
        Button(
            onClick = { onClose() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "好的")
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createImportWebView(context: Context): WebView {
    val view = WebView(context)

    // 只开页面本身需要的能力，视口、缩放、混合内容全部留 WebView 默认值：
    // 一旦替站点决定排版方式，它按自身度量算出的固定定位就会出现错位和遮挡。
    view.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        allowFileAccess = false
        allowContentAccess = false
        mediaPlaybackRequiresUserGesture = true
        javaScriptCanOpenWindowsAutomatically = true
        setSupportMultipleWindows(true)
    }

    // Cookie 由进程级 CookieManager 统一管理：身份码会话里已经登录过的账号会自动共享
    // 到这里，跨站 Cookie 也放行，否则登录后会立刻掉线。
    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(view, true)
    CookieManager.getInstance().flush()

    view.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            view.evaluateJavascript(HIDE_FEED_SCRIPT, null)
        }

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean {
            if (!request.isForMainFrame) return false
            val scheme = request.url.scheme?.lowercase(Locale.ROOT)
            // http(s) 一律在当前页面内浏览，其余交给系统 App（taobao://、intent:// 等）。
            if (scheme == "http" || scheme == "https") return false
            openExternal(context, request.url.toString(), request.url)
            return true
        }
    }

    view.webChromeClient = object : WebChromeClient() {
        /** window.open / target=_blank 的目标直接在当前页打开，避免新窗口被静默丢弃。 */
        override fun onCreateWindow(
            view: WebView,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message,
        ): Boolean {
            val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
            val pending = WebView(view.context)
            pending.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    target: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    view.loadUrl(request.url.toString())
                    target.destroy()
                    return true
                }
            }
            transport.webView = pending
            resultMsg.sendToTarget()
            return true
        }

        override fun onPermissionRequest(request: PermissionRequest) {
            request.deny()
        }
    }
    return view
}

private fun openExternal(context: Context, url: String, uri: Uri) {
    val intent = if (url.startsWith("intent:", ignoreCase = true)) {
        runCatching { Intent.parseUri(url, Intent.URI_INTENT_SCHEME) }.getOrNull()
    } else {
        Intent(Intent.ACTION_VIEW, uri)
    }
    intent ?: return
    runCatching { context.startActivity(intent) }
}
