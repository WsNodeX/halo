package halo.test;

import halo.index.ScoredId;
import halo.rerank.FeatureReranker;
import halo.rerank.Reranker;
import halo.rerank.Rerankable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestFeatureReranker {

    private static final class Cand implements Rerankable {
        final String id;
        final float dense, sparse;
        final Map<String, Float> features;
        Cand(String id, float dense, float sparse, Map<String, Float> features) {
            this.id = id; this.dense = dense; this.sparse = sparse; this.features = features;
        }
        public String id() { return id; }
        public float denseScore() { return dense; }
        public float sparseScore() { return sparse; }
        public Map<String, Float> features() { return features; }
    }

    @Test
    void denseWeightDominatesWhenSet() {
        List<Cand> cands = new ArrayList<>();
        cands.add(new Cand("lowDense", 0.1f, 0.9f, Map.of()));
        cands.add(new Cand("highDense", 0.9f, 0.1f, Map.of()));
        Reranker r = new FeatureReranker(1.0f, 0.0f);
        List<ScoredId> out = r.rerank("q", cands);
        assertEquals("highDense", out.get(0).id);
    }

    @Test
    void sparseWeightDominatesWhenSet() {
        List<Cand> cands = new ArrayList<>();
        cands.add(new Cand("lowSparse", 0.9f, 0.1f, Map.of()));
        cands.add(new Cand("highSparse", 0.1f, 0.9f, Map.of()));
        Reranker r = new FeatureReranker(0.0f, 1.0f);
        List<ScoredId> out = r.rerank("q", cands);
        assertEquals("highSparse", out.get(0).id);
    }

    @Test
    void extraFeatureInfluencesRanking() {
        Map<String, Float> w = Map.of("recency", 2.0f);
        List<Cand> cands = new ArrayList<>();
        cands.add(new Cand("old", 0.8f, 0.8f, Map.of("recency", 0.0f)));
        cands.add(new Cand("new", 0.5f, 0.5f, Map.of("recency", 1.0f)));
        Reranker r = new FeatureReranker(0.5f, 0.5f, w);
        List<ScoredId> out = r.rerank("q", cands);
        assertEquals("new", out.get(0).id);
    }
}
