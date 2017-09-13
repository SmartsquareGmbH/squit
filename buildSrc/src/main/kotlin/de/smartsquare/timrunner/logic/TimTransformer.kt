package de.smartsquare.timrunner.logic

import org.dom4j.Document
import org.dom4j.Element
import org.dom4j.Node
import java.time.LocalDate
import java.util.regex.Pattern

/**
 * Object with utility methods for transforming the requests and responses.
 *
 * @author Ruben Gees
 */
object TimTransformer {

    private val dateRegex = Regex("${Pattern.quote("$")}${Pattern.quote("{")}" +
            "#TestSuite#TaxAlgoDate(.*?)${Pattern.quote("}")}")

    private val errorTransactionIdRegex = Regex("transaction ID " +
            "${Pattern.quote("[")}(.+)${Pattern.quote("]")}")

    private val errorTransactionIdReplacementRegex = Regex("transaction ID " +
            "${Pattern.quote("[")}.*${Pattern.quote("]")}")

    /**
     * Replaces the date of the [request] and the [expectedResponse] to be the same for the given [elementName] (for
     * example TaxAlgorithmDate), while also interpreting the SOAP UI templates for dates.
     */
    fun replaceDateFromExpectedResponse(request: Document, expectedResponse: Document, elementName: String) {
        request.selectSingleNode("//$elementName")?.let { currentDateNode ->
            expectedResponse.selectSingleNode("//$elementName")?.let { expectedDateNode ->
                transformDate(currentDateNode, expectedDateNode)
            }
        }
    }

    /**
     * Replaces the TransactionId of the [actualResponse] to be the same as that of the [expectedResponse].
     */
    fun replaceTransactionIdFromExpectedResponse(actualResponse: Document, expectedResponse: Document) {
        expectedResponse.selectSingleNode("//TransactionId")?.let {
            actualResponse.selectSingleNode("//TransactionId")?.text = it.text
        }
    }

    /**
     * Replaces the transaction ID in an error message of the [actualResponse] to be the same as that in the
     * [expectedResponse].
     */
    fun replaceErrorTransactionIdFromExpectedResponse(actualResponse: Document, expectedResponse: Document) {
        actualResponse.selectNodes("//ErrorText").forEachIndexed { index, responseNode ->
            expectedResponse.selectNodes("//ErrorText").getOrNull(index)?.let { expectedResponseNode ->
                val newId = errorTransactionIdRegex.find(expectedResponseNode.text)?.groupValues?.let {
                    if (it.size == 2) it[1] else null
                }

                if (newId != null) {
                    responseNode.text = responseNode.text.replace(errorTransactionIdReplacementRegex,
                            "transaction ID [$newId]")
                }
            }
        }
    }

    /**
     * Sorts the TaxInvoiceSubTotals children of the given [elementName] (For example SellerTaxTotal)
     * of the [document] based on the TaxAmount, GrossAmount and TaxCode in that order.
     */
    fun sortTaxInvoiceSubTotals(document: Document, elementName: String) {
        sortElements(document, elementName, "TaxInvoiceSubTotal", compareBy({
            it.second.selectSingleNode("TaxAmount")?.text?.toFloatOrNull() ?: -1f
        }, {
            it.second.selectSingleNode("GrossAmount")?.text?.toFloatOrNull() ?: -1f
        }, {
            it.second.selectSingleNode("TaxCode")?.text?.toFloatOrNull() ?: -1f
        }))
    }

    /**
     * Sorts the Error children of the given [document], based on the ErrorCode and the ErrorText in that order.
     */
    fun sortErrors(document: Document) {
        sortElements(document, "return", "Error", compareBy({
            it.second.selectSingleNode("ErrorCode")?.text?.toIntOrNull() ?: -1
        }, {
            it.second.selectSingleNode("ErrorText")?.text ?: ""
        }))
    }

    /**
     * Strips all stack traces from the [document], in texts of ErrorTest nodes.
     */
    fun stripStackTraces(document: Document) {
        document.selectNodes("//ErrorText").forEach {
            if (it.text.startsWith("Technical error")) {
                val stacktraceIndex = it.text.indexOf("at com.")

                if (stacktraceIndex >= 0) {
                    it.text = it.text.substring(0, stacktraceIndex).trim()
                }
            }
        }
    }

    /**
     * Helper function for sorting elements (named [subElementName]) of a given [elementName], based on the given
     * [comparator] in the given [document].
     */
    private fun sortElements(document: Document, elementName: String, subElementName: String,
                             comparator: Comparator<Pair<Int, Element>>) {
        document.selectNodes("//$elementName").forEach { elementNode ->
            if (elementNode is Element) {
                val elements = elementNode.elements()
                val itemsToRemove = elements
                        .mapIndexed { index, it -> index to it }
                        .filter { it.second.name == subElementName }

                itemsToRemove.asReversed().forEach {
                    elements.removeAt(it.first)
                }

                itemsToRemove
                        .sortedWith(comparator)
                        .forEach { elements.add(it.second) }
            }
        }
    }

    /**
     * Transform the date of the [currentNode] and [expectedNode] to be the same, while also interpreting the
     * SOAP UI templates for dates if present.
     */
    private fun transformDate(currentNode: Node, expectedNode: Node) {
        dateRegex.find(currentNode.text)?.let { regexResult ->
            val dateToSet = if (regexResult.groupValues.size == 2) {
                LocalDate.now().plusDays(regexResult.groupValues[1].toLongOrNull() ?: 0).toString()
            } else {
                expectedNode.text
            }

            currentNode.text = dateToSet
            expectedNode.text = dateToSet
        }
    }
}
