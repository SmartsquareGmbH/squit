package de.smartsquare.timrunner.io

import org.gradle.api.GradleException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

object FilesUtils {

    fun getChildDirectories(path: Path) = Files.newDirectoryStream(path, { Files.isDirectory(it) })
            .use { it.toList() }

    fun getLeafDirectories(path: Path): List<Path> = mutableListOf<Path>().also { result ->
        Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(directory: Path, attributes: BasicFileAttributes): FileVisitResult {
                if (directory != path && containsDirectories(directory)) {
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

    private fun containsDirectories(path: Path) = Files.list(path).use {
        it.noneMatch { current ->
            Files.isDirectory(current)
        }
    }
}
