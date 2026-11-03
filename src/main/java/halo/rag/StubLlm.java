package halo.rag;

import halo.retrieval.RetrievalResult;

import java.util.List;

/**
 * A deterministic, fully offline stand-in for an LLM.
 *
 * <p>It grounds its answer strictly in the retrieved passages and emits inline
 * citation markers ({@code [1]}, {@code [2]}, ...) so the RAG pipeline can be
 * exercised end-to-end without any network call or API key. Swap in a real
 * OpenAI-compatible client in production by implementing the same
 * {@link #complete(String, List)} shape.</p>
 */
public class StubLlm {

    public String complete(String query, List<RetrievalResult> passages) {
        if (passages == null || passages.isEmpty()) {
            return "No relevant context was retrieved for: \"" + query + "\"";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Based on the retrieved material, ").append(query.toLowerCase().startsWith("what")
                ? "here is what I found" : "the answer is").append(":\n\n");

        RetrievalResult top = passages.get(0);
        sb.append(firstSentence(top.text())).append(" [1]");

        if (passages.size() > 1) {
            RetrievalResult second = passages.get(1);
            if (!second.docId().equals(top.docId())) {
                sb.append("\n\n").append(firstSentence(second.text())).append(" [2]");
            }
        }
        return sb.toString();
    }

    private static String firstSentence(String text) {
        if (text == null || text.isEmpty()) return "";
        int end = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?') { end = i + 1; break; }
        }
        if (end < 0) return text.trim();
        return text.substring(0, end).trim();
    }
}
