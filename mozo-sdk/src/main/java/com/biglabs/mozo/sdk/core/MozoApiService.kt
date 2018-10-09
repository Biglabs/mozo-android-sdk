package com.biglabs.mozo.sdk.core

import kotlinx.coroutines.experimental.Deferred
import retrofit2.Response
import retrofit2.http.*

interface MozoApiService {
    @GET("contacts")
    fun getContacts(): Deferred<Response<List<Models.Contact>>>

    @POST("contacts")
    fun saveContact(@Body contact: Models.Contact): Deferred<Response<Models.Contact>>

    @GET("user-profile")
    fun fetchProfile(): Deferred<Response<Models.Profile>>

    @PUT("user-profile/exchange-info")
    fun saveExchangeInfo(exchangeInfo: Models.Profile): Deferred<Response<Models.Profile>>

    @PUT("user-profile/settings")
    fun saveSettings(notificationThreshold: Int = 0): Deferred<Response<Models.Profile>>

    @PUT("user-profile/wallet")
    fun saveWallet(@Body walletInfo: Models.WalletInfo): Deferred<Response<Models.Profile>>

    @GET("solo/contract/solo-token/balance/{address}")
    fun getBalance(@Path("address") address: String): Deferred<Response<Models.BalanceInfo>>

    @POST("solo/contract/solo-token/transfer")
    fun createTransaction(@Body request: Models.TransactionRequest): Deferred<Response<Models.TransactionResponse>>

    @POST("solo/contract/solo-token/send-signed-tx")
    fun sendTransaction(@Body request: Models.TransactionResponse): Deferred<Response<Models.TransactionResponse>>

    @GET("solo/contract/solo-token/txhistory/{address}")
    fun getTransactionHistory(@Path("address") address: String, @Query("page") page: Int, @Query("size") size: Int): Deferred<Response<List<Models.TransactionHistory>>>
}