package halo.index;

import halo.embed.Tokenizers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Inverted index with Okapi BM25 ranking.
 *
 * <p>Complements the dense {@link VectorIndex}: it is exact, token-based and
 * excellent at matching rare keywords, product codes or named entities that a
 * dense embedder might blur. BM25 is the workhorse ranking function behind most
 * classical search engines.</p>
 */
public final class InvertedIndex {

    private final float k1;
    private final float b;

    /** term -> (docId -> term frequency) */
    private final Map<String, Map<String, Integer>> postings = new HashMap<>();
    private final Map<String, Integer> docLength = new HashMap<>();
    private int totalDocs = 0;
    private long totalLength = 0;

    public InvertedIndex() { this(1.5f, 0.75f); }

    public InvertedIndex(float k1, float b) {
        this.k1 = k1;
        this.b = b;
    }

    /** Index a document by id and raw text. Re-adding the same id replaces it. */
    public synchronized void add(String docId, String text) {
        add(docId, Tokenizers.words(text));
    }

    /** Index a document from pre-tokenized terms. */
    public synchronized void add(String docId, List<String> tokens) {
        if (docLength.containsKey(docId)) remove(docId);
        Map<String, Integer> tf = new HashMap<>();
        for (String t : tokens) tf.merge(t, 1, Integer::sum);
        for (Map.Entry<String, Integer> e : tf.entrySet()) {
            postings.computeIfAbsent(e.getKey(), k -> new HashMap<>()).put(docId, e.getValue());
        }
        docLength.put(docId, tokens.size());
        totalDocs++;
        totalLength += tokens.size();
    }

    /** Remove a document from the index. */
    public synchronized void remove(String docId) {
        Integer len = docLength.remove(docId);
        if (len == null) return;
        totalDocs--;
        totalLength -= len;
        for (Map<String, Integer> p : postings.values()) {
            if (p.remove(docId) != null && p.isEmpty()) {
                // leave empty posting lists; they are cheap and rare
            }
        }
    }

    /** BM25 score of every document that contains at least one query term. */
    public synchronized List<ScoredId> search(String query, int topK) {
        List<String> terms = Tokenizers.words(query);
        if (terms.isEmpty()) return new ArrayList<>();
        double avgdl = totalDocs == 0 ? 0 : (double) totalLength / totalDocs;
        Map<String, Double> scores = new HashMap<>();
        for (String term : terms) {
            Map<String, Integer> p = postings.get(term);
            if (p == null) continue;
            double idf = Math.log((totalDocs - p.size() + 0.5) / (p.size() + 0.5) + 1.0);
            for (Map.Entry<String, Integer> e : p.entrySet()) {
                double f = e.getValue();
                double dl = docLength.get(e.getKey());
                double denom = f + k1 * (1 - b + b * (dl / (avgdl + 1e-9)));
                double termScore = idf * (f * (k1 + 1)) / denom;
                scores.merge(e.getKey(), termScore, Double::sum);
            }
        }
        List<ScoredId> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : scores.entrySet()) {
            out.add(new ScoredId(e.getKey(), (float) (double) e.getValue()));
        }
        out.sort(null);
        return out.subList(0, Math.min(topK, out.size()));
    }

    public int size() { return docLength.size(); }
}
