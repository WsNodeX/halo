package halo.test;

import halo.embed.BagOfWordsEmbedder;
import halo.embed.Embedder;
import halo.embed.HashEmbedder;
import halo.embed.NgramEmbedder;
import halo.embed.TfIdfEmbedder;
import halo.math.Vectors;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestEmbedders {

    @Test
    void hashEmbedderDeterministicAndNormalized() {
        Embedder e = new HashEmbedder(256);
        assertEquals(256, e.dim());
        float[] a = e.embed("the quick brown fox");
        float[] b = e.embed("the quick brown fox");
        assertArrayEquals(a, b, 1e-6f);
        assertEquals(1.0f, l2(a), 1e-5f);
    }

    @Test
    void hashEmbedderSimilarBeatsDissimilar() {
        Embedder e = new HashEmbedder(512);
        float simSame = Vectors.cosine(e.embed("java garbage collection jvm memory"), e.embed("jvm garbage collector memory"));
        float simDiff = Vectors.cosine(e.embed("java garbage collection jvm memory"), e.embed("basketball soccer football goal"));
        assertTrue(simSame > simDiff, "similar texts should be closer than unrelated ones");
        assertTrue(simSame > 0.1, "overlapping texts should have positive similarity");
    }

    @Test
    void tfIdfNeedsFitThenRanksSimilarHigher() {
        TfIdfEmbedder e = new TfIdfEmbedder();
        List<String> corpus = List.of(
                "the cat sat on the mat",
                "the dog played in the park",
                "machine learning models train on data",
                "deep learning uses neural networks");
        e.fit(corpus);
        assertTrue(e.dim() > 0, "vocabulary should be non-empty");
        float simSame = Vectors.cosine(e.embed("neural networks deep learning"), e.embed("deep learning neural nets"));
        float simDiff = Vectors.cosine(e.embed("neural networks deep learning"), e.embed("the cat sat on mat"));
        assertTrue(simSame > simDiff);
    }

    @Test
    void bagOfWordsAndNgramBasics() {
        BagOfWordsEmbedder bow = new BagOfWordsEmbedder(128);
        assertEquals(1.0f, l2(bow.embed("hello world")), 1e-5f);
        NgramEmbedder ng = new NgramEmbedder(128, 3);
        assertEquals(128, ng.dim());
        assertArrayEquals(ng.embed("abc"), ng.embed("abc"));
    }

    private static float l2(float[] v) {
        double s = 0;
        for (float x : v) s += x * x;
        return (float) Math.sqrt(s);
    }
}
