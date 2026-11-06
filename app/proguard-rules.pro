# WebView 通过反射调用 @JavascriptInterface 方法，名字不能被混淆。
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
