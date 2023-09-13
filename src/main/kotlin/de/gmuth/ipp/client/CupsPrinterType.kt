package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import java.util.logging.Level
import java.util.logging.Level.INFO
import java.util.logging.Logger
import java.util.logging.Logger.getLogger

// https://www.cups.org/doc/spec-ipp.html
class CupsPrinterType(val value: Int) {

    val log = getLogger(javaClass.name)

    enum class Capability(val bit: Int, val description: String) {
        IsAPrinterClass(0, "Is a printer class."),
        IsARemoteDestination(1, "Is a remote destination."),
        CanPrintInBlack(2, "Can print in black."),
        CanPrintInColor(3, "Can print in color."),
        CanPrintOnBothSidesOfThePageInHardware(4, "Can print on both sides of the page in hardware."),
        CanStapleOutput(5, "Can staple output."),
        CanDoFastCopiesInHardware(6, "Can do fast copies in hardware."),
        CanDoFastCopyCollationInHardware(7, "Can do fast copy collation in hardware."),
        CanPunchOutput(8, "Can punch output."),
        CanCoverOutput(9, "Can cover output."),
        CanBindOutput(10, "Can bind output."),
        CanSortOutput(11, "Can sort output."),
        CanHandleMediaUpToUsLegalA4(12, "Can handle media up to US-Legal/A4."),
        CanHandleMediaFromUsLegalA4toIsoCA2(13, "Can handle media from US-Legal/A4 to ISO-C/A2."),
        CanHandleMediaLargerThanIsoCA2(14, "Can handle media larger than ISO-C/A2."),
        CanHandleUserDefinedMediaSizes(15, "Can handle user-defined media sizes."),
        IsAnImplicitServerGeneratedClass(16, "Is an implicit (server-generated) class."),
        IsTheDefaultPrinterOnTheNetwork(17, "Is the a default printer on the network."),
        IsAFacsimileDevice(18, "Is a facsimile device."),
        IsRejectingJobs(19, "Is rejecting jobs."),
        DeleteThisQueue(20, "Delete this queue."),
        QueueIsNotShared(21, "Queue is not shared."),
        QueueRequiresAuthentication(22, "Queue requires authentication."),
        QueueSupportsCUPSCommandFiles(23, "Queue supports CUPS command files."),
        QueueWasAutomaticallyDiscoveredAndAdded(24, "Queue was automatically discovered and added."),
        QueueIsAScannerWithNoPrintingCapabilities(25, "Queue is a scanner with no printing capabilities."),
        QueueIsAPrinterWithScanningCapabilities(26, "Queue is a printer with scanning capabilities."),
        QueueIsAPrinterWith3DCapabilities(27, "Queue is a printer with 3D capabilities.")
    }

    fun toSet(): Set<Capability> = Capability
        .values()
        .filter { (value shr it.bit) and 1 == 1 }
        .toSet()

    fun contains(capability: Capability) = toSet().contains(capability)

    override fun toString() = "$value (${toSet().joinToString(",")})"

    fun log(logger: Logger, level: Level = INFO) = logger.run {
        log(level) { "PRINTER-TYPE 0x%08X capabilities:".format(value) }
        for (capability in toSet()) {
            log(level) { "* ${capability.description}" }
        }
    }

}