# ipp-client-kotlin

A basic client implementation of the ipp protocol written in kotlin

## Status

This project is work in progress.

**Supported encodings**

 * `integer`, `enum`
 * `textWithoutLanguage`, `uri`, `charset`, `naturalLanguage`, `mimeMediaType`

**Supported models**

* [IppOperation](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppOperation.kt),
  [IppStatus](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppStatus.kt)
* [IppJobState](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/core/IppJobState.kt)
 
## Operations

### [Print-Job](https://github.com/gmuth/ipp-client-kotlin/blob/master/src/main/kotlin/de/gmuth/ipp/IppPrintJobOperation.kt)

Print a document

    val printerUri = URI.create("ipp://colorjet:631/ipp/printer")
    val file = File("A4-blank.pdf")

    IppMessage.verbose = true
    IppClient(printerUri).printDocument(FileInputStream(file))

Output

    send Print-Job request to ipp://colorjet:631/ipp/printer
    version = 1.1
    status-code = successful-ok
    request-id = 1
    operation group
      attributes-charset (charset) = us-ascii
      attributes-natural-language (naturalLanguage) = en
    job group
      job-uri (uri) = ipp://colorjet:631/jobs/352
      job-id (integer) = 352
      job-state (enum) = pending
      job-state-reasons (keyword) = none
      
## Build

To build `ippclient.jar` into `build/libs` you need an installed JDK.

    ./gradlew
