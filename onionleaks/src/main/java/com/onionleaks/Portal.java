package com.onionleaks;

import com.onionleaks.crypto.SealedBox;
import com.onionleaks.service.*;
import com.onionleaks.store.FileStore;
import com.onionleaks.web.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * Wires everything together on the JDK's built-in HTTP server. No frameworks, no dependencies:
 * every byte that touches a source's data is in this repository.
 *
 * Deliberately absent: access logging, request timing, error stack traces to the client,
 * a Server header. Tor sits in front and is the only way in (see tor/torrc).
 */
public class Portal {

    private final HttpServer server;
    private final Config cfg;

    public Portal(Config cfg) throws IOException {
        this.cfg = cfg;
        FileStore store = new FileStore(cfg.dataDir());
        Sessions sessions = new Sessions(Duration.ofMinutes(30));
        RateLimiter limiter = new RateLimiter();
        Views views = new Views(cfg.org(), cfg.onionAddress(), cfg.maxFileBytes());
        SourceService sourceService = new SourceService(store);
        SubmissionService submissionService = new SubmissionService(store, new MetadataScrubber(),
                SealedBox.decodePublic(cfg.reviewerPublicKey()), cfg.maxFileBytes());
        SourceHandler sourceHandler = new SourceHandler(sourceService, submissionService, limiter, sessions, views,
                cfg.maxFileBytes() * 4 + 1024 * 1024);
        ReviewerHandler reviewerHandler = new ReviewerHandler(submissionService, sourceService, sessions, limiter, views,
                cfg.reviewerUsername(), cfg.reviewerPassword());

        server = HttpServer.create(new InetSocketAddress(cfg.bind(), cfg.port()), 64);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.createContext("/", ex -> route(ex, sourceHandler, reviewerHandler, views));
    }

    private void route(HttpExchange ex, SourceHandler src, ReviewerHandler rev, Views views) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        try {
            if (path.startsWith("/css/")) { staticFile(ex, path); return; }
            if (src.handle(ex, method, path)) return;
            if (rev.handle(ex, method, path)) return;
            Http.html(ex, 404, views.error(404, "Page not found."));
        } catch (Exception e) {
            // Never leak internals. Log only the class name, never request content.
            System.err.println("ERROR " + e.getClass().getSimpleName());
            try { Http.html(ex, 500, views.error(500, "Something went wrong. Please try again.")); } catch (IOException ignored) {}
        } finally {
            ex.close();
        }
    }

    private void staticFile(HttpExchange ex, String path) throws IOException {
        String res = "/static" + path;
        if (path.contains("..") || !path.endsWith(".css")) { ex.sendResponseHeaders(404, -1); return; }
        try (InputStream in = Portal.class.getResourceAsStream(res)) {
            if (in == null) { ex.sendResponseHeaders(404, -1); return; }
            Http.send(ex, 200, "text/css; charset=utf-8", in.readAllBytes());
        }
    }

    public void start() {
        server.start();
        System.out.println("OnionLeaks listening on http://" + cfg.bind() + ":" + port() + "  (data: " + cfg.dataDir().toAbsolutePath() + ")");
    }

    public void stop() { server.stop(0); }

    public int port() { return server.getAddress().getPort(); }

    public static void main(String[] args) throws IOException {
        Config cfg = Config.load(args.length > 0 ? Path.of(args[0]) : Path.of(".env"));
        Portal p = new Portal(cfg);
        p.start();
        Runtime.getRuntime().addShutdownHook(new Thread(p::stop));
    }
}
