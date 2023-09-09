package de.gmuth.log

/**
 * Copyright (c) 2023 Gerhard Muth
 */

import java.util.function.Supplier
import java.util.logging.Level
import java.util.logging.Level.*
import java.util.logging.Logger

fun Logger.finest(thrown: Throwable, msgSupplier: Supplier<String?> = Supplier { "" }) =
    log(FINEST, thrown, msgSupplier)

fun Logger.finer(thrown: Throwable, msgSupplier: Supplier<String?> = Supplier { "" }) =
    log(FINER, thrown, msgSupplier)

fun Logger.fine(thrown: Throwable, msgSupplier: Supplier<String?> = Supplier { "" }) =
    log(FINE, thrown, msgSupplier)

fun Logger.config(thrown: Throwable, msgSupplier: Supplier<String?> = Supplier { "" }) =
    log(CONFIG, thrown, msgSupplier)

fun Logger.info(thrown: Throwable, msgSupplier: Supplier<String?> = Supplier { "" }) =
    log(INFO, thrown, msgSupplier)

fun Logger.warning(thrown: Throwable, msgSupplier: Supplier<String?> = Supplier { "" }) =
    log(WARNING, thrown, msgSupplier)

fun Logger.severe(thrown: Throwable, msgSupplier: Supplier<String?> = Supplier { "" }) =
    log(SEVERE, thrown, msgSupplier)

// ----- aliases -----

// trace --> finer
fun Logger.trace(thrown: Throwable? = null, msgSupplier: Supplier<String?> = Supplier { "" }) =
    if (thrown == null) finer(msgSupplier) else finer(thrown, msgSupplier)

// debug --> fine
fun Logger.debug(thrown: Throwable? = null, msgSupplier: Supplier<String?> = Supplier { "" }) =
    if (thrown == null) fine(msgSupplier) else fine(thrown, msgSupplier)

// warn --> warning
fun Logger.warn(thrown: Throwable? = null, msgSupplier: Supplier<String?> = Supplier { "" }) =
    if (thrown == null) warning(msgSupplier) else warning(thrown, msgSupplier)

// error --> severe
fun Logger.error(thrown: Throwable? = null, msgSupplier: Supplier<String?> = Supplier { "" }) =
    if (thrown == null) severe(msgSupplier) else severe(thrown, msgSupplier)

fun Logger.logWithCauseMessages(throwable: Throwable, level: Level) {
    throwable.cause?.let { logWithCauseMessages(it, level) }
    log(level) { "${throwable.javaClass.name}: ${throwable.message}" }
}