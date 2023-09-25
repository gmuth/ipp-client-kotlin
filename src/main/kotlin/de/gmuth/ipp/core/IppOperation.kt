package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

// https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-6

enum class IppOperation(val code: Short) {

    // RFC 8011
    PrintJob(0x0002),
    PrintURI(0x0003),
    ValidateJob(0x0004),
    CreateJob(0x0005),
    SendDocument(0x0006),
    SendURI(0x0007),
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
    CreatePrinterSubscriptions(0x0016),
    CreateJobSubscriptions(0x0017),
    GetSubscriptionAttributes(0x0018),
    GetSubscriptions(0x0019),
    RenewSubscription(0x001A),
    CancelSubscription(0x001B),
    GetNotifications(0x001C),
    GetResourceAttributes(0x001E),
    GetResources(0x0020),

    // RFC 3998
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

    CancelDocument(0x0033),
    GetDocumentAttributes(0x0034),
    GetDocuments(0x0035),
    DeleteDocument(0x0036),
    SetDocumentAttributes(0x0037),
    CancelJobs(0x0038),
    CancelMyJobs(0x0039),
    CloseJob(0x003A),
    ResubmitJob(0x003B),
    IdentifyPrinter(0x003C),
    ValidateDocument(0x003D),
    AddDocumentImages(0x003E),

    // PWG 5100.18 Infra
    AcknowledgeDocument(0x003F),
    AcknowledgeIdentifyPrinter(0x0040),
    AcknowledgeJob(0x0041),
    FetchDocument(0x0042),
    FetchJob(0x0043),
    GetOutputDeviceAttributes(0x0044),
    UpdateActiveJobs(0x0045),
    DeregisterOutputDevice(0x0046),
    UpdateDocumentStatus(0x0047),
    UpdateJobStatus(0x0048),
    UpdateOutputDeviceAttributes(0x0049),

    // PWG 5100.22 System Service
    GetNextDocumentData(0x004A),
    AllocatePrinterResources(0x004B),
    CreatePrinter(0x004C),
    DeallocatePrinterResources(0x004D),
    DeletePrinter(0x004E),
    GetPrinters(0x004F),
    ShutdownOnePrinter(0x0050),
    StartupOnePrinter(0x0051),
    CancelResource(0x0052),
    CreateResource(0x0053),
    InstallResource(0x0054),
    SendResourceData(0x0055),
    SetResourceAttributes(0x0056),
    CreateResourceSubscriptions(0x0057),
    CreateSystemSubscriptions(0x0058),
    DisableAllPrinters(0x0059),
    EnableAllPrinters(0x005A),
    GetSystemAttributes(0x005B),
    GetSystemSupportedValues(0x005C),
    PauseAllPrinters(0x005D),
    PauseAllPrintersAfterCurrentJob(0x005E),
    RegisterOutputDevice(0x005F),
    RestartSystem(0x0060),
    ResumeAllPrinters(0x0061),
    SetSystemAttributes(0x0062),
    ShutdownAllPrinters(0x0063),
    StartupAllPrinters(0x0064),
    GetPrinterResources(0x0065),
    GetUserPrinterAttributes(0x0066),
    RestartOnePrinter(0x0067),

    // CUPS Operations
    CupsGetDefault(0x4001),
    CupsGetPrinters(0x4002),
    CupsAddModifyPrinter(0x4003),
    CupsDeletePrinter(0x4004),
    CupsGetClasses(0x4005),
    CupsAddModifyClass(0x4006),
    CupsDeleteClass(0x4007),
    CupsAcceptJobs(0x4008),
    CupsRejectJobs(0x4009),
    CupsSetDefault(0x400A),
    CupsGetDevices(0x400B),
    CupsGetPPDs(0x400C),
    CupsMoveJob(0x400D),
    CupsAuthenticateJob(0x400E),
    CupsGetPPD(0x400F),
    CupsGetDocument(0x4027),
    CupsCreateLocalPrinter(0x4028);

    override fun toString(): String = registeredName()

    fun registeredName() = name
        .replace(Regex("[A-Z]+")) { "-" + it.value }
        .replace(Regex("^-"), "")

    companion object {
        fun fromShort(code: Short): IppOperation =
            values().find { it.code == code } ?: throw IppException("Unknown operation code %04x".format(code))
    }

}