package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup

// custom job attributes provided by Apple CUPS

data class ApplePrintJobInfo(
    val applicationName: String,
    val jobName: String,
    val jobOwner: String
) {
    constructor(jobAttributes: IppAttributesGroup) : this(
        jobAttributes.getText("com.apple.print.JobInfo.PMApplicationName"),
        jobAttributes.getText("com.apple.print.JobInfo.PMJobName"),
        jobAttributes.getText("com.apple.print.JobInfo.PMJobOwner")
    )
}