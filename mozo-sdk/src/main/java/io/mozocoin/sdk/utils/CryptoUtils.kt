package io.mozocoin.sdk.utils

import android.util.Base64
import androidx.annotation.IntRange
import org.bitcoinj.crypto.HDPath
import org.bitcoinj.wallet.DeterministicKeyChain
import org.bitcoinj.wallet.DeterministicSeed
import org.cryptonode.jncryptor.AES256JNCryptor
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.security.InvalidParameterException
import kotlin.experimental.and

class CryptoUtils {
    companion object {

        private const val ETH_DERIVATION_PATH = "M/44H/60H/0H/0/"
        internal const val FIRST_ADDRESS = 0
        internal const val SECOND_ADDRESS = 1

        @JvmStatic
        fun getAddressPrivateKey(
            @IntRange(from = 0, to = 1000) derived: Int,
            mnemonic: String
        ): String {
            if (derived < 0) {
                throw InvalidParameterException("Invalid derived")
            }
            val key = DeterministicKeyChain
                .builder()
                .seed(DeterministicSeed(mnemonic, null, "", System.nanoTime()))
                .build()
                .getKeyByPath(HDPath.parsePath(ETH_DERIVATION_PATH + derived), true)
            return key.privKey.toString(16)
        }

        @JvmStatic
        @Throws(Throwable::class)
        fun encrypt(value: String, password: String): String {
            return Base64.encodeToString(
                AES256JNCryptor().encryptData(
                    value.toByteArray(),
                    password.toCharArray()
                ), Base64.DEFAULT
            ).replace("\n", "")
        }

        @JvmStatic
        @Throws(Throwable::class)
        fun decrypt(value: String, password: String): String {
            return String(
                AES256JNCryptor().decryptData(
                    Base64.decode(
                        value.replace("\n", ""),
                        Base64.DEFAULT
                    ),
                    password.toCharArray()
                )
            )
        }

        @JvmStatic
        fun serializeSignature(signature: Sign.SignatureData): String {

            val r = canonicalize(signature.r)
            val s = canonicalize(signature.s)

            val totalLength = 6 + r.size + s.size
            val result = ByteArray(totalLength)

            result[0] = 0x30
            result[1] = (totalLength - 2).toByte()
            result[2] = 0x02
            result[3] = r.size.toByte()

            r.mapIndexed { index, byte ->
                result[index + 4] = byte
            }

            val offset = r.size + 4
            result[offset] = 0x02
            result[offset + 1] = s.size.toByte()

            s.mapIndexed { index, byte ->
                result[offset + 2 + index] = byte
            }

            return Numeric.toHexString(result)
        }

        private fun canonicalize(bytes: ByteArray): ByteArray {
            var b = bytes
            if (b.isEmpty()) {
                b = byteArrayOf(0x00)
            }
            if ((b[0].and(0x80.toByte())) != 0x00.toByte()) {
                val paddedBytes = ByteArray(b.size + 1)
                paddedBytes[0] = 0x00
                b.mapIndexed { index, byte ->
                    paddedBytes[index + 1] = byte
                }
                b = paddedBytes
            }
            return b
        }
    }
}