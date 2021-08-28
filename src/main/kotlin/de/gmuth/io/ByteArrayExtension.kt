package de.gmuth.io

fun ByteArray.hexdump(maxRows: Int = 32, dump: (String) -> Unit) =
        with(StringBuilder()) {
            for ((index, b) in withIndex()) {
                append("%02X ".format(b))
                if ((index + 1) % maxRows == 0) {
                    dump(toString())
                    clear()
                }
            }
            if (isNotEmpty()) dump(toString())
        }