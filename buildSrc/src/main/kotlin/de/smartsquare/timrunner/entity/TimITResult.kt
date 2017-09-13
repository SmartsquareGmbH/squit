package de.smartsquare.timrunner.entity

data class TimITResult(val context: String, val suite: String, val test: String, val result: String = "") {
    val path get() = "$context/$suite"
    val fullPath get() = "$path/$test"
}
