# ipp-client-kotlin

A client implementation of the ipp protocol written in kotlin.

[RFC 8010](https://tools.ietf.org/html/rfc8010),
[RFC 8011](https://tools.ietf.org/html/rfc8011)

## Usage

### [IppPrintService](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppPrintService.kt)

    val printer = URI.create("ipp://colorjet/ipp/printer")
    val file = File("A4-blank.pdf")
    val remoteFile = URI.create("http://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
 
    val printService = IppPrintService(printer)
    
    printService.printFile(file)
    printService.printFile(file, waitForTermination = true)
    printService.printFile(file, IppColorMode.Monochrome)
    printService.printUri(remoteFile) // -- make printer pull document from remote server

    printService.getJobs()
    printService.getJob(345)
    printService.cancelJob(345)
    
    printService.pausePrinter()
    printService.resumePrinter()

### exchange [IppRequest](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppRequest.kt) for [IppResponse](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppResponse.kt)

    val printer = URI.create("ipp://colorjet/ipp/printer")
    val file = File("A4-blank.pdf")
    
    val ippClient = IppClient()
    val request = IppRequest(IppOperation.PrintJob).apply {
      operationGroup.attribute("printer-uri", printer)
      operationGroup.attribute("document-format", "application/octet-stream")
      operationGroup.attribute("requesting-user-name", "kotlin-ipp")
    }
    val response = ippClient.exchange(printer, request, FileInputStream(file))

### IppTool
 
    with(IppTool()) {
        uri = URI.create("ipp://colorjet/ipp/printer")
        val filename = "A4-blank.pdf"
        
        run(
            "OPERATION Print-Job",
            "GROUP operation-attributes-tag",
            "ATTR charset attributes-charset utf-8",
            "ATTR naturalLanguage attributes-natural-language en",
            "ATTR uri printer-uri $uri",
            "FILE $filename"
        )
    }

### Use basic auth with invalid SSL certificates

    val printer = URI.create("ipps://pi.local/printers/Lexmark_E210")
    
    with(IppPrintService(printer)) {
        // trust cups self-signed certs
        ippClient.httpClient.config.disableSSLCertificateValidation = true
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

This project is work in progress.

**Model and semantics**

* [IppOperation](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppOperation.kt),
  [IppStatus](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppStatus.kt),
  [IppJobState](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppJobState.kt)
* [IppPrinter](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppPrinter.kt),
  [IppJob](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppJob.kt)

## Support

[Open a github issue](https://github.com/gmuth/ipp-client-kotlin/issues/new/choose) or contact me.
Commercial support is [available](http://ipp-software.com).

## ToDo

* support collection encoding
* multi platform support