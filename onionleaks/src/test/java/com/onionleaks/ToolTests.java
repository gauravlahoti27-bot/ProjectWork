package com.onionleaks;

import com.onionleaks.crypto.SealedBox;
import com.onionleaks.tool.DecryptTool;
import com.onionleaks.tool.KeyGenTool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.Base64;

import static com.onionleaks.T.*;

/** The offline CLI tools the reviewer runs on an air-gapped machine. */
public class ToolTests {

    public static void run() throws Exception {
        System.out.println("ToolTests");

        Path keys = Files.createTempDirectory("ol-keys");
        test("KeyGenTool writes a usable keypair", () -> {
            KeyGenTool.main(new String[]{keys.toString()});
            assertTrue(Files.exists(keys.resolve("reviewer.pub")), "pub written");
            assertTrue(Files.exists(keys.resolve("reviewer.key")), "key written");
            PublicKey pub = SealedBox.decodePublic(Files.readString(keys.resolve("reviewer.pub")));
            var priv = SealedBox.decodePrivate(Files.readString(keys.resolve("reviewer.key")));
            assertEquals("ok", new String(SealedBox.open(SealedBox.seal("ok".getBytes(), pub), priv, pub)), "keys pair up");
        });

        test("DecryptTool opens a downloaded .sealed container into the original file", () -> {
            PublicKey pub = SealedBox.decodePublic(Files.readString(keys.resolve("reviewer.pub")));
            Path dl = Files.createTempDirectory("ol-dl");
            String container = "SEALEDNAME:" + Base64.getEncoder().encodeToString(SealedBox.seal("ledger.csv".getBytes(), pub)) + "\n"
                    + Base64.getEncoder().encodeToString(SealedBox.seal("x,y\n1,2".getBytes(), pub)) + "\n";
            Files.writeString(dl.resolve("item1.sealed"), container);
            Files.writeString(dl.resolve("junk.sealed"), "not a container\n");
            DecryptTool.main(new String[]{keys.resolve("reviewer.key").toString(), keys.resolve("reviewer.pub").toString(), dl.toString()});
            assertEquals("x,y\n1,2", Files.readString(dl.resolve("item1__ledger.csv")), "decrypted file content");
        });

        test("DecryptTool refuses with the wrong key", () -> {
            Path other = Files.createTempDirectory("ol-keys2");
            KeyGenTool.main(new String[]{other.toString()});
            PublicKey pub = SealedBox.decodePublic(Files.readString(keys.resolve("reviewer.pub")));
            Path dl = Files.createTempDirectory("ol-dl2");
            Files.writeString(dl.resolve("a.sealed"), "SEALEDNAME:" + Base64.getEncoder().encodeToString(SealedBox.seal("n".getBytes(), pub)) + "\n"
                    + Base64.getEncoder().encodeToString(SealedBox.seal("s".getBytes(), pub)) + "\n");
            assertThrows(IllegalArgumentException.class, () -> DecryptTool.main(new String[]{
                    other.resolve("reviewer.key").toString(), other.resolve("reviewer.pub").toString(), dl.toString()}), "wrong key");
        });
    }
}
