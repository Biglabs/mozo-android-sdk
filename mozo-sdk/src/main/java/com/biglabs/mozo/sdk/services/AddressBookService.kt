package com.biglabs.mozo.sdk.services

import android.content.Context
import com.biglabs.mozo.sdk.core.Models
import com.biglabs.mozo.sdk.core.MozoApiService
import com.biglabs.mozo.sdk.core.MozoService
import kotlinx.coroutines.experimental.async

internal class AddressBookService {

    val data = arrayListOf<Models.Contact>()

    fun fetchData(context: Context) = async {
        val response = MozoService.getInstance(context).getContacts().await()
        response?.let { contacts ->
            data.clear()
            data.addAll(contacts.sortedBy { it.name })
        }
    }

    fun findByAddress(address: String) =
            data.find { it.soloAddress.equals(address, ignoreCase = true) }


    companion object {
        @Volatile
        private var instance: AddressBookService? = null

        fun getInstance() = instance ?: synchronized(this) {
            if (instance == null) instance = AddressBookService()
            instance
        }!!
    }
}