package com.onionleaks.store;

import java.time.LocalDate;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;

/**
 * A reviewer -> source reply, sealed to the source's X25519 public key. Only a source presenting
 * the correct codename (which unwraps their private key) can open it; a disk copy reveals nothing.
 */
public record Reply(UUID id, UUID sourceId, byte[] sealed, LocalDate sentDay, boolean seen) {

    public Reply withSeen() { return new Reply(id, sourceId, sealed, sentDay, true); }

    Properties toProps() {
        Properties p = new Properties();
        p.setProperty("id", id.toString());
        p.setProperty("source", sourceId.toString());
        p.setProperty("sealed", Base64.getEncoder().encodeToString(sealed));
        p.setProperty("day", sentDay.toString());
        p.setProperty("seen", Boolean.toString(seen));
        return p;
    }

    static Reply fromProps(Properties p) {
        return new Reply(UUID.fromString(p.getProperty("id")), UUID.fromString(p.getProperty("source")),
                Base64.getDecoder().decode(p.getProperty("sealed")), LocalDate.parse(p.getProperty("day")),
                Boolean.parseBoolean(p.getProperty("seen")));
    }
}
