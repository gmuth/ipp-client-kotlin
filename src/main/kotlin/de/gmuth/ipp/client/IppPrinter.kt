package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.ipp.core.*
import de.gmuth.ipp.cups.CupsMarker
import de.gmuth.ipp.cups.CupsPrinterCapability
import de.gmuth.ipp.cups.CupsPrinterType
import de.gmuth.ipp.iana.IppRegistrationsSection2
import de.gmuth.log.Log
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI

open class IppPrinter(
        val printerUri: URI,
        var attributes: IppAttributesGroup = IppAttributesGroup(IppTag.Printer),
        val ippClient: IppClient = IppClient(),
        trustAnyCertificate: Boolean = true
) {

    companion object {
        val log = Log.getWriter("IppPrinter", Log.Level.WARN)
    }

    var logDetails: Boolean
        get() = ippClient.logDetails
        set(value) {
            ippClient.logDetails = value
        }

    var httpBasicAuth: Http.BasicAuth?
        get() = ippClient.httpBasicAuth
        set(value) {
            ippClient.httpBasicAuth = value
        }

    var checkValueSupported: Boolean = true

    // by default operation Get-Jobs only returns attributes 'job-id' and 'job-uri'
    var requestedAttributesForGetJobsOperation = listOf(
            "job-id", "job-uri", "job-state", "job-state-reasons", "job-name", "job-originating-user-name"
    )

    init {
        if (trustAnyCertificate) ippClient.trustAnyCertificate()
        if (attributes.size == 0) updateAllAttributes()
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
        get() = IppPrinterState.fromInt(attributes.getValue("printer-state"))

    val stateReasons: List<String>
        get() = attributes.getValues("printer-state-reasons")

    val uriSupported: List<URI>
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

    fun pause() =
            exchangeSuccessfulIppRequest(IppOperation.PausePrinter)

    //---------------
    // Resume-Printer
    //---------------

    fun resume() =
            exchangeSuccessfulIppRequest(IppOperation.ResumePrinter)

    //-----------------------
    // Get-Printer-Attributes
    //-----------------------

    fun getPrinterAttributes(requestedAttributes: List<String>? = null): IppAttributesGroup {
        val ippRequest = ippRequest(IppOperation.GetPrinterAttributes, requestedAttributes = requestedAttributes)
        return exchangeSuccessful(ippRequest).printerGroup
    }

    fun updateAllAttributes() {
        // should we implement/incremental partial updates? e.g. for printer-state?
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
                    val groupTag = IppRegistrationsSection2.selectGroupForAttribute(attribute.name)
                    getSingleAttributesGroup(groupTag, createIfMissing = true).put(attribute)
                }
            }

    private fun handlePrintResponse(printResponse: IppResponse, waitForTermination: Boolean = false): IppJob {
        val job = IppJob(this, printResponse.jobGroup)
        if (waitForTermination) {
            job.waitForTermination()
        }
        if (ippClient.logDetails) {
            job.logDetails()
        }
        return job
    }

    //---------------------------
    // Get-Jobs (as List<IppJob>)
    //---------------------------

    fun getJobs(
            whichJobs: String? = null,
            requestedAttributes: List<String> = requestedAttributesForGetJobsOperation
    ): List<IppJob> {
        val ippRequest = ippRequest(IppOperation.GetJobs, requestedAttributes = requestedAttributes)
        if (whichJobs != null) {
            // PWG Job and Printer Extensions Set 2
            checkValueSupported("which-jobs-supported", whichJobs)
            ippRequest.operationGroup.attribute("which-jobs", IppTag.Keyword, whichJobs)
        }
        return exchangeSuccessful(ippRequest) // -> IppResponse
                .getAttributesGroups(IppTag.Job)
                .map { IppJob(this, it) }
    }

    //-------------------------------
    // Get-Job-Attributes (as IppJob)
    //-------------------------------

    fun getJob(jobId: Int): IppJob {
        val ippResponse = exchangeSuccessfulIppRequest(IppOperation.GetJobAttributes, jobId)
        return IppJob(this, ippResponse.jobGroup)
    }

    //----------------------
    // delegate to IppClient
    //----------------------

    fun ippRequest(operation: IppOperation, jobId: Int? = null, requestedAttributes: List<String>? = null) =
            ippClient.ippRequest(operation, printerUri).apply {
                // add optional 'job-id'
                if (jobId != null) {
                    operationGroup.attribute("job-id", IppTag.Integer, jobId)
                }
                // add optional 'requested-attributes'
                if (requestedAttributes != null && requestedAttributes.isNotEmpty()) {
                    operationGroup.attribute("requested-attributes", IppTag.Keyword, requestedAttributes)
                }
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
                            .forEach { log.warn { "member unsupported: $it" } }
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
                log.warn { "according to printer attributes value '${supportedAttribute.enumValueNameOrValue(value)}' is not supported." }
                log.warn { "$supportedAttribute" }
            }
            null -> log.warn { "unable to check if value '$value' is supported by $supportedAttribute" }
        }
        return isAttributeValueSupported
    }

    // ---------------
    // CUPS extensions
    // ---------------

    val printerType: CupsPrinterType
        get() = CupsPrinterType(attributes.getValue("printer-type"))

    fun hasCapability(capability: CupsPrinterCapability) =
            printerType.contains(capability)

    val markers: CupsMarker.List
        get() = CupsMarker.List(attributes)

    fun marker(color: CupsMarker.Color) =
            markers.single { it.color == color }

    val deviceUri: URI
        get() = attributes.getValue("device-uri")

}