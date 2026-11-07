package com.onionleaks.web;

import com.sun.net.httpserver.HttpExchange;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sessions keyed by a 256-bit random cookie. Nothing is written to disk, so a restart
 * forgets every session, which is exactly the behaviour a whistleblower portal wants.
 *
 * The cookie is HttpOnly + SameSite=Strict. It is NOT marked Secure because the site is served
 * as plain HTTP inside Tor; the onion service itself provides end-to-end encryption.
 */
public class Sessions {

    public static final String COOKIE = "ol_session";

    public static final class Session {
        public final String id;
        public final Map<String, Object> attrs = new ConcurrentHashMap<>();
        volatile Instant lastUsed = Instant.now();
        Session(String id) { this.id = id; }
        public Object get(String k) { return attrs.get(k); }
        public void set(String k, Object v) { attrs.put(k, v); }
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom rng = new SecureRandom();
    private final Duration ttl;

    public Sessions(Duration ttl) { this.ttl = ttl; }

    /** Existing session for this exchange or null. Touches it. */
    public Session current(HttpExchange ex) {
        String id = Http.cookie(ex, COOKIE);
        if (id == null) return null;
        Session s = sessions.get(id);
        if (s == null) return null;
        if (Duration.between(s.lastUsed, Instant.now()).compareTo(ttl) > 0) { sessions.remove(id); return null; }
        s.lastUsed = Instant.now();
        return s;
    }

    /** Always returns a session, creating one (and setting the cookie) if needed. */
    public Session getOrCreate(HttpExchange ex) {
        Session s = current(ex);
        if (s != null) return s;
        return create(ex);
    }

    /** Fresh session — used after login to prevent session fixation. */
    public Session create(HttpExchange ex) {
        byte[] b = new byte[32];
        rng.nextBytes(b);
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        Session s = new Session(id);
        sessions.put(id, s);
        ex.getResponseHeaders().add("Set-Cookie", COOKIE + "=" + id + "; Path=/; HttpOnly; SameSite=Strict");
        sweep();
        return s;
    }

    public void destroy(HttpExchange ex) {
        String id = Http.cookie(ex, COOKIE);
        if (id != null) sessions.remove(id);
        ex.getResponseHeaders().add("Set-Cookie", COOKIE + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict");
    }

    public int size() { return sessions.size(); }

    private void sweep() {
        if (sessions.size() < 1000) return;
        Instant cutoff = Instant.now().minus(ttl);
        sessions.values().removeIf(s -> s.lastUsed.isBefore(cutoff));
    }
}
