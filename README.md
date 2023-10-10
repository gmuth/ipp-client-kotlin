# ipp-client 3.0

A client implementation of the ipp protocol for java and kotlin.
RFCs [8010](https://tools.ietf.org/html/rfc8010),
[8011](https://tools.ietf.org/html/rfc8011),
[3995](https://datatracker.ietf.org/doc/html/rfc3995) and
[3996](https://datatracker.ietf.org/doc/html/rfc3996)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?label=license)](https://github.com/gmuth/ipp-client-kotlin/blob/master/LICENSE)
[![Build](https://github.com/gmuth/ipp-client-kotlin/workflows/build/badge.svg)](https://github.com/gmuth/ipp-client-kotlin/actions?query=workflow%3Abuild)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=gmuth_ipp-client-kotlin&metric=alert_status)](https://sonarcloud.io/summary/overall?id=gmuth_ipp-client-kotlin)
[![Sonar Coverage](https://img.shields.io/sonar/coverage/gmuth_ipp-client-kotlin?color=00AA00&server=https%3A%2F%2Fsonarcloud.io)](https://sonarcloud.io/component_measures?metric=Coverage&view=list&id=gmuth_ipp-client-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/de.gmuth/ipp-client.svg?label=maven%20central)](https://central.sonatype.com/artifact/de.gmuth/ipp-client/2.5/overview)

## Usage

You may use ```ippfind``` or other ZeroConf tools for printer discovery.
The [CupsClient](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/CupsClient.kt)
supports printer lookup by queue name.
Repository [ipp-samples](https://github.com/gmuth/ipp-samples) contains examples how to use jmDNS.

```
implementation("de.gmuth:ipp-client:3.0")
```

[README.md for version 2.x](https://github.com/gmuth/ipp-client-kotlin/blob/2.5/README.md) is still available.
### [IppPrinter](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppPrinter.kt) and [IppJob](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppJob.kt)

```kotlin
// initialize printer connection and show printer attributes
val ippPrinter = IppPrinter(URI.create("ipp://colorjet.local/ipp/printer"))
ippPrinter.attributes.log(logger)

// marker levels
ippPrinter.markers.forEach { println(it) }
println("black: ${ippPrinter.marker(BLACK).levelPercent()} %")

// print file
val file = File("A4-ten-pages.pdf")
val job = ippPrinter.printJob(
    file,
    copies(2),
    numberUp(2),
    jobPriority(30),
    jobName(file.name),
    DocumentFormat.PDF,
    pageRanges(2..3, 8..10),
    finishings(Punch, Staple),
    printerResolution(300, DPI),
    Sides.TwoSidedLongEdge,
    ColorMode.Monochrome,
    PrintQuality.High,
    Media.ISO_A4,
    mediaColWithSource("tray-1"),
    notifyEvents = listOf("job-state-changed", "job-stopped", "job-completed") // CUPS
)
job.subscription?.pollAndHandleNotifications { println(it) }

// print remote file, make printer pull document from remote server
val remoteFile = URI.create("http://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
ippPrinter.printUri(remoteFile)

// create job and send document
val job = ippPrinter.createJob(jobName(file.name))
job.sendDocument(FileInputStream(file))
job.waitForTermination()

// manage jobs
ippPrinter.getJobs().forEach { println(it) }
ippPrinter.getJobs(WhichJobs.Completed)

val job = ippPrinter.getJob(4)
job.hold()
job.release()
job.cancel()
job.cupsGetDocuments() // CUPS only

// print operator
ippPrinter.pause()
ippPrinter.resume()
ippPrinter.sound() // identify printer

// subscribe and handle/log events (e.g. from CUPS) for 5 minutes
ippPrinter
    .createPrinterSubscription(notifyLeaseDuration=Duration.ofMinutes(5))
    .pollAndHandleNotifications()
```

IppPrinter checks, if attribute values are supported by looking into `'...-supported'` printer attributes.

```
DocumentFormat("application/pcl")

WARN: according to printer attributes value 'application/pcl' is not supported.
document-format-supported (1setOf mimeMediaType) = application/pdf,application/postscript
```

### Exchange [IppRequest](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppRequest.kt) for [IppResponse](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppResponse.kt)

```kotlin
val uri = URI.create("ipp://colorjet.local/ipp/printer")
val file = File("A4-blank.pdf")

val ippClient = IppClient()
val request = IppRequest(IppOperation.PrintJob, uri).apply {
    // Constructor adds 'attributes-charset', 'attributes-natural-language' and 'printer-uri'
    operationGroup.attribute("document-format", IppTag.MimeMediaType, "application/pdf")
    documentInputStream = FileInputStream(file)
}
val response = ippClient.exchange(request)
println(response.jobGroup["job-id"])
```

### [CupsClient](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/CupsClient.kt)

Use the `CupsClient` to connect to a CUPS server.
If you want to access a cups queue you can construct an `IppPrinter` from its uri.

```kotlin
// Connect to default ipp://localhost:631
val cupsClient = CupsClient()

// Credentials (e.g. for remote connections)
cupsClient.basicAuth("admin", "secret")

// List all queues
cupsClient.getPrinters().forEach {
    println("${it.name} -> ${it.printerUri}")
}

// List all completed jobs for queue
cupsClient.getPrinter("ColorJet_HP")
    .getJobs(WhichJobs.Completed)
    .forEach { println(it) }

// Default printer
val defaultPrinter = cupsClient.getDefault()

// Check capability
if (defaultPrinter.hasCapability(Capability.CanPrintInColor)) {
    println("${defaultPrinter.name} can print in color")
}

// Get canceled jobs and save documents
cupsClient.getJobsAndSaveDocuments(WhichJobs.Canceled)

// Setup IPP Everywhere Printer
cupsClient.setupIppEverywherePrinter(
    "myprinter",
    URI.create("ipp://myprinter.local:631/ipp/print"),
    "My description",
    "My location"
)

```

### Print jpeg to 2" label printer

```kotlin
val printer = IppPrinter(URI.create("ipp://192.168.2.64"))
val jpegFile = File("label.jpeg")
val image = javax.imageio.ImageIO.read(jpegFile)
val width = 2540 * 2 // hundreds of mm

printer.printJob(
    jpegFile,
    DocumentFormat.JPEG,
    MediaCollection(
        MediaSize(width, width * image.height / image.width),
        MediaMargin(300) // 3 mm
    )
)
```

## Logging

From version 3.0 onwards the library
uses [Java Logging](https://docs.oracle.com/javase/8/docs/technotes/guides/logging/overview.html) - configure as you
like.
Tests can use Logging.configure() to load logging.properties from test/resources.
The behaviour of my previously
used [ConsoleLogger](https://github.com/gmuth/logging-kotlin/blob/main/src/main/kotlin/de/gmuth/log/ConsoleLogger.kt) is
now implemented by StdoutHandler and SimpleClassNameFormatter.
I moved all of my custom logging code to its own
repository [logging-kotlin](https://github.com/gmuth/logging-kotlin/tree/main/src/main/kotlin/de/gmuth/log).

## Sources

To build the jar make sure you have JDK 11 installed.
The default tasks build the jar in `build/libs`.

    ./gradlew

To install the artifact to your local maven repository run

    ./gradlew publishToMavenLocal

The build produces the jar, sources and javadoc artifacts. This software has **no dependencies** to
[javax.print](https://docs.oracle.com/javase/7/docs/technotes/guides/jps/),
[CUPS](https://www.cups.org) or
[ipptool](https://www.cups.org/doc/man-ipptool.html).
Operation has mostly been tested for target `jvm`. Android is supported since v1.6.

Package
[`de.gmuth.ipp.core`](https://github.com/gmuth/ipp-client-kotlin/tree/master/src/main/kotlin/de/gmuth/ipp/core)
contains the usual
[encoding](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppOutputStream.kt)
and
[decoding](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppInputStream.kt)
operations. RFC 8010 is fully supported.
Package
[`de.gmuth.ipp.client`](https://github.com/gmuth/ipp-client-kotlin/tree/master/src/main/kotlin/de/gmuth/ipp/client)
contains the
[IppClient](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppClient.kt)
and implementations of higher level IPP objects like
[IppPrinter]((https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppPrinter.kt)),
[IppJob]((https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppJob.kt)),
[IppSubscription]((https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppSubscription.kt)) and
[IppEventNotification]((https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppEventNotification.kt)).

IPP is based on the exchange of binary messages via HTTP.
For reading and writing binary data
[DataInputStream](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/DataInputStream.html)
and [DataOutputStream](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/DataOutputStream.html) are
used. For message transportation IppClient uses [HttpURLConnection](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/HttpURLConnection.html).

Only Java runtimes (including Android) provide implementations of these classes.
The Java standard libraries also
provide [support for SSL/TLS](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/javax/net/ssl/SSLContext.html)
.