# OnionLeaks — Privacy-Preserving Whistleblower Portal via Tor (.onion)

A full-stack Java 21 application that lets anonymous sources send messages and files to reviewers over a
Tor onion service, such that **neither the server operator nor a network observer can learn who the source is
or read what they sent.** Modelled on SecureDrop's architecture, reduced to a mini-project scale.

**Zero dependencies.** Built and run with nothing but the JDK: `com.sun.net.httpserver` for HTTP, `javax.crypto` for
X25519 + AES-GCM, `javax.imageio` for metadata scrubbing, plain files for storage. No Maven, no Gradle, no jars to download.

---

## 1. Features

| Area | What it does |
|---|---|
| Anonymous access | Served only as a v3 onion service. App binds to loopback / internal Docker network. No clearnet port. |
| No accounts | Source gets a **7-word codename** (≈ 57 bits entropy). No email, no password, no cookie that survives a restart. |
| End-to-end encryption | Every message and file is sealed to the reviewer's **X25519 public key** on arrival. The private key never exists on the server; reviewers decrypt **offline** with `DecryptTool`. |
| Encrypted filenames | The original filename is sealed separately — a disk dump shows only UUIDs. |
| Two-way anonymous replies | Each source gets its own X25519 keypair; the private half is wrapped under a key derived from the codename. Reviewer replies are sealed to the source's public key. |
| Metadata scrubbing | JPEG/PNG/GIF/BMP are re-encoded pixel-by-pixel, dropping EXIF/XMP/ICC/tEXt (GPS, camera serial, author). Other types are stored unchanged and the source is warned. |
| No logs | No access log, no IP, no User-Agent, no timestamps finer than the **day**. Error output is the exception class name only. |
| Hardening | CSP `default-src 'none'` (zero JavaScript — works at Tor Browser "Safest"), `Referrer-Policy: no-referrer`, `X-Frame-Options: DENY`, `Cache-Control: no-store`, no `Server` header, `HttpOnly; SameSite=Strict` session cookie, session rotation on login. |
| Abuse resistance | Per-session + global token-bucket rate limit (429), 25 MB file cap, PBKDF2-HMAC-SHA256 ×210 000 codename hash with constant-time compare and equal-time response for unknown codenames. |
| Reviewer dashboard | Password-protected inbox: sources, item counts, pending/downloaded status, `.sealed` download, encrypted reply. Reviewer sees **only ciphertext** in the browser. |

---

## 2. Architecture

```
            Tor network                              server (Docker)
 ┌─────────────┐    3-hop circuit     ┌───────────┐  internal net  ┌──────────────────────┐
 │ Tor Browser │ ───────────────────▶ │  tor      │ ─────────────▶ │  app  (Java 21)      │
 │ (source)    │   xyz…abc.onion:80   │ onion svc │   app:8080     │  Portal.java         │
 └─────────────┘                      └───────────┘                │   ├ SourceHandler    │
                                                                   │   ├ ReviewerHandler  │
 ┌─────────────┐                                                   │   ├ SealedBox (X25519│
 │ Tor Browser │ ───────────────────▶ (same onion, /reviewer)      │   │  + HKDF + AES-GCM)│
 │ (reviewer)  │                                                   │   └ FileStore        │
 └──────┬──────┘                                                   │        data/*.sealed │
        │ downloads .sealed                                        └──────────────────────┘
        ▼
 ┌─────────────────────┐
 │ Air-gapped laptop   │  java DecryptTool reviewer.key reviewer.pub downloads/
 │ holds reviewer.key  │  → plaintext files appear next to each .sealed
 └─────────────────────┘
```

### Crypto design (`SealedBox.java`)

```
ephemeral X25519 keypair  →  ECDH(eph_priv, recipient_pub)  →  shared secret
HKDF-SHA256(shared, salt = eph_pub ‖ recipient_pub, info = "onionleaks-v1")  →  256-bit AES key
AES-256-GCM(key, nonce = 12 random bytes, AAD = eph_pub)  →  ciphertext ‖ 128-bit tag

wire:  [0x01][44 B eph_pub (X.509)][12 B nonce][ciphertext+tag]
```

Anyone holding only the recipient's **public** key can seal; only the holder of the **private** key can open.
A fresh ephemeral key per message gives forward secrecy for the sealing side and makes identical plaintexts
produce unrelated ciphertexts.

