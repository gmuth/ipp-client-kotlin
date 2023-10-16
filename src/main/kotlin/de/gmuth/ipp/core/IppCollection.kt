package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import java.util.logging.Level
import java.util.logging.Level.INFO
import java.util.logging.Logger

// RFC8010 3.1.6.
data class IppCollection(val members: MutableCollection<IppAttribute<*>> = mutableListOf()) {

    constructor(vararg attributes: IppAttribute<*>) : this(attributes.toMutableList())

    fun addAttribute(name: String, tag: IppTag, vararg values: Any) =
        add(IppAttribute(name, tag, values.toMutableList()))

    fun add(attribute: IppAttribute<*>) =
        members.add(attribute)

    fun addAll(attributes: Collection<IppAttribute<*>>) =
        members.addAll(attributes)

    @Suppress("UNCHECKED_CAST")
    fun <T> getMember(memberName: String) =
        members.single { it.name == memberName } as IppAttribute<T>

    val size: Int
        get() = members.size

    override fun toString() = members.joinToString(" ", "{", "}") {
        "${it.name}=${it.values.joinToString(",")}"
    }

    fun log(logger: Logger, level: Level = INFO, prefix: String = "") {
        val string = toString()
        if (string.length < 160) logger.log(level) { "$prefix$string" }
        else members.forEach { member -> member.log(logger, level, prefix) }
    }

}