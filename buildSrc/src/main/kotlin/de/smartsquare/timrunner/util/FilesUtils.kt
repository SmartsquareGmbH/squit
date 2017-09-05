package de.smartsquare.timrunner.util

import org.gradle.api.GradleException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

object FilesUtils {

    fun getLeafDirectories(current: Path) = mutableListOf<Path>().also { result ->
        Files.walkFileTree(current, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(directory: Path, attributes: BasicFileAttributes): FileVisitResult {
                if (Files.list(directory).use { it.noneMatch { current -> Files.isDirectory(current) } }) {
                    result.add(directory)
                }

                return FileVisitResult.CONTINUE
            }
        })
    }

    fun createFileIfNotExists(path: Path): Path = when (Files.exists(path)) {
        true -> path
        false -> Files.createFile(path)
    }

    fun validateExistence(path: Path): Path = when (Files.exists(path)) {
        true -> path
        false -> throw GradleException("Missing expected file: $path")
    }
}
