package com.onionleaks.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/** AES-256-GCM with a random 12-byte nonce prefixed to the ciphertext. Used for replies at rest. */
public final class SymmetricBox {
    private static final SecureRandom RNG = new SecureRandom();
    private SymmetricBox() {}

    public static byte[] encrypt(byte[] key, byte[] plaintext) {
        try {
            byte[] nonce = new byte[12];
            RNG.nextBytes(nonce);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            byte[] ct = c.doFinal(plaintext);
            byte[] out = Arrays.copyOf(nonce, 12 + ct.length);
            System.arraycopy(ct, 0, out, 12, ct.length);
            return out;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] decrypt(byte[] key, byte[] blob) {
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, blob, 0, 12));
            return c.doFinal(blob, 12, blob.length - 12);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Decryption failed", e);
        }
    }
}
