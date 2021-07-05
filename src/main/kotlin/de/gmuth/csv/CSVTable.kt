package de.gmuth.csv

/**
 * Copyright (c) 2021 Gerhard Muth
 */

import java.io.InputStream

// https://tools.ietf.org/html/rfc4180

class CSVTable<T>(
        inputStream: InputStream? = null,
        val buildRow: (columns: List<String>) -> T
) {

    val rows: MutableList<T> = mutableListOf()
    val maxLengthMap = mutableMapOf<Int, Int>()

    init {
        inputStream?.let { read(it, true) }
    }

    constructor(resourcePath: String, rowFactory: (columns: List<String>) -> T) :
            this(CSVTable::class.java.getResourceAsStream(resourcePath), rowFactory)

    fun updateMaxLengthMap(columnIndex: Int, columnLength: Int) =
            with(maxLengthMap[columnIndex]) { if (this == null || this < columnLength) maxLengthMap[columnIndex] = columnLength }

    fun read(inputStream: InputStream, skipHeader: Boolean) {
        if (skipHeader) parseRow(inputStream)
        lineLoop@ while (true) {
            val columns = parseRow(inputStream) ?: break@lineLoop
            rows.add(buildRow(columns))
        }
    }

    fun parseRow(inputStream: InputStream): List<String>? {
        val fields = mutableListOf<String>()
        var currentField = StringBuffer()
        var inQuote = false
        var lastCharacterWasQuote = false
        fun updateMaxLengthMap() = updateMaxLengthMap(fields.size - 1, fields.last().length)
        columnLoop@ while (true) {
            val i = inputStream.read()
            if (i == -1) break@columnLoop
            val char = i.toChar()
            var appendCharacter = false
            if (inQuote) {
                appendCharacter = char != '"'
            } else {
                when (char) {
                    ',' -> {
                        fields.add(currentField.toString())
                        updateMaxLengthMap()
                        currentField = StringBuffer()
                    }
                    '\n' -> {
                        fields.add(currentField.toString())
                        updateMaxLengthMap()
                        return fields
                    }
                    '"' -> {
                        appendCharacter = lastCharacterWasQuote
                    }
                    else -> {
                        appendCharacter = char != '\r'
                    }
                }
            }
            lastCharacterWasQuote = char == '"'
            if (lastCharacterWasQuote) inQuote = !inQuote
            if (appendCharacter) currentField.append(char)
        }
        if (currentField.isEmpty()) return null
        fields.add(currentField.toString())
        updateMaxLengthMap()
        return fields
    }

    companion object {

        fun print(inputStream: InputStream, delimiter: String = "|") =
                with(CSVTable(inputStream, buildRow = { columns: List<String> -> columns })) {
                    for (columns in rows) with(StringBuffer(delimiter)) {
                        for ((columnIndex, column) in columns.withIndex()) {
                            append("%-${maxLengthMap[columnIndex]}s%s".format(column, delimiter))
                        }
                        println(toString())
                    }
                }

        fun print(resourcePath: String, delimiter: String = "|") =
                print(CSVTable::class.java.getResourceAsStream(resourcePath), delimiter)

    }
}