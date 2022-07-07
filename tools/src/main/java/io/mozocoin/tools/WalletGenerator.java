package io.mozocoin.tools;

import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDPath;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.cryptonode.jncryptor.AES256JNCryptor;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.MnemonicUtils;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.security.SecureRandom;

public class WalletGenerator {
    private static final int FIRST_ADDRESS = 0;
    private static final int SECOND_ADDRESS = 1;
    private static final String DEFAULT_PIN = "123456";
    private static final int NUMBER_OF_WALLET = 10;

    private static String getAddressPrivateKey(int derived, String mnemonic) throws UnreadableWalletException {
        if (derived < 0) {
            throw new InvalidParameterException("Invalid derived");
        }
        DeterministicKey key = DeterministicKeyChain
                .builder()
                .seed(new DeterministicSeed(mnemonic, null, "", System.nanoTime()))
                .build()
                .getKeyByPath(HDPath.parsePath("M/44H/60H/0H/0/" + derived), true);
        return key.getPrivKey().toString(16);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(String[] args) {

        try {
            File file = new File("tools/output");
            file.mkdirs();
            FileWriter myWriter = new FileWriter("tools/output/1001Wallets.csv");
            myWriter.write("STT,Offchain Wallet,Onchain Wallet,PIN,Seed Phrase,Encrypt Seed Phrase = PIN + Seed Phrase");

            System.out.println("WALLET GENERATOR");
            for (int i = 0; i < NUMBER_OF_WALLET; i++) {
                String mnemonic = MnemonicUtils.generateMnemonic(
                        new SecureRandom().generateSeed(16)
                );

                System.out.println("Seed Phrase " + (i + 1) + ": " + mnemonic);

                String encryptSeedPhrase = java.util.Base64.getEncoder().encodeToString(
                        new AES256JNCryptor().encryptData(
                                mnemonic.getBytes(StandardCharsets.UTF_8),
                                DEFAULT_PIN.toCharArray()
                        )
                ).replace("\n", "");
                //System.out.println("encryptSeedPhrase: " + encryptSeedPhrase);


                String privateKey = getAddressPrivateKey(FIRST_ADDRESS, mnemonic);
                String address = Credentials.create(privateKey).getAddress();
                //System.out.println("\nOffChain Wallet Address: " + address);
                //System.out.println("Private Key: " + privateKey);


                String pk2 = getAddressPrivateKey(SECOND_ADDRESS, mnemonic);
                String a2 = Credentials.create(pk2).getAddress();
                //System.out.println("\n\nOnChain Wallet Address: " + a2);
                //System.out.println("Private Key: " + pk2);

                String aWallet = "\n" + (i + 1) + "," + address + "," + a2 + "," + DEFAULT_PIN + "," + mnemonic + "," + encryptSeedPhrase;
                myWriter.write(aWallet);
            }


            myWriter.close();
            System.out.println("Write " + NUMBER_OF_WALLET + " wallet(s)! DONE");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}