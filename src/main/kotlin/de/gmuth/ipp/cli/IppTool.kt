package de.gmuth.ipp.cli

import de.gmuth.ipp.client.IppClient
import de.gmuth.ipp.core.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.Reader
import java.net.URI
import java.nio.charset.Charset
import java.util.*

class IppTool {
    var verbose: Boolean = false
    var uri: URI? = null
    var filename: String? = null
    val request = IppRequest()
    lateinit var currentGroup: IppAttributesGroup

    fun interpretResource(resource: String) = interpret(javaClass.getResourceAsStream(resource))
    fun interpretFile(path: String) = interpretFile(File(path))
    fun interpretFile(file: File) = interpret(FileInputStream(file))
    fun interpret(inputStream: InputStream) = interpret(inputStream.reader())
    fun interpret(reader: Reader) = interpret(reader.readLines())
    fun interpret(vararg lines: String) = if (lines.size == 1) interpret(lines[0].reader()) else interpret(lines.toList())

    fun interpret(lines: List<String>) {
        for (line: String in lines) {
            if (line.startsWith("#")) continue
            if (verbose) println("| ${line.trim()}")
            val lineItems = line.trim().split("\\s+".toRegex())
            if (lineItems.size > 1) interpretLine(lineItems)
        }
        executeIppRequest()
    }

    private fun interpretLine(lineItems: List<String>) {
        val command = lineItems.first()
        val firstArgument = lineItems[1]
        when (command) {
            "OPERATION" -> {
                val operation = IppOperation.fromString(firstArgument)
                request.code = operation.code
            }
            "GROUP" -> {
                val groupTag = IppTag.fromString(firstArgument)
                currentGroup = request.ippAttributesGroup(groupTag)
            }
            "ATTR" -> {
                val attribute = interpretAttr(lineItems)
                currentGroup.put(attribute)
            }
            "FILE" -> {
                if (firstArgument == "\$filename") {
                    if (filename == null) throw IppException("$firstArgument undefined")
                } else {
                    filename = firstArgument
                }
                request.documentInputStream = FileInputStream(File(filename))
            }
            else -> println("ignore unknown command '$command'")
        }
    }

    private fun interpretAttr(lineItems: List<String>): IppAttribute<*> {
        val tagName = if (lineItems[1] == "language") "naturalLanguage" else lineItems[1]
        val tag = IppTag.fromString(tagName)
        val name = lineItems[2]
        val valueString = lineItems[3]
        val value: Any = when {
            valueString == "\$uri" -> uri ?: throw IppException("\$uri undefined")
            tag == IppTag.Uri -> URI.create(valueString)
            tag == IppTag.Charset -> Charset.forName(valueString)
            else -> valueString
        }
        return IppAttribute(name, tag, value)
    }

    private fun executeIppRequest() {
        with(IppClient()) {
            verbose = true
            if (uri == null) throw IppException("uri missing")
            exchangeSuccessful(request)
        }
    }

}