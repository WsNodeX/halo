package halo.embed;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight tokenizers shared by every embedder in this package.
 *
 * <p>No external NLP dependency is required: tokenization is deterministic and
 * runs entirely on the JVM standard library.</p>
 */
public final class Tokenizers {

    private Tokenizers() {}

    /**
     * Word tokenizer: lower-cased runs of {@code [a-z0-9]} of length >= 1.
     * Punctuation and whitespace act as separators.
     */
    public static List<String> words(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        int i = 0, n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (isTokenChar(c)) {
                int j = i;
                StringBuilder sb = new StringBuilder();
                while (j < n && isTokenChar(text.charAt(j))) {
                    sb.append(Character.toLowerCase(text.charAt(j)));
                    j++;
                }
                if (sb.length() > 0) out.add(sb.toString());
                i = j;
            } else {
                i++;
            }
        }
        return out;
    }

    /** Character n-grams (length {@code n}) of the lower-cased input. */
    public static List<String> charNgrams(String text, int n) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        String t = text.toLowerCase();
        if (t.length() < n) {
            if (!t.isEmpty()) out.add(t);
            return out;
        }
        for (int i = 0; i + n <= t.length(); i++) out.add(t.substring(i, i + n));
        return out;
    }

    /** Word n-grams (joined by a space) of the tokenized input. */
    public static List<String> wordNgrams(String text, int n) {
        List<String> words = words(text);
        List<String> out = new ArrayList<>();
        if (words.size() < n) return out;
        for (int i = 0; i + n <= words.size(); i++) {
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < n; k++) {
                if (k > 0) sb.append(' ');
                sb.append(words.get(i + k));
            }
            out.add(sb.toString());
        }
        return out;
    }

    private static boolean isTokenChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                || (c >= 'A' && c <= 'Z') || Character.isIdeographic(c);
    }
}
