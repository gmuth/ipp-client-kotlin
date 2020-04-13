package de.gmuth.csv

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.toPluralString
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import kotlin.math.log10

// https://tools.ietf.org/html/rfc4180

class CSVReader<T>(
        private val rowMapper: RowMapper<T>,
        var verbose: Boolean = false
) {

    interface RowMapper<T> {
        fun mapRow(columns: List<String>, rowNum: Int): T
    }

    fun readResource(resource: String, skipHeader: Boolean): List<T> {
        return read(javaClass.getResourceAsStream(resource), skipHeader)
    }

    fun read(inputStream: InputStream, skipHeader: Boolean): List<T> {
        val mappedRows = mutableListOf<T>()
        if (skipHeader) parseLine(inputStream)
        var rowNum = 0
        lineLoop@ while (true) {
            val columns = parseLine(inputStream) ?: break@lineLoop
            val row = rowMapper.mapRow(columns, ++rowNum)
            mappedRows.add(row)
        }
        if (verbose) {
            println("mapped ${mappedRows.size.toPluralString("row")}")
        }
        return mappedRows
    }

    private fun parseLine(inputStream: InputStream): List<String>? {
        val fields = mutableListOf<String>()
        var currentField = StringBuffer()
        var inQuote = false
        var lastCharacterWasQuote = false
        columnLoop@ while (true) {
            val i = inputStream.read()
            if (i == -1) break@columnLoop
            val c = i.toChar()
            var appendCharacter = false
            if (inQuote) {
                appendCharacter = c != '"'
            } else {
                when (c) {
                    ',', '\n' -> {
                        fields.add(currentField.toString())
                        if (c == '\n') return fields
                        else currentField = StringBuffer()
                    }
                    '"' -> appendCharacter = lastCharacterWasQuote
                    else -> appendCharacter = c != '\r'
                }
            }
            lastCharacterWasQuote = c == '"'
            if (lastCharacterWasQuote) inQuote = !inQuote
            if (appendCharacter) currentField.append(c)
        }
        if (currentField.isEmpty()) return null
        fields.add(currentField.toString())
        return fields
    }

    // --- Utility for pretty printing ---

    companion object {

        fun prettyPrintResource(resource: String) {
            val rows = readRowsFromResource(resource)
            prettyPrint(rows)
        }

        fun readRowsFromResource(resource: String): List<List<String>> {
            return readRowsFromInputStream(CSVReader::class.java.getResourceAsStream(resource))
        }

        fun readRowsFromInputStream(inputStream: InputStream): List<List<String>> {
            val csvReader = CSVReader(
                    object : RowMapper<List<String>> {
                        override fun mapRow(columns: List<String>, rowNum: Int): List<String> = columns
                    }
            )
            return csvReader.read(inputStream, false)
        }

        fun prettyPrint(
                rows: List<List<String>>,
                withRowNumber: Boolean = true,
                delimiter: Char = '|',
                outputStream: OutputStream = System.out
        ) {
            // iterate over all fields and find the max column widths
            val maxLengthMap = mutableMapOf<Int, Int>()
            for (row in rows) {
                for ((columnNum, column) in row.withIndex()) {
                    with(maxLengthMap[columnNum]) {
                        if (this == null || this < column.length) maxLengthMap[columnNum] = column.length
                    }
                }
            }
            fun maxLength(column: Int) = maxLengthMap[column] ?: throw IllegalArgumentException("column $column not found")

            // iterate over all fields and layout with max column widths
            val printWriter = PrintWriter(outputStream, true, Charsets.UTF_8)
            val linesColumnLength: Int = (log10((rows).size.toDouble()) + 1).toInt()
            for ((rowNo, columns) in (rows).withIndex()) {
                val line = StringBuffer()
                if (withRowNumber) line.append(String.format("#%0${linesColumnLength}d%c", rowNo + 1, delimiter))
                else line.append(delimiter)
                for ((columnNum, column) in columns.withIndex()) {
                    line.append(String.format("%-${maxLength(columnNum)}s%c", column, delimiter))
                }
                printWriter.println(line.toString())
            }
        }

    }
}