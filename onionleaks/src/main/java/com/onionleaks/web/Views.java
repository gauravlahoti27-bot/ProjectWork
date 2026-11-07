package com.onionleaks.web;

import com.onionleaks.service.SourceService;
import com.onionleaks.service.SubmissionService;
import com.onionleaks.store.Submission;
import com.onionleaks.web.ReviewerHandler.SourceRow;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Server-rendered HTML with zero JavaScript, so every page works in Tor Browser at "Safest".
 * Every dynamic value passes through esc(); there is no raw-insertion path.
 */
public final class Views {

    private final String org;
    private final String onion;
    private final long maxMb;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy");

    public Views(String org, String onion, long maxFileBytes) {
        this.org = org;
        this.onion = onion == null ? "" : onion;
        this.maxMb = maxFileBytes / (1024 * 1024);
    }

    public static String esc(Object o) {
        if (o == null) return "";
        String s = o.toString();
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String day(LocalDate d) { return esc(d.format(DAY)); }

    // ---------- layout ----------

    public String page(String title, String main) {
        return """
            <!DOCTYPE html>
            <html lang="en"><head>
            <meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
            <meta name="referrer" content="no-referrer">
            <title>%s · %s</title>
            <link rel="stylesheet" href="/css/portal.css">
            </head><body><div class="wrap">
            <header class="top">
              <a class="brand" href="/"><span>◉</span> %s</a>
              <nav><a href="/start">New submission</a><a href="/login">Check replies</a></nav>
            </header>
            <main>%s</main>
            <footer>
              <p>This service is reachable only as a Tor onion service. Nothing you type is logged, and every submission
                 is encrypted before it is stored, with a key the server does not have.</p>
              %s
            </footer>
            </div></body></html>
            """.formatted(esc(title), esc(org), esc(org), main,
                onion.isEmpty() ? "" : "<p class=\"mono\">" + esc(onion) + "</p>");
    }

    // ---------- source pages ----------

    public String index() {
        return page("Home", """
            <h1>Share information safely.</h1>
            <p class="lead">This portal lets you send documents and messages to our reviewers without revealing who you are.
               We cannot see your IP address, your location, or anything about your device.</p>
            <div class="panel">
              <h2 style="margin-top:0">Before you begin</h2>
              <ol class="steps">
                <li>Use <strong>Tor Browser</strong>. If you can read this page at a <span class="mono">.onion</span> address, you already are.</li>
                <li>Do not use a work computer or work network.</li>
                <li>Do not tell anyone, including us, your real name unless you choose to inside the message.</li>
                <li>You will receive a <strong>codename</strong>. Write it down somewhere safe; it is the only way to see our replies.</li>
              </ol>
              <div class="row">
                <a class="btn" href="/start">Make a new submission</a>
                <a class="btn secondary" href="/login">I already have a codename</a>
              </div>
            </div>
            <h2>How your submission is protected</h2>
            <p>Everything you send is encrypted on arrival with the reviewers' public key. The private key never touches this server,
               so even a full copy of its storage is unreadable. Images are re-encoded to strip EXIF data such as GPS coordinates.
               Only the day of receipt is recorded, never the time.</p>
            """);
    }

    public String start() {
        return page("New submission", """
            <h1>Create your codename</h1>
            <p class="lead">A codename is seven random words. It is generated once and shown once.
               It is your only credential: no email, no password, no account.</p>
            <div class="panel"><form method="post" action="/start">
              <button class="btn" type="submit">Generate my codename</button>
            </form></div>
            """);
    }

    public String codename(String codename) {
        return page("Your codename", """
            <h1>Your codename</h1>
            <p class="lead">Copy this somewhere safe. It will not be shown again and cannot be recovered.</p>
            <div class="codename">%s</div>
            <div class="alert">Anyone who knows this codename can read replies meant for you. Do not store it on a work device.</div>
            %s
            """.formatted(esc(codename), submitForm("What would you like us to know? You can also just attach files.")));
    }

    public String login(String error) {
        return page("Check replies", """
            <h1>Enter your codename</h1>
            <p class="lead">Use the seven words you were given to see replies and send follow-ups.</p>
            %s
            <div class="panel"><form method="post" action="/login">
              <label for="codename">Codename</label>
              <input id="codename" name="codename" type="text" autocomplete="off" spellcheck="false" placeholder="seven words separated by spaces" required>
              <div class="row" style="margin-top:16px"><button class="btn" type="submit">Continue</button></div>
            </form></div>
            """.formatted(error == null ? "" : "<div class=\"alert err\">" + esc(error) + "</div>"));
    }

    public String lookup(long submissions, long unread, List<SourceService.DecryptedReply> replies, String error) {
        StringBuilder r = new StringBuilder();
        if (replies.isEmpty()) r.append("<p class=\"hint\">No replies yet. Check back in a few days.</p>");
        for (var rep : replies)
            r.append("<div class=\"msg\"><div class=\"meta\">").append(day(rep.sentDay()))
             .append(rep.wasNew() ? " · new" : "").append("</div>").append(esc(rep.text())).append("</div>");
        return page("Your conversation", """
            <div class="row" style="justify-content:space-between"><h1>Your conversation</h1>
              <form method="post" action="/logout"><button class="btn secondary small" type="submit">Log out</button></form></div>
            <p class="lead">You have sent <strong>%d</strong> item(s). %s</p>
            %s
            <h2>Replies from reviewers</h2>
            %s
            <h2>Send a follow-up</h2>
            %s
            """.formatted(submissions,
                unread > 0 ? "You have <strong>" + unread + "</strong> new reply(ies)." : "",
                error == null ? "" : "<div class=\"alert err\">" + esc(error) + "</div>",
                r, submitForm("")));
    }

    private String submitForm(String placeholder) {
        return """
            <div class="panel"><form method="post" action="/submit" enctype="multipart/form-data">
              <label for="message">Message</label>
              <textarea id="message" name="message" placeholder="%s"></textarea>
              <label for="files">Files (optional)</label>
              <input id="files" name="files" type="file" multiple>
              <p class="hint">Up to %d MB per file. JPEG/PNG/GIF/BMP images have metadata removed automatically.
                 For PDFs and office documents, strip metadata yourself first if it matters.</p>
              <div class="row" style="margin-top:16px"><button class="btn" type="submit">Submit securely</button></div>
            </form></div>
            """.formatted(esc(placeholder), maxMb);
    }

    public String sent(SubmissionService.Receipt receipt) {
        String warn = receipt.unscrubbedFiles().isEmpty() ? "" :
                "<div class=\"alert\">These files were stored as-is because automatic metadata removal only covers images: <span class=\"mono\">"
                + esc(String.join(", ", receipt.unscrubbedFiles())) + "</span>.</div>";
        return page("Sent", """
            <h1>Received and encrypted.</h1>
            <div class="alert ok"><strong>%d</strong> item(s) sealed to the reviewers' key and stored.</div>
            %s
            <p>Reviewers will see your submission the next time they check in. Come back with your codename to read any reply.</p>
            <div class="row"><a class="btn" href="/lookup">Back to my conversation</a>
              <form method="post" action="/logout"><button class="btn secondary" type="submit">Log out and clear session</button></form></div>
            """.formatted(receipt.items(), warn));
    }

    public String rateLimited() {
        return page("Slow down", """
            <h1>Too many requests</h1>
            <p class="lead">Please wait a minute and try again. This limit protects the service from automated abuse.</p>
            <a class="btn secondary" href="/">Back</a>
            """);
    }

    public String error(int code, String msg) {
        return page("Error " + code, "<h1>" + code + "</h1><p class=\"lead\">" + esc(msg) + "</p><a class=\"btn secondary\" href=\"/\">Home</a>");
    }

    // ---------- reviewer pages ----------

    public String reviewerLogin(boolean failed) {
        return page("Reviewer login", """
            <h1>Reviewer login</h1>
            %s
            <div class="panel"><form method="post" action="/reviewer-login">
              <label for="username">Username</label><input id="username" name="username" type="text" autocomplete="username" required>
              <label for="password">Password</label><input id="password" name="password" type="password" autocomplete="current-password" required>
              <div class="row" style="margin-top:16px"><button class="btn" type="submit">Sign in</button></div>
            </form></div>
            """.formatted(failed ? "<div class=\"alert err\">Invalid username or password.</div>" : ""));
    }

    public String dashboard(List<SourceRow> rows, long totalPending) {
        StringBuilder tb = new StringBuilder();
        for (SourceRow r : rows)
            tb.append("<tr><td class=\"mono\">").append(esc(r.shortId())).append("</td><td>").append(day(r.firstSeen()))
              .append("</td><td>").append(r.items()).append("</td><td><span class=\"tag ")
              .append(r.pending() > 0 ? "pending" : "done").append("\">").append(r.pending()).append("</span></td><td>")
              .append(r.replies()).append("</td><td><a class=\"btn small\" href=\"/reviewer/source/").append(r.id())
              .append("\">Open</a></td></tr>");
        if (rows.isEmpty()) tb.append("<tr><td colspan=\"6\" class=\"hint\">No submissions yet.</td></tr>");
        return page("Reviewer dashboard", """
            <div class="row" style="justify-content:space-between"><h1>Inbox</h1>
              <form method="post" action="/reviewer-logout"><button class="btn secondary small" type="submit">Sign out</button></form></div>
            <p class="lead"><strong>%d</strong> item(s) not yet downloaded. Everything below is ciphertext;
               decrypt offline with <span class="mono">DecryptTool</span>.</p>
            <table><thead><tr><th>Source</th><th>First seen</th><th>Items</th><th>Pending</th><th>Replies</th><th></th></tr></thead>
            <tbody>%s</tbody></table>
            """.formatted(totalPending, tb));
    }

    public String source(UUID id, String shortId, LocalDate createdDay, List<Submission> items, long replies, boolean replied) {
        StringBuilder tb = new StringBuilder();
        for (Submission s : items)
            tb.append("<tr><td>").append(day(s.receivedDay())).append("</td><td>").append(s.kind()).append("</td><td>")
              .append(String.format("%.1f KB", s.ciphertextBytes() / 1024.0)).append("</td><td><span class=\"tag ")
              .append(s.downloaded() ? "done\">downloaded" : "pending\">new").append("</span></td><td><a class=\"btn small secondary\" href=\"/reviewer/download/")
              .append(s.id()).append("\">Download .sealed</a></td></tr>");
        return page("Source " + shortId, """
            <p><a href="/reviewer">← Inbox</a></p>
            <h1>Source <span class="mono">%s</span></h1>
            <p class="lead">First seen %s. %d reply(ies) sent.</p>
            %s
            <h2>Encrypted items</h2>
            <table><thead><tr><th>Received</th><th>Kind</th><th>Size</th><th>Status</th><th></th></tr></thead><tbody>%s</tbody></table>
            <h2>Reply to this source</h2>
            <div class="panel"><form method="post" action="/reviewer/source/%s/reply">
              <textarea name="text" placeholder="Visible only to someone holding this source's codename." required></textarea>
              <p class="hint">Never ask the source for identifying details in a reply. Keep it short and avoid specifics about the case.</p>
              <div class="row" style="margin-top:16px"><button class="btn" type="submit">Send encrypted reply</button></div>
            </form></div>
            """.formatted(esc(shortId), day(createdDay), replies,
                replied ? "<div class=\"alert ok\">Reply sealed to the source's key and stored.</div>" : "", tb, id));
    }
}
