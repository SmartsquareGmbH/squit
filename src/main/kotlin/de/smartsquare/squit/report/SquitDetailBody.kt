@file:Suppress("StringLiteralDuplication")

package de.smartsquare.squit.report

import de.smartsquare.squit.entity.SquitResult
import kotlinx.html.ButtonType
import kotlinx.html.DIV
import kotlinx.html.HTML
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h4
import kotlinx.html.i
import kotlinx.html.id
import kotlinx.html.script
import kotlinx.html.span

/**
 * Extension function for generating the html body of a Squit detail page.
 */
fun HTML.squitDetailBody(result: SquitResult) {
    body {
        div(classes = "container-fluid") {
            squitTitle(result)
            squitControls()

            if (result.description != null) squitDescription()
            if (!result.expectedResponseInfo.isDefault) squitInfoDiff()

            squitDiff()
        }

        script(src = "../../js/jquery.js") {}
        script(src = "../../js/popper.js") {}
        script(src = "../../js/bootstrap.js") {}
        script(src = "../../js/marked.js") {}
        script(src = "../../js/diff2html.js") {}
        script(src = "../../js/diff2html-ui.js") {}
        script(src = "../../js/fontawesome.js") {}
        script(src = "detail.js") {}
    }
}

private fun DIV.squitTitle(result: SquitResult) {
    div(classes = "row mt-4 mb-2") {
        div(classes = "row offset-lg-1 col-12 col-lg-10") {
            div(classes = "d-inline-flex") {
                h1(classes = "d-inline-flex") {
                    id = "title"
                }

                h4(classes = "d-inline-flex") {
                    id = "subtitle"
                }
            }

            div(classes = "ml-2 d-inline-flex align-self-center") {
                val badgeType = when {
                    result.isIgnored -> "badge-secondary"
                    result.isSuccess -> "badge-success"
                    else -> "badge-danger"
                }

                val text = when {
                    result.isIgnored -> "Ignored"
                    result.isSuccess -> "Passed"
                    else -> "Failed"
                }

                span(classes = "badge $badgeType float-right") {
                    +text
                }
            }
        }
    }
}

private fun DIV.squitControls() {
    div(classes = "row mt-2 mb-2") {
        div(classes = "offset-lg-1 col-12 col-lg-10") {
            a(href = "../../index.html", classes = "btn btn-primary") {
                attributes += "role" to "button"

                i(classes = "fas fa-fw fa-arrow-left align-middle") {}
                span(classes = "align-middle") {
                    +" Back"
                }
            }

            button(classes = "btn btn-primary float-right") {
                id = "output-toggle"
                type = ButtonType.button

                span(classes = "align-middle") {
                    +"Show inline"
                }
            }
        }
    }
}

private fun DIV.squitDescription() {
    div(classes = "row mt-4 mb-2") {
        div(classes = "offset-lg-1 col-12 col-lg-10") {
            div(classes = "card card-body") {
                a(classes = "link-unstyled", href = "#description-container") {
                    id = "description-toggle"

                    attributes["data-toggle"] = "collapse"

                    i(classes = "fas fa-fw fa-chevron-right mr-2") {}

                    +"Description"
                }

                div(classes = "collapse") {
                    id = "description-container"

                    div(classes = "mt-4") {
                        id = "description"
                    }
                }
            }
        }
    }
}

private fun DIV.squitDiff() {
    div(classes = "row mt-4 mb-2") {
        div(classes = "offset-lg-1 col-12 col-lg-10") {
            id = "diff-view"
        }
    }
}

private fun DIV.squitInfoDiff() {
    div(classes = "row mt-4 mb-2") {
        div(classes = "offset-lg-1 col-12 col-lg-10") {
            +"HTTP Status Code"
        }
    }
    div(classes = "row mt-2 mb-2") {
        div(classes = "offset-lg-1 col-12 col-lg-10") {
            id = "info-diff-view"
        }
    }
}
