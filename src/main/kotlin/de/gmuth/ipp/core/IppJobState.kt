package de.gmuth.ipp.core

enum class IppJobState(val value: Int) {

    Pending(3),
    PendingHeld(4),
    Processing(5),
    ProcessingStopped(6),
    Canceled(7),
    Aborted(8),
    Completed(9);

    override fun toString() = name
            .replace("[A-Z]".toRegex()) { "-" + it.value.toLowerCase() }
            .replace("^-".toRegex(), "")

    companion object {
        private val map = IppJobState.values().associateBy(IppJobState::value)
        fun fromInt(value: Int): IppJobState = map[value] ?: throw IllegalArgumentException(String.format("job stage %02X undefined", value))
    }

}