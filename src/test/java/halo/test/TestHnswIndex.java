package halo.test;

import halo.index.FlatIndex;
import halo.index.HnswIndex;
import halo.index.ScoredId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TestHnswIndex {

    private static float[] randomUnit(Random r, int dim) {
        float[] v = new float[dim];
        double s = 0;
        for (int i = 0; i < dim; i++) { v[i] = r.nextFloat() - 0.5f; s += v[i] * v[i]; }
        double n = Math.sqrt(s);
        for (int i = 0; i < dim; i++) v[i] /= n;
        return v;
    }

    @Test
    void recallMatchesFlatIndexOnRandomVectors() {
        int dim = 48, n = 250, topK = 10;
        Random r = new Random(12345);
        List<float[]> vecs = new ArrayList<>();
        for (int i = 0; i < n; i++) vecs.add(randomUnit(r, dim));

        FlatIndex truth = new FlatIndex(dim);
        HnswIndex hnsw = new HnswIndex(dim, 16, 200, 128);
        for (int i = 0; i < n; i++) {
            truth.add("v" + i, vecs.get(i));
            hnsw.add("v" + i, vecs.get(i));
        }

        Random q = new Random(999);
        double totalRecall = 0;
        int queries = 25;
        for (int t = 0; t < queries; t++) {
            float[] probe = randomUnit(q, dim);
            List<ScoredId> exact = truth.search(probe, topK);
            List<ScoredId> approx = hnsw.search(probe, topK);
            Set<String> exactIds = new HashSet<>();
            for (ScoredId s : exact) exactIds.add(s.id);
            int hit = 0;
            for (ScoredId s : approx) if (exactIds.contains(s.id)) hit++;
            totalRecall += (double) hit / topK;
        }
        double recall = totalRecall / queries;
        assertTrue(recall >= 0.85,
                "HNSW recall@" + topK + " should be >= 0.85, was " + String.format("%.3f", recall));
    }

    @Test
    void returnsAtMostTopKAndIsDeterministic() {
        int dim = 16;
        HnswIndex hnsw = new HnswIndex(dim, 8, 100, 32);
        Random r = new Random(7);
        for (int i = 0; i < 60; i++) hnsw.add("v" + i, randomUnit(r, dim));

        List<ScoredId> a = hnsw.search(randomUnit(new Random(3), dim), 5);
        List<ScoredId> b = hnsw.search(randomUnit(new Random(3), dim), 5);
        assertEquals(a, b, "identical queries must produce identical results");
        assertTrue(a.size() <= 5);
    }

    @Test
    void emptyIndexReturnsNothing() {
        HnswIndex hnsw = new HnswIndex(8);
        assertTrue(hnsw.search(new float[]{1, 0, 0, 0, 0, 0, 0, 0}, 5).isEmpty());
    }
}
