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
        meta(content = "ie=edge") { attributes.put("http-equiv", "x-ua-compatible") }
        meta(name = "viewport", content = "width=device-width, initial-scale=1.0")

        link(rel = "stylesheet", href = "css/bootstrap.css")
        link(rel = "stylesheet", href = "css/font-awesome.css")
        link(rel = "stylesheet", href = "css/awesome-bootstrap-checkbox.css")
        link(rel = "stylesheet", href = "css/squit.css")

        title("Squit Results")
    }
}
