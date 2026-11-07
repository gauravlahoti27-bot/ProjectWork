package com.onionleaks.store;

import java.time.LocalDate;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;

/**
 * One anonymous source, identified only by a codename.
 *
 * Stored:  slow hash of codename (login), fast lookup index, salt,
 *          the source's X25519 public key (so reviewers can seal replies to it),
 *          the source's X25519 private key wrapped with a codename-derived AES key,
 *          the DAY (not time) it was created.
 * Not stored: IP, user agent, exact timestamps, anything typed in clear.
 */
public record Source(UUID id, byte[] lookupIndex, byte[] codenameHash, byte[] salt,
                     byte[] publicKey, byte[] wrappedPrivateKey, LocalDate createdDay) {

    Properties toProps() {
        Properties p = new Properties();
        p.setProperty("id", id.toString());
        p.setProperty("lookup", b64(lookupIndex));
        p.setProperty("hash", b64(codenameHash));
        p.setProperty("salt", b64(salt));
        p.setProperty("pub", b64(publicKey));
        p.setProperty("wrapped", b64(wrappedPrivateKey));
        p.setProperty("created", createdDay.toString());
        return p;
    }

    static Source fromProps(Properties p) {
        return new Source(UUID.fromString(p.getProperty("id")), un(p.getProperty("lookup")), un(p.getProperty("hash")),
                un(p.getProperty("salt")), un(p.getProperty("pub")), un(p.getProperty("wrapped")),
                LocalDate.parse(p.getProperty("created")));
    }

    static String b64(byte[] b) { return Base64.getEncoder().encodeToString(b); }
    static byte[] un(String s) { return Base64.getDecoder().decode(s); }
}
