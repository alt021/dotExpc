import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    // AGP 9 起不再声明 kotlin-android：Kotlin 编译已内置，只保留 Compose 编译器插件
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dotexpc.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    // 显式锁定 build-tools：避免 AGP 按自己的默认值去下载我们没装的版本
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        applicationId = "com.dotexpc.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.9.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // 侧载用：沿用项目内的 debug keystore，签名与调试版一致，可直接覆盖安装。
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // miuix-nav 0.9.4 以 JVM 21 编译，内联它的函数要求本模块同样产出 21 字节码
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.ui.tooling.preview)

    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.nav)
    implementation(libs.miuix.shader)

    implementation(libs.backdrop)

    debugImplementation(libs.compose.ui.tooling)
}
