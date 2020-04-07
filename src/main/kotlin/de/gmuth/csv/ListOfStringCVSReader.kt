package de.gmuth.csv

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import kotlin.math.log10

class ListOfStringCVSReader : CSVReader<List<String>>() {

    private var rows: List<List<String>>? = null

    fun parse(inputStream: InputStream, skipHeader: Boolean = false): List<List<String>> {
        rows = super.parse(
                inputStream,
                skipHeader,
                object : RowMapper<List<String>> {
                    override fun mapRow(columns: List<String>, rowNum: Int): List<String> = columns
                }
        )
        return rows as List<List<String>>
    }

    fun prettyPrint(outputStream: OutputStream, withRowNumber: Boolean = true, delimiter: Char = '|') {
        if (rows == null) return

        // iterate over all fields and find the max column widths
        val maxLengthMap = mutableMapOf<Int, Int>()
        for (row in rows as List<List<String>>) {
            for ((columnNum, column) in row.withIndex()) {
                with(maxLengthMap[columnNum]) {
                    if (this == null || this < column.length) maxLengthMap[columnNum] = column.length
                }
            }
        }
        fun maxLength(column: Int) = maxLengthMap[column] ?: throw IllegalArgumentException("column $column not found")

        // iterate over all fields and layout with max column widths
        val printWriter = PrintWriter(outputStream, false, Charsets.UTF_8)
        val linesColumnLength: Int = (log10((rows as List<List<String>>).size.toDouble()) + 1).toInt()
        for ((rowNo, columns) in (rows as List<List<String>>).withIndex()) {
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