package de.smartsquare.squit.report

import kotlinx.html.HTML
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.script
import kotlinx.html.span

/**
 * Extension function for generating the html body of a Squit detail page.
 */
fun HTML.squitDetailBody() {
    body {
        div(classes = "container") {
            div(classes = "row top-row") {
                div(classes = "col-xs-12") {
                    a(href = "../../main.html", classes = "btn btn-primary") {
                        attributes += "role" to "button"

                        span(classes = "fa fa-arrow-left fa-fw start-item") {}

                        +"Back"
                    }

                    a(classes = "btn btn-primary pull-right") {
                        attributes += "role" to "button"
                        id = "output-toggle"

                        +"Show side by side"
                    }
                }
            }

            div(classes = "row") {
                div(classes = "col-xs-12") {
                    id = "diff-view"
                }
            }
        }

        script(src = "../../js/jquery.js") {}
        script(src = "../../js/bootstrap.js") {}
        script(src = "../../js/diff2html.js") {}
        script(src = "../../js/diff2html-ui.js") {}
        script(src = "detail.js") {}
    }
}
