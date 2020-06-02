package de.maniac103.wearhrtest

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.gms.wearable.*
import de.maniac103.wearhrtest.shared.MessageApiConstants
import de.maniac103.wearhrtest.shared.toHeartRateRequestResult
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener, AdapterView.OnItemClickListener {
    private lateinit var messageClient: MessageClient
    private lateinit var deviceList: ListView
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusView = findViewById(R.id.status)
        deviceList = findViewById(R.id.devices)
        deviceList.onItemClickListener = this

        messageClient = Wearable.getMessageClient(this)
        messageClient.addListener(this)
        scanForWearDevices()
    }

    override fun onItemClick(view: AdapterView<*>?, itemView: View?, position: Int, id: Long) {
        val adapter = view?.adapter as DeviceAdapter?
        val node = adapter?.getItem(position)
        node?.let { sendHrRequest(it.id) }
    }

    private fun scanForWearDevices() {
        Wearable.getCapabilityClient(this)
                .getCapability(CAPABILITY_NAME_HR, CapabilityClient.FILTER_REACHABLE)
                .addOnSuccessListener { capInfo ->
                    Log.d(TAG, "Found ${capInfo.nodes.size} nodes")
                    capInfo.nodes.forEach { node ->
                        Log.d(TAG, "Found node ${node.displayName}, nearby ${node.isNearby}")
                    }
                    deviceList.adapter = DeviceAdapter(this, capInfo.nodes.toList())
                    statusView.text = ""
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "Failed scanning for devices", e)
                    statusView.setText(R.string.status_device_list_failure)
                }
    }

    private fun sendHrRequest(nodeId: String) {
        statusView.setText(R.string.status_loading_hr)
        messageClient.sendMessage(nodeId, MessageApiConstants.MESSAGE_PATH_HR_REQUEST, null)
                .addOnCompleteListener { task ->
                    Log.d(TAG, "task complete $task")
                }
    }

    override fun onMessageReceived(event: MessageEvent) {
        Log.d(TAG, "Received message $event")
        if (event.path == MessageApiConstants.MESSAGE_PATH_HR_RESULT) {
            val data = event.data.toHeartRateRequestResult()
            Log.d(TAG, "Got response $data")
            when (data?.responseCode) {
                MessageApiConstants.RESPONSE_CODE_SUCCESS -> {
                    statusView.setText(getString(R.string.status_hr, data.heartRate))
                }
                MessageApiConstants.RESPONSE_CODE_MEASUREMENT_TIMEOUT -> {
                    statusView.setText(R.string.status_timeout)
                }
                MessageApiConstants.RESPONSE_CODE_REQUESTING_PERMISSION -> {
                    statusView.setText(R.string.status_requesting_perm)
                }
                MessageApiConstants.RESPONSE_CODE_PERMISSION_GRANTED -> {
                    sendHrRequest(event.sourceNodeId)
                }
                MessageApiConstants.RESPONSE_CODE_PERMISSION_DENIED -> {
                    statusView.setText(R.string.status_permission_denied)
                }
                else -> {
                    statusView.setText(getString(R.string.status_unexpected, data?.responseCode))
                }
            }
        }
    }

    companion object {
        private const val CAPABILITY_NAME_HR = "wear_hr"
        private const val TAG = "WearHrActivity"

        private class DeviceAdapter(
            context: Context,
            nodes: List<Node>
        ) : ArrayAdapter<Node>(context, android.R.layout.simple_list_item_1, android.R.id.text1, nodes)
    }
}
