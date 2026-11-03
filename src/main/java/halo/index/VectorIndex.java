package halo.index;

import java.util.List;

/**
 * A dense vector index over identifiers.
 *
 * <p>Vectors are expected to be L2-normalized on insert so that the retrieval
 * score can be interpreted directly as cosine similarity ({@code [0, 1]} for
 * non-negative vectors).</p>
 */
public interface VectorIndex {

    /** Insert or replace a vector for {@code id}. Must be {@link #dim()}-dimensional. */
    void add(String id, float[] vector);

    /** Return the {@code topK} most similar identifiers to {@code query}, best first. */
    List<ScoredId> search(float[] query, int topK);

    /** Declared vector dimension. */
    int dim();

    /** Number of stored vectors. */
    int size();
}
