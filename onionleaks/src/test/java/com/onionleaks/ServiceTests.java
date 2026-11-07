package com.onionleaks;

import com.onionleaks.crypto.SealedBox;
import com.onionleaks.service.*;
import com.onionleaks.store.FileStore;
import com.onionleaks.store.Source;
import com.onionleaks.store.Submission;
import com.onionleaks.web.Http;

import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Base64;
import java.util.List;

import static com.onionleaks.T.*;

public class ServiceTests {

    public static void run() throws Exception {
        System.out.println("ServiceTests");

        test("metadata scrubber removes PNG text chunks (e.g. Author/GPS) and keeps pixels", () -> {
            byte[] tagged = pngWithTextChunk("Author", "Jane Whistleblower, 19.07N 72.87E");
            assertTrue(indexOf(tagged, "Jane Whistleblower".getBytes()) >= 0, "fixture must contain metadata");
            MetadataScrubber.Result r = new MetadataScrubber().scrub(tagged, "photo.png");
            assertTrue(r.scrubbed(), "png scrubbed");
            assertTrue(indexOf(r.bytes(), "Jane Whistleblower".getBytes()) < 0, "metadata gone");
            BufferedImage out = ImageIO.read(new java.io.ByteArrayInputStream(r.bytes()));
            assertEquals(8, out.getWidth(), "width preserved");
            assertEquals(0xFF336699, out.getRGB(2, 3), "pixel preserved");
        });

        test("metadata scrubber passes non-images through and flags them", () -> {
            byte[] pdf = "%PDF-1.4 /Author (someone)".getBytes();
            MetadataScrubber.Result r = new MetadataScrubber().scrub(pdf, "C:\\Users\\jane\\report.pdf");
            assertFalse(r.scrubbed(), "pdf not scrubbed");
            assertArrayEquals(pdf, r.bytes(), "bytes unchanged");
            assertEquals("report.pdf", r.filename(), "path stripped from filename");
        });

        test("filename sanitiser neutralises traversal and odd characters", () -> {
            assertEquals("etc_passwd", MetadataScrubber.sanitizeName("../../etc_passwd"), "traversal");
            assertEquals("a_b_.txt", MetadataScrubber.sanitizeName("a<b>.txt"), "angle brackets");
            assertEquals("file", MetadataScrubber.sanitizeName("   "), "blank");
        });

        test("rate limiter allows a burst then blocks, and isolates keys", () -> {
            RateLimiter rl = new RateLimiter(3, 0);
            assertTrue(rl.tryAcquire("a") && rl.tryAcquire("a") && rl.tryAcquire("a"), "3 allowed");
            assertFalse(rl.tryAcquire("a"), "4th blocked");
            assertTrue(rl.tryAcquire("b"), "other key unaffected");
        });

        test("multipart parser is binary-safe and handles multiple files", () -> {
            String b = "----XYZ";
            byte[] bin = new byte[256];
            for (int i = 0; i < 256; i++) bin[i] = (byte) i;      // includes CR, LF, '-' sequences
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            body.write(("--" + b + "\r\nContent-Disposition: form-data; name=\"message\"\r\n\r\nhello there\r\n").getBytes());
            body.write(("--" + b + "\r\nContent-Disposition: form-data; name=\"files\"; filename=\"bin.dat\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes());
            body.write(bin);
            body.write(("\r\n--" + b + "\r\nContent-Disposition: form-data; name=\"files\"; filename=\"\"\r\n\r\n\r\n").getBytes());
            body.write(("--" + b + "\r\nContent-Disposition: form-data; name=\"files\"; filename=\"t.txt\"\r\n\r\nabc\r\n").getBytes());
            body.write(("--" + b + "--\r\n").getBytes());
            Http.Form f = parseMultipart(body.toByteArray(), b);
            assertEquals("hello there", f.get("message"), "field");
            assertEquals(2, f.files().size(), "empty file part skipped");
            assertEquals("bin.dat", f.files().get(0).filename(), "name");
            assertArrayEquals(bin, f.files().get(0).bytes(), "binary intact");
            assertEquals("abc", new String(f.files().get(1).bytes()), "second file");
        });

        test("url-encoded parser decodes and groups repeated keys", () -> {
            var m = Http.parseUrlEncoded("a=1&b=x%20y&a=2&c=");
            assertEquals(List.of("1", "2"), m.get("a"), "repeat");
            assertEquals("x y", m.get("b").get(0), "decode");
            assertEquals("", m.get("c").get(0), "empty");
        });

        // ---------- store + services end to end (no HTTP) ----------
        Path dir = Files.createTempDirectory("ol-test");
        KeyPair reviewer = SealedBox.generateKeyPair();
        FileStore store = new FileStore(dir);
        SourceService sources = new SourceService(store);
        SubmissionService subs = new SubmissionService(store, new MetadataScrubber(), reviewer.getPublic(), 1024 * 1024);

        test("source creation -> login works with messy input, fails otherwise", () -> {
            SourceService.NewSource ns = sources.create();
            assertTrue(sources.login("  " + ns.codename().toUpperCase() + "  ").isPresent(), "login tolerant");
            assertTrue(sources.login(ns.codename() + " extra").isEmpty(), "wrong codename");
            assertTrue(sources.login("").isEmpty(), "empty");
            String[] w = ns.codename().split(" ");
            w[3] = w[3].equals("apple") ? "zebra" : "apple";
            assertTrue(sources.login(String.join(" ", w)).isEmpty(), "one word off");
        });

        test("submission is stored as ciphertext only; reviewer can decrypt offline", () -> {
            SourceService.NewSource ns = sources.create();
            String secret = "Board approved the bribe on 3 March. See attached ledger.";
            byte[] ledger = "ACCOUNT,AMOUNT\nSLUSH,900000".getBytes();
            SubmissionService.Receipt r = subs.submit(ns.source(), secret, List.of(new SubmissionService.UploadInput(ledger, "ledger.csv")));
            assertEquals(2, r.items(), "two items");
            assertEquals(List.of("ledger.csv"), r.unscrubbedFiles(), "csv flagged as unscrubbed");

            // Nothing on disk contains the plaintext.
            try (var files = Files.walk(dir)) {
                for (Path p : files.filter(Files::isRegularFile).toList()) {
                    byte[] bytes = Files.readAllBytes(p);
                    assertTrue(indexOf(bytes, "bribe".getBytes()) < 0, "plaintext leaked in " + p);
                    assertTrue(indexOf(bytes, "SLUSH".getBytes()) < 0, "file leaked in " + p);
                    assertTrue(indexOf(bytes, "ledger.csv".getBytes()) < 0, "filename leaked in " + p);
                }
            }
            // Reviewer flow: download container, open with private key.
            List<Submission> items = subs.forSource(ns.source());
            assertEquals(2, items.size(), "listed");
            int found = 0;
            for (Submission s : items) {
                String[] lines = new String(subs.download(s), StandardCharsets.US_ASCII).split("\n");
                String name = new String(SealedBox.open(Base64.getDecoder().decode(lines[0].substring("SEALEDNAME:".length())), reviewer.getPrivate(), reviewer.getPublic()));
                byte[] body = SealedBox.open(Base64.getDecoder().decode(lines[1]), reviewer.getPrivate(), reviewer.getPublic());
                if (name.equals("message.txt")) { assertEquals(secret, new String(body), "message"); found++; }
                if (name.equals("ledger.csv")) { assertArrayEquals(ledger, body, "ledger"); found++; }
            }
            assertEquals(2, found, "both decrypted");
            assertTrue(subs.forSource(ns.source()).stream().allMatch(Submission::downloaded), "marked downloaded");
        });

        test("empty submission and oversize file are rejected", () -> {
            Source s = sources.create().source();
            assertThrows(IllegalArgumentException.class, () -> subs.submit(s, "   ", List.of()), "empty");
            assertThrows(IllegalArgumentException.class, () -> subs.submit(s, null, List.of(new SubmissionService.UploadInput(new byte[2 * 1024 * 1024], "big.bin"))), "oversize");
        });

        test("reply is sealed to the source and readable only with the codename", () -> {
            SourceService.NewSource ns = sources.create();
            subs.reply(ns.source(), "We received it. Can you confirm the year?");
            assertEquals(1L, sources.unreadReplies(ns.source()), "one unread");
            try (var files = Files.walk(dir.resolve("replies"))) {
                for (Path p : files.filter(Files::isRegularFile).toList())
                    assertTrue(indexOf(Files.readAllBytes(p), "confirm the year".getBytes()) < 0, "reply plaintext on disk");
            }
            var replies = sources.readReplies(ns.source(), ns.codename());
            assertEquals(1, replies.size(), "read");
            assertEquals("We received it. Can you confirm the year?", replies.get(0).text(), "text");
            assertTrue(replies.get(0).wasNew(), "flagged new first time");
            assertEquals(0L, sources.unreadReplies(ns.source()), "now seen");
            assertFalse(sources.readReplies(ns.source(), ns.codename()).get(0).wasNew(), "not new second time");
            assertThrows(IllegalArgumentException.class, () -> sources.readReplies(ns.source(), "wrong codename words"), "wrong codename can't unwrap");
        });

        test("store survives restart (reload from disk)", () -> {
            long before = store.submissions().size();
            FileStore reloaded = new FileStore(dir);
            assertEquals(before, (long) reloaded.submissions().size(), "submissions reloaded");
            assertEquals(store.sources().size(), reloaded.sources().size(), "sources reloaded");
            assertTrue(Files.readString(dir.resolve("sources").resolve(store.sources().get(0).id() + ".properties")).lines().noneMatch(l -> l.startsWith("#")),
                    "no timestamp comment written to disk");
        });
    }

    // ---------- helpers ----------

    static Http.Form parseMultipart(byte[] body, String boundary) throws Exception {
        var m = Http.class.getDeclaredMethod("parseMultipart", byte[].class, String.class);
        m.setAccessible(true);
        return (Http.Form) m.invoke(null, body, boundary);
    }

    static int indexOf(byte[] hay, byte[] needle) {
        outer:
        for (int i = 0; i <= hay.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) if (hay[i + j] != needle[j]) continue outer;
            return i;
        }
        return -1;
    }

    /** 8x8 PNG with a tEXt chunk, the PNG equivalent of EXIF author/GPS tags. */
    static byte[] pngWithTextChunk(String key, String value) throws Exception {
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) img.setRGB(x, y, 0x336699);
        ImageWriter w = ImageIO.getImageWritersByFormatName("png").next();
        IIOMetadata meta = w.getDefaultImageMetadata(new javax.imageio.ImageTypeSpecifier(img), null);
        IIOMetadataNode text = new IIOMetadataNode("tEXt");
        IIOMetadataNode entry = new IIOMetadataNode("tEXtEntry");
        entry.setAttribute("keyword", key);
        entry.setAttribute("value", value);
        text.appendChild(entry);
        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_png_1.0");
        root.appendChild(text);
        meta.mergeTree("javax_imageio_png_1.0", root);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            w.setOutput(ios);
            w.write(null, new IIOImage(img, null, meta), null);
        }
        return out.toByteArray();
    }
}
