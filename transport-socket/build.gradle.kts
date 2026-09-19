plugins {
    id("hexprotocols.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.telen.protocols.transport.socket"
}

dependencies {
    api(project(":protocol-core"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
