package halo.retrieval;

import halo.rerank.Rerankable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One retrieved passage with its provenance and the scores accumulated through
 * the pipeline (fusion score, then possibly a rerank score).
 */
public final class RetrievalResult implements Rerankable {

    private final String id;
    private final String docId;
    private final String text;
    private final Map<String, String> metadata;
    private float denseScore;
    private float sparseScore;
    private float score;

    public RetrievalResult(String id, String docId, String text, Map<String, String> metadata) {
        this.id = id;
        this.docId = docId;
        this.text = text;
        this.metadata = metadata != null ? new LinkedHashMap<>(metadata) : Collections.emptyMap();
    }

    public String id() { return id; }
    public String docId() { return docId; }
    public String text() { return text; }
    public Map<String, String> metadata() { return Collections.unmodifiableMap(metadata); }

    public float denseScore() { return denseScore; }
    public void setDenseScore(float v) { this.denseScore = v; }
    public float sparseScore() { return sparseScore; }
    public void setSparseScore(float v) { this.sparseScore = v; }

    /** Currently authoritative ranking score (fusion, or rerank if applied). */
    public float score() { return score; }
    public void setScore(float v) { this.score = v; }

    @Override
    public Map<String, Float> features() {
        Map<String, Float> f = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : metadata.entrySet()) {
            f.put("meta:" + e.getKey(), 1.0f);
        }
        return f;
    }

    @Override
    public String toString() {
        String preview = text.length() > 80 ? text.substring(0, 80) + "…" : text;
        return "[" + id + "] " + preview + "  (score=" + String.format("%.4f", score) + ")";
    }
}
