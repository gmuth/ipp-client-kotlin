package de.gmuth.ipp.tool

import de.gmuth.ipp.client.IppClient
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppTag
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset

class IppTool(val uri: URI) {

    private val ippClient = IppClient(uri)
    var file: File? = null

    fun run(vararg lines: String) = run(lines.toList())

    fun run(inputStream: InputStream) = run(inputStream.reader().readLines())

    fun run(lines: List<String>) {

        // parse request command
        val ippRequest: IppRequest = IppRequest()
        var currentGroup: IppAttributesGroup? = null
        for (line in lines) {
            println("| $line")
            val lineItems = line.split("\\s+".toRegex())
            val command = lineItems.first()
            val firstArgument = lineItems[1]
            when (command) {
                "OPERATION" -> ippRequest.operation = lookupIppOperation(firstArgument)
                "GROUP" -> when (lookupIppTag(firstArgument)) {
                    IppTag.Operation -> currentGroup = ippRequest?.operationGroup
                }
                "ATTR" -> {
                    val name = lineItems[2]
                    val tag = lookupIppTag(firstArgument)
                    val value = lineItems[3]
                    currentGroup?.put(name, tag, value)
                    if (name == "attributes-charset") ippRequest.attributesCharset = Charset.forName(value)
                }
                else -> println("ignore unsuppoted command '$command'")
            }
        }

        // execute request
        val documentInputStream = if (file == null) null else FileInputStream(file)
        val ippResponse = ippClient.exchangeIpp(ippRequest, documentInputStream)
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