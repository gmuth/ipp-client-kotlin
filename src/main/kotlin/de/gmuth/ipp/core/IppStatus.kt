package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// RFC 8011
enum class IppStatus(val code: Short) {

    SuccessfulOk(0x0000),
    SuccessfulOkIgnoredOrSubstitutedAttributes(0x0001),
    SuccessfulOkConflictingAttributes(0x0002),
    SuccessfulOkIgnoredSubscriptions(0x0003),
    SuccessfulOkTooManyEvents(0x0005),
    SuccessfulOkEventsComplete(0x0007),

    ClientErrorBadRequest(0x0400),
    ClientErrorForbidden(0x0401),
    ClientErrorNotAuthenticated(0x0402),
    ClientErrorNotAuthorized(0x0403),
    ClientErrorNotPossible(0x0404),
    ClientErrorTimeout(0x0405),
    ClientErrorNotFound(0x0406),
    ClientErrorGone(0x0407),
    ClientErrorRequestEntityTooLarge(0x0408),
    ClientErrorRequestValueTooLarge(0x0409),
    ClientErrorDocumentFormatNotSupported(0x040A),
    ClientErrorAttributesOrValuesNotSupported(0x040B),
    ClientErrorUriSchemeNotSupported(0x040C),
    ClientErrorCharsetNotSupported(0x040D),
    ClientErrorConflictingAttribute(0x040E),
    ClientErrorCompressionNotSupported(0x040F),
    ClientErrorCompressionError(0x0410),
    ClientErrorDocumentFormatError(0x0411),
    ClientErrorDocumentAccessError(0x0412),
    ClientErrorAttributesNotSettable(0x0413),
    ClientErrorIgnoredAllSubscriptions(0x0414),
    ClientErrorTooManySubscriptions(0x0415),
    ClientErrorDocumentPasswordError(0x0418),
    ClientErrorDocumentPermissionError(0x0419),
    ClientErrorDocumentSecurityError(0x041A),
    ClientErrorDocumentUnprintableError(0x041B),
    ClientErrorAccountInfoNeeded(0x041C),
    ClientErrorAccountClosed(0x041D),
    ClientErrorAccountLimitReached(0x041E),
    ClientErrorAccountAuthorizationFailed(0x041F),
    ClientErrorNotFetchable(0x0420),

    ServerErrorInternalError(0x0500),
    ServerErrorOperationNotSupported(0x0501),
    ServerErrorServiceUnavailable(0x0502),
    ServerErrorVersionNotSupported(0x0503),
    ServerErrorDeviceError(0x0504),
    ServerErrorTemporaryError(0x0505),
    ServerErrorNotAcceptingJobs(0x0506),
    ServerErrorBusy(0x0507),
    ServerErrorJobCanceled(0x0508),
    ServerErrorMultipleDocumentJobsNotSupported(0x0509),
    ServerErrorPrinterIsDeactivated(0x050A),
    ServerErrorTooManyJobs(0x050B),
    ServerErrorTooManyDocuments(0x050c);

    fun isSuccessful() = code in 0x000..0x00FF
    fun isClientError() = code in 0x0400..0x04FF
    fun isServerError() = code in 0x0500..0x05FF

    // https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-11
    private fun registeredValue() = name
            .replace("[A-Z]".toRegex()) { "-" + it.value.toLowerCase() }
            .replace("^-".toRegex(), "")

    override fun toString() = registeredValue()

    companion object {
        private val codeMap = values().associateBy(IppStatus::code)
        fun fromCode(code: Short): IppStatus = codeMap[code]
                ?: throw IllegalArgumentException(String.format("ipp status %04X undefined", code))

        private val registeredValueMap = values().associateBy(IppStatus::registeredValue)
        fun fromRegisteredValue(value: String): IppStatus = registeredValueMap[value]
                ?: throw IllegalArgumentException(String.format("ipp status value '%s' not found", value))

    }

}