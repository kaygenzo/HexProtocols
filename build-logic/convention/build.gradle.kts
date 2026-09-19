plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.kotlin.serialization.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibraryConvention") {
            id = "hexprotocols.android.library"
            implementationClass = "hexprotocols.AndroidLibraryConventionPlugin"
        }
        register("commentLengthConvention") {
            id = "hexprotocols.comment-length"
            implementationClass = "hexprotocols.CommentLengthConventionPlugin"
        }
    }
}
