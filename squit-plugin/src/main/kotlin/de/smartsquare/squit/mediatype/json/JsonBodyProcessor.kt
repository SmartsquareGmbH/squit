package de.smartsquare.squit.mediatype.json

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.typesafe.config.Config
import de.smartsquare.squit.SquitJsonPostProcessor
import de.smartsquare.squit.SquitJsonPreProcessor
import de.smartsquare.squit.mediatype.BodyProcessor
import de.smartsquare.squit.util.postProcessorScripts
import de.smartsquare.squit.util.postProcessors
import de.smartsquare.squit.util.preProcessorScripts
import de.smartsquare.squit.util.preProcessors
import de.smartsquare.squit.util.read
import de.smartsquare.squit.util.write
import groovy.lang.Binding
import groovy.lang.GroovyShell
import java.nio.file.Files
import java.nio.file.Path

/**
 * @author Ruben Gees
 */
object JsonBodyProcessor : BodyProcessor {

    override fun preProcess(
        requestPath: Path?,
        responsePath: Path,
        resultRequestPath: Path,
        resultResponsePath: Path,
        config: Config
    ) {
        val request = requestPath?.let { JsonParser().read(it) }
        val response = JsonParser().read(responsePath)

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
        val actualResponse = JsonParser().read(actualResponsePath)
        val expectedResponse = JsonParser().read(expectedResponsePath)

        runPostProcessors(config, actualResponse, expectedResponse)

        actualResponse.write(resultActualResponseFilePath)
    }

    private fun runPreProcessors(config: Config, request: JsonElement?, response: JsonElement) {
        config.preProcessors.map { Class.forName(it).getConstructor().newInstance() }
            .filterIsInstance(SquitJsonPreProcessor::class.java)
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

    private fun runPostProcessors(config: Config, actualResponse: JsonElement, expectedResponse: JsonElement) {
        config.postProcessors.map { Class.forName(it).getConstructor().newInstance() }
            .filterIsInstance(SquitJsonPostProcessor::class.java)
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
