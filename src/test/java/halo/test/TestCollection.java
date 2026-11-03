package halo.test;

import halo.embed.HashEmbedder;
import halo.retrieval.RetrievalResult;
import halo.store.Collection;
import halo.store.Document;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestCollection {

    private Collection sample() {
        Collection c = new Collection("kb", new HashEmbedder(256));
        c.add(new Document("gc", "The JVM garbage collector reclaims unreachable objects automatically using generational collection."));
        c.add(new Document("raft", "Raft is a consensus algorithm that elects a leader to replicate a consistent log across servers."));
        c.add(new Document("btree", "A B-tree is a balanced search tree used by databases for efficient indexing and range scans."));
        return c;
    }

    @Test
    void relevantDocAppearsInTopResults() {
        Collection c = sample();
        List<RetrievalResult> r = c.search("garbage collection in the JVM", 3);
        assertTrue(r.stream().anyMatch(x -> x.docId().equals("gc")));
        assertTrue(c.chunkCount() >= 3);
    }

    @Test
    void searchCarriesScoresAndMetadata() {
        Collection c = new Collection("kb", new HashEmbedder(256));
        c.add(new Document("x", "kubernetes orchestrates containers", java.util.Map.of("title", "k8s")));
        List<RetrievalResult> r = c.search("kubernetes orchestration", 1);
        assertEquals(1, r.size());
        assertEquals("k8s", r.get(0).metadata().get("title"));
        assertTrue(r.get(0).score() > 0);
    }

    @Test
    void saveAndLoadPreservesRetrieval() throws Exception {
        Collection c = sample();
        File tmp = File.createTempFile("halo-collection", ".json");
        tmp.deleteOnExit();
        c.save(tmp);

        Collection loaded = Collection.load(tmp, new HashEmbedder(256));
        assertEquals(c.documentCount(), loaded.documentCount());
        List<RetrievalResult> r = loaded.search("distributed consensus leader", 3);
        assertTrue(r.stream().anyMatch(x -> x.docId().equals("raft")));
    }
}
