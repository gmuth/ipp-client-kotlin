package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppAttributesGroup(val tag: IppTag) : LinkedHashMap<String, IppAttribute<*>>() {

    companion object {
        var checkValueSupported: Boolean = true
    }

    init {
        if (!tag.isDelimiterTag()) {
            throw IppException("'$tag' is not a delimiter tag")
        }
    }

    fun put(attribute: IppAttribute<*>): IppAttribute<*>? {
        if (!attribute.tag.isOutOfBandTag() && attribute.values.isEmpty()) {
            throw IppException("put attribute rejected: value list is empty")
        }
        val replaced = put(attribute.name, attribute)
        if (replaced != null) {
            println("WARN: replaced '$replaced' with '$attribute'")
        }
        return replaced
    }

    private fun attribute(name: String, tag: IppTag, values: List<*>) =
            put(IppAttribute(name, tag, values.toMutableList()))

    fun attribute(name: String, tag: IppTag, vararg values: Any?) =
            put(IppAttribute(name, tag, values.toMutableList()))

    fun attribute(name: String, vararg values: Any?) =
            put(IppAttribute(name, values.toMutableList()))

    @Suppress("UNCHECKED_CAST")
    fun <T> getValue(name: String) = get(name)?.value as T

    @Suppress("UNCHECKED_CAST")
    fun <T> getValues(name: String) = get(name)?.values as T

    fun checkValueSupported(supportedAttributeName: String, value: Any) {
        if (this.tag != IppTag.Printer) {
            throw IppException("'...-supported' attribute values can only be found in printer attributes group")
        }
        if (!supportedAttributeName.endsWith("-supported")) {
            throw IppException("expected attribute name ending with '-supported' but got: '$supportedAttributeName'")
        }
        val supportedAttribute = get(supportedAttributeName)
        if (supportedAttribute == null || !checkValueSupported) {
            return
        }
        with(supportedAttribute) {
            val valueIsSupported = when (tag) {
                IppTag.MimeMediaType,
                IppTag.Keyword,
                IppTag.Enum,
                IppTag.Integer,
                IppTag.Resolution -> {
                    values.contains(value)
                }
                IppTag.RangeOfInteger -> with(this.value as IppIntegerRange) {
                    value is Int && value in IntRange(start, end)
                }
                else -> {
                    println("WARN: unable to check if value '$value' is supported by $this")
                    true
                }
            }
            if (!valueIsSupported) {
                println("ERROR: supported values: ${values.joinToString(",")}")
                throw IppException("'${valueOrEnumValueName(value)}' not supported by printer. $this")
            }
        }
    }

    override fun toString() = "IppAttributesGroup '$tag' containing ${size.toPluralString("attribute")}"

    fun logDetails(prefix: String = "") {
        println("${prefix}$tag")
        for (key in keys) {
            println("$prefix  ${get(key)}")
        }
    }

}