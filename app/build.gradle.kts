import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.util.Properties

val releaseArtifactTaskNames = setOf(
    "assembleRelease",
    "bundleRelease",
    "packageRelease",
    "packageReleaseBundle",
    "packageReleaseUniversalApk",
)
val keystorePropertiesFile = rootProject.file("keystore.properties")
val isCiReleaseCheck =
    providers.gradleProperty("ciReleaseCheck")
        .map(String::toBoolean)
        .orElse(false)
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.isFile) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}

fun requiredSigningProperty(name: String): String =
    keystoreProperties.getProperty(name)?.trim()?.takeIf(String::isNotEmpty)
        ?: throw GradleException("keystore.properties에 $name 값을 추가해야 합니다.")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.team.yeogibeoryeo"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    val localProperties = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            file.inputStream().use(::load)
        }
    }

    val naverClientId = localProperties.getProperty("NAVER_CLIENT_ID")?.trim()
        ?: throw GradleException("local.properties에 NAVER_CLIENT_ID를 추가해야 합니다.")

    val publicDataServiceKey = localProperties.getProperty("PUBLIC_DATA_SERVICE_KEY")?.trim()
        ?: throw GradleException("local.properties에 PUBLIC_DATA_SERVICE_KEY를 추가해야 합니다.")

    defaultConfig {
        applicationId = "com.team.yeogibeoryeo"
        minSdk = 28
        targetSdk = 36
        versionCode = 4
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["NAVER_CLIENT_ID"] = naverClientId

        buildConfigField(
            "String",
            "NAVER_CLIENT_ID",
            "\"$naverClientId\"",
        )

        buildConfigField(
            "String",
            "PUBLIC_DATA_SERVICE_KEY",
            "\"$publicDataServiceKey\"",
        )
    }

    val releaseSigningConfig = if (keystorePropertiesFile.isFile) {
        signingConfigs.create("release") {
            val configuredStoreFile = rootProject.file(requiredSigningProperty("storeFile"))
            if (!configuredStoreFile.isFile) {
                throw GradleException("출시용 키 저장소 파일을 찾을 수 없습니다: $configuredStoreFile")
            }

            storeFile = configuredStoreFile
            storePassword = requiredSigningProperty("storePassword")
            keyAlias = requiredSigningProperty("keyAlias")
            keyPassword = requiredSigningProperty("keyPassword")
        }
    } else {
        null
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }

        release {
            signingConfig = releaseSigningConfig
            isMinifyEnabled = true
            isShrinkResources = true
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = !isCiReleaseCheck.get()
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        managedDevices {
            localDevices {
                create("pixel9ProApi36") {
                    device = "Pixel 9 Pro"
                    apiLevel = 36
                    systemImageSource = "aosp"
                    testedAbi = "x86_64"
                }
                create("pixel2Api28") {
                    device = "Pixel 2"
                    apiLevel = 28
                    systemImageSource = "aosp"
                    testedAbi = "x86_64"
                }
            }
        }
    }
}

if (!keystorePropertiesFile.isFile) {
    val verifyReleaseSigning = tasks.register("verifyReleaseSigning") {
        doLast {
            throw GradleException("release 빌드에는 프로젝트 루트의 keystore.properties가 필요합니다.")
        }
    }

    tasks.matching { it.name in releaseArtifactTaskNames }.configureEach {
        dependsOn(verifyReleaseSigning)
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":presentation"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.bundles.coroutines)
    implementation(libs.bundles.naver.map)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.android.test)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
