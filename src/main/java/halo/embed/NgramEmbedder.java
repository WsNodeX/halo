package halo.embed;

import halo.math.Vectors;

import java.util.List;

/**
 * Character n-gram embedder.
 *
 * <p>Robust to morphology and to out-of-vocabulary words: "running", "runs" and
 * "ran" share many character trigrams. Useful for fuzzy / multilingual retrieval
 * where word tokenization is unreliable.</p>
 */
public final class NgramEmbedder implements Embedder {

    private final int dim;
    private final int n;

    public NgramEmbedder(int dim, int n) {
        this.dim = dim;
        this.n = n;
    }

    @Override
    public int dim() { return dim; }

    @Override
    public float[] embed(String text) {
        List<String> grams = Tokenizers.charNgrams(text, n);
        float[] v = new float[dim];
        if (grams.isEmpty()) return v;
        for (String g : grams) {
            int idx = Math.floorMod(Vectors.hashToken(g, 0xabcdef), dim);
            v[idx] += 1.0f;
        }
        Vectors.l2NormalizeInPlace(v);
        return v;
    }
}
