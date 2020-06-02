package de.maniac103.wearhrtest.shared

import java.nio.BufferUnderflowException
import java.nio.ByteBuffer

object MessageApiConstants {
    const val MESSAGE_PATH_HR_REQUEST = "/request_hr"
    const val MESSAGE_PATH_HR_RESULT = "/request_hr/result"

    const val RESPONSE_CODE_SUCCESS = 0
    const val RESPONSE_CODE_REQUESTING_PERMISSION = 1
    const val RESPONSE_CODE_PERMISSION_GRANTED = 2
    const val RESPONSE_CODE_PERMISSION_DENIED = 3
    const val RESPONSE_CODE_MEASUREMENT_TIMEOUT = 4

    data class HeartRateRequestResult(val responseCode: Int, val heartRate: Int)
}

fun MessageApiConstants.HeartRateRequestResult.toByteArray(): ByteArray {
    val buffer = ByteBuffer.allocate(3)
    buffer.put(responseCode.toByte())
    buffer.putShort(heartRate.toShort())
    return buffer.array()
}

fun ByteArray.toHeartRateRequestResult(): MessageApiConstants.HeartRateRequestResult? {
    val buffer = ByteBuffer.wrap(this)
    try {
        val responseCode = buffer.get()
        val value = buffer.getShort()
        return MessageApiConstants.HeartRateRequestResult(responseCode.toInt(), value.toInt())
    } catch (e: BufferUnderflowException) {
        return null
    }
}