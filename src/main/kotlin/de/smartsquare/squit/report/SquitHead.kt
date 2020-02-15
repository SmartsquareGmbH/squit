@file:Suppress("StringLiteralDuplication")

package de.smartsquare.squit.report

import kotlinx.html.HTML
import kotlinx.html.head
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.title

/**
 * Extension function for generating the head of the Squit report.
 */
fun HTML.squitHead() {
    head {
        meta(charset = "utf-8")
        meta(content = "ie=edge") { attributes["http-equiv"] = "x-ua-compatible" }
        meta(name = "viewport", content = "width=device-width, initial-scale=1.0, shrink-to-fit=no")

        link(rel = "stylesheet", href = "css/bootstrap.css")
        link(rel = "stylesheet", href = "css/squit.css")

        title("Squit Results")
    }
}
