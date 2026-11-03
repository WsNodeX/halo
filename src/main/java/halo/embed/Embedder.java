package halo.embed;

/**
 * Maps free text to a fixed-dimension dense vector.
 *
 * <p>Implementations are expected to be deterministic: the same input always
 * produces the same vector, which is what makes offline evaluation and
 * reproducible retrieval possible.</p>
 */
public interface Embedder {

    /** Dimension of the produced vectors. */
    int dim();

    /** Embed a single text into a {@code dim()}-length vector (not necessarily normalized). */
    float[] embed(String text);

    /** Convenience: embed and L2-normalize. */
    default float[] embedNormalized(String text) {
        return halo.math.Vectors.l2Normalize(embed(text));
    }
}
