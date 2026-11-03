package halo.retrieval;

import halo.store.Collection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Offline retrieval evaluator.
 *
 * <p>Computes recall@k (fraction of queries whose relevant document appears in
 * the top-k) and MRR@k (mean reciprocal rank). No labels beyond the relevant
 * document id per query are required, which makes it easy to track retrieval
 * quality as the index or fusion weights change.</p>
 */
public final class Evaluator {

    /** A single evaluation query and its relevant source document id. */
    public static final class Query {
        public final String text;
        public final String relevantDocId;
        public Query(String text, String relevantDocId) {
            this.text = text;
            this.relevantDocId = relevantDocId;
        }
    }

    /** Aggregated metrics for one run. */
    public static final class Metrics {
        public final int k;
        public final int queries;
        public final double recall;
        public final double mrr;
        public Metrics(int k, int queries, double recall, double mrr) {
            this.k = k; this.queries = queries; this.recall = recall; this.mrr = mrr;
        }
        @Override public String toString() {
            return String.format("recall@%d=%.3f  mrr@%d=%.3f  (n=%d)",
                    k, recall, k, mrr, queries);
        }
    }

    public static Metrics evaluate(Collection collection, List<Query> queries, int k) {
        int hits = 0;
        double mrrSum = 0.0;
        for (Query q : queries) {
            List<RetrievalResult> results = collection.search(q.text, k);
            boolean found = false;
            int rank = 0;
            for (int i = 0; i < results.size(); i++) {
                if (results.get(i).docId().equals(q.relevantDocId)) {
                    found = true;
                    rank = i + 1;
                    break;
                }
            }
            if (found) {
                hits++;
                mrrSum += 1.0 / rank;
            }
        }
        int n = Math.max(1, queries.size());
        return new Metrics(k, queries.size(), (double) hits / n, mrrSum / n);
    }

    public static List<Query> parseQueries(List<?> raw) {
        List<Query> out = new ArrayList<>();
        for (Object o : raw) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> m = (java.util.Map<String, Object>) o;
            out.add(new Query(String.valueOf(m.get("text")), String.valueOf(m.get("relevantDocId"))));
        }
        return out;
    }

    public static Set<String> uniqueRelevant(List<Query> queries) {
        Set<String> s = new java.util.HashSet<>();
        for (Query q : queries) s.add(q.relevantDocId);
        return s;
    }
}
