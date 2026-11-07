package com.onionleaks.tool;

import com.onionleaks.crypto.SealedBox;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;

/**
 * Generates the reviewer keypair. Run OFFLINE on the reviewer's machine:
 *
 *   java -cp onionleaks.jar -Dloader.main=com.onionleaks.tool.KeyGenTool org.springframework.boot.loader.launch.PropertiesLauncher keys/
 *
 * or during development:  mvn -q exec:java -Dexec.mainClass=com.onionleaks.tool.KeyGenTool -Dexec.args=keys/
 *
 * Copy ONLY reviewer.pub to the server (REVIEWER_PUBLIC_KEY). Keep reviewer.key offline.
 */
public class KeyGenTool {
    public static void main(String[] args) throws Exception {
        Path dir = Path.of(args.length > 0 ? args[0] : "keys");
        Files.createDirectories(dir);
        KeyPair kp = SealedBox.generateKeyPair();
        Path pub = dir.resolve("reviewer.pub");
        Path priv = dir.resolve("reviewer.key");
        Files.writeString(pub, SealedBox.encodePublic(kp.getPublic()) + "\n", StandardCharsets.US_ASCII);
        Files.writeString(priv, SealedBox.encodePrivate(kp.getPrivate()) + "\n", StandardCharsets.US_ASCII);
        try { Files.setPosixFilePermissions(priv, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")); } catch (Exception ignored) {}
        System.out.println("Public key  -> " + pub.toAbsolutePath() + "  (put on server as REVIEWER_PUBLIC_KEY)");
        System.out.println("Private key -> " + priv.toAbsolutePath() + "  (NEVER copy to server)");
    }
}
