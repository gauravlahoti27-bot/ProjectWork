package com.onionleaks;

import com.onionleaks.crypto.SealedBox;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.onionleaks.T.*;

/** Boots the real Portal on a random loopback port and drives it over HTTP like a browser would. */
public class HttpFlowTests {

    static String base;
    static Portal portal;
    static KeyPair reviewer;

    public static void run() throws Exception {
        System.out.println("HttpFlowTests");

        reviewer = SealedBox.generateKeyPair();
        Path data = Files.createTempDirectory("ol-http");
        Config cfg = Config.load(Map.of(
                "PORTAL_REVIEWER_PUBLIC_KEY", SealedBox.encodePublic(reviewer.getPublic()),
                "PORTAL_REVIEWER_PASSWORD", "hunter2-strong",
                "PORTAL_DATA_DIR", data.toString(),
                "PORTAL_PORT", "0",
                "PORTAL_ONION_ADDRESS", "abc123.onion"));
        portal = new Portal(cfg);
        portal.start();
        base = "http://127.0.0.1:" + portal.port();

        try {
            Client source = new Client();
            Client rev = new Client();
            String[] codename = new String[1];

            test("home page renders with hardened headers and no scripts", () -> {
                HttpResponse<String> r = source.get("/");
                assertEquals(200, r.statusCode(), "status");
                assertTrue(r.body().contains("Share information safely"), "content");
                assertTrue(r.body().contains("abc123.onion"), "onion shown");
                assertFalse(r.body().contains("<script"), "no scripts");
                assertEquals("default-src 'none'; style-src 'self'; img-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'",
                        r.headers().firstValue("Content-Security-Policy").orElse(""), "CSP");
                assertEquals("no-referrer", r.headers().firstValue("Referrer-Policy").orElse(""), "referrer");
                assertEquals("no-store", r.headers().firstValue("Cache-Control").orElse(""), "cache");
                assertTrue(r.headers().firstValue("Server").isEmpty(), "no Server header");
                assertEquals(200, source.get("/css/portal.css").statusCode(), "css served");
                assertEquals(404, source.get("/css/../Portal.class").statusCode(), "traversal blocked");
            });

            test("POST /start issues a 7-word codename and a session cookie", () -> {
                HttpResponse<String> r = source.post("/start", "");
                assertEquals(200, r.statusCode(), "status");
                Matcher m = Pattern.compile("<div class=\"codename\">([a-z ]+)</div>").matcher(r.body());
                assertTrue(m.find(), "codename shown");
                codename[0] = m.group(1);
                assertEquals(7, codename[0].split(" ").length, "7 words");
                assertTrue(source.cookies.containsKey("ol_session"), "session cookie set");
                assertTrue(r.headers().allValues("Set-Cookie").stream().allMatch(c -> c.contains("HttpOnly") && c.contains("SameSite=Strict")), "cookie flags");
            });

            test("source submits a message + image + csv via multipart", () -> {
                byte[] png = ServiceTests.pngWithTextChunk("Author", "Jane Whistleblower");
                Multipart mp = new Multipart()
                        .field("message", "Here is the <b>proof</b> & ledger")
                        .file("files", "evidence.png", png)
                        .file("files", "ledger.csv", "a,b\n1,2".getBytes());
                HttpResponse<String> r = source.send(source.req("/submit")
                        .header("Content-Type", mp.contentType())
                        .POST(HttpRequest.BodyPublishers.ofByteArray(mp.bytes())).build());
                assertEquals(303, r.statusCode(), "redirect after submit");
                assertEquals("/sent", r.headers().firstValue("Location").orElse(""), "to /sent");
                HttpResponse<String> sent = source.get("/sent");
                assertTrue(sent.body().contains("<strong>3</strong> item(s)"), "3 items: " + sent.body());
                assertTrue(sent.body().contains("ledger.csv"), "csv flagged unscrubbed");
                assertFalse(sent.body().contains("evidence.png"), "png was scrubbed, not flagged");
            });

            test("empty submission shows an error instead of storing", () -> {
                Multipart mp = new Multipart().field("message", "   ");
                HttpResponse<String> r = source.send(source.req("/submit").header("Content-Type", mp.contentType())
                        .POST(HttpRequest.BodyPublishers.ofByteArray(mp.bytes())).build());
                assertEquals(200, r.statusCode(), "re-render");
                assertTrue(r.body().contains("Nothing to submit"), "error shown");
            });

            test("logout clears the session; /lookup then redirects to login", () -> {
                assertEquals(303, source.post("/logout", "").statusCode(), "logout");
                HttpResponse<String> r = source.get("/lookup");
                assertEquals(303, r.statusCode(), "redirect");
                assertEquals("/login", r.headers().firstValue("Location").orElse(""), "to login");
            });

            test("reviewer area requires login; wrong password is rejected", () -> {
                HttpResponse<String> r = rev.get("/reviewer");
                assertEquals(303, r.statusCode(), "redirect");
                assertEquals("/reviewer-login", r.headers().firstValue("Location").orElse(""), "to reviewer login");
                HttpResponse<String> bad = rev.post("/reviewer-login", "username=reviewer&password=wrong");
                assertEquals("/reviewer-login?error", bad.headers().firstValue("Location").orElse(""), "rejected");
                assertEquals(303, rev.get("/reviewer").statusCode(), "still locked out");
            });

            String[] sourceId = new String[1];
            test("reviewer logs in, sees the source, downloads and decrypts all three items", () -> {
                HttpResponse<String> ok = rev.post("/reviewer-login", "username=reviewer&password=hunter2-strong");
                assertEquals("/reviewer", ok.headers().firstValue("Location").orElse(""), "login ok");
                HttpResponse<String> dash = rev.get("/reviewer");
                assertEquals(200, dash.statusCode(), "dashboard");
                assertTrue(dash.body().contains("<strong>3</strong> item(s) not yet downloaded"), "3 pending");
                Matcher m = Pattern.compile("/reviewer/source/([0-9a-f-]{36})").matcher(dash.body());
                assertTrue(m.find(), "source link");
                sourceId[0] = m.group(1);

                HttpResponse<String> page = rev.get("/reviewer/source/" + sourceId[0]);
                assertEquals(200, page.statusCode(), "source page");
                Matcher d = Pattern.compile("/reviewer/download/([0-9a-f-]{36})").matcher(page.body());
                Map<String, byte[]> got = new HashMap<>();
                while (d.find()) {
                    HttpResponse<byte[]> dl = rev.getBytes("/reviewer/download/" + d.group(1));
                    assertEquals(200, dl.statusCode(), "download");
                    assertTrue(dl.headers().firstValue("Content-Disposition").orElse("").contains(".sealed"), "attachment");
                    String[] lines = new String(dl.body(), StandardCharsets.US_ASCII).split("\n");
                    String name = new String(SealedBox.open(Base64.getDecoder().decode(lines[0].substring("SEALEDNAME:".length())), reviewer.getPrivate(), reviewer.getPublic()));
                    got.put(name, SealedBox.open(Base64.getDecoder().decode(lines[1]), reviewer.getPrivate(), reviewer.getPublic()));
                }
                assertEquals(Set.of("message.txt", "evidence.png", "ledger.csv"), got.keySet(), "all items");
                assertEquals("Here is the <b>proof</b> & ledger", new String(got.get("message.txt")), "message intact");
                assertEquals("a,b\n1,2", new String(got.get("ledger.csv")), "csv intact");
                assertTrue(ServiceTests.indexOf(got.get("evidence.png"), "Jane Whistleblower".getBytes()) < 0, "png metadata stripped");
                assertTrue(rev.get("/reviewer").body().contains("<strong>0</strong> item(s) not yet downloaded"), "none pending");
            });

            test("reviewer reply is escaped in HTML and readable by the source after codename login", () -> {
                HttpResponse<String> r = rev.post("/reviewer/source/" + sourceId[0] + "/reply",
                        "text=" + URLEncoder.encode("Thanks. <script>alert(1)</script> Which quarter?", StandardCharsets.UTF_8));
                assertEquals(303, r.statusCode(), "reply stored");

                Client again = new Client();
                HttpResponse<String> login = again.post("/login", "codename=" + URLEncoder.encode(codename[0].toUpperCase(), StandardCharsets.UTF_8));
                assertEquals("/lookup", login.headers().firstValue("Location").orElse(""), "codename login");
                HttpResponse<String> lookup = again.get("/lookup");
                assertEquals(200, lookup.statusCode(), "lookup");
                assertTrue(lookup.body().contains("You have sent <strong>3</strong>"), "count");
                assertTrue(lookup.body().contains("Thanks. &lt;script&gt;alert(1)&lt;/script&gt; Which quarter?"), "reply shown escaped");
                assertFalse(lookup.body().contains("<script>alert"), "no raw script");
            });

            test("wrong codename is rejected with a message", () -> {
                Client c = new Client();
                HttpResponse<String> r = c.post("/login", "codename=apple+bread+cedar+delta+eagle+fable+giant");
                assertEquals(200, r.statusCode(), "re-render");
                assertTrue(r.body().contains("not recognised"), "error");
            });

            test("rate limiter returns 429 after a burst from one session", () -> {
                Client c = new Client();
                c.post("/start", "");                       // get a session + source
                int last = 0;
                for (int i = 0; i < 15; i++) last = c.post("/login", "codename=x").statusCode();
                assertEquals(429, last, "429 after burst");
            });

            test("unknown routes 404 and errors never leak stack traces", () -> {
                HttpResponse<String> r = source.get("/does-not-exist");
                assertEquals(404, r.statusCode(), "404");
                assertFalse(r.body().contains("Exception"), "no internals");
                assertEquals(404, rev.get("/reviewer/source/not-a-uuid").statusCode(), "bad uuid");
            });
        } finally {
            portal.stop();
        }
    }

