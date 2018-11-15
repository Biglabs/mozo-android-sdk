package com.biglabs.mozo.sdk.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.ActivityCompat

internal class PermissionWrapperActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent != null && intent!!.hasExtra(TARGET_PERMISSION)) {
            val targetPermission = intent.getStringArrayExtra(TARGET_PERMISSION)
            if (targetPermission.isEmpty()) return

            ActivityCompat.requestPermissions(this@PermissionWrapperActivity, targetPermission, PERMISSIONS_REQUEST)
        } else {
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        finish()
        when (requestCode) {
            PERMISSIONS_REQUEST -> {
                //WalletService.getInstance().onPermissionsResult(permissions, grantResults)
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST = 0x1
        private const val TARGET_PERMISSION = "target-permission"

        fun startRequestPermission(context: Context, permission: String) {
            startRequestPermission(context, arrayOf(permission))
        }

        fun startRequestPermission(context: Context, permissions: Array<String>) {
            val intent = Intent(context, PermissionWrapperActivity::class.java)
            intent.putExtra(TARGET_PERMISSION, permissions)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }
}