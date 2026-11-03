package halo.rerank;

import halo.index.ScoredId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Linear feature reranker.
 *
 * <p>Each signal (dense similarity, sparse BM25, and any extra feature) is
 * min-max normalised across the candidate set and combined with a weight
 * vector:</p>
 *
 * <pre>score = wDense*dense + wSparse*sparse + Σ w_f * feature_f</pre>
 *
 * <p>Normalising per-query keeps the combination stable even though dense and
 * sparse scores live on wildly different scales. This is the same idea behind
 * learning-to-rank feature combination, expressed as a single interpretable
 * weighted sum.</p>
 */
public final class FeatureReranker implements Reranker {

    private final float wDense;
    private final float wSparse;
    private final Map<String, Float> featureWeights;

    public FeatureReranker(float wDense, float wSparse) {
        this(wDense, wSparse, Map.of());
    }

    public FeatureReranker(float wDense, float wSparse, Map<String, Float> featureWeights) {
        this.wDense = wDense;
        this.wSparse = wSparse;
        this.featureWeights = featureWeights;
    }

    @Override
    public List<ScoredId> rerank(String query, List<? extends Rerankable> candidates) {
        if (candidates.isEmpty()) return new ArrayList<>();

        float dMin = Float.MAX_VALUE, dMax = Float.MIN_VALUE;
        float sMin = Float.MAX_VALUE, sMax = Float.MIN_VALUE;
        for (Rerankable c : candidates) {
            dMin = Math.min(dMin, c.denseScore());
            dMax = Math.max(dMax, c.denseScore());
            sMin = Math.min(sMin, c.sparseScore());
            sMax = Math.max(sMax, c.sparseScore());
        }
        float dSpan = dMax - dMin;
        float sSpan = sMax - sMin;

        List<ScoredId> out = new ArrayList<>(candidates.size());
        for (Rerankable c : candidates) {
            float nd = dSpan > 1e-9f ? (c.denseScore() - dMin) / dSpan : 0f;
            float ns = sSpan > 1e-9f ? (c.sparseScore() - sMin) / sSpan : 0f;
            float score = wDense * nd + wSparse * ns;
            for (Map.Entry<String, Float> e : featureWeights.entrySet()) {
                Float raw = c.features().get(e.getKey());
                if (raw == null) continue;
                score += e.getValue() * raw;
            }
            out.add(new ScoredId(c.id(), score));
        }
        out.sort(Comparator.comparingDouble((ScoredId s) -> s.score).reversed());
        return out;
    }
}
