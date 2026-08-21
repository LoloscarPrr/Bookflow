package app.bookflow.reader.core.data

import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Node

/** Extracts readable paragraph text from the main Word document XML.
 * DOCX is an OPC/ZIP package, so this stays dependency-free and Android-friendly.
 */
object DocxTextExtractor {
    fun extract(input: InputStream): String {
        val documentXml = ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") return@use zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
            null
        } ?: error("El DOCX no contiene word/document.xml")

        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(documentXml.inputStream())
        val paragraphs = document.getElementsByTagNameNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "p")
        return buildString {
            for (i in 0 until paragraphs.length) {
                val paragraph = paragraphs.item(i)
                val text = buildString { appendTextNodes(paragraph, this) }.trim()
                if (text.isNotEmpty()) append(text).append("\n\n")
            }
        }.trim()
    }

    private fun appendTextNodes(node: Node, output: StringBuilder) {
        if (node.localName == "t") output.append(node.textContent)
        if (node.localName == "tab") output.append('\t')
        if (node.localName == "br") output.append('\n')
        val children = node.childNodes
        for (i in 0 until children.length) appendTextNodes(children.item(i), output)
    }
}
