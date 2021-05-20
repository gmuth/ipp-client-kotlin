package de.gmuth.io

fun ByteArray.hexdump(maxRows: Int = 32, dumpString: (String) -> Unit) {
    val line = StringBuilder()
    for ((index, b) in withIndex()) {
        line.append("%02X ".format(b))
        if ((index + 1) % maxRows == 0) {
            dumpString(line.toString())
            line.clear()
        }
    }
    if (line.isNotEmpty()) {
        dumpString(line.toString())
    }
}