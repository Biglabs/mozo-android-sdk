package io.mozocoin.sdk.common

import io.mozocoin.sdk.common.model.WalletInfo
import io.mozocoin.sdk.utils.CryptoUtils
import org.web3j.crypto.Credentials
import org.web3j.crypto.MnemonicUtils
import java.security.SecureRandom

internal class WalletHelper {

    private var mnemonic: String? = null
    private var mnemonicEncrypted: String? = null

    private var offChainAddress: String? = null
    private var offChainPrivateKey: String? = null

    private var onChainAddress: String? = null
    private var onChainPrivateKey: String? = null

    constructor(mnemonic: String) {
        this.mnemonic = mnemonic
        initAddresses()
    }

    constructor(walletInfo: WalletInfo) {
        this.mnemonicEncrypted = walletInfo.encryptSeedPhrase
        this.offChainAddress = walletInfo.offchainAddress
        this.onChainAddress = walletInfo.onchainAddress
    }

    private fun initAddresses() {
        mnemonic ?: return
        this.offChainPrivateKey = CryptoUtils.getAddressPrivateKey(CryptoUtils.FIRST_ADDRESS, mnemonic!!)
        this.offChainAddress = Credentials.create(offChainPrivateKey).address

        this.onChainPrivateKey = CryptoUtils.getAddressPrivateKey(CryptoUtils.SECOND_ADDRESS, mnemonic!!)
        this.onChainAddress = Credentials.create(offChainPrivateKey).address
    }

    fun isUnlocked() = !mnemonic.isNullOrEmpty()
            && MnemonicUtils.validateMnemonic(mnemonic)
            && !offChainPrivateKey.isNullOrEmpty()
            && !onChainPrivateKey.isNullOrEmpty()

    fun mnemonicPhrases() = mnemonic?.split(" ")

    fun encrypt(pin: String): WalletHelper {
        if (!mnemonic.isNullOrEmpty() && mnemonicEncrypted.isNullOrEmpty()) {
            mnemonicEncrypted = CryptoUtils.encrypt(mnemonic!!, pin)
        }
        return this
    }

    fun decrypt(pin: String): WalletHelper {
        if (mnemonic.isNullOrEmpty() && !mnemonicEncrypted.isNullOrEmpty()) {
            try {
                mnemonic = CryptoUtils.decrypt(mnemonicEncrypted!!, pin)
                if (mnemonic != null && MnemonicUtils.validateMnemonic(mnemonic)) {
                    initAddresses()
                }
            } catch (ignore: Exception) {
            }
        }
        return this
    }

    fun verifyPin(pin: String): Boolean {
        mnemonicEncrypted ?: return false

        val raw = CryptoUtils.decrypt(mnemonicEncrypted!!, pin)
        return !raw.isNullOrEmpty() && MnemonicUtils.validateMnemonic(raw)
    }

    fun buildWalletInfo() = WalletInfo().apply {
        encryptSeedPhrase = mnemonicEncrypted
        offchainAddress = offChainAddress
        onchainAddress = onChainAddress
    }

    fun buildOffChainCredentials() = if (isUnlocked() && !offChainPrivateKey.isNullOrEmpty())
        Credentials.create(offChainPrivateKey)
    else null

    fun buildOnChainCredentials() = if (isUnlocked() && !onChainPrivateKey.isNullOrEmpty())
        Credentials.create(onChainPrivateKey)
    else null

    companion object {
        fun create(): WalletHelper = WalletHelper(
                MnemonicUtils.generateMnemonic(
                        SecureRandom().generateSeed(16)
                )
        )

        fun initWithWalletInfo(walletInfo: WalletInfo?): WalletHelper? {
            return WalletHelper(walletInfo ?: return null)
        }
    }
}