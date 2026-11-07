package com.onionleaks.web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Small helpers around HttpExchange: form decoding, multipart parsing, cookies, responses.
 * No external libraries; everything the portal needs from HTTP fits here.
 */
public final class Http {

    private Http() {}

    public record Upload(String filename, byte[] bytes) {}

    /** Parsed request body: ordinary fields plus uploaded files. */
    public record Form(Map<String, List<String>> fields, List<Upload> files) {
        public String get(String k) {
            List<String> v = fields.get(k);
            return v == null || v.isEmpty() ? null : v.get(0);
        }
    }

    public static Form parseBody(HttpExchange ex, long maxBytes) throws IOException {
        String ct = Optional.ofNullable(ex.getRequestHeaders().getFirst("Content-Type")).orElse("");
        byte[] body = readLimited(ex.getRequestBody(), maxBytes);
        if (ct.startsWith("multipart/form-data")) {
            String boundary = param(ct, "boundary");
            if (boundary == null) throw new IOException("multipart without boundary");
            return parseMultipart(body, boundary);
        }
        return new Form(parseUrlEncoded(new String(body, StandardCharsets.UTF_8)), List.of());
    }

    public static Map<String, List<String>> parseUrlEncoded(String s) {
        Map<String, List<String>> m = new LinkedHashMap<>();
        if (s == null || s.isEmpty()) return m;
        for (String pair : s.split("&")) {
            int i = pair.indexOf('=');
            String k = URLDecoder.decode(i < 0 ? pair : pair.substring(0, i), StandardCharsets.UTF_8);
            String v = i < 0 ? "" : URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8);
            m.computeIfAbsent(k, x -> new ArrayList<>()).add(v);
        }
        return m;
    }

    /** RFC 7578 multipart/form-data. Binary-safe: works on byte offsets, never decodes the payload. */
    static Form parseMultipart(byte[] body, String boundary) throws IOException {
        Map<String, List<String>> fields = new LinkedHashMap<>();
        List<Upload> files = new ArrayList<>();
        byte[] delim = ("--" + boundary).getBytes(StandardCharsets.US_ASCII);

        int pos = indexOf(body, delim, 0);
        if (pos < 0) throw new IOException("bad multipart");
        pos += delim.length;
        while (true) {
            if (pos + 2 <= body.length && body[pos] == '-' && body[pos + 1] == '-') break;    // closing --
            pos = skipCrlf(body, pos);
            int headerEnd = indexOf(body, "\r\n\r\n".getBytes(StandardCharsets.US_ASCII), pos);
            if (headerEnd < 0) throw new IOException("bad multipart part");
            String headers = new String(body, pos, headerEnd - pos, StandardCharsets.ISO_8859_1);
            int dataStart = headerEnd + 4;
            int next = indexOf(body, delim, dataStart);
            if (next < 0) throw new IOException("unterminated multipart part");
            int dataEnd = next - 2;                                                           // strip CRLF before delimiter
            if (dataEnd < dataStart) dataEnd = dataStart;

            String disposition = headerLine(headers, "content-disposition");
            String name = param(disposition, "name");
            String filename = param(disposition, "filename");
            byte[] data = Arrays.copyOfRange(body, dataStart, dataEnd);
            if (filename != null) {
                if (!filename.isEmpty() && data.length > 0) files.add(new Upload(filename, data));
            } else if (name != null) {
                fields.computeIfAbsent(name, x -> new ArrayList<>()).add(new String(data, StandardCharsets.UTF_8));
            }
            pos = next + delim.length;
        }
        return new Form(fields, files);
    }

    private static String headerLine(String headers, String name) {
        for (String line : headers.split("\r\n"))
            if (line.toLowerCase(Locale.ROOT).startsWith(name + ":")) return line.substring(name.length() + 1).trim();
        return "";
    }

    /** Extract `key=value` or `key="value"` from a header value such as a Content-Type or Content-Disposition. */
    static String param(String header, String key) {
        if (header == null) return null;
        for (String part : header.split(";")) {
            String p = part.trim();
            int eq = p.indexOf('=');
            if (eq < 0) continue;
            if (!p.substring(0, eq).trim().equalsIgnoreCase(key)) continue;
            String v = p.substring(eq + 1).trim();
            if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
            return v;
        }
        return null;
    }

    private static int skipCrlf(byte[] b, int pos) {
        if (pos + 1 < b.length && b[pos] == '\r' && b[pos + 1] == '\n') return pos + 2;
        return pos;
    }

    static int indexOf(byte[] hay, byte[] needle, int from) {
        outer:
        for (int i = Math.max(0, from); i <= hay.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) if (hay[i + j] != needle[j]) continue outer;
            return i;
        }
        return -1;
    }

    static byte[] readLimited(InputStream in, long max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[64 * 1024];
        long total = 0;
        int n;
        while ((n = in.read(buf)) > 0) {
            total += n;
            if (total > max) throw new IOException("request body too large");
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    // ---------- cookies ----------

    public static String cookie(HttpExchange ex, String name) {
        List<String> all = ex.getRequestHeaders().get("Cookie");
        if (all == null) return null;
        for (String line : all)
            for (String c : line.split(";")) {
                String t = c.trim();
                if (t.startsWith(name + "=")) return t.substring(name.length() + 1);
            }
        return null;
    }

    // ---------- responses ----------

    public static void html(HttpExchange ex, int status, String body) throws IOException {
        send(ex, status, "text/html; charset=utf-8", body.getBytes(StandardCharsets.UTF_8));
    }

    public static void send(HttpExchange ex, int status, String contentType, byte[] body) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", contentType);
        // Hardening: no scripts at all, no referrer leakage, no framing, no caching, no MIME sniffing.
        h.set("Content-Security-Policy", "default-src 'none'; style-src 'self'; img-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'");
        h.set("Referrer-Policy", "no-referrer");
        h.set("X-Frame-Options", "DENY");
        h.set("X-Content-Type-Options", "nosniff");
        h.set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream out = ex.getResponseBody()) { out.write(body); }
    }

    public static void redirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().set("Location", location);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(303, -1);
        ex.close();
    }
}
