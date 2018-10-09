package com.biglabs.mozo.sdk;

import android.util.Log;

import com.biglabs.mozo.sdk.utils.CryptoUtils;

import org.junit.Test;
import org.web3j.crypto.MnemonicUtils;

import java.security.SecureRandom;

public class WalletServiceUnitTest {

    private static final String PIN = "000000";
    private static final int quantify = 10;

    @Test
    public void generateSeeds() {
        byte[] entropy = new SecureRandom().generateSeed(16);
        for (int i = 0; i < quantify; i++) {
            int index = i + 1;
            String mnemonic = MnemonicUtils.generateMnemonic(entropy);
            try {
                String mnemonicEn = CryptoUtils.encrypt(mnemonic, PIN);
                System.out.println(index + "-seed: " + mnemonic);
                Log.e(getClass().getName(), index + "-seed encrypted: " + mnemonicEn);

                String privateKey = CryptoUtils.getFirstAddressPrivateKey(mnemonic);
                String privateKeyEn = CryptoUtils.encrypt(privateKey, PIN);

                Log.e(getClass().getName(), index + "-privateKey: " + privateKey);
                Log.e(getClass().getName(), index + "-privateKey encrypted: " + privateKeyEn);
            } catch (Exception ignored) {
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }
}
