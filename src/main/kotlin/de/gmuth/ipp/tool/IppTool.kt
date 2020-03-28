package de.gmuth.ipp.tool

import de.gmuth.ipp.client.IppClient
import de.gmuth.ipp.core.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.Reader
import java.net.URI
import java.nio.charset.Charset

class IppTool() {
    var uri: URI? = null
    var filename: String? = null
    var verbose: Boolean = false

    fun runResource(resource: String) = run(javaClass.getResourceAsStream(resource))
    fun runFile(path: String) = runFile(File(path))
    fun runFile(file: File) = run(FileInputStream(file))
    fun run(inputStream: InputStream) = run(inputStream.reader())
    fun run(reader: Reader) = run(reader.readLines())
    fun run(vararg lines: String) = if (lines.size == 1) run(lines[0].reader()) else run(lines.toList())

    fun run(lines: List<String>) {
        var fileInputStream: FileInputStream? = null
        var currentGroup: IppAttributesGroup? = null
        val ippRequest = IppRequest()

        // parse commands and build ipp request
        for (line in lines) {
            val lineItems = line.trim().split("\\s+".toRegex())
            val command = lineItems.first()
            if (lineItems.size < 2 || command.startsWith("#")) continue
            if (verbose) println("| ${line.trim()}")
            val firstArgument = lineItems[1]
            when (command) {
                "OPERATION" -> ippRequest.operation = IppOperation.fromRegisteredName(firstArgument)
                "GROUP" -> when (firstArgument) {
                    "operation-attributes-tag" -> currentGroup = ippRequest.operationGroup
                    else -> throw IllegalArgumentException("unsupported group '$firstArgument'")
                }
                "ATTR" -> {
                    //val tag = IppRegistrations.ippTag(firstArgument)
                    val tag = IppTag.fromRegisteredName(firstArgument)
                    val name = lineItems[2]
                    var value = lineItems[3]
                    if (value == "\$uri") value = uri.toString()
                    currentGroup?.put(name, tag, value)
                    if (name == "attributes-charset") ippRequest.attributesCharset = Charset.forName(value)
                }
                "FILE" -> {
                    if (firstArgument == "\$file" || firstArgument == "\$filename") {
                        if (filename == null) throw IllegalArgumentException("$firstArgument undefined")
                    } else {
                        filename = firstArgument
                    }
                    fileInputStream = FileInputStream(File(filename))
                }
                else -> println("ignore unknown command '$command'")
            }
        }

        // exchange ipp request with ipp client to
        val ippClient = IppClient(uri ?: throw IllegalArgumentException("uri must not be null"))
        ippClient.exchangeIpp(ippRequest, fileInputStream)
    }
}