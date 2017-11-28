package de.smartsquare.squit.report

import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.script

/**
 * Extension function for generating the html body of a Squit detail page.
 */
fun HTML.squitDetailBody() {
    body {
        div(classes = "container") {
            div(classes = "row") {
                div(classes = "col-lg-12") {
                    id = "diffview"
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
