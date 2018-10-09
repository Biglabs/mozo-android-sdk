package com.biglabs.mozo.sdk.utils

import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import com.biglabs.mozo.sdk.ui.PermissionWrapperActivity


class PermissionUtils {
    companion object {
        private fun isPermissionGranted(context: Context, permission: String): Boolean = (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)

        fun requestPermission(context: Context, permission: String): Boolean {
            if (!isPermissionGranted(context, permission)) {
                PermissionWrapperActivity.startRequestPermission(context, permission)
                return false
            }
            return true
        }
    }
}