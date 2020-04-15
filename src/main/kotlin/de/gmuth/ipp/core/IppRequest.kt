package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppRequest() : IppMessage() {

    override val codeDescription: String
        get() = "operation = $operation"

    val operation: IppOperation
        get() = IppOperation.fromCode(code ?: throw IppException("operation-code must not be null"))

    val operationGroup = attributesGroup(IppTag.Operation)

    constructor(
            version: IppVersion,
            operation: IppOperation,
            requestId: Int,
            naturalLanguage: String = "en",
            requestingUserName: String? = System.getenv("USER")
    ) : this() {
        this.version = version
        this.code = operation.code
        this.requestId = requestId
        operationGroup.attribute("attributes-charset", IppTag.Charset, Charsets.UTF_8.name().toLowerCase())
        operationGroup.attribute("attributes-natural-language", IppTag.NaturalLanguage, naturalLanguage)
        if (requestingUserName != null) {
            operationGroup.attribute("requesting-user-name", IppTag.NameWithoutLanguage, requestingUserName)
        }
    }

}