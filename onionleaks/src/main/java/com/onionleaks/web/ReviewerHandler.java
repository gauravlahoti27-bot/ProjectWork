package com.onionleaks.web;

import com.onionleaks.service.RateLimiter;
import com.onionleaks.service.SourceService;
import com.onionleaks.service.SubmissionService;
import com.onionleaks.store.Source;
import com.onionleaks.store.Submission;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;

/**
 * Reviewer dashboard. Protected by username/password (constant-time compare, rate limited).
 * The reviewer only ever sees ciphertext here; .sealed containers are opened offline with DecryptTool.
 */
public class ReviewerHandler {

    public record SourceRow(UUID id, String shortId, long items, long pending, long replies, LocalDate firstSeen) {}

    static final String S_REVIEWER = "reviewer";

    private final SubmissionService submissions;
    private final SourceService sources;
    private final Sessions sessions;
    private final RateLimiter limiter;
    private final Views views;
    private final byte[] username;
    private final byte[] password;

    public ReviewerHandler(SubmissionService submissions, SourceService sources, Sessions sessions,
                           RateLimiter limiter, Views views, String username, String password) {
        this.submissions = submissions;
        this.sources = sources;
        this.sessions = sessions;
        this.limiter = limiter;
        this.views = views;
        this.username = username.getBytes(StandardCharsets.UTF_8);
        this.password = password.getBytes(StandardCharsets.UTF_8);
    }

    public boolean handle(HttpExchange ex, String method, String path) throws IOException {
        if (path.equals("/reviewer-login")) {
            if (method.equals("GET")) { Http.html(ex, 200, views.reviewerLogin(ex.getRequestURI().getQuery() != null)); return true; }
            if (method.equals("POST")) { login(ex); return true; }
            return false;
        }
        if (path.equals("/reviewer-logout") && method.equals("POST")) { sessions.destroy(ex); Http.redirect(ex, "/"); return true; }
        if (!path.startsWith("/reviewer")) return false;

        Sessions.Session s = sessions.current(ex);
        if (s == null || !Boolean.TRUE.equals(s.get(S_REVIEWER))) { Http.redirect(ex, "/reviewer-login"); return true; }

        if (path.equals("/reviewer") && method.equals("GET")) { dashboard(ex); return true; }
        if (path.startsWith("/reviewer/download/") && method.equals("GET")) { download(ex, path.substring("/reviewer/download/".length())); return true; }
        if (path.startsWith("/reviewer/source/")) {
            String rest = path.substring("/reviewer/source/".length());
            if (rest.endsWith("/reply") && method.equals("POST")) { reply(ex, rest.substring(0, rest.length() - "/reply".length())); return true; }
            if (method.equals("GET")) { source(ex, rest); return true; }
        }
        return false;
    }

    private void login(HttpExchange ex) throws IOException {
        Sessions.Session anon = sessions.getOrCreate(ex);
        if (!limiter.tryAcquire("reviewer-login:" + anon.id)) { Http.html(ex, 429, views.rateLimited()); return; }
        Http.Form f = Http.parseBody(ex, 16 * 1024);
        byte[] u = Optional.ofNullable(f.get("username")).orElse("").getBytes(StandardCharsets.UTF_8);
        byte[] p = Optional.ofNullable(f.get("password")).orElse("").getBytes(StandardCharsets.UTF_8);
        boolean ok = MessageDigest.isEqual(u, username) & MessageDigest.isEqual(p, password);
        if (!ok) { Http.redirect(ex, "/reviewer-login?error"); return; }
        Sessions.Session s = sessions.create(ex);
        s.set(S_REVIEWER, Boolean.TRUE);
        Http.redirect(ex, "/reviewer");
    }

    private void dashboard(HttpExchange ex) throws IOException {
        Map<UUID, List<Submission>> bySource = new LinkedHashMap<>();
        for (Submission s : submissions.all()) bySource.computeIfAbsent(s.sourceId(), k -> new ArrayList<>()).add(s);
        List<SourceRow> rows = new ArrayList<>();
        for (var e : bySource.entrySet()) {
            Source src = sources.byId(e.getKey()).orElse(null);
            if (src == null) continue;
            long pending = e.getValue().stream().filter(x -> !x.downloaded()).count();
            rows.add(new SourceRow(src.id(), shortId(src.id()), e.getValue().size(), pending, submissions.replyCount(src), src.createdDay()));
        }
        Http.html(ex, 200, views.dashboard(rows, submissions.pending()));
    }

    private void source(HttpExchange ex, String idStr) throws IOException {
        UUID id = parse(idStr);
        Source src = id == null ? null : sources.byId(id).orElse(null);
        if (src == null) { Http.html(ex, 404, views.error(404, "No such source.")); return; }
        boolean replied = "replied".equals(ex.getRequestURI().getQuery());
        Http.html(ex, 200, views.source(src.id(), shortId(src.id()), src.createdDay(), submissions.forSource(src), submissions.replyCount(src), replied));
    }

    private void download(HttpExchange ex, String idStr) throws IOException {
        UUID id = parse(idStr);
        Submission sub = id == null ? null : submissions.byId(id).orElse(null);
        if (sub == null) { Http.html(ex, 404, views.error(404, "No such item.")); return; }
        byte[] container = submissions.download(sub);
        ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + id + ".sealed\"");
        Http.send(ex, 200, "text/plain; charset=us-ascii", container);
    }

    private void reply(HttpExchange ex, String idStr) throws IOException {
        UUID id = parse(idStr);
        Source src = id == null ? null : sources.byId(id).orElse(null);
        if (src == null) { Http.html(ex, 404, views.error(404, "No such source.")); return; }
        Http.Form f = Http.parseBody(ex, 64 * 1024);
        try {
            submissions.reply(src, f.get("text"));
            Http.redirect(ex, "/reviewer/source/" + id + "?replied");
        } catch (IllegalArgumentException e) {
            Http.redirect(ex, "/reviewer/source/" + id);
        }
    }

    static UUID parse(String s) {
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }

    static String shortId(UUID id) { return id.toString().substring(0, 8).toUpperCase(Locale.ROOT); }
}
