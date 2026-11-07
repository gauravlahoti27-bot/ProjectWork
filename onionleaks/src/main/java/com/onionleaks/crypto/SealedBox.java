package com.onionleaks.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Anonymous public-key encryption ("sealed box") built only from JDK primitives:
 *
 *   ephemeral X25519 keypair  ->  ECDH shared secret
 *   HKDF-SHA256(shared, salt = ephPub || recipientPub)  ->  256-bit AES key
 *   AES-256-GCM(nonce = 12 random bytes, AAD = ephPub)
 *
 * Wire format (all raw bytes, Base64 when stored):
 *   [0x01 version][44 bytes ephemeral X.509 pub][12 bytes nonce][ciphertext+tag]
 *
 * The sender needs only the recipient's public key, so the web server can encrypt
 * every submission but can never decrypt one. Only the holder of the private key
 * (the reviewer, offline) can open the box.
 */
public final class SealedBox {

    private static final byte VERSION = 0x01;
    private static final int NONCE_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final int X25519_X509_LEN = 44;
    private static final SecureRandom RNG = new SecureRandom();

    private SealedBox() {}

    // ---------- key management ----------

    public static KeyPair generateKeyPair() {
        try {
            return KeyPairGenerator.getInstance("X25519").generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("X25519 unavailable", e);
        }
    }

    public static String encodePublic(PublicKey k)   { return Base64.getEncoder().encodeToString(k.getEncoded()); }
    public static String encodePrivate(PrivateKey k) { return Base64.getEncoder().encodeToString(k.getEncoded()); }

    public static PublicKey decodePublic(String b64) {
        try {
            return KeyFactory.getInstance("X25519").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(b64.trim())));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid X25519 public key", e);
        }
    }

    public static PrivateKey decodePrivate(String b64) {
        try {
            return KeyFactory.getInstance("X25519").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(b64.trim())));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid X25519 private key", e);
        }
    }

    // ---------- seal / open ----------

    public static byte[] seal(byte[] plaintext, PublicKey recipient) {
        try {
            KeyPair eph = generateKeyPair();
            byte[] ephPub = eph.getPublic().getEncoded();
            byte[] shared = agree(eph.getPrivate(), recipient);
            byte[] aesKey = hkdf(shared, concat(ephPub, recipient.getEncoded()), 32);
            Arrays.fill(shared, (byte) 0);

            byte[] nonce = new byte[NONCE_LEN];
            RNG.nextBytes(nonce);

            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
            c.updateAAD(ephPub);
            byte[] ct = c.doFinal(plaintext);
            Arrays.fill(aesKey, (byte) 0);

            return ByteBuffer.allocate(1 + ephPub.length + NONCE_LEN + ct.length)
                    .put(VERSION).put(ephPub).put(nonce).put(ct).array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("seal failed", e);
        }
    }

    public static byte[] open(byte[] box, PrivateKey recipientPriv, PublicKey recipientPub) {
        if (box == null || box.length < 1 + X25519_X509_LEN + NONCE_LEN + 16 || box[0] != VERSION)
            throw new IllegalArgumentException("Malformed sealed box");
        try {
            ByteBuffer bb = ByteBuffer.wrap(box);
            bb.get(); // version
            byte[] ephPubBytes = new byte[X25519_X509_LEN];
            bb.get(ephPubBytes);
            byte[] nonce = new byte[NONCE_LEN];
            bb.get(nonce);
            byte[] ct = new byte[bb.remaining()];
            bb.get(ct);

            PublicKey ephPub = KeyFactory.getInstance("X25519").generatePublic(new X509EncodedKeySpec(ephPubBytes));
            byte[] shared = agree(recipientPriv, ephPub);
            byte[] aesKey = hkdf(shared, concat(ephPubBytes, recipientPub.getEncoded()), 32);
            Arrays.fill(shared, (byte) 0);

            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
            c.updateAAD(ephPubBytes);
            byte[] pt = c.doFinal(ct);
            Arrays.fill(aesKey, (byte) 0);
            return pt;
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Decryption failed (wrong key or tampered data)", e);
        }
    }

    // ---------- helpers ----------

    private static byte[] agree(PrivateKey priv, PublicKey pub) throws GeneralSecurityException {
        KeyAgreement ka = KeyAgreement.getInstance("X25519");
        ka.init(priv);
        ka.doPhase(pub, true);
        return ka.generateSecret();
    }

    /** RFC 5869 HKDF with SHA-256, info = "onionleaks-v1". */
    static byte[] hkdf(byte[] ikm, byte[] salt, int len) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        byte[] info = "onionleaks-v1".getBytes();
        byte[] out = new byte[len];
        byte[] t = new byte[0];
        int pos = 0;
        for (byte i = 1; pos < len; i++) {
            mac.update(t);
            mac.update(info);
            mac.update(i);
            t = mac.doFinal();
            int n = Math.min(t.length, len - pos);
            System.arraycopy(t, 0, out, pos, n);
            pos += n;
        }
        return out;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] r = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }
}
