package de.smartsquare.timrunner.task

import org.gradle.api.Task
import org.gradle.api.reporting.ConfigurableReport
import org.gradle.api.reporting.DirectoryReport
import org.gradle.api.reporting.Report
import org.gradle.api.reporting.SingleFileReport
import org.gradle.api.reporting.internal.TaskGeneratedSingleDirectoryReport
import org.gradle.api.reporting.internal.TaskGeneratedSingleFileReport
import org.gradle.api.reporting.internal.TaskReportContainer

class TimITReportContainerImpl(task: Task)
    : TaskReportContainer<Report>(ConfigurableReport::class.java, task), TimITReportContainer {

    init {
        add(TaskGeneratedSingleDirectoryReport::class.java, "html", task, "index.html")
        add(TaskGeneratedSingleFileReport::class.java, "xml", task)
    }

    override fun getHtml(): DirectoryReport {
        return getByName("html") as DirectoryReport
    }

    override fun getXml(): SingleFileReport {
        return getByName("xml") as SingleFileReport
    }
}
