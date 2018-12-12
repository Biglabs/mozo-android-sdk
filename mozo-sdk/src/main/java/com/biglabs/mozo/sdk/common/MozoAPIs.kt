package com.biglabs.mozo.sdk.common

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.*

internal interface MozoAPIs {
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

    @GET("eth/solo/txs/{hash}/status")
    fun getTransactionStatus(@Path("hash") hash: String): Deferred<Response<Models.TransactionStatus>>

    @GET("exchange/rate")
    fun getExchangeRate(@Query("currency") currency: String, @Query("symbol") symbol: String): Deferred<Response<Models.ExchangeRate>>

    @GET("payment-request")
    fun getPaymentRequests(@Query("page") page: Int, @Query("size") size: Int, @Query("sort") sort: String = "timeInSec,desc"): Deferred<Response<List<Models.PaymentRequest>>>

    @POST("payment-request/{address}")
    fun sendPaymentRequest(@Path("address") address: String, @Body request: Models.PaymentRequest): Deferred<Response<Models.PaymentRequest>>

    @DELETE("payment-request/{id}")
    fun deletePaymentRequest(@Path("id") id: Long): Deferred<Response<Any>>
}