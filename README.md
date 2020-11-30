# ipp-client-kotlin

A client implementation of the ipp protocol written in kotlin.
[RFC 8010](https://tools.ietf.org/html/rfc8010),
[RFC 8011](https://tools.ietf.org/html/rfc8011)

[![Build with Gradle](https://github.com/gmuth/ipp-client-kotlin/workflows/Build/badge.svg)](https://github.com/gmuth/ipp-client-kotlin/actions?query=workflow%3ABuild)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=gmuth_ipp-client-kotlin&metric=alert_status)](https://sonarcloud.io/dashboard?id=gmuth_ipp-client-kotlin)
## Usage

You should know the IPP printerUri. You may use ```ippfind``` or other ZeroConf tools for discovery. The CupsClient supports printer lookup up by queue name. 

### [IppPrinter](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppPrinter.kt) and [IppJob](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppJob.kt)
```kotlin
// initialize printer connection and show printer attributes
val ippPrinter = IppPrinter(URI.create("ipp://colorjet.local/ipp/printer"))
ippPrinter.attributes.logDetails()

// marker levels
ippPrinter.markers.forEach { println(it) }
println("black: ${ippPrinter.marker(BLACK).levelPercent()} %")

// print file
val file = File("A4-ten-blank.pdf")
val job = ippPrinter.printJob(
    file,
    jobName(file.name),
    jobPriority(30),
    documentFormat("application/pdf"),
    media("iso_a4_210x297mm"),
    copies(2),
    numberUp(2),
    pageRanges(2..3, 8..10),
    printerResolution(300),
    IppPrintQuality.High,
    IppColorMode.Monochrome,
    IppSides.TwoSidedLongEdge,
    IppMedia.Collection(
        type = "stationery",
        source = "tray-1"
    )
)
job.logDetails()

// print remote file, make printer pull document from remote server
val remoteFile = URI.create("http://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
ippPrinter.printUri(remoteFile)

// create job and send document
val job = ippPrinter.createJob(jobName(file.name))
job.sendDocument(file)
job.waitForTermination()

// manage jobs
ippPrinter.getJobs().forEach { println(it) }
ippPrinter.getJobs("completed") // which-jobs

val job = ippPrinter.getJob(42)
job.hold()
job.release()
job.cancel()

// admin operations
ippPrinter.basicAuth("admin", "secret")
ippPrinter.pause()
ippPrinter.resume()
ippPrinter.identify("sound")
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
if (response.isSuccessful()) {
    println(response.jobGroup["job-id"])
} else {
    println(response.status)
}
```    
### IppTool

has very limited tag support (only charset, language and uri). If you like this API let me know.
```kotlin
with(IppTool()) {
    uri = URI.create("ipp://colorjet.local/ipp/printer")
    filename = "A4-blank.pdf"
    interpret(
        "OPERATION Print-Job",
        "GROUP operation-attributes-tag",
        "ATTR charset attributes-charset utf-8",
        "ATTR language attributes-natural-language en",
        "ATTR uri printer-uri \$uri",
        "FILE \$filename"
    )
}
```

### CupsClient

```kotlin
// connect to default ipp://localhost:631
val cupsClient = CupsClient()

// list all queues
cupsClient.getPrinters().forEach { 
    println("${it.name} -> ${it.printerUri}")
}

// list all completed jobs for queue
cupsClient.getPrinter("ColorJet_HP")
          .getJobs("completed")
          .forEach { println(it) }

// default printer
val defaultPrinter = cupsClient.getDefault()

// check capabality
if(defaultPrinter.hasCapability(CupsPrinterCapability.CanPrintInColor)) {
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
                
## Packages

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
which requires a http transport that implements interface
[Http.Client](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/http/Http.kt):
[HttpURLConnectionClient](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/http/HttpURLConnectionClient.kt).

## Build

To build `ipp-client.jar` and `ipp-client-fat.jar` into `build/libs` run

    ./gradlew

To install the artifact to your local maven repository run

    ./gradlew publishToMavenLocal

This software has **no dependencies** to
[javax.print](https://docs.oracle.com/javase/7/docs/technotes/guides/jps/),
[CUPS](https://www.cups.org) or
[ipptool](https://www.cups.org/doc/man-ipptool.html).
Operation has mostly been tested for target `jvm`. Android is supported since v1.6.

## Artifacts

[Configure Gradle for use with Github Packages](https://docs.github.com/en/packages/using-github-packages-with-your-projects-ecosystem/configuring-gradle-for-use-with-github-packages).
Use this [github package](https://github.com/gmuth/ipp-client-kotlin/packages/214725/versions) of the project as maven repo.
You can access the repo with any github account. If you prefer to download the jar look for [Release assets](https://github.com/gmuth/ipp-client-kotlin/releases).

```
repositories {
    jcenter()
    maven {
      url = uri("https://maven.pkg.github.com/gmuth/ipp-client-kotlin")
      credentials {
          // configure gpr.user and gpr.token in ~/.gradle/gradle.properties
          // gpr.user=yourname
          // gpr.token=yourtoken
          username = project.findProperty("gpr.user") as String?
          password = project.findProperty("gpr.token") as String?
      }
    }
}
```

Add dependency:

```
    implementation("de.gmuth.ipp:ipp-client-kotlin:1.9")
or  implementation("de.gmuth.ipp:ipp-client-kotlin:2.0-SNAPSHOT")
```

