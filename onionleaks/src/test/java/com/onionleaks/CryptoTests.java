package com.onionleaks;

import com.onionleaks.crypto.Codenames;
import com.onionleaks.crypto.SealedBox;
import com.onionleaks.crypto.SymmetricBox;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.onionleaks.T.*;

public class CryptoTests {

    public static void run() {
        System.out.println("CryptoTests");

        test("sealed box round-trips", () -> {
            KeyPair kp = SealedBox.generateKeyPair();
            byte[] pt = "the quick brown fox".getBytes(StandardCharsets.UTF_8);
            byte[] box = SealedBox.seal(pt, kp.getPublic());
            assertArrayEquals(pt, SealedBox.open(box, kp.getPrivate(), kp.getPublic()), "round trip");
        });

        test("sealed box handles empty and large payloads", () -> {
            KeyPair kp = SealedBox.generateKeyPair();
            assertArrayEquals(new byte[0], SealedBox.open(SealedBox.seal(new byte[0], kp.getPublic()), kp.getPrivate(), kp.getPublic()), "empty");
            byte[] big = new byte[3 * 1024 * 1024];
            new java.util.Random(42).nextBytes(big);
            assertArrayEquals(big, SealedBox.open(SealedBox.seal(big, kp.getPublic()), kp.getPrivate(), kp.getPublic()), "3MB");
        });

        test("same plaintext seals to different ciphertext every time (fresh ephemeral key + nonce)", () -> {
            KeyPair kp = SealedBox.generateKeyPair();
            byte[] pt = "same".getBytes();
            assertFalse(Arrays.equals(SealedBox.seal(pt, kp.getPublic()), SealedBox.seal(pt, kp.getPublic())), "ciphertexts must differ");
        });

        test("wrong private key cannot open the box", () -> {
            KeyPair a = SealedBox.generateKeyPair(), b = SealedBox.generateKeyPair();
            byte[] box = SealedBox.seal("secret".getBytes(), a.getPublic());
            assertThrows(IllegalArgumentException.class, () -> SealedBox.open(box, b.getPrivate(), b.getPublic()), "wrong key");
            assertThrows(IllegalArgumentException.class, () -> SealedBox.open(box, b.getPrivate(), a.getPublic()), "wrong key, right pub");
        });

        test("tampering with any byte is detected (AES-GCM tag)", () -> {
            KeyPair kp = SealedBox.generateKeyPair();
            byte[] box = SealedBox.seal("integrity".getBytes(), kp.getPublic());
            for (int i : new int[]{1, 20, 45, 50, box.length - 1}) {
                byte[] bad = box.clone();
                bad[i] ^= 0x01;
                assertThrows(IllegalArgumentException.class, () -> SealedBox.open(bad, kp.getPrivate(), kp.getPublic()), "tamper @" + i);
            }
        });

        test("malformed / truncated boxes are rejected", () -> {
            KeyPair kp = SealedBox.generateKeyPair();
            assertThrows(IllegalArgumentException.class, () -> SealedBox.open(new byte[10], kp.getPrivate(), kp.getPublic()), "short");
            byte[] box = SealedBox.seal("x".getBytes(), kp.getPublic());
            box[0] = 0x02;
            assertThrows(IllegalArgumentException.class, () -> SealedBox.open(box, kp.getPrivate(), kp.getPublic()), "bad version");
        });

        test("keys survive base64 encode/decode", () -> {
            KeyPair kp = SealedBox.generateKeyPair();
            var pub = SealedBox.decodePublic(SealedBox.encodePublic(kp.getPublic()));
            var priv = SealedBox.decodePrivate(SealedBox.encodePrivate(kp.getPrivate()));
            byte[] box = SealedBox.seal("roundtrip".getBytes(), pub);
            assertEquals("roundtrip", new String(SealedBox.open(box, priv, pub)), "encoded keys");
            assertThrows(IllegalArgumentException.class, () -> SealedBox.decodePublic("not base64!!"), "garbage key");
        });

        test("symmetric box round-trips and detects tampering", () -> {
            byte[] key = new byte[32];
            new java.security.SecureRandom().nextBytes(key);
            byte[] blob = SymmetricBox.encrypt(key, "wrapped".getBytes());
            assertEquals("wrapped", new String(SymmetricBox.decrypt(key, blob)), "sym round trip");
            blob[blob.length - 1] ^= 1;
            assertThrows(IllegalArgumentException.class, () -> SymmetricBox.decrypt(key, blob), "sym tamper");
        });

        test("codenames are 7 dictionary words and unique", () -> {
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < 200; i++) {
                String c = Codenames.generate();
                assertEquals(7, c.split(" ").length, "word count");
                seen.add(c);
            }
            assertEquals(200, seen.size(), "200 codenames should all differ");
        });

        test("codename normalisation is forgiving of case and whitespace", () -> {
            assertEquals("apple bread cedar", Codenames.normalize("  Apple   BREAD\tcedar "), "normalize");
            assertArrayEquals(Codenames.lookupIndex("Apple  Bread"), Codenames.lookupIndex("apple bread"), "same index");
        });

        test("codename hash is salted, deterministic, and login/reply keys differ", () -> {
            byte[] salt = Codenames.newSalt();
            byte[] h1 = Codenames.hash("one two three", salt);
            byte[] h2 = Codenames.hash("one two three", salt);
            byte[] h3 = Codenames.hash("one two three", Codenames.newSalt());
            assertArrayEquals(h1, h2, "deterministic");
            assertFalse(Arrays.equals(h1, h3), "different salt -> different hash");
            assertFalse(Arrays.equals(h1, Codenames.replyKey("one two three", salt)), "reply key != login hash");
            assertEquals(32, h1.length, "256-bit");
        });

        test("slow hash takes meaningful time (anti brute-force)", () -> {
            byte[] salt = Codenames.newSalt();
            long t0 = System.nanoTime();
            Codenames.hash("timing test words", salt);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            assertTrue(ms >= 20, "PBKDF2 should take >=20ms, took " + ms + "ms");
        });
    }
}
