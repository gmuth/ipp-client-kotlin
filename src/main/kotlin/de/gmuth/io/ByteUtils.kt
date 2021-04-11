package de.gmuth.io

class ByteUtils {
    companion object {
        fun hexdump(bytes: ByteArray, maxRows: Int = 32, dumpString: (String) -> Unit) {
            val line = StringBuilder()
            for ((index, b) in bytes.withIndex()) {
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
    }
}