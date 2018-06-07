package de.smartsquare.squit.mediatype.xml

import com.typesafe.config.Config
import de.smartsquare.squit.SquitXmlPostProcessor
import de.smartsquare.squit.SquitXmlPreProcessor
import de.smartsquare.squit.mediatype.BodyProcessor
import de.smartsquare.squit.util.postProcessorScripts
import de.smartsquare.squit.util.postProcessors
import de.smartsquare.squit.util.preProcessorScripts
import de.smartsquare.squit.util.preProcessors
import de.smartsquare.squit.util.read
import de.smartsquare.squit.util.write
import groovy.lang.Binding
import groovy.lang.GroovyShell
import org.dom4j.Document
import org.dom4j.io.SAXReader
import java.nio.file.Files
import java.nio.file.Path

/**
 * Xml-specific [BodyProcessor] implementation. It allows for user-supplied pre- and post-processor implementations
 * to run on the generated [Document] instances and saves the results.
 *
 * @author Ruben Gees
 */
object XmlBodyProcessor : BodyProcessor {

    override fun preProcess(
        requestPath: Path?,
        responsePath: Path,
        resultRequestPath: Path,
        resultResponsePath: Path,
        config: Config
    ) {
        val request = requestPath?.let { SAXReader().read(requestPath) }
        val response = SAXReader().read(responsePath)

        runPreProcessors(config, request, response)

        request?.write(resultRequestPath)
        response.write(resultResponsePath)
    }

    override fun postProcess(
        actualResponsePath: Path,
        expectedResponsePath: Path,
        resultActualResponseFilePath: Path,
        config: Config
    ) {
        val actualResponse = SAXReader().read(actualResponsePath)
        val expectedResponse = SAXReader().read(expectedResponsePath)

        runPostProcessors(config, actualResponse, expectedResponse)

        actualResponse.write(resultActualResponseFilePath)
    }

    private fun runPreProcessors(config: Config, request: Document?, response: Document) {
        config.preProcessors.map { Class.forName(it).getConstructor().newInstance() }
            .filterIsInstance(SquitXmlPreProcessor::class.java)
            .forEach { it.process(request, response) }

        config.preProcessorScripts.forEach {
            GroovyShell(javaClass.classLoader).parse(Files.newBufferedReader(it)).apply {
                binding = Binding(mapOf(
                    "request" to request,
                    "expectedResponse" to response
                ))
            }.run()
        }
    }

    private fun runPostProcessors(config: Config, actualResponse: Document, expectedResponse: Document) {
        config.postProcessors.map { Class.forName(it).getConstructor().newInstance() }
            .filterIsInstance(SquitXmlPostProcessor::class.java)
            .forEach { it.process(actualResponse, expectedResponse) }

        config.postProcessorScripts.forEach {
            GroovyShell(javaClass.classLoader).parse(Files.newBufferedReader(it)).apply {
                binding = Binding(mapOf(
                    "actualResponse" to actualResponse,
                    "expectedResponse" to expectedResponse
                ))
            }.run()
        }
    }
}
