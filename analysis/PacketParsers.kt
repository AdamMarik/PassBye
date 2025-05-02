package com.project.passbye.analysis

object PacketParsers {
    fun extractDnsQuery(payload: ByteArray): String? {
        return try {
            var index = 12
            val parts = mutableListOf<String>()
            while (index < payload.size && payload[index].toInt() != 0) {
                val len = payload[index++].toInt() and 0xFF
                if (index + len > payload.size) return null
                parts.add(String(payload, index, len))
                index += len
            }
            parts.joinToString(".")
        } catch (_: Exception) {
            null
        }
    }

    fun extractSni(payload: ByteArray): String? {
        return try {
            var index = 0
            if (payload[index].toInt() != 0x16) return null
            index += 5
            if (payload[index].toInt() != 0x01) return null
            index += 38
            val sessionIdLength = payload[index++].toInt() and 0xFF
            index += sessionIdLength
            val cipherLength = ((payload[index++].toInt() and 0xFF) shl 8) or (payload[index++].toInt() and 0xFF)
            index += cipherLength
            val compressionLen = payload[index++].toInt() and 0xFF
            index += compressionLen
            val extensionsLen = ((payload[index++].toInt() and 0xFF) shl 8) or (payload[index++].toInt() and 0xFF)

            val end = index + extensionsLen
            while (index + 4 < end) {
                val type = ((payload[index++].toInt() and 0xFF) shl 8) or (payload[index++].toInt() and 0xFF)
                val length = ((payload[index++].toInt() and 0xFF) shl 8) or (payload[index++].toInt() and 0xFF)
                if (type == 0x00) {
                    index += 2
                    val sniLength = payload[index++].toInt() and 0xFF
                    return String(payload, index, sniLength)
                }
                index += length
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
