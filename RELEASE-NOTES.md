3.5
---

* Introduced release notes
* Updated IANA registration files section 2,4 and 6 (PWG5100.18, PWG5100.TRUSTNOONE, ...)
* Added encryption related operation codes to enum `IppOperaton`
* Enabled correct grouping for encryption related operation attributes 
* Convenient logging config: `ippClient.onExchangeLogRequestAndResponseWithLevel`
* Support for NAT and reverse proxy scenarios: `ippClient.onExchangeOverrideRequestPrinterOrJobUri`
* Support printer firmware attributes: `ippPrinter.printerFirmware`
* Fixed issue where unknown (not IANA registered) attributes could not be added to a request

Previous Versions
-----------------

* See [published releases](https://github.com/gmuth/ipp-client-kotlin/releases) and associated notes.
