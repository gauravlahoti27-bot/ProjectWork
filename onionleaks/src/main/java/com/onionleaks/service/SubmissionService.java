package com.onionleaks.service;

import com.onionleaks.crypto.SealedBox;
import com.onionleaks.store.FileStore;
import com.onionleaks.store.Reply;
import com.onionleaks.store.Source;
import com.onionleaks.store.Submission;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.LocalDate;
import java.util.*;

/** Encrypts incoming material to the reviewer's public key and stores only ciphertext. */
public class SubmissionService {

    public record UploadInput(byte[] bytes, String filename) {}
    public record Receipt(int items, List<String> unscrubbedFiles) {}

    private final FileStore store;
    private final MetadataScrubber scrubber;
    private final PublicKey reviewerKey;
    private final long maxFileBytes;

    public SubmissionService(FileStore store, MetadataScrubber scrubber, PublicKey reviewerKey, long maxFileBytes) {
        this.store = store;
        this.scrubber = scrubber;
        this.reviewerKey = reviewerKey;
        this.maxFileBytes = maxFileBytes;
    }

    public Receipt submit(Source source, String message, List<UploadInput> files) {
        int count = 0;
        List<String> unscrubbed = new ArrayList<>();

        if (message != null && !message.isBlank()) {
            storeSealed(source, Submission.Kind.MESSAGE, "message.txt", message.getBytes(StandardCharsets.UTF_8));
            count++;
        }
        for (UploadInput f : files) {
            if (f.bytes() == null || f.bytes().length == 0) continue;
            if (f.bytes().length > maxFileBytes)
                throw new IllegalArgumentException("File exceeds the " + (maxFileBytes / (1024 * 1024)) + " MB limit");
            MetadataScrubber.Result r = scrubber.scrub(f.bytes(), f.filename());
            if (!r.scrubbed()) unscrubbed.add(r.filename());
            storeSealed(source, Submission.Kind.FILE, r.filename(), r.bytes());
            count++;
        }
        if (count == 0) throw new IllegalArgumentException("Nothing to submit: write a message or attach a file");
        return new Receipt(count, unscrubbed);
    }

    private void storeSealed(Source source, Submission.Kind kind, String name, byte[] plaintext) {
        byte[] sealed = SealedBox.seal(plaintext, reviewerKey);
        byte[] sealedName = SealedBox.seal(name.getBytes(StandardCharsets.UTF_8), reviewerKey);
        Submission s = new Submission(UUID.randomUUID(), source.id(), kind, sealed.length, LocalDate.now(), false);
        store.saveSubmission(s, sealedName, sealed);
    }

    public List<Submission> all() { return store.submissions(); }
    public List<Submission> forSource(Source s) { return store.submissionsFor(s.id()); }
    public long countForSource(Source s) { return store.submissionsFor(s.id()).size(); }
    public long pending() { return store.submissions().stream().filter(x -> !x.downloaded()).count(); }
    public Optional<Submission> byId(UUID id) { return store.submission(id); }

    public byte[] download(Submission s) {
        byte[] container = store.sealedContainer(s.id());
        if (!s.downloaded()) store.updateSubmission(s.withDownloaded());
        return container;
    }

    /** Reviewer -> source. Sealed to the source's own public key. */
    public Reply reply(Source source, String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Reply is empty");
        PublicKey sourcePub = SealedBox.decodePublic(Base64.getEncoder().encodeToString(source.publicKey()));
        byte[] sealed = SealedBox.seal(text.getBytes(StandardCharsets.UTF_8), sourcePub);
        return store.saveReply(new Reply(UUID.randomUUID(), source.id(), sealed, LocalDate.now(), false));
    }

    public long replyCount(Source s) { return store.repliesFor(s.id()).size(); }
}
