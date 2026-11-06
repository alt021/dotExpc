#!/usr/bin/env bash
# =============================================================================
# dotExpc 隔离式 Android 工具链环境  (Git Bash / MSYS2)
#
#   用法：  source scripts/env.sh
#
# 策略：
#   · JDK 复用全局已装的 JDK 21 —— 只引用其路径，不复制、不修改，故无残留
#   · 其余组件（Android SDK、Gradle）全部落在项目内 toolchain/
#   · 不碰 ~/.gradle、~/.android、%LOCALAPPDATA%\Android、系统 PATH、注册表
# 关闭终端即失效。删除 toolchain/ 即卸载本项目引入的组件。
# =============================================================================

# --- 定位项目根目录（兼容 source 与直接执行） -------------------------------
if [ -n "${BASH_SOURCE[0]:-}" ]; then
  _dotexpc_self="${BASH_SOURCE[0]}"
else
  _dotexpc_self="$0"
fi
DOTEXPC_ROOT="$(cd "$(dirname "$_dotexpc_self")/.." && pwd)"
export DOTEXPC_ROOT

DOTEXPC_TC="$DOTEXPC_ROOT/toolchain"

# --- 路径风格转换 -----------------------------------------------------------
# JAVA_HOME / ANDROID_HOME 等会被 cmd.exe、.bat、java.exe 读取，
# 必须使用 Windows 风格路径；PATH 则保持 POSIX 风格供 shell 使用。
if command -v cygpath >/dev/null 2>&1; then
  _dotexpc_win() { cygpath -w "$1" 2>/dev/null || printf '%s' "$1"; }
else
  _dotexpc_win() { printf '%s' "$1"; }
fi

# --- 目录预建（全部在项目内） -----------------------------------------------
mkdir -p "$DOTEXPC_TC/android-user" "$DOTEXPC_TC/android-user/.android" \
         "$DOTEXPC_TC/android-user/avd" "$DOTEXPC_TC/gradle-home"

# --- JDK：复用全局已装的 JDK 21，不重复安装 ---------------------------------
# 优先级：项目内便携版 toolchain/jdk-21  >  全局 Adoptium jdk-21*
DOTEXPC_JDK_POSIX=""
if [ -x "$DOTEXPC_TC/jdk-21/bin/java.exe" ]; then
  DOTEXPC_JDK_POSIX="$DOTEXPC_TC/jdk-21"
else
  for _d in "/c/Program Files/Eclipse Adoptium/jdk-21"*; do
    if [ -x "$_d/bin/java.exe" ]; then DOTEXPC_JDK_POSIX="$_d"; break; fi
  done
fi
export DOTEXPC_JDK_POSIX

if [ -n "$DOTEXPC_JDK_POSIX" ]; then
  export JAVA_HOME="$(_dotexpc_win "$DOTEXPC_JDK_POSIX")"
else
  printf '\033[1;31m[dotExpc][错误]\033[0m 未找到 JDK 21（项目内 toolchain/jdk-21 或全局 Adoptium jdk-21）\n'
fi

# --- 核心隔离变量 -----------------------------------------------------------
# 幂等清理：同一个终端里重复 source 时，上一轮可能残留过这两个变量（或者用户
# 自己设过）。它们中的任何一个只要和 ANDROID_PREFS_ROOT 同时存在，AGP 就会拒启，
# 而报错信息完全看不出「是残留」——所以这里主动抹掉，保证只有一个来源。
unset ANDROID_USER_HOME ANDROID_SDK_HOME 2>/dev/null || true

export ANDROID_HOME="$(_dotexpc_win "$DOTEXPC_TC/android-sdk")"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

# 顶替 ~/.android。
#
# ⚠️ 只能设 ANDROID_PREFS_ROOT，**绝不能同时设 ANDROID_USER_HOME**。
#
# 两个变量的语义并不一样（com.android.prefs.Global 的 androidLeaf 字段）：
#     ANDROID_USER_HOME   leaf = null        → 它自己就是 .android 目录
#     ANDROID_PREFS_ROOT  leaf = ".android"  → 它指向 .android 的父目录，工具会再拼一级
# AGP 的 AbstractAndroidLocations.computeAndroidFolder() 调用
#     singlePathOf(ANDROID_USER_HOME, ANDROID_PREFS_ROOT, ANDROID_SDK_HOME)
# 要求「只有一个来源」，且它比较的是**拼过 leaf 之后**的路径：两个变量即使填完全一样的值，
# 也会分别算成 X 与 X/.android 两个不同路径 → 抛 AndroidLocationsException，AGP 起不来。
#
# 为什么留 ANDROID_PREFS_ROOT 而不是那个「更正式」的 ANDROID_USER_HOME：
# com.android.tools.analytics.AnalyticsPaths 只认 ANDROID_PREFS_ROOT / ANDROID_SDK_HOME，
# 而且是**直接拿原始值、不拼 .android**。只设 ANDROID_USER_HOME 时它会退回 user.home，
# 在 ~/.android 下写出 analytics.settings —— 隔离就漏了。
#
# 于是 $DOTEXPC_TC/android-user 一个目录同时承担两种用法：
#     android-user/analytics.settings   ← AnalyticsSettings（原始值，不拼）
#     android-user/.android/            ← AGP 与命令行工具的 prefs（debug.keystore 等）
export ANDROID_PREFS_ROOT="$(_dotexpc_win "$DOTEXPC_TC/android-user")"

