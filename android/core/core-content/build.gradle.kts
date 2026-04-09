plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.fortunepocket.core.content"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    sourceSets {
        getByName("test") {
            resources.srcDir("../../../shared-content/data/astrology")
            resources.srcDir("../../../shared-content/data/bazi")
            resources.srcDir("../../../shared-content/data/tarot")
            resources.srcDir("../../../shared-content/data/lucky")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)

    testImplementation(libs.junit)
}
