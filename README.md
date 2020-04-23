# ipp-client-kotlin

A client implementation of the ipp protocol written in kotlin.

[RFC 8010](https://tools.ietf.org/html/rfc8010),
[RFC 8011](https://tools.ietf.org/html/rfc8011)

## Usage

### [IppPrintService](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppPrintService.kt)

    with(
      IppPrintService(URI.create("ipp://colorjet.local/ipp/printer"))
    ) {
    
      val file = File("A4-blank.pdf")
      printFile(file)
      printFile(file, waitForTermination = true).logDetails()
      printFile(file, IppCopies(2))
      printFile(file, IppPageRanges(2..3, 8..10))
      printFile(file, IppMonochrome(), IppDuplex())
      printFile(file, IppColorMode.Monochrome, IppSides.TwoSidedLongEdge)
                  
      val remoteFile = URI.create("http://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
      printUri(remoteFile) // -- make printer pull document from remote server

      getJobs()
      getJobs("completed") // which-jobs
      getJob(345)
      cancelJob(345)
    
      identifyPrinter("sound")
      pausePrinter()
      resumePrinter()
    }

### exchange [IppRequest](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppRequest.kt) for [IppResponse](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppResponse.kt)

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
    
### IppTool
 
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

### Use basic auth

    val printer = URI.create("ipps://pi.local/printers/Lexmark_E210")
    
    with(IppPrintService(printer)) {
        httpAuth = Http.Auth("admin", "secret")
        pausePrinter()    
    }
          
## Build

To build `ippclient.jar` into `build/libs` run

    ./gradlew

This software has **no dependencies** to
[javax.print](https://docs.oracle.com/javase/7/docs/technotes/guides/jps/),
[CUPS](https://www.cups.org) or
[ipptool](https://www.cups.org/doc/man-ipptool.html).
Currently only the target `jvm` is supported. 

## Status

[Version 1.0](https://github.com/gmuth/ipp-client-kotlin/releases/tag/v1.0)
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
For convenience some common operations are implemented:
 - `getPrinterAttributes(printerUri)`
 - `getJobAttributes(printerUri, jobId)`
 - `getJobAttributes(jobUri)`
 - `cancelJob(printerUri, jobId)`
 - `cancelJob(jobUri)`
 - `getJob(whichJobs)`

Operation `printFile()` in implemented by
[IppPrintService](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppPrintService.kt)
using model
[IppPrinter](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppPrinter.kt)
and
[IppJob](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppJob.kt). 
## Support

[Open a github issue](https://github.com/gmuth/ipp-client-kotlin/issues/new/choose) or contact me.
