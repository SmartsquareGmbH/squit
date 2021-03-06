@file:Suppress("StringLiteralDuplication")

package de.smartsquare.squit.report

import de.smartsquare.squit.entity.SquitResult
import de.smartsquare.squit.entity.SquitResultTree
import kotlinx.html.ButtonType
import kotlinx.html.DIV
import kotlinx.html.HTML
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.classes
import kotlinx.html.code
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h4
import kotlinx.html.i
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

private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/**
 * Extension function for generating the html body of the Squit report with the given [results].
 */
fun HTML.squitBody(results: List<SquitResult>) {
    body {
        div(classes = "container-fluid") {
            squitTitle(results)
            squitOverviewTable(results)
            squitControls()
            squitItems(results)
        }

        script(src = "js/jquery.js") {}
        script(src = "js/popper.js") {}
        script(src = "js/bootstrap.js") {}
        script(src = "js/fontawesome.js") {}
        script(src = "js/squit.js") {}
    }
}

private fun DIV.squitTitle(results: List<SquitResult>) {
    val totalFailedTests = results.count { !it.isSuccess }

    div(classes = "row mt-4") {
        div(classes = "offset-lg-1 col-12 col-lg-10") {
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
}

private fun DIV.squitOverviewTable(results: List<SquitResult>) {
    val firstTest = results.minByOrNull { it.metaInfo.date }
    val duration = results.fold(0L) { acc, next -> acc + next.metaInfo.duration }

    val averageTime = results.map { it.metaInfo.duration }.average().let { if (it.isNaN()) 0L else it.toLong() }
    val slowestTest = results.maxByOrNull { it.metaInfo.duration }

    div(classes = "row") {
        div(classes = "offset-lg-1 col-12 col-lg-10") {
            div(classes = "table-responsive") {
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
                                    +(slowestTest?.simpleName ?: "None")
                                }

                                +" (${durationToString(slowestTest?.metaInfo?.duration ?: 0)})"
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DIV.squitControls() {
    div(classes = "row") {
        div(classes = "offset-lg-1 col-6 col-lg-5 d-flex align-items-center") {
            form {
                role = "form"

                div(classes = "custom-control custom-checkbox") {
                    input(type = InputType.checkBox, classes = "custom-control-input") {
                        id = "failed-only"
                    }

                    label(classes = "custom-control-label") {
                        attributes += "for" to "failed-only"
                        classes = classes + "unselectable"

                        +"Show only failed tests"
                    }
                }
            }
        }

        div(classes = "col-6 col-lg-5") {
            button(classes = "btn btn-primary float-right") {
                type = ButtonType.button
                id = "collapse-all"

                +"Collapse all"
            }

            button(classes = "btn btn-primary float-right mr-2") {
                type = ButtonType.button
                id = "expand-all"

                +"Expand all"
            }
        }
    }
}

private fun DIV.squitItems(results: List<SquitResult>) {
    div(classes = "row mt-3 mb-2") {
        id = "result-tree"

        div(classes = "offset-lg-1 col-12 col-lg-10") {
            squitItemContainers(SquitResultTree.fromList(results), 1)
        }
    }
}

private fun DIV.squitItemContainers(resultTrees: List<SquitResultTree>, level: Int) {
    resultTrees.forEach {
        if (it.children.isEmpty()) {
            squitLeafItem(it, level)
        } else {
            squitContainerItem(it, level)
        }
    }
}

private fun DIV.squitContainerItem(resultTree: SquitResultTree, level: Int) {
    val currentId = IdHolder.currentId++

    a(href = "#item-$currentId", classes = "list-group-item list-group-item-action") {
        attributes["data-success"] = if (resultTree.isSuccess) "true" else "false"
        attributes["style"] = "padding-left: ${12 * level}px"
        attributes["data-toggle"] = "collapse"

        i(classes = "fas fa-fw fa-chevron-right mr-2") {}

        +resultTree.name

        val badgeType = when {
            resultTree.isIgnored -> "badge-secondary"
            resultTree.isSuccess -> "badge-success"
            else -> "badge-danger"
        }

        span(classes = "badge $badgeType float-right") {
            +"${resultTree.successfulTests}/${resultTree.totalTests - resultTree.ignoredTests} passed"
        }
    }

    div(classes = "list-group collapse") {
        id = "item-$currentId"

        squitItemContainers(resultTree.children, level + 1)
    }
}

private fun DIV.squitLeafItem(resultTree: SquitResultTree, level: Int) {
    a(href = "detail/${resultTree.id}/detail.html", classes = "list-group-item list-group-item-action") {
        attributes["data-success"] = if (resultTree.isSuccess) "true" else "false"
        attributes["style"] = "padding-left: ${12 * level}px"

        +resultTree.name

        val badgeType = when {
            resultTree.isIgnored -> "badge-secondary"
            resultTree.isSuccess -> "badge-success"
            else -> "badge-danger"
        }

        val text = when {
            resultTree.isIgnored -> "Ignored"
            resultTree.isSuccess -> "Passed"
            else -> "Failed"
        }

        span(classes = "badge $badgeType float-right") {
            +text
        }
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
