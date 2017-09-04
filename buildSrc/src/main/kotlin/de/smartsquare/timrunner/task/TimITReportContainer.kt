package de.smartsquare.timrunner.task

import org.gradle.api.reporting.Report
import org.gradle.api.reporting.ReportContainer

interface TimITReportContainer : ReportContainer<Report> {
    fun getHtml(): Report
    fun getXml(): Report
}
