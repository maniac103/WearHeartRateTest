package de.maniac103.wearhrtest.wear

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.PowerManager
import android.util.SparseArray
import androidx.core.util.forEach
import androidx.core.util.isEmpty
import androidx.core.util.isNotEmpty
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import de.maniac103.wearhrtest.shared.MessageApiConstants
import kotlin.math.roundToInt

class HeartRateMeasurementService: Service(), SensorEventListener, Handler.Callback {
    private val waitingNodeIds = SparseArray<String>()
    private lateinit var messageClient: MessageClient
    private lateinit var sensorManager: SensorManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private val handler = Handler(this)

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SensorManager::class.java)
        messageClient = Wearable.getMessageClient(this)

        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "hrtest:measurement")
        wakeLock.setReferenceCounted(false)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val hadListeners = waitingNodeIds.isEmpty()
        intent?.getStringExtra("nodeId")?.let { waitingNodeIds.put(startId, it) }
        if (!hadListeners && waitingNodeIds.isNotEmpty()) {
            wakeLock.acquire()
            startMeasurement()
        }
        return if (waitingNodeIds.size() > 0) START_STICKY else START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun handleMessage(msg: Message): Boolean {
        if (msg.what == MSG_TIMEOUT) {
            sendResponseToWaitingNodesAndStop(MessageApiConstants.RESPONSE_CODE_MEASUREMENT_TIMEOUT)
            return true
        }
        return false
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            sensorManager.unregisterListener(this)
            sendResponseToWaitingNodesAndStop(MessageApiConstants.RESPONSE_CODE_SUCCESS,
                event.values[0].roundToInt())
        }
    }

    private fun startMeasurement() {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        handler.removeMessages(MSG_TIMEOUT)

        val msg = handler.obtainMessage(MSG_TIMEOUT)
        handler.sendMessageDelayed(msg, MEASUREMENT_TIMEOUT_MS)
    }

    private fun sendResponseToWaitingNodesAndStop(responseCode: Int, heartRate: Int = 0) {
        var maxStartId = 0
        waitingNodeIds.forEach { startId, nodeId ->
            maxStartId = Math.max(startId, maxStartId)
            messageClient.sendHeartRateResponse(nodeId, responseCode, heartRate)
        }
        waitingNodeIds.clear()
        wakeLock.release()
        handler.removeMessages(MSG_TIMEOUT)
        stopSelf(maxStartId)
    }

    companion object {
        private const val MSG_TIMEOUT = 1
        private const val MEASUREMENT_TIMEOUT_MS = 30000L

        fun startRequest(context: Context, nodeId: String) {
            val intent = Intent(context, HeartRateMeasurementService::class.java).apply {
                putExtra("nodeId", nodeId)
            }
            context.startService(intent)
        }
    }
}