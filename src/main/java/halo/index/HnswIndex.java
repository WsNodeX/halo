package halo.index;

import halo.math.Vectors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

/**
 * Hierarchical Navigable Small World (HNSW) approximate nearest-neighbour index.
 *
 * <p>Implemented from scratch with no external dependency. HNSW builds a
 * multi-layer graph where the top layers are sparse "express" routes and the
 * bottom layer is dense. Search descends from the top, greedily walking toward
 * the query, which gives sub-linear query cost while keeping recall high.</p>
 *
 * <p>Key parameters:</p>
 * <ul>
 *   <li>{@code M} – maximum connections per node on layers above 0 (layer 0 uses {@code 2M}).</li>
 *   <li>{@code efConstruction} – candidate set size while building.</li>
 *   <li>{@code efSearch} – candidate set size while querying (raise for better recall).</li>
 * </ul>
 *
 * <p>Levels are assigned with a fixed-seed RNG so index construction is
 * deterministic and reproducible in tests and CI.</p>
 */
public final class HnswIndex implements VectorIndex {

    private static final class Cand {
        final int node;
        final float d;
        Cand(int node, float d) { this.node = node; this.d = d; }
    }

    private static final class Node {
        final String id;
        final float[] vec;
        final List<List<Integer>> links = new ArrayList<>();
        Node(String id, float[] vec) { this.id = id; this.vec = vec; }
    }

    private final int dim;
    private final int m;
    private final int efConstruction;
    private int efSearch;
    private final double mL;
    private final Random rng;
    private final List<Node> nodes = new ArrayList<>();
    private int entryPoint = -1;
    private int entryLevel = -1;

    public HnswIndex(int dim) { this(dim, 16, 200, 64); }

    public HnswIndex(int dim, int m, int efConstruction, int efSearch) {
        this.dim = dim;
        this.m = m;
        this.efConstruction = efConstruction;
        this.efSearch = efSearch;
        this.mL = 1.0 / Math.log(m);
        this.rng = new Random(0x5eed1234L);
    }

    public void setEfSearch(int ef) { this.efSearch = ef; }
    public int getEfSearch() { return efSearch; }

    @Override
    public synchronized void add(String id, float[] vector) {
        if (vector.length != dim) throw new IllegalArgumentException("dim mismatch");
        float[] v = Vectors.l2Normalize(vector);
        int idx = nodes.size();
        Node node = new Node(id, v);
        int level = (int) Math.floor(-Math.log(rng.nextDouble() + 1e-12) * mL);

        // allocate link lists for every layer 0..level
        for (int l = 0; l <= level; l++) node.links.add(new ArrayList<>());

        if (entryPoint == -1) {
            nodes.add(node);
            entryPoint = idx;
            entryLevel = level;
            return;
        }

        // descend from the top layer down to (level + 1) to find the insertion entry
        int ep = entryPoint;
        for (int lc = entryLevel; lc > level; lc--) {
            List<Cand> w = searchLayer(v, List.of(ep), 1, lc);
            ep = w.get(0).node;
        }

        // the new node is now part of the graph so connect() can resolve it
        nodes.add(node);

        List<Integer> entryPoints = List.of(ep);
        for (int lc = Math.min(level, entryLevel); lc >= 0; lc--) {
            List<Cand> w = searchLayer(v, entryPoints, efConstruction, lc);
            int cap = (lc == 0) ? 2 * m : m;
            List<Integer> neighbors = selectClosest(w, cap, idx);
            node.links.set(lc, new ArrayList<>(neighbors));
            for (int nb : neighbors) connect(idx, nb, lc, cap);
            entryPoints = List.of(nearestNode(w));
        }

        if (level > entryLevel) {
            entryPoint = idx;
            entryLevel = level;
        }
    }

    @Override
    public List<ScoredId> search(float[] query, int topK) {
        if (entryPoint == -1) return new ArrayList<>();
        float[] q = Vectors.l2Normalize(query);
        int ep = entryPoint;
        for (int lc = entryLevel; lc > 0; lc--) {
            List<Cand> w = searchLayer(q, List.of(ep), 1, lc);
            ep = w.get(0).node;
        }
        List<Cand> w = searchLayer(q, List.of(ep), efSearch, 0);
        w.sort(Comparator.comparingDouble(c -> c.d));
        List<ScoredId> out = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, w.size()); i++) {
            Cand c = w.get(i);
            out.add(new ScoredId(nodes.get(c.node).id, 1.0f - c.d)); // score = cosine
        }
        return out;
    }

    // ---- internals ----

    private void connect(int a, int b, int layer, int cap) {
        Node na = nodes.get(a);
        Node nb = nodes.get(b);
        List<Integer> al = na.links.get(layer);
        if (!al.contains(b)) al.add(b);
        if (al.size() > cap) {
            al.sort((x, y) -> Float.compare(dist(na.vec, nodes.get(y).vec), dist(na.vec, nodes.get(x).vec)));
            al.subList(cap, al.size()).clear();
        }
        List<Integer> bl = nb.links.get(layer);
        if (!bl.contains(a)) bl.add(a);
        if (bl.size() > cap) {
            bl.sort((x, y) -> Float.compare(dist(nb.vec, nodes.get(y).vec), dist(nb.vec, nodes.get(x).vec)));
            bl.subList(cap, bl.size()).clear();
        }
    }

    private List<Integer> selectClosest(List<Cand> w, int cap, int self) {
        List<Cand> sorted = new ArrayList<>(w);
        sorted.sort(Comparator.comparingDouble(c -> c.d));
        List<Integer> out = new ArrayList<>();
        for (Cand c : sorted) {
            if (c.node == self) continue;
            out.add(c.node);
            if (out.size() >= cap) break;
        }
        return out;
    }

    private int nearestNode(List<Cand> w) {
        Cand best = w.get(0);
        for (Cand c : w) if (c.d < best.d) best = c;
        return best.node;
    }

    private List<Cand> searchLayer(float[] q, List<Integer> entryPoints, int ef, int layer) {
        PriorityQueue<Cand> cands = new PriorityQueue<>(Comparator.comparingDouble(c -> c.d));
        List<Cand> w = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        for (int ep : entryPoints) {
            float d = dist(q, nodes.get(ep).vec);
            cands.add(new Cand(ep, d));
            w.add(new Cand(ep, d));
            visited.add(ep);
        }
        while (!cands.isEmpty()) {
            Cand c = cands.poll();
            float farthest = farthestDist(w);
            if (c.d > farthest) break;
            for (int nb : nodes.get(c.node).links.get(layer)) {
                if (visited.contains(nb)) continue;
                visited.add(nb);
                float f = farthestDist(w);
                float dnb = dist(q, nodes.get(nb).vec);
                if (dnb < f || w.size() < ef) {
                    cands.add(new Cand(nb, dnb));
                    w.add(new Cand(nb, dnb));
                    if (w.size() > ef) removeFarthest(w);
                }
            }
        }
        return w;
    }

    private static float farthestDist(List<Cand> w) {
        float max = Float.NEGATIVE_INFINITY;
        for (Cand c : w) if (c.d > max) max = c.d;
        return max;
    }

    private static void removeFarthest(List<Cand> w) {
        int bi = 0;
        float max = w.get(0).d;
        for (int i = 1; i < w.size(); i++) {
            if (w.get(i).d > max) { max = w.get(i).d; bi = i; }
        }
        w.remove(bi);
    }

    private static float dist(float[] a, float[] b) {
        return 1.0f - Vectors.cosine(a, b); // distance in [0, 2]
    }

    @Override
    public int dim() { return dim; }

    @Override
    public int size() { return nodes.size(); }
}
