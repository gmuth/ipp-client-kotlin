package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

enum class IppOperation(val code: Short) {

    PrintJob(0x0002),
    PrintUri(0x0003),
    ValidateJob(0x0004),
    CreateJob(0x0005),
    SendDocument(0x0006),
    SendUri(0x0007),
    CancelJob(0x0008),
    GetJobAttributes(0x0009),
    GetJobs(0x000A),
    GetPrinterAttributes(0x000B),
    HoldJob(0x000C),
    ReleaseJob(0x000D),
    RestartJob(0x000E),
    PausePrinter(0x0010),
    ResumePrinter(0x0011),
    PurgeJobs(0x0012),
    SetPrinterAttributes(0x0013),
    SetJobAttributes(0x0014),
    GetPrinterSupportedValues(0x0015),
    CreatePrinterSubscription(0x0016),
    CreateJobSubscription(0x0017),
    GetSubscriptionAttributes(0x0018),
    GetSubscriptions(0x0019),
    RenewSubscritpion(0x001A),
    CancelSubscription(0x001B),
    GetNotifications(0x001C),
    SendNotifications(0x001D),
    GetPrinterSupportFiles(0x0021),
    EnablePrinter(0x0022),
    DisablePrinter(0x0023),
    PausePrinterAfterCurrentJob(0x0024),
    HoldNewJobs(0x0025),
    ReleaseHeldNewJobs(0x0026),
    DeactivatePrinter(0x0027),
    ActivatePrinter(0x0028),
    RestartPrinter(0x0029),
    ShutdownPrinter(0x002A),
    StartupPrinter(0x002B),
    ReprocessJob(0x002C),
    CancelCurrentJob(0x002D),
    SuspendCurrentJob(0x002E),
    ResumeJob(0x002F),
    PromoteJob(0x0030),
    ScheduleJobAfter(0x0031),
    // a few more are missing

    // CUPS operations
    CupsGetDefault(0x4001),
    CupsGetPrinters(0x4002),
    CupsAddPrinter(0x4003),
    CupsDeletePrinter(0x4004),
    CupsGetClasses(0x4005),
    CuspAddClass(0x4006),
    CupsDeleteClass(0x4007),
    CupsAcceptJobs(0x4008),
    CupsRejectJobs(0x4009),
    CupsSetDefault(0x400A),
    CupsGetDevices(0x400B),
    CupsGetPPDs(0x400C),
    CupsMoveJob(0x400D);

    override fun toString(): String = registeredName()

    // https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-10
    private fun registeredName() = name
            .replace("[A-Z]".toRegex()) { "-" + it.value }
            .replace("^-".toRegex(), "")

    companion object {
        private val codeMap = values().associateBy(IppOperation::code)
        fun fromCode(code: Short): IppOperation = codeMap[code]
                ?: throw IppException(String.format("operation code '%04X' unknown", code))

        private val registeredNameMap = values().associateBy(IppOperation::registeredName)
        fun fromRegisteredName(name: String): IppOperation = registeredNameMap[name]
                ?: throw IppException(String.format("operation name '%s' unknown", name))
    }

}