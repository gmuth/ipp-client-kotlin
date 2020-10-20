# ipp-client-kotlin

A client implementation of the ipp protocol written in kotlin.
[RFC 8010](https://tools.ietf.org/html/rfc8010),
[RFC 8011](https://tools.ietf.org/html/rfc8011)

![Build with Gradle](https://github.com/gmuth/ipp-client-kotlin/workflows/Build/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=gmuth_ipp-client-kotlin&metric=alert_status)](https://sonarcloud.io/dashboard?id=gmuth_ipp-client-kotlin)
## Usage

You need to know the correct printerUri. You may use ```ippfind``` or other ZeroConf tools for discovery.
### [IppPrinter](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppPrinter.kt) and [IppJob](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppJob.kt)
```kotlin
// initialize printer connection and show printer attributes
val ippPrinter = IppPrinter(URI.create("ipp://colorjet.local/ipp/printer"))
ippPrinter.attributes.logDetails()

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
    printerResolutionDpi(300),
    IppPrintQuality.High,
    IppColorMode.Monochrome,
    IppSides.TwoSidedLongEdge
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
ippPrinter.httpBasicAuth = Http.BasicAuth("admin", "secret")
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
val request = ippClient.ippRequest(IppOperation.PrintJob).apply {
  operationGroup.attribute("printer-uri", IppTag.Uri, uri)
  operationGroup.attribute("document-format", IppTag.MimeMediaType, "application/pdf")
  operationGroup.attribute("requesting-user-name", IppTag.NameWithoutLanguage, "gmuth")
  documentInputStream  = FileInputStream(file)
}
val response = ippClient.exchange(uri, request)
response.logDetails()
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

This software has **no dependencies** to
[javax.print](https://docs.oracle.com/javase/7/docs/technotes/guides/jps/),
[CUPS](https://www.cups.org) or
[ipptool](https://www.cups.org/doc/man-ipptool.html).
Operation has mostly been tested for target `jvm`. Android is supported since v1.6.

## Artifacts

[Configure Gradle for use with Github Packages](https://docs.github.com/en/packages/using-github-packages-with-your-projects-ecosystem/configuring-gradle-for-use-with-github-packages).
Use this [github package](https://github.com/gmuth/ipp-client-kotlin/packages/214725/versions) of the project as maven repo.
You can access the repo with any github account.

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
implementation("de.gmuth.ipp:ipp-client-kotlin:1.7")
```
