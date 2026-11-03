package halo.index;

import halo.math.Vectors;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exact (brute-force) dense vector index.
 *
 * <p>Computes the similarity of the query against every stored vector and sorts
 * the results. O(N) per query, which is perfectly adequate for thousands of
 * vectors and also serves as the ground-truth reference for measuring the
 * recall of approximate indexes such as {@link HnswIndex}.</p>
 */
public final class FlatIndex implements VectorIndex {

    private final int dim;
    private final List<String> ids = new ArrayList<>();
    private final List<float[]> vectors = new ArrayList<>();
    private final Map<String, Integer> posById = new IdentityHashMap<>();

    public FlatIndex(int dim) { this.dim = dim; }

    @Override
    public synchronized void add(String id, float[] vector) {
        if (vector.length != dim) throw new IllegalArgumentException("dim mismatch");
        Integer existing = posById.get(id);
        if (existing != null) {
            vectors.set(existing, Vectors.l2Normalize(vector));
        } else {
            posById.put(id, ids.size());
            ids.add(id);
            vectors.add(Vectors.l2Normalize(vector));
        }
    }

    @Override
    public List<ScoredId> search(float[] query, int topK) {
        if (query.length != dim) throw new IllegalArgumentException("dim mismatch");
        float[] q = Vectors.l2Normalize(query);
        List<ScoredId> scored = new ArrayList<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            scored.add(new ScoredId(ids.get(i), Vectors.cosine(q, vectors.get(i))));
        }
        scored.sort(null);
        return scored.subList(0, Math.min(topK, scored.size()));
    }

    @Override
    public int dim() { return dim; }

    @Override
    public int size() { return ids.size(); }
}
