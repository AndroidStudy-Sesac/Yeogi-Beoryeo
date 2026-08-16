plugins {
    id("java-library")
    id("com.android.lint")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kover)
}

kover {
    currentProject {
        createVariant("focused") {
            add("jvm")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Coroutine
    implementation(libs.kotlinx.coroutines.core)

    // Inject
    implementation(libs.javax.inject)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
