package de.smartsquare.squit.util

import org.gradle.api.logging.Logger
import org.gradle.api.logging.configuration.ConsoleOutput

private object LoggerState {
    var isNewLineNeeded = false
}

/**
 * Prints the given [message] on the same line and returns the carriage to the start. Useful for progress printing.
 */
fun Logger.lifecycleOnSameLine(message: String, consoleOutput: ConsoleOutput) {
    if (isLifecycleEnabled) {
        if (consoleOutput != ConsoleOutput.Plain) {
            printAndFlush("\r$message")

            LoggerState.isNewLineNeeded = true
        } else {
            lifecycle(message)
        }
    }
}

/**
 * Prints a new line if needed by previous calls to [lifecycleOnSameLine].
 */
fun Logger.newLineIfNeeded() {
    if (isLifecycleEnabled && LoggerState.isNewLineNeeded) {
        println()

        LoggerState.isNewLineNeeded = false
    }
}
