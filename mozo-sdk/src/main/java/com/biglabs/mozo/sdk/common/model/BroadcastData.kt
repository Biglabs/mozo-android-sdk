package com.biglabs.mozo.sdk.common.model

import com.google.gson.Gson

data class BroadcastData(
        val time: Long,
        val content: String
) {
    fun getData() = try {
        Gson().fromJson(content, BroadcastDataContent::class.java)?.apply {
            time = this@BroadcastData.time
        }
    } catch (e: Exception) {
        null
    }
}