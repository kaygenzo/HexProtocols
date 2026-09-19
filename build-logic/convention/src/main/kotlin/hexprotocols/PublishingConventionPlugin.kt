package hexprotocols

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get

/**
 * `maven-publish` wired to each Android library's `release` variant, replacing the old repo's dead
 * Bintray/JCenter scripts. Publishes to whatever repository the consumer configures (defaults to
 * none beyond `mavenLocal()`, which every module gets via `./gradlew publishToMavenLocal` with no
 * extra setup) — picking a remote host (GitHub Packages, JitPack, Maven Central, ...) is a
 * deployment decision for whoever owns the release process, not something to hardcode here.
 */
class PublishingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("maven-publish")

        extensions.configure<LibraryExtension> {
            publishing {
                singleVariant("release") {
                    withSourcesJar()
                }
            }
        }

        afterEvaluate {
            extensions.configure<PublishingExtension> {
                publications {
                    create<MavenPublication>("release") {
                        from(components["release"])
                        groupId = project.group.toString()
                        artifactId = project.name
                        version = project.version.toString()

                        pom {
                            name.set(project.name)
                            description.set(
                                "Part of HexProtocols, a transport-agnostic protocol engine for " +
                                    "describing hardware device commands in JSON."
                            )
                            url.set("https://github.com/kaygenzo/HexProtocols")
                            licenses {
                                license {
                                    name.set("The Apache License, Version 2.0")
                                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                }
                            }
                            developers {
                                developer {
                                    id.set("kaygenzo")
                                    name.set("Karim Yarboua")
                                }
                            }
                            scm {
                                url.set("https://github.com/kaygenzo/HexProtocols")
                                connection.set(
                                    "scm:git:https://github.com/kaygenzo/HexProtocols.git"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
