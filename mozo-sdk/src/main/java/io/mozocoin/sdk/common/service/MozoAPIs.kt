package io.mozocoin.sdk.common.service

import io.mozocoin.sdk.common.model.*
import retrofit2.Call
import retrofit2.http.*

internal interface MozoAPIs {
    /**
     * Check System status
     */
    @GET("system-status")
    fun checkSystemStatus(): Call<Base<Status>>

    @GET("${MozoAPIsService.APIS_SOLOMON}/contacts")
    fun getContacts(): Call<Base<BaseData<Contact>>>

    @POST("${MozoAPIsService.APIS_SOLOMON}/contacts")
    fun saveContact(@Body contact: Contact): Call<Base<Contact>>

    @GET("${MozoAPIsService.APIS_STORE}/store-books")
    fun getStoreBook(): Call<Base<BaseData<Contact>>>

    @GET("${MozoAPIsService.APIS_SOLOMON}/user-profile")
    fun getProfile(): Call<Base<Profile>>

    @PUT("${MozoAPIsService.APIS_SOLOMON}/user-profile")
    fun updateProfile(@Body profile: Profile): Call<Base<Profile>>

    @PUT("${MozoAPIsService.APIS_SOLOMON}/user-profile/exchange-info")
    fun saveExchangeInfo(exchangeInfo: Profile): Call<Base<Profile>>

    @PUT("${MozoAPIsService.APIS_SOLOMON}/user-profile/settings")
    fun saveSettings(notificationThreshold: Int = 0): Call<Base<Profile>>

    @PUT("${MozoAPIsService.APIS_SOLOMON}/user-profile/walletAll")
    fun saveWallet(@Body walletInfo: WalletInfo): Call<Base<Profile>>

    @PUT("${MozoAPIsService.APIS_SOLOMON}/user-profile/wallet/reset-pin")
    fun resetWallet(@Body walletInfo: WalletInfo): Call<Base<Profile>>

    @GET("${MozoAPIsService.APIS_SOLOMON}/solo/contract/solo-token/balance/{address}")
    fun getBalance(@Path("address") address: String): Call<Base<BalanceInfo>>

    @GET("${MozoAPIsService.APIS_SOLOMON}/exchange/rateETHAndToken")
    fun getExchangeRate(@Query("locale") locale: String): Call<Base<ExchangeRateData>>

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

    /**
     * OnChain Wallet APIs
     */
    @PUT("${MozoAPIsService.APIS_SOLOMON}/user-profile/updateWalletOnchain")
    fun saveOnChainWallet(@Body walletInfo: WalletInfo): Call<Base<Profile>>

    @GET("${MozoAPIsService.APIS_STORE}/onchain/getBalanceETHAndToken/{address}")
    fun getOnChainBalance(@Path("address") address: String): Call<Base<BalanceData>>

    @GET("${MozoAPIsService.APIS_SOLOMON}/getGasPrices")
    fun getGasInfo(): Call<Base<GasInfo>>

    @POST("${MozoAPIsService.APIS_STORE}/onchain/prepareConvertMozoXToSolo")
    fun prepareConvertRequest(@Body request: ConvertRequest): Call<Base<TransactionResponse>>

    @POST("${MozoAPIsService.APIS_STORE}/onchain/sign-transfer")
    fun signConvertRequest(@Body request: TransactionResponse): Call<Base<TransactionResponse>>

    @GET("${MozoAPIsService.APIS_STORE}/onchain/status/{hash}")
    fun getConvertStatus(@Path("hash") hash: String): Call<Base<TransactionStatus>>

    /**
     * OnChain inside OffChain Address APIs
     */
    @GET("${MozoAPIsService.APIS_STORE}/onchain/getBalanceTokenOnchainOffchain/{address}")
    fun getOnChainBalanceInOffChain(@Path("address") address: String): Call<Base<BalanceTokensData>>

    @GET("${MozoAPIsService.APIS_STORE}/onchain/getBalanceETHAndFeeTransferERC20/{address}")
    fun getEthBalanceInOffChain(@Path("address") address: String): Call<Base<BalanceEthData>>
}