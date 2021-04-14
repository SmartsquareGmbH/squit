package de.smartsquare.squit.mediatype.xml

import de.smartsquare.squit.mediatype.MediaTypeConfig
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class XmlCanonicalizerTest {
    private val canonicalizer = XmlCanonicalizer()

    @Test
    fun `canonicalizing an xml structure`() {
        // language=xml
        val structure = """
            <test>
                <hello b="b" a="a">Abc</hello>
                <!-- Test -->
            </test>
        """.trimIndent()

        val result = canonicalizer.canonicalize(
            structure,
            MediaTypeConfig(
                xmlStrict = false,
                xmlCanonicalize = true,
                jsonCanonicalize = false
            )
        )

        // language=xml
        val expected = """
            <?xml version="1.0" encoding="UTF-8"?>

            <test>
              <hello a="a" b="b">Abc</hello>
            </test>

        """.trimIndent()

        result shouldBeEqualTo expected
    }

    @Test
    fun `canonicalizing an xml structure when canonicalization is disabled`() {
        // language=xml
        val structure = """
            <test>
                <hello b="b" a="a">Abc</hello>
                <!-- Test -->
            </test>
        """.trimIndent()

        val result = canonicalizer.canonicalize(
            structure,
            MediaTypeConfig(
                xmlStrict = false,
                xmlCanonicalize = false,
                jsonCanonicalize = false
            )
        )

        result shouldBeEqualTo structure
    }

    @Test
    fun `canonicalize an xml structure with resolving invalid namespaces`() {
        // language=xml
        val structure = """
            <ns3:test xmlns:ns3='w3.org/2001/XMLSchema-instance'>
                <hello b="b" a="a">Abc</hello>
            </ns3:test>
        """.trimIndent()

        val result = canonicalizer.canonicalize(
            structure,
            MediaTypeConfig(
                xmlStrict = false,
                xmlCanonicalize = true,
                jsonCanonicalize = false,
                resolveInvalidNamespaces = true
            )
        )

        // language=xml
        val expected = """
            <?xml version="1.0" encoding="UTF-8"?>

            <ns3:test xmlns:ns3="http://w3.org/2001/XMLSchema-instance">
              <hello a="a" b="b">Abc</hello>
            </ns3:test>

        """.trimIndent()

        result.trim() shouldBeEqualTo expected.trim()
    }

    @Test
    fun `canonicalize an xml structure with resolving valid namespaces`() {
        // language=xml
        val structure = """
            <ns3:test xmlns:ns3='http://w3.org/2001/XMLSchema-instance'>
                <hello b="b" a="a">Abc</hello>
            </ns3:test>
        """.trimIndent()

        val result = canonicalizer.canonicalize(
            structure,
            MediaTypeConfig(
                xmlStrict = false,
                xmlCanonicalize = true,
                jsonCanonicalize = false,
                resolveInvalidNamespaces = true
            )
        )

        // language=xml
        val expected = """
            <?xml version="1.0" encoding="UTF-8"?>

            <ns3:test xmlns:ns3="http://w3.org/2001/XMLSchema-instance">
              <hello a="a" b="b">Abc</hello>
            </ns3:test>

        """.trimIndent()

        result.trim() shouldBeEqualTo expected.trim()
    }
}
