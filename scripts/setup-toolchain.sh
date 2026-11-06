#!/usr/bin/env bash
# =============================================================================
# dotExpc · 便携式 Android 工具链安装脚本
#
#   用法：  bash scripts/setup-toolchain.sh
#
# 策略：
#   · JDK 复用全局已装的 JDK 21，不重复安装
#   · Android SDK / Gradle 安装到项目内 toolchain/
#   · 不产生任何全局写入
# 脚本幂等：已装好的步骤自动跳过，可重复执行。
#
# 版本基线（与 Project-argon 实测确认，配套 AGP 9.4.1）：
#   Gradle 9.6.0            ← AGP 9.4 的最低要求
#   build-tools 36.0.0      ← AGP 9.4 的最低/默认版本
#   platforms;android-37.0  ← Miuix 强制 minCompileSdk=37，无法下调
# =============================================================================
set -euo pipefail

# --- 版本配置 ---------------------------------------------------------------
CMDTOOLS_URL="https://dl.google.com/android/repository/commandlinetools-win-16111833_latest.zip"
CMDTOOLS_ZIP_NAME="cmdline-tools.zip"

BUILDTOOLS_ZIP="build-tools_r36_windows.zip"
BUILDTOOLS_URL="https://dl.google.com/android/repository/$BUILDTOOLS_ZIP"
BUILDTOOLS_INNER="android-16"          # 包内顶层目录名，解压后需重命名为 36.0.0
BUILDTOOLS_VER="36.0.0"

GRADLE_VER="9.6.0"
GRADLE_URL="https://mirrors.cloud.tencent.com/gradle/gradle-${GRADLE_VER}-bin.zip"

# platforms;android-37.0 不在本脚本里手工部署：
# 该包只带 source.properties、没有 package.xml，手工放置 AGP 不认，
# 会另行安装到 platforms/android-37.0-2 造成重复。交给 AGP 首次构建时自动拉取即可。

# --- 路径 -------------------------------------------------------------------
SELF="${BASH_SOURCE[0]}"
ROOT="$(cd "$(dirname "$SELF")/.." && pwd)"
TC="$ROOT/toolchain"
DL="$TC/downloads"
SDK="$TC/android-sdk"
CLI_BIN="$SDK/cmdline-tools/latest/bin"

step() { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }
ok()   { printf '    \033[1;32m[OK]\033[0m %s\n' "$*"; }
skip() { printf '    \033[1;33m[跳过]\033[0m %s\n' "$*"; }
warn() { printf '    \033[1;33m[警告]\033[0m %s\n' "$*"; }
die()  { printf '\n\033[1;31m[失败]\033[0m %s\n' "$*" >&2; exit 1; }

mkdir -p "$DL"

download() {  # download <url> <输出文件>
  local url="$1" out="$2"
  if [ -s "$out" ]; then
    skip "已存在 $(basename "$out")（$(du -h "$out" | cut -f1)）"
    return 0
  fi
  echo "    下载 $(basename "$out") ..."
  curl -fL --retry 3 --retry-delay 2 --connect-timeout 30 -o "$out.part" "$url" \
    || die "下载失败: $url"
  mv "$out.part" "$out"
  ok "$(basename "$out")  $(du -h "$out" | cut -f1)"
}

# =============================================================================
step "Step 1/5 · 检查 JDK 21（复用全局，不安装）"
# =============================================================================
JDK_FOUND=""
for d in "$TC/jdk-21" "/c/Program Files/Eclipse Adoptium/jdk-21"*; do
  [ -x "$d/bin/java.exe" ] && JDK_FOUND="$d" && break
done
[ -n "$JDK_FOUND" ] || die "未找到 JDK 21。请先安装 Temurin JDK 21，或把便携 JDK 解压到 toolchain/jdk-21"
ok "使用 JDK: $(cygpath -w "$JDK_FOUND" 2>/dev/null || echo "$JDK_FOUND")"
"$JDK_FOUND/bin/java.exe" -version 2>&1 | head -1 | sed 's/^/    /'

# =============================================================================
step "Step 2/5 · Android SDK 命令行工具"
# =============================================================================
if [ -x "$CLI_BIN/sdkmanager.bat" ]; then
  skip "cmdline-tools/latest 已存在"
else
  download "$CMDTOOLS_URL" "$DL/$CMDTOOLS_ZIP_NAME"
  mkdir -p "$SDK/cmdline-tools"
  rm -rf "$SDK/cmdline-tools/cmdline-tools"
  unzip -q "$DL/$CMDTOOLS_ZIP_NAME" -d "$SDK/cmdline-tools"
  # 关键：目录名必须是 latest，否则报 Could not determine SDK root
  mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
  ok "已解压并重命名为 cmdline-tools/latest"
fi

# =============================================================================
step "Step 3/5 · 加载隔离环境"
# =============================================================================
# 必须在此之后才调用 android CLI，否则会污染 ~/.android
# shellcheck source=./env.sh
source "$ROOT/scripts/env.sh" >/dev/null

