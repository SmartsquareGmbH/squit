package de.smartsquare.squit.report

import de.smartsquare.squit.entity.SquitResult

data class SquitHtmlReportData(
    val version: String,
    val generatedAt: String?,
    val startedAt: String?,
    val totalDuration: Long,
    val averageDuration: Long,
    val slowestTest: SquitSlowestTest?,
    val results: SquitReportResultBranch,
)

data class SquitSlowestTest(val id: Long, val name: String, val duration: Long)

sealed interface SquitReportResultNode

data class SquitReportResultBranch(
    val children: MutableMap<String, SquitReportResultNode> = mutableMapOf<String, SquitReportResultNode>(),
) : SquitReportResultNode

class SquitReportResultLeaf(val result: SquitResult) : SquitReportResultNode

data class SquitReportResult(
    val id: Long,
    val alternativeName: String,
    val description: String?,
    val success: Boolean,
    val ignored: Boolean,
    val error: Boolean,
    val duration: Long,
    val expected: String,
    val actual: String,
    val infoExpected: String?,
    val infoActual: String?,
    val language: String?,
)
