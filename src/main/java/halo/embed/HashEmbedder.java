package halo.embed;

import halo.math.Vectors;

import java.util.List;

/**
 * Hashing-trick embedder.
 *
 * <p>Each token is deterministically hashed into one of {@code dim} buckets and
 * accumulated. The resulting vector is L2-normalized so that cosine similarity
 * reflects lexical overlap. This needs no training corpus and no external model,
 * which makes it ideal for offline demos, tests and cold-start retrieval.</p>
 *
 * <p>Collisions are possible (two tokens may share a bucket), but with a large
 * enough dimension the effect on nearest-neighbour quality is negligible for
 * typical text sizes.</p>
 */
public final class HashEmbedder implements Embedder {

    private final int dim;
    private final int seed;
    private final boolean sublinear;

    public HashEmbedder(int dim) {
        this(dim, 0x9e3779b9, true);
    }

    public HashEmbedder(int dim, int seed, boolean sublinear) {
        if (dim <= 0) throw new IllegalArgumentException("dim must be positive");
        this.dim = dim;
        this.seed = seed;
        this.sublinear = sublinear;
    }

    @Override
    public int dim() { return dim; }

    @Override
    public float[] embed(String text) {
        List<String> tokens = Tokenizers.words(text);
        float[] v = new float[dim];
        for (String tok : tokens) {
            int h = Vectors.hashToken(tok, seed);
            int bucket = Math.floorMod(h, dim);
            if (sublinear) {
                v[bucket] += 1.0f;
            } else {
                v[bucket] += 1.0f;
            }
        }
        if (sublinear) {
            for (int i = 0; i < v.length; i++) {
                if (v[i] > 0) v[i] = (float) (1.0 + Math.log(v[i]));
            }
        }
        Vectors.l2NormalizeInPlace(v);
        return v;
    }
}
