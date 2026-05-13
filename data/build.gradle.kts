import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.team.yeogibeoryeo.data"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    val localProperties = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            load(file.inputStream())
        }
    }

    val publicDataServiceKey = localProperties.getProperty("PUBLIC_DATA_SERVICE_KEY")?.trim()
        ?: throw GradleException("local.properties에 PUBLIC_DATA_SERVICE_KEY를 추가해야 합니다.")

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField(
            "String",
            "PUBLIC_DATA_SERVICE_KEY",
            "\"$publicDataServiceKey\""
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)

    // Coroutine
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp.logging.interceptor)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}