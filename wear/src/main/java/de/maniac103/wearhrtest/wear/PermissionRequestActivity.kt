package de.maniac103.wearhrtest.wear

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import com.google.android.gms.wearable.Wearable
import de.maniac103.wearhrtest.shared.MessageApiConstants

class PermissionRequestActivity : WearableActivity() {
    private lateinit var permission: String
    private lateinit var nodeId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_permission_request)
        setAmbientEnabled()

        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        nodeId = intent.getStringExtra("nodeId")!!
        permission = intent.getStringExtra("permission")!!

        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            sendResultAndFinish(true)
            return
        }

        requestPermissions(arrayOf(permission), REQUEST_CODE_PERMISSION)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            sendResultAndFinish(grantResults[0] == PackageManager.PERMISSION_GRANTED)
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun sendResultAndFinish(granted: Boolean) {
        val responseCode = if (granted) {
            MessageApiConstants.RESPONSE_CODE_PERMISSION_GRANTED
        } else {
            MessageApiConstants.RESPONSE_CODE_PERMISSION_DENIED
        }
        Wearable.getMessageClient(this).sendHeartRateResponse(nodeId, responseCode)
        finish()
    }

    companion object {
        private val REQUEST_CODE_PERMISSION = 1000

        fun createIntent(context: Context, permission: String, nodeId: String): Intent {
            return Intent(context, PermissionRequestActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("permission", permission)
                putExtra("nodeId", nodeId)
            }
        }
    }
}
