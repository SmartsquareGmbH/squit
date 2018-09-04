@file:Suppress("StringLiteralDuplication")

package de.smartsquare.squit.report

import kotlinx.html.ButtonType
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
fun HTML.squitDetailBody() {
    body {
        div(classes = "container") {
            div(classes = "row mt-2 mb-2") {
                div(classes = "col-12") {
                    h1 {
                        id = "title"
                    }

                    h4 {
                        id = "subtitle"
                    }
                }
            }

            div(classes = "row") {
                div(classes = "col-12") {
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

            div(classes = "row mt-2") {
                div(classes = "col-12") {
                    a(href = "../../main.html", classes = "btn btn-primary") {
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
                            +"Show side by side"
                        }
                    }
                }
            }

            div(classes = "row mt-2 mb-2") {
                div(classes = "col-12") {
                    id = "diff-view"
                }
            }
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
