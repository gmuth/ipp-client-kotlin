# ipp-client for java and kotlin

A client implementation of the ipp protocol written in kotlin.
RFCs [8010](https://tools.ietf.org/html/rfc8010),
[8011](https://tools.ietf.org/html/rfc8011),
[3995](https://datatracker.ietf.org/doc/html/rfc3995) and
[3996](https://datatracker.ietf.org/doc/html/rfc3996)

[![Build](https://github.com/gmuth/ipp-client-kotlin/workflows/build/badge.svg)](https://github.com/gmuth/ipp-client-kotlin/actions?query=workflow%3Abuild)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=gmuth_ipp-client-kotlin&metric=alert_status)](https://sonarcloud.io/summary/overall?id=gmuth_ipp-client-kotlin)
[![Sonar Coverage](https://img.shields.io/sonar/coverage/gmuth_ipp-client-kotlin?color=00AA00&server=https%3A%2F%2Fsonarcloud.io)](https://sonarcloud.io/component_measures?metric=Coverage&view=list&id=gmuth_ipp-client-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/de.gmuth/ipp-client.svg?label=maven%20central)](https://search.maven.org/search?q=g:%22de.gmuth%22%20AND%20a:%22ipp-client%22)

## Usage

You may use ```ippfind``` or other ZeroConf tools for printer discovery.
The [CupsClient](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/CupsClient.kt) supports printer lookup by queue name.
Repository [ipp-samples](https://github.com/gmuth/ipp-samples) contains examples how to use jmDNS.

### [IppPrinter](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppPrinter.kt) and [IppJob](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppJob.kt)
```kotlin
// initialize printer connection and show printer attributes
val ippPrinter = IppPrinter(URI.create("ipp://colorjet.local/ipp/printer"))
ippPrinter.attributes.logDetails()

// marker levels
ippPrinter.markers.forEach { println(it) }
println("black: ${ippPrinter.marker(BLACK).levelPercent()} %")

// print file
val file = File("A4-ten-pages.pdf")
val job = ippPrinter.printJob(
    file,
    jobName(file.name),
    jobPriority(30),
    documentFormat("application/pdf"),
    copies(2),
    numberUp(2),
    pageRanges(2..3, 8..10),
    printerResolution(300),
    finishings(Punch, Staple),
    IppPrintQuality.High,
    IppColorMode.Monochrome,
    IppSides.TwoSidedLongEdge,
    media("iso_a4_210x297mm"),
    mediaColSource("tray-1"),
    notifyEvents = listOf("job-state-changed", "job-stopped", "job-completed") // CUPS
)
job.logDetails()
job.subscription?.processEvents { println(it) }

// print remote file, make printer pull document from remote server
val remoteFile = URI.create("http://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
ippPrinter.printUri(remoteFile)

// create job and send document
val job = ippPrinter.createJob(jobName(file.name))
job.sendDocument(FileInputStream(file))
job.waitForTermination()

// manage jobs
ippPrinter.getJobs().forEach { println(it) }
ippPrinter.getJobs(IppWhichJobs.Completed)

val job = ippPrinter.getJob(4)
job.hold()
job.release()
job.cancel()

// print operator
ippPrinter.pause()
ippPrinter.resume()
ippPrinter.sound() // identify printer

// subscribe and log events (e.g. from CUPS) for 1 minute
ippPrinter.createPrinterSubscription(60)
          .processEvents()
```
### Printer Capabilities

`IppPrinter` checks, if attribute values are supported by looking into `'...-supported'` printer attributes.
```
documentFormat("application/pdf")

WARN: according to printer attributes value 'application/pdf' is not supported.
document-format-supported (1setOf mimeMediaType) = application/PCL,application/postscript
```

### exchange [IppRequest](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppRequest.kt) for [IppResponse](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppResponse.kt)

```kotlin
val uri = URI.create("ipp://colorjet.local/ipp/printer")
val file = File("A4-blank.pdf")

val ippClient = IppClient()
val request = IppRequest(IppOperation.PrintJob, uri).apply {
  // constructor adds 'attributes-charset', 'attributes-natural-language' and 'printer-uri'
  operationGroup.attribute("document-format", IppTag.MimeMediaType, "application/pdf")
  documentInputStream  = FileInputStream(file)
}
val response = ippClient.exchange(request)
println(response.jobGroup["job-id"])
```

### [CupsClient](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/CupsClient.kt)

Use the `CupsClient` to connect to a CUPS server.
If you want to access a cups queue you can construct an `IppPrinter` from it's uri.

```kotlin
// connect to default ipp://localhost:631
val cupsClient = CupsClient()

// credentials (e.g. for remote connections)
cupsClient.basicAuth("admin", "secret")

// list all queues
cupsClient.getPrinters().forEach { 
    println("${it.name} -> ${it.printerUri}")
}

// list all completed jobs for queue
cupsClient.getPrinter("ColorJet_HP")
          .getJobs(IppWhichJobs.Completed)
          .forEach { println(it) }

// default printer
val defaultPrinter = cupsClient.getDefault()

// check capability
if(defaultPrinter.hasCapability(Capability.CanPrintInColor)) {
    println("${defaultPrinter.name} can print in color")
}
```

### Print jpeg to 2" label printer

```kotlin
val printer = IppPrinter(URI.create("ipp://192.168.2.64"))
val width = 2540 * 2 // hundreds of mm

val jpegFile = File("label.jpg")
val image = javax.imageio.ImageIO.read(jpegFile)
            
printer.printJob(
    jpegFile, documentFormat("image/jpeg"),
    IppMedia.Collection(
        size = IppMedia.Size(width, width * image.height / image.width),
        margins = IppMedia.Margins(0)
    )
)
```

## Logging

Log levels can be changed globally or individually.
The `defaultLogLevel` must be changed before any constructor of a logger is called.

```kotlin
Logging.defaultLogLevel = Logging.LogLevel.ERROR
IppInputStream.log.logLevel = Logging.LogLevel.DEBUG
IppOutputStream.log.logLevel = Logging.LogLevel.TRACE
```

A simple stdout console writer is enabled by default and can be disabled.

```kotlin
Logging.consoleWriterEnabled = false
```

You can configure the library to use 
[Java Util Logging](https://docs.oracle.com/en/java/javase/11/core/java-logging-overview.html)
or [Slf4j](http://www.slf4j.org).
Then the log levels must be configured according to the underlaying implementation 
(e.g. [logback](http://logback.qos.ch/manual/configuration.html)
or [Slf4j-Android](http://www.slf4j.org/android/)).

```kotlin
JulAdapter.configure("/ipp-client-logging.conf")
Slf4jAdapter.configure()
```
    
## Source packages

Package
[`de.gmuth.ipp.core`](https://github.com/gmuth/ipp-client-kotlin/tree/master/src/main/kotlin/de/gmuth/ipp/core)
contains the usual
[encoding](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppOutputStream.kt)
and
[decoding](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppInputStream.kt)
operations. RFC 8010 is fully supported.
E.g. decode a cups spool file: 
`IppRequest().read(File("/var/spool/cups/c01579")).logDetails()`

Package
[`de.gmuth.ipp.client`](https://github.com/gmuth/ipp-client-kotlin/tree/master/src/main/kotlin/de/gmuth/ipp/client)
contains the
[IppClient](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppClient.kt)
which requires a
[Http.Client](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/http/Http.kt)
that implements HTTP:
e.g. [HttpURLConnectionClient](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/http/HttpURLConnectionClient.kt)
or [JavaHttpClient](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/http/JavaHttpClient.kt)

## Build

To build the jar make sure you have JDK 11 installed.
The default tasks build the jar in `build/libs`. 

    ./gradlew

To install the artifact to your local maven repository run

    ./gradlew publishToMavenLocal

This software has **no dependencies** to
[javax.print](https://docs.oracle.com/javase/7/docs/technotes/guides/jps/),
[CUPS](https://www.cups.org) or
[ipptool](https://www.cups.org/doc/man-ipptool.html).
Operation has mostly been tested for target `jvm`. Android is supported since v1.6.
A Java Version 11 Runtime is only required if you want to use the Java 11 HttpClient.

## Artifact coordinates

The build produces the jar, sources and javadoc artifacts. They are available at Sonatypes
[Maven Central Repository](https://search.maven.org/artifact/de.gmuth/ipp-client)

- group: gmuth.de
- artifact: ipp-client
- version: 2.3

Add dependency:

```
    implementation("de.gmuth:ipp-client:2.3")
```

## No Multiplatform support yet

IPP is based on the exchange of binary messages via HTTP.
For reading and writing binary data
[DataInputStream](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/DataInputStream.html)
and [DataOutputStream](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/DataOutputStream.html) are used.

For the transport layer I've created a
[HTTP interface](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/http/Http.kt).
By default implementation [HttpURLConnectionClient](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/http/HttpURLConnectionClient.kt)
is used which in turn uses javas [HttpURLConnection](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/HttpURLConnection.html).

Only java runtimes (including Android) provide implementations of these classes.
The java standard libraries also provide [support for SSL/TLS](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/javax/net/ssl/SSLContext.html).