### Codename design (`Codenames.java`)

- 7 words from a 280-word list → ≈ 57 bits, plus the slow hash and rate limit make online guessing infeasible.
- Stored per source: `SHA-256("lookup:"+codename)` (fast index), `PBKDF2(codename, salt‖"login")` (login check),
  and the source's X25519 private key wrapped with `PBKDF2(codename, salt‖"reply")` via AES-GCM.
- The server can read a source's replies **only while the source is presenting the codename** in that session.

### Storage layout (`FileStore.java`)

```
data/
  sources/<uuid>.properties        lookup, hash, salt, pub, wrapped-priv, created-day
  submissions/<uuid>.properties    source, kind, size, day, downloaded
  submissions/<uuid>.sealed        SEALEDNAME:<b64 sealed filename>\n<b64 sealed payload>
  replies/<uuid>.properties        source, sealed reply, day, seen
```

Every write is atomic (temp file + rename). The `#<timestamp>` comment `Properties` normally writes is stripped.

---

## 3. Build, test, run

Requires **JDK 21+** only.

```bash
./build.sh            # → build/onionleaks.jar
./build.sh test       # also compiles and runs the 37-test suite (crypto, services, HTTP end-to-end, CLI tools)
```

### 3.1 Reviewer keys — on the reviewer's machine, NOT the server

```bash
java -cp build/onionleaks.jar com.onionleaks.tool.KeyGenTool keys/
# keys/reviewer.pub  → goes to the server
# keys/reviewer.key  → stays offline, ideally on an air-gapped laptop
```

### 3.2 Run locally (development)

```bash
cat > .env <<EOF
PORTAL_REVIEWER_PUBLIC_KEY=$(cat keys/reviewer.pub)
PORTAL_REVIEWER_PASSWORD=choose-a-long-passphrase
PORTAL_ONION_ADDRESS=
EOF
java -jar build/onionleaks.jar .env
# http://127.0.0.1:8080  (source)      http://127.0.0.1:8080/reviewer-login  (reviewer)
```

### 3.3 Run as an onion service (Docker)

```bash
cp .env.example .env            # fill PORTAL_REVIEWER_PUBLIC_KEY and PORTAL_REVIEWER_PASSWORD
docker compose up -d --build
docker compose exec tor cat /var/lib/tor/onionleaks/hostname   # your .onion address
# put it in .env as PORTAL_ONION_ADDRESS and `docker compose up -d` again so it shows in the footer
```

Open the address in **Tor Browser**. The `app` container has no published ports and sits on an
`internal: true` network; only the `tor` container can reach it.

### 3.4 Decrypt submissions (offline)

Download the `.sealed` files from the reviewer dashboard, move them to the offline machine, then:

```bash
java -cp build/onionleaks.jar com.onionleaks.tool.DecryptTool keys/reviewer.key keys/reviewer.pub downloads/
# decrypted a1b2….sealed -> a1b2…__message.txt (40 bytes)
# decrypted c3d4….sealed -> c3d4…__ledger.csv  (1,204 bytes)
```

---

## 4. Configuration

| Variable | Default | Meaning |
|---|---|---|
| `PORTAL_REVIEWER_PUBLIC_KEY` | *(required)* | base64 X25519 public key from `KeyGenTool` |
| `PORTAL_REVIEWER_PASSWORD` | *(required)* | dashboard password |
| `PORTAL_REVIEWER_USERNAME` | `reviewer` | dashboard username |
| `PORTAL_DATA_DIR` | `./data` | where ciphertext is stored |
| `PORTAL_BIND` / `PORTAL_PORT` | `127.0.0.1` / `8080` | never bind to a public interface |
| `PORTAL_MAX_FILE_MB` | `25` | per-file upload cap |
| `PORTAL_ONION_ADDRESS` | — | shown in the footer |
| `PORTAL_ORG` | `OnionLeaks` | display name |

---

## 5. Threat model

