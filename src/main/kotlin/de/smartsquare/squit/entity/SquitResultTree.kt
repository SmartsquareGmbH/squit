package de.smartsquare.squit.entity

import de.smartsquare.squit.util.countTestResults

/**
 * Data structure for representing the results in a tree.
 *
 * @property id A unique id to use for further processing.
 * @property children List of child trees.
 * @property name The name of the test(-group).
 * @property successfulTests The amount of successful tests. If this is a leaf, this may be at most one.
 * @property failedTests The amount of failed tests. If this is a leaf, this may be at most one.
 * @property totalTests The total amount of tests. If this is a leaf, this is one.
 * @property ignoredTests The amount of ignored tests. If this is a leaf, this may be at most one.
 */
data class SquitResultTree(
    val id: Long,
    val children: List<SquitResultTree>,
    val name: String,
    val successfulTests: Int,
    val failedTests: Int,
    val ignoredTests: Int,
) {

    companion object {

        /**
         * Constructs a list of [SquitResultTree]s from the given [resultList].
         */
        fun fromList(resultList: List<SquitResult>): List<SquitResultTree> {
            val groupedResultList = resultList.groupBy { it.fullPath.first() }

            return groupedResultList.map { (_, group) ->
                val path = group.first().fullPath
                val (successfulTests, failedTests, ignoredTests) = group.countTestResults()

                if (group.size == 1 && path.toList().size == 1) {
                    SquitResultTree(
                        group.first().id,
                        emptyList(),
                        group.first().combinedName,
                        successfulTests,
                        failedTests,
                        ignoredTests,
                    )
                } else {
                    SquitResultTree(
                        -1,
                        fromList(group.map { it.cutFirstPathElement() }),
                        path.first().fileName.toString(),
                        successfulTests,
                        failedTests,
                        ignoredTests,
                    )
                }
            }
        }
    }

    /**
     * Convenience property for the total amount of tests.
     */
    val totalTests = successfulTests + failedTests + ignoredTests

    /**
     * Convenience property indicating if this group is a success.
     */
    val isSuccess = failedTests == 0

    /**
     * Convenience property indicating if this group is ignored.
     */
    val isIgnored = successfulTests == 0 && failedTests == 0
}
