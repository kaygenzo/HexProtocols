plugins {
    id("hexprotocols.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.telen.protocols.core"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