| Adversary | What they can see | Mitigation |
|---|---|---|
| Source's ISP / employer network | Encrypted Tor traffic to guard node | Tor; onion services never exit the Tor network |
| Server's ISP / hosting provider | Encrypted Tor traffic from guard nodes | Onion service; no clearnet listener |
| Someone who steals the server disk | UUIDs, sizes, days, ciphertext | X25519 sealed boxes; private key offline; filenames sealed; replies sealed to source key |
| Malicious or compromised server admin | Same as above, plus live memory | Server never holds the reviewer private key; codename shown once and never stored in clear |
| Attacker guessing codenames | Login form | ≈ 57-bit codename, PBKDF2 ×210k, rate limit, constant-time compare, equal-time failure |
| Web attacker (XSS/CSRF/clickjacking) | Pages | CSP with no scripts, every value HTML-escaped, `SameSite=Strict`, `X-Frame-Options: DENY`, form-action self |
| DoS | HTTP | Token bucket per session + global, body size limits, Tor PoW defences in `torrc` |
| Metadata in uploads | File contents | Images re-encoded (EXIF/XMP/ICC dropped); other types flagged to the source |

### Out of scope / known limitations (state these in the report)

- Does not protect a **compromised source device** (keylogger, screenshots).
- No metadata stripping for PDF/Office files (production: run `mat2` or `exiftool` in the pipeline).
- In-memory sessions and rate-limit state: a restart forgets them (acceptable; intentional for sessions).
- Single reviewer identity; a real deployment would use per-reviewer keys and an audit trail on the offline side.
- No legal case management, no mobile app, no multi-language UI.
- Tor itself: timing correlation by a global passive adversary is outside what any onion service can prevent.

---

## 6. Project layout

```
src/main/java/com/onionleaks/
  Portal.java               HTTP server wiring, routing, static CSS, error handling
  Config.java               env / .env loader
  crypto/SealedBox.java     X25519 + HKDF-SHA256 + AES-256-GCM anonymous encryption
  crypto/Codenames.java     word list, PBKDF2 hashing, key derivation, constant-time compare
  crypto/SymmetricBox.java  AES-GCM wrapper used to wrap source private keys
  store/FileStore.java      atomic file-backed store + in-memory index
  store/{Source,Submission,Reply}.java   records + .properties (de)serialisation
  service/SourceService.java      create / login / read replies
  service/SubmissionService.java  seal + store, download, reply
  service/MetadataScrubber.java   image re-encoding
  service/RateLimiter.java        token bucket
  web/Http.java             form + binary-safe multipart parsing, cookies, hardened responses
  web/Sessions.java         in-memory random-id sessions
  web/Views.java            server-rendered HTML, mandatory escaping
  web/SourceHandler.java    /, /start, /login, /lookup, /submit, /sent, /logout
  web/ReviewerHandler.java  /reviewer-login, /reviewer, /reviewer/source/{id}, /reviewer/download/{id}, …/reply
  tool/KeyGenTool.java      offline reviewer key generation
  tool/DecryptTool.java     offline decryption of .sealed containers
src/main/resources/static/css/portal.css
src/test/java/com/onionleaks/   T (harness), CryptoTests, ServiceTests, HttpFlowTests, ToolTests, AllTests
tor/torrc, tor/Dockerfile       onion service configuration
Dockerfile, docker-compose.yml  two-container deployment (app on internal net, tor as the only ingress)
build.sh                        javac + jar + test runner
```

---

## 7. Test evidence

`./build.sh test` — 37 tests:

- **Crypto (12):** round-trip, 3 MB payload, non-deterministic ciphertext, wrong-key rejection, bit-flip detection at
  five offsets, malformed/version checks, key encoding, symmetric box, codename uniqueness/normalisation/hashing, PBKDF2 timing.
- **Services (11):** PNG `tEXt` chunk removal with pixel preservation, non-image passthrough, path traversal sanitising,
  rate limiter, binary-safe multipart, URL-decoding, login tolerance, **no plaintext/filenames anywhere on disk**,
  reviewer offline decrypt, rejection of empty/oversize, reply sealing + wrong-codename failure, restart persistence.
- **HTTP end-to-end (11):** real server on a random port; hardened headers, codename issuance, multipart submit
  (message + scrubbed PNG + CSV), empty-submit error, logout, reviewer auth + wrong password, download + decrypt of
  all items, reply round-trip with XSS payload escaped, bad codename, 429 after burst, 404s without stack traces.
- **CLI tools (3):** KeyGenTool output, DecryptTool success, DecryptTool wrong-key refusal.

A manual run with `curl` against the packaged jar reproduced the full source → reviewer → source loop and
`grep` over `data/` found no plaintext.
