package com.onionleaks.web;

import com.onionleaks.service.RateLimiter;
import com.onionleaks.service.SourceService;
import com.onionleaks.service.SubmissionService;
import com.onionleaks.store.Source;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The anonymous source side. No accounts, no persistent cookies, no JavaScript.
 * Session holds only the source id and the codename (needed to unwrap replies), in memory.
 */
public class SourceHandler {

    static final String S_SOURCE = "src.id";
    static final String S_CODENAME = "src.codename";
    static final String S_RECEIPT = "src.receipt";

    private final SourceService sources;
    private final SubmissionService submissions;
    private final RateLimiter limiter;
    private final Sessions sessions;
    private final Views views;
    private final long maxRequestBytes;

    public SourceHandler(SourceService sources, SubmissionService submissions, RateLimiter limiter,
                         Sessions sessions, Views views, long maxRequestBytes) {
        this.sources = sources;
        this.submissions = submissions;
        this.limiter = limiter;
        this.sessions = sessions;
        this.views = views;
        this.maxRequestBytes = maxRequestBytes;
    }

    /** Returns true if this handler served the request. */
    public boolean handle(HttpExchange ex, String method, String path) throws IOException {
        switch (path) {
            case "/" -> { if (method.equals("GET")) { Http.html(ex, 200, views.index()); return true; } }
            case "/start" -> {
                if (method.equals("GET")) { Http.html(ex, 200, views.start()); return true; }
                if (method.equals("POST")) { createCodename(ex); return true; }
            }
            case "/login" -> {
                if (method.equals("GET")) { Http.html(ex, 200, views.login(null)); return true; }
                if (method.equals("POST")) { login(ex); return true; }
            }
            case "/lookup" -> { if (method.equals("GET")) { lookup(ex, null); return true; } }
            case "/submit" -> { if (method.equals("POST")) { submit(ex); return true; } }
            case "/sent" -> { if (method.equals("GET")) { sent(ex); return true; } }
            case "/logout" -> { if (method.equals("POST")) { sessions.destroy(ex); Http.redirect(ex, "/"); return true; } }
            default -> { return false; }
        }
        return false;
    }

    private void createCodename(HttpExchange ex) throws IOException {
        Sessions.Session anon = sessions.getOrCreate(ex);
        if (!limiter.tryAcquire(anon.id)) { Http.html(ex, 429, views.rateLimited()); return; }
        SourceService.NewSource ns = sources.create();
        Sessions.Session s = sessions.create(ex);                  // fresh id once a source exists
        s.set(S_SOURCE, ns.source().id());
        s.set(S_CODENAME, ns.codename());
        Http.html(ex, 200, views.codename(ns.codename()));
    }

    private void login(HttpExchange ex) throws IOException {
        Sessions.Session anon = sessions.getOrCreate(ex);
        if (!limiter.tryAcquire(anon.id)) { Http.html(ex, 429, views.rateLimited()); return; }
        Http.Form f = Http.parseBody(ex, 16 * 1024);
        String codename = f.get("codename");
        Optional<Source> src = sources.login(codename);
        if (src.isEmpty()) { Http.html(ex, 200, views.login("That codename was not recognised. Check spelling and spacing.")); return; }
        Sessions.Session s = sessions.create(ex);                  // rotate id on login (anti-fixation)
        s.set(S_SOURCE, src.get().id());
        s.set(S_CODENAME, codename);
        Http.redirect(ex, "/lookup");
    }

    private void lookup(HttpExchange ex, String error) throws IOException {
        Sessions.Session s = sessions.current(ex);
        Source src = current(s);
        if (src == null) { Http.redirect(ex, "/login"); return; }
        List<SourceService.DecryptedReply> replies = sources.readReplies(src, (String) s.get(S_CODENAME));
        long unread = replies.stream().filter(SourceService.DecryptedReply::wasNew).count();
        Http.html(ex, 200, views.lookup(submissions.countForSource(src), unread, replies, error));
    }

    private void submit(HttpExchange ex) throws IOException {
        Sessions.Session s = sessions.current(ex);
        Source src = current(s);
        if (src == null) { Http.redirect(ex, "/login"); return; }
        if (!limiter.tryAcquire(s.id)) { Http.html(ex, 429, views.rateLimited()); return; }

        Http.Form f;
        try {
            f = Http.parseBody(ex, maxRequestBytes);
        } catch (IOException e) {
            Http.html(ex, 413, views.error(413, "Upload too large or malformed."));
            return;
        }
        List<SubmissionService.UploadInput> uploads = new ArrayList<>();
        for (Http.Upload u : f.files()) uploads.add(new SubmissionService.UploadInput(u.bytes(), u.filename()));
        try {
            SubmissionService.Receipt r = submissions.submit(src, f.get("message"), uploads);
            s.set(S_RECEIPT, r);
            Http.redirect(ex, "/sent");
        } catch (IllegalArgumentException e) {
            Http.html(ex, 200, views.lookup(submissions.countForSource(src), 0, List.of(), e.getMessage()));
        }
    }

    private void sent(HttpExchange ex) throws IOException {
        Sessions.Session s = sessions.current(ex);
        if (current(s) == null) { Http.redirect(ex, "/"); return; }
        Object r = s.attrs.remove(S_RECEIPT);
        if (!(r instanceof SubmissionService.Receipt receipt)) { Http.redirect(ex, "/lookup"); return; }
        Http.html(ex, 200, views.sent(receipt));
    }

    private Source current(Sessions.Session s) {
        if (s == null) return null;
        Object id = s.get(S_SOURCE);
        if (!(id instanceof UUID uuid)) return null;
        return sources.byId(uuid).orElse(null);
    }
}
