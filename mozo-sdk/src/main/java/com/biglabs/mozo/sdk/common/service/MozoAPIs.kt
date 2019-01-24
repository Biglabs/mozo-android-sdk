package com.biglabs.mozo.sdk.common.service

import com.biglabs.mozo.sdk.common.model.*
import retrofit2.Call
import retrofit2.http.*

internal interface MozoAPIs {
    @GET("contacts")
    fun getContacts(): Call<Base<BaseData<Contact>>>

    @POST("contacts")
    fun saveContact(@Body contact: Contact): Call<Base<Contact>>

    @GET("store-book")
    fun getStoreBook(): Call<Base<BaseData<Contact>>>

    @GET("user-profile")
    fun fetchProfile(): Call<Base<Profile>>

    @PUT("user-profile/exchange-info")
    fun saveExchangeInfo(exchangeInfo: Profile): Call<Base<Profile>>

    @PUT("user-profile/settings")
    fun saveSettings(notificationThreshold: Int = 0): Call<Base<Profile>>

    @PUT("user-profile/wallet")
    fun saveWallet(@Body walletInfo: WalletInfo): Call<Base<Profile>>

    @GET("solo/contract/solo-token/balance/{address}")
    fun getBalance(@Path("address") address: String): Call<Base<BalanceInfo>>

    @GET("exchange/rate")
    fun getExchangeRate(@Query("currency") currency: String, @Query("symbol") symbol: String): Call<Base<ExchangeRate>>

    @POST("solo/contract/solo-token/transfer")
    fun createTx(@Body request: TransactionRequest): Call<Base<TransactionResponse>>

    @POST("solo/contract/solo-token/send-signed-tx")
    fun sendTx(@Body request: TransactionResponse): Call<Base<TransactionResponse>>

    @GET("solo/contract/solo-token/txhistory/{address}")
    fun getTransactionHistory(@Path("address") address: String, @Query("page") page: Int, @Query("size") size: Int): Call<Base<BaseData<TransactionHistory>>>

    @GET("eth/solo/txs/{hash}/status")
    fun getTxStatus(@Path("hash") hash: String): Call<Base<TransactionStatus>>

    @GET("payment-request")
    fun getPaymentRequests(@Query("page") page: Int, @Query("size") size: Int, @Query("sort") sort: String = "timeInSec,desc"): Call<Base<BaseData<PaymentRequest>>>

    @POST("payment-request/{address}")
    fun sendPaymentRequest(@Path("address") address: String, @Body request: PaymentRequest): Call<Base<PaymentRequest>>

    @DELETE("payment-request/{id}")
    fun deletePaymentRequest(@Path("id") id: Long): Call<Base<Any>>
}