package com.dotexpc.app.web

/** 由网页内脚本上报的聚合状态，不含账号、码值或 Cookie。 */
data class WebState(
    val bound: Boolean = false,
    val identityReady: Boolean = false,
    val phase: String = "loading",
) {
    val isLoginRequired: Boolean get() = phase == "login"
    val isLoading: Boolean get() = phase == "loading"
}
