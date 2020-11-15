package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.ipp.core.*
import de.gmuth.ipp.cups.CupsMarker
import de.gmuth.ipp.cups.CupsPrinterType
import de.gmuth.ipp.iana.IppRegistrationsSection2
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI

open class IppPrinter(
        val printerUri: URI,
        var attributes: IppAttributesGroup = IppAttributesGroup(IppTag.Printer),
        val ippClient: IppClient = IppClient()
) {

    var verbose: Boolean
        get() = ippClient.verbose
        set(value) {
            ippClient.verbose = value
        }

    var httpBasicAuth: Http.BasicAuth?
        get() = ippClient.httpBasicAuth
        set(value) {
            ippClient.httpBasicAuth = value
        }

    var checkValueSupported: Boolean = true

    init {
        if (attributes.size == 0) {
            attributes = getPrinterAttributes()
        }
    }

    constructor(printerAttributes: IppAttributesGroup, ippClient: IppClient = IppClient()) : this(
            (printerAttributes.getValues("printer-uri-supported") as List<URI>).first(),
            printerAttributes,
            ippClient
    )

    //--------------
    // IppAttributes
    //--------------

    val name: IppString
        get() = attributes.getValue("printer-name")

    val makeAndModel: IppString
        get() = attributes.getValue("printer-make-and-model")

    val isAcceptingJobs: Boolean
        get() = attributes.getValue("printer-is-accepting-jobs")

    val state: IppPrinterState
        get() = IppPrinterState.fromInt(attributes.getValue("printer-state") as Int)

    val stateReasons: List<String>
        get() = attributes.getValues("printer-state-reasons")

    val printerUriSupported: List<URI>
        get() = attributes.getValues("printer-uri-supported")

    //-----------------
    // Identify-Printer
    //-----------------

    fun identify(vararg actions: String) = identify(actions.toList())

    fun identify(actions: List<String>) {
        checkValueSupported("identify-actions-supported", actions)
        val request = ippRequest(IppOperation.IdentifyPrinter).apply {
            operationGroup.attribute("identify-actions", IppTag.Keyword, actions)
        }
        exchangeSuccessful(request)
    }

    //--------------
    // Pause-Printer
    //--------------

    fun pause() = exchangeSuccessfulIppRequest(IppOperation.PausePrinter)

    //---------------
    // Resume-Printer
    //---------------

    fun resume() = exchangeSuccessfulIppRequest(IppOperation.ResumePrinter)

    //-----------------------
    // Get-Printer-Attributes
    //-----------------------

    fun getPrinterAttributes(requestedAttributes: List<String> = listOf()): IppAttributesGroup {
        val request = ippRequest(IppOperation.GetPrinterAttributes).apply {
            if (requestedAttributes.isNotEmpty()) {
                operationGroup.attribute("requested-attributes", IppTag.Keyword, requestedAttributes)
            }
        }
        val response = exchangeSuccessful(request)
        return response.printerGroup
    }

    fun updateAttributes() {
        attributes = getPrinterAttributes()
    }

    //----------
    // Print-Job
    //----------

    fun printInputStream(
            inputStream: InputStream,
            attributeBuilders: Array<out IppAttributeBuilder>,
            waitForTermination: Boolean

    ): IppJob {
        val request = attributeBuildersRequest(IppOperation.PrintJob, attributeBuilders).apply {
            documentInputStream = inputStream
        }
        val response = exchangeSuccessful(request)
        return handlePrintResponse(response, waitForTermination)
    }

    fun printJob(
            inputStream: InputStream,
            vararg attributeBuilders: IppAttributeBuilder,
            waitForTermination: Boolean = false

    ) = printInputStream(inputStream, attributeBuilders, waitForTermination)

    fun printJob(
            file: File,
            vararg attributeBuilders: IppAttributeBuilder,
            waitForTermination: Boolean = false

    ) = printInputStream(FileInputStream(file), attributeBuilders, waitForTermination)

    //----------
    // Print-Uri
    //----------

    fun printUri(
            documentUri: URI,
            vararg attributeBuilders: IppAttributeBuilder,
            waitForTermination: Boolean = false

    ): IppJob {
        val request = attributeBuildersRequest(IppOperation.PrintUri, attributeBuilders).apply {
            operationGroup.attribute("document-uri", IppTag.Uri, documentUri)
        }
        val response = exchangeSuccessful(request)
        return handlePrintResponse(response, waitForTermination)
    }

    //-------------
    // Validate-Job
    //-------------

    fun validateJob(vararg attributeBuilders: IppAttributeBuilder): IppResponse {
        val request = attributeBuildersRequest(IppOperation.ValidateJob, attributeBuilders)
        return exchangeSuccessful(request)
    }

    //-----------
    // Create-Job
    //-----------

    fun createJob(vararg attributeBuilders: IppAttributeBuilder): IppJob {
        val request = attributeBuildersRequest(IppOperation.CreateJob, attributeBuilders)
        val response = exchangeSuccessful(request)
        return IppJob(this, response.jobGroup)
    }

    // ---- factory method for IppRequest with Operation Print-Job, Print-Uri, Validate-Job, Create-Job

    private fun attributeBuildersRequest(operation: IppOperation, attributeBuilders: Array<out IppAttributeBuilder>) =
            ippRequest(operation).apply {
                for (attributeBuilder in attributeBuilders) {
                    val attribute = attributeBuilder.buildIppAttribute(attributes)
                    checkValueSupported("${attribute.name}-supported", attribute.values)
                    // put attribute in operation or job group?
                    val groupTag = IppRegistrationsSection2.attributesMap[attribute.name]?.collectionGroupTag() ?: IppTag.Job
                    getSingleAttributesGroup(groupTag, true).put(attribute)
                }
            }

    private fun handlePrintResponse(printResponse: IppResponse, waitForTermination: Boolean = false): IppJob {
        val job = IppJob(this, printResponse.jobGroup)
        if (waitForTermination) {
            job.waitForTermination()
        }
        if (ippClient.verbose) {
            job.logDetails()
        }
        return job
    }

    //---------------------------
    // Get-Jobs (as List<IppJob>)
    //---------------------------

    fun getJobs(whichJobs: String? = null): List<IppJob> {
        val request = ippRequest(IppOperation.GetJobs)
        if (whichJobs != null) {
            // PWG Job and Printer Extensions Set 2
            checkValueSupported("which-jobs-supported", whichJobs)
            request.operationGroup.attribute("which-jobs", IppTag.Keyword, whichJobs)
        }
        val response = exchangeSuccessful(request)
        val jobGroups = response.getAttributesGroups(IppTag.Job)
        return jobGroups.map {
            IppJob(this, it)
        }
    }

    //-------------------------------
    // Get-Job-Attributes (as IppJob)
    //-------------------------------

    fun getJob(jobId: Int): IppJob {
        val response = exchangeSuccessfulIppRequest(IppOperation.GetJobAttributes, jobId)
        return IppJob(this, response.jobGroup)
    }

    //----------------------
    // delegate to IppClient
    //----------------------

    fun ippRequest(operation: IppOperation, jobId: Int? = null) =
            ippClient.ippRequest(operation, printerUri).apply {
                jobId?.let { operationGroup.attribute("job-id", IppTag.Integer, it) }
            }

    fun exchangeSuccessfulIppRequest(operation: IppOperation, jobId: Int? = null) =
            exchangeSuccessful(ippRequest(operation, jobId))

    fun exchangeSuccessful(request: IppRequest): IppResponse {
        checkValueSupported("ipp-versions-supported", ippClient.ippVersion)
        checkValueSupported("operations-supported", request.code!!.toInt())
        checkValueSupported("charset-supported", request.operationGroup.attributesCharset)
        return ippClient.exchangeSuccessful(request)
    }

    // -------
    // Logging
    // -------

    override fun toString() =
            "IppPrinter: name='$name', makeAndModel='$makeAndModel', state=$state, stateReasons=$stateReasons"

    fun logDetails() =
            attributes.logDetails(title = "PRINTER-$name ($makeAndModel), $state (${stateReasons.joinToString(",")})")

    // ------------------------------------------------------
    // attribute value checking based on printer capabilities
    // ------------------------------------------------------

    private fun checkValueSupported(supportedAttributeName: String, value: Any) {
        // condition is NOT always false, because this method is used during class initialization
        if (attributes.size == 0 || !checkValueSupported)
            return

        if (!supportedAttributeName.endsWith("-supported"))
            throw IppException("attribute name not ending with '-supported'")

        if (value is Collection<*>) { // instead of providing another signature just check collections iteratively
            for (collectionValue in value) {
                checkValueSupported(supportedAttributeName, collectionValue!!)
            }
        } else {
            isAttributeValueSupported(supportedAttributeName, value)
        }
    }

    private fun isAttributeValueSupported(supportedAttributeName: String, value: Any): Boolean? {

        val supportedAttribute = attributes[supportedAttributeName] ?: return null
        val isAttributeValueSupported = when (supportedAttribute.tag) {
            IppTag.Boolean -> supportedAttribute.value as Boolean // e.g. 'page-ranges-supported'
            IppTag.Charset,
            IppTag.NaturalLanguage,
            IppTag.MimeMediaType,
            IppTag.Keyword,
            IppTag.Enum,
            IppTag.Resolution -> when (supportedAttributeName) {
                "media-col-supported" -> {
                    (value as IppCollection).members
                            .filter { !supportedAttribute.values.contains(it.name) }
                            .forEach { println("WARN: member unsupported: $it") }
                    // all member names must be supported
                    supportedAttribute.values.containsAll(
                            value.members.map { it.name }
                    )
                }
                "ipp-versions-supported" -> supportedAttribute.values.contains(value.toString())
                else -> supportedAttribute.values.contains(value)
            }
            IppTag.Integer -> {
                if (supportedAttribute.is1setOf()) supportedAttribute.values.contains(value)
                else value is Int && value <= supportedAttribute.value as Int // e.g. 'job-priority-supported'
            }
            IppTag.RangeOfInteger -> {
                value is Int && value in supportedAttribute.value as IntRange
            }
            else -> null
        }
        when (isAttributeValueSupported) {
            true -> {
                // supported by printer
            }
            false -> {
                println("WARN: according to printer attributes value '${supportedAttribute.enumValueNameOrValue(value)}' is not supported.")
                println(supportedAttribute)
            }
            null -> println("WARN: unable to check if value '$value' is supported by $supportedAttribute")
        }
        return isAttributeValueSupported
    }

    // ---------------
    // CUPS extensions
    // ---------------

    val printerType: CupsPrinterType?
        get() = CupsPrinterType.fromInt(attributes.getValue("printer-type"))

    val markers: CupsMarker.List
        get() = CupsMarker.List(attributes)

    fun marker(color: CupsMarker.Color) =
            markers.single { it.color == color }

    val deviceUri: URI
        get() = attributes.getValue("device-uri")

}