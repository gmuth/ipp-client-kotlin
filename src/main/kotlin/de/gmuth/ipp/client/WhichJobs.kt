package de.gmuth.ipp.client

/**
 * Copyright (c) 2021-2023 Gerhard Muth
 */

// PWG Job, Printer and shared Infrastructure Extensions
enum class WhichJobs(val keyword: String) {
    // RFC 8011
    Completed("completed"),
    NotCompleted("not-completed"),

    // PWG 5100.7
    All("all"),
    Aborted("aborted"),
    Canceled("canceled"),
    Pending("pending"),
    Processing("processing"),
    PendingHeld("pending-held"),
    ProcessingStopped("processing-stopped"),

    // PWG 5100.11
    ProofPrint("proof-print"),
    Saved("saved"),

    // PWG 5100.18
    Fetchable("fetchable")
}