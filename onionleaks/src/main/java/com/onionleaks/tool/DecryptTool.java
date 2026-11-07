package com.onionleaks.tool;

import com.onionleaks.crypto.SealedBox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

/**
 * Offline decryptor. Takes the .sealed files downloaded from the reviewer dashboard and the
 * reviewer private key, writes plaintext next to each file.
 *
 *   java -cp onionleaks.jar -Dloader.main=com.onionleaks.tool.DecryptTool \
 *        org.springframework.boot.loader.launch.PropertiesLauncher keys/reviewer.key keys/reviewer.pub downloads/
 *
 * A .sealed file is a tiny line-based container:
 *   line 1: "SEALEDNAME:" + base64 sealed box holding the original filename (or "message.txt")
 *   line 2: base64 sealed box holding the content
 */
public class DecryptTool {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("usage: DecryptTool <reviewer.key> <reviewer.pub> <file-or-dir> [...]");
            System.exit(2);
        }
        PrivateKey priv = SealedBox.decodePrivate(Files.readString(Path.of(args[0])));
        PublicKey pub = SealedBox.decodePublic(Files.readString(Path.of(args[1])));

        for (int i = 2; i < args.length; i++) {
            Path p = Path.of(args[i]);
            List<Path> files;
            try (Stream<Path> s = Files.isDirectory(p) ? Files.walk(p) : Stream.of(p)) {
                files = s.filter(f -> f.toString().endsWith(".sealed")).toList();
            }
            for (Path f : files) decryptOne(f, priv, pub);
        }
    }

    static void decryptOne(Path f, PrivateKey priv, PublicKey pub) throws IOException {
        List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
        if (lines.size() < 2 || !lines.get(0).startsWith("SEALEDNAME:")) { System.err.println("skip (bad container): " + f); return; }
        byte[] nameBox = Base64.getDecoder().decode(lines.get(0).substring("SEALEDNAME:".length()).trim());
        String name = new String(SealedBox.open(nameBox, priv, pub), StandardCharsets.UTF_8).replaceAll("[^A-Za-z0-9._-]", "_");
        byte[] plain = SealedBox.open(Base64.getDecoder().decode(lines.get(1).trim()), priv, pub);
        Path out = f.resolveSibling(f.getFileName().toString().replace(".sealed", "") + "__" + name);
        Files.write(out, plain);
        System.out.println("decrypted " + f.getFileName() + " -> " + out.getFileName() + " (" + plain.length + " bytes)");
    }
}
