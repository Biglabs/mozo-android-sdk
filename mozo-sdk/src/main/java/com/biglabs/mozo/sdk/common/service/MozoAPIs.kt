package com.biglabs.mozo.sdk.common.service

import com.biglabs.mozo.sdk.common.model.*
import retrofit2.Call
import retrofit2.http.*

internal interface MozoAPIs {
    @GET("${MozoAPIsService.APIS_SOLOMON}/contacts")
    fun getContacts(): Call<Base<BaseData<Contact>>>

    @POST("${MozoAPIsService.APIS_SOLOMON}/contacts")
    fun saveContact(@Body contact: Contact): Call<Base<Contact>>

    @GET("${MozoAPIsService.APIS_STORE}/store-books")
    fun getStoreBook(): Call<Base<BaseData<Contact>>>

    @GET("${MozoAPIsService.APIS_SOLOMON}/user-profile")
    fun fetchProfile(): Call<Base<Profile>>

    @PUT("${MozoAPIsService.APIS_SOLOMON}/user-profile/exchange-info")
    fun saveExchangeInfo(exchangeInfo: Profile): Call<Base<Profile>>

    @PUT("${MozoAPIsService.APIS_SOLOMON}/user-profile/settings")
    fun saveSettings(notificationThreshold: Int = 0): Call<Base<Profile>>

    @PUT("${MozoAPIsService.APIS_SOLOMON}/user-profile/wallet")
    fun saveWallet(@Body walletInfo: WalletInfo): Call<Base<Profile>>

    @GET("${MozoAPIsService.APIS_SOLOMON}/solo/contract/solo-token/balance/{address}")
    fun getBalance(@Path("address") address: String): Call<Base<BalanceInfo>>

    @GET("${MozoAPIsService.APIS_SOLOMON}/exchange/rate")
    fun getExchangeRate(@Query("locale") locale: String): Call<Base<ExchangeRate>>

    @POST("${MozoAPIsService.APIS_SOLOMON}/solo/contract/solo-token/transfer")
    fun createTx(@Body request: TransactionRequest): Call<Base<TransactionResponse>>

    @POST("${MozoAPIsService.APIS_SOLOMON}/solo/contract/solo-token/send-signed-tx")
    fun sendTx(@Body request: TransactionResponse): Call<Base<TransactionResponse>>

    @GET("${MozoAPIsService.APIS_SOLOMON}/solo/contract/solo-token/txhistory/{address}")
    fun getTransactionHistory(@Path("address") address: String, @Query("page") page: Int, @Query("size") size: Int): Call<Base<BaseData<TransactionHistory>>>

    @GET("${MozoAPIsService.APIS_SOLOMON}/eth/solo/txs/{hash}/status")
    fun getTxStatus(@Path("hash") hash: String): Call<Base<TransactionStatus>>

    @GET("${MozoAPIsService.APIS_SOLOMON}/payment-request")
    fun getPaymentRequests(@Query("page") page: Int, @Query("size") size: Int, @Query("sort") sort: String = "timeInSec,desc"): Call<Base<BaseData<PaymentRequest>>>

    @POST("${MozoAPIsService.APIS_SOLOMON}/payment-request/{address}")
    fun sendPaymentRequest(@Path("address") address: String, @Body request: PaymentRequest): Call<Base<PaymentRequest>>

    @DELETE("${MozoAPIsService.APIS_SOLOMON}/payment-request/{id}")
    fun deletePaymentRequest(@Path("id") id: Long): Call<Base<Any>>
}