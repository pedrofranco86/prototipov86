plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.pelops.assistant"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.pelops.assistant"
        minSdk = 26
        targetSdk = 28
        versionCode = 1
        versionName = "0.1.0"
    }
}
