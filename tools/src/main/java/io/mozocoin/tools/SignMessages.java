package io.mozocoin.tools;

import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDUtils;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.security.InvalidParameterException;

public class SignMessages {
    private static final String wallet = "hub razor edge example spike raise skull club tilt current evil rebuild";

    private static final String[] toSigns = {
            "0x3652a3dfe364d9c7a1b6768aecbb8dd16d2ed6af24a4921bb89cb2edb2d96f3e"
    };

    public static void main(String[] args) {
        try {
            String privateKey = getAddressPrivateKey(/*Off-chain address*/0, wallet);

            Credentials credentials = Credentials.create(privateKey);
            String publicKey = Numeric.toHexStringWithPrefixSafe(credentials.getEcKeyPair().getPublicKey());

            for (String toSign : toSigns) {
                Sign.SignatureData signatureData = Sign.signMessage(
                        Numeric.hexStringToByteArray(toSign),
                        credentials.getEcKeyPair(),
                        false
                );
                String signature = serializeSignature(signatureData);
                System.out.println("\n\nToSign: " + toSign + "\nsignature: " + signature + "\npublicKey: " + publicKey);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String getAddressPrivateKey(int derived, String mnemonic) throws UnreadableWalletException {
        if (derived < 0) {
            throw new InvalidParameterException("Invalid derived");
        }
        DeterministicKey key = DeterministicKeyChain
                .builder()
                .seed(new DeterministicSeed(mnemonic, null, "", System.nanoTime()))
                .build()
                .getKeyByPath(HDUtils.parsePath("M/44H/60H/0H/0/" + derived), true);
        return key.getPrivKey().toString(16);
    }

    static String serializeSignature(Sign.SignatureData signature) {

        byte[] r = canonicalize(signature.getR());
        byte[] s = canonicalize(signature.getS());

        int totalLength = 6 + r.length + s.length;
        byte[] result = new byte[totalLength];

        result[0] = 0x30;
        result[1] = (byte) (totalLength - 2);
        result[2] = 0x02;
        result[3] = (byte) r.length;

        System.arraycopy(r, 0, result, 4, r.length);

        int offset = r.length + 4;
        result[offset] = 0x02;
        result[offset + 1] = (byte) s.length;

        for (int i = 0; i < s.length; i++) {
            result[offset + 2 + i] = s[i];
        }

        return Numeric.toHexString(result);
    }

    private static byte[] canonicalize(byte[] bytes) {
        byte[] b = bytes;
        if (b.length == 0) {
            b = new byte[]{0x00};
        }
        if ((b[0] & ((byte) 0x80)) != ((byte) 0x00)) {
            byte[] paddedBytes = new byte[b.length + 1];
            paddedBytes[0] = 0x00;
            System.arraycopy(b, 0, paddedBytes, 1, b.length);
            b = paddedBytes;
        }
        return b;
    }
}
