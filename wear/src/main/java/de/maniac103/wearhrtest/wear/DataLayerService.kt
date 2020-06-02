package de.maniac103.wearhrtest.wear

import android.Manifest
import android.content.pm.PackageManager
import com.google.android.gms.wearable.*
import de.maniac103.wearhrtest.shared.MessageApiConstants

class DataLayerService: WearableListenerService() {
    private lateinit var messageClient: MessageClient

    override fun onCreate() {
        super.onCreate()
        messageClient = Wearable.getMessageClient(this)
    }

    override fun onMessageReceived(event: MessageEvent?) {
        if (event?.path == MessageApiConstants.MESSAGE_PATH_HR_REQUEST) {
            checkPermissionAndStartMeasurement(event.sourceNodeId)
        }
    }

    private fun checkPermissionAndStartMeasurement(nodeId: String) {
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            messageClient.sendHeartRateResponse(nodeId,
                MessageApiConstants.RESPONSE_CODE_REQUESTING_PERMISSION)
            requestPermission(nodeId)
        } else {
            HeartRateMeasurementService.startRequest(this, nodeId)
        }
    }

    private fun requestPermission(nodeId: String) {
        val intent = PermissionRequestActivity.createIntent(this,
            Manifest.permission.BODY_SENSORS, nodeId)
        startActivity(intent)
    }
}