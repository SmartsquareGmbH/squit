package de.smartsquare.squit.mediatype.json

import com.google.gson.JsonElement
import com.typesafe.config.Config
import de.smartsquare.squit.config.postProcessorScripts
import de.smartsquare.squit.config.postProcessors
import de.smartsquare.squit.config.preProcessorScripts
import de.smartsquare.squit.config.preProcessors
import de.smartsquare.squit.interfaces.SquitJsonPostProcessor
import de.smartsquare.squit.interfaces.SquitJsonPreProcessor
import de.smartsquare.squit.io.JsonParserSupport
import de.smartsquare.squit.mediatype.BodyProcessor
import de.smartsquare.squit.util.write
import groovy.lang.Binding
import groovy.lang.GroovyShell
import java.nio.file.Path

/**
 * Json-specific [BodyProcessor] implementation. It allows for user-supplied pre- and post-processor implementations
 * to run on the generated [JsonElement] instances and saves the results.
 */
class JsonBodyProcessor : BodyProcessor {

    override fun preProcess(
        requestPath: Path?,
        responsePath: Path,
        resultRequestPath: Path,
        resultResponsePath: Path,
        config: Config
    ) {
        val request = requestPath?.let { JsonParserSupport.read(it) }
        val response = JsonParserSupport.read(responsePath)

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
        val actualResponse = JsonParserSupport.read(actualResponsePath)
        val expectedResponse = JsonParserSupport.read(expectedResponsePath)

        runPostProcessors(config, actualResponse, expectedResponse)

        actualResponse.write(resultActualResponseFilePath)
    }

    private fun runPreProcessors(config: Config, request: JsonElement?, response: JsonElement) {
        config.preProcessors.map { Class.forName(it).getConstructor().newInstance() }
            .filterIsInstance(SquitJsonPreProcessor::class.java)
            .forEach { it.process(request, response) }

        config.preProcessorScripts.forEach {
            GroovyShell(javaClass.classLoader).parse(it.toFile()).apply {
                binding = Binding(
                    mapOf(
                        "request" to request,
                        "expectedResponse" to response
                    )
                )
            }.run()
        }
    }

    private fun runPostProcessors(config: Config, actualResponse: JsonElement, expectedResponse: JsonElement) {
        config.postProcessors.map { Class.forName(it).getConstructor().newInstance() }
            .filterIsInstance(SquitJsonPostProcessor::class.java)
            .forEach { it.process(actualResponse, expectedResponse) }

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
