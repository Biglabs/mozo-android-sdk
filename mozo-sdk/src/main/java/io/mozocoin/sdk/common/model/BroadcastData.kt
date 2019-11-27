package io.mozocoin.sdk.common.model

import com.google.gson.Gson

data class BroadcastData(
        val time: Long,
        val content: String,
        val imageId: String
) {
    fun getData() = try {
        Gson().fromJson(content, BroadcastDataContent::class.java)?.apply {
            time = this@BroadcastData.time
            imageId = this@BroadcastData.imageId
        }
    } catch (e: Exception) {
        null
    }
}