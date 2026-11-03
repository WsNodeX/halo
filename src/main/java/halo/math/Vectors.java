package halo.math;

/**
 * Numeric helpers for dense vectors and similarity metrics.
 *
 * <p>All operations are pure functions over {@code float[]} so they can run on
 * any JVM without native dependencies.</p>
 */
public final class Vectors {

    private Vectors() {}

    /** Cosine similarity in {@code [-1, 1]}. Returns 0 when either vector is zero. */
    public static float cosine(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("vector dimension mismatch: "
                    + a.length + " vs " + b.length);
        }
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0.0 || nb == 0.0) return 0.0f;
        return (float) (dot / (Math.sqrt(na) * Math.sqrt(nb)));
    }

    /** Dot product. */
    public static float dot(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("vector dimension mismatch");
        }
        double s = 0.0;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return (float) s;
    }

    /** Euclidean distance. */
    public static float euclidean(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("vector dimension mismatch");
        }
        double s = 0.0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            s += d * d;
        }
        return (float) Math.sqrt(s);
    }

    /** In-place L2 normalization. A zero vector is left unchanged. */
    public static void l2NormalizeInPlace(float[] v) {
        double s = 0.0;
        for (float x : v) s += x * x;
        if (s == 0.0) return;
        double norm = Math.sqrt(s);
        for (int i = 0; i < v.length; i++) v[i] = (float) (v[i] / norm);
    }

    /** Returns a new L2-normalized copy of {@code v}. */
    public static float[] l2Normalize(float[] v) {
        float[] out = v.clone();
        l2NormalizeInPlace(out);
        return out;
    }

    /** Element-wise addition. */
    public static float[] add(float[] a, float[] b) {
        float[] out = new float[a.length];
        for (int i = 0; i < a.length; i++) out[i] = a[i] + b[i];
        return out;
    }

    /** Scalar multiplication (new array). */
    public static float[] scale(float[] a, float k) {
        float[] out = new float[a.length];
        for (int i = 0; i < a.length; i++) out[i] = a[i] * k;
        return out;
    }

    /** Build an all-zero vector of the given dimension. */
    public static float[] zeros(int dim) {
        return new float[dim];
    }

    /**
     * Deterministic 32-bit hash of a token. Uses the FNV-1a variant with a
     * fixed seed so the same token always maps to the same bucket across runs
     * and across machines.
     */
    public static int hashToken(String token, int seed) {
        int h = 0x811c9dc5 ^ seed;
        for (int i = 0; i < token.length(); i++) {
            h ^= token.charAt(i);
            h *= 0x01000193;
        }
        return h;
    }
}
