package com.onionleaks.service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * Removes identifying metadata from uploads before they are encrypted.
 *
 *  - JPEG/PNG/GIF/BMP: fully re-encoded pixel-by-pixel, which drops EXIF (GPS, camera serial,
 *    author), XMP, ICC profiles, comments and thumbnails. Lossless for PNG; JPEG is re-compressed.
 *  - Everything else: passed through unchanged but flagged so the source is warned in the UI.
 *    (A production system would also run mat2/exiftool for PDF/Office documents.)
 */
public class MetadataScrubber {

    public record Result(byte[] bytes, String filename, boolean scrubbed) {}

    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "bmp");

    public Result scrub(byte[] input, String originalName) {
        String name = sanitizeName(originalName);
        String ext = extensionOf(name);
        if (!IMAGE_EXT.contains(ext)) {
            return new Result(input, name, false);
        }
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(input));
            if (img == null) return new Result(input, name, false);
            String fmt = ext.equals("jpg") ? "jpeg" : ext;
            if (fmt.equals("jpeg") || fmt.equals("bmp")) {                 // no alpha channel in these formats
                BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
                rgb.getGraphics().drawImage(img, 0, 0, java.awt.Color.WHITE, null);
                img = rgb;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream(input.length);
            if (!ImageIO.write(img, fmt, out)) return new Result(input, name, false);
            return new Result(out.toByteArray(), name, true);
        } catch (IOException e) {
            return new Result(input, name, false);
        }
    }

    /** Strip paths and odd characters; the name is encrypted anyway but keep it tidy for the reviewer. */
    public static String sanitizeName(String n) {
        if (n == null || n.isBlank()) return "file";
        String base = n.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (base.length() > 100) base = base.substring(base.length() - 100);
        return base.isEmpty() ? "file" : base;
    }

    public static String extensionOf(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase(Locale.ROOT);
    }
}
