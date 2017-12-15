@file:Suppress("StringLiteralDuplication")

package de.smartsquare.squit.report

import de.smartsquare.squit.entity.SquitResult
import de.smartsquare.squit.entity.SquitResultTree
import kotlinx.html.DIV
import kotlinx.html.HTML
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.code
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h4
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.role
import kotlinx.html.script
import kotlinx.html.span
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.tr
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private object IdHolder {
    var currentId = 0L
}

/**
 * Extension function for generating the html body of the Squit report with the given [results].
 */
fun HTML.squitBody(results: List<SquitResult>) {
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    val totalFailedTests = results.count { !it.isSuccess }
    val firstTest = results.minBy { it.metaInfo.date }
    val duration = results.fold(0L, { acc, next -> acc + next.metaInfo.duration })
    val averageTime = results.map { it.metaInfo.duration }.average().let { if (it.isNaN()) 0L else it.toLong() }
    val slowestTest = results.maxBy { it.metaInfo.duration }

    body {
        div(classes = "container") {
            div(classes = "row") {
                div(classes = "col-xs-12") {
                    h1 { +"Squit Results" }
                    h4 {
                        +when {
                            results.isEmpty() -> "No tests run."
                            totalFailedTests <= 0 -> "${results.size} tests run. All passed!"
                            results.size == 1 -> "One test run. $totalFailedTests failed."
                            else -> "${results.size} tests run. $totalFailedTests failed."
                        }
                    }
                }
            }

            div(classes = "row") {
                div(classes = "col-xs-12") {
                    div(classes = "responsive-table") {
                        table(classes = "table table-striped table-bordered") {
                            tbody {
                                tr {
                                    td { +"Started at" }
                                    td { +dateTimeFormatter.format(firstTest?.metaInfo?.date ?: LocalDateTime.now()) }
                                }
                                tr {
                                    td { +"Total time" }
                                    td { +durationToString(duration) }
                                }
                                tr {
                                    td { +"Average time per test" }
                                    td { +durationToString(averageTime) }
                                }
                                tr {
                                    td { +"Slowest test" }
                                    td {
                                        code {
                                            +(slowestTest?.name ?: "None")
                                        }

                                        +" (${durationToString(slowestTest?.metaInfo?.duration ?: 0)})"
                                    }
                                }
                            }
                        }
                    }
                }
            }

            div(classes = "row") {
                div(classes = "col-xs-6") {
                    form {
                        role = "form"

                        div(classes = "checkbox checkbox-primary") {
                            input(type = InputType.checkBox, classes = "styled") {
                                id = "failed-only"
                            }

                            label {
                                attributes += "for" to "failed-only"

                                +"Show only failed tests"
                            }
                        }
                    }
                }

                div(classes = "col-xs-6") {
                    a(classes = "btn btn-primary pull-right") {
                        attributes += "role" to "button"
                        id = "collapse-all"

                        +"Collapse all"
                    }

                    a(classes = "btn btn-primary pull-right start-item") {
                        attributes += "role" to "button"
                        id = "expand-all"

                        +"Expand all"
                    }
                }
            }

            div(classes = "row") {
                id = "result-tree"

                div(classes = "col-xs-12") {
                    squitItemContainers(SquitResultTree.fromList(results), 1)
                }
            }
        }

        script(src = "js/jquery.js") {}
        script(src = "js/bootstrap.js") {}
        script(src = "js/squit.js") {}
    }
}

/**
 * Helper extension function for generating a list of item containers with the given [resultTrees].
 */
fun DIV.squitItemContainers(resultTrees: List<SquitResultTree>, level: Int) {
    resultTrees.forEach {
        if (it.children.isEmpty()) {
            squitLeafItem(it, level)
        } else {
            squitContainerItem(it, level)
        }
    }
}

/**
 * Helper function for generating a single leaf item with the given [resultTree] and [level] for indentation.
 */
fun DIV.squitLeafItem(resultTree: SquitResultTree, level: Int) {
    a(href = "detail/${resultTree.id}/detail.html", classes = "list-group-item") {
        attributes.put("data-success", if (resultTree.isSuccess) "true" else "false")
        attributes.put("style", "padding-left: ${12 * level + 8}px")

        +resultTree.name

        val badgeType = when {
            resultTree.isIgnored -> "badge-ignored"
            resultTree.isSuccess -> "badge-success"
            else -> "badge-failure"
        }

        val text = when {
            resultTree.isIgnored -> "Ignored"
            resultTree.isSuccess -> "Passed"
            else -> "Failed"
        }

        span(classes = "badge $badgeType") {
            +text
        }
    }
}

/**
 * Helper function for generating a single item with the given [resultTree] and [level] for indentation.
 */
fun DIV.squitContainerItem(resultTree: SquitResultTree, level: Int) {
    val currentId = IdHolder.currentId++

    a(href = "#$currentId", classes = "list-group-item") {
        attributes.put("data-success", if (resultTree.isSuccess) "true" else "false")
        attributes.put("style", "padding-left: ${12 * level}px")
        attributes.put("data-toggle", "collapse")

        span(classes = "fa fa-chevron-right fa-fw start-item") {}

        +resultTree.name

        val badgeType = when {
            resultTree.isIgnored -> "badge-ignored"
            resultTree.isSuccess -> "badge-success"
            else -> "badge-failure"
        }

        span(classes = "badge $badgeType") {
            +"${resultTree.successfulTests}/${resultTree.totalTests - resultTree.ignoredTests} passed"
        }
    }

    div(classes = "list-group collapse") {
        id = currentId.toString()

        squitItemContainers(resultTree.children, level + 1)
    }
}

@Suppress("ComplexMethod")
private fun durationToString(duration: Long): String {
    var timeDuration = Duration.of(duration, ChronoUnit.MILLIS)
    var result = ""

    if (timeDuration.toHours() >= 1) {
        result += "${if (result.isNotEmpty()) " " else ""}${timeDuration.toHours()}h"

        timeDuration = timeDuration.minusHours(timeDuration.toHours())
    }

    if (timeDuration.toMinutes() >= 1) {
        result += "${if (result.isNotEmpty()) " " else ""}${timeDuration.toMinutes()}m"

        timeDuration = timeDuration.minusMinutes(timeDuration.toMinutes())
    }

    if (timeDuration.seconds >= 1) {
        result += "${if (result.isNotEmpty()) " " else ""}${timeDuration.seconds}s"

        timeDuration = timeDuration.minusSeconds(timeDuration.seconds)
    }

    if (timeDuration.toMillis() >= 1 || result.isEmpty()) {
        result += "${if (result.isNotEmpty()) " " else ""}${timeDuration.toMillis()}ms"
    }

    return result
}
