# =============================================================================
# dotExpc 隔离式 Android 工具链环境  (PowerShell)
#
#   用法：  . .\scripts\env.ps1      ← 注意开头的点号，表示在当前会话生效
#
# 策略：
#   · JDK 复用全局已装的 JDK 21 —— 只引用其路径，不复制、不修改，故无残留
#   · 其余组件（Android SDK、Gradle）全部落在项目内 toolchain\
#   · 不碰 ~\.gradle、~\.android、%LOCALAPPDATA%\Android、系统 PATH、注册表
# 关闭终端即失效。删除 toolchain\ 即卸载本项目引入的组件。
# =============================================================================

$DotexpcRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$env:DOTEXPC_ROOT = $DotexpcRoot
$tc = Join-Path $DotexpcRoot 'toolchain'

foreach ($d in @('android-user', 'android-user\.android', 'android-user\avd', 'gradle-home')) {
    $p = Join-Path $tc $d
    if (-not (Test-Path $p)) { New-Item -ItemType Directory -Force -Path $p | Out-Null }
}

# --- JDK：复用全局已装的 JDK 21，不重复安装 ---------------------------------
$portableJdk = Join-Path $tc 'jdk-21'
if (Test-Path (Join-Path $portableJdk 'bin\java.exe')) {
    $dotexpcJdk = $portableJdk
    $jdkOrigin = '项目内便携版'
} else {
    $found = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue |
             Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } |
             Sort-Object Name -Descending | Select-Object -First 1
    if ($found) {
        $dotexpcJdk = $found.FullName
        $jdkOrigin = '全局已装 JDK 21（复用，未改动）'
    } else {
        $dotexpcJdk = $null
        $jdkOrigin = '未找到'
    }
}
$env:DOTEXPC_JDK = $dotexpcJdk

if ($dotexpcJdk) {
    $env:JAVA_HOME = $dotexpcJdk
} else {
    Write-Warning '未找到 JDK 21（项目内 toolchain\jdk-21 或全局 Adoptium jdk-21）'
}

# --- 核心隔离变量 -----------------------------------------------------------
# 幂等清理：重复点加载时可能残留。它们与 ANDROID_PREFS_ROOT 同时存在会让 AGP 拒启。
Remove-Item Env:ANDROID_USER_HOME -ErrorAction SilentlyContinue
Remove-Item Env:ANDROID_SDK_HOME  -ErrorAction SilentlyContinue

$env:ANDROID_HOME      = Join-Path $tc 'android-sdk'
$env:ANDROID_SDK_ROOT  = $env:ANDROID_HOME

# 顶替 ~/.android。⚠️ 只能设 ANDROID_PREFS_ROOT，**绝不能同时设 ANDROID_USER_HOME**。
# 两者的 androidLeaf 语义不同（前者会再拼一级 .android，后者不会），AGP 的
# computeAndroidFolder() 要求「只有一个来源」，同时设会直接抛异常起不来 ——
# 即使两个变量的值完全相同也一样。完整推导见 scripts\env.sh 里的注释。
# 留 ANDROID_PREFS_ROOT 是因为 com.android.tools.analytics.AnalyticsPaths 只认它。
$env:ANDROID_PREFS_ROOT = Join-Path $tc 'android-user'

# AVD 与模拟器用独立变量，不参与上面那个「只能有一个」的判定
$env:ANDROID_AVD_HOME  = Join-Path $tc 'android-user\avd'
$env:ANDROID_EMULATOR_HOME = Join-Path $tc 'android-user'

$env:GRADLE_USER_HOME  = Join-Path $tc 'gradle-home'
$env:KONAN_DATA_DIR    = Join-Path $tc 'konan'

# --- PATH 前置：项目内工具优先于全局 adb / 其他 JDK --------------------------
# scripts\shim 排最前：里面的 `android` 包装脚本只给那一个进程补 ANDROID_USER_HOME
$prepend = @((Join-Path $DotexpcRoot 'scripts\shim'))
if ($dotexpcJdk) { $prepend += (Join-Path $dotexpcJdk 'bin') }
$prepend += (Join-Path $tc 'android-sdk\platform-tools')
$prepend += (Join-Path $tc 'android-sdk\cmdline-tools\latest\bin')
# toolchain 下可能并存多个便携 Gradle，取版本号最高的那个
$gradleDirs = Get-ChildItem (Join-Path $tc 'gradle-*') -Directory -ErrorAction SilentlyContinue |
              Sort-Object Name -Descending
if ($gradleDirs) { $prepend += (Join-Path $gradleDirs[0].FullName 'bin') }
$env:PATH = ($prepend -join ';') + ';' + $env:PATH

# --- 状态汇报 ---------------------------------------------------------------
Write-Host '[dotExpc] 隔离环境已加载'
Write-Host "  JAVA_HOME          = $env:JAVA_HOME"
Write-Host "  JDK 来源           = $jdkOrigin"
Write-Host "  ANDROID_HOME       = $env:ANDROID_HOME"
Write-Host "  ANDROID_PREFS_ROOT = $env:ANDROID_PREFS_ROOT"
Write-Host "  GRADLE_USER_HOME   = $env:GRADLE_USER_HOME"

if ($dotexpcJdk) {
    Write-Host "  java               = $((& (Join-Path $dotexpcJdk 'bin\java.exe') -version 2>&1 | Select-Object -First 1))"
}
