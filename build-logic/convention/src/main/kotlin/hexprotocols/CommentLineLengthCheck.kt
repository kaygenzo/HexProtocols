package hexprotocols

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * ktlint's max-line-length rule only checks code, not comments or KDoc (verified against ktlint
 * 1.3.1: a 200+ char `//`/`/** */` line produces zero violations). This task fills that gap so
 * comment/KDoc lines are still capped, since nothing else enforces it.
 */
open class CommentLineLengthCheck : DefaultTask() {
    @Input
    var maxLength: Int = 100

    @TaskAction
    fun check() {
        val root = project.projectDir.resolve("src")
        if (!root.exists()) return

        val violations = mutableListOf<String>()
        root
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                file.readLines().forEachIndexed { index, line ->
                    val trimmed = line.trimStart()
                    val isComment =
                        trimmed.startsWith("//") ||
                            trimmed.startsWith("/**") ||
                            trimmed.startsWith("/*") ||
                            trimmed.startsWith("*")
                    if (isComment && line.length > maxLength) {
                        violations +=
                            "${file.relativeTo(
                                project.projectDir
                            )}:${index + 1}: ${line.length} chars (max $maxLength)"
                    }
                }
            }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Comment/KDoc lines exceeding $maxLength characters:\n${violations.joinToString(
                    "\n"
                )}"
            )
        }
    }
}