# =============================================================================
step "Step 4/5 · 安装 SDK 组件"
# =============================================================================
# 走 shim（scripts/shim/android）：它只在这一次调用里补 ANDROID_USER_HOME。
# 直接调 android.exe 会让 CLI 退回用户主目录自举 89 MB 的 android-cli。
command -v android >/dev/null 2>&1 || die "PATH 中未找到 android shim"

# --- platform-tools ---
if [ -x "$SDK/platform-tools/adb.exe" ]; then
  skip "platform-tools"
else
  echo "    安装 platform-tools ..."
  # 首次运行会自举下载 android-cli.exe，此处串行执行，不可并行
  android --no-metrics --sdk="$ANDROID_SDK_ROOT" sdk install platform-tools >/dev/null 2>&1 || true
  [ -x "$SDK/platform-tools/adb.exe" ] || die "platform-tools 安装失败"
  ok "platform-tools"
fi

# --- build-tools;36.0.0（CLI 在 Windows 上会崩溃，必须用 zip 手动部署） ---
if [ -f "$SDK/build-tools/$BUILDTOOLS_VER/source.properties" ]; then
  skip "build-tools;$BUILDTOOLS_VER"
else
  download "$BUILDTOOLS_URL" "$DL/$BUILDTOOLS_ZIP"
  mkdir -p "$SDK/build-tools"
  unzip -q "$DL/$BUILDTOOLS_ZIP" -d "$SDK/build-tools"
  [ -d "$SDK/build-tools/$BUILDTOOLS_INNER" ] || die "build-tools 包内目录名不是 $BUILDTOOLS_INNER"
  rm -rf "$SDK/build-tools/$BUILDTOOLS_VER"
  mv "$SDK/build-tools/$BUILDTOOLS_INNER" "$SDK/build-tools/$BUILDTOOLS_VER"
  ok "build-tools;$BUILDTOOLS_VER"
fi

# --- build-tools 的 package.xml（官方 zip 只带 source.properties） ---
# 注意：不要写 <uses-license ref="android-sdk-license" />，
# 该 ID 需要在文档内定义，单独引用会让 SDK 解析器报「未定义的 ID」。
PKGXML="$SDK/build-tools/$BUILDTOOLS_VER/package.xml"
if [ ! -f "$PKGXML" ]; then
  cat > "$PKGXML" <<XMLEOF
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns2:repository xmlns:ns2="http://schemas.android.com/repository/android/common/02" xmlns:ns3="http://schemas.android.com/generic/01">
  <localPackage path="build-tools;$BUILDTOOLS_VER" obsolete="false">
    <type-details xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns3:genericDetailsType" />
    <revision><major>36</major><minor>0</minor><micro>0</micro></revision>
    <display-name>Android SDK Build-Tools 36</display-name>
  </localPackage>
</ns2:repository>
XMLEOF
  ok "已补写 build-tools/$BUILDTOOLS_VER/package.xml"
fi

# =============================================================================
step "Step 5/5 · 便携 Gradle $GRADLE_VER"
# =============================================================================
if [ -x "$TC/gradle-$GRADLE_VER/bin/gradle" ]; then
  skip "Gradle $GRADLE_VER"
else
  download "$GRADLE_URL" "$DL/gradle-${GRADLE_VER}-bin.zip"
  # -o 覆盖：重复解压时避免交互式覆盖提示卡住脚本
  unzip -oq "$DL/gradle-${GRADLE_VER}-bin.zip" -d "$TC"
  [ -x "$TC/gradle-$GRADLE_VER/bin/gradle" ] || die "Gradle 解压异常"
  ok "Gradle $GRADLE_VER"
fi

# =============================================================================
step "验证"
# =============================================================================
source "$ROOT/scripts/env.sh"
printf '    %-22s %s\n' "java"    "$(java -version 2>&1 | head -1)"
printf '    %-22s %s\n' "adb"     "$(adb version 2>&1 | head -1)"
printf '    %-22s %s\n' "gradle"  "$(gradle --version 2>&1 | grep -m1 '^Gradle')"
printf '    %-22s %s\n' "aapt2"   "$("$SDK/build-tools/$BUILDTOOLS_VER/aapt2.exe" version 2>&1 | head -1)"

echo
echo "    platforms;android-37.0 由 AGP 首次构建时自动安装（需联网），首次运行会比较慢。"

echo
echo "    全局残留反向检查（以下应全部 OK）："
for d in "$HOME/.gradle" "$LOCALAPPDATA/Android"; do
  if [ -e "$d" ]; then warn "存在 $d"; else printf '    \033[1;32mOK\033[0m 不存在 %s\n' "$d"; fi
done
if [ -e "$HOME/.android/bin" ]; then
  warn "~/.android 含 bin/ —— 说明有 android 命令未加载隔离环境就执行过"
else
  printf '    \033[1;32mOK\033[0m ~/.android 无自举残留\n'
fi

cat <<'EOF'

────────────────────────────────────────────────────────
 安装完成。每次开工前先加载环境：

     source scripts/env.sh

 然后即可构建：

     ./gradlew :app:assembleDebug

 完全卸载：rm -rf toolchain/
────────────────────────────────────────────────────────
EOF