    // ---------- tiny cookie-aware client ----------

    static final class Client {
        final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        final Map<String, String> cookies = new LinkedHashMap<>();

        HttpRequest.Builder req(String path) {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
            if (!cookies.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                cookies.forEach((k, v) -> sb.append(k).append('=').append(v).append("; "));
                b.header("Cookie", sb.toString());
            }
            return b;
        }
        HttpResponse<String> get(String path) throws Exception { return send(req(path).GET().build()); }
        HttpResponse<String> post(String path, String form) throws Exception {
            return send(req(path).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)).build());
        }
        HttpResponse<String> send(HttpRequest r) throws Exception {
            HttpResponse<String> resp = http.send(r, HttpResponse.BodyHandlers.ofString());
            absorb(resp.headers().allValues("Set-Cookie"));
            return resp;
        }
        HttpResponse<byte[]> getBytes(String path) throws Exception {
            HttpResponse<byte[]> resp = http.send(req(path).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            absorb(resp.headers().allValues("Set-Cookie"));
            return resp;
        }
        void absorb(List<String> setCookies) {
            for (String sc : setCookies) {
                String kv = sc.split(";")[0];
                int eq = kv.indexOf('=');
                String k = kv.substring(0, eq), v = kv.substring(eq + 1);
                if (sc.contains("Max-Age=0") || v.isEmpty()) cookies.remove(k); else cookies.put(k, v);
            }
        }
    }

    static final class Multipart {
        final String boundary = "----ol" + UUID.randomUUID();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        Multipart field(String name, String value) throws Exception {
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n").getBytes(StandardCharsets.UTF_8));
            return this;
        }
        Multipart file(String name, String filename, byte[] bytes) throws Exception {
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(bytes);
            out.write("\r\n".getBytes());
            return this;
        }
        String contentType() { return "multipart/form-data; boundary=" + boundary; }
        byte[] bytes() throws Exception { out.write(("--" + boundary + "--\r\n").getBytes()); return out.toByteArray(); }
    }
}
