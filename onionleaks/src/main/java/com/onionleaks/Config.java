package com.onionleaks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * All configuration comes from environment variables (or a .env-style properties file passed as
 * the first argument). Nothing sensitive is baked into the jar.
 *
 *   PORTAL_REVIEWER_PUBLIC_KEY   base64 X25519 public key from KeyGenTool  (required)
 *   PORTAL_REVIEWER_USERNAME     dashboard login, default "reviewer"
 *   PORTAL_REVIEWER_PASSWORD     dashboard password                         (required)
 *   PORTAL_DATA_DIR              where ciphertext lives, default ./data
 *   PORTAL_BIND                  default 127.0.0.1 (never expose directly)
 *   PORTAL_PORT                  default 8080
 *   PORTAL_MAX_FILE_MB           default 25
 *   PORTAL_ONION_ADDRESS         shown in the footer once Tor has generated it
 *   PORTAL_ORG                   display name, default "OnionLeaks"
 */
public record Config(String reviewerPublicKey, String reviewerUsername, String reviewerPassword,
                     Path dataDir, String bind, int port, long maxFileBytes, String onionAddress, String org) {

    public static Config load(Map<String, String> env) {
        String pub = req(env, "PORTAL_REVIEWER_PUBLIC_KEY", "Run KeyGenTool and set PORTAL_REVIEWER_PUBLIC_KEY");
        String pass = req(env, "PORTAL_REVIEWER_PASSWORD", "Set PORTAL_REVIEWER_PASSWORD for the reviewer dashboard");
        return new Config(
                pub,
                env.getOrDefault("PORTAL_REVIEWER_USERNAME", "reviewer"),
                pass,
                Path.of(env.getOrDefault("PORTAL_DATA_DIR", "data")),
                env.getOrDefault("PORTAL_BIND", "127.0.0.1"),
                Integer.parseInt(env.getOrDefault("PORTAL_PORT", "8080")),
                Long.parseLong(env.getOrDefault("PORTAL_MAX_FILE_MB", "25")) * 1024 * 1024,
                env.getOrDefault("PORTAL_ONION_ADDRESS", ""),
                env.getOrDefault("PORTAL_ORG", "OnionLeaks"));
    }

    /** Env vars win; a KEY=VALUE file (if given) fills in the rest. */
    public static Config load(Path envFile) throws java.io.IOException {
        Map<String, String> m = new java.util.HashMap<>();
        if (envFile != null && Files.exists(envFile)) {
            for (String line : Files.readAllLines(envFile)) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#")) continue;
                int eq = t.indexOf('=');
                if (eq > 0) m.put(t.substring(0, eq).trim(), t.substring(eq + 1).trim());
            }
        }
        m.putAll(System.getenv());
        return load(m);
    }

    private static String req(Map<String, String> env, String key, String help) {
        String v = env.get(key);
        if (v == null || v.isBlank()) throw new IllegalStateException(key + " is not set. " + help);
        return v;
    }
}
