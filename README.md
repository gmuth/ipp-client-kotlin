# ipp-client-kotlin

A basic client implementation of the ipp protocol written in kotlin

Road map:
[RFC 8010](https://tools.ietf.org/html/rfc8010),
[RFC 8011](https://tools.ietf.org/html/rfc8011)

## Usage

### [print service API](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/print)

    val uri = URI.create("ipp://colorjet/ipp/printer")
    val file = File("A4-blank.pdf")

    val printService = IppPrintService(uri)
    printService.printFile(file, waitForTermination = true)
    
### ipp operation level API

    val uri = URI.create("ipp://colorjet/ipp/printer")
    val file = File("A4-blank.pdf")

    val ippClient = IppClient()
    val printJob = IppPrintJob(uri, file)
    val job = ippClient.submitPrintJob(uri, printJob)
    ippClient.waitForTermination(job)
    job.logDetails()
    
* [IppPrintJob](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppPrintJob.kt)
* [IppPrintUri](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppPrintUri.kt) - ask printer to pull a document - nice feature if supported by printer
* [IppGetJobAttributes](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/client/IppGetJobAttributes.kt)
    
### ipp message level API

    val uri = URI.create("ipp://colorjet/ipp/printer")
    val file = File("A4-blank.pdf")
    
    val ippClient = IppClient()
    val request = IppRequest(IppOperation.PrintJob).apply {
      operationGroup.attribute("printer-uri", uri)
      operationGroup.attribute("document-format", "application/octet-stream")
      operationGroup.attribute("requesting-user-name", "kotlin-ipp")
    }
    val response = ippClient.exchange(uri, request, FileInputStream(file))

### ipp tool API
 
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

**Encodings**

 * `no-value` 
 * `integer`, `enum`
 * `textWithoutLanguage`, `nameWithoutLanguage`, `uri`, `uriScheme`, `charset`, `naturalLanguage`,
   `mimeMediaType`, `keyword`

**Model and semantics**

* [IppOperation](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppOperation.kt),
  [IppStatus](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppStatus.kt)
* [IppJob](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppJob.kt),
  [IppJobState](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppJobState.kt)

## Support

[Open a github issue](https://github.com/gmuth/ipp-client-kotlin/issues/new/choose) or contact me.
Commercial support is [available](http://ipp-software.com).

## ToDo

* implement get-printer-attributes
* support more encodings
* multi platform support