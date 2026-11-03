package halo.embed;

import halo.math.Vectors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * TF-IDF embedder trained on a corpus.
 *
 * <p>The vocabulary is the set of terms whose document frequency falls inside a
 * configurable band (too-frequent stop-words and hapax legomena are dropped).
 * Each document becomes a weighted vector over that vocabulary, sublinearly
 * scaled by term frequency and multiplied by inverse document frequency, then
 * L2-normalized.</p>
 */
public final class TfIdfEmbedder implements Embedder {

    private final int maxTerms;
    private final int minDf;
    private final double maxDfRatio;

    private int dim;
    private List<String> vocab = new ArrayList<>();
    private Map<String, Integer> termIndex = new HashMap<>();
    private double[] idf;

    public TfIdfEmbedder() { this(4096, 2, 0.5); }

    public TfIdfEmbedder(int maxTerms, int minDf, double maxDfRatio) {
        this.maxTerms = maxTerms;
        this.minDf = minDf;
        this.maxDfRatio = maxDfRatio;
    }

    /** Fit the vocabulary on a corpus. Idempotent: call once before embedding. */
    public void fit(List<String> corpus) {
        int n = corpus.size();
        Map<String, Integer> df = new HashMap<>();
        for (String doc : corpus) {
            for (String term : new java.util.HashSet<>(Tokenizers.words(doc))) {
                df.merge(term, 1, Integer::sum);
            }
        }
        TreeMap<String, Integer> sorted = new TreeMap<>(df);
        vocab = new ArrayList<>();
        for (Map.Entry<String, Integer> e : sorted.entrySet()) {
            int d = e.getValue();
            if (d < minDf) continue;
            if (maxDfRatio > 0 && (double) d / n > maxDfRatio) continue;
            vocab.add(e.getKey());
            if (vocab.size() >= maxTerms) break;
        }
        termIndex = new HashMap<>();
        for (int i = 0; i < vocab.size(); i++) termIndex.put(vocab.get(i), i);
        idf = new double[vocab.size()];
        for (int i = 0; i < vocab.size(); i++) {
            int d = df.get(vocab.get(i));
            idf[i] = Math.log((1.0 + n) / (1.0 + d)) + 1.0;
        }
        dim = vocab.size();
    }

    public List<String> vocabulary() { return new ArrayList<>(vocab); }

    @Override
    public int dim() { return dim; }

    @Override
    public float[] embed(String text) {
        if (vocab.isEmpty()) throw new IllegalStateException("embedder not fitted");
        float[] v = new float[dim];
        Map<String, Float> tf = new HashMap<>();
        List<String> tokens = Tokenizers.words(text);
        for (String t : tokens) tf.merge(t, 1.0f, Float::sum);
        for (Map.Entry<String, Float> e : tf.entrySet()) {
            Integer idx = termIndex.get(e.getKey());
            if (idx == null) continue;
            float raw = e.getValue();
            float tfWeight = (float) (1.0 + Math.log(raw)); // sublinear tf
            v[idx] = (float) (tfWeight * idf[idx]);
        }
        Vectors.l2NormalizeInPlace(v);
        return v;
    }
}
