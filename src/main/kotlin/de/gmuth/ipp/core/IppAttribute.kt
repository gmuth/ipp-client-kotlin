package de.gmuth.ipp.core

import de.gmuth.ipp.iana.IppRegistrationsSection2
import de.gmuth.ipp.iana.IppRegistrationsSection6

/**
 * Copyright (c) 2020 Gerhard Muth
 */

open class IppAttribute<T> constructor(val name: String, val tag: IppTag) : IppAttributeHolder {

    val values = mutableListOf<T>()

    init {
        if (tag.isDelimiterTag()) {
            throw IppException("delimiter tag '$tag' must not be used for attributes")
        }
    }

    companion object {
        var allowAutomaticTag: Boolean = true
    }

    constructor(name: String, tag: IppTag, vararg values: T) : this(name, tag, values.toList())

    constructor(name: String, tag: IppTag, values: Collection<T>) : this(name, tag) {
        // do not add null to values
        if (values.size > 1 || values.size == 1 && values.first() != null) {
            this.values.addAll(values)
        }
    }

    // automatic tag

    constructor(name: String, vararg values: T) : this(name, values.toList())

    constructor(name: String, values: Collection<T>) : this(
            name,
            IppRegistrationsSection2.tagForAttribute(name) ?: throw IppException("no tag found for attribute '$name'"),
            values
    ) {
        if (!allowAutomaticTag) {
            throw IppException("automatic tag disabled: for attribute '$name' use IppTag.${tag.name}")
        }
    }

    override fun getIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*> {
        for (value in values) {
            if (tag in listOf(IppTag.MimeMediaType, IppTag.Keyword, IppTag.Enum)) {
                printerAttributes.checkValueSupported("$name-supported", value as Any)
            }
        }
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun additionalValue(attribute: IppAttribute<*>) {
        when {
            attribute.name.isNotEmpty() -> throw IppException("name must be empty for additional values")
            tag == attribute.tag -> values.add(attribute.value as T)
            else -> throw IppException("'$name' 1setOf error: expected tag '$tag' for additional value but found '${attribute.tag}'")
        }
    }

    fun is1setOf() = values.size > 1 || IppRegistrationsSection2.attributeIs1setOf(name) == true

    val value: T?
        get() {
            if (IppRegistrationsSection2.attributeIs1setOf(name) == true) {
                println("WARN: '$name' is registered as '1setOf', use 'values' or 'getValues' instead")
            }
            if (values.size > 1) {
                throw IppException("found ${values.size.toPluralString("value")} but expected 0 or 1 for '$name'")
            }
            return values.firstOrNull()
        }

    fun valueOrEnumValueName(value: Any?): Any? =
            if (tag == IppTag.Enum) {
                IppRegistrationsSection6.getEnumValueName(name, value!!)
            } else {
                value
            }

    override fun toString(): String {
        val tagString = "${if (is1setOf()) "1setOf " else ""}$tag"
        val valuesString = if (values.isEmpty()) "no-value" else
            try {
                values.joinToString(",") { valueOrEnumValueName(it as Any?).toString() }
            } catch (exception: Exception) {
                "<${exception.message}>"
            }
        return "$name ($tagString) = $valuesString"
    }

    fun logDetails(prefix: String = "") {
        val string = toString()
        if (string.length < 160) {
            println("$prefix$string")
        } else {
            println("$prefix$name ($tag) =")
            for (value in values) {
                if (value is IppCollection) {
                    value.logDetails("$prefix  ")
                } else {
                    println("${prefix}  ${valueOrEnumValueName(value)}")
                }
            }
        }
    }

}