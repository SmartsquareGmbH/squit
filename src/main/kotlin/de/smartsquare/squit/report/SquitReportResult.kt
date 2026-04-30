package de.smartsquare.squit.report

sealed interface SquitReportResultNode

data class SquitReportResultBranch(val children: MutableMap<String, SquitReportResultNode> = mutableMapOf()) :
    SquitReportResultNode {

    fun toMap(): Map<String, Any> = children.mapValues { (_, value) ->
        when (value) {
            is SquitReportResultBranch -> value.toMap()
            is SquitReportResultNode -> value
        }
    }
}

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
) : SquitReportResultNode

data class SquitSlowestTest(val id: Long, val name: String, val duration: Long)

data class SquitHtmlReportData(
    val startedAt: String?,
    val totalDuration: Long,
    val averageDuration: Long,
    val slowestTest: SquitSlowestTest?,
    val results: Map<String, Any>,
)
