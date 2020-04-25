# ipp-client-kotlin

A client implementation of the ipp protocol written in kotlin.

[RFC 8010](https://tools.ietf.org/html/rfc8010),
[RFC 8011](https://tools.ietf.org/html/rfc8011)

## Usage

### [IppPrintService](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppPrintService.kt)
```kotlin
with(
  IppPrintService(URI.create("ipp://colorjet.local/ipp/printer"))
) {

  // print file
  val file = File("A4-blank.pdf")
  printFile(file).logDetails()
  printFile(file, waitForTermination = true)
  printFile(file, IppCopies(2))
  printFile(file, IppPageRanges(2..3, 8..10))
  printFile(file, IppMonochrome(), IppDuplex())
  printFile(file, IppColorMode.Monochrome, IppSides.TwoSidedLongEdge)
              
  // print remote file,make printer pull document from remote server
  val remoteFile = URI.create("http://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
  printUri(remoteFile)

  // manage jobs
  getJobs()
  getJobs("completed") // which-jobs
  getJob(42).logDetails()
  holdJob(42)
  releaseJob(42)
  cancelJob(42)

  // get attributes
  getPrinterAttributes()
  getJobAttributes(42)

  // admin operations
  httpAuth = Http.Auth("admin", "secret")
  identifyPrinter("sound")
  pausePrinter()
  resumePrinter()
}
```
### exchange [IppRequest](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppRequest.kt) for [IppResponse](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppResponse.kt)
```kotlin
val uri = URI.create("ipp://colorjet.local/ipp/printer")
val file = File("A4-blank.pdf")

val ippClient = IppClient()
val request = IppRequest(IppOperation.PrintJob).apply {
  operationGroup.attribute("printer-uri", uri)
  operationGroup.attribute("document-format", "application/pdf")
  operationGroup.attribute("requesting-user-name", "gmuth")
}
val response = ippClient.exchange(uri, request, FileInputStream(file))
response.logDetails()
```    
### IppTool
```kotlin
with(IppTool()) {
    uri = URI.create("ipp://colorjet.local/ipp/printer")
    val filename = "A4-blank.pdf"
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
## Status

[Version 1.1](https://github.com/gmuth/ipp-client-kotlin/releases/tag/v1.1)
has been released April 2020.
The package
[`de.gmuth.ipp.core`](https://github.com/gmuth/ipp-client-kotlin/tree/master/src/main/kotlin/de/gmuth/ipp/core)
contains the usual
[encoding](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppOutputStream.kt)
and
[decoding](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppInputStream.kt)
operations. RFC 8010 is fully supported.

Example to decode a cups spool file: 
`IppRequest().readFrom(File("/var/spool/cups/c01579")).logDetails()`

## IppClient

[IppClient](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppClient.kt)
requires a http transport that implements interface
[Http.Client](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/http/Http.kt).
Provided implementations are
[HttpClientByHttpURLConnection](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/http/HttpClientByHttpURLConnection.kt)
and
[HttpClientByJava11HttpClient](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/http/HttpClientByJava11HttpClient.kt).
[SSLUtil](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/http/SSLUtil.kt)
helps connecting to endpoints secured by self signed certificates - e.g. CUPS.

Operation `printFile()` is implemented by
[IppPrintService](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppPrintService.kt)
using model
[IppPrinter](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppPrinter.kt)
and
[IppJob](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppJob.kt). 
## Build

To build `ippclient.jar` into `build/libs` run

    ./gradlew

This software has **no dependencies** to
[javax.print](https://docs.oracle.com/javase/7/docs/technotes/guides/jps/),
[CUPS](https://www.cups.org) or
[ipptool](https://www.cups.org/doc/man-ipptool.html).
Currently only the target `jvm` is supported. 
