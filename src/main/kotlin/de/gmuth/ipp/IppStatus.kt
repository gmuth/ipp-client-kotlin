package de.gmuth.ipp

/**
 * Author: Gerhard Muth
 */

enum class IppStatus(val code: Short) {

    SuccessfulOk(0x0000),
    SuccessfulOkIgnoredOrSubstitutedAttributes(0x0001),
    SuccessfulOkConflictingAttributes(0x0002),
    SuccessfulOkIgnoredSubscriptions(0x0003),
    SuccessfulOkIgnoredNotification(0x0004),
    SuccessfulOkTooManyEvents(0x0005),
    SuccessfulOkButCancelSubscription(0x0006),

    RedirectionOtherSite(0x0300),

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
    ClientErrorIgnoresAllNotifications(0x0416),
    ClientErrorPrintSupportFileNotFound(0x0417),

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
    ServerErrorPrinterIsDeactivated(0x050A);

    fun successfulOk() = code < 0x100

    override fun toString() = String.format("%04X %s", code, name)

    companion object {
        private val map = values().associateBy(IppStatus::code)
        fun fromShort(code: Short): IppStatus = map[code] ?: throw IllegalArgumentException(String.format("ipp status %04X undefined", code))
    }


}