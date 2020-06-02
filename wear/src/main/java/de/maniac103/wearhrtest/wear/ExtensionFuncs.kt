package de.maniac103.wearhrtest.wear

import com.google.android.gms.wearable.MessageClient
import de.maniac103.wearhrtest.shared.MessageApiConstants
import de.maniac103.wearhrtest.shared.toByteArray

fun MessageClient.sendHeartRateResponse(nodeId: String, responseCode: Int, value: Int = 0) {
    val data = MessageApiConstants.HeartRateRequestResult(responseCode, value).toByteArray()
    sendMessage(nodeId,
        MessageApiConstants.MESSAGE_PATH_HR_RESULT, data)
        .addOnCompleteListener { }
}