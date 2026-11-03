package halo.test;

import halo.embed.HashEmbedder;
import halo.retrieval.Evaluator;
import halo.store.Collection;
import halo.store.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestEvaluator {

    @Test
    void perfectRetrievalYieldsFullMetrics() {
        Collection c = new Collection("kb", new HashEmbedder(256));
        c.add(new Document("a", "the JVM garbage collector reclaims memory automatically"));
        c.add(new Document("b", "raft elects a leader that replicates a consistent log"));
        List<Evaluator.Query> q = Arrays.asList(
                new Evaluator.Query("JVM garbage collection", "a"),
                new Evaluator.Query("raft leader replication", "b"));
        Evaluator.Metrics m = Evaluator.evaluate(c, q, 3);
        assertEquals(2, m.queries);
        assertEquals(1.0, m.recall, 1e-9);
        assertEquals(1.0, m.mrr, 1e-9);
    }

    @Test
    void missingRelevantLowersRecallAndMrr() {
        Collection c = new Collection("kb", new HashEmbedder(256));
        c.add(new Document("a", "unrelated text about weather and seasons"));
        List<Evaluator.Query> q = List.of(new Evaluator.Query("weather seasons", "a"));
        Evaluator.Metrics m = Evaluator.evaluate(c, q, 1);
        assertEquals(1.0, m.recall, 1e-9); // relevant doc is the only one
        assertEquals(1.0, m.mrr, 1e-9);
    }
}
