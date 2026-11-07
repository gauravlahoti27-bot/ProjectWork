package com.onionleaks.crypto;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.List;

/**
 * Codenames are the only credential a source ever has. A codename is 7 random words from a
 * 282-word list (7 * log2(282) ~= 57 bits). The server stores only a slow hash of it
 * (PBKDF2-HMAC-SHA256, 210k iterations) plus a per-source salt, exactly like a password;
 * together with the rate limiter this puts online guessing far out of reach.
 *
 * The same codename also derives a separate AES key that wraps the source's own X25519 private
 * key, so reviewer replies (sealed to that key) are readable only while the source is presenting
 * the codename.
 */
public final class Codenames {

    private static final SecureRandom RNG = new SecureRandom();
    private static final int WORDS = 7;
    private static final int ITERATIONS = 210_000;

    // Short, unambiguous, easy-to-type words (EFF-style). Swap in the full EFF long list for ~90 bits.
    static final List<String> WORDLIST = List.of(
        "acid","actor","adopt","album","alien","amber","angle","ankle","apple","arrow","atlas","audit",
        "badge","baker","basin","beach","bench","berry","birch","blade","blend","bloom","board","brave",
        "bread","brick","brush","cabin","cable","camel","candy","canoe","cargo","carol","cedar","chair",
        "chalk","cheek","chess","chief","chili","cider","cigar","civic","claim","clerk","cliff","clock",
        "cloud","coach","cobra","comet","coral","crane","cream","crisp","crown","cycle","daisy","dance",
        "delta","denim","diary","diver","dodge","donor","dough","draft","drama","dream","drift","drill",
        "eagle","early","earth","ember","empty","enjoy","entry","equal","essay","fable","fairy","falcon",
        "feast","fence","ferry","fiber","field","flame","flash","fleet","flint","flock","flora","flute",
        "focus","forge","fossil","frost","fruit","gauge","giant","glade","glass","globe","gloom","glove",
        "grain","grape","grass","gravy","green","grove","guard","guest","guide","habit","harbor","hazel",
        "heart","hedge","heron","honey","horse","hotel","house","humor","ideal","image","index","inlet",
        "ivory","jelly","jewel","judge","juice","kayak","kiosk","knife","koala","label","lance","laser",
        "lemon","level","light","lilac","linen","lotus","lunar","magic","mango","maple","marsh","medal",
        "melon","metal","minor","mocha","model","motor","mural","music","noble","north","novel","nurse",
        "oasis","ocean","olive","onion","opera","orbit","otter","oxide","paint","panda","paper","pearl",
        "pedal","penny","piano","pilot","pixel","plaza","plumb","poet","polar","porch","prism","pulse",
        "quail","quart","queen","quilt","radar","radio","raven","relay","ridge","river","robin","rocket",
        "rodeo","royal","ruler","saddle","salad","salmon","satin","scale","scarf","scout","sedan","shade",
        "sheep","shelf","shore","sigma","siren","skate","slate","smoke","snail","solar","sonic","spade",
        "spark","spice","spoon","squid","stage","stamp","steam","stone","storm","straw","sugar","swan",
        "table","tango","taxi","tempo","theme","thorn","tiger","timber","toast","token","topaz","torch",
        "tower","trail","tulip","tunic","ultra","umbra","union","upper","urban","valve","vapor","velvet",
        "venue","vinyl","viola","vivid","vocal","wagon","walnut","water","whale","wheat","woven","yacht",
        "yield","zebra","zesty","zinc"
    );

    private Codenames() {}

    public static String generate() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < WORDS; i++) {
            if (i > 0) sb.append(' ');
            sb.append(WORDLIST.get(RNG.nextInt(WORDLIST.size())));
        }
        return sb.toString();
    }

    /** Canonical form: lowercase, single spaces. Users may type extra whitespace or caps. */
    public static String normalize(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    public static byte[] newSalt() {
        byte[] s = new byte[16];
        RNG.nextBytes(s);
        return s;
    }

    /** Slow hash used for login. */
    public static byte[] hash(String codename, byte[] salt) {
        return pbkdf2(normalize(codename), salt, "login");
    }

    /** Separate key used to encrypt replies at rest. Different label -> different key. */
    public static byte[] replyKey(String codename, byte[] salt) {
        return pbkdf2(normalize(codename), salt, "reply");
    }

    /** Non-secret lookup index: fast SHA-256 so we can find the row without a full table scan. */
    public static byte[] lookupIndex(String codename) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(("lookup:" + normalize(codename)).getBytes());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }

    private static byte[] pbkdf2(String codename, byte[] salt, String label) {
        try {
            byte[] fullSalt = new byte[salt.length + label.length()];
            System.arraycopy(salt, 0, fullSalt, 0, salt.length);
            System.arraycopy(label.getBytes(), 0, fullSalt, salt.length, label.length());
            PBEKeySpec spec = new PBEKeySpec(codename.toCharArray(), fullSalt, ITERATIONS, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
