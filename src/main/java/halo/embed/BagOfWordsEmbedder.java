package halo.embed;

import halo.math.Vectors;

import java.util.List;

/**
 * Bag-of-words embedder based on the hashing trick, but with a higher collision
 * tolerance than {@link HashEmbedder} by also folding bigrams into separate
 * buckets. Produces a sparse, interpretable representation.
 */
public final class BagOfWordsEmbedder implements Embedder {

    private final int dim;
    private final boolean bigrams;

    public BagOfWordsEmbedder(int dim) { this(dim, true); }

    public BagOfWordsEmbedder(int dim, boolean bigrams) {
        this.dim = dim;
        this.bigrams = bigrams;
    }

    @Override
    public int dim() { return dim; }

    @Override
    public float[] embed(String text) {
        List<String> unigrams = Tokenizers.words(text);
        float[] v = new float[dim];
        for (String tok : unigrams) {
            int idx = Math.floorMod(Vectors.hashToken(tok, 0x1234567), dim);
            v[idx] += 1.0f;
        }
        if (bigrams) {
            List<String> big = Tokenizers.wordNgrams(text, 2);
            for (String tok : big) {
                int idx = Math.floorMod(Vectors.hashToken(tok, 0x7654321), dim);
                v[idx] += 1.0f;
            }
        }
        Vectors.l2NormalizeInPlace(v);
        return v;
    }
}
