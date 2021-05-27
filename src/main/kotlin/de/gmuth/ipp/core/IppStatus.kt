package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// https://www.rfc-editor.org/rfc/rfc8011.html#appendix-B
// https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-11

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
    ClientErrorAttributesNotSettable(0x0413), // https://datatracker.ietf.org/doc/html/rfc3380#page-29
    ClientErrorIgnoredAllSubscriptions(0x0414), // https://datatracker.ietf.org/doc/html/rfc3995#page-71
    ClientErrorTooManySubscriptions(0x0415), // https://datatracker.ietf.org/doc/html/rfc3995#page-72
    ClientErrorDocumentPasswordError(0x0418), // https://ftp.pwg.org/pub/pwg/candidates/cs-ippjobprinterext3v10-20120727-5100.13.pdf
    ClientErrorDocumentPermissionError(0x0419),
    ClientErrorDocumentSecurityError(0x041A),
    ClientErrorDocumentUnprintableError(0x041B),
    ClientErrorAccountInfoNeeded(0x041C), // https://ftp.pwg.org/pub/pwg/candidates/cs-ipptrans10-20131108-5100.16.pdf
    ClientErrorAccountClosed(0x041D),
    ClientErrorAccountLimitReached(0x041E),
    ClientErrorAccountAuthorizationFailed(0x041F),
    ClientErrorNotFetchable(0x0420), // https://ftp.pwg.org/pub/pwg/candidates/cs-ippinfra10-20150619-5100.18.pdf

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
    ServerErrorPrinterIsDeactivated(0x050A), // https://datatracker.ietf.org/doc/html/rfc3998#page-23
    ServerErrorTooManyJobs(0x050B), // https://ftp.pwg.org/pub/pwg/candidates/cs-ippjobext20-20190816-5100.7.pdf
    ServerErrorTooManyDocuments(0x050C),

    // placeholder for all unknown status code
    UnknownStatusCode(-1);

    fun isSuccessful() = (0x0000..0x00FF).contains(code)
    fun isClientError() = (0x0400..0x04FF).contains(code)
    fun isServerError() = (0x0500..0x05FF).contains(code)

    override fun toString() = name
            .replace(Regex("[A-Z]+")) { "-" + it.value.toLowerCase() }
            .replace(Regex("^-"), "")

    companion object {
        fun fromShort(code: Short): IppStatus =
                values().find { it.code == code } ?: UnknownStatusCode
    }

}