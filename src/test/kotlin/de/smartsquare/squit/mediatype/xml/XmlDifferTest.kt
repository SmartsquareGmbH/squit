package de.smartsquare.squit.mediatype.xml

import de.smartsquare.squit.mediatype.MediaTypeConfig
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class XmlDifferTest {

    private val differ =
        XmlDiffer(MediaTypeConfig(xmlStrict = false, xmlCanonicalize = false, jsonCanonicalize = false))

    private val strictDiffer =
        XmlDiffer(MediaTypeConfig(xmlStrict = true, xmlCanonicalize = false, jsonCanonicalize = false))

    @Test
    fun `diffing identic XMLs`() {
        differ.diff("<cool/>".toByteArray(), "<cool/>".toByteArray()).shouldBeEmpty()
    }

    @Test
    fun `diffing similar XMLs with only differences in namespace prefixes`() {
        // language=xml
        val expected = """
            <ns:root xmlns:ns="http://example.com">
              <cool/>
            </ns:root>
        """.trimIndent()

        // language=xml
        val actual = """
            <something:root xmlns:something="http://example.com">
              <cool/>
            </something:root>
        """.trimIndent()

        differ.diff(expected.toByteArray(), actual.toByteArray()).shouldBeEmpty()
    }

    @Test
    fun `diffing different XMLs`() {
        val expected = "<good/>"
        val actual = "<bad/>"

        differ.diff(expected.toByteArray(), actual.toByteArray()).shouldNotBeEmpty()
    }

    @Test
    fun `strictly diffing identic XMLs`() {
        strictDiffer.diff("<cool/>".toByteArray(), "<cool/>".toByteArray()).shouldBeEmpty()
    }

    @Test
    fun `strictly diffing similar XMLs with only differences in namespace prefixes`() {
        // language=xml
        val expected = """
            <ns:root xmlns:ns="http://example.com">
              <cool/>
            </ns:root>
        """.trimIndent()

        // language=xml
        val actual = """
            <something:root xmlns:something="http://example.com">
              <cool/>
            </something:root>
        """.trimIndent()

        strictDiffer.diff(expected.toByteArray(), actual.toByteArray()).shouldNotBeEmpty()
    }

    @Test
    fun `strictly diffing different XMLs`() {
        val expected = "<good/>"
        val actual = "<bad/>"

        strictDiffer.diff(expected.toByteArray(), actual.toByteArray()).shouldNotBeEmpty()
    }
}
