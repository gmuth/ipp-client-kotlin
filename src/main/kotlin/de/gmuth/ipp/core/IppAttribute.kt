package de.gmuth.ipp.core

import de.gmuth.ipp.iana.IppRegistrations
import de.gmuth.ipp.iana.IppRegistrationsSection6

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppAttribute<T> constructor(val name: String, val tag: IppTag) {

    val values = mutableListOf<T>()

    init {
        if (tag.isGroupTag()) {
            throw IppException("group tag '$tag' must not be used for attributes")
        }
    }

    companion object {
        var allowAutomaticTag: Boolean = true
    }

    constructor(name: String, tag: IppTag, vararg values: T) : this(name, tag, values.toList())

    constructor(name: String, tag: IppTag, values: Collection<T>) : this(name, tag) {
        if (values.size > 1 || values.first() != null) {
            this.values.addAll(values)
        }
    }

    // ------ use automatic tag

    constructor(name: String, vararg values: T) : this(name, values.toList())

    constructor(name: String, values: Collection<T>) : this(name, IppRegistrations.tagForAttribute(name), values) {
        if (!allowAutomaticTag) {
            throw IppException("for '$name' use IppTag.${tag.name}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun additionalValue(attribute: IppAttribute<*>) {
        if (tag == attribute.tag) {
            values.add(attribute.value as T)
        } else {
            throw IppSpecViolation("'$name' 1setOf error: expected tag '$tag' for additional value but found '${attribute.tag}'")
        }
    }

    fun is1setOf() = values.size > 1
            || IppRegistrations.attributeNameIsRegistered(name) && IppRegistrations.attributeIs1setOf(name)

    val value: T
        get() =
            if (values.size == 1) values.first()
            else throw IppException("found ${values.size.toPluralString("value")} but expected only 1 for '$name'")

    private fun valueOrEnumValueName(value: Any?): Any? =
            if (tag == IppTag.Enum) {
                IppRegistrationsSection6.getEnumValueName(name, value!!)
            } else {
                value
            }

    override fun toString(): String {
        val valuesString = if (values.isEmpty()) "no-value" else try {
            values.joinToString(",") {
                valueOrEnumValueName(it as Any?).toString()
            }
        } catch (exception: Exception) {
            "<${exception.message}>"
        }
        return String.format("%s (%s) = %s", name, if (is1setOf()) "1setOf $tag" else "$tag", valuesString)
    }

    fun logDetails() {
        if (values.size == 1) {
            println(toString())
        } else {
            println("$name ($tag) =")
            for (value in values) {
                println(" $value")
            }
        }
    }

}