// dotExpc 注入脚本：分别适配淘宝 / 拼多多的身份码区域，并向原生层上报聚合状态。
// 只读取「是否已绑定、身份码是否出现」，不读取账号、手机号、Cookie 或具体码值。
(function () {
  'use strict';
  if (window.DexpMode) {
    window.DexpMode.apply();
    window.DexpMode.report();
    return;
  }

  var last = '';
  var STYLE_ID = 'dexp-identity-style';

  // 淘宝合并壳：去掉壳自身的边框和「身份码」标题栏，官方身份码内容保持原样。
  var TAOBAO_CSS = [
    'html,body{background:transparent!important;margin:0!important;padding:0!important}',
    'body#handycode-merged > section:nth-of-type(2){display:none!important}',
    'body#handycode-merged > section:first-of-type{border:0!important;border-radius:0!important;background:transparent!important;margin:0!important;box-shadow:none!important}',
    'body#handycode-merged > section:first-of-type > .hc-heading{display:none!important}',
    'body#handycode-merged > section:first-of-type > .hc-content{padding:4px 0 0!important;overflow:visible!important}',
    '#identity-content{width:100%!important;margin:0 auto!important;text-align:center!important}'
  ].join('\n');

  // 拼多多投影壳：壳自带外边距、卡片边框和 310px 最小高度，会让码区上下错位；
  // 官方页面里的固定宽度也会跟着克隆节点带进来，导致整列向右偏。
  // 这里把投影链路上每一层的宽度、内边距、定位都锁成视口宽度，并在卡片内垂直居中。
  var PDD_CSS = [
    'html,body{width:100%!important;min-width:0!important;max-width:100%!important;overflow-x:hidden!important;background:transparent!important;margin:0!important;padding:0!important;box-sizing:border-box!important;transform:none!important}',
    'html:not(.hc-pdd-source-mode),html:not(.hc-pdd-source-mode) body{height:100%!important}',
    // 官方页面会给 body 加内边距或最小宽度，把投影整体推偏；钉住视口避开这些影响。
    '#hc-pdd-shell{display:block!important;position:fixed!important;top:0!important;right:0!important;bottom:0!important;left:0!important;width:auto!important;min-width:0!important;max-width:none!important;height:auto!important;margin:0!important;padding:0!important;transform:none!important;filter:none!important;contain:none!important}',
    '#hc-pdd-shell > section{display:none!important}',
    '#hc-pdd-shell > section:first-of-type{display:block!important;width:100%!important;min-width:0!important;max-width:100%!important;height:100%!important;border:0!important;border-radius:0!important;background:transparent!important;margin:0!important;padding:0!important;box-shadow:none!important;overflow:visible!important}',
    '#hc-pdd-shell .hc-pdd-heading{display:none!important}',
    '#hc-pdd-shell .hc-pdd-content{display:flex!important;align-items:center!important;justify-content:center!important;width:100%!important;min-width:0!important;max-width:100%!important;height:100%!important;min-height:0!important;margin:0!important;padding:0!important;box-sizing:border-box!important;overflow:visible!important}',
    '#hc-pdd-identity{width:100%!important;min-width:0!important;max-width:100%!important;min-height:0!important;margin:0!important;padding:0!important;box-sizing:border-box!important}',
    '#hc-pdd-shell .hc-pdd-code{display:flex!important;flex-direction:column!important;align-items:stretch!important;justify-content:center!important;width:100%!important;min-width:0!important;max-width:100%!important;margin:0!important;padding:0!important;gap:8px!important;box-sizing:border-box!important;position:static!important;transform:none!important}',
    '#hc-pdd-shell .hc-pdd-code > *{width:100%!important;min-width:0!important;max-width:100%!important;margin:0!important;padding-left:0!important;padding-right:0!important;box-sizing:border-box!important;text-align:center!important;position:static!important;left:auto!important;right:auto!important;transform:none!important}',
    // 克隆出来的官方节点即使清掉内联尺寸，也可能带着类名样式里的定位和最小宽度；
    // 这里把投影链路上每一层的可继承布局都压平，只保留颜色和字号。
    '#hc-pdd-shell .hc-pdd-phone,#hc-pdd-shell .hc-pdd-phone *,#hc-pdd-shell .hc-pdd-tips,#hc-pdd-shell .hc-pdd-tips *{max-width:100%!important;min-width:0!important;margin-left:0!important;margin-right:0!important;padding-left:0!important;padding-right:0!important;left:auto!important;right:auto!important;float:none!important;position:static!important;transform:none!important;text-align:center!important;box-sizing:border-box!important}',
    '#hc-pdd-shell .hc-pdd-barcode{display:block!important;width:100%!important;height:auto!important;max-height:88px!important;object-fit:contain!important;margin:0!important}',
    '#hc-pdd-shell .hc-pdd-value,#hc-pdd-shell .hc-pdd-warning,#hc-pdd-shell .hc-pdd-tips{width:100%!important;text-align:center!important}'
  ].join('\n');

  // 全屏子页：WebView 被摆成横屏尺寸并整体旋转，这里只负责去掉页面留白、
  // 让条形码吃满可用高度。
  var TAOBAO_FULLSCREEN = [
    'html.dexp-fullscreen,html.dexp-fullscreen body{height:100%!important;overflow:hidden!important}',
    'html.dexp-fullscreen body#handycode-merged{padding:0!important}',
    'html.dexp-fullscreen body#handycode-merged > section:first-of-type{display:flex!important;flex-direction:column!important;height:100%!important}',
    'html.dexp-fullscreen body#handycode-merged > section:first-of-type > .hc-content{flex:1 1 auto!important;padding:0 8px!important}',
    'html.dexp-fullscreen #identity-content{display:flex!important;align-items:center!important;justify-content:center!important;width:100%!important}'
  ].join('\n');

  var PDD_FULLSCREEN = [
    'html.dexp-fullscreen #hc-pdd-shell .hc-pdd-barcode{width:92%!important;height:auto!important;max-height:60vh!important;margin:0 auto!important;object-fit:contain!important}',
    'html.dexp-fullscreen #hc-pdd-shell .hc-pdd-code{gap:14px!important}',
    'html.dexp-fullscreen #hc-pdd-shell .hc-pdd-phone{font-size:16px!important}',
    'html.dexp-fullscreen #hc-pdd-shell .hc-pdd-value{font-size:15px!important}'
  ].join('\n');

  function injectStyle() {
    if (!document.head || document.getElementById(STYLE_ID)) return;
    var style = document.createElement('style');
    style.id = STYLE_ID;
    style.textContent = window.DEXP_PLATFORM === 'pdd'
      ? PDD_CSS + '\n' + PDD_FULLSCREEN
      : TAOBAO_CSS + '\n' + TAOBAO_FULLSCREEN;
    document.head.appendChild(style);
  }

  function taobaoState() {
    var merged = !!document.getElementById('handycode-merged');
    var identity = document.getElementById('identity-content');
    var identityReady = !!identity
      && !identity.querySelector('.hc-loading,.hc-section-placeholder')
      && !!identity.querySelector('img,svg,canvas');
    var login = !merged && !!document.querySelector('input,textarea,[contenteditable="true"],iframe');
    return {
      platform: 'taobao',
      bound: merged,
      identity: identityReady,
      phase: merged ? 'ready' : (login ? 'login' : 'loading')
    };
  }

  function pddState() {
    var shell = document.getElementById('hc-pdd-shell');
    var identity = document.getElementById('hc-pdd-identity');
    var identityReady = !!identity
      && !identity.querySelector('.hc-pdd-placeholder,.hc-pdd-wait')
      && !!identity.querySelector('img,svg');
    var login = /login|passport/i.test(location.pathname || '');
    return {
      platform: 'pdd',
      bound: !!shell,
      identity: identityReady,
      phase: shell ? 'ready' : (login ? 'login' : 'loading')
    };
  }

  function report() {
    var bridge = window.DexpBridge;
    if (!bridge || typeof bridge.report !== 'function') return;
    var state;
    try {
      state = window.DEXP_PLATFORM === 'pdd' ? pddState() : taobaoState();
    } catch (error) {
      return;
    }
    var payload = JSON.stringify(state);
    if (payload === last) return;
    last = payload;
    try { bridge.report(payload); } catch (error) { /* 页面切换瞬间忽略 */ }
  }

  window.DexpMode = {
    apply: injectStyle,
    // 全屏开关：只切换 CSS 类；淘宝的条形码尺寸由投影脚本生成，需要重投影一次。
    fullscreen: function (on) {
      var active = !!on;
      var root = document.documentElement;
      if (!root) return false;
      root.classList.toggle('dexp-fullscreen', active);
      try {
        // 淘宝条形码尺寸由投影脚本写入内联样式，进出全屏都要按当前视口重建。
        if (typeof window.HandyCodeProject === 'function') window.HandyCodeProject();
        window.scrollTo(0, 0);
      } catch (error) { /* 页面切换瞬间忽略 */ }
      return active;
    },
    // 返回 true 表示页面内的刷新控件已接管；false 交给原生层 reload。
    refresh: function () {
      var selector = window.DEXP_PLATFORM === 'pdd'
        ? '[data-hc-pdd-refresh="identity"]'
        : '[data-refresh="identity"]';
      var button = document.querySelector(selector);
      if (button && !button.disabled) {
        button.click();
        return true;
      }
      return false;
    },
    report: report
  };

  injectStyle();
  report();
  setInterval(report, 1200);
})();
