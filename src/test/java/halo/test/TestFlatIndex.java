package halo.test;

import halo.index.FlatIndex;
import halo.index.ScoredId;
import halo.math.Vectors;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestFlatIndex {

    @Test
    void exactSelfMatchRanksFirst() {
        FlatIndex idx = new FlatIndex(3);
        idx.add("a", new float[]{1, 0, 0});
        idx.add("b", new float[]{0, 1, 0});
        idx.add("c", new float[]{0, 0, 1});
        List<ScoredId> r = idx.search(new float[]{1, 0, 0}, 3);
        assertEquals("a", r.get(0).id);
        assertEquals(1.0f, r.get(0).score, 1e-5f);
    }

    @Test
    void respectsTopK() {
        FlatIndex idx = new FlatIndex(2);
        for (int i = 0; i < 10; i++) idx.add("x" + i, Vectors.scale(new float[]{1, 1}, i));
        assertEquals(5, idx.search(new float[]{1, 1}, 5).size());
        assertEquals(3, idx.search(new float[]{1, 1}, 3).size());
    }

    @Test
    void replaceKeepsSingleEntry() {
        FlatIndex idx = new FlatIndex(2);
        idx.add("a", new float[]{1, 0});
        idx.add("a", new float[]{0, 1});
        assertEquals(1, idx.size());
        assertEquals("a", idx.search(new float[]{0, 1}, 1).get(0).id);
    }
}
