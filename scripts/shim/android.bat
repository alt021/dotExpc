@echo off
rem ============================================================================
rem `android` CLI 包装脚本   (cmd.exe / PowerShell 版)
rem
rem 作用与同一目录下的 POSIX 版本完全一致，原因见那个文件的头部注释：
rem 只在调用这一个进程时补上 ANDROID_USER_HOME，避免 Android CLI 退回
rem 用户主目录、在 ~/.android 里重新自举一份 89 MB 的副本。
rem ============================================================================
setlocal
for %%I in ("%~dp0..\..") do set "DOTEXPC_ROOT=%%~fI"
set "ANDROID_USER_HOME=%DOTEXPC_ROOT%\toolchain\android-user"
"%DOTEXPC_ROOT%\toolchain\android-sdk\cmdline-tools\latest\bin\android.exe" %*
exit /b %ERRORLEVEL%
