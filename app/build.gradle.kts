// app/build.gradle.kts

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp) // <--- 修改这里，使用 libs.versions.toml 中定义的别名 ---
}

android {
    namespace = "com.example.aichatpet"
    compileSdk = 34

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.aichatpet"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists() && localPropertiesFile.isFile) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        } else {
            println("Warning: local.properties file not found. API keys might be missing or default to empty.")
        }


        buildConfigField("String", "MOONSHOT_API_KEY",    "\"${localProperties.getProperty("MOONSHOT_API_KEY") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false // 对于个人测试可以，发布时建议设为 true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {}
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Retrofit 及其 Gson 转换器
    implementation(libs.squareup.retrofit.core)
    implementation(libs.squareup.retrofit.converter.gson)
    implementation(libs.google.gson)
    implementation(libs.squareup.okhttp3.logging.interceptor)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.activity) // <--- 使用 libs 别名 ---
    ksp(libs.androidx.room.compiler)        // <--- 使用 libs 别名 ---
    implementation(libs.androidx.room.ktx)      // <--- 使用 libs 别名 ---

    // Coil 图片加载库
    implementation("io.coil-kt:coil:2.4.0") // 旧方式
     // <--- 假设你在 libs.versions.toml 中添加了 coil-kt 别名 ---

    // 测试依赖
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}