plugins {
    id("hexprotocols.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.telen.protocols.transport.ble"
}

dependencies {
    api(project(":protocol-core"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.mockk)
}
