package de.smartsquare.timrunner.entity

import java.nio.file.Path

data class TimITResult(val context: Path, val suite: Path, val test: Path, val result: String = "") {
    val isSuccess = result.isBlank()
    val path: Path get() = context.resolve(suite)
    val fullPath: Path get() = path.resolve(test)
}
