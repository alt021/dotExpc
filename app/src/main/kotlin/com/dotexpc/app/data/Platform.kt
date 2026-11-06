package com.dotexpc.app.data

/** 支持的两个取件平台。顺序即界面顺序。 */
enum class Platform(
    val id: String,
    val title: String,
) {
    TAOBAO(id = "taobao", title = "淘宝"),
    PDD(id = "pdd", title = "拼多多"),
    ;

    companion object {
        fun fromId(id: String): Platform? = entries.firstOrNull { it.id == id }
    }
}
