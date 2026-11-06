// 根构建脚本
//
// AGP 9 自带内置 Kotlin 支持，默认打包的是 KGP 2.2.10。Miuix 0.9.4 是用 Kotlin 2.4.20 编译的，
// 其 class 文件元数据版本高于 2.2.10，内置编译器会读不了（"compiled by a newer version of Kotlin"）。
// 因此这里通过 buildscript classpath 把 KGP 顶到 2.4.20。
//
// 说明：版本目录（libs.versions.toml）在 buildscript 块里不可用，只能写字面量，
//      升级 Kotlin 时需与 libs.versions.toml 的 kotlin 保持同步。
//      这个覆盖只能「向上」，要降级必须先 android.builtInKotlin=false。
buildscript {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
