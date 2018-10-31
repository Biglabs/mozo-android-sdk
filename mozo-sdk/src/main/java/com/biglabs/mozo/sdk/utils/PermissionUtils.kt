package com.biglabs.mozo.sdk.utils

import android.Manifest
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

        fun requestLocationPermission(context: Context) {
            PermissionWrapperActivity.startRequestPermission(context, arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ))
        }
    }
}