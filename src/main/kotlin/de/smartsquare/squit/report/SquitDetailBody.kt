package de.smartsquare.squit.report

import kotlinx.html.ButtonType
import kotlinx.html.HTML
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.i
import kotlinx.html.id
import kotlinx.html.script

/**
 * Extension function for generating the html body of a Squit detail page.
 */
fun HTML.squitDetailBody() {
    body {
        div(classes = "container") {
            div(classes = "row mt-2") {
                div(classes = "col-12") {
                    a(href = "../../main.html", classes = "btn btn-primary") {
                        attributes += "role" to "button"

                        i(classes = "fas fa-fw fa-arrow-left fa-fw") {}

                        +"Back"
                    }

                    button(classes = "btn btn-primary float-right") {
                        type = ButtonType.button
                        id = "output-toggle"

                        +"Show side by side"
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
        script(src = "../../js/diff2html.js") {}
        script(src = "../../js/diff2html-ui.js") {}
        script(src = "detail.js") {}
    }
}
