package com.onionleaks.store;

import java.time.LocalDate;
import java.util.Properties;
import java.util.UUID;

/**
 * Metadata for one encrypted item. The ciphertext itself lives in <id>.sealed next to this record.
 * Everything a reviewer needs (text, filename) is inside the sealed box.
 */
public record Submission(UUID id, UUID sourceId, Kind kind, long ciphertextBytes, LocalDate receivedDay, boolean downloaded) {

    public enum Kind { MESSAGE, FILE }

    public Submission withDownloaded() {
        return new Submission(id, sourceId, kind, ciphertextBytes, receivedDay, true);
    }

    Properties toProps() {
        Properties p = new Properties();
        p.setProperty("id", id.toString());
        p.setProperty("source", sourceId.toString());
        p.setProperty("kind", kind.name());
        p.setProperty("bytes", Long.toString(ciphertextBytes));
        p.setProperty("day", receivedDay.toString());
        p.setProperty("downloaded", Boolean.toString(downloaded));
        return p;
    }

    static Submission fromProps(Properties p) {
        return new Submission(UUID.fromString(p.getProperty("id")), UUID.fromString(p.getProperty("source")),
                Kind.valueOf(p.getProperty("kind")), Long.parseLong(p.getProperty("bytes")),
                LocalDate.parse(p.getProperty("day")), Boolean.parseBoolean(p.getProperty("downloaded")));
    }
}
