package halo.test;

import halo.embed.HashEmbedder;
import halo.index.FusedHit;
import halo.index.HybridIndex;
import halo.index.InvertedIndex;
import halo.index.VectorIndex;
import halo.index.FlatIndex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestHybridIndex {

    private HybridIndex build() {
        HashEmbedder embedder = new HashEmbedder(256);
        VectorIndex dense = new FlatIndex(embedder.dim());
        HybridIndex h = new HybridIndex(dense, new InvertedIndex(), embedder);
        h.add("jvm", "the JVM garbage collector reclaims memory automatically");
        h.add("sport", "basketball is a popular team sport");
        h.add("ml", "machine learning trains neural network models on data");
        return h;
    }

    @Test
    void keywordQueryFavoursSparseMatch() {
        List<FusedHit> r = build().searchDetailed("garbage collector memory", 3);
        assertEquals("jvm", r.get(0).id);
        assertTrue(r.get(0).sparse > 0.0f);
    }

    @Test
    void semanticQueryRetrievesRelevantDoc() {
        List<FusedHit> r = build().searchDetailed("neural network training models", 3);
        assertTrue(r.stream().anyMatch(h -> h.id.equals("ml")));
    }

    @Test
    void fusedScoresCombineBothSignals() {
        List<FusedHit> r = build().searchDetailed("memory", 3);
        for (FusedHit h : r) {
            assertTrue(h.fused > 0.0f);
            assertTrue(h.dense >= 0.0f);
        }
    }
}
