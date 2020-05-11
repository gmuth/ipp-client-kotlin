# ipp-client-kotlin

A client implementation of the ipp protocol written in kotlin.
[RFC 8010](https://tools.ietf.org/html/rfc8010),
[RFC 8011](https://tools.ietf.org/html/rfc8011)

![Build with Gradle](https://github.com/gmuth/ipp-client-kotlin/workflows/Build%20with%20Gradle/badge.svg)

## Usage

### [IppPrinter](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppPrinter.kt) and [IppJob](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppJob.kt)
```kotlin
with(
  IppPrinter(URI.create("ipp://colorjet.local/ipp/printer"))
) {

  // print file
  val file = File("A4-ten-blank.pdf")
  val job = printJob(file,
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
    IppSides.TwoSidedLongEdge,
    attribute("document-name", IppTag.NameWithoutLanguage, "IPP Implementation Guide"),
    waitForTermination = true
  )
  job.logDetails()

  // print remote file, make printer pull document from remote server
  val remoteFile = URI.create("http://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
  printUri(remoteFile)
  
  // create job and send document
  val job = createJob(jobName(file.name))
  job.sendDocument(file, lastDocument = true)
  job.waitForTermination()

  // manage jobs
  getJobs().forEach { println(it) }
  getJobs("completed") // which-jobs

  val job = getJob(42)
  job.hold()
  job.release()
  job.cancel()

  // admin operations
  httpAuth = Http.Auth("admin", "secret")
  pause()
  resume()
  identify("sound")
}
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
}
val response = ippClient.exchange(uri, request, FileInputStream(file))
response.logDetails()
```    
### IppTool
```kotlin
with(IppTool()) {
    uri = URI.create("ipp://colorjet.local/ipp/printer")
    filename = "A4-blank.pdf"
    run(
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
`IppRequest().readFrom(File("/var/spool/cups/c01579")).logDetails()`

Package
[`de.gmuth.ipp.client`](https://github.com/gmuth/ipp-client-kotlin/tree/master/src/main/kotlin/de/gmuth/ipp/client)
contains the
[IppClient](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppClient.kt)
which requires a http transport that implements interface
[Http.Client](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/http/Http.kt).
Provided implementations are
[HttpClientByHttpURLConnection](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/http/HttpClientByHttpURLConnection.kt)
and
[HttpClientByJava11HttpClient](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/http/HttpClientByJava11HttpClient.kt).
[AnyCertificateX509TrustManager](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/http/AnyCertificateX509TrustManager.kt)
helps connecting to ipps endpoints secured by self signed certificates - e.g. CUPS.

## Build

To build `ipp-client.jar` and `ipp-client-fat.jar` into `build/libs` run

    ./gradlew

This software has **no dependencies** to
[javax.print](https://docs.oracle.com/javase/7/docs/technotes/guides/jps/),
[CUPS](https://www.cups.org) or
[ipptool](https://www.cups.org/doc/man-ipptool.html).
Currently only the target `jvm` is supported. 

## Artifacts

Use [github packages](https://github.com/gmuth/ipp-client-kotlin/packages) as maven repo:

```
<dependency>
  <groupId>de.gmuth.ipp</groupId>
  <artifactId>ipp-client-kotlin</artifactId>
  <version>1.3</version>
</dependency>
```
