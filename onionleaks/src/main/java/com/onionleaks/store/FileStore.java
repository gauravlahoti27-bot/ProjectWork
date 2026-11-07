package com.onionleaks.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Durable storage with no database: one directory per record type, one .properties file per
 * record, and one .sealed file per submission payload. Loaded into memory at startup, every
 * write goes to disk atomically (write temp, then move). Like SecureDrop, the "database" is
 * just a directory of ciphertext that can be backed up, inspected or wiped with ordinary tools.
 *
 *   data/
 *     sources/<uuid>.properties
 *     submissions/<uuid>.properties + <uuid>.sealed
 *     replies/<uuid>.properties
 */
public class FileStore {

    private final Path root;
    private final Map<UUID, Source> sources = new ConcurrentHashMap<>();
    private final Map<String, UUID> sourceByLookup = new ConcurrentHashMap<>();   // base64(lookupIndex) -> id
    private final Map<UUID, Submission> submissions = new ConcurrentHashMap<>();
    private final Map<UUID, Reply> replies = new ConcurrentHashMap<>();

    public FileStore(Path root) {
        this.root = root;
        try {
            Files.createDirectories(dir("sources"));
            Files.createDirectories(dir("submissions"));
            Files.createDirectories(dir("replies"));
            load("sources", p -> { Source s = Source.fromProps(p); sources.put(s.id(), s); sourceByLookup.put(Source.b64(s.lookupIndex()), s.id()); });
            load("submissions", p -> { Submission s = Submission.fromProps(p); submissions.put(s.id(), s); });
            load("replies", p -> { Reply r = Reply.fromProps(p); replies.put(r.id(), r); });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------- sources ----------

    public synchronized Source saveSource(Source s) {
        write(dir("sources").resolve(s.id() + ".properties"), s.toProps());
        sources.put(s.id(), s);
        sourceByLookup.put(Source.b64(s.lookupIndex()), s.id());
        return s;
    }

    public Optional<Source> source(UUID id) { return Optional.ofNullable(sources.get(id)); }

    public Optional<Source> sourceByLookup(byte[] lookupIndex) {
        UUID id = sourceByLookup.get(Source.b64(lookupIndex));
        return id == null ? Optional.empty() : source(id);
    }

    public List<Source> sources() { return new ArrayList<>(sources.values()); }

    // ---------- submissions ----------

    public synchronized Submission saveSubmission(Submission s, byte[] sealedName, byte[] sealedPayload) {
        Path sealed = dir("submissions").resolve(s.id() + ".sealed");
        String container = "SEALEDNAME:" + Base64.getEncoder().encodeToString(sealedName) + "\n"
                + Base64.getEncoder().encodeToString(sealedPayload) + "\n";
        writeAtomic(sealed, container.getBytes());
        write(dir("submissions").resolve(s.id() + ".properties"), s.toProps());
        submissions.put(s.id(), s);
        return s;
    }

    public synchronized Submission updateSubmission(Submission s) {
        write(dir("submissions").resolve(s.id() + ".properties"), s.toProps());
        submissions.put(s.id(), s);
        return s;
    }

    public Optional<Submission> submission(UUID id) { return Optional.ofNullable(submissions.get(id)); }

    /** The raw .sealed container bytes, exactly what the reviewer downloads. */
    public byte[] sealedContainer(UUID id) {
        try {
            return Files.readAllBytes(dir("submissions").resolve(id + ".sealed"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<Submission> submissions() {
        List<Submission> l = new ArrayList<>(submissions.values());
        l.sort(Comparator.comparing(Submission::receivedDay).reversed().thenComparing(x -> x.id().toString()));
        return l;
    }

    public List<Submission> submissionsFor(UUID sourceId) {
        return submissions().stream().filter(s -> s.sourceId().equals(sourceId)).toList();
    }

    // ---------- replies ----------

    public synchronized Reply saveReply(Reply r) {
        write(dir("replies").resolve(r.id() + ".properties"), r.toProps());
        replies.put(r.id(), r);
        return r;
    }

    public List<Reply> repliesFor(UUID sourceId) {
        List<Reply> l = replies.values().stream().filter(r -> r.sourceId().equals(sourceId)).collect(java.util.stream.Collectors.toList());
        l.sort(Comparator.comparing(Reply::sentDay).thenComparing(x -> x.id().toString()));
        return l;
    }

    // ---------- helpers ----------

    private Path dir(String name) { return root.resolve(name); }

    private interface Loader { void accept(Properties p); }

    private void load(String d, Loader loader) throws IOException {
        try (Stream<Path> s = Files.list(dir(d))) {
            for (Path p : s.filter(f -> f.toString().endsWith(".properties")).toList()) {
                Properties props = new Properties();
                try (InputStream in = Files.newInputStream(p)) { props.load(in); }
                loader.accept(props);
            }
        }
    }

    private void write(Path file, Properties props) {
        try {
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStream out = Files.newOutputStream(tmp)) {
                props.store(out, null);          // note: Properties.store writes a date comment; stripped below
            }
            // Remove the "#<date>" header line so no precise timestamp hits the disk.
            List<String> lines = Files.readAllLines(tmp);
            lines.removeIf(l -> l.startsWith("#"));
            Files.write(tmp, lines);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeAtomic(Path file, byte[] bytes) {
        try {
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(tmp, bytes);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