# AVD 与模拟器走的是独立变量，不参与上面那个「只能有一个」的判定
export ANDROID_AVD_HOME="$(_dotexpc_win "$DOTEXPC_TC/android-user/avd")"
export ANDROID_EMULATOR_HOME="$(_dotexpc_win "$DOTEXPC_TC/android-user")"

# 顶替 ~/.gradle：依赖缓存、wrapper 发行版、daemon 日志全部落在项目内
export GRADLE_USER_HOME="$(_dotexpc_win "$DOTEXPC_TC/gradle-home")"

# 顶替 ~/.konan（纯 Android 工程用不到，预置以防将来引入 KMP）
export KONAN_DATA_DIR="$(_dotexpc_win "$DOTEXPC_TC/konan")"

# --- PATH 前置：项目内工具优先于全局 adb / 其他 JDK --------------------------
# 便携 Gradle：toolchain 下可能并存多个版本，glob 结果按版本号升序展开，
# 逐个「前插」后，PATH 中最靠前的即为版本号最高的那个。
_dotexpc_gradle_bin=""
for _g in "$DOTEXPC_TC"/gradle-*/bin; do
  [ -d "$_g" ] && _dotexpc_gradle_bin="$_g:$_dotexpc_gradle_bin"
done

# scripts/shim 排在最前：它里面只有一个 `android` 包装脚本，
# 作用见该文件头部注释（把 ANDROID_USER_HOME 只喂给这一个进程）。

export PATH="$DOTEXPC_ROOT/scripts/shim:\
${DOTEXPC_JDK_POSIX:+$DOTEXPC_JDK_POSIX/bin:}\
$DOTEXPC_TC/android-sdk/platform-tools:\
$DOTEXPC_TC/android-sdk/cmdline-tools/latest/bin:\
${_dotexpc_gradle_bin}$PATH"

# --- 构建参数（可选） -------------------------------------------------------
export GRADLE_OPTS="${GRADLE_OPTS:--Dfile.encoding=UTF-8}"

# --- 状态汇报 ---------------------------------------------------------------
echo "[dotExpc] 隔离环境已加载"
printf '  %-18s %s\n' \
  "JAVA_HOME"          "$JAVA_HOME" \
  "ANDROID_HOME"       "$ANDROID_HOME" \
  "ANDROID_PREFS_ROOT" "$ANDROID_PREFS_ROOT" \
  "GRADLE_USER_HOME"   "$GRADLE_USER_HOME"

if [ -n "$DOTEXPC_JDK_POSIX" ] && [ -x "$DOTEXPC_JDK_POSIX/bin/java.exe" ]; then
  printf '  %-18s %s\n' "java" "$("$DOTEXPC_JDK_POSIX/bin/java.exe" -version 2>&1 | head -1)"
  case "$DOTEXPC_JDK_POSIX" in
    "$DOTEXPC_TC"/*) printf '  %-18s %s\n' "JDK 来源" "项目内便携版" ;;
    *)               printf '  %-18s %s\n' "JDK 来源" "全局已装 JDK 21（复用，未改动）" ;;
  esac
fi

if [ -x "$DOTEXPC_TC/android-sdk/platform-tools/adb.exe" ]; then
  printf '  %-18s %s\n' "adb" "项目内 platform-tools"
else
  echo "  [警告] 未找到 platform-tools，请先运行: bash scripts/setup-toolchain.sh"
fi

if [ -n "$_dotexpc_gradle_bin" ]; then
  printf '  %-18s %s\n' "gradle" "${_dotexpc_gradle_bin%%:*}"
else
  printf '  %-18s %s\n' "gradle" "无便携版，用 ./gradlew（wrapper 自动取用）"
fi

unset _dotexpc_self _dotexpc_win _dotexpc_gradle_bin 2>/dev/null
