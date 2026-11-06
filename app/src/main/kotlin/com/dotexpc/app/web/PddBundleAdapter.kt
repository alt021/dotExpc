package com.dotexpc.app.web

/**
 * 给拼多多官方只读查询回调挂上生命周期钩子，用于卡片级刷新。
 * 上游脚本结构变化时返回 null（fail closed），绝不误绑回调。
 *
 * 逻辑与 HandyCode 的 PddBundleAdapter 一致（MIT）。
 */
object PddBundleAdapter {

    fun patch(part: String, source: String): String? {
        val hook: String
        val replacement: String
        val outcome: String
        val outcomeReplacement: String

        when (part) {
            "identity" -> {
                hook = "(0,w.p)((function(){pi()})),(0,R.jsxs)(\"div\",{className:V()[\"identification-code-container\"]"
                replacement = "(0,w.p)((function(){pi()})),(0,_.useEffect)((function(){return window.HandyPddRefresh?window.HandyPddRefresh.register(\"identity\",pi):void 0}),[pi]),(0,R.jsxs)(\"div\",{className:V()[\"identification-code-container\"]"
                outcome = "entry_source:O.idcode_entry_source||\"APP\"});case 2:null!==(e=i.sent)&&void 0!==e&&e.success?"
                outcomeReplacement = "entry_source:O.idcode_entry_source||\"APP\"});case 2:e=i.sent,window.HandyPddRefresh&&window.HandyPddRefresh.report(\"identity\",!!e&&!!e.success);null!==e&&void 0!==e&&e.success?"
            }

            "parcels" -> {
                hook = "return[4,3].includes(null===Y||void 0===Y?void 0:Y.pick_up_mode)?"
                replacement = "(0,c.useEffect)((function(){return window.HandyPddRefresh?window.HandyPddRefresh.register(\"parcels\",function(){return kn(!0)}):void 0}),[kn]);" + hook
                outcome = "case 3:t=e.sent,rn(!1),null!==t&&void 0!==t&&t.success?"
                outcomeReplacement = "case 3:t=e.sent,window.HandyPddRefresh&&window.HandyPddRefresh.report(\"parcels\",!!t&&!!t.success),rn(!1),null!==t&&void 0!==t&&t.success?"
            }

            else -> return null
        }

        if (!unique(source, hook) || !unique(source, outcome)) return null
        return source.replace(hook, replacement).replace(outcome, outcomeReplacement)
    }

    private fun unique(source: String, target: String): Boolean {
        val index = source.indexOf(target)
        return index >= 0 && source.indexOf(target, index + target.length) < 0
    }
}
