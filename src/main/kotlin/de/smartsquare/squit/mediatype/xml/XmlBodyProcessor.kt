package de.smartsquare.squit.mediatype.xml

import com.typesafe.config.Config
import de.smartsquare.squit.config.postProcessorScripts
import de.smartsquare.squit.config.postProcessors
import de.smartsquare.squit.config.preProcessorScripts
import de.smartsquare.squit.config.preProcessors
import de.smartsquare.squit.interfaces.SquitXmlPostProcessor
import de.smartsquare.squit.interfaces.SquitXmlPreProcessor
import de.smartsquare.squit.io.SAXReaderSupport
import de.smartsquare.squit.mediatype.BodyProcessor
import de.smartsquare.squit.util.write
import groovy.lang.Binding
import groovy.lang.GroovyShell
import org.dom4j.Document
import java.nio.file.Path

/**
 * Xml-specific [BodyProcessor] implementation. It allows for user-supplied pre- and post-processor implementations
 * to run on the generated [Document] instances and saves the results.
 */
class XmlBodyProcessor : BodyProcessor {

    override fun preProcess(
        requestPath: Path?,
        responsePath: Path,
        resultRequestPath: Path,
        resultResponsePath: Path,
        config: Config
    ) {
        val request = requestPath?.let { SAXReaderSupport.read(requestPath) }
        val response = SAXReaderSupport.read(responsePath)

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
        val actualResponse = SAXReaderSupport.read(actualResponsePath)
        val expectedResponse = SAXReaderSupport.read(expectedResponsePath)

        runPostProcessors(config, actualResponse, expectedResponse)

        actualResponse.write(resultActualResponseFilePath)
    }

    private fun runPreProcessors(config: Config, request: Document?, response: Document) {
        config.preProcessors.map { Class.forName(it).getConstructor().newInstance() }
            .filterIsInstance(SquitXmlPreProcessor::class.java)
            .forEach { it.process(request, response, config) }

        config.preProcessorScripts.forEach {
            GroovyShell(javaClass.classLoader).parse(it.toFile()).apply {
                binding = Binding(
                    mapOf(
                        "request" to request,
                        "expectedResponse" to response,
                        "config" to config
                    )
                )
            }.run()
        }
    }

    private fun runPostProcessors(config: Config, actualResponse: Document, expectedResponse: Document) {
        config.postProcessors.map { Class.forName(it).getConstructor().newInstance() }
            .filterIsInstance(SquitXmlPostProcessor::class.java)
            .forEach { it.process(actualResponse, expectedResponse, config) }

        config.postProcessorScripts.forEach {
            GroovyShell(javaClass.classLoader).parse(it.toFile()).apply {
                binding = Binding(
                    mapOf(
                        "actualResponse" to actualResponse,
                        "expectedResponse" to expectedResponse
                    )
                )
            }.run()
        }
    }
}
