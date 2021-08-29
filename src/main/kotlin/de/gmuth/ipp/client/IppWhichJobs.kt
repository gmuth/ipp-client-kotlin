package de.gmuth.ipp.client

/**
 * Copyright (c) 2021 Gerhard Muth
 */

// PWG Job, Printer and shared Infrastructure Extensions
enum class IppWhichJobs(val keyword: String) {
    // RFC 8011
    Completed("completed"),

    // PWG5100.7
    Aborted("aborted"),
    All("all"),
    Canceled("canceled"),
    Pending("pending"),
    PendingHeld("pending-held"),
    Processing("processing"),
    ProcessingStopped("processing-stopped"),

    // PWG5100.11
    NotCompleted("not-completed"),
    ProofPrint("proof-print"),
    Saved("saved"),

    // PWG5100.18
    Fetchable("fetchable")
}