package halo.index;

import halo.embed.Embedder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hybrid retriever that fuses a dense (semantic) index with a sparse (lexical)
 * index using Reciprocal Rank Fusion (RRF).
 *
 * <p>Dense retrieval catches paraphrases; sparse BM25 catches exact keywords.
 * RRF merges the two ranked lists without needing to normalise their very
 * different score scales:</p>
 *
 * <pre>score(d) = sum_lists  1 / (k + rank(d))</pre>
 *
 * <p>RRF is robust, parameter-light and a common production pattern for
 * "semantic + keyword" search.</p>
 */
public final class HybridIndex {

    private final VectorIndex dense;
    private final InvertedIndex sparse;
    private final Embedder embedder;
    private final int rrfK;

    public HybridIndex(VectorIndex dense, InvertedIndex sparse, Embedder embedder) {
        this(dense, sparse, embedder, 60);
    }

    public HybridIndex(VectorIndex dense, InvertedIndex sparse, Embedder embedder, int rrfK) {
        this.dense = dense;
        this.sparse = sparse;
        this.embedder = embedder;
        this.rrfK = rrfK;
    }

    /** Index a document: store its dense vector and its lexical postings. */
    public void add(String id, String text) {
        dense.add(id, embedder.embed(text));
        sparse.add(id, text);
    }

    /** Fuse dense + sparse results into a single ranked list. */
    public List<ScoredId> search(String query, int topK) {
        return toScoredIds(searchDetailed(query, topK));
    }

    /** Fuse and also expose the raw dense/sparse contributions per hit. */
    public List<FusedHit> searchDetailed(String query, int topK) {
        float[] qv = embedder.embed(query);
        List<ScoredId> denseHits = dense.search(qv, topK * 3);
        List<ScoredId> sparseHits = sparse.search(query, topK * 3);

        Map<String, Float> fused = new HashMap<>();
        Map<String, Float> denseScore = new HashMap<>();
        Map<String, Float> sparseScore = new HashMap<>();
        for (ScoredId h : denseHits) { denseScore.put(h.id, h.score); }
        for (ScoredId h : sparseHits) { sparseScore.put(h.id, h.score); }
        accumulate(fused, denseHits);
        accumulate(fused, sparseHits);

        List<FusedHit> out = new ArrayList<>();
        for (Map.Entry<String, Float> e : fused.entrySet()) {
            float d = denseScore.getOrDefault(e.getKey(), 0.0f);
            float s = sparseScore.getOrDefault(e.getKey(), 0.0f);
            out.add(new FusedHit(e.getKey(), e.getValue(), d, s));
        }
        out.sort((a, b) -> Float.compare(b.fused, a.fused));
        return out.subList(0, Math.min(topK, out.size()));
    }

    private static List<ScoredId> toScoredIds(List<FusedHit> hits) {
        List<ScoredId> out = new ArrayList<>(hits.size());
        for (FusedHit h : hits) out.add(h.toScoredId());
        return out;
    }

    private void accumulate(Map<String, Float> fused, List<ScoredId> hits) {
        for (int i = 0; i < hits.size(); i++) {
            int rank = i + 1;
            float contribution = 1.0f / (rrfK + rank);
            fused.merge(hits.get(i).id, contribution, Float::sum);
        }
    }
}
