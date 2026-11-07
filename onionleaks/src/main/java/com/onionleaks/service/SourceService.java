package com.onionleaks.service;

import com.onionleaks.crypto.Codenames;
import com.onionleaks.crypto.SealedBox;
import com.onionleaks.crypto.SymmetricBox;
import com.onionleaks.store.FileStore;
import com.onionleaks.store.Reply;
import com.onionleaks.store.Source;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDate;
import java.util.*;

/** Source lifecycle: create a codename, log in with it, read replies. */
public class SourceService {

    public record NewSource(Source source, String codename) {}
    public record DecryptedReply(String text, LocalDate sentDay, boolean wasNew) {}

    private final FileStore store;

    public SourceService(FileStore store) { this.store = store; }

    /** New source: fresh codename, fresh X25519 keypair, private half wrapped under the codename. */
    public NewSource create() {
        String codename;
        byte[] idx;
        do {
            codename = Codenames.generate();
            idx = Codenames.lookupIndex(codename);
        } while (store.sourceByLookup(idx).isPresent());

        byte[] salt = Codenames.newSalt();
        byte[] hash = Codenames.hash(codename, salt);
        byte[] wrapKey = Codenames.replyKey(codename, salt);
        KeyPair kp = SealedBox.generateKeyPair();
        byte[] wrapped = SymmetricBox.encrypt(wrapKey, kp.getPrivate().getEncoded());
        Arrays.fill(wrapKey, (byte) 0);

        Source s = store.saveSource(new Source(UUID.randomUUID(), idx, hash, salt,
                kp.getPublic().getEncoded(), wrapped, LocalDate.now()));
        return new NewSource(s, codename);
    }

    /** Constant-time compare on the slow hash; burns equal time for unknown codenames. */
    public Optional<Source> login(String codename) {
        String norm = Codenames.normalize(codename);
        if (norm.isEmpty()) return Optional.empty();
        Optional<Source> found = store.sourceByLookup(Codenames.lookupIndex(norm));
        if (found.isEmpty()) {
            Codenames.hash(norm, Codenames.newSalt());
            return Optional.empty();
        }
        Source s = found.get();
        byte[] candidate = Codenames.hash(norm, s.salt());
        return Codenames.constantTimeEquals(candidate, s.codenameHash()) ? Optional.of(s) : Optional.empty();
    }

    /** Unwraps the source's private key with the codename and opens every reply addressed to them. */
    public List<DecryptedReply> readReplies(Source s, String codename) {
        byte[] wrapKey = Codenames.replyKey(codename, s.salt());
        PrivateKey priv;
        try {
            priv = SealedBox.decodePrivate(Base64.getEncoder().encodeToString(SymmetricBox.decrypt(wrapKey, s.wrappedPrivateKey())));
        } finally {
            Arrays.fill(wrapKey, (byte) 0);
        }
        PublicKey pub = SealedBox.decodePublic(Base64.getEncoder().encodeToString(s.publicKey()));

        List<DecryptedReply> out = new ArrayList<>();
        for (Reply r : store.repliesFor(s.id())) {
            String text = new String(SealedBox.open(r.sealed(), priv, pub), StandardCharsets.UTF_8);
            out.add(new DecryptedReply(text, r.sentDay(), !r.seen()));
            if (!r.seen()) store.saveReply(r.withSeen());
        }
        return out;
    }

    public long unreadReplies(Source s) {
        return store.repliesFor(s.id()).stream().filter(r -> !r.seen()).count();
    }

    public Optional<Source> byId(UUID id) { return store.source(id); }
}
