package de.gmuth.ipp.client

/**
 * Copyright (c) 2021 Gerhard Muth
 */

// PWG Job, Printer and shared Infrastructure Extensions
enum class IppWhichJobs(val keyword: String) {
    // RFC 8011
    Completed("completed"),
    NotCompleted("not-completed"),

    // PWG5100.7
    All("all"),
    Aborted("aborted"),
    Canceled("canceled"),
    Pending("pending"),
    Processing("processing"),
    PendingHeld("pending-held"),
    ProcessingStopped("processing-stopped"),

    // PWG5100.11
    ProofPrint("proof-print"),
    Saved("saved"),

    // PWG5100.18
    Fetchable("fetchable")
}