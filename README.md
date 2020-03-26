# ipp-client-kotlin

A basic client implementation of the ipp protocol written in kotlin

## Usage

### IppClient API

    val uri = URI.create("ipp://colorjet:631/ipp/printer")
    val file = File("A4-blank.pdf")
    
    val ippJob = IppClient(uri).printDocument(FileInputStream(file))

### IppMessage API

    val uri = URI.create("ipp://colorjet:631/ipp/printer")
    val file = File("A4-blank.pdf")

    val ippRequest = IppRequest(IppOperation.PrintJob).apply {
      addOperationAttribute("printer-uri", "$uri")
      addOperationAttribute("document-format", "application/octet-stream")
      addOperationAttribute("requesting-user-name", "kotlin-ipp")
    }

    val ippResponse = exchangeIpp(ippRequest, FileInputStream(file))
    println(ippResponse.status)
        
### IppTool API
 
    with(IppTool()) {
        uri = URI.create("ipp://colorjet:631/ipp/printer")
        val filename = "A4-blank.pdf"
        
        run(
            "OPERATION Print-Job",
            "GROUP operation-attributes-tag",
            "ATTR charset attributes-charset utf-8",
            "ATTR language attributes-natural-language en",
            "ATTR uri printer-uri $uri",
            "FILE $filename"
        )
    }
          
## Build

To build `ippclient.jar` into `build/libs` run

    ./gradlew

No dependencies to CUPS or ipptool exist. Currently only the target `jvm` is supported. 


## Status

This project is work in progress.

**Encodings**

 * `integer`, `enum`
 * `textWithoutLanguage`, `nameWithoutLanguage`, `uri`, `charset`, `naturalLanguage`, `mimeMediaType`

**Model and semantics**

* [IppOperation](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppOperation.kt),
  [IppStatus](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppStatus.kt)
* [IppJobState](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppJobState.kt),
  [IppJob](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppJob.kt)

## ToDo

* read types from iana registrations file
* support more encodings
* multi platform support
