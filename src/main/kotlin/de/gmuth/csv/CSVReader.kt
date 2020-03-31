package de.gmuth.csv

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.InputStream

// https://tools.ietf.org/html/rfc4180

open class CSVReader<T> {

    interface RowMapper<T> {
        fun mapRow(columns: List<String>, rowNum: Int): T
    }

    fun parse(inputStream: InputStream, skipHeader: Boolean, rowMapper: RowMapper<T>): List<T> {
        val mappedRows = mutableListOf<T>()
        if (skipHeader) parseLine(inputStream)
        var rowNum = 0
        lineLoop@ while (true) {
            val columns = parseLine(inputStream) ?: break@lineLoop
            val row = rowMapper.mapRow(columns, ++rowNum)
            mappedRows.add(row)
        }
        return mappedRows
    }

    private fun parseLine(inputStream: InputStream): List<String>? {
        val fields = mutableListOf<String>()
        var currentField = StringBuffer()
        var inQuote = false
        var lastCharIsQuote = false
        columnLoop@ while (true) {
            val i = inputStream.read()
            if (i == -1) break@columnLoop
            val c = i.toChar()
            var append = false
            if (inQuote) {
                append = c != '"'
            } else {
                when (c) {
                    ',', '\n' -> {
                        fields.add(currentField.toString())
                        if (c == '\n') return fields
                        else currentField = StringBuffer()
                    }
                    '"' -> append = lastCharIsQuote
                    else -> append = c != '\r'
                }
            }
            lastCharIsQuote = c == '"'
            if (lastCharIsQuote) inQuote = !inQuote
            if (append) currentField.append(c)
        }
        if (currentField.isEmpty()) return null
        fields.add(currentField.toString())
        return fields
    }

}