package hexprotocols

import org.gradle.api.Plugin
import org.gradle.api.Project

/** Wires [CommentLineLengthCheck] into `check`, for any module — library or application. */
class CommentLengthConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val checkCommentLineLength =
            tasks.register("checkCommentLineLength", CommentLineLengthCheck::class.java)
        tasks.named("check").configure { dependsOn(checkCommentLineLength) }
    }
}
