package halo.test;

import halo.math.Vectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestVectors {

    @Test
    void cosineOfIdenticalIsOne() {
        float[] a = {1, 2, 3};
        assertEquals(1.0f, Vectors.cosine(a, a.clone()), 1e-5f);
    }

    @Test
    void cosineOfOrthogonalIsZero() {
        assertEquals(0.0f, Vectors.cosine(new float[]{1, 0}, new float[]{0, 1}), 1e-6f);
    }

    @Test
    void dotAndEuclidean() {
        assertEquals(14.0f, Vectors.dot(new float[]{1, 2, 3}, new float[]{1, 2, 3}), 1e-5f);
        assertEquals((float) Math.sqrt(3.0), Vectors.euclidean(new float[]{1, 0, 0}, new float[]{0, 1, 1}), 1e-5f);
    }

    @Test
    void l2NormalizeNormalizes() {
        float[] v = Vectors.l2Normalize(new float[]{3, 4});
        assertEquals(1.0f, (float) Math.hypot(v[0], v[1]), 1e-6f);
    }

    @Test
    void l2NormalizeLeavesZeroVectorUntouched() {
        float[] v = Vectors.l2Normalize(new float[]{0, 0});
        assertArrayEquals(new float[]{0, 0}, v, 1e-9f);
    }

    @Test
    void hashTokenIsDeterministicAndSeedDependent() {
        int a = Vectors.hashToken("hello", 1);
        int b = Vectors.hashToken("hello", 1);
        int c = Vectors.hashToken("hello", 2);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }
}
