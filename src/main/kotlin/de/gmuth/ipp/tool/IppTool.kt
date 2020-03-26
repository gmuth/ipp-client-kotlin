package de.gmuth.ipp.tool

import de.gmuth.ipp.client.IppClient
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppTag
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.Reader
import java.net.URI
import java.nio.charset.Charset

class IppTool() {
    var uri: URI? = null
    var file: File? = null
    var verbose: Boolean = false

    fun runResource(resource: String) = run(javaClass.getResourceAsStream(resource))
    fun runFile(path: String) = runFile(File(path))
    fun runFile(file: File) = run(FileInputStream(file))
    fun run(inputStream: InputStream) = run(inputStream.reader())
    fun run(reader: Reader) = run(reader.readLines())
    fun run(vararg lines: String) = if (lines.size == 1) run(lines[0].reader()) else run(lines.toList())

    fun run(lines: List<String>) {
        val ippClient = IppClient(uri ?: throw IllegalArgumentException("uri must not be null"))
        val ippRequest = buildIppRequest(lines)
        val fileInputStream = if (file == null) null else FileInputStream(file)
        ippClient.exchangeIpp(ippRequest, fileInputStream)
    }

    fun buildIppRequest(lines: List<String>): IppRequest {

        val ippRequest = IppRequest()
        var currentGroup: IppAttributesGroup? = null

        for (line in lines) {
            val lineItems = line.trim().split("\\s+".toRegex())
            val command = lineItems.first()
            if (lineItems.size < 2 || command.startsWith("#")) continue
            if (verbose) println("| ${line.trim()}")

            val firstArgument = lineItems[1]
            when (command) {
                "OPERATION" -> ippRequest.operation = lookupIppOperation(firstArgument)
                "GROUP" -> when (firstArgument) {
                    "operation-attributes-tag" -> currentGroup = ippRequest.operationGroup
                    else -> throw IllegalArgumentException("unsupported group '$firstArgument'")
                }
                "ATTR" -> {
                    val tag = lookupIppTag(firstArgument)
                    val name = lineItems[2]
                    var value = lineItems[3]
                    if (value == "\$uri") value = uri.toString()
                    currentGroup?.put(name, tag, value)
                    if (name == "attributes-charset") ippRequest.attributesCharset = Charset.forName(value)
                }
                "FILE" -> {
                    if (firstArgument == "\$file" || firstArgument == "\$filename") {
                        if (file == null) throw IllegalArgumentException("$firstArgument undefined")
                    } else {
                        file = File(firstArgument)
                    }
                }
                else -> println("ignore unknown command '$command'")
            }
        }
        return ippRequest
    }

    fun lookupIppOperation(operationName: String): IppOperation =
            when (operationName) {
                "Print-Job" -> IppOperation.PrintJob
                else -> throw IllegalArgumentException("unknown operation '$operationName'")
            }


    fun lookupIppTag(tagName: String): IppTag =
            when (tagName) {
                "operation-attributes-tag" -> IppTag.Operation
                "charset" -> IppTag.Charset
                "language" -> IppTag.NaturalLanguage
                "uri" -> IppTag.Uri
                else -> throw IllegalArgumentException("unknown tag '$tagName'")
            }

}