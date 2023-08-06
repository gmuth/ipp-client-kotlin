package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppString

// custom job attributes provided by Apple CUPS

data class ApplePrintJobInfo(
    val applicationName: String,
    val jobOwner: String,
    val jobName: String?, // not available in CUPS 2.2.5
) {
    constructor(jobAttributes: IppAttributesGroup) : this(
        jobAttributes.getTextValue("com.apple.print.JobInfo.PMApplicationName"),
        jobAttributes.getTextValue("com.apple.print.JobInfo.PMJobOwner"),
        jobAttributes.getValueOrNull<IppString>("com.apple.print.JobInfo.PMJobName")?.text,
    )
}