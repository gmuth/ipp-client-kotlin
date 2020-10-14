package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.ipp.core.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI

class IppPrinter(val printerUri: URI, val ippClient: IppClient = IppClient(), val verbose: Boolean = false) {

    var attributes: IppAttributesGroup
    var httpAuth: Http.Auth?
        get() = ippClient.httpAuth
        set(value) {
            ippClient.httpAuth = value
        }
    var checkValueSupported: Boolean = true

    init {
        ippClient.verbose = verbose
        attributes = getPrinterAttributes()
    }

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
                with(ippAttributesGroup(IppTag.Job)) {
                    for (attributeBuilder in attributeBuilders) {
                        val attribute = attributeBuilder.buildIppAttribute(attributes)
                        checkValueSupported("${attribute.name}-supported", attribute.values)
                        put(attribute, validateTag = true)
                    }
                }
            }

    private fun handlePrintResponse(printResponse: IppResponse, wait: Boolean = false): IppJob {
        val job = IppJob(this, printResponse.jobGroup)
        if (wait) {
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
        val response = exchangeSuccessfulIppJobRequest(IppOperation.GetJobAttributes, jobId)
        return IppJob(this, response.jobGroup)
    }

    //----------------------
    // delegate to IppClient
    //----------------------

    fun ippRequest(operation: IppOperation) =
            ippClient.ippRequest(operation, printerUri)

    fun ippJobRequest(operation: IppOperation, jobId: Int) =
            ippClient.ippJobRequest(operation, printerUri, jobId)

    fun exchangeSuccessfulIppRequest(operation: IppOperation) =
            exchangeSuccessful(ippRequest(operation))

    fun exchangeSuccessfulIppJobRequest(operation: IppOperation, jobId: Int) =
            exchangeSuccessful(ippJobRequest(operation, jobId))

    fun exchangeSuccessful(request: IppRequest): IppResponse {
        checkValueSupported("ipp-versions-supported", ippClient.ippVersion)
        checkValueSupported("operations-supported", request.code!!.toInt())
        checkValueSupported("charset-supported", request.attributesCharset)
        return ippClient.exchangeSuccessful(printerUri, request)
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
        if (attributes == null || !checkValueSupported) return
        if (value is Collection<*>) { // instead of providing another signature just check collections iteratively
            for (collectionValue in value) checkValueSupported(supportedAttributeName, collectionValue!!)
        } else {
            isAttributeValueSupported(supportedAttributeName, value)
        }
    }

    private fun isAttributeValueSupported(supportedAttributeName: String, value: Any): Boolean? {
        if (!supportedAttributeName.endsWith("-supported"))
            throw IppException("attribute name not ending with '-supported'")

        val supportedAttribute = attributes[supportedAttributeName] ?: return null
        val isAttributeValueSupported = when (supportedAttribute.tag) {
            IppTag.Boolean -> supportedAttribute.value as Boolean // e.g. 'page-ranges-supported'
            IppTag.Charset,
            IppTag.NaturalLanguage,
            IppTag.MimeMediaType,
            IppTag.Keyword,
            IppTag.Enum,
            IppTag.Resolution -> supportedAttribute.values.contains(value) || supportedAttributeName.toLowerCase() == "media-col-supported"
            IppTag.Integer ->
                if (supportedAttribute.is1setOf()) supportedAttribute.values.contains(value)
                else value is Int && value <= supportedAttribute.value as Int // e.g. 'job-priority-supported'
            IppTag.RangeOfInteger -> value is Int && value in supportedAttribute.value as IntRange
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

}
